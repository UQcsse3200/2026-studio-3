package com.csse3200.game.components.player;

import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.StatusEffectCalculator;
import com.csse3200.game.entities.Entity;

/** What it does: resolves player attack and defend against the shared CombatStatsComponent */
public class PlayerBehaviourComponent extends Component {

  /**
   * Attack the target with cardDamage calculation. The card damage plus player's base attack is
   * multiplied by the player's modifier and enemy's modifier.
   *
   * @param target the entity being attacked
   * @param cardDamage the base damage value from the card
   */
  public void attack(Entity target, int cardDamage) {
    if (target == null) {
      return;
    }
    CombatStatsComponent targetStats = target.getComponent(CombatStatsComponent.class);
    CombatStatsComponent playerStats = entity.getComponent(CombatStatsComponent.class);
    if (targetStats == null || playerStats == null) {
      return;
    }
    int damage =
        Math.round(
            ((cardDamage + playerStats.getBaseAttack())
                * StatusEffectCalculator.getOutgoingDamageModifier(playerStats)
                * StatusEffectCalculator.getIncomingDamageModifier(targetStats)));
    targetStats.takeDamage(damage);
  }

  /**
   * Add the armor amount to the player stats.
   *
   * @param amount armor amount
   */
  public void defend(int amount) {
    CombatStatsComponent playerStats = entity.getComponent(CombatStatsComponent.class);
    if (playerStats == null) {
      return;
    }
    playerStats.addArmor(amount);
  }
}
