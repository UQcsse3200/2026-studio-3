package com.csse3200.game.components.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
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
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rewards.ItemDiscoveryService;
import com.csse3200.game.rewards.ItemFormatting;
import com.csse3200.game.rewards.ItemType;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.ItemIconView;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
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
  private ItemDiscoveryService discovery;

  @BeforeEach
  void setUp() {
    stage = new Stage(new FitViewport(1280f, 800f), mock(Batch.class));
    RenderService renderService = new RenderService();
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerEntityService(new EntityService());
    game = mock(GdxGame.class);
    discovery = new ItemDiscoveryService();
    for (ItemType item : ItemType.values()) {
      discovery.recordDiscovered(item);
    }
    when(game.getItemDiscoveryService()).thenReturn(discovery);
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
  void undiscoveredItemsKeepTheirSlotsButHideDetailsAndArtworkUntilUnlocked() {
    discovery.replaceProgress(Map.of());

    assertEquals(relicCount(), itemEntryButtons().size());
    assertTrue(
        itemEntryButtons().stream().allMatch(button -> button.getText().toString().equals("???")));
    assertEquals("Discovered 0 / " + ItemType.values().length, discoveryCountText());
    for (ItemType item : ItemType.values()) {
      String name = ItemFormatting.formatItemName(item);
      assertFalse(buttonTexts().contains(name), name);
      assertFalse(labelTexts().contains(name), name);
    }
    assertTrue(labelTexts().contains("???"));
    assertTrue(labelTexts().contains("Obtain this item during a run to reveal it."));
    assertFalse(labelTexts().stream().anyMatch(text -> text.startsWith("Obtained from:")));
    assertFalse(labelTexts().contains("+1 max energy per copy, capped at 5 total max energy."));
    ItemIconView icon = (ItemIconView) stage.getRoot().findActor("itemIconView");
    assertFalse(icon.getArtwork().isVisible());
    assertTrue(((Label) stage.getRoot().findActor("itemLockedArtwork")).isVisible());

    itemEntryButtons()
        .get(1)
        .fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());
    assertTrue(labelTexts().contains("Obtain this item during a run to reveal it."));
    assertFalse(icon.getArtwork().isVisible());
    discovery.recordDiscovered(ItemType.ENERGY_CRYSTAL);

    assertEquals("Discovered 1 / " + ItemType.values().length, discoveryCountText());
    assertEquals(
        1,
        itemEntryButtons().stream()
            .filter(
                button ->
                    button
                        .getText()
                        .toString()
                        .equals(ItemFormatting.formatItemName(ItemType.ENERGY_CRYSTAL)))
            .count());
    assertTrue(labelTexts().contains("+1 max energy per copy, capped at 5 total max energy."));
    assertTrue(
        labelTexts()
            .contains(
                "Obtained from: " + ItemFormatting.formatItemSource(ItemType.ENERGY_CRYSTAL)));
    assertTrue(icon.getArtwork().isVisible());
    assertFalse(((Label) stage.getRoot().findActor("itemLockedArtwork")).isVisible());
    assertFalse(buttonTexts().contains(ItemFormatting.formatItemName(ItemType.IRON_AEGIS)));
  }

  @Test
  void lockedCurseSlotsNeverRevealNamesEffectsOrSources() {
    discovery.replaceProgress(Map.of());
    button("Curses").setChecked(true);

    assertEquals(curseCount(), itemEntryButtons().size());
    assertTrue(
        itemEntryButtons().stream().allMatch(button -> button.getText().toString().equals("???")));
    assertTrue(labelTexts().contains("Obtain this item during a run to reveal it."));
    assertFalse(labelTexts().stream().anyMatch(text -> text.startsWith("Obtained from:")));
    assertFalse(labelTexts().stream().anyMatch(text -> text.contains("Lose 2 Health")));
    assertFalse(buttonTexts().contains(ItemFormatting.formatItemName(ItemType.BLOOD_OATH_SIGIL)));

    discovery.recordDiscovered(ItemType.BLOOD_OATH_SIGIL);

    assertEquals("Discovered 1 / " + ItemType.values().length, discoveryCountText());
    assertTrue(buttonTexts().contains(ItemFormatting.formatItemName(ItemType.BLOOD_OATH_SIGIL)));
    assertFalse(buttonTexts().contains(ItemFormatting.formatItemName(ItemType.ENERGY_CRYSTAL)));
  }

  @Test
  void consumedItemStaysDiscoveredInTheLibrary() {
    discovery.replaceProgress(Map.of());
    PlayerRunState player = new PlayerRunState(20, 20, 0);
    player.addOwnedItem(ItemType.LUCKY_COIN);
    discovery.recordDiscovered(ItemType.LUCKY_COIN);
    player.claimGoldReward(10, true);

    assertEquals(0, player.getOwnedItemCount(ItemType.LUCKY_COIN));
    assertTrue(discovery.isDiscovered(ItemType.LUCKY_COIN));
    assertTrue(buttonTexts().contains(ItemFormatting.formatItemName(ItemType.LUCKY_COIN)));
    assertEquals("Discovered 1 / " + ItemType.values().length, discoveryCountText());
    assertFalse(((Label) stage.getRoot().findActor("itemLockedArtwork")).isVisible());
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
  void everyItemHasItsOwnSquareArtworkAsset() throws IOException {
    for (ItemType item : ItemType.values()) {
      String path = ItemLibraryDisplay.resolveArtworkPath(item);
      assertTrue(Gdx.files.internal(path).exists(), path);
      try (InputStream input = Gdx.files.internal(path).read()) {
        var image = ImageIO.read(input);
        assertNotNull(image, path);
        assertEquals(96, image.getWidth(), path);
        assertEquals(96, image.getHeight(), path);
      }
    }
  }

  @Test
  void everyItemHasTransparentCutoutWhileLibraryKeepsFramedArtwork() throws IOException {
    for (ItemType item : ItemType.values()) {
      String slug = item.name().toLowerCase(Locale.ROOT).replace('_', '-');
      String framedPath = ItemLibraryDisplay.resolveArtworkPath(item);
      String cutoutPath = "images/ui/item-cutouts/" + slug + ".png";
      assertEquals("images/ui/" + slug + ".png", framedPath);
      assertTrue(Gdx.files.internal(framedPath).exists(), framedPath);
      assertTrue(Gdx.files.internal(cutoutPath).exists(), cutoutPath);
      try (InputStream input = Gdx.files.internal(cutoutPath).read()) {
        var image = ImageIO.read(input);
        assertNotNull(image, cutoutPath);
        assertEquals(96, image.getWidth(), cutoutPath);
        assertEquals(96, image.getHeight(), cutoutPath);
        assertTrue(image.getColorModel().hasAlpha(), cutoutPath);
        assertEquals(0, image.getRGB(0, 0) >>> 24, cutoutPath);
      }
    }
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

  private List<TextButton> itemEntryButtons() {
    Table entries = (Table) stage.getRoot().findActor("itemLibraryEntries");
    List<TextButton> buttons = new ArrayList<>();
    for (Actor actor : entries.getChildren()) {
      buttons.add((TextButton) actor);
    }
    return buttons;
  }

  private String discoveryCountText() {
    return ((Label) stage.getRoot().findActor("itemDiscoveryCount")).getText().toString();
  }

  private long relicCount() {
    return java.util.Arrays.stream(ItemType.values()).filter(item -> !item.isCurse()).count();
  }

  private long curseCount() {
    return java.util.Arrays.stream(ItemType.values()).filter(ItemType::isCurse).count();
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
