package com.csse3200.game.components.chance;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.chance.DiceRoll;

/** Pixel-style two-dice display driven by an already resolved, authoritative roll. */
final class DiceRollDisplay {
  static final float ROLL_DURATION = 0.8f;

  private static final Color BACKDROP = new Color(0.08f, 0.055f, 0.06f, 1f);
  private static final Color FRAME = new Color(0.43f, 0.29f, 0.16f, 1f);
  private static final Color FACE = new Color(0.84f, 0.76f, 0.61f, 1f);
  private static final Color PIP = new Color(0.17f, 0.09f, 0.08f, 1f);
  private static final Color MAGIC_GOLD = new Color(0.94f, 0.67f, 0.27f, 0.7f);
  private static final Color MAGIC_VIOLET = new Color(0.68f, 0.47f, 0.93f, 0.7f);

  private final Table table;
  private final DieFace firstDie;
  private final DieFace secondDie;
  private final Label firstValueLabel;
  private final Label secondValueLabel;
  private final Label totalLabel;
  private final Label scenicTotalValueLabel;
  private boolean rolling;

  DiceRollDisplay(Skin skin, LabelStyle labelStyle) {
    this(skin, labelStyle, false);
  }

  DiceRollDisplay(Skin skin, LabelStyle labelStyle, boolean scenic) {
    table = new Table();
    if (!scenic) {
      table.setBackground(skin.newDrawable("white", BACKDROP));
    }
    table.pad(10f, 16f, 10f, 16f);

    firstDie = new DieFace(skin, scenic ? new Color(0.55f, 0.38f, 0.19f, 1f) : FRAME);
    secondDie = new DieFace(skin, scenic ? new Color(0.38f, 0.27f, 0.49f, 1f) : FRAME);
    firstValueLabel = new Label("?", labelStyle);
    secondValueLabel = new Label("?", labelStyle);
    totalLabel = new Label("TOTAL  ?", labelStyle);
    totalLabel.setFontScale(1.15f);

    table
        .add(dieColumn(skin, firstDie, firstValueLabel, scenic ? MAGIC_GOLD : null))
        .padRight(scenic ? 8f : 20f);
    table
        .add(dieColumn(skin, secondDie, secondValueLabel, scenic ? MAGIC_VIOLET : null))
        .padRight(scenic ? 12f : 30f);
    if (scenic) {
      Label heading = new Label("TOTAL", labelStyle);
      heading.setFontScale(1.1f);
      scenicTotalValueLabel = new Label("?", labelStyle);
      scenicTotalValueLabel.setFontScale(2.35f);
      Table totalColumn = new Table();
      totalColumn.add(heading).center();
      totalColumn.row();
      totalColumn.add(scenicTotalValueLabel).center().padTop(5f);
      table.add(totalColumn).minWidth(120f).center().expandX();
    } else {
      scenicTotalValueLabel = null;
      table.add(totalLabel).left().expandX();
    }
  }

  private static Table dieColumn(Skin skin, DieFace die, Label valueLabel, Color magicColour) {
    Table column = new Table();
    if (magicColour == null) {
      column.add(die).size(82f);
    } else {
      column.add(magicDie(skin, die, magicColour)).size(116f, 112f);
    }
    column.row();
    column.add(valueLabel).center().padTop(4f);
    return column;
  }

  private static Group magicDie(Skin skin, DieFace die, Color colour) {
    Group surround = new Group();
    surround.setSize(116f, 112f);
    addRuneFragments(skin, surround, colour);
    addOrbitingMotes(skin, surround, colour);

    float[][] points = {
      {6f, 53f}, {15f, 94f}, {51f, 105f}, {98f, 94f},
      {107f, 52f}, {94f, 10f}, {52f, 3f}, {12f, 17f}
    };
    for (int i = 0; i < points.length; i++) {
      Group spark = new Group();
      spark.setSize(10f, 10f);
      spark.setPosition(points[i][0], points[i][1]);
      Table horizontal = new Table();
      horizontal.setBackground(skin.newDrawable("white", colour));
      horizontal.setBounds(0f, 4f, 10f, 2f);
      spark.addActor(horizontal);
      Table vertical = new Table();
      vertical.setBackground(skin.newDrawable("white", colour));
      vertical.setBounds(4f, 0f, 2f, 10f);
      spark.addActor(vertical);
      spark.getColor().a = 0.25f;
      spark.addAction(
          Actions.forever(
              Actions.sequence(
                  Actions.delay((i % 4) * 0.18f),
                  Actions.alpha(0.8f, 0.7f),
                  Actions.alpha(0.18f, 0.9f))));
      surround.addActor(spark);
    }
    die.setBounds(17f, 15f, 82f, 82f);
    surround.addActor(die);
    return surround;
  }

  private static void addRuneFragments(Skin skin, Group surround, Color colour) {
    Group fragments = new Group();
    fragments.setSize(116f, 112f);
    float[][] strokes = {
      {33f, 106f, 18f, 2f}, {65f, 106f, 18f, 2f},
      {33f, 4f, 18f, 2f}, {65f, 4f, 18f, 2f},
      {6f, 31f, 2f, 17f}, {6f, 65f, 2f, 17f},
      {108f, 31f, 2f, 17f}, {108f, 65f, 2f, 17f}
    };
    for (float[] stroke : strokes) {
      Table fragment = new Table();
      fragment.setBackground(skin.newDrawable("white", colour));
      fragment.setBounds(stroke[0], stroke[1], stroke[2], stroke[3]);
      fragments.addActor(fragment);
    }
    fragments.getColor().a = 0.22f;
    fragments.addAction(
        Actions.forever(Actions.sequence(Actions.alpha(0.55f, 1.4f), Actions.alpha(0.22f, 1.4f))));
    surround.addActor(fragments);
  }

