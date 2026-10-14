# Brightens enemy frames that came out too dark: lifts the shadows towards a cool tint, leaves
# glowing areas untouched, then adds a cool rim light just inside the silhouette.
# Usage: python brighten.py <gamma> <rim_strength> <out_dir> <frame.png> [<frame.png> ...]
#   gamma: below 1, the smaller the brighter, e.g. 0.65
#   rim_strength: rim light strength from 0 to 1, e.g. 0.45
import colorsys
import os
import sys
from PIL import Image, ImageFilter

STEEL_HUE = 0.6          # cool hue to tint towards (blue-grey)
STEEL_MIN_SAT = 0.15     # minimum cool tint carried by grey areas
RIM_COLOR = (150, 185, 215)
RIM_WIDTH = 5            # rim light width, in pixels on a 512 frame


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

    # The band just inside the silhouette: the original alpha minus the eroded alpha.
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
