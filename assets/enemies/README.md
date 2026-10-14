# Enemy art working files (Sprint 3, issue #279)

Raw material behind the restyled enemy sprites. Nothing here is loaded by the game; the packed
sheets live in `source/core/assets/images/enemies/`.

The sheets in the game are the **pixel-art** version (`frames_pixel/`). The smooth version (`frames/`)
is kept as the source the pixel art is generated from.

## Layout

| Path | Contents |
|---|---|
| `<enemy>/source/` | Original generated images (`idle`, `attack`) and, where kept, a full-resolution transparent cutout; transparent `defend`, `cast` and `idle_taunt` poses for #438 |
| `<enemy>/frames/` | Smooth 512×512 frames: `idle_0`, `idle_1`, `attack_0`, `hurt_0`, `death_0`, and the new poses `defend_0`, `cast_0`, `idle_taunt_0` |
| `<enemy>/frames_pixel/` | Pixel-art 512×512 frames used in game (4 px grid) |
| `intents/` | Smooth source files for the status effect intent icons (128×128); the game uses pixel-art versions |
| `enemy_descriptions.json` | Bestiary backstory copy, agreed with Team 6 against the *Sprint 3 Story Outline* |
| `tools/` | Scripts used to produce the frames and sheets |

## How the frames were made

Follows the wiki page *Enemy Art Asset Conventions*.

Smooth frames (`frames/`):

- `idle_0` and `attack_0` come from the source images: background, watermark and floor shadow removed,
  scaled, and placed so the feet sit on row 478 of the frame (the same ground line as `tomb_guardian`).
  Floating enemies sit above that line.
- `void_knight` was brightened with `tools/Brighten.java` (gamma 0.5, rim 0.55) so it reads against the
  dark dungeon background.
