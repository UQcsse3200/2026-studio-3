package com.csse3200.game.components.cards;

import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.runtime.ResolvedCard;
import java.util.Objects;

/**
 * Compatibility wrapper for callers of the original Library-only Uncommon widget. Rendering is
 * shared with every other authored card frame; the Library's discovery and selection stay
 * unchanged.
 */
public final class UncommonCardLibraryWidget extends FramedCardFace {
  public static final String FRAME_TEXTURE = CardWidgetAssets.UNCOMMON_FRAME_TEXTURE;

  /** Creates an Uncommon card face using externally owned frame and artwork resources. */
  public UncommonCardLibraryWidget(
      ResolvedCard card, CardWidgetAssets assets, Drawable frameDrawable) {
    super(assets, frameDrawable, CardFrameLayout.UNCOMMON);
    setCard(card);
  }

  @Override
  public void setCard(ResolvedCard card) {
    Objects.requireNonNull(card, "card cannot be null");
    if (card.rarity() != Rarity.UNCOMMON) {
      throw new IllegalArgumentException("uncommon frame requires an UNCOMMON card");
    }
    super.setCard(card);
  }
}
