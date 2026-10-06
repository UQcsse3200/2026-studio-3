package com.csse3200.game.areas;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.enemy.IntentIcons;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.components.player.PlayerStatsDisplay;
import com.csse3200.game.components.player.PlayerStatsTopDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.factories.PlayerFactory;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.MapGraph;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

@ExtendWith(GameExtension.class)
class EncounterGameAreaAssetsTest {
  @Test
  void shouldPreloadAndUnloadPlayerHudIconsForShop() {
    verifyPlayerHudAssetLifecycle(RoomType.SHOP, null);
  }

  @Test
  void shouldPreloadAndUnloadPlayerHudIconsForChanceEncounter() {
    verifyPlayerHudAssetLifecycle(RoomType.EVENT, "wishing-fountain");
  }

  private static void verifyPlayerHudAssetLifecycle(RoomType roomType, String eventId) {
    ResourceService resources = mock(ResourceService.class);
    Set<String> queued = new HashSet<>();
    Set<String> loaded = new HashSet<>();
    doAnswer(
            invocation -> {
              queued.addAll(Arrays.asList(invocation.getArgument(0, String[].class)));
              return null;
            })
        .when(resources)
        .loadTextures(any(String[].class));
    when(resources.loadForMillis(10))
        .thenAnswer(
            invocation -> {
              loaded.addAll(queued);
              return true;
            });
    doAnswer(
            invocation -> {
              loaded.removeAll(Arrays.asList(invocation.getArgument(0, String[].class)));
              return null;
            })
        .when(resources)
        .unloadAssets(any(String[].class));
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(20);
    when(texture.getHeight()).thenReturn(20);
    when(resources.getAsset(anyString(), eq(Texture.class)))
        .thenAnswer(
            invocation -> {
              String path = invocation.getArgument(0);
              if (!loaded.contains(path)) {
                throw new GdxRuntimeException("Asset not loaded: " + path);
              }
              return texture;
            });
    ServiceLocator.registerResourceService(resources);
    RenderService renderService = mock(RenderService.class);
    Stage stage = mock(Stage.class);
    when(renderService.getStage()).thenReturn(stage);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerCardLibrary(new CardLibrary(CardConfigLoader.loadCards()));

    RunState run = new RunState();
    run.setMapGraph(new MapGraph(Map.of(7, new MapNode(7, roomType)), false));
    PlayerStatsDisplay hud = new PlayerStatsDisplay();
    PlayerStatsTopDisplay topHud = new PlayerStatsTopDisplay(run);
    AtomicBoolean topHudCreated = new AtomicBoolean();
    Entity player =
        new Entity()
            .addComponent(new CombatStatsComponent(30, 4))
            .addComponent(new EnergyComponent(3))
            .addComponent(new InventoryComponent(20))
            .addComponent(hud)
            .addComponent(topHud);
    EncounterGameArea area =
        new EncounterGameArea(
            mock(TerrainFactory.class),
            7,
            roomType,
            (nodeId, success) -> {
              // Asset lifecycle verification does not complete an encounter.
            },
            null,
            null,
            run,
            eventId) {
          @Override
          protected void spawnEntity(Entity entity) {
            // Only player HUD asset dependencies are exercised, not the Shop/Event UI itself.
          }
        };

    try (MockedStatic<PlayerFactory> factory = mockStatic(PlayerFactory.class)) {
      factory
          .when(() -> PlayerFactory.createPlayer(run))
          .thenAnswer(
              invocation -> {
                assertTrue(loaded.containsAll(Arrays.asList(IntentIcons.all())));
                hud.create();
                topHud.create();
                topHudCreated.set(true);
                return player;
              });
      area.create();

      assertSame(player, area.getPlayer());
      for (String status : new String[] {"SILENCE", "DAMAGE_ON_CARD_PLAY", "TAUNT:42"}) {
        player.getComponent(CombatStatsComponent.class).applyStatusEffect(status, 1, 2);
        hud.update();
        verify(resources).getAsset(IntentIcons.pathForStatus(status), Texture.class);
      }
      verify(resources).getAsset(IntentIcons.DEBUFF, Texture.class);
      verify(resources).loadTextures(IntentIcons.all());
      factory.verify(() -> PlayerFactory.createPlayer(run));
    } finally {
      hud.dispose();
      if (topHudCreated.get()) {
        topHud.dispose();
      }
      area.dispose();
    }

    verify(resources).unloadAssets(IntentIcons.all());
    assertTrue(loaded.isEmpty());
  }
}
