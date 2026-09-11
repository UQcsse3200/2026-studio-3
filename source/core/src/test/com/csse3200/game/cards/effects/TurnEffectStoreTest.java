package com.csse3200.game.cards.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.TargetType;
import java.util.List;
import org.junit.jupiter.api.Test;

class TurnEffectStoreTest {
  @Test
  void shouldPreservePlayAndEffectOrderAcrossResolutions() {
    TurnEffectStore store = new TurnEffectStore();
    ResolvedCardEffect damage =
        effect("poison_dagger", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 4, 0, 0);
    ResolvedCardEffect poison =
        effect("poison_dagger", EffectType.POISON, TargetType.SINGLE_ENEMY, 3, 3, 1);
    ResolvedCardEffect block = effect("defend", EffectType.BLOCK, TargetType.SELF, 5, 0, 0);

    store.record(new CardEffectResolution("poison_dagger", List.of(damage, poison)));
    store.record(new CardEffectResolution("defend", List.of(block)));

    assertEquals(List.of(damage, poison), store.getEnemyEffects());
    assertEquals(List.of(block), store.getPlayerEffects());
    assertEquals(List.of(poison), store.getEffectsOfType(EffectType.POISON));
    assertEquals(
        List.of("poison_dagger", "defend"),
        store.getResolutions().stream().map(CardEffectResolution::cardId).toList());
  }

  @Test
  void shouldReturnImmutableSnapshotsAndClearTheStore() {
    TurnEffectStore store = new TurnEffectStore();
    store.record(
        new CardEffectResolution(
            "strike",
            List.of(effect("strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 6, 0, 0))));
    List<CardEffectResolution> snapshot = store.getResolutions();

    assertThrows(
        UnsupportedOperationException.class,
        () -> snapshot.add(new CardEffectResolution("empty", List.of())));

    store.clear();

    assertEquals(1, snapshot.size());
    assertEquals(List.of(), store.getResolutions());
    assertEquals(List.of(), store.getEnemyEffects());
  }

  @Test
  void shouldRejectInvalidQueriesAndRecords() {
    TurnEffectStore store = new TurnEffectStore();

    assertThrows(IllegalArgumentException.class, () -> store.record(null));
    assertThrows(IllegalArgumentException.class, () -> store.getEffectsOfType(null));
  }

  private static ResolvedCardEffect effect(
      String cardId, EffectType type, TargetType target, int value, int duration, int sequence) {
    return new ResolvedCardEffect(cardId, type, target, value, duration, sequence);
  }
}
