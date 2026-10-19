package com.csse3200.game.ui.terminal.commands;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.enemy.IntentEffectType;
import com.csse3200.game.entities.Entity;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

/** Debug command: forceintent &lt;zero-based slot&gt; &lt;intent&gt;. */
public class ForceIntentCommand implements Command {
  private final BattleController battle;

  public ForceIntentCommand(BattleController battle) {
    this.battle = Objects.requireNonNull(battle, "battle cannot be null");
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (args == null || args.size() != 2) return false;
    try {
      int slot = Integer.parseInt(args.get(0));
      Entity enemy = battle.getEnemyInSlot(slot);
      if (enemy == null) return false;
      CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);
      if (stats == null || stats.isDead()) return false;
      String name = args.get(1).toLowerCase(Locale.ROOT);
      EnemyIntent intent =
          switch (name) {
            case "attack" -> EnemyIntent.attack(stats.getBaseAttack());
            case "defend" -> EnemyIntent.defend(5);
            case "silence" -> EnemyIntent.debuff(IntentEffectType.SILENCE, 1, 2);
            case "damage_on_card_play" ->
                EnemyIntent.debuff(IntentEffectType.DAMAGE_ON_CARD_PLAY, 3, 3);
            case "taunt" -> EnemyIntent.debuff(IntentEffectType.TAUNT, 0, 2);
            default -> null;
          };
      return intent != null && battle.forceNextEnemyIntent(slot, intent);
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
