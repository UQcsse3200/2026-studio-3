package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardPlayRequest;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.combat.BattleEvent;
import com.csse3200.game.components.combat.BattlePhase;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.player.PlayerIntent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class BattleActionsTest {
  private BattleController controller;
  private Entity player;
  private Entity entity;

  @BeforeEach
  void setUp() {
    player = new Entity();
    player.addComponent(new CombatStatsComponent(20, 0));
    Entity enemy =
        new Entity()
            .addComponent(new CombatStatsComponent(20, 1))
            .addComponent(new EnemyBehaviourComponent("test"));
    controller = new BattleController(player, List.of(enemy));
    GdxGame game = mock(GdxGame.class);
    entity = new Entity().addComponent(new BattleActions(controller, game, realLibrary()));
    entity.create();
  }

  private static CardLibrary realLibrary() {
    return new CardLibrary(
        List.of(
            card(
                "strike",
                CardType.ATTACK,
                TargetType.SINGLE_ENEMY,
                new EffectConfig(EffectType.DAMAGE, 6)),
            card(
                "defend", CardType.SKILL, TargetType.SELF, new EffectConfig(EffectType.BLOCK, 5))));
  }

  private static CardConfig card(String id, CardType type, TargetType target, EffectConfig effect) {
    CardConfig config = new CardConfig();
    config.id = id;
    config.name = id;
    config.cost = 1;
    config.type = type;
    config.target = target;
    config.effects = new EffectConfig[] {effect};
    config.texturePath = "images/cards/" + id + ".png";
    return config;
  }

  @Test
  void shouldResolveAttackCardPlayDuringPlayerTurn() {
    advanceToPlayerTurn();
    List<String> played = new ArrayList<>();
    entity.getEvents().addListener("cardPlayed", (String name, String target) -> played.add(name));

    entity.getEvents().trigger("playCard", "strike", "bone_crawler");

    // Card was accepted and, with no resolution service wired, resolved straight away.
    assertEquals(List.of("strike"), played);
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
  }

  @Test
  void shouldStepThroughPlayerAttackPhaseWhenCardPlayed() {
    List<BattlePhase> phases = new ArrayList<>();
    entity.getEvents().addListener("phaseChange", (BattlePhase phase) -> phases.add(phase));
    advanceToPlayerTurn();

    entity.getEvents().trigger("playCard", "strike", "bone_crawler");

    assertTrue(phases.contains(BattlePhase.PLAYER_ATTACK));
  }

  @Test
  void shouldSubmitStrikeWithBoneCrawlerTarget() {
    BattleController mockController = mock(BattleController.class);
    GdxGame mockGame = mock(GdxGame.class);
    CardLibrary mockLibrary = mock(CardLibrary.class);
    CardConfig strike = new CardConfig();
    strike.id = "strike";
    strike.name = "Strike";
    strike.type = CardType.ATTACK;
    strike.effects = new EffectConfig[0];
    when(mockLibrary.getCard("strike")).thenReturn(Optional.of(strike));
    Entity battleUI =
        new Entity().addComponent(new BattleActions(mockController, mockGame, mockLibrary));
    battleUI.create();
    battleUI.getEvents().trigger("playCard", "strike", "bone_crawler");
    verify(mockController)
        .submitCardPlayRequest(new CardPlayRequest("strike", "bone_crawler"), PlayerIntent.ATTACK);
  }

  @Test
  void shouldRunEnemyPhaseWhenEndTurnSelected() {
    advanceToPlayerTurn();

    entity.getEvents().trigger("endTurnSelected");

    // End turn runs the enemy phase (enemy attacks for its base attack of 1) and hands control
    // back.
    assertEquals(BattlePhase.PLAYER_TURN, controller.getCurrentPhase());
    assertEquals(19, player.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void shouldIgnoreSelectionWhenCurrentPhaseCannotHandleIt() {
    entity.getEvents().trigger("playCard", "strike", "bone_crawler");

    assertEquals(BattlePhase.SETUP, controller.getCurrentPhase());
  }

  private void advanceToPlayerTurn() {
    controller.handle(BattleEvent.SETUP_COMPLETE);
  }
}
