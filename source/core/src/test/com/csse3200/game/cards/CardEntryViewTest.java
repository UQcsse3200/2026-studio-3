package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.configs.CardConfig;
import org.junit.jupiter.api.Test;

class CardEntryViewTest {
  @Test
  void shouldMaskEveryCardDetailWhileLocked() {
    CardEntryView view = CardEntryView.from(card(), CardUnlockState.LOCKED);

    assertEquals("strike", view.cardId());
    assertEquals(CardUnlockState.LOCKED, view.unlockState());
    assertEquals("???", view.displayName());
    assertTrue(view.description().isEmpty());
    assertTrue(view.cost().isEmpty());
    assertTrue(view.type().isEmpty());
    assertTrue(view.target().isEmpty());
    assertTrue(view.rarity().isEmpty());
    assertTrue(view.effects().isEmpty());
    assertTrue(view.texturePath().isEmpty());
  }

  @Test
  void shouldExposeEveryCardDetailWhenSeen() {
    CardConfig card = card();

    CardEntryView view = CardEntryView.from(card, CardUnlockState.SEEN);

    assertEquals(card.name, view.displayName());
    assertEquals(card.description, view.description().orElseThrow());
    assertEquals(card.cost, view.cost().orElseThrow());
    assertEquals(card.type, view.type().orElseThrow());
    assertEquals(card.target, view.target().orElseThrow());
    assertEquals(card.rarity, view.rarity().orElseThrow());
    assertEquals(0, view.effects().orElseThrow().size());
    assertEquals(card.texturePath, view.texturePath().orElseThrow());
  }

  private static CardConfig card() {
    return CardDiscoveryServiceTest.card("strike", "Strike");
  }
}
