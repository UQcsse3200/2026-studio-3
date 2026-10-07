package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.play.CardPlayRequest;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.cards.play.CardPlayTarget;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffect;
import com.csse3200.game.components.cards.CardEffectHandler;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.combat.BattlePhase;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.services.audio.AudioService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/** Real card resolution and PNG loading, with mocked OpenGL; this is not a window test. */
@ExtendWith(GameExtension.class)
class EnemyStatusVisualIntegrationTest {
  private ResourceService resources;
  private EntityService entities;
  private GameTime time;
  private BattleController battle;
  private BattleAnimationCoordinator coordinator;
  private BattleDeck deck;
  private Entity target;
  private Entity other;

  @BeforeEach
  void setUp() {
    time = mock(GameTime.class);
    ServiceLocator.registerTimeSource(time);
    entities = spy(new EntityService());
    ServiceLocator.registerEntityService(entities);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    resources = new ResourceService();
    ServiceLocator.registerResourceService(resources);
    resources.loadTextures(EnemyStatusEffectVisuals.texturePaths());
    // Card effects now play sounds through AudioService, which needs them loaded.
    AudioService.load();
    resources.loadAll();
    for (String path : EnemyStatusEffectVisuals.texturePaths()) {
      assertTrue(resources.containsAsset(path, Texture.class), path);
    }
  }

  @AfterEach
  void tearDown() {
    entities.dispose();
    resources.dispose();
  }

