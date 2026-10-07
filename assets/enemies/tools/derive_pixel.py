# Derives idle_1 / hurt_0 / death_0 from a pixelated idle_0. Every change moves by whole cells,
# so the pixel grain stays intact.
# Usage: python derive_pixel.py <pixel_size> <frames_dir> <enemy_id> [--floating]
import os
import sys
from PIL import Image, ImageEnhance, ImageOps

FRAME = 512
BREATH_SHARE = 0.7       # grounded enemies: this share of the body drops one cell in idle_1
FLOAT_LIFT = 2           # floating enemies: cells the whole sprite rises by in idle_1
DEATH_SQUASH = 0.75      # death frame is squashed to this fraction of its original height
HURT_TINT = (255, 70, 60)
GROUND_Y = 478           # ground line, in pixel rows on a 512 frame, matching tomb_guardian


def _to_grid(frame, size):
    return frame.resize((FRAME // size, FRAME // size), Image.NEAREST)


def _to_frame(grid, size):
    return grid.resize((grid.width * size, grid.height * size), Image.NEAREST)


def _breathe(grid, floating):
    out = Image.new("RGBA", grid.size, (0, 0, 0, 0))
    if floating:
        out.paste(grid, (0, -FLOAT_LIFT))
        return out
    left, top, right, bottom = grid.getbbox()
    cut = top + round((bottom - top) * BREATH_SHARE)
    out.paste(grid.crop((0, cut, grid.width, grid.height)), (0, cut))       # lower body stays put, feet on the ground
    out.alpha_composite(grid.crop((0, 0, grid.width, cut)), (0, 1))          # upper body drops one cell
    return out


def _hurt(grid):
    alpha = grid.getchannel("A")
    gray = ImageOps.grayscale(grid.convert("RGB"))
    red = ImageOps.colorize(gray, (0, 0, 0), HURT_TINT)
    tinted = Image.blend(grid.convert("RGB"), red, 0.75).convert("RGBA")
    tinted.putalpha(alpha)
    out = Image.new("RGBA", grid.size, (0, 0, 0, 0))
    out.paste(tinted, (1, 0))                                                # recoils one cell to the right, away from the player
    return out


def _death(grid, floating, size):
    alpha = grid.getchannel("A")
    gray = ImageEnhance.Brightness(ImageOps.grayscale(grid.convert("RGB")).convert("RGB")).enhance(0.8)
    gray = gray.convert("RGBA")
    gray.putalpha(alpha.point(lambda v: 0 if v == 0 else 215))
    left, top, right, bottom = gray.getbbox()
    body = gray.crop((left, top, right, bottom))
    body = body.resize((body.width, max(1, round(body.height * DEATH_SQUASH))), Image.NEAREST)
    # Grounded enemies collapse to their original lowest point; floating ones drop to the ground line.
    floor = round(GROUND_Y / size) if floating else bottom
    out = Image.new("RGBA", grid.size, (0, 0, 0, 0))
    out.paste(body, (left, floor - body.height))
    return out


def derive(size, frames_dir, enemy, floating):
    grid = _to_grid(Image.open(os.path.join(frames_dir, f"{enemy}_idle_0.png")).convert("RGBA"), size)
    derived = (
        ("idle_1", _breathe(grid, floating)),
        ("hurt_0", _hurt(grid)),
        ("death_0", _death(grid, floating, size)),
    )
    for name, img in derived:
        _to_frame(img, size).save(os.path.join(frames_dir, f"{enemy}_{name}.png"))
        print("derived", f"{enemy}_{name}.png")


if __name__ == "__main__":
    derive(int(sys.argv[1]), sys.argv[2], sys.argv[3], "--floating" in sys.argv[4:])
