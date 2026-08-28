package com.csse3200.game.components;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component used to store information related to combat such as health, attack, armor and status
 * effects. Any entities which engage it combat should have an instance of this class registered.
 * This class can be extended for more specific combat needs.
 */
public class CombatStatsComponent extends Component {

  private static final Logger logger = LoggerFactory.getLogger(CombatStatsComponent.class);
  private static final String EVT_IS_DEAD = "entityIsDead";
  private static final String EVT_MAX_HEALTH = "updateMaxHealth";
  private int health;
  private int baseAttack;
  private int maxHealth;
  private int armor = 0;
  private final Map<String, StatusEffect> statusEffects = new HashMap<>();

  public CombatStatsComponent(int health, int baseAttack) {
    setMaxHealth(health);
    setHealth(health);
    setBaseAttack(baseAttack);
  }

  private void updateHealth() {
    if (entity != null) {
      entity.getEvents().trigger("updateHealth", this.health);
    }
  }

  /**
   * Returns true if the entity's has 0 health, otherwise false.
   *
   * @return is player dead
   */
  public Boolean isDead() {
    return this.health == 0;
  }

  /**
   * Returns the entity's health.
   *
   * @return entity's health
   */
  public int getHealth() {
    return this.health;
  }

  /**
   * Returns the entity's max health.
   *
   * @return entity's max health
   */
  public int getMaxHealth() {
    return this.maxHealth;
  }

  /**
   * Sets the entity's health. Health has a minimum bound of 0.
   *
   * @param health health
   */
  public void setHealth(int health) {
    if (health >= 0) {
      this.health = Math.min(health, this.maxHealth);
    } else {
      this.health = 0;
    }
    updateHealth();
  }

  /**
   * Heals. Health has a maximum bound of the max health
   *
   * @param health health
   */
  public void heal(int health) {
    if (health > 0) {
      setHealth(Math.min(this.health + health, this.maxHealth));
    }
  }

  /**
   * Damage the entity's health. Incoming damage is first modified by any active status effects
   * (e.g. Vulnerable), then absorbed by armor, and the remainder is applied to health. If health
   * reaches 0 the entity dies.
   *
   * @param damage damage
   */
  public void takeDamage(int damage) {
    if (damage >= 0 && !isDead()) {
      int modifiedDamage = applyStatusEffectDamageModifiers(damage);
      int remainingDamage = absorbDamageWithArmor(modifiedDamage);
      setHealth(Math.max(this.health - remainingDamage, 0));
      if (entity != null && isDead()) {
        entity.getEvents().trigger("entityIsDead");
      }
    }
  }

  /**
   * A setter function for maxHealth, contains a safegaurd to avoid MaxHealth going lower than 1
   * send an update to every listener is changed to ensure real time changes updated.
   *
   * @param healthAmount to be set as the MaxHealth
   */
  public void setMaxHealth(int healthAmount) {
    this.maxHealth =
        Math.max(healthAmount, 1); // Use math.max to restrict the maxhealth from going below 1
    if (entity != null) {
      entity
          .getEvents()
          .trigger(
              "updateMaxHealth",
              this.maxHealth); // this line (basically tells other that is listening to this that
      // the value is changed)
    }
  }

  /**
   * A function to increase the max health. It uses setMaxHealth function to avoid redundancy
   *
   * @param healthAmount to be set as the MaxHealth
   */
  public void addMaxHealth(int healthAmount) {
    setMaxHealth(healthAmount + this.maxHealth);
  }

  /**
   * Returns the entity's base attack damage.
   *
   * @return base attack damage
   */
  public int getBaseAttack() {
    return baseAttack;
  }

  /**
   * Sets the entity's attack damage. Attack damage has a minimum bound of 0.
   *
   * @param attack Attack damage
   */
  public void setBaseAttack(int attack) {
    if (attack >= 0) {
      this.baseAttack = attack;
    } else {
      logger.error("Can not set base attack to a negative attack value");
    }
  }

  /**
   * Returns the entity's base attack damage. Unused will remove in later sprint
   *
   * @return base attack damage
   */
  public void addHealth(int health) {
    setHealth(this.health + health);
  }

  /**
   * Returns the entity's base attack damage. Unused will remove in later sprint
   *
   * @return base attack damage
   */
  public void hit(CombatStatsComponent attacker) {
    int newHealth = getHealth() - attacker.getBaseAttack();
    setHealth(newHealth);
  }

  /**
   * Returns the entity's current armor value.
   *
   * @return armor
   */
  public int getArmor() {
    return armor;
  }

