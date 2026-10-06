# Pixelates 512x512 enemy frames: downscale -> shared palette -> hard-edged alpha -> inner
# outline -> nearest-neighbour upscale.
# Usage: python pixelate.py <pixel_size> <colors> <out_dir> <frame.png> [<frame.png> ...]
# Pass every frame of the same enemy together, so they share one palette.
import os
import sys
from PIL import Image

ALPHA_CUT = 110          # pixels below this alpha are dropped outright
OUTLINE_DARKEN = 0.35    # inner outline: pixels just inside the silhouette darken to this fraction
ACCENT_SHARE = 0.15      # above this share of bright/vivid pixels in a cell, their colour is kept
GLOW_MIN_ALPHA = 60      # semi-transparent glow pixels need at least this alpha to count
VIVID_MIN = 2            # this many vivid pixels in a cell is enough to keep the vivid colour


def _shrink(frame, size):
    n = round(FRAME / size)
    # Average after premultiplying alpha, so colour from transparent areas doesn't bleed into edges.
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
            # When a cell has enough glowing or vivid pixels, use their average colour: small
            # highlights such as eyes and orbs are not washed out by the darker surroundings, and
            # semi-transparent glow (beams, blade trails) becomes solid bright pixels instead of
            # being cut away by the alpha threshold.
            accents = [
                src[sx, sy][:3]
                for sy in range(y * size, min((y + 1) * size, FRAME))
                for sx in range(x * size, min((x + 1) * size, FRAME))
                if _is_glow_pixel(src[sx, sy])
            ]
            vivid = [c for c in accents if _is_vivid(c)]
              # A few vivid pixels are enough to keep them, and only vivid pixels are averaged, so
              # white highlights don't dilute them — eyes are often just two or three pixels.
            keep = vivid if len(vivid) >= VIVID_MIN else accents
            if len(vivid) >= VIVID_MIN or len(accents) >= size * size * ACCENT_SHARE:
              # Only the brightest half: the core of a small highlight such as an eye is not
              # dragged down by the dimmer glow around it.
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
    # One palette shared by every frame. Vivid colours (eyes, orbs) and white highlights each get
    # their own reserved slots, so they neither crowd each other out nor get swallowed by the
    # large areas of body colour.
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
    # Each hue (green, orange, red, cyan, ...) is guaranteed at least one slot, and the rest are
    # shared by area, so a small but distinctive detail such as green eyes is not swallowed by a
    # large area of a similar hue such as amber runes.
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
    # Vivid colours: red eyes, amber orbs, cyan light.
    hi, lo = max(rgb), min(rgb)
    return hi > 140 and (hi - lo) / hi > 0.55


def _is_hot_core(rgb):
    # Glowing cores: very bright and clearly tinted, such as the pale yellow-white centre of an
    # eye, rather than plain grey-white.
    hi, lo = max(rgb), min(rgb)
    return hi > 230 and hi - lo > 60


def _is_accent(rgb):
    # Vivid colours, glowing cores, or near-white highlights.
    return _is_vivid(rgb) or _is_hot_core(rgb) or min(rgb) > 200


def _is_glow_pixel(rgba):
    r, g, b, a = rgba
    if a < GLOW_MIN_ALPHA:
        return False
    if _is_accent((r, g, b)):
        return True
    # Semi-transparent but bright pixels: beams, blade trails and glowing tails.
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
            # Transparent pixels get no outline, and neither do glowing bright ones — otherwise a
            # thin light effect would be darkened along its whole length.
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
