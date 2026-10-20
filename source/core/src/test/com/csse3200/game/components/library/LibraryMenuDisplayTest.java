package com.csse3200.game.components.library;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.csse3200.game.GdxGame;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.services.audio.AudioService;
import com.csse3200.game.services.audio.SoundId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

@ExtendWith(GameExtension.class)
class LibraryMenuDisplayTest {
  private static final List<GdxGame.ScreenType> TARGETS =
      List.of(
          GdxGame.ScreenType.CARD_LIBRARY,
          GdxGame.ScreenType.BESTIARY,
          GdxGame.ScreenType.ITEM_LIBRARY,
          GdxGame.ScreenType.STORY_LIBRARY,
          GdxGame.ScreenType.MAIN_MENU);

  private GdxGame game;
  private Stage stage;
  private LibraryMenuDisplay display;
  private Group root;
  private List<TextButton> buttons;

  @BeforeEach
  void setUp() {
    game = mock(GdxGame.class);
    stage = mock(Stage.class);
    RenderService renderer = mock(RenderService.class);
    when(renderer.getStage()).thenReturn(stage);
    ServiceLocator.registerRenderService(renderer);
    ResourceService resources = mock(ResourceService.class);
    when(resources.getAsset(anyString(), eq(Texture.class))).thenReturn(mock(Texture.class));
    ServiceLocator.registerResourceService(resources);
    display = new LibraryMenuDisplay(game);
    display.create();
    root = captureRoot(1);
    buttons = collectButtons(root);
  }

  @AfterEach
  void tearDown() {
    display.dispose();
  }

  @Test
  void enterOpensCards() {
    assertButtonRoute(0, Input.Keys.ENTER);
  }

  @Test
  void spaceOpensCards() {
    assertButtonRoute(0, Input.Keys.SPACE);
  }

  @Test
  void enterOpensEnemies() {
    assertButtonRoute(1, Input.Keys.ENTER);
  }

  @Test
  void spaceOpensEnemies() {
    assertButtonRoute(1, Input.Keys.SPACE);
  }

  @Test
  void enterOpensItems() {
    assertButtonRoute(2, Input.Keys.ENTER);
  }

  @Test
  void spaceOpensItems() {
    assertButtonRoute(2, Input.Keys.SPACE);
  }

  @Test
  void enterOpensStory() {
    assertButtonRoute(3, Input.Keys.ENTER);
  }

  @Test
  void spaceOpensStory() {
    assertButtonRoute(3, Input.Keys.SPACE);
  }

  @Test
  void enterGoesBack() {
    assertButtonRoute(4, Input.Keys.ENTER);
  }

  @Test
  void spaceGoesBack() {
    assertButtonRoute(4, Input.Keys.SPACE);
  }

  private void assertButtonRoute(int index, int key) {
    String context = buttons.get(index).getText() + " via " + Input.Keys.toString(key);
    assertEquals(
        List.of("Card Library", "Enemy Library", "Item Library", "Story", "Back"),
        buttons.stream().map(button -> button.getText().toString()).toList(),
        context);
    for (int i = 0; i <= index; i++) assertTrue(press(Input.Keys.DOWN), context);
    verifyNoInteractions(game);
    assertTrue(press(key), context);
    verify(game, description(context)).setScreen(TARGETS.get(index));
  }

  @Test
  void upFirstSelectsFirstButtonAndBoundariesDoNotWrap() {
    assertDirectionAndBoundaries(Input.Keys.UP);
  }

  @Test
  void wFirstSelectsFirstButtonAndBoundariesDoNotWrap() {
    assertDirectionAndBoundaries(Input.Keys.W);
  }

  @Test
  void downFirstSelectsFirstButtonAndBoundariesDoNotWrap() {
    assertDirectionAndBoundaries(Input.Keys.DOWN);
  }

  @Test
  void sFirstSelectsFirstButtonAndBoundariesDoNotWrap() {
    assertDirectionAndBoundaries(Input.Keys.S);
  }

  private void assertDirectionAndBoundaries(int firstKey) {
    assertEquals(-1, display.getSelectedIndex());
    assertTrue(press(firstKey));
    assertEquals(0, display.getSelectedIndex());
    press(Input.Keys.UP);
    press(Input.Keys.W);
    assertEquals(0, display.getSelectedIndex());
    for (int i = 0; i < 7; i++) press(Input.Keys.S);
    assertEquals(4, display.getSelectedIndex());
    press(Input.Keys.DOWN);
    assertEquals(4, display.getSelectedIndex());
    press(Input.Keys.W);
    assertEquals(3, display.getSelectedIndex());
    verifyNoInteractions(game);
  }

  @Test
  void enterWithoutSelectionDoesNothing() {
    assertFalse(press(Input.Keys.ENTER));
    verifyNoInteractions(game);
  }

  @Test
  void spaceWithoutSelectionDoesNothing() {
    assertFalse(press(Input.Keys.SPACE));
    verifyNoInteractions(game);
  }

  @Test
  void escapeUsesBackAction() {
    assertTrue(press(Input.Keys.ESCAPE));
    verify(game).setScreen(GdxGame.ScreenType.MAIN_MENU);
  }

