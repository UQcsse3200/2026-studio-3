package com.csse3200.game.cards.deck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.CardUpgradeConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.runtime.CardInstance;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Regression coverage for stable per-copy identity across persistent and battle decks. */
class DeckIdentityTest {
  private final CardLibrary cards = new CardLibrary(List.of(strikeConfig()));
  private final CardInstance base = new CardInstance("strike-base", "strike", 0);
  private final CardInstance upgraded = new CardInstance("strike-plus", "strike", 1);

  @Test
  void playerDeckAllowsDuplicateDefinitionsButRejectsDuplicateInstanceIds() {
    PlayerDeck deck = PlayerDeck.fromInstances(cards, List.of(base, upgraded));

    assertEquals(2, deck.countByCardId("strike"));
    assertThrows(IllegalArgumentException.class, () -> deck.addCard(base));
  }

  @Test
  void playerDeckRemovesOnlyTheSelectedDuplicate() {
    PlayerDeck deck = PlayerDeck.fromInstances(cards, List.of(base, upgraded));

    assertTrue(deck.removeCard(upgraded.instanceId()));

    assertEquals(List.of(base), deck.getCards());
  }

  @Test
  void playerDeckUpgradesOnlyTheSelectedCopyAndPreservesIdentity() {
    CardInstance sibling = new CardInstance("strike-sibling", "strike", 0);
    PlayerDeck deck = PlayerDeck.fromInstances(cards, List.of(base, sibling));

    CardInstance result = deck.upgradeCard(sibling.instanceId());

    assertEquals(List.of(base, result), deck.getCards());
    assertEquals(sibling.instanceId(), result.instanceId());
    assertTrue(result.isUpgraded());
    assertFalse(deck.getCards().getFirst().isUpgraded());
  }

  @Test
  void playerDeckCopyPreservesIdentityAndUpgradeStateButNotCollectionMutation() {
    PlayerDeck original = PlayerDeck.fromInstances(cards, List.of(base, upgraded));
    PlayerDeck copy = original.copy();

    original.removeCard(base.instanceId());

    assertEquals(List.of(upgraded), original.getCards());
    assertEquals(List.of(base, upgraded), copy.getCards());
  }

  @Test
  void battleDeckMovesOnlyTheSelectedDuplicate() {
    BattleDeck battle = new BattleDeck(PlayerDeck.fromInstances(cards, List.of(base, upgraded)));
    battle.drawCards(2);

    assertTrue(battle.playCard(upgraded.instanceId()));

    assertEquals(List.of(base), battle.getHand());
    assertEquals(List.of(upgraded), battle.getDiscardPile());
  }

  @Test
  void definitionIdCannotSelectEitherOwnedCopy() {
    BattleDeck battle = new BattleDeck(PlayerDeck.fromInstances(cards, List.of(base, upgraded)));
    battle.drawCards(2);

    assertFalse(battle.playCard("strike"));
    assertEquals(List.of(base, upgraded), battle.getHand());
    assertTrue(battle.getDiscardPile().isEmpty());
  }

  @Test
  void identityAndUpgradeStateSurviveDiscardReshuffleAndRedraw() {
    BattleDeck battle = new BattleDeck(PlayerDeck.fromInstances(cards, List.of(upgraded)));
    assertEquals(upgraded, battle.drawOne());
    assertTrue(battle.discardCard(upgraded.instanceId()));

    assertEquals(upgraded, battle.drawOne());
    assertEquals(List.of(upgraded), battle.getHand());
  }

  private static CardConfig strikeConfig() {
    CardConfig card = new CardConfig();
    card.id = "strike";
    card.name = "Strike";
    card.description = "Deal 6 damage.";
    card.cost = 1;
    card.type = CardType.ATTACK;
    card.rarity = Rarity.COMMON;
    card.target = TargetType.SINGLE_ENEMY;
    card.effects = new EffectConfig[] {new EffectConfig(EffectType.DAMAGE, 6)};
    card.texturePath = "images/cards/strike.png";
    CardUpgradeConfig upgrade = new CardUpgradeConfig();
    upgrade.name = "Strike+";
    upgrade.description = "Deal 12 damage.";
    upgrade.cost = 2;
    upgrade.rarity = Rarity.COMMON;
    upgrade.effects = new EffectConfig[] {new EffectConfig(EffectType.DAMAGE, 12)};
    card.upgrade = upgrade;
    return card;
  }
}
