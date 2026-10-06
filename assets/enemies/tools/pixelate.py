# 把 512x512 的敌人帧像素化：缩小 -> 共用调色板 -> 硬边透明 -> 内描边 -> 最近邻放大
# 用法: python pixelate.py <pixel_size> <colors> <out_dir> <frame.png> [<frame.png> ...]
# 同一只怪的所有帧要一起传入，这样它们共用一套调色板
import os
import sys
from PIL import Image

FRAME = 512
ALPHA_CUT = 110          # 低于这个透明度的像素直接去掉
OUTLINE_DARKEN = 0.35    # 内描边：轮廓内侧像素压暗到原亮度的这个比例
ACCENT_SHARE = 0.15      # 格子里高亮/鲜艳像素占比超过这个值，就保留它们的颜色
GLOW_MIN_ALPHA = 60      # 半透明光效像素至少要有这个不透明度才算数
VIVID_MIN = 2            # 格子里至少有这么多鲜艳像素，就保留鲜艳色


def _shrink(frame, size):
    n = round(FRAME / size)
    # 预乘透明度后再平均，避免透明区域的颜色渗进边缘
    premul = Image.new("RGBA", frame.size)
    premul.paste(frame, mask=frame.getchannel("A"))
    small = frame.resize((n, n), Image.BOX)
    color = premul.convert("RGB").resize((n, n), Image.BOX)
    alpha = frame.getchannel("A").resize((n, n), Image.BOX)
    px, cx, ax = small.load(), color.load(), alpha.load()
    src = frame.load()
    for y in range(n):
        for x in range(n):
            a = ax[x, y]
            # 格子里发光/鲜艳的像素够多时，用它们的平均色：眼睛、宝珠这类小亮点不会被周围暗色抹掉，
            # 半透明的光效（光束、刀光）也会变成实心亮色像素，而不是被透明度门槛直接切掉
            accents = [
                src[sx, sy][:3]
                for sy in range(y * size, min((y + 1) * size, FRAME))
                for sx in range(x * size, min((x + 1) * size, FRAME))
                if _is_glow_pixel(src[sx, sy])
            ]
            vivid = [c for c in accents if _is_vivid(c)]
            # 鲜艳像素只要有几个就保留，且只取鲜艳像素的平均色，避免被白色高光冲淡（眼睛往往只有两三个像素）
            keep = vivid if len(vivid) >= VIVID_MIN else accents
            if len(vivid) >= VIVID_MIN or len(accents) >= size * size * ACCENT_SHARE:
                # 只取最亮的一半：小光点（眼睛）的亮芯不会被外圈较暗的光晕拉暗
                keep = sorted(keep + [c for c in accents if _is_hot_core(c)], key=max, reverse=True)
                keep = keep[: max(1, len(keep) // 2)]
                px[x, y] = tuple(sum(c[i] for c in keep) // len(keep) for i in range(3)) + (255,)
                continue
            if a < ALPHA_CUT:
                px[x, y] = (0, 0, 0, 0)
                continue
            r, g, b = cx[x, y]
            k = 255 / a
            px[x, y] = (min(255, int(r * k)), min(255, int(g * k)), min(255, int(b * k)), 255)
    return small


def _median_cut(pixels, colors):
    strip = Image.new("RGB", (len(pixels), 1))
    strip.putdata(pixels)
    pal = strip.quantize(colors=colors, method=Image.Quantize.MEDIANCUT).getpalette()
    used = len(set(pixels))
    return [tuple(pal[i * 3:i * 3 + 3]) for i in range(min(colors, used))]


def _shared_palette(smalls, colors, vivid_slots=6, glow_slots=3):
    # 所有帧共用一套调色板。鲜艳色（眼睛、宝珠）和白色高光分别单独留名额，
    # 互不挤占，也不会被大面积的主体颜色吞掉
    pixels = [p[:3] for s in smalls for p in s.getdata() if p[3] == 255]
    vivid = [p for p in pixels if _is_vivid(p)]
    glow = [p for p in pixels if not _is_vivid(p) and _is_accent(p)]
    base = [p for p in pixels if not _is_accent(p)] or pixels
    reserved = (vivid_slots if vivid else 0) + (glow_slots if glow else 0)
    entries = _median_cut(base, colors - reserved)
    if vivid:
        entries += _vivid_entries(vivid, vivid_slots)
    if glow:
        entries += _median_cut(glow, glow_slots)
    flat = [c for rgb in entries for c in rgb]
    palette = Image.new("P", (1, 1))
    palette.putpalette(flat + flat[:3] * (256 - len(entries)))
    return palette


def _vivid_entries(vivid, slots, hue_bins=12, min_pixels=3):
    # 每种色相（绿、橙、红、青……）先保证至少一个名额，剩下的再按面积分，
    # 这样少量但颜色独特的细节（比如绿眼睛）不会被大面积的同类色（比如琥珀色符文）吞掉
    import colorsys

    bins = {}
    for p in vivid:
        h = colorsys.rgb_to_hsv(*[c / 255 for c in p])[0]
        bins.setdefault(int(h * hue_bins) % hue_bins, []).append(p)
    groups = sorted((g for g in bins.values() if len(g) >= min_pixels), key=len, reverse=True)
    entries = [tuple(sum(c[i] for c in g) // len(g) for i in range(3)) for g in groups[:slots]]
    if len(entries) < slots:
        entries += _median_cut(vivid, slots - len(entries))
    return entries


def _is_vivid(rgb):
    # 鲜艳色：红眼睛、琥珀色宝珠、青色光
    hi, lo = max(rgb), min(rgb)
    return hi > 140 and (hi - lo) / hi > 0.55


def _is_hot_core(rgb):
    # 发光的亮芯：很亮，而且带明显色彩（比如眼睛中心的淡黄白色），不是灰白
    hi, lo = max(rgb), min(rgb)
    return hi > 230 and hi - lo > 60


def _is_accent(rgb):
    # 鲜艳色、发光亮芯，或接近白色的高光
    return _is_vivid(rgb) or _is_hot_core(rgb) or min(rgb) > 200


def _is_glow_pixel(rgba):
    r, g, b, a = rgba
    if a < GLOW_MIN_ALPHA:
        return False
    if _is_accent((r, g, b)):
        return True
    # 半透明且明亮的像素：光束、刀光、拖尾的发光部分
    return a < 255 and max(r, g, b) > 200


def _saturate(img, amount):
    from PIL import ImageEnhance

    alpha = img.getchannel("A")
    out = ImageEnhance.Color(img.convert("RGB")).enhance(amount).convert("RGBA")
    out.putalpha(alpha)
    return out


def _apply_palette(small, palette):
    rgb = small.convert("RGB").quantize(palette=palette, dither=Image.Dither.NONE).convert("RGB")
    out = rgb.convert("RGBA")
    out.putalpha(small.getchannel("A"))
    return out


def _inner_outline(img):
    w, h = img.size
    src = img.load()
    out = img.copy()
    dst = out.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = src[x, y]
            # 透明像素不描；发光的亮色像素也不描，否则细光效会整条被压暗
            if a == 0 or _is_accent((r, g, b)) or max(r, g, b) > 200:
                continue
            edge = any(
                not (0 <= nx < w and 0 <= ny < h) or src[nx, ny][3] == 0
                for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1))
            )
            if edge:
                dst[x, y] = (int(r * OUTLINE_DARKEN), int(g * OUTLINE_DARKEN), int(b * OUTLINE_DARKEN), 255)
    return out


def pixelate(paths, size, colors):
    frames = [Image.open(p).convert("RGBA") for p in paths]
    smalls = [_saturate(_shrink(f, size), 1.2) for f in frames]
    palette = _shared_palette(smalls, colors)
    results = []
    for small in smalls:
        img = _inner_outline(_apply_palette(small, palette))
        big = img.resize((img.width * size, img.height * size), Image.NEAREST)
        canvas = Image.new("RGBA", (FRAME, FRAME), (0, 0, 0, 0))
        off = (FRAME - big.width) // 2
        canvas.alpha_composite(big.crop((0, 0, min(big.width, FRAME), min(big.height, FRAME))), (max(0, off), max(0, off)))
        results.append(canvas)
    return results


if __name__ == "__main__":
    size, colors, out_dir = int(sys.argv[1]), int(sys.argv[2]), sys.argv[3]
    paths = sys.argv[4:]
    os.makedirs(out_dir, exist_ok=True)
    for path, img in zip(paths, pixelate(paths, size, colors)):
        img.save(os.path.join(out_dir, os.path.basename(path)))
        print("pixelated", os.path.basename(path))
