package com.csse3200.game.cards.debug;

import com.csse3200.game.cards.effects.CardEffectResolution;
import com.csse3200.game.cards.effects.CardEffectResolutionService;
import com.csse3200.game.components.Component;
import java.util.List;

/**
 * State tracker for the card effect debug dialog. Holds whether the dialog is open and exposes this
 * turn's resolved card effects for display.
 *
 * <p>This does not own any resolution logic — it only reads from Team 5's {@link
 * CardEffectResolutionService}, the same service used to actually play cards. Attach the same
 * instance here as the one combat uses, so the dialog shows real results, not a separate copy.
 */
public class CardEffectDebugComponent extends Component {
  private final CardEffectResolutionService cardEffects;
  private boolean isOpen = false;

  public CardEffectDebugComponent(CardEffectResolutionService cardEffects) {
    if (cardEffects == null) {
      throw new IllegalArgumentException("Card effect resolution service cannot be null");
    }
    this.cardEffects = cardEffects;
  }

  public boolean isOpen() {
    return isOpen;
  }

  public void toggleOpen() {
    isOpen = !isOpen;
  }

  /**
   * @return this turn's resolved card effects, in play order
   */
  public List<CardEffectResolution> getResolutions() {
    return cardEffects.getResolutions();
  }
}