  private static void addOrbitingMotes(Skin skin, Group surround, Color colour) {
    Group orbit = new Group();
    orbit.setSize(116f, 112f);
    orbit.setOrigin(58f, 56f);
    orbit.setTransform(true);
    for (int i = 0; i < 16; i++) {
      double angle = i * Math.PI / 8d;
      float size = i % 4 == 0 ? 5f : 3f;
      float x = 58f + (float) Math.cos(angle) * 51f - size / 2f;
      float y = 56f + (float) Math.sin(angle) * 50f - size / 2f;
      Table mote = new Table();
      mote.setBackground(skin.newDrawable("white", colour));
      mote.setBounds(x, y, size, size);
      orbit.addActor(mote);
    }
    orbit.getColor().a = 0.56f;
    orbit.addAction(Actions.forever(Actions.rotateBy(360f, 12f, Interpolation.linear)));
    surround.addActor(orbit);
  }

  Table getTable() {
    return table;
  }

  boolean isRolling() {
    return rolling;
  }

  int getFirstValue() {
    return firstDie.value;
  }

  int getSecondValue() {
    return secondDie.value;
  }

  String getTotalText() {
    return totalLabel.getText().toString();
  }

  String getDisplayedTotalValue() {
    return scenicTotalValueLabel == null ? "" : scenicTotalValueLabel.getText().toString();
  }

  void play(DiceRoll result, Runnable onRevealed) {
    if (rolling) {
      throw new IllegalStateException("A dice reveal is already in progress");
    }
    rolling = true;
    totalLabel.setText("ROLLING...");
    if (scenicTotalValueLabel != null) {
      scenicTotalValueLabel.setText("...");
    }
    firstValueLabel.setText("?");
    secondValueLabel.setText("?");
    firstDie.spin(0, result.firstDie());
    secondDie.spin(3, result.secondDie());
    table.addAction(
        Actions.sequence(
            Actions.delay(ROLL_DURATION),
            Actions.run(
                () -> {
                  firstDie.clearActions();
                  secondDie.clearActions();
                  firstDie.setRotation(0f);
                  secondDie.setRotation(0f);
                  firstDie.setFace(result.firstDie());
                  secondDie.setFace(result.secondDie());
                  firstValueLabel.setText(Integer.toString(result.firstDie()));
                  secondValueLabel.setText(Integer.toString(result.secondDie()));
                  totalLabel.setText("TOTAL  " + result.total());
                  if (scenicTotalValueLabel != null) {
                    scenicTotalValueLabel.setText(Integer.toString(result.total()));
                  }
                  rolling = false;
                  onRevealed.run();
                })));
  }

  private static final class DieFace extends Table {
    private final Table[] pips = new Table[9];
    private int value;

    private DieFace(Skin skin, Color frameColour) {
      setTransform(true);
      setOrigin(41f, 41f);
      setBackground(skin.newDrawable("white", frameColour));

      Table face = new Table();
      face.setBackground(skin.newDrawable("white", FACE));
      Table grid = new Table();
      for (int row = 0; row < 3; row++) {
        for (int column = 0; column < 3; column++) {
          int index = row * 3 + column;
          Table pip = new Table();
          pip.setBackground(skin.newDrawable("white", PIP));
          pips[index] = pip;
          grid.add(pip).size(10f).pad(5f);
        }
        grid.row();
      }
      face.add(grid).center();
      add(face).grow().pad(5f);
      setFace(0);
    }

    private void spin(int offset, int finalFace) {
      clearActions();
      setRotation(0f);
      TemporalAction cyclingFaces =
          new TemporalAction(ROLL_DURATION) {
            @Override
            protected void update(float progress) {
              // The final face stays visible during the last quarter of the spin. It therefore
              // matches the face shown when the result labels appear, without a last-frame jump.
              int face = progress >= 0.75f ? finalFace : ((int) (progress * 8f) + offset) % 6 + 1;
              if (face != value) {
                setFace(face);
              }
            }
          };
      addAction(
          Actions.parallel(
              Actions.rotateBy(720f, ROLL_DURATION, Interpolation.pow2Out), cyclingFaces));
    }

    private void setFace(int face) {
      value = face;
      boolean[] lit = new boolean[9];
      switch (face) {
        case 1 -> lit[4] = true;
        case 2 -> {
          lit[0] = true;
          lit[8] = true;
        }
        case 3 -> {
          lit[0] = true;
          lit[4] = true;
          lit[8] = true;
        }
        case 4 -> {
          lit[0] = true;
          lit[2] = true;
          lit[6] = true;
          lit[8] = true;
        }
        case 5 -> {
          lit[0] = true;
          lit[2] = true;
          lit[4] = true;
          lit[6] = true;
          lit[8] = true;
        }
        case 6 -> {
          lit[0] = true;
          lit[2] = true;
          lit[3] = true;
          lit[5] = true;
          lit[6] = true;
          lit[8] = true;
        }
        default -> {
          // A blank face is shown before the first roll.
        }
      }
      for (int i = 0; i < pips.length; i++) {
        pips[i].setVisible(lit[i]);
      }
    }
  }
}
