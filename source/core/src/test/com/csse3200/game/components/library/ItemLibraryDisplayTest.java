package com.csse3200.game.components.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.GdxGame;
import com.csse3200.game.components.mainmenu.MainMenuDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rewards.ItemFormatting;
import com.csse3200.game.rewards.ItemType;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ItemLibraryDisplayTest {
  private Stage stage;
  private Entity entity;
  private GdxGame game;
  private ResourceService resources;

  @BeforeEach
  void setUp() {
    stage = new Stage(new FitViewport(1280f, 800f), mock(Batch.class));
    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerEntityService(new EntityService());
    game = mock(GdxGame.class);
    resources = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    when(texture.getWidth()).thenReturn(64);
    when(texture.getHeight()).thenReturn(64);
    when(resources.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);
    when(resources.containsAsset(anyString(), eq(Texture.class))).thenReturn(true);
    ServiceLocator.registerResourceService(resources);
    entity = new Entity().addComponent(new ItemLibraryDisplay(game));
    entity.create();
  }

  @AfterEach
  void tearDown() {
    entity.dispose();
    stage.dispose();
  }

  @Test
  void relicsAndCursesAreSeparatedAndShowTheirActualEffects() {
    assertTrue(buttonTexts().contains("Eternal Vitality"));
    assertFalse(buttonTexts().contains("Blood-Oath Sigil"));
    button(ItemFormatting.formatItemName(ItemType.ENERGY_CRYSTAL))
        .fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());
    assertTrue(labelTexts().contains("+1 max energy per copy, capped at 5 total max energy."));

    button("Curses").setChecked(true);
    assertTrue(buttonTexts().contains("Blood-Oath Sigil"));
    assertFalse(buttonTexts().contains("Eternal Vitality"));

    button(ItemFormatting.formatItemName(ItemType.BLOOD_OATH_SIGIL))
        .fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());
    assertTrue(
        labelTexts().stream()
            .anyMatch(text -> text.contains("Lose 2 Health") && text.contains("Bypasses Block")));
    assertTrue(
        labelTexts()
            .contains(
                "Obtained from: " + ItemFormatting.formatItemSource(ItemType.BLOOD_OATH_SIGIL)));
  }

  @Test
  void backButtonOpensLibraryAndArtworkPathFollowsItemType() {
    assertEquals(
        "images/ui/eternal-vitality.png",
        ItemLibraryDisplay.resolveArtworkPath(ItemType.ETERNAL_VITALITY));
    verify(resources).getAsset(MainMenuDisplay.BACKGROUND_TEXTURE, Texture.class);
    button("Back").fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());
    verify(game).setScreen(GdxGame.ScreenType.LIBRARY);
  }

  @Test
  void missingItemArtworkHidesOnlyTheArtwork() {
    String path = ItemLibraryDisplay.resolveArtworkPath(ItemType.ETERNAL_VITALITY);
    when(resources.containsAsset(path, Texture.class)).thenReturn(false);

    button(ItemFormatting.formatItemName(ItemType.ETERNAL_VITALITY))
        .fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());

    assertTrue(labelTexts().contains("+20 max Health per copy, and fully heal when obtained."));
    assertTrue(
        actors().stream()
            .filter(Image.class::isInstance)
            .map(Image.class::cast)
            .anyMatch(image -> !image.isVisible()));
  }

  @Test
  void libraryPanelKeepsItsLogicalSizeAfterWindowResize() {
    Table wrapper = (Table) stage.getRoot().findActor("itemLibraryWrapper");
    Table panel = (Table) stage.getRoot().findActor("itemLibraryPanel");
    assertEquals(panel, wrapper.getCells().first().getActor());
    assertEquals(1120f, wrapper.getCells().first().getPrefWidth(), 0.01f);
    assertEquals(680f, wrapper.getCells().first().getPrefHeight(), 0.01f);

    stage.getViewport().update(1920, 1080, true);
    assertEquals(1120f, wrapper.getCells().first().getPrefWidth(), 0.01f);
    assertEquals(680f, wrapper.getCells().first().getPrefHeight(), 0.01f);
  }

  private TextButton button(String text) {
    return actors().stream()
        .filter(TextButton.class::isInstance)
        .map(TextButton.class::cast)
        .filter(button -> text.equals(button.getText().toString()))
        .findFirst()
        .orElseThrow();
  }

  private List<String> buttonTexts() {
    return actors().stream()
        .filter(TextButton.class::isInstance)
        .map(TextButton.class::cast)
        .map(button -> button.getText().toString())
        .toList();
  }

  private List<String> labelTexts() {
    return actors().stream()
        .filter(Label.class::isInstance)
        .map(Label.class::cast)
        .map(label -> label.getText().toString())
        .toList();
  }

  private List<Actor> actors() {
    List<Actor> found = new ArrayList<>();
    collect(stage.getRoot(), found);
    return found;
  }

  private void collect(Actor actor, List<Actor> found) {
    found.add(actor);
    if (actor instanceof Group group) {
      for (Actor child : group.getChildren()) {
        collect(child, found);
      }
    }
  }
}
