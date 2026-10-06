# 提亮过暗的敌人帧：暗部提亮并偏冷色，发光部分保持不变，再沿轮廓内侧加一圈冷色边缘光
# 用法: python brighten.py <gamma> <rim_strength> <out_dir> <frame.png> [<frame.png> ...]
#   gamma: 小于 1 越小越亮，比如 0.65；rim_strength: 边缘光强度 0~1，比如 0.45
import colorsys
import os
import sys
from PIL import Image, ImageFilter

STEEL_HUE = 0.6          # 偏向的冷色色相（蓝灰）
STEEL_MIN_SAT = 0.15     # 灰色部分至少带这么多冷色
RIM_COLOR = (150, 185, 215)
RIM_WIDTH = 5            # 边缘光宽度（512 帧上的像素）


def _is_glow(r, g, b):
    hi, lo = max(r, g, b), min(r, g, b)
    return hi > 200 or (hi > 140 and (hi - lo) / hi > 0.55)


def brighten(frame, gamma, rim_strength):
    out = frame.copy()
    px = out.load()
    w, h = out.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0 or _is_glow(r, g, b):
                continue
            hh, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            v = v ** gamma
            if s < 0.25:
                hh, s = STEEL_HUE, max(s, STEEL_MIN_SAT)
            nr, ng, nb = colorsys.hsv_to_rgb(hh, s, v)
            px[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), a)

    # 轮廓内侧的一圈：原 alpha 减去收缩后的 alpha
    alpha = out.getchannel("A").point(lambda v: 255 if v > 128 else 0)
    inner = alpha.filter(ImageFilter.MinFilter(RIM_WIDTH))
    rim = alpha.load(), inner.load()
    for y in range(h):
        for x in range(w):
            if rim[0][x, y] and not rim[1][x, y]:
                r, g, b, a = px[x, y]
                if _is_glow(r, g, b):
                    continue
                k = rim_strength
                px[x, y] = (
                    round(r * (1 - k) + RIM_COLOR[0] * k),
                    round(g * (1 - k) + RIM_COLOR[1] * k),
                    round(b * (1 - k) + RIM_COLOR[2] * k),
                    a,
                )
    return out


if __name__ == "__main__":
    gamma, rim, out_dir = float(sys.argv[1]), float(sys.argv[2]), sys.argv[3]
    os.makedirs(out_dir, exist_ok=True)
    for path in sys.argv[4:]:
        brighten(Image.open(path).convert("RGBA"), gamma, rim).save(os.path.join(out_dir, os.path.basename(path)))
        print("brightened", os.path.basename(path))
