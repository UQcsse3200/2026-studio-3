package com.csse3200.game.entities.factories;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.enemy.EnemyAnimationController;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.enemy.EnemyStatsComponent;
import com.csse3200.game.components.enemy.IntentIcons;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.configs.EnemyConfig;
import com.csse3200.game.entities.configs.EnemyTier;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.DebugRenderer;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class EnemyFactoryTest {

  private ResourceService resourceService;

  @BeforeEach
  void registerServices() {
    // Headless tests have no real assets. Hand the factory a mocked ResourceService that returns a
    // dummy atlas for any request, so AnimationRenderComponent can be built without a GPU.
    resourceService = mock(ResourceService.class);
    when(resourceService.getAsset(anyString(), eq(TextureAtlas.class)))
        .thenReturn(mock(TextureAtlas.class));
    ServiceLocator.registerResourceService(resourceService);

    RenderService renderService = new RenderService();
    renderService.setDebug(mock(DebugRenderer.class));
    ServiceLocator.registerRenderService(renderService);

    ServiceLocator.registerTimeSource(mock(GameTime.class));
  }

  @Test
  void createAttachesStatsAndBehaviour() {
    Entity enemy = EnemyFactory.create("lesser_shade");

    assertNotNull(enemy.getComponent(CombatStatsComponent.class));
    assertNotNull(enemy.getComponent(EnemyStatsComponent.class));
    assertNotNull(enemy.getComponent(EnemyBehaviourComponent.class));
  }

  @Test
  void createAttachesAnimationComponents() {
    Entity enemy = EnemyFactory.create("lesser_shade");

    assertNotNull(enemy.getComponent(AnimationRenderComponent.class));
    assertNotNull(enemy.getComponent(EnemyAnimationController.class));
  }

  @Test
  void createdEnemyLifecycleDoesNotThrow() {
    Entity enemy = EnemyFactory.create("lesser_shade");

    assertDoesNotThrow(enemy::create);
  }

  @Test
  void createUsesRosterStats() {
    Entity enemy = EnemyFactory.create("lesser_shade");
    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);

    assertEquals(24, stats.getHealth());
  }

  @Test
  void createUsesRosterDisplayName() {
    Entity enemy = EnemyFactory.create("lesser_shade");
    EnemyStatsComponent enemyStats = enemy.getComponent(EnemyStatsComponent.class);

    assertNotNull(enemyStats.getDisplayName());
  }

  @Test
  void createFallsBackForUnknownId() {
    Entity enemy = EnemyFactory.create("does_not_exist");
    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);

    assertNotNull(stats);
    assertEquals(1, stats.getHealth()); // BaseEntityConfig default health
  }

  @Test
  void createFromConfigUsesConfigValues() {
    EnemyConfig config = new EnemyConfig();
    config.health = 50;
    config.baseAttack = 9;
    config.armour = 3;

    Entity enemy = EnemyFactory.create(config);
    CombatStatsComponent stats = enemy.getComponent(CombatStatsComponent.class);

    assertEquals(50, stats.getHealth());
    assertEquals(9, stats.getBaseAttack());
    assertEquals(3, stats.getArmor());
  }

  @Test
  void createWithFloorZeroKeepsBaseStats() {
    Entity enemy = EnemyFactory.create("void_knight", 0);

    assertEquals(72, enemy.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void createWithHigherFloorScalesStatsUp() {
    int baseHealth =
        EnemyFactory.create("void_knight", 0).getComponent(CombatStatsComponent.class).getHealth();
    int scaledHealth =
        EnemyFactory.create("void_knight", 5).getComponent(CombatStatsComponent.class).getHealth();

    assertTrue(scaledHealth > baseHealth);
  }

  @Test
  void getIdsByTierReturnsOnlyMatchingTier() {
    List<String> normals = EnemyFactory.getIdsByTier(EnemyTier.NORMAL);
    List<String> elites = EnemyFactory.getIdsByTier(EnemyTier.ELITE);

    assertTrue(normals.contains("lesser_shade"));
    assertFalse(normals.contains("void_knight"));
    assertTrue(elites.contains("void_knight"));
  }

  @Test
  void getIdsByTierReturnsEmptyListWhenNoneMatch() {
    assertTrue(EnemyFactory.getIdsByTier(EnemyTier.BOSS).isEmpty());
  }

  @Test
  void availableEnemiesIsNeverNull() {
    assertNotNull(EnemyFactory.availableEnemies());
  }

  @Test
  void getAtlasPathsCoversDefaultAndRosterEnemies() {
    List<String> paths = Arrays.asList(EnemyFactory.getAtlasPaths());

    assertTrue(paths.contains("images/enemies/default.atlas"));
    assertTrue(paths.contains("images/enemies/lesser_shade.atlas"));
    assertTrue(paths.contains("images/enemies/void_knight.atlas"));
  }

  @Test
  void getAtlasPathsHasNoDuplicates() {
    String[] paths = EnemyFactory.getAtlasPaths();

    assertEquals(paths.length, new HashSet<>(Arrays.asList(paths)).size());
  }

  @Test
  void loadAssetsQueuesEnemyAtlasesAndIntentIcons() {
    EnemyFactory.loadAssets();

    ArgumentCaptor<String[]> atlases = ArgumentCaptor.forClass(String[].class);
    verify(resourceService).loadTextureAtlases(atlases.capture());
    assertArrayEquals(EnemyFactory.getAtlasPaths(), atlases.getValue());

    verify(resourceService).loadTextures(IntentIcons.all());
  }

  @Test
  void unloadAssetsReleasesEnemyAtlasesAndIntentIcons() {
    EnemyFactory.unloadAssets();

    ArgumentCaptor<String[]> released = ArgumentCaptor.forClass(String[].class);
    verify(resourceService, atLeastOnce()).unloadAssets(released.capture());

    List<String[]> calls = released.getAllValues();
    assertTrue(calls.stream().anyMatch(a -> Arrays.equals(a, EnemyFactory.getAtlasPaths())));
    assertTrue(calls.stream().anyMatch(a -> Arrays.equals(a, IntentIcons.all())));
  }
}
