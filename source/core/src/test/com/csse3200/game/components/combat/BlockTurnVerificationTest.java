package com.csse3200.game.components.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.player.PlayerActions;
import com.csse3200.game.entities.Entity;
import java.util.List;
import org.junit.jupiter.api.Test;

class BlockTurnVerificationTest {
  @Test
  void leftoverBlockShouldProtectDuringEnemyActionThenClearWhileArmourRemains() {
    CombatStatsComponent playerStats = new CombatStatsComponent(100, 0);
    Entity player = new Entity().addComponent(playerStats).addComponent(new PlayerActions());
    EnemyBehaviourComponent behaviour = mock(EnemyBehaviourComponent.class);
    Entity enemy = mock(Entity.class);
    CombatStatsComponent enemyStats = new CombatStatsComponent(30, 0);
    when(enemy.getComponent(CombatStatsComponent.class)).thenReturn(enemyStats);
    when(enemy.getComponent(EnemyBehaviourComponent.class)).thenReturn(behaviour);
    when(behaviour.rollIntent()).thenReturn(EnemyIntent.defend(1));
    doAnswer(
            invocation -> {
              assertEquals(8, playerStats.getBlock());
              playerStats.takeDamage(3);
              assertEquals(5, playerStats.getBlock());
              assertEquals(4, playerStats.getArmour());
              assertEquals(100, playerStats.getHealth());
              return null;
            })
        .when(behaviour)
        .executeIntent(player);

    BattleController battle = new BattleController(player, List.of(enemy));
    battle.start();
    playerStats.addBlock(8);
    playerStats.addArmour(4);
    battle.endPlayerTurn();

    assertEquals(BattlePhase.PLAYER_TURN, battle.getCurrentPhase());
    assertEquals(0, playerStats.getBlock());
    assertEquals(4, playerStats.getArmour());
    assertEquals(100, playerStats.getHealth());
  }

  @Test
  void unusedBlockShouldClearEvenWhenEnemyDealsNoDamage() {
    CombatStatsComponent playerStats = new CombatStatsComponent(100, 0);
    Entity player = new Entity().addComponent(playerStats).addComponent(new PlayerActions());
    EnemyBehaviourComponent behaviour = mock(EnemyBehaviourComponent.class);
    Entity enemy = mock(Entity.class);
    when(enemy.getComponent(CombatStatsComponent.class))
        .thenReturn(new CombatStatsComponent(30, 0));
    when(enemy.getComponent(EnemyBehaviourComponent.class)).thenReturn(behaviour);
    when(behaviour.rollIntent()).thenReturn(EnemyIntent.defend(1));
    BattleController battle = new BattleController(player, List.of(enemy));
    battle.start();
    playerStats.addBlock(12);
    playerStats.addArmour(4);
    battle.endPlayerTurn();
    assertEquals(0, playerStats.getBlock());
    assertEquals(4, playerStats.getArmour());
    battle.endPlayerTurn();
    assertEquals(0, playerStats.getBlock());
    assertEquals(4, playerStats.getArmour());
  }
}
