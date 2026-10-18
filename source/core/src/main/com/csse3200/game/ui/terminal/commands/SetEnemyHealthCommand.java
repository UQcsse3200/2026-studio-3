package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.Objects;

/** Debug command: setenemyhp &lt;zero-based slot&gt; &lt;health&gt;. */
public class SetEnemyHealthCommand implements Command {
  private final BattleController battle;

  public SetEnemyHealthCommand(BattleController battle) {
    this.battle = Objects.requireNonNull(battle, "battle cannot be null");
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (args == null || args.size() != 2) return false;
    try {
      int slot = Integer.parseInt(args.get(0));
      int health = Integer.parseInt(args.get(1));
      if (health < 0) return false;
      Entity enemy = battle.getEnemyInSlot(slot);
      if (enemy == null) return false;
      CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
      if (stats == null) return false;
      // setHealth emits updateHealth, which refreshes the existing health display.
      stats.setHealth(health);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
