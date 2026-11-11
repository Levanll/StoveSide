#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""StoveSide 一键发布脚本（仅依赖 Python 标准库，不引第三方包）。

流程：检查仓库 -> 必要时创建仓库 -> git add/commit/push -> 打 tag
      -> 建 Release 并上传 APK + SHA256SUMS -> 把 docs/ 部署到 gh-pages

每一步执行前都会先运行 scripts/scan_secrets.py；命中敏感信息立即中止。
--dry-run 只打印将要执行的动作：不发起网络请求、不写入本地状态、不跑 git 变更命令。

Token 只从环境变量 GITHUB_TOKEN 或 lanpulse/.gh_token 文件读取；
脚本只打印长度和前缀类型，绝不打印 token 内容。
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
SCANNER = PROJECT_ROOT / "scripts" / "scan_secrets.py"
GRADLE_FILE = PROJECT_ROOT / "app" / "build.gradle.kts"
APK_RELATIVE = Path("app/build/outputs/apk/release/app-release.apk")
DOCS_DIR = PROJECT_ROOT / "docs"
DEFAULT_REPO = "Levanll/StoveSide"
API_ROOT = "https://api.github.com"
UPLOADS_ROOT = "https://uploads.github.com"
TOKEN_ENV = "GITHUB_TOKEN"
TOKEN_FILE = Path(r"D:\Documents\deepseek-harness\default-workspace\lanpulse\.gh_token")
USER_AGENT = "StoveSide-publish/1.0"
PAGES_BRANCH = "gh-pages"


class PublishError(RuntimeError):
    """发布流程中可预期的失败。"""


# --------------------------------------------------------------------------
# 输出
# --------------------------------------------------------------------------
class Reporter:
    def __init__(self, dry_run: bool) -> None:
        self.dry_run = dry_run
        self.index = 0

    @staticmethod
    def _emit(text: str) -> None:
        print(text, flush=True)

    def title(self, text: str) -> None:
        print("\n== " + text + " " + "=" * max(0, 60 - len(text)))

    def step(self, title: str) -> None:
        self.index += 1
        tag = "[dry-run] " if self.dry_run else ""
        print("\n--- " + tag + "步骤 " + str(self.index) + ": " + title + " ---")

    def info(self, text: str) -> None:
        print("    " + text)

    def action(self, text: str) -> None:
        prefix = "[dry-run] 将执行: " if self.dry_run else "    执行: "
        print(prefix + text)


# --------------------------------------------------------------------------
# 敏感信息闸门
# --------------------------------------------------------------------------
def gate(reporter: Reporter, step_name: str) -> None:
    """执行任何写操作前先扫描仓库，命中即中止。"""
    if not SCANNER.is_file():
        raise PublishError("找不到敏感信息扫描脚本: " + str(SCANNER))
    print("    [scan] 运行 scan_secrets.py（保护步骤 " + str(reporter.index) + ": " + step_name + "）", flush=True)
    proc = subprocess.run([sys.executable, str(SCANNER)], cwd=str(PROJECT_ROOT))
    if proc.returncode != 0:
        raise PublishError("敏感信息扫描未通过，发布中止（保护步骤: " + step_name + "）")


# --------------------------------------------------------------------------
# Token
# --------------------------------------------------------------------------
def load_token() -> str:
    token = os.environ.get(TOKEN_ENV, "").strip()
    if token:
        return token
    if TOKEN_FILE.is_file():
        token = TOKEN_FILE.read_text(encoding="utf-8").strip()
        if token:
            return token
    raise PublishError(
        "没有可用的 GitHub Token：请设置环境变量 " + TOKEN_ENV + " 或写入 " + str(TOKEN_FILE)
    )


def describe_token(token: str) -> str:
    """只描述类型与长度，绝不返回内容。"""
    if not token:
        return "未配置（dry-run 不读取凭据）"
    kinds = (
        ("ghp_", "classic personal access token"),
        ("github_pat_", "fine-grained personal access token"),
        ("gho_", "OAuth token"),
        ("ghs_", "App installation token"),
    )
    for prefix, label in kinds:
        if token.startswith(prefix):
            return "前缀匹配 " + prefix + "（" + label + "），长度 " + str(len(token))
    return "前缀不匹配已知 GitHub token 形式，长度 " + str(len(token))


