# 白底 AI 图抠图：去白底/水印/被包围的白色空隙，光晕做半透明"去白"并保持明亮
from collections import deque
import colorsys
from PIL import Image


def _flood(w, h, seeds, ok):
    reg = bytearray(w * h)
    seen = bytearray(w * h)
    q = deque(seeds)
    while q:
        x, y = q.popleft()
        i = y * w + x
        if seen[i]:
            continue
        seen[i] = 1
        if not ok(x, y):
            continue
        reg[i] = 1
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < w and 0 <= ny < h and not seen[ny * w + nx]:
                q.append((nx, ny))
    return reg


def cutout(path, watermark=(1262, 1440, 1536, 1536), min_gap=300, bg_tol=3, alpha_min=0.03):
    im = Image.open(path).convert("RGB")
    px = im.load()
    w, h = im.size
    # 用四周边缘像素的中位数当作背景色
    edge = sorted(px[x, y] for x in range(0, w, 8) for y in (0, h - 1)) + sorted(
        px[x, y] for y in range(0, h, 8) for x in (0, w - 1)
    )
    bg = tuple(sorted(p[i] for p in edge)[len(edge) // 2] for i in range(3))
    if watermark:
        im.paste(bg, watermark)
    sv = [[colorsys.rgb_to_hsv(*[c / 255 for c in px[x, y]])[1:] for x in range(w)] for y in range(h)]

    def light(x, y):
        s, v = sv[y][x]
        return v > 0.80 and s < 0.45

    border = [(x, 0) for x in range(w)] + [(x, h - 1) for x in range(w)]
    border += [(0, y) for y in range(h)] + [(w - 1, y) for y in range(h)]
    reg = _flood(w, h, border, light)

    # 被身体围住的大块背景色（与外部不连通）；只认和背景色几乎相同的像素，避免误删白色光芯
    def pure_white(x, y):
        return all(abs(c - b) <= bg_tol for c, b in zip(px[x, y], bg))

    done = bytearray(w * h)
    gap_seeds = []
    for y in range(0, h, 3):
        for x in range(0, w, 3):
            i = y * w + x
            if reg[i] or done[i] or not pure_white(x, y):
                continue
            comp = _flood(w, h, [(x, y)], lambda a, b: not reg[b * w + a] and pure_white(a, b))
            big = sum(comp) > min_gap
            for j in range(w * h):
                if comp[j]:
                    done[j] = 1
                    if big:
                        gap_seeds.append((j % w, j // w))
    # 从空隙往外扩展到相邻的浅色过渡像素，去掉空隙边缘的白色毛边
    if gap_seeds:
        grown = _flood(w, h, gap_seeds, lambda a, b: light(a, b) or pure_white(a, b))
        for j in range(w * h):
            if grown[j]:
                reg[j] = 1

    out = Image.new("RGBA", (w, h))
    o = out.load()
    for y in range(h):
        for x in range(w):
            r, g, b = px[x, y]
            if not reg[y * w + x]:
                o[x, y] = (r, g, b, 255)
                continue
            a = max(255 - r, 255 - g, 255 - b) / 255
            if a < alpha_min:
                o[x, y] = (0, 0, 0, 0)
                continue
            a = min(1.0, a * 1.3)
            c = [max(0, min(255, (ch - 255 * (1 - a)) / a)) for ch in (r, g, b)]
            m = max(c) or 1
            c = [round(ch * 255 / m) for ch in c]
            o[x, y] = (c[0], c[1], c[2], round(a * 255))
    return out