  private void start(String cardId, int copies) {
    CardLibrary library = new CardLibrary(CardConfigLoader.loadCards());
    deck = new BattleDeck(new PlayerDeck(library, Collections.nCopies(copies, cardId)));
    deck.drawCards(copies);
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(100, 0))
            .addComponent(new EnergyComponent(3));
    target = enemy(2f);
    other = enemy(8f);
    CardEffectHandler handler = new CardEffectHandler(Map.of("target", target, "other", other));
    battle =
        new BattleController(
            player,
            List.of(target, other),
            handler,
            new CardPlayService(library, deck, player.getComponent(EnergyComponent.class)));
    EffectVisualRegistry registry = new EffectVisualRegistry();
    OffensiveEffectVisuals.registerAll(registry);
    EnemyStatusEffectVisuals.registerAll(registry);
    coordinator =
        new BattleAnimationCoordinator(battle, handler, List.of(target, other), player, registry);
    entities.register(new Entity().addComponent(coordinator));
    battle.start();
    clearInvocations(entities);
  }

  private Entity enemy(float x) {
    EnemyBehaviourComponent behaviour = mock(EnemyBehaviourComponent.class);
    when(behaviour.rollIntent()).thenReturn(EnemyIntent.defend(1));
    when(behaviour.getCurrentIntent()).thenReturn(EnemyIntent.defend(1));
    Entity enemy =
        new Entity().addComponent(spy(new CombatStatsComponent(30, 6))).addComponent(behaviour);
    enemy.setPosition(x, 4f);
    enemy.setScale(2f, 3f);
    return enemy;
  }

  private void play(TargetType type) {
    assertTrue(
        battle.submitCardPlayRequest(
            new CardPlayRequest(
                deck.getHand().get(0).instanceId(),
                new CardPlayTarget(type, type == TargetType.SINGLE_ENEMY ? "target" : null))));
    assertEquals(BattlePhase.PLAYER_TURN, battle.getCurrentPhase());
  }

  private List<Entity> spawned(int count) {
    ArgumentCaptor<Entity> capture = ArgumentCaptor.forClass(Entity.class);
    verify(entities, times(count)).register(capture.capture());
    return capture.getAllValues();
  }

  @Test
  void poisonDaggerPlaysDamageThenFourPoisonFramesOnlyOnItsTarget() {
    assertResolvedVisuals("poison_dagger", "POISON", 3, 3, false);
  }

  @Test
  void exposePlaysVulnerableOnEachActualTarget() {
    assertResolvedVisuals("expose", "VULNERABLE", 2, 2, true);
  }

  @Test
  void sentinelsRebukePlaysDamageThenFeebleWithoutReapplyingEffects() {
    assertResolvedVisuals("sentinels_rebuke", "FEEBLE", 1, 2, false);
  }

  private void assertResolvedVisuals(
      String cardId, String statusType, int value, int duration, boolean allEnemies) {
    start(cardId, 1);
    play(allEnemies ? TargetType.ALL_ENEMIES : TargetType.SINGLE_ENEMY);
    List<Entity> visuals = spawned(2);
    if (!allEnemies) {
      assertNotNull(visuals.get(0).getComponent(EffectBurstComponent.class));
      assertNull(visuals.get(0).getComponent(EnemyStatusEffectVisualComponent.class));
    }
    List<Entity> affected = allEnemies ? List.of(target, other) : List.of(target);
    for (int i = 0; i < affected.size(); i++) {
      Entity visualEntity = visuals.get(allEnemies ? i : 1);
      EnemyStatusEffectVisualComponent visual =
          visualEntity.getComponent(EnemyStatusEffectVisualComponent.class);
      assertNotNull(visual);
      assertNull(visualEntity.getComponent(EffectVisualComponent.class));
      assertEquals(affected.get(i).getCenterPosition(), visualEntity.getPosition());
      SpriteBatch batch = mock(SpriteBatch.class);
      if (!allEnemies) {
        visual.render(batch);
        verifyNoInteractions(batch);
      }
      coordinator.update();
      verify(entities, never()).unregister(visualEntity);
      // Sample the centre of each frame, after the existing coordinator's 0.3-second delay.
      when(time.getDeltaTime()).thenReturn((allEnemies ? 0f : 0.3f) + 0.09375f);
      visual.update();
      for (int frame = 0; frame < 4; frame++) {
        visual.render(batch);
        when(time.getDeltaTime()).thenReturn(0.1875f);
        visual.update();
      }
      ArgumentCaptor<TextureRegion> frames = ArgumentCaptor.forClass(TextureRegion.class);
      verify(batch, times(4))
          .draw(frames.capture(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
      for (int frame = 0; frame < 4; frame++) {
        TextureRegion region = frames.getAllValues().get(frame);
        assertEquals((frame % 2) * 256, region.getRegionX());
        assertEquals((frame / 2) * 256, region.getRegionY());
        assertSame(
            resources.getAsset(
                "images/effects/enemy-status/"
                    + statusType.toLowerCase(java.util.Locale.ROOT)
                    + ".png",
                Texture.class),
            region.getTexture());
      }
      coordinator.update();
      verify(entities, never()).unregister(visualEntity);
      assertFalse(visual.isExpired());
      CombatStatsComponent stats = affected.get(i).getComponent(CombatStatsComponent.class);
      verify(stats, times(1)).applyStatusEffect(any(StatusEffect.class));
      assertEquals(allEnemies ? 30 : 26, stats.getHealth());
      assertEquals(value, stats.getStatusEffect(statusType).getValue());
      assertEquals(duration, stats.getStatusEffect(statusType).getDuration());
      stats.removeStatusEffect(statusType);
      coordinator.update();
      verify(entities, times(1)).unregister(visualEntity);
    }
    if (!allEnemies) {
      CombatStatsComponent untouched = other.getComponent(CombatStatsComponent.class);
      assertEquals(30, untouched.getHealth());
      assertFalse(untouched.hasStatusEffect(statusType));
    }
  }

  @Test
  void poisonMarkKeepsBothStatusVisualsAndReusesThemWithoutDirectDamage() {
    start("poison_mark", 2);
    play(TargetType.SINGLE_ENEMY);
    List<Entity> visuals = spawned(2);
    CombatStatsComponent stats = target.getComponent(CombatStatsComponent.class);
    for (Entity visual : visuals) {
      assertNotNull(visual.getComponent(EnemyStatusEffectVisualComponent.class));
      assertNull(visual.getComponent(EffectBurstComponent.class));
      assertNull(visual.getComponent(EffectProjectileComponent.class));
    }
    EnemyStatusEffectVisualComponent vulnerable =
        visuals.get(0).getComponent(EnemyStatusEffectVisualComponent.class);
    EnemyStatusEffectVisualComponent poison =
        visuals.get(1).getComponent(EnemyStatusEffectVisualComponent.class);
    SpriteBatch vulnerableBatch = mock(SpriteBatch.class);
    SpriteBatch poisonBatch = mock(SpriteBatch.class);
    vulnerable.render(vulnerableBatch);
    poison.render(poisonBatch);
    verify(vulnerableBatch)
        .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    verifyNoInteractions(poisonBatch);

    when(time.getDeltaTime()).thenReturn(1f);
    for (Entity visual : visuals) visual.update();
    coordinator.update();
    assertFalse(vulnerable.isExpired());
    assertFalse(poison.isExpired());
    assertEquals(30, stats.getHealth());
    assertEquals(2, stats.getStatusEffect("VULNERABLE").getDuration());
    assertEquals(2, stats.getStatusEffect("POISON").getDuration());
    play(TargetType.SINGLE_ENEMY);
    spawned(2);
    assertEquals(30, stats.getHealth());
    assertEquals(4, stats.getStatusEffect("POISON").getValue());
    assertFalse(other.getComponent(CombatStatsComponent.class).hasStatusEffect("POISON"));
    assertFalse(other.getComponent(CombatStatsComponent.class).hasStatusEffect("VULNERABLE"));
    stats.clearNegativeStatusEffects();
    coordinator.update();
    for (Entity visual : visuals) verify(entities).unregister(visual);
  }

  @Test
  void repeatedCardsReuseTheSameMarkerAndDeathCleansItUp() {
    start("poison_dagger", 2);
    play(TargetType.SINGLE_ENEMY);
    Entity marker = spawned(2).get(1);
    EnemyStatusEffectVisualComponent visual =
        marker.getComponent(EnemyStatusEffectVisualComponent.class);
    when(time.getDeltaTime()).thenReturn(1.1f);
    visual.update();
    coordinator.update();
    play(TargetType.SINGLE_ENEMY);
    List<Entity> visuals = spawned(3);
    assertSame(marker, visuals.get(1));
    assertNotNull(visuals.get(2).getComponent(EffectBurstComponent.class));
    SpriteBatch waitingBatch = mock(SpriteBatch.class);
    visual.render(waitingBatch);
    verify(waitingBatch, times(1))
        .draw(any(TextureRegion.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    CombatStatsComponent stats = target.getComponent(CombatStatsComponent.class);
    assertEquals(22, stats.getHealth());
    assertEquals(6, stats.getStatusEffect("POISON").getValue());
    verify(stats, times(2)).applyStatusEffect(any(StatusEffect.class));
    stats.setHealth(0);
    coordinator.update();
    assertTrue(visual.isExpired());
    verify(entities, times(1)).unregister(marker);
    SpriteBatch batch = mock(SpriteBatch.class);
    visual.render(batch);
    verifyNoInteractions(batch);
  }

  @Test
  void externallyAppliedStatusesGetOneMarkerEachAndCleanseRemovesThem() {
    start("poison_dagger", 1);
    CombatStatsComponent stats = target.getComponent(CombatStatsComponent.class);
    for (String type : List.of("POISON", "VULNERABLE", "FEEBLE")) {
      stats.applyStatusEffect(type, 1, 2);
    }
    coordinator.update();
    List<Entity> markers = spawned(3);
    coordinator.update();
    spawned(3);
    for (Entity marker : markers) {
      EnemyStatusEffectVisualComponent visual =
          marker.getComponent(EnemyStatusEffectVisualComponent.class);
      SpriteBatch batch = mock(SpriteBatch.class);
      visual.render(batch);
      ArgumentCaptor<TextureRegion> frame = ArgumentCaptor.forClass(TextureRegion.class);
      verify(batch).draw(frame.capture(), anyFloat(), anyFloat(), anyFloat(), anyFloat());
      assertEquals(0, frame.getValue().getRegionX());
      assertEquals(0, frame.getValue().getRegionY());
      assertFalse(visual.isExpired());
    }
    stats.clearNegativeStatusEffects();
    coordinator.update();
    coordinator.update();
    for (Entity marker : markers) verify(entities, times(1)).unregister(marker);
    spawned(3);
  }

  @Test
  void poisonMarkerSurvivesUntilTheLastDurationGroupExpires() {
    start("poison_dagger", 1);
    CombatStatsComponent stats = target.getComponent(CombatStatsComponent.class);
    stats.applyStatusEffect("POISON", 2, 1);
    stats.applyStatusEffect("POISON", 3, 2);
    coordinator.update();
    Entity marker = spawned(1).get(0);
    stats.processPoisonTick(stats::takePiercingDamage);
    coordinator.update();
    verify(entities, never()).unregister(marker);
    assertEquals(25, stats.getHealth());
    stats.processPoisonTick(stats::takePiercingDamage);
    coordinator.update();
    verify(entities, times(1)).unregister(marker);
    assertEquals(22, stats.getHealth());
    spawned(1);
  }

  @Test
  void feebleAndVulnerableMarkersFollowExplicitStatusExpiry() {
    start("expose", 1);
    CombatStatsComponent stats = target.getComponent(CombatStatsComponent.class);
    stats.applyStatusEffect("FEEBLE", 1, 2);
    stats.applyStatusEffect("VULNERABLE", 2, 1);
    coordinator.update();
    List<Entity> markers = spawned(2);
    assertTrue(stats.tickStatusEffect("VULNERABLE"));
    assertFalse(stats.tickStatusEffect("FEEBLE"));
    coordinator.update();
    verify(entities, times(1)).unregister(markers.get(0));
    verify(entities, never()).unregister(markers.get(1));
    assertTrue(stats.tickStatusEffect("FEEBLE"));
    coordinator.update();
    verify(entities, times(1)).unregister(markers.get(1));
    spawned(2);
  }

  @Test
  void pierceStillExpiresWhileItsPoisonMarkerRemains() {
    start("poison_blade", 1);
    play(TargetType.SINGLE_ENEMY);
    List<Entity> visuals = spawned(2);
    EffectProjectileComponent pierce = visuals.get(0).getComponent(EffectProjectileComponent.class);
    assertNotNull(pierce);
    when(time.getDeltaTime()).thenReturn(1f);
    pierce.update();
    visuals.get(1).getComponent(EnemyStatusEffectVisualComponent.class).update();
    coordinator.update();
    verify(entities).unregister(visuals.get(0));
    verify(entities, never()).unregister(visuals.get(1));
    assertEquals(20, target.getComponent(CombatStatsComponent.class).getHealth());
    spawned(2);
  }

  @Test
  void sunderAndDamageKeepTheirOriginalCreationAndExpiry() {
    start("unseal_the_breach", 1);
    target.getComponent(CombatStatsComponent.class).setArmour(3);
    play(TargetType.SINGLE_ENEMY);
    List<Entity> visuals = spawned(2);
    when(time.getDeltaTime()).thenReturn(0.1f);
    for (Entity visual : visuals) {
      assertNull(visual.getComponent(EnemyStatusEffectVisualComponent.class));
      EffectBurstComponent burst = visual.getComponent(EffectBurstComponent.class);
      assertNotNull(burst);
      burst.update();
      assertFalse(burst.isExpired());
    }
    when(time.getDeltaTime()).thenReturn(1f);
    for (Entity visual : visuals) visual.getComponent(EffectBurstComponent.class).update();
    coordinator.update();
    for (Entity visual : visuals) verify(entities).unregister(visual);
    assertEquals(28, target.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(0, target.getComponent(CombatStatsComponent.class).getArmour());
    spawned(2);
  }

  @Test
  void leavingBattleDisposesQueuedVisualsBeforeUnloadingSheets() {
    start("sentinels_rebuke", 1);
    play(TargetType.SINGLE_ENEMY);
    EnemyStatusEffectVisualComponent visual =
        spawned(2).get(1).getComponent(EnemyStatusEffectVisualComponent.class);
    entities.dispose();
    resources.unloadAssets(EnemyStatusEffectVisuals.texturePaths());
    assertTrue(visual.isExpired());
    SpriteBatch batch = mock(SpriteBatch.class);
    visual.render(batch);
    verifyNoInteractions(batch);
    for (String path : EnemyStatusEffectVisuals.texturePaths()) {
      assertFalse(resources.containsAsset(path, Texture.class));
    }
  }

  @Test
  void repeatedCardKeepsOneStatusVisualAndDeathStopsDrawing() {
    start("poison_dagger", 2);
    play(TargetType.SINGLE_ENEMY);
    Entity marker = spawned(2).get(1);
    EnemyStatusEffectVisualComponent visual =
        marker.getComponent(EnemyStatusEffectVisualComponent.class);
    when(time.getDeltaTime()).thenReturn(1.1f);
    visual.update();
    coordinator.update();
    play(TargetType.SINGLE_ENEMY);
    assertSame(marker, spawned(3).get(1));
    target.getComponent(CombatStatsComponent.class).setHealth(0);
    coordinator.update();
    assertTrue(visual.isExpired());
    verify(entities, times(1)).unregister(marker);
    SpriteBatch batch = mock(SpriteBatch.class);
    visual.render(batch);
    verifyNoInteractions(batch);
  }
}
