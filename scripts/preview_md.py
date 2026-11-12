# -*- coding: utf-8 -*-
"""极简 Markdown 渲染，仅用于本地视觉核对 README 版式（不参与构建/发布）。"""
import io, re, sys, html

def inline(t):
    t = html.escape(t)
    t = re.sub(r"`([^`]+)`", r"<code>\1</code>", t)
    t = re.sub(r"\!\[([^\]]*)\]\(([^)]+)\)", r'<img alt="\1" src="\2">', t)
    t = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r'<a href="\2">\1</a>', t)
    t = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", t)
    t = re.sub(r"(?<![*\w])\*([^*\n]+)\*(?!\*)", r"<em>\1</em>", t)
    return t

def render(md):
    lines = md.splitlines()
    out, i = [], 0
    while i < len(lines):
        ln = lines[i]
        if ln.startswith("```"):
            buf, i = [], i + 1
            while i < len(lines) and not lines[i].startswith("```"):
                buf.append(html.escape(lines[i])); i += 1
            out.append("<pre><code>" + "\n".join(buf) + "</code></pre>"); i += 1; continue
        m = re.match(r"^(#{1,4}) (.*)$", ln)
        if m:
            n = len(m.group(1)); out.append(f"<h{n}>{inline(m.group(2))}</h{n}>"); i += 1; continue
        if ln.strip() == "---":
            out.append("<hr>"); i += 1; continue
        if ln.startswith("|"):
            rows = []
            while i < len(lines) and lines[i].startswith("|"):
                rows.append(lines[i]); i += 1
            cells = [[c.strip() for c in r.strip().strip("|").split("|")] for r in rows]
            hdr = cells[0]; body = [c for c in cells[1:] if not all(set(x) <= set("-: ") for x in c)]
            t = "<table><thead><tr>" + "".join(f"<th>{inline(c)}</th>" for c in hdr) + "</tr></thead><tbody>"
            t += "".join("<tr>" + "".join(f"<td>{inline(c)}</td>" for c in r) + "</tr>" for r in body)
            out.append(t + "</tbody></table>"); continue
        if re.match(r"^\s*[-*] ", ln) or re.match(r"^\s*\d+\. ", ln):
            items = []
            while i < len(lines) and re.match(r"^\s*([-*]|\d+\.) ", lines[i]):
                items.append(re.sub(r"^\s*([-*]|\d+\.) ", "", lines[i])); i += 1
            out.append("<ul>" + "".join(f"<li>{inline(x)}</li>" for x in items) + "</ul>"); continue
        if ln.startswith(">"):
            out.append("<blockquote>" + inline(ln.lstrip("> ")) + "</blockquote>"); i += 1; continue
        if ln.strip() == "":
            i += 1; continue
        out.append("<p>" + inline(ln) + "</p>"); i += 1
    return "\n".join(out)

CSS = """<style>body{max-width:980px;margin:0 auto;padding:28px;font:15px/1.75 -apple-system,"Microsoft YaHei",sans-serif;color:#24292f;background:#fff}
img{max-width:100%;border-radius:8px}table{border-collapse:collapse;margin:14px 0;width:100%}
td,th{border:1px solid #d8dee4;padding:7px 11px;text-align:left;vertical-align:top}th{background:#f6f8fa}
h1{border-bottom:1px solid #d8dee4;padding-bottom:10px}h2{border-bottom:1px solid #eaecef;padding-bottom:6px;margin-top:34px}
code{background:#f6f8fa;padding:2px 6px;border-radius:4px;font-size:13px}pre{background:#f6f8fa;padding:14px;border-radius:8px;overflow:auto}
pre code{background:none;padding:0}blockquote{margin:0;padding:6px 14px;color:#57606a;border-left:4px solid #d8dee4}
hr{border:0;border-top:1px solid #d8dee4;margin:26px 0}</style>"""

if __name__ == "__main__":
    src, dst = sys.argv[1], sys.argv[2]
    md = io.open(src, encoding="utf-8").read()
    io.open(dst, "w", encoding="utf-8").write("<!doctype html><meta charset=utf-8>" + CSS + render(md))
    print("ok")
