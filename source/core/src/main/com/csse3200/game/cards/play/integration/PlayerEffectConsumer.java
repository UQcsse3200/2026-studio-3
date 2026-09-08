package com.csse3200.game.cards.play.integration;

import com.csse3200.game.cards.effects.ResolvedCardEffect;
import java.util.List;

/** Team 7 boundary used by the battle flow to apply resolved player effects. */
public interface PlayerEffectConsumer {
  /** Applies a successful card play's player-owned effects to the real player state. */
  void applyPlayerEffects(List<ResolvedCardEffect> effects);
}
