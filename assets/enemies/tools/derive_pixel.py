# 从像素化后的 idle_0 派生 idle_1 / hurt_0 / death_0，所有变化都按整格进行，不破坏像素颗粒
# 用法: python derive_pixel.py <pixel_size> <frames_dir> <enemy_id> [--floating]
import os
import sys
from PIL import Image, ImageEnhance, ImageOps

FRAME = 512
BREATH_SHARE = 0.7       # 站立的怪：上面这个比例的身体在 idle_1 里下压 1 格
FLOAT_LIFT = 2           # 悬浮的怪：idle_1 整体上浮的格数
DEATH_SQUASH = 0.75      # 死亡帧纵向压扁到原高度的这个比例
HURT_TINT = (255, 70, 60)
GROUND_Y = 478           # 地面线（512 帧上的像素行），和 tomb_guardian 一致


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
    out.paste(grid.crop((0, cut, grid.width, grid.height)), (0, cut))       # 下半身不动，脚不离地
    out.alpha_composite(grid.crop((0, 0, grid.width, cut)), (0, 1))          # 上半身下压 1 格
    return out


def _hurt(grid):
    alpha = grid.getchannel("A")
    gray = ImageOps.grayscale(grid.convert("RGB"))
    red = ImageOps.colorize(gray, (0, 0, 0), HURT_TINT)
    tinted = Image.blend(grid.convert("RGB"), red, 0.75).convert("RGBA")
    tinted.putalpha(alpha)
    out = Image.new("RGBA", grid.size, (0, 0, 0, 0))
    out.paste(tinted, (1, 0))                                                # 往右（远离玩家）后仰 1 格
    return out


def _death(grid, floating, size):
    alpha = grid.getchannel("A")
    gray = ImageEnhance.Brightness(ImageOps.grayscale(grid.convert("RGB")).convert("RGB")).enhance(0.8)
    gray = gray.convert("RGBA")
    gray.putalpha(alpha.point(lambda v: 0 if v == 0 else 215))
    left, top, right, bottom = gray.getbbox()
    body = gray.crop((left, top, right, bottom))
    body = body.resize((body.width, max(1, round(body.height * DEATH_SQUASH))), Image.NEAREST)
    # 站立的怪塌到原来的最低点；悬浮的怪落到地面线上
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