# --------------------------------------------------------------------------
# 本地命令
# --------------------------------------------------------------------------
def git(*args: str, capture: bool = True) -> str:
    shown = "git " + " ".join(args)
    if not capture:
        print("    $ " + shown)
    proc = subprocess.run(
        ["git", *args],
        cwd=str(PROJECT_ROOT),
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if proc.returncode != 0:
        detail = (proc.stderr or proc.stdout or "").strip()
        raise PublishError("命令失败（exit " + str(proc.returncode) + "）: " + shown + "\n" + detail)
    return (proc.stdout or "").strip()


def git_ok(*args: str) -> bool:
    proc = subprocess.run(["git", *args], cwd=str(PROJECT_ROOT), capture_output=True, text=True)
    return proc.returncode == 0


def run_git(reporter: Reporter, *args: str) -> None:
    reporter.action("git " + " ".join(args))
    if reporter.dry_run:
        return
    git(*args, capture=False)


# --------------------------------------------------------------------------
# 项目信息
# --------------------------------------------------------------------------
def read_version() -> tuple:
    if not GRADLE_FILE.is_file():
        raise PublishError("找不到 " + str(GRADLE_FILE))
    text = GRADLE_FILE.read_text(encoding="utf-8")
    version_name = re.search(r'versionName\s*=\s*"([^"]+)"', text)
    version_code = re.search(r"versionCode\s*=\s*(\d+)", text)
    if not version_name or not version_code:
        raise PublishError("无法从 app/build.gradle.kts 解析 versionName / versionCode")
    return version_name.group(1), version_code.group(1)


def sha256_of(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def collect_assets(apk: Path, workdir: Path) -> list:
    """写出 SHA256SUMS 并返回要上传的资产文件名列表。"""
    if not apk.is_file():
        raise PublishError(
            "找不到 release APK: "
            + str(apk)
            + "\n请先运行 scripts/build.ps1（或让 CI 产出），再执行发布。"
        )
    sums = workdir / "SHA256SUMS"
    sums.write_text(sha256_of(apk) + "  " + apk.name + "\n", encoding="utf-8")
    return [apk.name, "SHA256SUMS"]


# --------------------------------------------------------------------------
# GitHub API
# --------------------------------------------------------------------------
class GitHub:
    def __init__(self, token: str, dry_run: bool, reporter: Reporter) -> None:
        self.token = token
        self.dry_run = dry_run
        self.reporter = reporter

    def _request(self, method: str, url: str, data=None, headers=None):
        full = url if url.startswith("http") else API_ROOT + url
        request_headers = {
            "Accept": "application/vnd.github+json",
            "Authorization": "Bearer " + self.token,
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": USER_AGENT,
        }
        if headers:
            request_headers.update(headers)
        req = urllib.request.Request(full, data=data, method=method, headers=request_headers)
        try:
            with urllib.request.urlopen(req, timeout=120) as response:
                return response.status, response.read()
        except urllib.error.HTTPError as exc:
            return exc.code, exc.read()
        except urllib.error.URLError as exc:
            raise PublishError("网络请求失败（" + method + " " + full + "）: " + str(exc.reason))

    def call(self, method: str, path: str, payload=None, expect=(), preview=None) -> tuple:
        """返回 (status, json|None)。dry-run 下不发请求，返回 (0, None)。"""
        printable = method + " " + (path if path.startswith("http") else API_ROOT + path)
        if payload is not None:
            printable += "  body=" + json.dumps(payload, ensure_ascii=False)
        self.reporter.action(printable)
        if self.dry_run:
            return 0, None
        headers = {}
        if preview:
            headers["Accept"] = preview
        data = None
        if payload is not None:
            data = json.dumps(payload).encode("utf-8")
            headers["Content-Type"] = "application/json"
        status, body = self._request(method, path, data=data, headers=headers)
        if status in expect:
            if not body:
                return status, None
            try:
                return status, json.loads(body.decode("utf-8"))
            except ValueError:
                return status, None
        detail = body.decode("utf-8", errors="replace")[:400]
        raise PublishError("GitHub API 失败: " + printable + "\n  HTTP " + str(status) + " " + detail)

    def upload(self, upload_url: str, filename: str, content: bytes) -> tuple:
        base = upload_url.split("{", 1)[0]
        url = base + "?name=" + urllib.parse.quote(filename)
        self.reporter.action("POST " + url + "  (" + str(len(content)) + " bytes)")
        if self.dry_run:
            return 0, None
        status, body = self._request(
            "POST",
            url,
            data=content,
            headers={"Content-Type": "application/octet-stream"},
        )
        if status not in (200, 201):
            raise PublishError(
                "上传 " + filename + " 失败：HTTP " + str(status) + " " + body.decode("utf-8", "replace")[:300]
            )
        return status, json.loads(body.decode("utf-8"))


# --------------------------------------------------------------------------
# 各步骤
# --------------------------------------------------------------------------
def step_check_repo(gh: GitHub, repo: str) -> bool:
    status, data = gh.call("GET", "/repos/" + repo, expect=(200, 404))
    if status == 404:
        return False
    if data is None:
        return True
    print("    仓库存在: " + data.get("full_name", repo) + "（private=" + str(data.get("private")) + "）")
    return True


def step_create_repo(gh: GitHub, repo: str, private: bool, description: str, homepage: str) -> None:
    owner, name = repo.split("/", 1)
    _, user = gh.call("GET", "/user", expect=(200, 401))
    login = (user or {}).get("login", "")
    payload = {
        "name": name,
        "private": private,
        "has_issues": True,
        "has_wiki": False,
        "description": description,
        "homepage": homepage,
    }
    if owner.lower() == login.lower():
        gh.call("POST", "/user/repos", payload, expect=(201,))
    else:
        gh.call("POST", "/orgs/" + owner + "/repos", payload, expect=(201,))


def step_git_commit_push(reporter: Reporter, branch: str, message: str, repo: str) -> None:
    if not git_ok("rev-parse", "--git-dir"):
        run_git(reporter, "init", "-q")
    run_git(reporter, "add", "-A")
    if reporter.dry_run:
        reporter.action("git commit -m " + json.dumps(message, ensure_ascii=False) + "（有改动时）")
    else:
        status = git("status", "--porcelain")
        if status:
            reporter.action("有 " + str(len(status.splitlines())) + " 项改动，提交")
            git("commit", "-q", "-m", message, capture=False)
        else:
            reporter.info("工作区无改动，跳过 commit")
    remote_url = "https://github.com/" + repo + ".git"
    if not reporter.dry_run and git_ok("remote", "get-url", "origin"):
        reporter.info("origin 已存在: " + git("remote", "get-url", "origin"))
    else:
        run_git(reporter, "remote", "add", "origin", remote_url)
    run_git(reporter, "push", "-u", "origin", "HEAD:" + branch)


def step_tag(reporter: Reporter, tag: str, message: str) -> None:
    if not reporter.dry_run and git_ok("rev-parse", "-q", "--verify", "refs/tags/" + tag):
        reporter.info("tag " + tag + " 已存在（" + git("rev-parse", "--short", tag) + "），跳过创建")
    else:
        run_git(reporter, "tag", "-a", tag, "-m", message)
    run_git(reporter, "push", "origin", "refs/tags/" + tag)


def step_release(gh: GitHub, repo: str, tag: str, title: str, body: str, assets: list, workdir: Path) -> None:
    status, data = gh.call("GET", "/repos/" + repo + "/releases/tags/" + tag, expect=(200, 404))
    if status == 200 and data:
        print("    Release 已存在: " + data.get("html_url", tag))
        upload_url = data.get("upload_url", "")
        existing = {item.get("name") for item in data.get("assets", [])}
    else:
        status, data = gh.call(
            "POST",
            "/repos/" + repo + "/releases",
            {"tag_name": tag, "name": title, "body": body, "draft": False, "prerelease": False},
            expect=(201,),
        )
        if data:
            print("    已创建 Release: " + data.get("html_url", ""))
        upload_url = (data or {}).get(
            "upload_url", UPLOADS_ROOT + "/repos/" + repo + "/releases/0/assets"
        )
        existing = set()
    for name in assets:
        if name in existing:
            print("    跳过已存在资产: " + name)
            continue
        path = workdir / name
        if gh.dry_run:
            gh.reporter.action(
                "读取本地文件并上传 " + str(path) + ("（dry-run 不读取，当前不存在）" if not path.is_file() else "")
            )
            continue
        gh.upload(upload_url, name, path.read_bytes())


def build_release_body(version_name: str, version_code: str, sha256: str) -> str:
    template = (
        "StoveSide {name} (versionCode {code})\n\n"
        "- 包名: cn.levanll.stoveside\n"
        "- 构建: `gradle assembleRelease`（scripts/build.ps1）\n"
        "- 安装包 SHA256: `{sha}`\n\n"
        "校验方式：下载 APK 与 SHA256SUMS 后执行 `sha256sum -c SHA256SUMS`。\n"
    )
    return template.format(name=version_name, code=version_code, sha=sha256)


def step_pages(reporter: Reporter, repo: str, message: str) -> None:
    if not DOCS_DIR.is_dir():
        reporter.info("没有 docs/ 目录，跳过 gh-pages 部署")
        return
    workdir = (
        Path(tempfile.gettempdir()) / "stoveside-pages-dry-run"
        if reporter.dry_run
        else Path(tempfile.mkdtemp(prefix="stoveside-pages-"))
    )
    reporter.action("复制 " + str(DOCS_DIR) + " -> " + str(workdir))
    if reporter.dry_run:
        reporter.action("在 " + str(workdir) + " 写入 .nojekyll")
        reporter.action("git init && git add -A && git commit -m " + json.dumps(message, ensure_ascii=False))
        reporter.action("git push --force origin HEAD:" + PAGES_BRANCH + "（仓库 " + repo + "）")
        return
    for item in DOCS_DIR.iterdir():
        target = workdir / item.name
        if item.is_dir():
            shutil.copytree(item, target)
        else:
            shutil.copy2(item, target)
    (workdir / ".nojekyll").write_text("", encoding="utf-8")
    remote_url = "https://github.com/" + repo + ".git"
    for args in (
        ("init", "-q"),
        ("remote", "add", "origin", remote_url),
        ("checkout", "-q", "-b", PAGES_BRANCH),
        ("add", "-A"),
        ("commit", "-q", "-m", message),
    ):
        print("    $ git " + " ".join(args))
        proc = subprocess.run(["git", *args], cwd=str(workdir), capture_output=True, text=True, encoding="utf-8", errors="replace")
        if proc.returncode != 0:
            raise PublishError("gh-pages 准备失败: git " + " ".join(args) + "\n" + (proc.stderr or "").strip())
    print("    $ git push --force origin HEAD:" + PAGES_BRANCH)
    proc = subprocess.run(
        ["git", "push", "--force", "origin", "HEAD:" + PAGES_BRANCH],
        cwd=str(workdir),
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if proc.returncode != 0:
        raise PublishError("gh-pages 推送失败:\n" + (proc.stderr or "").strip())


# --------------------------------------------------------------------------
# 主流程
# --------------------------------------------------------------------------
def parse_args(argv=None):
    parser = argparse.ArgumentParser(
        description="StoveSide 一键发布：仓库检查/创建、提交推送、打 tag、建 Release、部署 gh-pages",
    )
    parser.add_argument("--tag", help="发布 tag，默认 v<versionName>")
    parser.add_argument("--repo", default=DEFAULT_REPO, help="owner/repo，默认 " + DEFAULT_REPO)
    parser.add_argument("--message", default=None, help="commit message，默认按版本自动生成")
    parser.add_argument("--branch", default=None, help="推送分支，默认当前分支")
    parser.add_argument("--public", action="store_true", help="仓库不存在时创建为 public（默认 private）")
    parser.add_argument("--skip-pages", action="store_true", help="跳过 gh-pages 部署")
    parser.add_argument(
        "--dry-run", action="store_true", help="只打印将要执行的动作，不发网络请求、不改本地状态"
    )
    parser.add_argument("--yes", action="store_true", help="跳过交互确认（无人值守时使用）")
    return parser.parse_args(argv)


def main(argv=None) -> int:
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8")
    args = parse_args(argv)
    reporter = Reporter(args.dry_run)

    try:
        version_name, version_code = read_version()
        tag = args.tag or ("v" + version_name)
        message = args.message or (
            "release: StoveSide " + version_name + " (versionCode " + version_code + ")"
        )
        try:
            token = load_token()
        except PublishError:
            if not args.dry_run:
                raise
            token = ""

        if not git_ok("rev-parse", "--git-dir"):
            print("    git: 当前目录还不是 git 仓库，脚本会在推送前初始化")
            branch = args.branch or "main"
            commit = "(初始化后生成)"
        else:
            branch = args.branch or git("rev-parse", "--abbrev-ref", "HEAD")
            commit = git("rev-parse", "--short", "HEAD")

        apk = PROJECT_ROOT / APK_RELATIVE
        workdir = (
            Path(tempfile.gettempdir()) / "stoveside-dist-dry-run"
            if args.dry_run
            else Path(tempfile.mkdtemp(prefix="stoveside-dist-"))
        )
        sha256 = sha256_of(apk) if apk.is_file() else "(APK 尚未构建)"
        assets = [apk.name, "SHA256SUMS"]

        reporter.title("StoveSide 发布" + ("（dry-run，不产生任何变更）" if args.dry_run else ""))
        reporter.info("仓库:   " + args.repo)
        reporter.info("版本:   " + version_name + " (versionCode " + version_code + ")")
        reporter.info("tag:    " + tag)
        reporter.info("分支:   " + branch + " @ " + commit)
        reporter.info("APK:    " + str(APK_RELATIVE) + ("（已存在）" if apk.is_file() else "（缺失）"))
        reporter.info("Token:  " + describe_token(token) + "（内容不打印）")

        if not apk.is_file() and not args.dry_run:
            raise PublishError("release APK 不存在，请先运行 scripts/build.ps1 构建：" + str(apk))

        if not args.dry_run and not args.yes:
            print("\n即将推送代码、打 tag 并发布到 " + args.repo + "，输入 yes 继续：", end=" ")
            if sys.stdin.readline().strip().lower() != "yes":
                print("已取消。")
                return 1

        gh = GitHub(token, args.dry_run, reporter)

        reporter.step("检查仓库是否存在")
        gate(reporter, "检查仓库")
        exists = step_check_repo(gh, args.repo)

        reporter.step("仓库已存在，跳过创建" if exists else "创建仓库")
        gate(reporter, "仓库创建")
        if not exists:
            owner, name = args.repo.split("/", 1)
            homepage = "https://" + owner.lower() + ".github.io/" + name + "/"
            step_create_repo(
                gh,
                args.repo,
                private=not args.public,
                description="StoveSide 灶暖 - 本地优先的厨房计时与菜谱管理 Android 应用",
                homepage=homepage,
            )

        reporter.step("提交并推送源码")
        gate(reporter, "提交推送")
        step_git_commit_push(reporter, branch, message, args.repo)

        reporter.step("打 tag " + tag)
        gate(reporter, "打 tag")
        step_tag(reporter, tag, message)
        gate(reporter, "推送 tag")

        reporter.step("创建 Release 并上传产物")
        gate(reporter, "创建 Release")
        if args.dry_run:
            reporter.action("生成 SHA256SUMS（APK sha256 前缀 " + sha256[:12] + "）")
        else:
            collect_assets(apk, workdir)
        step_release(
            gh,
            args.repo,
            tag,
            "StoveSide " + version_name,
            build_release_body(version_name, version_code, sha256),
            assets,
            workdir,
        )

        reporter.step("部署 docs/ 到 gh-pages")
        gate(reporter, "部署 gh-pages")
        if args.skip_pages:
            reporter.info("--skip-pages：跳过")
        else:
            step_pages(reporter, args.repo, "docs: 更新站点（" + version_name + "）")

        reporter.title("完成" if not args.dry_run else "dry-run 结束：以上动作均未执行")
        if not args.dry_run:
            owner, name = args.repo.split("/", 1)
            reporter.info("Release: https://github.com/" + args.repo + "/releases/tag/" + tag)
            reporter.info("Pages:   https://" + owner.lower() + ".github.io/" + name + "/")
        return 0
    except PublishError as exc:
        print("\n[发布失败] " + str(exc), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
