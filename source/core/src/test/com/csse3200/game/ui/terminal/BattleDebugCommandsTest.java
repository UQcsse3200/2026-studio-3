package com.csse3200.game.ui.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.EnemyStatsComponent;
import com.csse3200.game.components.enemy.IntentEffectType;
import com.csse3200.game.components.enemy.IntentType;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.ui.terminal.commands.ForceIntentCommand;
import com.csse3200.game.ui.terminal.commands.SetEnemyHealthCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BattleDebugCommandsTest {
  private CombatStatsComponent enemyStats;
  private EnemyBehaviourComponent behaviour;
  private BattleController battle;

  @BeforeEach
  void setUp() {
    Entity player = new Entity().addComponent(new CombatStatsComponent(100, 0));
    player.create();
    enemyStats = new CombatStatsComponent(150, 14);
    behaviour = new EnemyBehaviourComponent("cycle_attack_defend");
    Entity enemy =
        new Entity()
            .addComponent(enemyStats)
            .addComponent(new EnemyStatsComponent("Boss"))
            .addComponent(behaviour);
    enemy.create();
    battle = new BattleController(player, List.of(enemy));
    battle.start();
  }

  @Test
  void setEnemyHpUpdatesHealthAndEmitsRefreshEvent() {
    AtomicInteger displayedHealth = new AtomicInteger(-1);
    battle
        .getEnemyInSlot(0)
        .getEvents()
        .addListener("updateHealth", (Integer hp, Integer max) -> displayedHealth.set(hp));

    assertTrue(new SetEnemyHealthCommand(battle).action(args("0", "40")));

    assertEquals(40, enemyStats.getHealth());
    assertEquals(40, displayedHealth.get());
  }

  @Test
  void setEnemyHpRejectsInvalidArgumentsWithoutMutation() {
    SetEnemyHealthCommand command = new SetEnemyHealthCommand(battle);
    assertFalse(command.action(args("1", "40")));
    assertFalse(command.action(args("-1", "40")));
    assertFalse(command.action(args("x", "40")));
    assertFalse(command.action(args("0", "x")));
    assertFalse(command.action(args("0", "-1")));
    assertFalse(command.action(args("0")));
    assertEquals(150, enemyStats.getHealth());
  }

  @Test
  void forceIntentAppliesOnlyToTheUpcomingEnemyAction() {
    ForceIntentCommand command = new ForceIntentCommand(battle);
    assertTrue(command.action(args("0", "silence")));
    assertEquals(IntentEffectType.SILENCE, behaviour.getCurrentIntent().getEffectType());

    battle.endPlayerTurn();

    assertTrue(battle.playerHasStatusEffect(IntentEffectType.SILENCE.name()));
    assertEquals(IntentType.DEFEND, behaviour.getCurrentIntent().getType());
  }

  @Test
  void forceIntentMapsSupportedNamesAndRejectsInvalidInputs() {
    ForceIntentCommand command = new ForceIntentCommand(battle);
    assertTrue(command.action(args("0", "attack")));
    assertEquals(EnemyIntent.attack(14), behaviour.getCurrentIntent());
    assertTrue(command.action(args("0", "defend")));
    assertEquals(IntentType.DEFEND, behaviour.getCurrentIntent().getType());
    assertTrue(command.action(args("0", "damage_on_card_play")));
    assertEquals(
        IntentEffectType.DAMAGE_ON_CARD_PLAY, behaviour.getCurrentIntent().getEffectType());
    assertTrue(command.action(args("0", "taunt")));
    assertEquals(IntentEffectType.TAUNT, behaviour.getCurrentIntent().getEffectType());
    EnemyIntent valid = behaviour.getCurrentIntent();
    assertFalse(command.action(args("0", "unknown")));
    assertFalse(command.action(args("1", "attack")));
    assertFalse(command.action(args("x", "attack")));
    assertFalse(command.action(args("0")));
    assertEquals(valid, behaviour.getCurrentIntent());
  }

  private static ArrayList<String> args(String... values) {
    return new ArrayList<>(List.of(values));
  }
}
