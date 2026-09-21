package com.csse3200.game.cards.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.CardUpgradeConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.components.player.EnergyComponent;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Regression coverage for playing one exact base/upgraded copy among duplicate definitions. */
class InstanceCardPlayTest {
  private final CardInstance base = new CardInstance("strike-base", "strike", 0);
  private final CardInstance upgraded = new CardInstance("strike-plus", "strike", 1);
  private BattleDeck deck;
  private EnergyComponent energy;
  private CardPlayService service;

  @BeforeEach
  void setUp() {
    CardLibrary library = new CardLibrary(List.of(strikeConfig()));
    deck = new BattleDeck(PlayerDeck.fromInstances(library, List.of(base, upgraded)));
    deck.drawCards(2);
    energy = new EnergyComponent(2);
    service = new CardPlayService(library, deck, energy);
  }

  @Test
  void playsSelectedUpgradeUsingResolvedCostValuesAndDuration() {
    CardPlayResult result =
        service.playCard(CardPlayRequest.singleEnemy(upgraded.instanceId(), "enemy-1"));

    assertTrue(result.success());
    assertEquals(upgraded.instanceId(), result.instanceId());
    assertEquals("strike", result.cardId());
    assertEquals(2, result.energyCost());
    assertEquals(0, energy.getCurrentEnergy());
    assertEquals(List.of(12, 4), result.enemyEffects().stream().map(e -> e.value()).toList());
    assertEquals(List.of(0, 5), result.enemyEffects().stream().map(e -> e.duration()).toList());
    assertEquals(List.of(base), result.updatedHand());
    assertEquals(List.of(upgraded), result.updatedDiscardPile());
  }

  @Test
  void unaffordableUpgradeDoesNotPreventPlayingItsBaseSibling() {
    energy.setCurrentEnergy(1);

    CardPlayResult denied =
        service.playCard(CardPlayRequest.singleEnemy(upgraded.instanceId(), "enemy-1"));
    CardPlayResult played =
        service.playCard(CardPlayRequest.singleEnemy(base.instanceId(), "enemy-1"));

    assertFalse(denied.success());
    assertEquals(CardPlayFailureReason.NOT_ENOUGH_ENERGY, denied.failureReason());
    assertTrue(played.success());
    assertEquals(6, played.enemyEffects().getFirst().value());
    assertEquals(List.of(upgraded), deck.getHand());
    assertEquals(List.of(base), deck.getDiscardPile());
  }

  @Test
  void definitionIdCannotSelectEitherDuplicate() {
    CardPlayResult result = service.playCard(CardPlayRequest.singleEnemy("strike", "enemy-1"));

    assertFalse(result.success());
    assertEquals(CardPlayFailureReason.CARD_NOT_IN_HAND, result.failureReason());
    assertEquals(List.of(base, upgraded), deck.getHand());
    assertEquals(2, energy.getCurrentEnergy());
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
    upgrade.description = "Deal 12 damage and apply poison.";
    upgrade.cost = 2;
    upgrade.rarity = Rarity.COMMON;
    upgrade.effects =
        new EffectConfig[] {
          new EffectConfig(EffectType.DAMAGE, 12), new EffectConfig(EffectType.POISON, 4, 5)
        };
    card.upgrade = upgrade;
    return card;
  }
}
