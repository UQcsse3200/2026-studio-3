package com.csse3200.game.cards.debug;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.effects.CardEffectResolution;
import com.csse3200.game.cards.effects.ResolvedCardEffect;
import com.csse3200.game.ui.UIComponent;
import java.util.EnumMap;
import java.util.Map;

public class CardEffectDebugDisplay extends UIComponent {
  private static final float Z_INDEX = 10f;
  private static final float CARD_COL = 220f;
  private static final float EFFECT_COL = 160f;
  private static final float TARGET_COL = 200f;
  private static final float VALUE_COL = 70f;
  private static final float DURATION_COL = 140f;
  private static final float TABLE_WIDTH =
      CARD_COL + EFFECT_COL + TARGET_COL + VALUE_COL + DURATION_COL;
  private static final float ROW_GAP = 4f;
  private static final Color STRIPE_COLOR = new Color(1f, 1f, 1f, 0.06f);
  private static final Color LINE_COLOR = new Color(0f, 0f, 0f, 0.35f);

  private static final Map<EffectType, Color> EFFECT_COLORS = new EnumMap<>(EffectType.class);

  static {
    EFFECT_COLORS.put(EffectType.DAMAGE, new Color(0.85f, 0.15f, 0.15f, 1f));
    EFFECT_COLORS.put(EffectType.HEAL, new Color(0.25f, 0.75f, 0.3f, 1f));
    EFFECT_COLORS.put(EffectType.BLOCK, new Color(0.55f, 0.65f, 0.8f, 1f));
    EFFECT_COLORS.put(EffectType.POISON, new Color(0.45f, 0.75f, 0.2f, 1f));
    EFFECT_COLORS.put(EffectType.VULNERABLE, new Color(0.9f, 0.4f, 0.1f, 1f));
    EFFECT_COLORS.put(EffectType.STRENGTH, new Color(1f, 0.92f, 0.15f, 1f));
  }

  private CardEffectDebugComponent debug;
  private Window window;
  private Table rows;
  private Label.LabelStyle defaultStyle;

  @Override
  public void create() {
    super.create();
    debug = entity.getComponent(CardEffectDebugComponent.class);
    defaultStyle = skin.get("default", Label.LabelStyle.class);
    addActors();
  }

  private void addActors() {
    window = new Window("Card effect resolution", skin);
    window.setPosition(20f, 20f);
    window.setSize(TABLE_WIDTH + 40f, 320f);
    window.setVisible(false);
    window.top().left();
    window.pad(10f);
    window.getTitleTable().padTop(6f).padBottom(6f);

    Table header = new Table();
    header.add(new Label("Card", skin)).width(CARD_COL).left();
    header.add(new Label("Effect", skin)).width(EFFECT_COL).left();
    header.add(new Label("Target", skin)).width(TARGET_COL).left();
    header.add(new Label("Value", skin)).width(VALUE_COL).right();
    header.add(new Label("Duration", skin)).width(DURATION_COL).right();
    window.add(new Table()).height(16f).row();
    window.add(header).left().padBottom(14f).row();

    Image separator = new Image(skin.newDrawable("white", LINE_COLOR));
    window.add(separator).width(TABLE_WIDTH).height(2f).padBottom(8f).left().row();

    rows = new Table();
    window.add(rows).expand().fill().top();

    stage.addActor(window);
  }

  @Override
  public void draw(SpriteBatch batch) {
    boolean open = debug.isOpen();
    window.setVisible(open);
    if (open) {
      refreshRows();
    }
  }

  private void refreshRows() {
    rows.clear();
    var resolutions = debug.getResolutions();

    if (resolutions.isEmpty()) {
      rows.add(new Label("No cards played yet this turn", skin)).colspan(5).left();
      return;
    }

    int rowIndex = 0;
    for (CardEffectResolution resolution : resolutions) {
      for (ResolvedCardEffect effect : resolution.effects()) {
        Table rowTable = new Table();
        if (rowIndex % 2 == 1) {
          rowTable.setBackground(skin.newDrawable("white", STRIPE_COLOR));
        }

        Label.LabelStyle typeStyle = styleFor(effect.type());

        rowTable.add(new Label(resolution.cardId(), skin)).width(CARD_COL).left();
        rowTable.add(new Label(effect.type().name(), typeStyle)).width(EFFECT_COL).left();
        rowTable.add(new Label(effect.target().name(), skin)).width(TARGET_COL).left();
        rowTable.add(new Label(String.valueOf(effect.value()), typeStyle)).width(VALUE_COL).right();
        rowTable
            .add(new Label(String.valueOf(effect.duration()), skin))
            .width(DURATION_COL)
            .right();

        rows.add(rowTable).width(TABLE_WIDTH).left().padBottom(ROW_GAP).row();
        rowIndex++;
      }
    }
  }

  private Label.LabelStyle styleFor(EffectType type) {
    Color color = EFFECT_COLORS.get(type);
    if (color == null) {
      return defaultStyle;
    }
    return new Label.LabelStyle(defaultStyle.font, color);
  }

  @Override
  public float getZIndex() {
    return Z_INDEX;
  }

  @Override
  public void dispose() {
    super.dispose();
    window.remove();
  }
}
