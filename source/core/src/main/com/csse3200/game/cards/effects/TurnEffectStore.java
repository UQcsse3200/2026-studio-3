package com.csse3200.game.cards.effects;

import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.TargetType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** In-memory record of card resolutions produced during the current turn. */
public final class TurnEffectStore implements CardEffectResultStore {
  private final List<CardEffectResolution> resolutions = new ArrayList<>();

  @Override
  public void record(CardEffectResolution resolution) {
    if (resolution == null) {
      throw new IllegalArgumentException("Card effect resolution cannot be null");
    }
    resolutions.add(resolution);
  }

  @Override
  public List<CardEffectResolution> getResolutions() {
    return List.copyOf(resolutions);
  }

  @Override
  public List<ResolvedCardEffect> getEnemyEffects() {
    return allEffects().filter(effect -> effect.target() != TargetType.SELF).toList();
  }

  @Override
  public List<ResolvedCardEffect> getPlayerEffects() {
    return allEffects().filter(effect -> effect.target() == TargetType.SELF).toList();
  }

  @Override
  public List<ResolvedCardEffect> getEffectsOfType(EffectType type) {
    if (type == null) {
      throw new IllegalArgumentException("Effect type cannot be null");
    }
    return allEffects().filter(effect -> effect.type() == type).toList();
  }

  @Override
  public void clear() {
    resolutions.clear();
  }

  private Stream<ResolvedCardEffect> allEffects() {
    return resolutions.stream().flatMap(resolution -> resolution.effects().stream());
  }
}
