package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.effects.ResolvedCardEffect;
import com.csse3200.game.components.cards.CardEffectHandler;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

@ExtendWith(GameExtension.class)
class BattleAnimationCoordinatorTest {
  private ResourceService resources;
  private EntityService entities;
  private BattleAnimationCoordinator coordinator;
  private EventListener1<List<ResolvedCardEffect>> playerEffects;

  @BeforeEach
  void setUp() {
    resources = mock(ResourceService.class);
    ServiceLocator.registerResourceService(resources);
    ServiceLocator.registerRenderService(mock(RenderService.class));
    entities = spy(new EntityService());
    ServiceLocator.registerEntityService(entities);
    GameTime time = mock(GameTime.class);
    when(time.getDeltaTime()).thenReturn(0.2f);
    ServiceLocator.registerTimeSource(time);
  }

  @AfterEach
  void tearDown() {
    if (coordinator != null) {
      coordinator.dispose();
    }
  }

  private void createCoordinator(EffectVisualRegistry registry) {
    BattleController controller = mock(BattleController.class);
    Entity player = new Entity();
    player.setScale(2f, 2f);
    coordinator =
        new BattleAnimationCoordinator(
            controller, mock(CardEffectHandler.class), List.of(), player, registry);
    coordinator.create();
    @SuppressWarnings("unchecked")
    ArgumentCaptor<EventListener1<List<ResolvedCardEffect>>> listener =
        ArgumentCaptor.forClass(EventListener1.class);
    verify(controller).addPlayerEffectsListener(listener.capture());
    playerEffects = listener.getValue();
  }

  private static ResolvedCardEffect effect(EffectType type) {
    return new ResolvedCardEffect("test-card", type, TargetType.SELF, 1, 0, 0);
  }

  private Entity spawnedVisual() {
    ArgumentCaptor<Entity> visual = ArgumentCaptor.forClass(Entity.class);
    verify(entities).register(visual.capture());
    return visual.getValue();
  }

  @Test
  void shouldDrawUnregisteredEffectsUsingOneSharedGeneratedGlow() {
    createCoordinator(new EffectVisualRegistry());
    try (MockedConstruction<Texture> textures =
        mockConstruction(
            Texture.class,
            (texture, context) -> {
              Pixmap pixels = (Pixmap) context.arguments().get(0);
              assertEquals(64, pixels.getWidth());
              assertEquals(64, pixels.getHeight());
              Color centre = new Color(pixels.getPixel(32, 32));
              Color edge = new Color(pixels.getPixel(0, 0));
              assertTrue(centre.a > 0.9f);
              assertEquals(0f, edge.a);
              assertEquals(1f, centre.r);
            })) {
      playerEffects.handle(List.of(effect(EffectType.HEAL), effect(EffectType.BLOCK)));
      assertEquals(1, textures.constructed().size());
      Texture glow = textures.constructed().get(0);
      verify(glow).setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
      ArgumentCaptor<Entity> visuals = ArgumentCaptor.forClass(Entity.class);
      verify(entities, times(2)).register(visuals.capture());
      SpriteBatch batch = mock(SpriteBatch.class);
      when(batch.getPackedColor()).thenReturn(123f);
      for (Entity visual : visuals.getAllValues()) {
        EffectVisualComponent component = visual.getComponent(EffectVisualComponent.class);
        component.update();
        component.update();
        component.render(batch);
      }
      verify(batch, times(2)).draw(eq(glow), anyFloat(), anyFloat(), anyFloat(), anyFloat());
      verify(batch, times(2)).setPackedColor(123f);
      verifyNoInteractions(resources);

      for (Entity visual : visuals.getAllValues()) {
        EffectVisualComponent component = visual.getComponent(EffectVisualComponent.class);
        component.update();
        component.update();
      }
      coordinator.update();
      for (Entity visual : visuals.getAllValues()) {
        verify(entities).unregister(visual);
      }
      verify(glow, never()).dispose();

      coordinator.dispose();
      coordinator.dispose();
      verify(glow).dispose();
      playerEffects.handle(List.of(effect(EffectType.HEAL)));
      verify(entities, times(2)).register(any(Entity.class));
      assertEquals(1, textures.constructed().size());
    }
  }

  @Test
  void shouldUseGeneratedGlowWhenRegisteredIconIsNotLoaded() {
    EffectVisualRegistry registry = new EffectVisualRegistry();
    registry.register(
        EffectType.HEAL, new EffectVisualStyle("missing-icon.png", Color.GREEN, 0.4f, 1f, 1f, 0f));
    createCoordinator(registry);
    when(resources.getAsset(anyString(), eq(Texture.class)))
        .thenThrow(new GdxRuntimeException("Asset not loaded"));

    try (MockedConstruction<Texture> textures = mockConstruction(Texture.class)) {
      playerEffects.handle(List.of(effect(EffectType.HEAL)));

      assertEquals(1, textures.constructed().size());
      SpriteBatch batch = mock(SpriteBatch.class);
      spawnedVisual().getComponent(EffectVisualComponent.class).render(batch);
      verify(batch)
          .draw(eq(textures.constructed().get(0)), anyFloat(), anyFloat(), anyFloat(), anyFloat());
      verify(batch).setColor(0f, 1f, 0f, 1f);
    }
  }

  @Test
  void shouldBorrowManagedIconsWithoutGeneratingOrDisposingThem() {
    EffectVisualRegistry registry = new EffectVisualRegistry();
    PlayerEffectVisuals.registerAll(registry);
    createCoordinator(registry);
    Texture icon = mock(Texture.class);
    when(resources.getAsset(anyString(), eq(Texture.class))).thenReturn(icon);

    try (MockedConstruction<Texture> textures = mockConstruction(Texture.class)) {
      playerEffects.handle(List.of(effect(EffectType.HEAL)));

      assertTrue(textures.constructed().isEmpty());
      SpriteBatch batch = mock(SpriteBatch.class);
      spawnedVisual().getComponent(EffectVisualComponent.class).render(batch);
      verify(batch).draw(eq(icon), anyFloat(), anyFloat(), anyFloat(), anyFloat());
      coordinator.dispose();
      verify(icon, never()).dispose();
    }
  }
}
