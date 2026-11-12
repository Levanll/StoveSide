# -*- coding: utf-8 -*-
"""生成 README 首图（hero banner）：灶暖 StoveSide，奶油底 + 陶土橙，与应用内视觉一致。"""
import sys
from PIL import Image, ImageDraw, ImageFont

CREAM = (253, 248, 243)
CARD = (255, 253, 250)
CLAY = (201, 111, 74)
CLAY_DEEP = (156, 79, 47)
CLAY_SOFT = (247, 226, 214)
SAGE = (126, 155, 128)
SAGE_SOFT = (228, 237, 227)
INK = (61, 50, 41)
INK_SOFT = (122, 106, 93)
LINE = (232, 220, 209)
W, H = 1280, 420

F_CN = r"C:\Windows\Fonts\msyh.ttc"
F_CN_B = r"C:\Windows\Fonts\msyhbd.ttc"
F_EN = r"C:\Windows\Fonts\arialbd.ttf"

def fnt(path, size):
    return ImageFont.truetype(path, size)

def rounded(d, box, r, fill=None, outline=None, width=1):
    d.rounded_rectangle(box, radius=r, fill=fill, outline=outline, width=width)

def build(lang):
    img = Image.new("RGB", (W, H), CREAM)
    d = ImageDraw.Draw(img)
    # 淡点阵，呼应应用里的克制留白
    for y in range(0, H, 34):
        for x in range(0, W, 34):
            d.ellipse([x, y, x + 2, y + 2], fill=(245, 236, 227))

    x0 = 72
    title = "STOVESIDE"
    d.text((x0, 78), title, font=fnt(F_EN, 66), fill=INK)
    # 标题下划线：主色短横
    d.rounded_rectangle([x0 + 2, 168, x0 + 190, 178], radius=5, fill=CLAY)

    if lang == "zh":
        sub = "灶暖 · 多流程并行烹饪计时"
        feats = "多锅同开  ·  每步留备注  ·  同菜多份各记各的"
        pills = ["并行计时", "步骤备注", "多份追踪", "中 / EN 双语"]
        foot = "纯本地运行 · 零账号 · 数据只在本机"
    else:
        sub = "Multi-timer cooking companion for Android"
        feats = "Many pots at once  ·  Per-step notes  ·  Every batch tracked"
        pills = ["Multi-timer", "Step notes", "Batch tracking", "ZH / EN"]
        foot = "Runs fully offline · No account · Data stays on device"

    d.text((x0, 196), sub, font=fnt(F_CN_B, 30), fill=CLAY_DEEP)
    d.text((x0, 242), feats, font=fnt(F_CN, 21), fill=INK_SOFT)

    px = x0
    pf = fnt(F_CN, 19)
    for label in pills:
        w = d.textlength(label, font=pf)
        rounded(d, [px, 288, px + w + 34, 330], 21, fill=None, outline=CLAY, width=2)
        d.text((px + 17, 298), label, font=pf, fill=CLAY_DEEP)
        px += w + 34 + 12

    d.text((x0, 356), foot, font=fnt(F_CN, 18), fill=INK_SOFT)

    # 右侧：一张与应用同款的小卡片，示意"三锅并行"
    cx0, cy0, cx1, cy1 = 786, 66, 1214, 354
    d.rounded_rectangle([cx0 + 4, cy0 + 6, cx1 + 4, cy1 + 6], radius=26, fill=(242, 232, 223))
    rounded(d, [cx0, cy0, cx1, cy1], 26, fill=CARD, outline=LINE, width=1)

    if lang == "zh":
        rows = [
            ("红烧肉 #1", "小火慢炒，别糊了", "04:12", CLAY, CLAY_SOFT),
            ("番茄牛腩 #1", "这次水少放一点", "17:45", SAGE, SAGE_SOFT),
            ("银耳羹 #2", "提前两小时泡发", "38:20", (168, 138, 118), (240, 231, 223)),
        ]
    else:
        rows = [
            ("Braised pork #1", "low heat, don't burn it", "04:12", CLAY, CLAY_SOFT),
            ("Tomato beef #1", "less water this time", "17:45", SAGE, SAGE_SOFT),
            ("Tremella soup #2", "soak 2h in advance", "38:20", (168, 138, 118), (240, 231, 223)),
        ]
    ry = cy0 + 30
    for name, note, time_s, dot, chip in rows:
        d.ellipse([cx0 + 26, ry + 4, cx0 + 54, ry + 32], fill=chip)
        d.ellipse([cx0 + 34, ry + 12, cx0 + 46, ry + 24], fill=dot)
        d.text((cx0 + 68, ry + 4), name, font=fnt(F_CN_B, 21), fill=INK)
        d.text((cx0 + 68, ry + 34), note, font=fnt(F_CN, 15), fill=INK_SOFT)
        tw = d.textlength(time_s, font=fnt(F_EN, 24))
        d.text((cx1 - 26 - tw, ry + 8), time_s, font=fnt(F_EN, 24), fill=CLAY)
        ry += 78
        if ry < cy1 - 40:
            d.line([cx0 + 26, ry - 16, cx1 - 26, ry - 16], fill=(240, 232, 224), width=1)

    d.text((cx1 - 96, cy1 - 40), "v1.0.2", font=fnt(F_EN, 17), fill=(196, 182, 170))
    return img

if __name__ == "__main__":
    out_dir = sys.argv[1] if len(sys.argv) > 1 else "docs/images"
    build("zh").save(out_dir + "/hero-banner-zh.png")
    build("en").save(out_dir + "/hero-banner-en.png")
    print("banner ok")