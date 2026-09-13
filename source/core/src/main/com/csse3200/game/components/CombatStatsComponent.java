package com.csse3200.game.components;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component used to store information related to combat such as health, attack, armor, block and
 * status effects. Any entities which engage it combat should have an instance of this class
 * registered. This class can be extended for more specific combat needs. SCOPE NOTE: this class
 * owns the generic mechanics of armor, block and status effects - how they are stored, applied,
 * queried, removed and expired. It deliberately does NOT own the specific calculation rules for any
 * individual effect (e.g. exactly how "Vulnerable" or "Strength" changes a number). Those
 * calculations are owned by whichever system/teammate needs them. ARMOR vs BLOCK: these are two
 * distinct mechanics, not two names for the same thing. Armor is a permanent damage-reduction pool.
 * It persists until consumed by incoming damage or explicitly cleared via clearArmor() - it does
 * not reset automatically at any point in the turn cycle. Block is a per-turn damage-reduction
 * pool, matching the "Slay the Spire" style block mechanic (Team 6). It is intended to reset to 0
 * once per turn via resetBlock(), regardless of whether it was consumed. TODO: exact reset timing
 * (start vs end of turn) is not yet wired up - depends on Team 3's turn/battle-sequence event.
 */
public class CombatStatsComponent extends Component {

  private static final Logger logger = LoggerFactory.getLogger(CombatStatsComponent.class);
  private static final String EVT_IS_DEAD = "entityIsDead";
  private static final String EVT_MAX_HEALTH = "updateMaxHealth";
  private int health;
  private int baseAttack;
  private int maxHealth;
  private int armor = 0;
  private int block = 0;
  private final Map<String, StatusEffect> statusEffects = new HashMap<>();

  /**
   * Entity's base constructor
   *
   * @param health entity's health
   * @param baseAttack entity's base attack
   */
  public CombatStatsComponent(int health, int baseAttack) {
    setMaxHealth(health);
    setHealth(health);
    setBaseAttack(baseAttack);
  }

  /**
   * Overload constructor with maxHealth as the third param
   *
   * @param health entity's health
   * @param baseAttack entity's base attack
   * @param maxHealth entity's max Health
   */
  public CombatStatsComponent(int health, int baseAttack, int maxHealth) {
    setMaxHealth(maxHealth);
    setHealth(health);
    setBaseAttack(baseAttack);
  }

  private void updateHealth() {
    if (entity != null) {
      entity.getEvents().trigger("updateHealth", this.health, this.maxHealth);
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
   * Damage the entity's health. Incoming damage is first absorbed by armor, and the remainder is
   * applied to health. If health reaches 0 the entity dies.
   *
   * <p>NOTE: this method does NOT apply any status-effect-based damage modifiers (e.g. Vulnerable).
   * Callers that need a status effect to change the amount of incoming damage should calculate the
   * final damage value themselves (e.g. by reading the relevant StatusEffect via getStatusEffect())
   * before calling this method. This keeps this class limited to armor/status-effect storage and
   * lifecycle management, not the specific calculation rules for any individual effect type.
   *
   * @param damage damage
   */
  public void takeDamage(int damage) {
    if (damage >= 0 && !isDead()) {
      int afterBlock = absorbDamageWithBlock(damage);
      int remainingDamage = absorbDamageWithArmor(afterBlock);
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
   * Unused will remove in later sprint
   *
   * @param health entity's health
   */
  public void addHealth(int health) {
    setHealth(this.health + health);
  }

  /**
   * Unused will remove in later sprint
   *
   * @param attacker attacker parameter
   */
  public void hit(CombatStatsComponent attacker) {
    takeDamage(attacker.getBaseAttack());
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

  // Block - per-turn damage reduction pool (Team 6's "Slay the Spire" style block)
  /**
   * Returns the entity's current block value.
   *
   * @return block
   */
  public int getBlock() {
    return block;
  }

  /**
   * Sets the entity's block. Block is clamped to a minimum of 0.
   *
   * @param block block value
   */
  public void setBlock(int block) {
    this.block = Math.max(block, 0);
    if (entity != null) {
      entity.getEvents().trigger("updateBlock", this.block);
    }
  }

  /**
   * Adds block to the entity. Non-positive amounts are ignored.
   *
   * @param amount amount of block to add
   */
  public void addBlock(int amount) {
    if (amount <= 0) {
      return;
    }
    setBlock(this.block + amount);
  }

  /**
   * Resets block to 0. Intended to be called once per turn, regardless of whether the block was
   * consumed. TODO: wire this up to Team 3's turn event, same as updateStatusEffects() - timing
   * (start vs end of turn) still needs confirmation.
   */
  public void resetBlock() {
    setBlock(0);
  }

  /**
   * Uses current block to absorb as much of the incoming damage as possible, reducing block
   * accordingly, and returns whatever damage remains.
   *
   * @param incomingDamage damage to be absorbed
   * @return damage remaining after block absorption
   */
  public int absorbDamageWithBlock(int incomingDamage) {
    if (incomingDamage <= 0) {
      return 0;
    }
    int absorbed = Math.min(block, incomingDamage);
    if (absorbed > 0) {
      setBlock(block - absorbed);
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
    statusEffects.put(effect.getType(), effect);
    if (entity != null) {
      entity.getEvents().trigger("statusEffectApplied", effect.getType());
    }
  }

  /**
   * Convenience overload of applyStatusEffect that constructs the StatusEffect internally. Provided
   * so callers can apply a status effect without constructing a StatusEffect object themselves.
   *
   * @param type identifier for the effect, e.g. "VULNERABLE"
   * @param value magnitude of the effect
   * @param duration number of turns the effect remains active for; 0 or fewer means permanent
   */
  public void applyStatusEffect(String type, int value, int duration) {
    applyStatusEffect(new StatusEffect(type, value, duration));
  }

  /**
   * Returns the active status effect with the given type, or null if not present.
   *
   * @param type status effect type identifier
   * @return active StatusEffect, or null
   */
  public StatusEffect getStatusEffect(String type) {
    return statusEffects.get(type);
  }

  /**
   * Returns true if a status effect with the given type is currently active.
   *
   * @param type status effect type identifier
   * @return whether the effect is active
   */
  public boolean hasStatusEffect(String type) {
    return statusEffects.containsKey(type);
  }

  /**
   * Explicitly removes a status effect from this entity, if present.
   *
   * @param type status effect type identifier
   */
  public void removeStatusEffect(String type) {
    if (statusEffects.remove(type) != null && entity != null) {
      entity.getEvents().trigger("statusEffectRemoved", type);
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
}
