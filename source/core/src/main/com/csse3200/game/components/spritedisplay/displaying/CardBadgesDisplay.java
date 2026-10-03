package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.csse3200.game.components.battle.DeckEditorEvents;
import java.util.ArrayList;
import java.util.List;

/**
 * Display-only numbered badges that straddle the top-right corner of cards. The deck editor owns
 * the card layout, so it pushes the badge list in (fire the record's {@code trigger} with a {@code
 * List<Badge>}); this class only owns how a badge looks (size, colour, font). An empty list, or
 * {@link DeckEditorEvents#CLOSED}, clears them.
 *
 * <p>The record's own position/size/text are unused here (badge positions come from the payload).
 *
 * <p>The record's {@code skin} is optional. If supplied, the badge picks up the skin's default
 * {@code Label.LabelStyle} and, if the skin defines a {@code "white"} drawable, gets a semi-opaque
 * dark background box. If {@code skin} is null (or the skin has no {@code Label.LabelStyle}), a
 * plain {@link BitmapFont} fallback is used and the badge renders as a bare white number.
 */
public class CardBadgesDisplay extends Displaying {

  /**
   * @param number the number shown in the badge
   * @param cornerX x of the card's top-right corner, in stage coordinates
   * @param cornerY y of the card's top-right corner, in stage coordinates (y up)
   */
  public record Badge(int number, float cornerX, float cornerY) {}

  private static final float SIZE = 24f;
  // How much of the badge pokes out past the corner (the rest sits inside the card).
  private static final float OVERHANG = SIZE * 0.25f;
  private static final Color BACKGROUND = new Color(0.1f, 0.1f, 0.1f, 0.9f);

  private final List<Label> badges = new ArrayList<>();
  private Label.LabelStyle badgeStyle;
  // Only non-null when we fell back to a plain BitmapFont (no skin supplied). Owned by us, so
  // disposed in dispose().
  private BitmapFont fallbackFont;

  public CardBadgesDisplay(DisplayingRecord rec) {
    super(rec);
    label.setVisible(false); // the inherited label isn't used
  }

  @Override
  public void create() {
    super.create();
    badgeStyle = buildBadgeStyle();
    entity.getEvents().addListener(DeckEditorEvents.CLOSED, this::clear);
    entity.getEvents().addListener(DeckEditorEvents.TO_FRONT, () -> badges.forEach(Label::toFront));
  }

  /**
   * Prefers the record's own skin for styling; falls back to a plain {@link BitmapFont} if the skin
   * is null or missing a default {@code Label.LabelStyle}. Never throws.
   */
  private Label.LabelStyle buildBadgeStyle() {
    if (skin != null) {
      try {
        Label.LabelStyle style = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        style.fontColor = Color.WHITE;
        if (skin.has("white", Drawable.class)) {
          style.background = skin.newDrawable("white", BACKGROUND);
        }
        return style;
      } catch (GdxRuntimeException ignored) {
        // No Label.LabelStyle registered under the default key — fall through to the fallback.
      }
    }
    fallbackFont = new BitmapFont();
    fallbackFont.getData().setScale(1.2f);
    return new Label.LabelStyle(fallbackFont, Color.WHITE);
  }

  @Override
  public void onTrigger(Object payload) {
    clear();
    if (!(payload instanceof List<?> list)) {
      return;
    }
    for (Object item : list) {
      if (item instanceof Badge badge) {
        add(badge);
      }
    }
  }

  private void add(Badge badge) {
    Label badgeLabel = new Label(String.valueOf(badge.number()), badgeStyle);
    badgeLabel.setAlignment(Align.center);
    badgeLabel.setSize(SIZE, SIZE);
    badgeLabel.setPosition(badge.cornerX() - SIZE + OVERHANG, badge.cornerY() - SIZE + OVERHANG);
    badgeLabel.setTouchable(Touchable.disabled); // clicks fall through to the card underneath
    stage.addActor(badgeLabel);
    badges.add(badgeLabel);
  }

  private void clear() {
    for (Label badge : badges) {
      badge.remove();
    }
    badges.clear();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Badges are positioned once, when they're pushed in.
  }

  @Override
  public void dispose() {
    clear();
    if (fallbackFont != null) {
      fallbackFont.dispose();
      fallbackFont = null;
    }
    super.dispose();
  }
}
