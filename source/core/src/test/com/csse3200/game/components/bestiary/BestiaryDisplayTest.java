package com.csse3200.game.components.bestiary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.bestiary.BestiaryEntryView;
import com.csse3200.game.bestiary.BestiaryService;
import com.csse3200.game.bestiary.BestiaryUnlockState;
import com.csse3200.game.entities.configs.EnemyConfig;
import com.csse3200.game.entities.configs.EnemyConfigs;
import com.csse3200.game.entities.configs.EnemyTier;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class BestiaryDisplayTest {
  private final BestiaryEntryView locked = createView(BestiaryUnlockState.LOCKED);
  private final BestiaryEntryView encountered = createView(BestiaryUnlockState.ENCOUNTERED);
  private final BestiaryEntryView defeated = createView(BestiaryUnlockState.DEFEATED);

  @Test
  void searchMatchesOnlyDiscoveredNamesAndDoesNotChangeProgress() {
    BestiaryService service = searchService();
    Map<String, BestiaryUnlockState> before = service.getProgressSnapshot();
    BestiaryDisplay display = createDisplay(service);
    display.create();
    Group root = rootActor();
    TextField search = root.findActor("bestiary-search");
    search.setText("  ShAdE  ");
    assertEquals(List.of("Lesser Shade"), visibleNames(root));
    assertEquals("1 result", resultCount(root));
    for (String query :
        List.of("Hidden Shade", "???", "a_shade", "images/enemies/", "24", "Enemy lore")) {
      search.setText(query);
      assertTrue(visibleNames(root).isEmpty(), query);
      assertEquals("0 results", resultCount(root));
      assertNull(display.getDisplayedEntry());
      assertEquals(BestiaryDisplay.NO_RESULTS_MESSAGE, display.getEmptyStateText());
    }
    assertEquals(before, service.getProgressSnapshot());
    display.dispose();
  }

  @Test
  void searchIntersectsEachTierAndClearingKeepsTheSelectedTierAndOrdering() {
    BestiaryDisplay display = createDisplay(searchService());
    display.create();
    Group root = rootActor();
    TextField search = root.findActor("bestiary-search");
    search.setText("shade");
    assertEquals(List.of("Lesser Shade"), visibleNames(root));
    display.applyFilter(EnemyTier.ELITE);
    assertEquals("shade", search.getText());
    assertEquals(List.of("Shade Knight"), visibleNames(root));
    display.applyFilter(EnemyTier.BOSS);
    assertEquals(List.of("Shade King"), visibleNames(root));
    search.setText("acolyte");
    assertTrue(visibleNames(root).isEmpty());
    TextButton clear = root.findActor("bestiary-search-clear");
    clear.setChecked(true);
    assertEquals("", search.getText());
    assertEquals(List.of("Shade King"), visibleNames(root));
    verify(ServiceLocator.getRenderService().getStage()).setKeyboardFocus(search);
    display.applyFilter(EnemyTier.NORMAL);
    search.setText(" \t ");
    assertEquals(List.of("Lesser Shade", "Dark Acolyte"), visibleNames(root));
    display.dispose();
  }

  @Test
  void filteredSelectionClearsDetailsAndOldButtonsCannotRestoreThem() {
    BestiaryService service = searchService();
    BestiaryDisplay display = createDisplay(service);
    display.create();
    Group root = rootActor();
    TextButton acolyte = button(root, "Dark Acolyte");
    acolyte.setChecked(true);
    assertEquals("b_acolyte", display.getDisplayedEntry().enemyId());
    TextField search = root.findActor("bestiary-search");
    search.setText("dark");
    assertEquals("b_acolyte", display.getDisplayedEntry().enemyId());
    search.setText("shade");
    assertNull(display.getDisplayedEntry());
    assertEquals("", ((Label) root.findActor("bestiary-stats")).getText().toString());
    assertNull(((Image) root.findActor("bestiary-artwork")).getDrawable());
    acolyte.setChecked(false);
    assertNull(display.getDisplayedEntry());
    TextButton shade = button(root, "Lesser Shade");
    shade.setChecked(true);
    assertEquals("a_shade", display.getDisplayedEntry().enemyId());
    service.replaceProgress(Map.of("b_acolyte", BestiaryUnlockState.DEFEATED));
    assertTrue(visibleNames(root).isEmpty());
    shade.setChecked(false);
    assertNull(display.getDisplayedEntry());
    display.dispose();
  }

  @Test
  void activeSearchRefreshesNewRecordsAndDefeatsWithoutRevealingEncounteredStats() {
    BestiaryService service = searchService();
    BestiaryDisplay display = createDisplay(service);
    display.create();
    Group root = rootActor();
    TextField search = root.findActor("bestiary-search");
    search.setText("hidden");
    assertTrue(visibleNames(root).isEmpty());
    service.recordEncountered("c_hidden");
    assertEquals(List.of("Hidden Shade"), visibleNames(root));
    assertNull(display.getDisplayedEntry());
    TextButton hidden = button(root, "Hidden Shade");
    hidden.setChecked(true);
    assertEquals(BestiaryUnlockState.ENCOUNTERED, display.getDisplayedEntry().unlockState());
    Label stats = root.findActor("bestiary-stats");
    Label description = root.findActor("bestiary-description");
    assertEquals("HP  ???     ATTACK  ???     ARMOUR  ???", stats.getText().toString());
    assertEquals(
        "Defeat this enemy to reveal its complete record.", description.getText().toString());
    service.recordDefeated("c_hidden");
    assertEquals("hidden", search.getText());
    assertSame(hidden, button(root, "Hidden Shade"));
    assertEquals(BestiaryUnlockState.DEFEATED, display.getDisplayedEntry().unlockState());
    assertTrue(stats.getText().toString().contains("HP  24"));
    assertEquals("Enemy lore", description.getText().toString());
    display.dispose();
    service.replaceProgress(Map.of());
    assertEquals(BestiaryUnlockState.DEFEATED, display.getDisplayedEntry().unlockState());
  }

  @Test
  void searchSupportsTypingDeletionPasteResizeAndReopeningWithoutStaleFocus() {
    Graphics previousGraphics = Gdx.graphics;
    Input previousInput = Gdx.input;
    Gdx.graphics = mock(Graphics.class);
    when(Gdx.graphics.getWidth()).thenReturn(1280);
    when(Gdx.graphics.getHeight()).thenReturn(800);
    Gdx.input = mock(Input.class);
    Stage stage = new Stage(new ScreenViewport(), mock(Batch.class));
    stage.getViewport().update(1280, 800, true);
    BestiaryService service = searchService();
    BestiaryDisplay display = createDisplay(service);
    when(ServiceLocator.getRenderService().getStage()).thenReturn(stage);
    try {
      display.create();
      TextField search = stage.getRoot().findActor("bestiary-search");
      stage.setKeyboardFocus(search);
      stage.keyDown(Keys.F);
      stage.keyTyped('F');
      stage.keyUp(Keys.F);
      assertEquals("F", search.getText());
      stage.keyTyped('\b');
      assertEquals("", search.getText());
      Clipboard clipboard = mock(Clipboard.class);
      when(clipboard.getContents()).thenReturn("shade");
      search.setClipboard(clipboard);
      when(Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)).thenReturn(true);
      stage.keyDown(Keys.V);
      stage.keyUp(Keys.V);
      when(Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)).thenReturn(false);
      assertEquals("shade", search.getText());
      assertEquals(List.of("Lesser Shade"), visibleNames(stage.getRoot()));
      when(Gdx.graphics.getWidth()).thenReturn(1920);
      when(Gdx.graphics.getHeight()).thenReturn(1080);
      stage.getViewport().update(1920, 1080, true);
      assertEquals("shade", search.getText());
      assertSame(search, stage.getKeyboardFocus());
      display.applyFilter(EnemyTier.ELITE);
      display.dispose();
      assertNull(stage.getKeyboardFocus());
      search.setText("detached field");
      BestiaryDisplay reopened = new BestiaryDisplay(service, () -> {});
      reopened.create();
      TextField fresh = stage.getRoot().findActor("bestiary-search");
      assertEquals("", fresh.getText());
      assertEquals(List.of("Lesser Shade", "Dark Acolyte"), visibleNames(stage.getRoot()));
      reopened.dispose();
    } finally {
      stage.dispose();
      Gdx.graphics = previousGraphics;
      Gdx.input = previousInput;
    }
  }

  @Test
  void longDefeatedRecordsScrollInsideThePanelAtBothSupportedResolutions() {
    Graphics previous = Gdx.graphics;
    Gdx.graphics = mock(Graphics.class);
    when(Gdx.graphics.getWidth()).thenReturn(1280);
    when(Gdx.graphics.getHeight()).thenReturn(720);
    Stage stage = new Stage(new ExtendViewport(1280, 800), mock(Batch.class));
    stage.getViewport().update(1280, 720, true);
    EnemyConfigs configs = new EnemyConfigs();
    EnemyConfig config = createConfig("long_record", "Long Record", EnemyTier.NORMAL);
    config.description =
        "An enemy's complete history remains available through scrolling. ".repeat(60);
    configs.enemies = new EnemyConfig[] {config};
    BestiaryService service = new BestiaryService(configs);
    service.recordDefeated(config.id);
    BestiaryDisplay display = createDisplay(service);
    when(ServiceLocator.getRenderService().getStage()).thenReturn(stage);
    try {
      display.create();
      for (int[] size : new int[][] {{1280, 720}, {1920, 1080}}) {
        when(Gdx.graphics.getWidth()).thenReturn(size[0]);
        when(Gdx.graphics.getHeight()).thenReturn(size[1]);
        stage.getViewport().update(size[0], size[1], true);
        ((Table) stage.getRoot().getChildren().first()).validate();
        ScrollPane details = stage.getRoot().findActor("bestiary-detail-scroll");
        details.validate();
        Vector2 lower = details.localToStageCoordinates(new Vector2());
        Vector2 upper =
            details.localToStageCoordinates(new Vector2(details.getWidth(), details.getHeight()));
        assertTrue(lower.x >= 0f && lower.y >= 0f);
        assertTrue(upper.x <= stage.getWidth() && upper.y <= stage.getHeight());
        assertTrue(details.getMaxY() > 100f);
        assertTrue(details.getActor().getWidth() <= details.getScrollWidth());
        details.setScrollY(details.getMaxY());
        assertTrue(details.getScrollY() > 0f);
        Label description = stage.getRoot().findActor("bestiary-description");
        assertEquals(
            service.getEntry(config.id).orElseThrow().description().orElseThrow(),
            description.getText().toString());
      }
      display.dispose();
    } finally {
      stage.dispose();
      Gdx.graphics = previous;
    }
  }

  @Test
  void queryChangesResetListScrollWithoutReplacingSearchControls() {
    Graphics previous = Gdx.graphics;
    Gdx.graphics = mock(Graphics.class);
    when(Gdx.graphics.getWidth()).thenReturn(1280);
    when(Gdx.graphics.getHeight()).thenReturn(800);
    Stage stage = new Stage(new ScreenViewport(), mock(Batch.class));
    stage.getViewport().update(1280, 800, true);
    EnemyConfigs configs = new EnemyConfigs();
    configs.enemies = new EnemyConfig[30];
    for (int i = 0; i < configs.enemies.length; i++) {
      configs.enemies[i] = createConfig("enemy_" + i, "Enemy " + i, EnemyTier.NORMAL);
    }
    BestiaryService service = new BestiaryService(configs);
    for (EnemyConfig config : configs.enemies) service.recordEncountered(config.id);
    BestiaryDisplay display = createDisplay(service);
    when(ServiceLocator.getRenderService().getStage()).thenReturn(stage);
    try {
      display.create();
      ((Table) stage.getRoot().getChildren().first()).validate();
      ScrollPane scroll = stage.getRoot().findActor("bestiary-enemy-scroll");
      scroll.validate();
      assertTrue(scroll.getMaxY() > 100f);
      scroll.setScrollY(100f);
      TextField search = stage.getRoot().findActor("bestiary-search");
      search.setText("Enemy");
      assertEquals(0f, scroll.getScrollY());
      assertSame(search, stage.getRoot().findActor("bestiary-search"));
      display.dispose();
    } finally {
      stage.dispose();
      Gdx.graphics = previous;
    }
  }

  private BestiaryService searchService() {
    EnemyConfigs configs = new EnemyConfigs();
    configs.enemies =
        new EnemyConfig[] {
          createConfig("a_shade", "Lesser Shade", EnemyTier.NORMAL),
          createConfig("b_acolyte", "Dark Acolyte", EnemyTier.NORMAL),
          createConfig("c_hidden", "Hidden Shade", EnemyTier.NORMAL),
          createConfig("d_knight", "Shade Knight", EnemyTier.ELITE),
          createConfig("e_king", "Shade King", EnemyTier.BOSS)
        };
    for (EnemyConfig config : configs.enemies) config.description = "Enemy lore";
    BestiaryService service = new BestiaryService(configs);
    service.recordEncountered("a_shade");
    service.recordDefeated("b_acolyte");
    service.recordEncountered("d_knight");
    service.recordDefeated("e_king");
    return service;
  }

  private static Group rootActor() {
    ArgumentCaptor<Actor> root = ArgumentCaptor.forClass(Actor.class);
    verify(ServiceLocator.getRenderService().getStage()).addActor(root.capture());
    return (Group) root.getValue();
  }

  private static List<String> visibleNames(Group root) {
    Group list = root.findActor("bestiary-enemy-list");
    List<String> names = new ArrayList<>();
    for (Actor actor : list.getChildren()) {
      if (actor instanceof TextButton button) names.add(button.getText().toString());
    }
    return names;
  }

  private static TextButton button(Group root, String name) {
    Group list = root.findActor("bestiary-enemy-list");
    for (Actor actor : list.getChildren()) {
      if (actor instanceof TextButton button && name.contentEquals(button.getText())) return button;
    }
    throw new AssertionError("No visible enemy named " + name);
  }

  private static String resultCount(Group root) {
    return ((Label) root.findActor("bestiary-result-count")).getText().toString();
  }

  @Test
  void shouldKeepLockedEntryDataObscuredAsDefenceInDepth() {
    assertEquals("???", BestiaryDisplay.visibleName(locked));
    assertEquals(
        "Encounter this enemy to reveal its record.", BestiaryDisplay.descriptionFor(locked));
    assertEquals("HP  ???     ATTACK  ???     ARMOUR  ???", BestiaryDisplay.statsFor(locked));
  }

  @Test
  void shouldKeepEncounteredCombatDetailsHidden() {
    assertEquals("Enemy", BestiaryDisplay.visibleName(encountered));
    assertEquals(
        "Defeat this enemy to reveal its complete record.",
        BestiaryDisplay.descriptionFor(encountered));
    assertEquals("HP  ???     ATTACK  ???     ARMOUR  ???", BestiaryDisplay.statsFor(encountered));
  }

  @Test
  void shouldShowDefeatedEnemyDetails() {
    assertEquals("Enemy description", BestiaryDisplay.descriptionFor(defeated));
    assertEquals(
        "HP  24     ATTACK  6     ARMOUR  2\nBEHAVIOUR  CYCLE ATTACK DEFEND",
        BestiaryDisplay.statsFor(defeated));
  }

  @Test
  void shouldUseFallbackWhenDefeatedDescriptionIsMissing() {
    BestiaryEntryView noDescription =
        new BestiaryEntryView(
            "enemy",
            EnemyTier.NORMAL,
            BestiaryUnlockState.DEFEATED,
            "Enemy",
            Optional.of("images/enemies/default.atlas"),
            Optional.empty(),
            OptionalInt.of(24),
            OptionalInt.of(6),
            OptionalInt.of(2),
            Optional.of("cycle_attack_defend"));

    assertEquals("No description available.", BestiaryDisplay.descriptionFor(noDescription));
  }

  @Test
  void shouldHideUndiscoveredEntryThenRevealItOnEncounter() {
    EnemyConfigs configs = new EnemyConfigs();
    configs.enemies = new EnemyConfig[] {createConfig("enemy", "Enemy", EnemyTier.NORMAL)};
    BestiaryService service = new BestiaryService(configs);
    BestiaryDisplay display = createDisplay(service);
    display.create();
    assertNull(display.getDisplayedEntry());
    assertEquals(BestiaryDisplay.EMPTY_STATE_MESSAGE, display.getEmptyStateText());

    service.recordEncountered("enemy");
    assertEquals(BestiaryUnlockState.ENCOUNTERED, display.getDisplayedEntry().unlockState());
    assertNull(display.getEmptyStateText());

    display.dispose();
    service.recordDefeated("enemy");
    assertEquals(BestiaryUnlockState.ENCOUNTERED, display.getDisplayedEntry().unlockState());
  }

  @Test
  void shouldSwitchTiersAndRefreshAnEmptyCategoryAfterDiscovery() {
    EnemyConfigs configs = new EnemyConfigs();
    configs.enemies =
        new EnemyConfig[] {
          createConfig("normal_enemy", "Normal Enemy", EnemyTier.NORMAL),
          createConfig("elite_enemy", "Elite Enemy", EnemyTier.ELITE),
          createConfig("boss_enemy", "Boss Enemy", EnemyTier.BOSS)
        };
    BestiaryService service = new BestiaryService(configs);
    service.recordEncountered("elite_enemy");

    BestiaryDisplay display = createDisplay(service);
    display.create();

    assertNull(display.getDisplayedEntry());
    assertEquals(BestiaryDisplay.EMPTY_STATE_MESSAGE, display.getEmptyStateText());

    display.applyFilter(EnemyTier.ELITE);
    assertEquals("elite_enemy", display.getDisplayedEntry().enemyId());
    assertNull(display.getEmptyStateText());

    display.applyFilter(EnemyTier.BOSS);
    assertNull(display.getDisplayedEntry());
    assertEquals(BestiaryDisplay.EMPTY_STATE_MESSAGE, display.getEmptyStateText());

    service.recordEncountered("boss_enemy");
    assertEquals("boss_enemy", display.getDisplayedEntry().enemyId());
    assertNull(display.getEmptyStateText());

    display.applyFilter(EnemyTier.NORMAL);
    assertNull(display.getDisplayedEntry());
    assertEquals(BestiaryDisplay.EMPTY_STATE_MESSAGE, display.getEmptyStateText());

    display.dispose();
  }

  private BestiaryDisplay createDisplay(BestiaryService service) {
    RenderService renderService = mock(RenderService.class);
    when(renderService.getStage()).thenReturn(mock(Stage.class));
    ServiceLocator.registerRenderService(renderService);
    ResourceService resourceService = mock(ResourceService.class);
    when(resourceService.getAsset(BestiaryDisplay.BUTTON_TEXTURE, Texture.class))
        .thenReturn(mock(Texture.class));
    ServiceLocator.registerResourceService(resourceService);
    return new BestiaryDisplay(service, () -> {});
  }

  private EnemyConfig createConfig(String id, String name, EnemyTier tier) {
    EnemyConfig config = new EnemyConfig();
    config.id = id;
    config.name = name;
    config.tier = tier;
    config.health = 24;
    return config;
  }

  private BestiaryEntryView createView(BestiaryUnlockState state) {
    boolean encountered = state.isAtLeast(BestiaryUnlockState.ENCOUNTERED);
    boolean defeatedState = state == BestiaryUnlockState.DEFEATED;
    return new BestiaryEntryView(
        "enemy",
        EnemyTier.NORMAL,
        state,
        encountered ? "Enemy" : "???",
        encountered ? Optional.of("images/enemies/default.atlas") : Optional.empty(),
        defeatedState ? Optional.of("Enemy description") : Optional.empty(),
        defeatedState ? OptionalInt.of(24) : OptionalInt.empty(),
        defeatedState ? OptionalInt.of(6) : OptionalInt.empty(),
        defeatedState ? OptionalInt.of(2) : OptionalInt.empty(),
        defeatedState ? Optional.of("cycle_attack_defend") : Optional.empty());
  }
}