  /**
   * Sets the entity's armor. Armor is clamped to a minimum of 0.
   *
   * @param armor armor value
   */
  public void setArmor(int armor) {
    this.armor = Math.max(armor, 0);
    if (entity != null) {
      entity.getEvents().trigger("updateArmor", this.armor);
    }
  }

  /**
   * Adds armor to the entity. Non-positive amounts are ignored.
   *
   * @param amount amount of armor to add
   */
  public void addArmor(int amount) {
    if (amount <= 0) {
      return;
    }
    setArmor(this.armor + amount);
  }

  /** Clears all armor from the entity, setting it to 0. */
  public void clearArmor() {
    setArmor(0);
  }

  /**
   * Uses current armor to absorb as much of the incoming damage as possible, reducing armor
   * accordingly, and returns whatever damage remains to be applied to health.
   *
   * @param incomingDamage damage to be absorbed (after status-effect modifiers)
   * @return damage remaining after armor absorption
   */
  public int absorbDamageWithArmor(int incomingDamage) {
    if (incomingDamage <= 0) {
      return 0;
    }
    int absorbed = Math.min(armor, incomingDamage);
    if (absorbed > 0) {
      setArmor(armor - absorbed);
    }
    return incomingDamage - absorbed;
  }

  /**
   * Applies a status effect to this entity. If an effect with the same id is already active, it is
   * overwritten by the new one (design choice: overwrite, not stack. To be confirmed with Team 5/6
   * if card design expects stacking behaviour instead).
   *
   * @param effect status effect to apply
   */
  public void applyStatusEffect(StatusEffect effect) {
    if (effect == null) {
      return;
    }
    statusEffects.put(effect.getEffectId(), effect);
    if (entity != null) {
      entity.getEvents().trigger("statusEffectApplied", effect.getEffectId());
    }
  }

  /**
   * Returns the active status effect with the given id, or null if not present.
   *
   * @param effectId status effect identifier
   * @return active StatusEffect, or null
   */
  public StatusEffect getStatusEffect(String effectId) {
    return statusEffects.get(effectId);
  }

  /**
   * Returns true if a status effect with the given id is currently active.
   *
   * @param effectId status effect identifier
   * @return whether the effect is active
   */
  public boolean hasStatusEffect(String effectId) {
    return statusEffects.containsKey(effectId);
  }

  /**
   * Explicitly removes a status effect from this entity, if present.
   *
   * @param effectId status effect identifier
   */
  public void removeStatusEffect(String effectId) {
    if (statusEffects.remove(effectId) != null && entity != null) {
      entity.getEvents().trigger("statusEffectRemoved", effectId);
    }
  }

  /**
   * Ticks down the duration of all active status effects by one and removes any that have expired.
   * This method's internal logic (tick/expire/cleanup) is self-contained. IMPORTANT - external
   * dependency: this method must be called exactly once per turn for durations to mean "number of
   * turns". WHEN it gets called is not yet wired up - it depends on Team 3's turn/battle-sequence
   * event, which is not confirmed yet.
   */
  public void updateStatusEffects() {
    statusEffects
        .entrySet()
        .removeIf(
            entry -> {
              boolean expired = entry.getValue().tickAndCheckExpired();
              if (expired && entity != null) {
                entity.getEvents().trigger("statusEffectRemoved", entry.getKey());
              }
              return expired;
            });
  }

  /**
   * PLACEHOLDER / NOT FINAL: applies incoming-damage modifiers from active status effects to a raw
   * damage amount, before armor absorption. This currently only demonstrates the mechanism using a
   * single hard-coded example effect ("vulnerable", +50% incoming damage) so that the
   * armor+status-effect pipeline in takeDamage() can be built and tested end-to-end. The actual set
   * of supported status effect types, and how each one's effectValue should be interpreted
   * (percentage vs flat amount, multiplicative vs additive, etc.), still needs to be confirmed with
   * Team 5 (card effects) and Team 6 (card data model). This method WILL need to be
   * rewritten/extended once that is confirmed; do not treat the current formula as final.
   *
   * @param rawDamage the un-modified incoming damage
   * @return damage after status-effect modifiers are applied
   */
  private int applyStatusEffectDamageModifiers(int rawDamage) {
    // Example only - pending confirmation with Team 5/6 on final effect calculation rules.
    StatusEffect vulnerable = statusEffects.get("vulnerable");
    if (vulnerable != null) {
      rawDamage = Math.round(rawDamage * (1 + vulnerable.getEffectValue()));
    }
    return rawDamage;
  }
}