- `tomb_guardian` was redrawn (ivory stone armour, bronze trim, rune shield). Its source images have a
  light grey background and a floor shadow, so `tools/Cutout.java` was run with `--grey-sat` (drops grey
  shadow and haze) and, for the idle image only, `--floor-y` (treats darker grey below the feet as
  background while stopping at the boots' outline).

Pixel-art frames (`frames_pixel/`):

- `idle_0` and `attack_0` are pixelated from the smooth frames with `tools/Pixelate.java` on a 4 px grid
  with a 32-colour palette shared by both frames. Small bright details (eyes, orbs, glows) keep their
  colour, and a dark outline is drawn on the inside of the silhouette.
- `idle_1`, `hurt_0` and `death_0` are derived from the pixel `idle_0` with `tools/DerivePixel.java`,
  moving whole grid cells so the pixel grid is never broken:
  - `idle_1`: upper body pressed down one cell (standing) or lifted two cells (floating)
  - `hurt_0`: tinted red and shifted one cell away from the player
  - `death_0`: desaturated, darkened and squashed onto the ground line

Action and form frames (issue #438):

- `defend_0` (Lesser Shade, Bone Crawler, Dark Acolyte, Tomb Guardian), `cast_0` (Tomb Guardian's taunt)
  and the Tomb Guardian taunting form `idle_taunt_0` come from the transparent poses in `source/`. Each
  is scaled and placed into a 512 frame with `tools/PlaceFrame.java` so it lines up with `idle_0`; the
  Tomb Guardian poses are scaled so the eye-to-feet distance matches `idle_0` and the eyes sit at the
  same x.
- They are pixelated with `Pixelate.java --palette`, reusing the colours of that enemy's existing
  `idle_0`/`attack_0` pixel frames, so the tones match and the existing frames are not regenerated.
  The Lesser Shade bubble and the Dark Acolyte ward also use `--soft-alpha`, which keeps their
  translucency in a few alpha steps.
- `idle_taunt_1` and `hurt_taunt_0` are derived from `idle_taunt_0` with `DerivePixel.java --form taunt`.
- The Riven Knight (`void_knight`) enraged form needs no new art: `tools/Recolor.java` turns the cyan
  glow of the armour cracks, visor and sword (hue 175–196°, value ≥ 0.6, saturation ≥ 0.45, plus the
  near-white cores) red in the existing pixel `idle_0` and `attack_0`, giving `idle_enraged_0` and
  `attack_enraged_0`; the dark steel armour is left alone. `idle_enraged_1` and `hurt_enraged_0` come from
  `DerivePixel.java --form enraged`.
- Region names follow the convention in issue #436: `defend`, `cast`, `idle_taunt` (×2), `hurt_taunt`,
  `idle_enraged` (×2), `attack_enraged`, `hurt_enraged`.
  Non-boss `defend`/`cast` are a single held pose; `EnemyFactory` holds it for 0.8 s.

| Frame | `PlaceFrame.java` arguments (height, x, y) |
|---|---|
| `lesser_shade_defend_0` | `330 256 320 --center` |
| `bone_crawler_defend_0` | `200 256 478` |
| `dark_acolyte_defend_0` | `480 267 510` (the ward lifts the feet about 20 px off the ground) |
| `tomb_guardian_defend_0` | `459 247.9 479` |
| `tomb_guardian_cast_0` | `476 245.1 479` (2.5% smaller so the raised spear fits the frame) |
| `tomb_guardian_idle_taunt_0` | `471 257.6 479` |

## Rebuilding

The tools are plain Java that only use the JDK, so each one runs as a single source file with
`java <Tool>.java` (JDK 11+, no compile step). Run from `assets/enemies/`.

```bash
java tools/Pixelate.java 4 32 <enemy_id>/frames_pixel <enemy_id>/frames/<enemy_id>_idle_0.png <enemy_id>/frames/<enemy_id>_attack_0.png
java tools/DerivePixel.java 4 <enemy_id>/frames_pixel <enemy_id>
java tools/PackEnemy.java <enemy_id> <enemy_id>/frames_pixel ../../source/core/assets/images/enemies --nearest
```

Add `--floating` to `DerivePixel.java` for enemies that hover (`lesser_shade`).

New action or form poses for an existing enemy (example: Tomb Guardian):

```bash
java tools/PlaceFrame.java tomb_guardian/source/defend.png tomb_guardian/frames/tomb_guardian_defend_0.png 459 247.9 479
java tools/Pixelate.java --palette tomb_guardian/frames_pixel/tomb_guardian_idle_0.png,tomb_guardian/frames_pixel/tomb_guardian_attack_0.png 4 32 tomb_guardian/frames_pixel tomb_guardian/frames/tomb_guardian_defend_0.png tomb_guardian/frames/tomb_guardian_cast_0.png tomb_guardian/frames/tomb_guardian_idle_taunt_0.png
java tools/DerivePixel.java 4 tomb_guardian/frames_pixel tomb_guardian --form taunt
java tools/PackEnemy.java tomb_guardian tomb_guardian/frames_pixel ../../source/core/assets/images/enemies --nearest
```

Riven Knight enraged form:

```bash
java tools/Recolor.java 175 196 0.45 0.6 180 1.35 void_knight/frames_pixel/void_knight_idle_0.png void_knight/frames_pixel/void_knight_idle_enraged_0.png void_knight/frames_pixel/void_knight_attack_0.png void_knight/frames_pixel/void_knight_attack_enraged_0.png
java tools/DerivePixel.java 4 void_knight/frames_pixel void_knight --form enraged
java tools/PackEnemy.java void_knight void_knight/frames_pixel ../../source/core/assets/images/enemies --nearest
```

The tools were ported from earlier Python/Pillow scripts and give pixel-identical output to them. Only
`tomb_guardian`'s committed `idle_0`/`attack_0` match what `Pixelate.java` makes from the current smooth
frames; for `bone_crawler`, `dark_acolyte`, `lesser_shade` and `void_knight`, re-running it changes
roughly 100–500 cells per frame, so check the result before replacing those frames.

Other tools:

- `java tools/Brighten.java <gamma> <rim_strength> <out_dir> <frame.png>...` lightens dark frames.
- `java tools/Cutout.java <in.png> <out.png> [--grey-sat F] [--floor-y N] [--watermark x0,y0,x1,y1|none]`
  removes the white or light grey background from a generated image. The JDK cannot read `.webp`, so
  save the source as PNG first.
- `java tools/Pixelate.java --flatten <pixel_size> <colors> <out_dir> <frame.png>...` merges small
  colour noise in already pixelated frames into flat colour blocks (experimental, not used in game).

`PackEnemy.java` collects every `<enemy_id>_<animation>_<index>.png` in the frames folder and writes
`<enemy_id>.png` and `<enemy_id>.atlas`. `idle` (×2), `attack`, `hurt` and `death` always take the first
five slots, so `default` (= `idle_0`, read by the bestiary) and the existing regions keep their positions;
further animations follow, four per row. The sheet is 2048×1024, or 2048×2048 when there are more than
eight frames. `--nearest` sets the atlas filter to `Nearest` so pixels stay sharp when the game scales the
sprite. Region names must not change: `EnemyAnimationController` looks animations up by name and fails
silently on a mismatch.

Status effect icons (`silence`, `taunt`, `damage_on_card_play`) are pixelated straight into the game
folder with `tools/PixelateIcon.java`: 4 px grid (32×32 cells), a 5-colour palette per icon and a
uniform one-cell white outline. White separators between shapes inside an icon are kept. It only uses
the JDK, so it runs as a single source file (JDK 11+):

```bash
java tools/PixelateIcon.java 4 5 ../../source/core/assets/images/enemies/intents intents/silence.png intents/taunt.png intents/damage_on_card_play.png
```

`tools/AtlasCheck.java` parses an atlas with libGDX's own `TextureAtlasData` to confirm every region
is found. Compile and run it against the `gdx` jar from the Gradle cache.
