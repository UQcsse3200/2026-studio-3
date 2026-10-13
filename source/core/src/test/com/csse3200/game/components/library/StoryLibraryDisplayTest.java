package com.csse3200.game.components.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.csse3200.game.GdxGame;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.story.StoryEntry;
import com.csse3200.game.story.StoryEntryLoader;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(GameExtension.class)
class StoryLibraryDisplayTest {
  private static final List<StoryEntry> ENTRIES =
      List.of(
          new StoryEntry("one", "One", List.of("First paragraph.", "Second paragraph.")),
          new StoryEntry("two", "Two", List.of("Only paragraph.")),
          new StoryEntry("three", "Three", List.of("Last entry.")));

  @Test
  void showsFirstEntryAndListsEveryEntryInOrder() {
    StoryLibraryDisplay display = createDisplay(mock(GdxGame.class), ENTRIES);
    display.create();

    Group list = findRoot().findActor(StoryLibraryDisplay.ENTRY_LIST_NAME);
    assertEquals(ENTRIES.size(), list.getChildren().size);
    for (int i = 0; i < ENTRIES.size(); i++) {
      TextButton button = assertInstanceOf(TextButton.class, list.getChildren().get(i));
      assertEquals(ENTRIES.get(i).title(), button.getText().toString());
    }

    assertEquals(0, display.getSelectedIndex());
    assertEquals("One", display.getDisplayedTitle());
    assertEquals("First paragraph.\n\nSecond paragraph.", display.getDisplayedBody());
    display.dispose();
  }

  @Test
  void clickingAnEntryShowsItsText() {
    StoryLibraryDisplay display = createDisplay(mock(GdxGame.class), ENTRIES);
    display.create();

    Group list = findRoot().findActor(StoryLibraryDisplay.ENTRY_LIST_NAME);
    TextButton third = (TextButton) list.getChildren().get(2);
    third.setChecked(true);

    assertEquals(2, display.getSelectedIndex());
    assertEquals("Three", display.getDisplayedTitle());
    assertEquals("Last entry.", display.getDisplayedBody());
    assertFalse(((TextButton) list.getChildren().get(0)).isChecked());
    display.dispose();
  }

  @Test
  void arrowKeysMoveBetweenEntriesAndStopAtTheEnds() {
    StoryLibraryDisplay display = createDisplay(mock(GdxGame.class), ENTRIES);
    display.create();

    assertTrue(display.handleKey(Input.Keys.UP));
    assertEquals(0, display.getSelectedIndex());

    assertTrue(display.handleKey(Input.Keys.DOWN));
    assertTrue(display.handleKey(Input.Keys.S));
    assertEquals(2, display.getSelectedIndex());
    assertTrue(display.handleKey(Input.Keys.DOWN));
    assertEquals(2, display.getSelectedIndex());

    assertTrue(display.handleKey(Input.Keys.W));
    assertEquals(1, display.getSelectedIndex());
    assertEquals("Two", display.getDisplayedTitle());

    assertFalse(display.handleKey(Input.Keys.SPACE));
    display.dispose();
  }

  @Test
  void escapeReturnsToTheLibraryMenu() {
    GdxGame game = mock(GdxGame.class);
    StoryLibraryDisplay display = createDisplay(game, ENTRIES);
    display.create();

    assertTrue(display.handleKey(Input.Keys.ESCAPE));
    verify(game).setScreen(GdxGame.ScreenType.LIBRARY);
    display.dispose();
  }

  @Test
  void showsAFallbackMessageWhenThereAreNoEntries() {
    StoryLibraryDisplay display = createDisplay(mock(GdxGame.class), List.of());
    display.create();

    Group list = findRoot().findActor(StoryLibraryDisplay.ENTRY_LIST_NAME);
    assertEquals(0, list.getChildren().size);
    assertEquals(-1, display.getSelectedIndex());
    assertEquals(StoryLibraryDisplay.EMPTY_TITLE, display.getDisplayedTitle());
    assertEquals(StoryLibraryDisplay.EMPTY_BODY, display.getDisplayedBody());
    assertFalse(display.handleKey(Input.Keys.DOWN));
    display.dispose();
  }

  @Test
  void defaultConstructorReadsTheConfiguredEntries() {
    List<StoryEntry> configured = StoryEntryLoader.loadDefault();
    StoryLibraryDisplay display = createDisplay(mock(GdxGame.class), null);
    display.create();

    assertEquals(configured.get(0).title(), display.getDisplayedTitle());
    display.dispose();
  }

  private static StoryLibraryDisplay createDisplay(GdxGame game, List<StoryEntry> entries) {
    RenderService renderService = mock(RenderService.class);
    Stage stage = mock(Stage.class);
    when(renderService.getStage()).thenReturn(stage);
    ServiceLocator.registerRenderService(renderService);

    ResourceService resources = mock(ResourceService.class);
    Texture texture = mock(Texture.class);
    when(resources.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);
    ServiceLocator.registerResourceService(resources);

    return entries == null ? new StoryLibraryDisplay(game) : new StoryLibraryDisplay(game, entries);
  }

  private static Group findRoot() {
    ArgumentCaptor<Actor> root = ArgumentCaptor.forClass(Actor.class);
    verify(ServiceLocator.getRenderService().getStage()).addActor(root.capture());
    return (Group) root.getValue();
  }
}
