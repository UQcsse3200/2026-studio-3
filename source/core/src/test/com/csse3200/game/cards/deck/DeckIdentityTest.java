package com.csse3200.game.cards.deck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.TestCardService;
import com.csse3200.game.cards.configs.CardUpgradeConfig;
import com.csse3200.game.cards.runtime.CardInstance;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeckIdentityTest {
  private CardService service;
  private PlayerDeck player;

  @BeforeEach
  void setUp() {
    service = TestCardService.withCards("strike", "defend");
    service.getCard("strike").orElseThrow().upgrade = new CardUpgradeConfig();
    player = new PlayerDeck(service, List.of("strike", "strike", "strike"));
  }

  @Test
  void createsUniqueInstancesForEveryAcquisition() {
    List<CardInstance> cards = player.getCards();
    assertEquals(3, cards.stream().map(CardInstance::instanceId).distinct().count());
    assertTrue(cards.stream().allMatch(card -> card.upgradeLevel() == 0));
    assertEquals(3, player.countByCardId("strike"));
    assertEquals(0, player.countByCardId("defend"));
    PlayerDeck other = new PlayerDeck(service, List.of("strike"));
    assertFalse(player.containsInstance(other.getCards().get(0).instanceId()));
  }

  @Test
  void upgradesOnlySelectedCopyAndPreservesOrderAndOldSnapshots() {
    List<CardInstance> before = player.getCards();
    CardInstance selected = before.get(1);
    CardInstance upgraded = player.upgradeCard(selected.instanceId());

    assertEquals(new CardInstance(selected.instanceId(), "strike", 1), upgraded);
    assertEquals(List.of(before.get(0), upgraded, before.get(2)), player.getCards());
    assertSame(before.get(0), player.getCards().get(0));
    assertSame(before.get(2), player.getCards().get(2));
    assertFalse(before.get(1).isUpgraded());
    assertEquals(upgraded, player.getCard(selected.instanceId()).orElseThrow());
  }

  @Test
  void removeAcceptsOnlyInstanceIdAndRemovesTheSelectedDuplicate() {
    List<CardInstance> before = player.getCards();
    assertFalse(player.removeCard("strike"));
    assertFalse(player.removeCard("missing"));
    assertTrue(player.removeCard(before.get(1).instanceId()));
    assertFalse(player.containsInstance(before.get(1).instanceId()));
    assertEquals(List.of(before.get(0), before.get(2)), player.getCards());
    assertEquals(before.get(2), player.removeCardAt(1));
  }

  @Test
  void rejectsDuplicateIdentityEvenAcrossDifferentDefinitionsWithoutMutation() {
    List<CardInstance> before = player.getCards();
    CardInstance first = before.get(0);
    CardInstance duplicateIdentity = new CardInstance(first.instanceId(), "defend", 0);
    assertThrows(IllegalArgumentException.class, () -> player.addCard(first));
    assertThrows(IllegalArgumentException.class, () -> player.addCard(duplicateIdentity));
    assertEquals(before, player.getCards());
  }

  @Test
  void rejectsInvalidInstancesAndMissingUpgradeDefinitionsWithoutMutation() {
    List<CardInstance> before = player.getCards();
    CardInstance unknown = new CardInstance("unknown", "unknown", 0);
    CardInstance missingUpgrade = new CardInstance("defend-plus", "defend", 1);
    assertThrows(IllegalArgumentException.class, () -> player.addCard((CardInstance) null));
    assertThrows(IllegalArgumentException.class, () -> player.addCard(unknown));
    assertThrows(IllegalArgumentException.class, () -> player.addCard(missingUpgrade));
    assertThrows(IllegalArgumentException.class, () -> player.getCard(" "));
    assertThrows(IllegalArgumentException.class, () -> player.removeCard(null));
    assertEquals(before, player.getCards());
  }

  @Test
  void invalidUpgradesLeaveDeckUnchanged() {
    player.addCard(new CardInstance("defend-instance", "defend", 0));
    player.upgradeCard(player.getCards().get(1).instanceId());
    List<CardInstance> before = player.getCards();
    assertThrows(IllegalArgumentException.class, () -> player.upgradeCard("strike"));
    assertThrows(IllegalArgumentException.class, () -> player.upgradeCard("missing"));
    assertThrows(IllegalArgumentException.class, () -> player.upgradeCard("defend-instance"));
    String alreadyUpgradedId = before.get(1).instanceId();
    assertThrows(IllegalStateException.class, () -> player.upgradeCard(alreadyUpgradedId));
    assertEquals(before, player.getCards());
  }

  @Test
  void copiesAndRestoresPreserveIdentityButDoNotShareMutableCollections() {
    player.upgradeCard(player.getCards().get(1).instanceId());
    List<CardInstance> before = player.getCards();
    PlayerDeck copy = player.copy();
    ArrayList<CardInstance> input = new ArrayList<>(before);
    PlayerDeck restored = PlayerDeck.fromInstances(service, input);
    input.clear();
    assertEquals(before, restored.getCards());
    assertEquals(before, copy.getCards());
    copy.upgradeCard(before.get(0).instanceId());
    copy.removeCard(before.get(2).instanceId());
    assertEquals(before, player.getCards());
    assertThrows(UnsupportedOperationException.class, before::clear);
    player.clear();
    assertEquals(3, before.size());
    assertEquals(3, restored.size());
    assertEquals(2, copy.size());
  }

  @Test
  void restoreRejectsNullCollectionsAndDuplicateInstances() {
    CardInstance card = player.getCards().get(0);
    List<CardInstance> duplicateCards = List.of(card, card);
    assertThrows(IllegalArgumentException.class, () -> PlayerDeck.fromInstances(service, null));
    assertThrows(
        IllegalArgumentException.class, () -> PlayerDeck.fromInstances(service, duplicateCards));
  }

  @Test
  void selectedUpgradeSurvivesPlayDiscardAndAutomaticReshuffle() {
    CardInstance upgraded = player.upgradeCard(player.getCards().get(1).instanceId());
    List<CardInstance> owned = player.getCards();
    BattleDeck battle = new BattleDeck(player);
    assertEquals(owned, battle.getDrawPile());
    assertEquals(owned, battle.drawCards(3));
    List<CardInstance> oldHand = battle.getHand();
    assertSame(upgraded, battle.getCardInHand(upgraded.instanceId()).orElseThrow());
    assertTrue(battle.playCard(upgraded.instanceId()));
    assertEquals(List.of(owned.get(0), owned.get(2)), battle.getHand());
    assertEquals(List.of(upgraded), battle.getDiscardPile());
    assertTrue(battle.getCardInHand(upgraded.instanceId()).isEmpty());
    assertSame(upgraded, battle.drawOne());
    assertEquals(List.of(owned.get(0), owned.get(2), upgraded), battle.getHand());
    assertTrue(battle.getDiscardPile().isEmpty());
    assertEquals(owned, oldHand);
    assertEquals(owned, player.getCards());
  }

  @Test
  void discardHandAndExplicitShufflePreserveEveryInstanceExactlyOnce() {
    player.upgradeCard(player.getCards().get(1).instanceId());
    BattleDeck battle = new BattleDeck(player);
    battle.shuffleDrawPile();
    assertEquals(new HashSet<>(player.getCards()), new HashSet<>(battle.getDrawPile()));
    assertEquals(3, battle.drawCards(9).size());
    assertEquals(3, battle.discardHand());
    assertEquals(0, battle.discardHand());
    assertTrue(battle.reshuffleDiscardIntoDrawPile());
    assertEquals(new HashSet<>(player.getCards()), new HashSet<>(battle.drawCards(3)));
    assertEquals(3, battle.getHand().stream().map(CardInstance::instanceId).distinct().count());
    assertEquals(1, battle.getHand().stream().filter(CardInstance::isUpgraded).count());
    assertNull(battle.drawOne());
  }

  @Test
  void invalidPlayAndDiscardDoNotChangePilesOrInterpretCardIdAsInstanceId() {
    BattleDeck battle = new BattleDeck(player);
    CardInstance drawn = battle.drawOne();
    CardInstance notDrawn = battle.getDrawPile().get(0);
    List<CardInstance> hand = battle.getHand();
    List<CardInstance> draw = battle.getDrawPile();
    for (String id : new String[] {null, "", "missing", "strike", notDrawn.instanceId()}) {
      assertFalse(battle.playCard(id));
      assertFalse(battle.discardCard(id));
    }
    assertEquals(hand, battle.getHand());
    assertEquals(draw, battle.getDrawPile());
    assertTrue(battle.getDiscardPile().isEmpty());
    assertTrue(battle.discardCard(drawn.instanceId()));
    assertFalse(battle.discardCard(drawn.instanceId()));
    assertSame(drawn, battle.getDiscardPile().get(0));
    assertFalse(battle.reshuffleDiscardIntoDrawPile());
  }

  @Test
  void allBattleSnapshotsAndDrawResultsAreImmutableAndIndependent() {
    BattleDeck battle = new BattleDeck(player);
    List<CardInstance> draw = battle.getDrawPile();
    List<CardInstance> drawn = battle.drawCards(2);
    List<CardInstance> hand = battle.getHand();
    battle.discardCard(drawn.get(0).instanceId());
    List<CardInstance> discard = battle.getDiscardPile();
    for (List<CardInstance> snapshot : List.of(draw, drawn, hand, discard)) {
      assertThrows(UnsupportedOperationException.class, snapshot::clear);
    }
    battle.discardHand();
    assertEquals(3, draw.size());
    assertEquals(2, drawn.size());
    assertEquals(2, hand.size());
    assertEquals(1, discard.size());
  }

  @Test
  void battleIsAnIndependentSnapshotOfPlayerUpgradeState() {
    BattleDeck existingBattle = new BattleDeck(player);
    List<CardInstance> before = player.getCards();
    player.upgradeCard(before.get(1).instanceId());
    assertEquals(before, existingBattle.getDrawPile());
    assertEquals(player.getCards(), new BattleDeck(player).getDrawPile());
  }

  @Test
  void strictPlayRejectsDefinitionIdEvenWithUpgradedCopies() {
    var upgraded = player.upgradeCard(player.getCards().get(0).instanceId());
    BattleDeck battle = new BattleDeck(player);
    battle.drawCards(3);
    assertFalse(battle.playCard("strike"));
    assertEquals(player.getCards(), battle.getHand());
    assertTrue(battle.playCard(upgraded.instanceId()));
    assertEquals(List.of(upgraded), battle.getDiscardPile());
  }
}
