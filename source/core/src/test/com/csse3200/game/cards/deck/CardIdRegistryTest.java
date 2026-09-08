package com.csse3200.game.cards.deck;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CardIdRegistryTest {
  @Test
  void shouldRecogniseSprintOneCardIds() {
    List<String> registeredCardIds =
        List.of("strike", "defend", "poison_dagger", "expose", "inner_focus", "bandage");

    for (String cardId : registeredCardIds) {
      assertTrue(CardIdRegistry.isRegistered(cardId));
    }
  }

  @Test
  void shouldRejectNullBlankAndUnknownCardIds() {
    assertFalse(CardIdRegistry.isRegistered(null));
    assertFalse(CardIdRegistry.isRegistered(""));
    assertFalse(CardIdRegistry.isRegistered("  "));
    assertFalse(CardIdRegistry.isRegistered("unknown_card"));
  }
}
