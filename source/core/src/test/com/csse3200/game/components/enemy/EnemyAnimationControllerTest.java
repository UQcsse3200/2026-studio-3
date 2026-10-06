package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.combat.BattlePhase;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.events.EventHandler;
import com.csse3200.game.events.listeners.EventListener0;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class EnemyAnimationControllerTest {

  private AnimationRenderComponent animator;
  private Entity enemy;
  private GameTime time;
  private CombatStatsComponent stats;
  private EventHandler events;

  @BeforeEach
  void setUp() {
    animator = mock(AnimationRenderComponent.class);
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    stats = new CombatStatsComponent(30, 6);
    enemy = spy(new Entity());
    events = spy(enemy.getEvents());
    doReturn(events).when(enemy).getEvents();
    enemy.addComponent(stats);
    enemy.addComponent(animator);
    enemy.addComponent(new EnemyAnimationController());
    enemy.create();
    enemy.setPosition(10f, 3f);
  }

  @Test
  void shouldStartIdleOnCreate() {
    verify(animator).startAnimation("idle");
  }

  @Test
  void shouldPlayHurtWhenDamaged() {
    enemy.getEvents().trigger("enemyDamaged", 5);

    verify(animator).startAnimation("hurt");
  }

  @Test
  void shouldPlayHurtWhenDefeatedAndNoDeathAnimationExists() {
    enemy.getEvents().trigger("enemyDefeated");

    verify(animator).startAnimation("hurt");
  }

  // 图集里有 death 帧的敌人，死亡时应该播 death，而不是退回 hurt
  @Test
  void shouldPlayDeathWhenDeathAnimationAvailable() {
    when(animator.hasAnimation("death")).thenReturn(true);

    enemy.getEvents().trigger("enemyDefeated");

    verify(animator).startAnimation("death");
  }

  @Test
  void shouldPlayDeathWhenAvailable() {
    when(animator.hasAnimation("death")).thenReturn(true);

    enemy.getEvents().trigger("enemyDefeated");

    verify(animator).startAnimation("death");
  }

  @Test
  void shouldPlayCastWhenAvailable() {
    when(animator.hasAnimation("cast")).thenReturn(true);

    enemy.getEvents().trigger("enemyCast");

    verify(animator).startAnimation("cast");
  }

  @Test
  void shouldPlayDefendWhenAvailable() {
    when(animator.hasAnimation("defend")).thenReturn(true);

    enemy.getEvents().trigger("enemyDefend");

    verify(animator).startAnimation("defend");
  }

  @Test
  void shouldReturnToIdleAfterHurtFinishes() {
    when(animator.getCurrentAnimation()).thenReturn("hurt");
    when(animator.isFinished()).thenReturn(true);

    enemy.update();

    verify(animator, times(2)).startAnimation("idle");
  }

  @Test
  void shouldReturnToIdleAfterCastFinishes() {
    when(animator.getCurrentAnimation()).thenReturn("cast");
    when(animator.isFinished()).thenReturn(true);

    enemy.update();

    verify(animator, times(2)).startAnimation("idle");
  }

  @Test
  void shouldHoldTheFinalDeathFrame() {
    when(animator.getCurrentAnimation()).thenReturn("death");
    when(animator.isFinished()).thenReturn(true);

    enemy.update();

    verify(animator, times(1)).startAnimation("idle");
  }

  @Test
  void shouldStayOnHurtWhileItIsStillPlaying() {
    when(animator.getCurrentAnimation()).thenReturn("hurt");
    when(animator.isFinished()).thenReturn(false);

    enemy.update();

    verify(animator, times(1)).startAnimation("idle");
  }

  @Test
  void shouldNotRestartIdleWhileIdleIsPlaying() {
    when(animator.getCurrentAnimation()).thenReturn("idle");
    when(animator.isFinished()).thenReturn(true);

    enemy.update();

    verify(animator, times(1)).startAnimation("idle");
  }

  @Test
  void shouldTolerateANullCurrentAnimation() {
    when(animator.getCurrentAnimation()).thenReturn(null);

    enemy.update();

    verify(animator, never()).startAnimation("hurt");
  }

  @Test
  void shouldPlayAvailableAttackOnceAndReturnToIdle() {
    when(animator.hasAnimation("attack")).thenReturn(true);
    enemy.getEvents().trigger("enemyAttack");
    verify(animator).startAnimation("attack");
    when(animator.getCurrentAnimation()).thenReturn("attack");
    when(animator.isFinished()).thenReturn(false);
    enemy.update();
    verify(animator, times(1)).startAnimation("idle");
    when(animator.isFinished()).thenReturn(true);
    enemy.update();
    verify(animator, times(2)).startAnimation("idle");
    assertEquals(new Vector2(10f, 3f), enemy.getPosition());
  }

  @Test
  void shouldLungeAndReturnWithoutMissingAnimationOrCombatChanges() {
    stats.applyStatusEffect("FEEBLE", 1, 2);
    when(time.getDeltaTime()).thenReturn(0.15f);
    enemy.getEvents().trigger("enemyAttack");
    enemy.update();
    assertTrue(enemy.getPosition().x < 10f);
    assertEquals(3f, enemy.getPosition().y);
    enemy.update();
    assertEquals(new Vector2(10f, 3f), enemy.getPosition());
    verify(animator, never()).startAnimation("attack");
    assertEquals(30, stats.getHealth());
    assertEquals(2, stats.getStatusEffect("FEEBLE").getDuration());
  }

  @Test
  void repeatedAttacksAndLongFramesShouldNotAccumulatePositionDrift() {
    when(time.getDeltaTime()).thenReturn(0.1f);
    for (int i = 0; i < 4; i++) {
      enemy.getEvents().trigger("enemyAttack");
      enemy.update();
    }
    when(time.getDeltaTime()).thenReturn(1f);
    enemy.update();
    assertEquals(new Vector2(10f, 3f), enemy.getPosition());
  }

  @Test
  void hurtShouldCancelLungeAndRestorePosition() {
    when(time.getDeltaTime()).thenReturn(0.1f);
    enemy.getEvents().trigger("enemyAttack");
    enemy.update();
    enemy.getEvents().trigger("enemyDamaged", 1);
    assertEquals(new Vector2(10f, 3f), enemy.getPosition());
    verify(animator).startAnimation("hurt");
  }

  @Test
  void deathShouldCancelLungeAndPreventFurtherActionsOrIdle() {
    when(time.getDeltaTime()).thenReturn(0.1f);
    enemy.getEvents().trigger("enemyAttack");
    enemy.update();
    enemy.getEvents().trigger("enemyDefeated");
    enemy.getEvents().trigger("enemyAttack");
    enemy.getEvents().trigger("enemyCast");
    enemy.getEvents().trigger("enemyDefend");
    enemy.getEvents().trigger("enemyDamaged", 1);
    when(animator.getCurrentAnimation()).thenReturn("hurt");
    when(animator.isFinished()).thenReturn(true);
    enemy.update();
    assertEquals(new Vector2(10f, 3f), enemy.getPosition());
    verify(animator, times(2)).startAnimation("idle");
    verify(animator, times(1)).startAnimation("hurt");
  }

  @Test
  void disposalShouldRestorePositionAndDetachAttackListener() {
    when(time.getDeltaTime()).thenReturn(0.1f);
    enemy.getEvents().trigger("enemyAttack");
    enemy.update();
    enemy.getComponent(EnemyAnimationController.class).dispose();
    enemy.getEvents().trigger("enemyAttack");
    enemy.update();
    assertEquals(new Vector2(10f, 3f), enemy.getPosition());
    verify(animator, times(2)).startAnimation("idle");
  }

  @Test
  @SuppressWarnings("unchecked")
  void disposalShouldDetachAllOwnedListenersAndPreserveOtherSubscribers() {
    EventListener0 otherAttackListener = mock(EventListener0.class);
    events.addListener("enemyAttack", otherAttackListener);
    EnemyAnimationController controller = enemy.getComponent(EnemyAnimationController.class);
    controller.dispose();

    for (String event : new String[] {"enemyDefeated", "enemyCast", "enemyDefend"}) {
      ArgumentCaptor<EventListener0> listener = ArgumentCaptor.forClass(EventListener0.class);
      verify(events).addListener(eq(event), listener.capture());
      verify(events).removeListener(eq(event), same(listener.getValue()));
    }
    ArgumentCaptor<EventListener1<Integer>> damaged = ArgumentCaptor.forClass(EventListener1.class);
    verify(events).addListener(eq("enemyDamaged"), damaged.capture());
    verify(events).removeListener(eq("enemyDamaged"), same(damaged.getValue()));
    ArgumentCaptor<EventListener0> attacks = ArgumentCaptor.forClass(EventListener0.class);
    verify(events, times(2)).addListener(eq("enemyAttack"), attacks.capture());
    verify(events).removeListener(eq("enemyAttack"), same(attacks.getAllValues().get(0)));

    controller.dispose();
    events.trigger("enemyAttack");
    events.trigger("enemyDamaged", 1);
    events.trigger("enemyDefeated");
    events.trigger("enemyCast");
    events.trigger("enemyDefend");
    enemy.update();
    verify(otherAttackListener).handle();
    verify(events, never()).removeListener("enemyAttack", otherAttackListener);
    verify(animator, times(1)).startAnimation("idle");
    verify(animator, never()).startAnimation("hurt");
  }

  @Test
  void actualAttackPathUsesAvailableAtlasFramesOrReturnsFromLunge() {
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerEntityService(new EntityService());
    when(time.getDeltaTime()).thenReturn(0.1f);
    for (String id :
        new String[] {
          "tomb_guardian",
          "lesser_shade",
          "bone_crawler",
          "dark_acolyte",
          "void_knight",
          "boss_knight",
          "default"
        }) {
      TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("images/enemies/" + id + ".atlas"));
      AnimationRenderComponent actualAnimator = new AnimationRenderComponent(atlas);
      assertTrue(actualAnimator.addAnimation("idle", 0.5f, Animation.PlayMode.LOOP));
      assertTrue(actualAnimator.addAnimation("hurt", 0.15f));
      boolean hasAttack = actualAnimator.addAnimation("attack", 0.2f);
      actualAnimator.addAnimation("death", 0.3f);
      EnemyBehaviourComponent behaviour =
          new EnemyBehaviourComponent("test_attack", context -> EnemyIntent.attack(6));
      CombatStatsComponent actualStats = new CombatStatsComponent(30, 6);
      Entity actualEnemy =
          new Entity()
              .addComponent(actualStats)
              .addComponent(new EnemyStatsComponent(id))
              .addComponent(behaviour)
              .addComponent(actualAnimator)
              .addComponent(new EnemyAnimationController());
      actualEnemy.setPosition(10f, 3f);
      ServiceLocator.getEntityService().register(actualEnemy);
      CombatStatsComponent playerStats = new CombatStatsComponent(100, 0);
      Entity player = new Entity().addComponent(playerStats);
      SpriteBatch batch = mock(SpriteBatch.class);
      try {
        assertEquals(!"boss_knight".equals(id) && !"default".equals(id), hasAttack, id);
        for (int attack = 1; attack <= 3; attack++) {
          behaviour.rollIntent();
          behaviour.executeIntent(player);
          assertEquals(hasAttack ? "attack" : "idle", actualAnimator.getCurrentAnimation(), id);
          actualEnemy.update();
          assertTrue(actualEnemy.getPosition().x < 10f, id);
          for (int frame = 0; frame < 5; frame++) {
            actualAnimator.render(batch);
            actualEnemy.update();
          }
          assertEquals("idle", actualAnimator.getCurrentAnimation(), id);
          assertEquals(new Vector2(10f, 3f), actualEnemy.getPosition(), id);
          assertEquals(100 - attack * 6, playerStats.getHealth(), id);
          assertFalse(actualStats.isDead());
        }
      } finally {
        actualEnemy.dispose();
        atlas.dispose();
      }
    }
  }

  @Test
  void completionWaitsForBothFramesAndReturnAndRunsOnce() {
    EnemyAnimationController controller = enemy.getComponent(EnemyAnimationController.class);
    Runnable complete = mock(Runnable.class);
    when(animator.hasAnimation("attack")).thenReturn(true);
    when(animator.getCurrentAnimation()).thenReturn("attack");
    when(time.getDeltaTime()).thenReturn(0.3f);
    enemy.getEvents().trigger("enemyAttack");
    controller.runAfterAttack(complete);
    enemy.update();
    assertEquals(new Vector2(10f, 3f), enemy.getPosition());
    verify(complete, never()).run();
    when(animator.isFinished()).thenReturn(true);
    enemy.update();
    enemy.update();
    verify(complete, times(1)).run();
  }

  @Test
  void interruptedAttackCompletesButDisposalNeverContinuesBattle() {
    EnemyAnimationController controller = enemy.getComponent(EnemyAnimationController.class);
    for (String event : new String[] {"enemyDamaged", "enemyDefeated"}) {
      Runnable complete = mock(Runnable.class);
      enemy.getEvents().trigger("enemyAttack");
      controller.runAfterAttack(complete);
      if (event.equals("enemyDamaged")) enemy.getEvents().trigger(event, 1);
      else enemy.getEvents().trigger(event);
      enemy.update();
      enemy.update();
      verify(complete, times(1)).run();
      assertEquals(new Vector2(10f, 3f), enemy.getPosition());
    }
    // A separate, live controller covers exiting while an attack is pending.
    Entity exiting = visualEnemy(mock(AnimationRenderComponent.class));
    EnemyAnimationController exitingAnimation =
        exiting.getComponent(EnemyAnimationController.class);
    Runnable afterExit = mock(Runnable.class);
    exiting.getEvents().trigger("enemyAttack");
    exitingAnimation.runAfterAttack(afterExit);
    exitingAnimation.dispose();
    exitingAnimation.update();
    verify(afterExit, never()).run();
  }

  private Entity visualEnemy(AnimationRenderComponent animation) {
    Entity entity =
        new Entity().addComponent(animation).addComponent(new EnemyAnimationController());
    entity.create();
    entity.setPosition(10f, 3f);
    return entity;
  }

  @Test
  void realAttackCompletionCanStartNextVisualOnlyAfterFramesAndReturnFinish() {
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerEntityService(new EntityService());
    when(time.getDeltaTime()).thenReturn(0.1f);
    TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("images/enemies/tomb_guardian.atlas"));
    AnimationRenderComponent animation = new AnimationRenderComponent(atlas);
    animation.addAnimation("idle", 0.5f, Animation.PlayMode.LOOP);
    animation.addAnimation("attack", 0.4f);
    Entity first = visualEnemy(animation);
    Entity second = visualEnemy(mock(AnimationRenderComponent.class));
    Runnable completed = mock(Runnable.class);
    SpriteBatch batch = mock(SpriteBatch.class);
    try {
      first.getEvents().trigger("enemyAttack");
      first
          .getComponent(EnemyAnimationController.class)
          .runAfterAttack(
              () -> {
                completed.run();
                second.getEvents().trigger("enemyAttack");
              });
      for (int frame = 0; frame < 4; frame++) {
        first.update();
        second.update();
        verify(completed, never()).run();
        assertEquals(new Vector2(10f, 3f), second.getPosition());
        animation.render(batch);
      }
      first.update();
      verify(completed, times(1)).run();
      assertEquals("idle", animation.getCurrentAnimation());
      assertEquals(new Vector2(10f, 3f), first.getPosition());
      second.update();
      assertTrue(second.getPosition().x < 10f);
      for (int frame = 0; frame < 5; frame++) {
        first.update();
        second.update();
      }
      verify(completed, times(1)).run();
      assertEquals(new Vector2(10f, 3f), second.getPosition());
    } finally {
      first.dispose();
      second.dispose();
      atlas.dispose();
    }
  }

  private Entity battleEnemy() {
    return battleEnemy(mock(AnimationRenderComponent.class));
  }

  private Entity battleEnemy(AnimationRenderComponent animation) {
    Entity attacker =
        new Entity()
            .addComponent(new CombatStatsComponent(30, 8))
            .addComponent(new EnemyStatsComponent("audit"))
            .addComponent(new EnemyBehaviourComponent("attack", context -> EnemyIntent.attack(8)))
            .addComponent(animation)
            .addComponent(new EnemyAnimationController());
    attacker.create();
    attacker.setPosition(10f, 3f);
    return attacker;
  }

  @Test
  void battleWaitsBetweenEnemiesWithoutRepeatingDamageOrStatusTicks() {
    Entity first = battleEnemy();
    Entity second = battleEnemy();
    CombatStatsComponent firstStats = first.getComponent(CombatStatsComponent.class);
    firstStats.applyStatusEffect("FEEBLE", 1, 2);
    firstStats.applyStatusEffect("POISON", 1, 2);
    CombatStatsComponent playerStats = new CombatStatsComponent(100, 0);
    Entity player = new Entity().addComponent(playerStats);
    BattleController battle = new BattleController(player, List.of(first, second));
    when(time.getDeltaTime()).thenReturn(0.15f);
    battle.start();
    for (int round = 0; round < 2; round++) {
      int before = playerStats.getHealth();
      battle.endPlayerTurn();
      assertEquals(BattlePhase.ENEMY_ATTACK, battle.getCurrentPhase());
      assertEquals(0, battle.getCurrentEnemyIndex());
      assertEquals(before - 6, playerStats.getHealth());
      assertEquals(29 - round, firstStats.getHealth());
      assertEquals(2 - round, firstStats.getStatusEffect("FEEBLE").getDuration());
      first.update();
      assertTrue(first.getPosition().x < 10f);
      assertEquals(new Vector2(10f, 3f), second.getPosition());
      assertEquals(before - 6, playerStats.getHealth());
      first.update();
      assertEquals(new Vector2(10f, 3f), first.getPosition());
      assertEquals(1, battle.getCurrentEnemyIndex());
      assertEquals(before - 14, playerStats.getHealth());
      if (round == 0) assertEquals(1, firstStats.getStatusEffect("FEEBLE").getDuration());
      else assertFalse(firstStats.hasStatusEffect("FEEBLE"));
      second.update();
      assertTrue(second.getPosition().x < 10f);
      second.update();
      assertEquals(new Vector2(10f, 3f), second.getPosition());
      assertEquals(BattlePhase.PLAYER_TURN, battle.getCurrentPhase());
      first.update();
      second.update();
      assertEquals(before - 14, playerStats.getHealth());
    }
    assertFalse(firstStats.hasStatusEffect("POISON"));
  }

  @Test
  void battleWaitsForRealAttackFramesLongerThanLunge() {
    ServiceLocator.registerRenderService(mock(RenderService.class));
    ServiceLocator.registerEntityService(new EntityService());
    when(time.getDeltaTime()).thenReturn(0.1f);
    TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("images/enemies/tomb_guardian.atlas"));
    AnimationRenderComponent animation = new AnimationRenderComponent(atlas);
    animation.addAnimation("idle", 0.5f, Animation.PlayMode.LOOP);
    animation.addAnimation("attack", 0.4f);
    Entity first = battleEnemy(animation);
    Entity second = battleEnemy();
    CombatStatsComponent playerStats = new CombatStatsComponent(100, 0);
    Entity player = new Entity().addComponent(playerStats);
    BattleController battle = new BattleController(player, List.of(first, second));
    SpriteBatch batch = mock(SpriteBatch.class);
    try {
      battle.start();
      battle.endPlayerTurn();
      for (int frame = 0; frame < 4; frame++) {
        first.update();
        second.update();
        assertEquals(BattlePhase.ENEMY_ATTACK, battle.getCurrentPhase());
        assertEquals(0, battle.getCurrentEnemyIndex());
        assertEquals(92, playerStats.getHealth());
        assertEquals(new Vector2(10f, 3f), second.getPosition());
        animation.render(batch);
      }
      first.update();
      assertEquals("idle", animation.getCurrentAnimation());
      assertEquals(new Vector2(10f, 3f), first.getPosition());
      assertEquals(1, battle.getCurrentEnemyIndex());
      assertEquals(84, playerStats.getHealth());
      second.update();
      assertTrue(second.getPosition().x < 10f);
      for (int frame = 0; frame < 5; frame++) {
        first.update();
        second.update();
      }
      assertEquals(BattlePhase.PLAYER_TURN, battle.getCurrentPhase());
      assertEquals(84, playerStats.getHealth());
      assertEquals(new Vector2(10f, 3f), second.getPosition());
    } finally {
      first.dispose();
      second.dispose();
      atlas.dispose();
    }
  }

  @Test
  void lethalAttackFinishesBeforeBattleEndsAndSecondEnemyDoesNotAttack() {
    ServiceLocator.registerEntityService(new EntityService());
    when(time.getDeltaTime()).thenReturn(0.15f);
    Entity first = battleEnemy();
    Entity second = battleEnemy();
    Entity player = new Entity().addComponent(new CombatStatsComponent(5, 0));
    EventListener0 secondAttack = mock(EventListener0.class);
    second.getEvents().addListener("enemyAttack", secondAttack);
    BattleController battle = new BattleController(player, List.of(first, second));
    try {
      battle.start();
      battle.endPlayerTurn();
      assertEquals(0, player.getComponent(CombatStatsComponent.class).getHealth());
      assertEquals(BattlePhase.ENEMY_ATTACK, battle.getCurrentPhase());
      first.update();
      assertTrue(first.getPosition().x < 10f);
      assertEquals(BattlePhase.ENEMY_ATTACK, battle.getCurrentPhase());
      first.update();
      assertEquals(BattlePhase.DEFEAT, battle.getCurrentPhase());
      assertEquals(new Vector2(10f, 3f), first.getPosition());
      verify(secondAttack, never()).handle();
    } finally {
      first.dispose();
      second.dispose();
    }
  }
}