  @Test
  void enterRemainsHandledWhenActivationDisposesTheDisplay() {
    assertActivationRemainsHandledAfterDisposal(Input.Keys.ENTER);
  }

  @Test
  void spaceRemainsHandledWhenActivationDisposesTheDisplay() {
    assertActivationRemainsHandledAfterDisposal(Input.Keys.SPACE);
  }

  private void assertActivationRemainsHandledAfterDisposal(int key) {
    display.handleKey(Input.Keys.DOWN);
    doAnswer(
            invocation -> {
              display.dispose();
              return null;
            })
        .when(game)
        .setScreen(GdxGame.ScreenType.CARD_LIBRARY);

    assertTrue(display.handleKey(key), Input.Keys.toString(key));
    assertEquals(-1, display.getSelectedIndex());
    verify(game).setScreen(GdxGame.ScreenType.CARD_LIBRARY);
  }

  @Test
  void escapeRemainsHandledWhenActivationDisposesTheDisplay() {
    doAnswer(
            invocation -> {
              display.dispose();
              return null;
            })
        .when(game)
        .setScreen(GdxGame.ScreenType.MAIN_MENU);

    assertTrue(display.handleKey(Input.Keys.ESCAPE));
    assertEquals(-1, display.getSelectedIndex());
    verify(game).setScreen(GdxGame.ScreenType.MAIN_MENU);
  }

  @Test
  void mouseEntryThenDownContinuesFromHoveredButtonWithoutChangingHoverSound() {
    try (MockedStatic<AudioService> audio = mockStatic(AudioService.class)) {
      InputEvent enter = new InputEvent();
      enter.setType(InputEvent.Type.enter);
      enter.setPointer(-1);
      buttons.get(2).fire(enter);
      assertEquals(2, display.getSelectedIndex());
      assertTrue(press(Input.Keys.DOWN));
      assertEquals(3, display.getSelectedIndex());
      audio.verify(() -> AudioService.playSound(SoundId.MENU_HOVER, 0.5f));
      verifyNoInteractions(game);
    }
  }

  @Test
  void unrelatedKeyIsNotHandled() {
    assertFalse(press(Input.Keys.F));
    assertEquals(-1, display.getSelectedIndex());
    verifyNoInteractions(game);
  }

  @Test
  void disposingTwiceDoesNotThrowAndClearsSelection() {
    display.handleKey(Input.Keys.DOWN);
    assertDoesNotThrow(
        () -> {
          display.dispose();
          display.dispose();
        });
    assertEquals(-1, display.getSelectedIndex());
  }

  @Test
  void disposalRemovesInputAndRecreationStartsWithoutSelection() {
    press(Input.Keys.DOWN);
    when(stage.getKeyboardFocus()).thenReturn(root);
    Group oldRoot = root;
    display.dispose();
    verify(stage).setKeyboardFocus(null);
    assertFalse(oldRoot.getListeners().size > 0);
    assertFalse(display.handleKey(Input.Keys.DOWN));
    display.create();
    root = captureRoot(2);
    assertEquals(-1, display.getSelectedIndex());
    verify(stage).setKeyboardFocus(root);
    assertFalse(press(Input.Keys.ENTER));
    verifyNoInteractions(game);
  }

  @Test
  void selectionUsesOwnHoverDrawableAndDeselectionRestoresOriginalStyleObject() {
    List<TextButton.TextButtonStyle> originals = buttons.stream().map(TextButton::getStyle).toList();
    for (int i = 0; i < buttons.size(); i++) {
      TextButton.TextButtonStyle original = originals.get(i);
      assertNotNull(original.over, buttons.get(i).getText().toString());
      assertSame(original, buttons.get(i).getStyle());
      press(Input.Keys.DOWN);
      assertSame(original.over, buttons.get(i).getStyle().up);
      assertSame(original.overFontColor, buttons.get(i).getStyle().fontColor);
      if (i > 0) assertSame(originals.get(i - 1), buttons.get(i - 1).getStyle());
    }
    press(Input.Keys.UP);
    assertSame(originals.get(4), buttons.get(4).getStyle());
    verifyNoInteractions(game);
  }

  @Test
  void missingHoverDrawableLeavesOriginalStyleUnchanged() {
    TextButton.TextButtonStyle original = buttons.get(0).getStyle();
    original.over = null;
    press(Input.Keys.DOWN);
    assertSame(original, buttons.get(0).getStyle());
  }

  private boolean press(int key) {
    InputEvent event = new InputEvent();
    event.setType(InputEvent.Type.keyDown);
    event.setKeyCode(key);
    root.fire(event);
    return event.isHandled();
  }

  private Group captureRoot(int count) {
    ArgumentCaptor<Actor> captured = ArgumentCaptor.forClass(Actor.class);
    verify(stage, times(count)).addActor(captured.capture());
    return (Group) captured.getValue();
  }

  private static List<TextButton> collectButtons(Group group) {
    List<TextButton> found = new ArrayList<>();
    for (Actor child : group.getChildren()) {
      if (child instanceof TextButton button) found.add(button);
      else if (child instanceof Group nested) found.addAll(collectButtons(nested));
    }
    return found;
  }
}
