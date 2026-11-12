# -*- coding: utf-8 -*-
"""把 Release 附件统一成 StoveSide-<版本号>.apk 并同步 SHA256SUMS 与正文。

为什么不用空格：GitHub 上传附件时会把文件名里的空格替换成点，
实测 "StoveSide 1.0.2.apk" 会被存成 "StoveSide.1.0.2.apk"，
不换行符也不行（U+00A0 直接 422）。所以统一用连字符。
"""

import hashlib
import io
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TOKEN_FILE = Path(r"D:\Documents\deepseek-harness\default-workspace\lanpulse\.gh_token")
REPO = "Levanll/StoveSide"
API = "https://api.github.com"


def h():
    return {
        "Authorization": "Bearer " + TOKEN_FILE.read_text(encoding="utf-8").strip(),
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "StoveSide-assets/1.0",
    }


def call(method, url, data=None, extra=None):
    headers = h()
    if extra:
        headers.update(extra)
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=180) as r:
            return r.status, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()


def asset_name(version: str) -> str:
    return "StoveSide-" + version + ".apk"


def process(tag: str) -> None:
    version = tag.lstrip("v")
    want = asset_name(version)
    status, body = call("GET", API + "/repos/" + REPO + "/releases/tags/" + tag)
    if status != 200:
        print(tag, "读取失败 HTTP", status)
        return
    rel = json.loads(body.decode("utf-8"))
    assets = rel.get("assets", [])
    names = [x["name"] for x in assets]
    apk = next((x for x in assets if x["name"].endswith(".apk")), None)
    if not apk:
        print(tag, "没有 APK 附件")
        return
    if apk["name"] == want:
        print(tag, "已是目标名称:", want)
        return
    content = call("GET", apk["browser_download_url"])[1]
    digest = hashlib.sha256(content).hexdigest()
    print(tag, ":", apk["name"], "->", want, "(" + str(len(content)) + " bytes)")
    upload_url = rel["upload_url"].split("{", 1)[0]
    s, b = call("POST", upload_url + "?name=" + urllib.parse.quote(want), content,
                {"Content-Type": "application/octet-stream"})
    if s not in (200, 201):
        print("   上传失败 HTTP", s, b.decode("utf-8", "replace")[:200])
        return
    new_name = json.loads(b.decode("utf-8"))["name"]
    print("   已上传:", new_name)
    for x in assets:
        if x["name"] in (apk["name"], "SHA256SUMS"):
            d, _ = call("DELETE", API + "/repos/" + REPO + "/releases/assets/" + str(x["id"]))
            print("   删除旧附件", x["name"], "HTTP", d)
    sums = (digest + "  " + new_name + "\n").encode("utf-8")
    s2, _ = call("POST", upload_url + "?name=SHA256SUMS", sums,
                 {"Content-Type": "application/octet-stream"})
    print("   SHA256SUMS 重建 HTTP", s2)
    notes = ROOT / "notes" / (tag + ".md")
    if notes.is_file():
        s3, _ = call("PATCH", API + "/repos/" + REPO + "/releases/" + str(rel["id"]),
                     json.dumps({"body": notes.read_text(encoding="utf-8").strip()}, ensure_ascii=False).encode("utf-8"),
                     {"Content-Type": "application/json; charset=utf-8"})
        print("   正文更新 HTTP", s3)


if __name__ == "__main__":
    for t in ("v1.0.0", "v1.0.1", "v1.0.2"):
        process(t)
