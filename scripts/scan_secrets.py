#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""StoveSide 敏感信息扫描。

在发布前扫描项目目录里可能泄漏的凭据，命中即退出码 1。
只输出「模式名 + 文件 + 行号」，绝不输出命中的真实内容。

用法:
    python scripts/scan_secrets.py                    # 扫描将要发布的内容（遵守 .gitignore）
    python scripts/scan_secrets.py --include-ignored  # 连被忽略的文件一起扫（仓库转公开前审计用）
    python scripts/scan_secrets.py --root <dir>
"""

from __future__ import annotations

import argparse
import fnmatch
import re
import subprocess
import sys
from pathlib import Path
from typing import Iterator, List, NamedTuple

PROJECT_ROOT = Path(__file__).resolve().parent.parent

SKIP_DIRS = {
    ".git",
    "build",
    ".gradle",
    ".venv",
    "venv",
    "__pycache__",
    ".idea",
    ".kotlin",
    "dist",
    "out",
}

BINARY_SUFFIXES = {
    ".apk", ".aab", ".jar", ".jks", ".keystore", ".zip", ".png", ".jpg", ".jpeg",
    ".webp", ".gif", ".ttf", ".otf", ".so", ".dex", ".class", ".bin",
}

# 模式名 -> 正则。关键字用字符串拼接构造，避免扫描器命中自身源码。
PATTERNS = (
    ("github-token", re.compile("gh" + r"p_[A-Za-z0-9]{20,}")),
    ("github-fine-grained-token", re.compile("github" + r"_pat_[A-Za-z0-9_]{40,}")),
    ("private-key", re.compile("-{5}BEGIN[A-Z ]{0,40}PRIVATE KEY-{5}")),
)

TOKEN_FILE_NAME = "." + "gh_token"

# 口令字段：等号或冒号右侧跟着实际口令值
PASSWORD_FIELD = re.compile(
    r"(store" + r"Password|key" + r"Password)\s*[=:]\s*(?P<value>\S.*)$",
    re.IGNORECASE,
)


class Finding(NamedTuple):
    path: str
    line: int
    pattern: str


def _relative(path: Path, root: Path) -> str:
    try:
        return str(path.relative_to(root))
    except ValueError:
        return str(path)


def _git_tracked_and_untracked(root: Path) -> List[Path]:
    """返回 git 视角下「可能被发布」的文件（已跟踪 + 未忽略的新文件）。"""
    try:
        proc = subprocess.run(
            ["git", "-C", str(root), "ls-files", "-z", "--cached", "--others", "--exclude-standard"],
            capture_output=True,
            check=True,
        )
    except (OSError, subprocess.CalledProcessError):
        return []
    files = []
    for raw in proc.stdout.split(b"\x00"):
        if not raw:
            continue
        candidate = root / raw.decode("utf-8", "surrogateescape")
        if candidate.is_file():
            files.append(candidate)
    return files


def _walk(root: Path) -> Iterator[Path]:
    for path in root.rglob("*"):
        if not path.is_file():
            continue
        parts = path.relative_to(root).parts
        if any(part in SKIP_DIRS for part in parts[:-1]):
            continue
        if path.suffix.lower() in BINARY_SUFFIXES:
            continue
        yield path


def _gitignore_matchers(root: Path) -> list:
    """非 git 仓库时，用 .gitignore 里简单可判定的规则兜底。"""
    path = root / ".gitignore"
    if not path.is_file():
        return []
    matchers = []
    for raw in path.read_text(encoding="utf-8", errors="replace").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or line.startswith("!"):
            continue
        if line.endswith("/"):
            matchers.append(("dir", line.rstrip("/")))
        else:
            matchers.append(("glob", line))
    return matchers


def _is_ignored(path: Path, root: Path, matchers: list) -> bool:
    parts = path.relative_to(root).parts
    for kind, value in matchers:
        if kind == "dir" and value in parts[:-1]:
            return True
        if kind == "glob" and fnmatch.fnmatch(parts[-1], value):
            return True
    return False


def _ignored_by_git(root: Path, files: List[Path]) -> set:
    """用 git check-ignore 标出被忽略的文件（非 git 仓库时返回空集合）。"""
    if not files:
        return set()
    payload = "\n".join(str(item) for item in files)
    try:
        proc = subprocess.run(
            ["git", "-C", str(root), "check-ignore", "--stdin"],
            input=payload.encode("utf-8", "surrogateescape"),
            capture_output=True,
        )
    except OSError:
        return set()
    if proc.returncode not in (0, 1):
        return set()
    return {
        Path(line.decode("utf-8", "surrogateescape")).resolve()
        for line in proc.stdout.splitlines()
        if line.strip()
    }


def iter_targets(root: Path, include_ignored: bool) -> Iterator[Path]:
    """默认只扫「可能被发布」的内容：git 已跟踪 + 未被 .gitignore 排除的新文件。"""
    walked = list(_walk(root))
    if include_ignored:
        yield from walked
        return
    if (root / ".git").exists():
        tracked = _git_tracked_and_untracked(root)
        if tracked:
            yield from tracked
            return
    ignored = _ignored_by_git(root, walked)
    matchers = _gitignore_matchers(root)
    for path in walked:
        if path.resolve() in ignored:
            continue
        if matchers and _is_ignored(path, root, matchers):
            continue
        yield path


def is_literal_secret(value: str) -> bool:
    """判断口令字段右侧是「真实填入的口令」，而不是代码里的变量名或文档占位。

    只做形态判断，不保留、不返回任何内容。
    """
    text = value.strip().strip("\"',;").strip()
    if not text:
        return False
    if "getProperty(" in text or "getenv(" in text or "environ" in text:
        return False
    if text.startswith(("$", "%", "(")):  # printf 格式、PowerShell / Gradle / 模板占位
        return False
    return True


def scan_file(path: Path, root: Path) -> List[Finding]:
    findings: List[Finding] = []
    relative = _relative(path, root)
    if path.name == TOKEN_FILE_NAME:
        findings.append(Finding(relative, 0, "token-file"))
    try:
        data = path.read_bytes()
    except OSError:
        return findings
    if b"\x00" in data[:8192]:
        return findings
    for lineno, line in enumerate(data.decode("utf-8", errors="replace").splitlines(), 1):
        for name, pattern in PATTERNS:
            if pattern.search(line):
                findings.append(Finding(relative, lineno, name))
        match = PASSWORD_FIELD.search(line)
        if match and is_literal_secret(match.group("value")):
            findings.append(Finding(relative, lineno, "literal-" + match.group(1).lower()))
    return findings


def scan(root: Path = PROJECT_ROOT, include_ignored: bool = False) -> List[Finding]:
    root = Path(root).resolve()
    findings: List[Finding] = []
    for path in iter_targets(root, include_ignored):
        findings.extend(scan_file(path, root))
    return findings


def report(findings: List[Finding], root: Path = PROJECT_ROOT) -> int:
    if not findings:
        print("[scan] OK 未发现敏感信息（根目录 " + str(root) + "）")
        return 0
    print("[scan] FAILED 发现 " + str(len(findings)) + " 处敏感信息，发布已中止：", file=sys.stderr)
    for item in findings:
        location = item.path + ":" + str(item.line) if item.line else item.path
        print("  - " + location + "  模式=" + item.pattern, file=sys.stderr)
    print("[scan] 只显示模式名，命中内容不打印。请先清理后再发布。", file=sys.stderr)
    return 1


def main(argv=None) -> int:
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description="扫描项目中可能泄漏的凭据，命中即 exit 1")
    parser.add_argument("--root", default=str(PROJECT_ROOT), help="扫描根目录，默认项目根")
    parser.add_argument("--include-ignored", action="store_true", help="连 .gitignore 排除的文件一起扫描")
    args = parser.parse_args(argv)
    findings = scan(Path(args.root), args.include_ignored)
    return report(findings, Path(args.root).resolve())


if __name__ == "__main__":
    sys.exit(main())
