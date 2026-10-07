# Packs <enemy>_{idle_0,idle_1,attack_0,hurt_0,death_0}.png into a 2048x1024 sheet and writes
# the matching .atlas.
# Usage: python pack_enemy.py <enemy_id> <frames_dir> <out_dir> [--nearest]
#   --nearest: use the Nearest filter for pixel-art sheets, so pixels stay sharp when the game
#              scales them
import sys
from PIL import Image

S = 512
LAYOUT = [  # (region name, index, frame file suffix, column, row)
    ("idle", 0, "idle_0", 0, 0),
    ("idle", 1, "idle_1", 1, 0),
    ("attack", 0, "attack_0", 2, 0),
    ("hurt", 0, "hurt_0", 3, 0),
    ("death", 0, "death_0", 0, 1),
]


def region(name, x, y, index):
    return (
        f"{name}\n  rotate: false\n  xy: {x}, {y}\n  size: {S}, {S}\n"
        f"  orig: {S}, {S}\n  offset: 0, 0\n  index: {index}\n"
    )


def main(enemy, frames_dir, out_dir, nearest=False):
    sheet = Image.new("RGBA", (2048, 1024), (0, 0, 0, 0))
    for _, _, suffix, cx, cy in LAYOUT:
        frame = Image.open(f"{frames_dir}/{enemy}_{suffix}.png").convert("RGBA")
        assert frame.size == (S, S), f"{suffix} is {frame.size}, expected {S}x{S}"
        sheet.alpha_composite(frame, (cx * S, cy * S))
    sheet.save(f"{out_dir}/{enemy}.png", optimize=True)

    filt = "Nearest,Nearest" if nearest else "Linear,Linear"
    text = f"{enemy}.png\nsize: 2048, 1024\nformat: RGBA8888\nfilter: {filt}\nrepeat: none\n"
    text += region("default", 0, 0, -1)
    for name, index, _, cx, cy in LAYOUT:
        text += region(name, cx * S, cy * S, index)
    with open(f"{out_dir}/{enemy}.atlas", "w", newline="\n") as f:
        f.write(text)
    print(f"packed {enemy}")


if __name__ == "__main__":
    main(*sys.argv[1:4], nearest="--nearest" in sys.argv[4:])
