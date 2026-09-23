package com.csse3200.game.components.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.play.CardPlayRequest;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffect;
import com.csse3200.game.components.cards.CardEffectHandler;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** End-to-end checks for enemy effects applied through the live battle controller path. */
@ExtendWith(GameExtension.class)
class LiveEnemyEffectsTest {
  private static final String ENEMY_ID = "enemy-1";

  @Test
  void pierceDamagesHealthWithoutChangingArmourOrBlock() {
    CombatStatsComponent stats = new CombatStatsComponent(30, 1);
    stats.setArmour(5);
    stats.setBlock(4);

    play(singleEffectCard("pierce", EffectType.PIERCE, 7), stats);

    assertEquals(23, stats.getHealth());
    assertEquals(5, stats.getArmour());
    assertEquals(4, stats.getBlock());
  }

  @Test
  void sunderReducesArmourWithoutGoingBelowZero() {
    CombatStatsComponent reducedStats = new CombatStatsComponent(30, 1);
    reducedStats.setArmour(5);
    play(singleEffectCard("sunder", EffectType.SUNDER, 3), reducedStats);
    assertEquals(2, reducedStats.getArmour());

    CombatStatsComponent clampedStats = new CombatStatsComponent(30, 1);
    clampedStats.setArmour(2);
    play(singleEffectCard("sunder", EffectType.SUNDER, 3), clampedStats);
    assertEquals(0, clampedStats.getArmour());
  }

  @Test
  void unsealTheBreachSundersBeforeDealingDamage() {
    CardLibrary library = configuredLibrary();
    CombatStatsComponent stats = new CombatStatsComponent(30, 1);
    stats.setArmour(3);

    play(library, "unseal_the_breach", stats);

    assertEquals(0, stats.getArmour());
    assertEquals(28, stats.getHealth());
  }

  @Test
  void poisonBladeDealsPiercingDamageAndAppliesPoison() {
    CardLibrary library = configuredLibrary();
    CombatStatsComponent stats = new CombatStatsComponent(30, 1);
    stats.setArmour(5);
    stats.setBlock(4);

    play(library, "poison_blade", stats);

    assertEquals(20, stats.getHealth());
    assertEquals(5, stats.getArmour());
    assertEquals(4, stats.getBlock());
    StatusEffect poison = stats.getStatusEffect(EffectType.POISON.name());
    assertNotNull(poison);
    assertEquals(4, poison.getValue());
    assertEquals(2, poison.getDuration());
  }

  private static CardConfig singleEffectCard(String id, EffectType effectType, int value) {
    CardConfig card = new CardConfig();
    card.id = id;
    card.name = id;
    card.description = "Test card";
    card.cost = 1;
    card.type = CardType.ATTACK;
    card.target = TargetType.SINGLE_ENEMY;
    card.effects = new EffectConfig[] {new EffectConfig(effectType, value)};
    card.texturePath = "images/cards/" + id + ".png";
    return card;
  }

  private static CardLibrary configuredLibrary() {
    return new CardLibrary(CardConfigLoader.loadCards());
  }

  private static void play(CardConfig card, CombatStatsComponent enemyStats) {
    play(new CardLibrary(List.of(card)), card.id, enemyStats);
  }

  private static void play(CardLibrary library, String cardId, CombatStatsComponent enemyStats) {
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(30, 0))
            .addComponent(new EnergyComponent(10));
    Entity enemy =
        new Entity().addComponent(enemyStats).addComponent(new EnemyBehaviourComponent("test"));
    BattleDeck deck = new BattleDeck(new PlayerDeck(library, List.of(cardId)));
    String instanceId = deck.drawCards(1).get(0).instanceId();
    CardEffectHandler effectHandler = new CardEffectHandler(Map.of(ENEMY_ID, enemy));
    CardPlayService cardPlayService =
        new CardPlayService(library, deck, player.getComponent(EnergyComponent.class));
    BattleController controller =
        new BattleController(player, List.of(enemy), effectHandler, cardPlayService);
    controller.start();

    assertTrue(controller.submitCardPlayRequest(CardPlayRequest.singleEnemy(instanceId, ENEMY_ID)));
  }
}
