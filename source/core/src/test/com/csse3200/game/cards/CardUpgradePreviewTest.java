package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CardUpgradePreviewTest {
  @Test
  void all26PreviewsUseConfiguredUpgradeValuesWithoutChangingOwnedCopiesOrDefinitions() {
    List<CardConfig> cards = CardConfigLoader.loadCards();
    assertEquals(26, cards.size());
    CardLibrary library = new CardLibrary(cards);
    PlayerDeck deck = new PlayerDeck(library, cards.stream().map(card -> card.id).toList());
    List<CardInstance> owned = deck.getCards();
    CardDiscoveryService discovery = new CardDiscoveryService(cards);
    discovery.recordSeenAll(cards.stream().map(card -> card.id).toList());
    Map<String, CardUnlockState> progress = discovery.getProgressSnapshot();
    for (CardConfig card : cards) {
      var before = discovery.getEntry(card.id).orElseThrow();
      var upgraded = discovery.getUpgradePreview(card.id).orElseThrow();
      assertTrue(upgraded.upgraded(), card.id);
      assertTrue(upgraded.instanceId().startsWith("upgrade-preview-"), card.id);
      assertFalse(owned.stream().anyMatch(copy -> copy.instanceId().equals(upgraded.instanceId())));
      assertEquals(card.upgrade.name, upgraded.name(), card.id);
      assertEquals(card.upgrade.cost, upgraded.cost(), card.id);
      assertEquals(card.upgrade.description, upgraded.description(), card.id);
      assertEquals(card.upgrade.effects.length, upgraded.effects().size(), card.id);
      for (int i = 0; i < card.upgrade.effects.length; i++) {
        assertEquals(card.upgrade.effects[i].type, upgraded.effects().get(i).type, card.id);
        assertEquals(card.upgrade.effects[i].value, upgraded.effects().get(i).value, card.id);
        assertEquals(card.upgrade.effects[i].duration, upgraded.effects().get(i).duration, card.id);
      }
      int value = card.upgrade.effects[0].value;
      upgraded.effects().getFirst().value = -999;
      assertEquals(value, card.upgrade.effects[0].value, card.id);
      assertEquals(
          value, discovery.getUpgradePreview(card.id).orElseThrow().effects().getFirst().value);
      assertEquals(before, discovery.getEntry(card.id).orElseThrow());
    }
    assertEquals(owned, deck.getCards());
    assertTrue(deck.getCards().stream().noneMatch(CardInstance::isUpgraded));
    assertEquals(progress, discovery.getProgressSnapshot());
  }

  @Test
  void unknownLockedRelockedAndMissingUpgradesReturnNoPreview() {
    List<CardConfig> cards = CardConfigLoader.loadCards();
    cards.stream().filter(card -> "defend".equals(card.id)).findFirst().orElseThrow().upgrade =
        null;
    CardDiscoveryService discovery = new CardDiscoveryService(cards);
    assertTrue(discovery.getUpgradePreview(null).isEmpty());
    assertTrue(discovery.getUpgradePreview("missing").isEmpty());
    assertTrue(discovery.getUpgradePreview("strike").isEmpty());
    discovery.recordSeen("defend");
    assertTrue(discovery.getUpgradePreview("defend").isEmpty());
    discovery.recordSeen("strike");
    assertTrue(discovery.getUpgradePreview("strike").isPresent());
    discovery.replaceProgress(Map.of());
    assertTrue(discovery.getUpgradePreview("strike").isEmpty());
  }
}
