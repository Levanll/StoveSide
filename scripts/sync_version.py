# -*- coding: utf-8 -*-
"""把 app/build.gradle.kts 里的 versionName / versionCode 同步到站点与文档。

为什么需要它：站点首页的「版本 1.0.0」是手写死在 HTML 里的，
改版本号时最容易漏——1.0.1、1.0.2 两版就都漏了。
这里把它做成脚本，发布流程里强制先跑一遍，版本号只有一个真相来源。

用法：
    python scripts/sync_version.py           # 就地同步
    python scripts/sync_version.py --check   # 只检查，不同步（CI 用）
"""

import io
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GRADLE = ROOT / "app" / "build.gradle.kts"

# (文件, 正则, 替换模板)；模板里 {name} 是 versionName，{code} 是 versionCode
TARGETS = [
    ("docs/index.html", r"版本 \d+\.\d+\.\d+ ·", "版本 {name} ·"),
    ("docs/en/index.html", r"Version \d+\.\d+\.\d+ ·", "Version {name} ·"),
    ("README.md", r"\| 当前版本 \| \d+\.\d+\.\d+（versionCode \d+） \|", "| 当前版本 | {name}（versionCode {code}） |"),
    ("README.md", r"当前版本 \d+\.\d+\.\d+。", "当前版本 {name}。"),
    ("README_EN.md", r"Current version \d+\.\d+\.\d+", "Current version {name}"),
    ("README_EN.md", r"\| Version \| \d+\.\d+\.\d+ \(versionCode \d+\) \|", "| Version | {name} (versionCode {code}) |"),
]


def read_version():
    text = io.open(GRADLE, encoding="utf-8").read()
    name = re.search(r'versionName\s*=\s*"([^"]+)"', text)
    code = re.search(r"versionCode\s*=\s*(\d+)", text)
    if not name or not code:
        raise SystemExit("无法从 app/build.gradle.kts 解析 versionName / versionCode")
    return name.group(1), code.group(1)


def sync(check_only: bool) -> int:
    name, code = read_version()
    changed = []
    missed = []
    for rel, pattern, template in TARGETS:
        path = ROOT / rel
        if not path.is_file():
            print("  [skip] 缺文件 " + rel)
            continue
        text = io.open(path, encoding="utf-8").read()
        repl = template.format(name=name, code=code)
        new, n = re.subn(pattern, repl, text)
        if n == 0:
            missed.append(rel + " -> " + pattern)
        if n and new != text:
            changed.append(rel)
            if not check_only:
                io.open(path, "w", encoding="utf-8", newline="\n").write(new)
    print("版本真相来源: " + name + " (versionCode " + code + ")")
    if missed:
        print("以下目标未匹配到版本号，请检查正则或文档格式: " + ", ".join(missed))
        return 1
    if check_only:
        if changed:
            print("以下文件版本号不一致，需要同步: " + ", ".join(changed))
            return 1
        print("全部一致")
        return 0
    if changed:
        print("已同步: " + ", ".join(changed))
    else:
        print("无需改动")
    return 0


if __name__ == "__main__":
    sys.exit(sync("--check" in sys.argv))
