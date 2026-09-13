package com.csse3200.game.entities.factories;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.csse3200.game.ai.tasks.AITaskComponent;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.TouchAttackComponent;
import com.csse3200.game.components.npc.GhostAnimationController;
import com.csse3200.game.components.tasks.ChaseTask;
import com.csse3200.game.components.tasks.WanderTask;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.BaseEntityConfig;
import com.csse3200.game.entities.configs.GhostKingConfig;
import com.csse3200.game.entities.configs.NPCConfigs;
import com.csse3200.game.files.FileLoader;
import com.csse3200.game.physics.PhysicsLayer;
import com.csse3200.game.physics.PhysicsUtils;
import com.csse3200.game.physics.components.ColliderComponent;
import com.csse3200.game.physics.components.HitboxComponent;
import com.csse3200.game.physics.components.PhysicsComponent;
import com.csse3200.game.physics.components.PhysicsMovementComponent;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.services.ServiceLocator;

/**
 * Factory to create non-playable character (NPC) entities with predefined components.
 *
 * <p>Each NPC entity type should have a creation method that returns a corresponding entity.
 * Predefined entity properties can be loaded from configs stored as json files which are defined in
 * "NPCConfigs".
 *
 * <p>If needed, this factory can be separated into more specific factories for entities with
 * similar characteristics.
 */
public class NPCFactory {
  private static final NPCConfigs configs =
      FileLoader.readClass(NPCConfigs.class, "configs/NPCs.json");

  /**
   * Creates a ghost entity.
   *
   * @param target entity to chase
   * @return entity
   */
  public static Entity createGhost(Entity target) {
    Entity ghost = createBaseNPC(target);
    BaseEntityConfig config = configs.ghost;

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService().getAsset("images/ghost.atlas", TextureAtlas.class));
    animator.addAnimation("angry_float", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("float", 0.1f, Animation.PlayMode.LOOP);

    ghost
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(animator)
        .addComponent(new GhostAnimationController());

    ghost.getComponent(AnimationRenderComponent.class).scaleEntity();

    // 碰撞监听：撞到任何实体障碍物（树、墙等非 Sensor 物体）时施加反弹冲量并强行重置 AI 重新选路
    ghost
        .getEvents()
        .addListener(
            "collisionStart",
            (Fixture fixtureA, Fixture fixtureB) -> {
              // 找到与幽灵发生碰撞的另一个 Fixture
              Fixture other = (fixtureA.getBody().getUserData() == ghost) ? fixtureB : fixtureA;

              // 只要对方不是 Sensor（即属于实体障碍物/碰撞体），就触发反弹与转向
              if (other != null && !other.isSensor()) {
                PhysicsMovementComponent movement =
                    ghost.getComponent(PhysicsMovementComponent.class);
                PhysicsComponent physics = ghost.getComponent(PhysicsComponent.class);

                if (movement != null && physics != null && physics.getBody() != null) {
                  // 1. 停止当前移动并清空线性速度
                  movement.setMoving(false);
                  physics.getBody().setLinearVelocity(Vector2.Zero);

                  // 2. 算出一个远离障碍物的反向冲量，防止幽灵卡在障碍物内部
                  Vector2 ghostPos = ghost.getPosition();
                  Vector2 obstaclePos = other.getBody().getPosition();
                  Vector2 bounceDir = ghostPos.cpy().sub(obstaclePos).nor().scl(2f);
                  physics
                      .getBody()
                      .applyLinearImpulse(bounceDir, physics.getBody().getWorldCenter(), true);
                }

                // 3. 强行重置 AI 组件，使 WanderTask 重新计算并生成新的远端巡逻点
                AITaskComponent ai = ghost.getComponent(AITaskComponent.class);
                if (ai != null) {
                  ai.setEnabled(false);
                  ai.setEnabled(true);
                }
              }
            });

    return ghost;
  }

  /**
   * Creates a ghost king entity.
   *
   * @param target entity to chase
   * @return entity
   */
  public static Entity createGhostKing(Entity target) {
    Entity ghostKing = createBaseNPC(target);
    GhostKingConfig config = configs.ghostKing;

    AnimationRenderComponent animator =
        new AnimationRenderComponent(
            ServiceLocator.getResourceService()
                .getAsset("images/ghostKing.atlas", TextureAtlas.class));
    animator.addAnimation("float", 0.1f, Animation.PlayMode.LOOP);
    animator.addAnimation("angry_float", 0.1f, Animation.PlayMode.LOOP);

    ghostKing
        .addComponent(new CombatStatsComponent(config.health, config.baseAttack))
        .addComponent(animator)
        .addComponent(new GhostAnimationController());

    ghostKing.getComponent(AnimationRenderComponent.class).scaleEntity();
    return ghostKing;
  }

  /**
   * Creates a generic NPC to be used as a base entity by more specific NPC creation methods.
   *
   * @return entity
   */
  private static Entity createBaseNPC(Entity target) {
    // 漫游范围设为适中的 (4f, 4f)，防止范围太大跑出地图或太小原地打转
    AITaskComponent aiComponent =
        new AITaskComponent()
            .addTask(new WanderTask(new Vector2(4f, 4f), 1.5f))
            .addTask(new ChaseTask(target, 10, 3f, 4f));

    Entity npc =
        new Entity()
            .addComponent(new PhysicsComponent())
            .addComponent(new PhysicsMovementComponent())
            .addComponent(new ColliderComponent())
            .addComponent(new HitboxComponent().setLayer(PhysicsLayer.NPC))
            .addComponent(new TouchAttackComponent(PhysicsLayer.PLAYER, 1.5f))
            .addComponent(aiComponent);

    PhysicsUtils.setScaledCollider(npc, 0.9f, 0.4f);
    return npc;
  }

  private NPCFactory() {
    throw new IllegalStateException("Instantiating static util class");
  }
}
