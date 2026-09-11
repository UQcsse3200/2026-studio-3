package com.csse3200.game.cards.effects;

import com.csse3200.game.cards.EffectType;
import java.util.List;

/**
 * Read-only query boundary for resolved card effects recorded during the current turn.
 *
 * <p>Team 1 and Team 7 can consume these results without depending on Team 5's resolver or
 * calculation state. Implementations store resolution data only and do not mutate player or enemy
 * entities.
 */
public interface CardEffectResultStore {
  /** Records the immutable result produced for one played card. */
  void record(CardEffectResolution resolution);

  /**
   * @return recorded card resolutions in play order
   */
  List<CardEffectResolution> getResolutions();

  /**
   * @return all recorded enemy-targeting effects in play and effect sequence order
   */
  List<ResolvedCardEffect> getEnemyEffects();

  /**
   * @return all recorded player-targeting effects in play and effect sequence order
   */
  List<ResolvedCardEffect> getPlayerEffects();

  /**
   * Returns all recorded effects of one type.
   *
   * @param type effect type to query
   * @return matching effects in play and effect sequence order
   */
  List<ResolvedCardEffect> getEffectsOfType(EffectType type);

  /** Clears the recorded turn results without changing Team 5's combat-long modifier state. */
  void clear();
}
