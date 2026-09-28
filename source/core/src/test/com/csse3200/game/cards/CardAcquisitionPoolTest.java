package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CardAcquisitionPoolTest {
  private CardService cards;

  @BeforeEach
  void setUp() {
    cards = new CardLibrary(CardConfigLoader.loadCards());
  }

  @Test
  void shouldValidateAndReturnStableImmutableEligibility() {
    List<String> source = new ArrayList<>(List.of("strike", "bandage", "defend"));
    CardAcquisitionPool pool = new CardAcquisitionPool(cards, source);
    source.clear();

    assertEquals(List.of("bandage", "defend", "strike"), pool.eligibleCardIds());
    assertEquals(
        List.of("bandage", "defend", "strike"),
        pool.getEligibleCards().stream().map(card -> card.id).toList());
    assertTrue(pool.isEligible("strike"));
    assertFalse(pool.isEligible("poison_dagger"));
    assertThrows(
        UnsupportedOperationException.class, () -> pool.eligibleCardIds().add("poison_dagger"));
  }

  @Test
  void shouldRejectInvalidEligibility() {
    assertThrows(NullPointerException.class, () -> new CardAcquisitionPool(null, List.of()));
    assertThrows(IllegalArgumentException.class, () -> new CardAcquisitionPool(cards, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CardAcquisitionPool(cards, List.of("strike", "strike")));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CardAcquisitionPool(cards, List.of("missing-card")));
    assertThrows(
        IllegalArgumentException.class, () -> new CardAcquisitionPool(cards, List.of(" ")));
  }

  @Test
  void defaultPoolShouldContainEveryCurrentProductionDefinition() {
    CardAcquisitionPool pool = CardAcquisitionPoolLoader.loadDefault(cards);

    assertEquals(cards.getAllCards().size(), pool.eligibleCardIds().size());
    assertTrue(cards.getAllCards().stream().allMatch(card -> pool.isEligible(card.id)));
  }

  @Test
  void loaderShouldRejectMissingOrBlankConfigurationPaths() {
    assertThrows(IllegalArgumentException.class, () -> CardAcquisitionPoolLoader.load(" ", cards));
    assertThrows(
        IllegalArgumentException.class,
        () -> CardAcquisitionPoolLoader.load("configs/not-a-real-pool.json", cards));
  }
}
