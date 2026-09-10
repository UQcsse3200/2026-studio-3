package com.csse3200.game.components.player;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.Component;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Action component for interacting with the player. Player events should be initialised in create()
 * and when triggered should call methods within this class.
 */
public class PlayerActions extends Component {
  private static final Vector2 MAX_SPEED = new Vector2(3f, 3f); // Metres per second

  private PhysicsComponent physicsComponent;
  private Vector2 walkDirection = Vector2.Zero.cpy();
  private boolean moving = false;

  @Override
  public void create() {
    physicsComponent = entity.getComponent(PhysicsComponent.class);
    entity.getEvents().addListener("walk", this::walk);
    entity.getEvents().addListener("walkStop", this::stopWalking);
    entity.getEvents().addListener("attack", this::attack);

    // Card effects land here as plain, game-agnostic events (see Card / CardService /
    // EnemyDropTargetComponent) — this is the one place that decides what "damage" and
    // "heal" actually mean for the player. CombatStatsComponent.setHealth() already fires
    // "updateHealth" with the new value, so HealthDisplay updates automatically — no need
    // to trigger "updateHealth" manually anywhere else.
    entity.getEvents().addListener("damage", this::onDamage);
    entity.getEvents().addListener("heal", this::onHeal);
  }

  @Override
  public void update() {
    if (moving) {
      updateSpeed();
    }
  }

  private void updateSpeed() {
    Body body = physicsComponent.getBody();
    Vector2 velocity = body.getLinearVelocity();
    Vector2 desiredVelocity = walkDirection.cpy().scl(MAX_SPEED);
    // impulse = (desiredVel - currentVel) * mass
    Vector2 impulse = desiredVelocity.sub(velocity).scl(body.getMass());
    body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
  }

  /**
   * Moves the player towards a given direction.
   *
   * @param direction direction to move in
   */
  void walk(Vector2 direction) {
    this.walkDirection = direction;
    moving = true;
  }

  /** Stops the player from walking. */
  void stopWalking() {
    this.walkDirection = Vector2.Zero.cpy();
    updateSpeed();
    moving = false;
  }

  /** Makes the player attack. */
  public void attack() {
    // TODO: Logic for player attack
    Sound attackSound =
        ServiceLocator.getResourceService().getAsset("sounds/Impact4.ogg", Sound.class);
    attackSound.play();
  }

  /** Applies a "damage" card effect, e.g. from Card(trigger="damage", args=[10]). */
  private void onDamage(Integer amount) {
    CombatStatsComponent combatStats = entity.getComponent(CombatStatsComponent.class);
    if (combatStats != null) {
      combatStats.addHealth(-amount);
    }
  }

  /** Applies a "heal" card effect, e.g. from Card(trigger="heal", args=[15]). */
  private void onHeal(Integer amount) {
    CombatStatsComponent combatStats = entity.getComponent(CombatStatsComponent.class);
    if (combatStats != null) {
      combatStats.addHealth(amount);
    }
  }
}
