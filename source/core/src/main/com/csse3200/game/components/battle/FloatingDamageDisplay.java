package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.combat.DamageTakenEvent;
import com.csse3200.game.components.combat.DamageType;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.rendering.RenderComponent;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws short-lived damage numbers above an entity after it loses health.
 *
 * <p>Each number rises and fades independently. The component owns only presentation: combat logic
 * emits the {@code damageTaken} event, while this class reads its actual health-loss value.
 */
public class FloatingDamageDisplay extends RenderComponent {
  static final float DISPLAY_DURATION = 0.8f;
  private static final float RISE_DISTANCE = 0.7f;
  private static final float ANCHOR_GAP = 0.15f;
  private static final float STACK_GAP = 0.12f;
  private static final float HORIZONTAL_GAP = 0.12f;
  private static final float FONT_SCALE = 0.03f;
  private static final float FRONT_Z_INDEX = 1000f;

  private static final Color PIERCE_COLOR = new Color(1f, 0.72f, 0.2f, 1f);
  private static final Color POISON_COLOR = new Color(0.35f, 0.9f, 0.35f, 1f);

  private final List<FloatingNumber> numbers = new ArrayList<>();
  private final EventListener1<DamageTakenEvent> damageListener = this::onDamageTaken;
  private final GlyphLayout layout = new GlyphLayout();

  private BitmapFont font;

  @Override
  public void create() {
    super.create();

    font = new BitmapFont();
    font.setUseIntegerPositions(false);
    font.getData().setScale(FONT_SCALE);

    entity.getEvents().addListener("damageTaken", damageListener);
  }

  @Override
  public void update() {
    advance(ServiceLocator.getTimeSource().getDeltaTime());
  }

  @Override
  public float getZIndex() {
    return FRONT_Z_INDEX;
  }

  @Override
  protected void draw(SpriteBatch batch) {
    if (font == null || numbers.isEmpty()) {
      return;
    }

    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();
    float previousColor = batch.getPackedColor();

    for (int index = 0; index < numbers.size(); index++) {
      FloatingNumber number = numbers.get(index);
      float elapsed = number.elapsed;
      float progress = Math.min(elapsed / DISPLAY_DURATION, 1f);
      float alpha = 1f - progress;
      Color color = colorFor(number.type);

      String text = Integer.toString(number.amount);
      layout.setText(font, text);

      int horizontalSlot = index % 3 - 1;
      int verticalSlot = index / 3;
      float textX = position.x + scale.x / 2f - layout.width / 2f + horizontalSlot * HORIZONTAL_GAP;
      float textY =
          position.y + scale.y + ANCHOR_GAP + verticalSlot * STACK_GAP + verticalOffset(elapsed);

      font.setColor(color.r, color.g, color.b, alpha);
      font.draw(batch, text, textX, textY);
    }

    batch.setPackedColor(previousColor);
  }

  @Override
  public void dispose() {
    if (entity != null) {
      entity.getEvents().removeListener("damageTaken", damageListener);
    }

    if (font != null) {
      font.dispose();
      font = null;
    }

    numbers.clear();
    super.dispose();
  }

  int activeNumberCount() {
    return numbers.size();
  }

  void advance(float delta) {
    float safeDelta = Math.max(0f, delta);

    for (FloatingNumber number : numbers) {
      number.elapsed += safeDelta;
    }

    numbers.removeIf(number -> isExpired(number.elapsed));
  }

  static Color colorFor(DamageType type) {
    return switch (type) {
      case NORMAL -> Color.WHITE;
      case PIERCE -> PIERCE_COLOR;
      case POISON -> POISON_COLOR;
    };
  }

  static boolean isExpired(float elapsed) {
    return elapsed >= DISPLAY_DURATION;
  }

  static float verticalOffset(float elapsed) {
    float progress = Math.min(Math.max(elapsed / DISPLAY_DURATION, 0f), 1f);
    return RISE_DISTANCE * progress;
  }

  private void onDamageTaken(DamageTakenEvent event) {
    if (event != null && event.amount() > 0) {
      numbers.add(new FloatingNumber(event.amount(), event.type()));
    }
  }

  private static class FloatingNumber {
    private final int amount;
    private final DamageType type;
    private float elapsed;

    private FloatingNumber(int amount, DamageType type) {
      this.amount = amount;
      this.type = type;
    }
  }
}
