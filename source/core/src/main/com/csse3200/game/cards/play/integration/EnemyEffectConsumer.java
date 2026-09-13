package com.csse3200.game.cards.play.integration;

import com.csse3200.game.cards.effects.ResolvedCardEffect;
import com.csse3200.game.cards.play.CardPlayTarget;
import java.util.List;

/** Team 1 boundary used by the battle flow to apply resolved enemy effects. */
public interface EnemyEffectConsumer {
  /** Applies a successful card play's enemy-owned effects to the selected target or targets. */
  void applyEnemyEffects(CardPlayTarget target, List<ResolvedCardEffect> effects);
}
