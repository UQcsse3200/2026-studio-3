package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.components.battle.DeckEditorEvents;
import java.util.ArrayList;
import java.util.List;

/**
 * Display-only numbered badges that straddle the top-right corner of cards. The deck editor owns
 * the card layout, so it pushes the badge list in (fire the record's {@code trigger} with a {@code
 * List<Badge>}); this class only owns how a badge looks (size, colours, font). An empty list, or
 * {@link DeckEditorEvents#CLOSED}, clears them.
 *
 * <p>The record's own position/size/text are unused here (badge positions come from the payload).
 * The badge font comes from the UI skin's default {@code Label.LabelStyle}, and its background is a
 * flat box built from the skin's {@code "white"} drawable.
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
  private static final Color BACKGROUND = new Color(0.06f, 0.03f, 0.04f, 0.92f); // near-black
  private static final Color TEXT = Color.valueOf("F2BA47"); // gold

  private final List<Label> badges = new ArrayList<>();
  private Label.LabelStyle badgeStyle;

  public CardBadgesDisplay(DisplayingRecord rec) {
    super(rec);
    label.setVisible(false); // the inherited label isn't used
  }

  @Override
  public void create() {
    super.create();
    badgeStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    badgeStyle.fontColor = TEXT;
    // "white" is a plain rectangle in the skin's atlas; newDrawable tints a copy of it.
    badgeStyle.background = skin.newDrawable("white", BACKGROUND);
    entity.getEvents().addListener(DeckEditorEvents.CLOSED, this::clear);
    entity.getEvents().addListener(DeckEditorEvents.TO_FRONT, () -> badges.forEach(Label::toFront));
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
    super.dispose();
  }
}
