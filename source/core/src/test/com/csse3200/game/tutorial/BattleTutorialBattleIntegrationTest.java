package com.csse3200.game.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.csse3200.game.components.cards.CardEffectHandler;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BattleTutorialBattleIntegrationTest {
  @Test
  void insufficientEnergyDoesNotAdvanceButSuccessfulPlayAndEndTurnDo() {
    Fixture fixture = new Fixture();
    fixture.start();
    fixture.energy.setCurrentEnergy(0);
    fixture.battle.submitCardPlayRequest(CardPlayRequest.self(fixture.cardId));
    assertEquals(BattleTutorialController.Step.PLAY_A_CARD, fixture.step());
    assertEquals(1, fixture.deck.getHandSize());

    fixture.energy.setCurrentEnergy(3);
    fixture.battle.submitCardPlayRequest(CardPlayRequest.self(fixture.cardId));
    assertEquals(BattleTutorialController.Step.END_TURN, fixture.step());
    assertEquals(2, fixture.energy.getCurrentEnergy());
    fixture.battle.endPlayerTurn();
    assertEquals(BattleTutorialController.Step.FREE_PLAY, fixture.step());
    assertEquals(3, fixture.energy.getCurrentEnergy());
    assertEquals(1, fixture.playerDeck.size());
    fixture.tutorial.close();
  }

  @Test
  void actualBattleVictoryReportsOnceAndPreservesOtherObserversAfterCleanup() {
    Fixture fixture = new Fixture();
    List<Boolean> normalOutcomes = new ArrayList<>();
    List<BattleTutorialController.Outcome> tutorialOutcomes = new ArrayList<>();
    fixture.battle.addBattleEndListener(normalOutcomes::add);
    fixture.tutorial.addOutcomeListener(tutorialOutcomes::add);
    fixture.start();
    fixture.enemy.getComponent(CombatStatsComponent.class).setHealth(0);
    fixture.battle.endPlayerTurn();
    assertEquals(List.of(BattleTutorialController.Outcome.WON), tutorialOutcomes);
    assertEquals(List.of(true), normalOutcomes);
    fixture.tutorial.close();

    fixture.enemy.getComponent(CombatStatsComponent.class).setHealth(10);
    fixture.battle.resetBattle();
    fixture.battle.start();
    fixture.enemy.getComponent(CombatStatsComponent.class).setHealth(0);
    fixture.battle.endPlayerTurn();
    assertEquals(List.of(true, true), normalOutcomes);
    assertEquals(1, tutorialOutcomes.size());
  }

  private static final class Fixture {
    private final EnergyComponent energy = new EnergyComponent(3);
    private final Entity player =
        new Entity().addComponent(new CombatStatsComponent(20, 0)).addComponent(energy);
    private final Entity enemy;
    private final PlayerDeck playerDeck;
    private final BattleDeck deck;
    private final BattleController battle;
    private final BattleTutorialController tutorial = new BattleTutorialController();
    private final String cardId;

    private Fixture() {
      EnemyBehaviourComponent behaviour = mock(EnemyBehaviourComponent.class);
      when(behaviour.rollIntent()).thenReturn(EnemyIntent.defend(1));
      when(behaviour.getCurrentIntent()).thenReturn(EnemyIntent.defend(1));
      enemy = new Entity().addComponent(new CombatStatsComponent(10, 0)).addComponent(behaviour);

      CardConfig defend = new CardConfig();
      defend.id = "defend";
      defend.name = "Defend";
      defend.cost = 1;
      defend.type = CardType.SKILL;
      defend.target = TargetType.SELF;
      defend.effects = new EffectConfig[] {new EffectConfig(EffectType.BLOCK, 5)};
      defend.texturePath = "images/cards/defend.png";
      CardLibrary cards = new CardLibrary(List.of(defend));
      playerDeck = new PlayerDeck(cards, List.of("defend"));
      deck = new BattleDeck(playerDeck);
      deck.drawCards(1);
      cardId = deck.getHand().get(0).instanceId();
      CardPlayService playService = new CardPlayService(cards, deck, energy);
      battle = new BattleController(player, List.of(enemy), new CardEffectHandler(), playService);
      tutorial.bindTo(battle);
    }

    private void start() {
      tutorial.start();
      battle.start();
      for (int i = 0; i < 7; i++) {
        assertTrue(tutorial.continueInformation());
      }
    }

    private BattleTutorialController.Step step() {
      return tutorial.getCurrentStep().orElseThrow();
    }
  }
}
