
import os
from PIL import Image, ImageDraw

base = r"D:\Documents\deepseek-harness\default-workspace\cooking-timer\app\src\main\res"
sizes = {"mipmap-mdpi":48,"mipmap-hdpi":72,"mipmap-xhdpi":96,"mipmap-xxhdpi":144,"mipmap-xxxhdpi":192}
def draw_icon(size):
    s = size*4
    img = Image.new("RGBA", (s, s), (253, 243, 234, 255))
    d = ImageDraw.Draw(img)
    # bowl
    d.pieslice([s*0.17, s*0.27, s*0.83, s*0.97], 0, 180, fill=(201,111,74,255))
    d.rectangle([s*0.17, s*0.44, s*0.83, s*0.62], fill=(201,111,74,255))
    # base
    d.rounded_rectangle([s*0.38, s*0.80, s*0.62, s*0.88], radius=s*0.03, fill=(169,86,58,255))
    # steam
    d.line([(s*0.44,s*0.30),(s*0.47,s*0.22),(s*0.44,s*0.14)], fill=(126,155,128,255), width=int(s*0.035), joint="curve")
    d.line([(s*0.58,s*0.30),(s*0.61,s*0.22),(s*0.58,s*0.14)], fill=(126,155,128,255), width=int(s*0.035), joint="curve")
    return img.resize((size, size), Image.LANCZOS)

for folder, size in sizes.items():
    p = os.path.join(base, folder)
    os.makedirs(p, exist_ok=True)
    ic = draw_icon(size)
    ic.save(os.path.join(p, "ic_launcher.png"))
    # round: 圆形遮罩
    mask = Image.new("L", (size*4, size*4), 0)
    ImageDraw.Draw(mask).ellipse([0,0,size*4-1,size*4-1], fill=255)
    round_ic = draw_icon(size).resize((size*4,size*4), Image.LANCZOS)
    round_ic.putalpha(mask.resize((size*4,size*4), Image.LANCZOS))
    round_ic.resize((size,size), Image.LANCZOS).save(os.path.join(p, "ic_launcher_round.png"))
print("icons ok")
