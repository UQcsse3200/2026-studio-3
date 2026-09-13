package com.csse3200.game.cards.play.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.play.CardPlayResult;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class Team3CardPlayAdapterTest {

  @Test
  void shouldResolveAndReturnOneResultWithoutApplyingEnemyEffects() {
    CardConfig strike = strike();
    CardConfig defend = defend();

    CardLibrary cards = new CardLibrary(List.of(strike, defend));

    BattleDeck deck =
            new BattleDeck(
                    new PlayerDeck(
                            cards,
                            List.of("strike", "defend")));

    // Only draw the first card. defend remains in the draw pile as the replacement.
    deck.drawOne();

    EnergyComponent energy = new EnergyComponent(3);
    CombatStatsComponent playerStats = new CombatStatsComponent(10, 1);
    Team7PlayerStateAdapter player =
            new Team7PlayerStateAdapter(energy, playerStats);

    CombatStatsComponent enemyStats = new CombatStatsComponent(10, 1);
    Entity enemyEntity = new Entity().addComponent(enemyStats);
    Team1EnemyStateAdapter enemies =
            new Team1EnemyStateAdapter(Map.of("enemy-1", enemyEntity));

    CardPlayService playService =
            new CardPlayService(cards, deck, energy, player, enemies);

    Team3CardPlayAdapter adapter =
            new Team3CardPlayAdapter(cards, playService);

    Entity battleFlow = new Entity().addComponent(adapter);

    AtomicReference<CardPlayResult> observed = new AtomicReference<>();
    battleFlow
            .getEvents()
            .addListener(
                    Team3CardPlayAdapter.CARD_PLAY_RESULT_EVENT,
                    observed::set);

    battleFlow.create();

    battleFlow
            .getEvents()
            .trigger(
                    Team3CardPlayAdapter.PLAY_CARD_EVENT,
                    "strike",
                    "enemy-1");

    assertNotNull(observed.get());
    assertTrue(observed.get().success());
    assertEquals(2, energy.getCurrentEnergy());
    assertEquals(6, observed.get().enemyEffects().get(0).value());

    // The adapter/service only resolves the effect; it does not apply it
    // directly to the enemy entity.
    assertEquals(10, enemyStats.getHealth());

    // strike was discarded and defend was drawn as its replacement.
    assertEquals(List.of("defend"), deck.getHand());
    assertEquals(List.of("strike"), deck.getDiscardPile());
  }


  private static CardConfig strike() {
    CardConfig card = new CardConfig();
    card.id = "strike";
    card.name = "Strike";
    card.description = "Deal damage";
    card.cost = 1;
    card.type = CardType.ATTACK;
    card.rarity = Rarity.COMMON;
    card.target = TargetType.SINGLE_ENEMY;
    card.texturePath = "images/cards/strike.png";
    card.effects = new EffectConfig[] {new EffectConfig(EffectType.DAMAGE, 6)};
    return card;
  }

  private static CardConfig defend() {
    CardConfig card = new CardConfig();
    card.id = "defend";
    card.name = "Defend";
    card.description = "Add armor";
    card.cost = 1;
    card.type = CardType.SKILL;
    card.rarity = Rarity.COMMON;
    card.target = TargetType.SELF;
    card.texturePath = "images/cards/defend.png";
    card.effects = new EffectConfig[] {new EffectConfig(EffectType.HEAL, 3)};
    return card;
  }
}
