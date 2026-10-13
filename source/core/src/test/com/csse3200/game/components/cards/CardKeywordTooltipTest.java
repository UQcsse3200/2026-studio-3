package com.csse3200.game.components.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.csse3200.game.extensions.GameExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CardKeywordTooltipTest {
  private Skin skin;
  private Stage stage;
  private CardKeywordTooltip tooltip;
  private Actor card;

  @BeforeEach
  void setUp() {
    skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    stage = new Stage(new FitViewport(1200f, 800f), mock(Batch.class));
    tooltip = new CardKeywordTooltip(skin);
    card = new Actor();
    card.setBounds(100f, 100f, 225f, 456f);
    stage.addActor(card);
    tooltip.attachTo(card, "Gain 5 Block and 2 Strength. Gain more block.");
  }

  @AfterEach
  void tearDown() {
    tooltip.hide();
    stage.dispose();
    skin.dispose();
  }

  @Test
  void shouldKeepThePanelWhenThePointerMovesWithinTheCard() {
    Group cardGroup = new Group();
    cardGroup.setBounds(100f, 100f, 225f, 456f);
    Actor child = new Actor();
    cardGroup.addActor(child);
    stage.addActor(cardGroup);
    tooltip.attachTo(cardGroup, "Gain 5 Block.");
    mouseEvent(cardGroup, InputEvent.Type.enter);

    InputEvent exit = new InputEvent();
    exit.setType(InputEvent.Type.exit);
    exit.setPointer(-1);
    exit.setRelatedActor(child);
    cardGroup.fire(exit);

    assertTrue(tooltip.isVisible());
  }

  @Test
  void shouldIgnoreOtherCardHoversWhileTheLeftMouseButtonIsHeld() {
    Input originalInput = Gdx.input;
    Gdx.input = mock(Input.class);
    when(Gdx.input.isButtonPressed(Input.Buttons.LEFT)).thenReturn(true);
    try {
      mouseEvent(card, InputEvent.Type.enter);
      assertFalse(tooltip.isVisible());
    } finally {
      Gdx.input = originalInput;
    }
  }

  @Test
  void shouldShowOneExplanationPerKeywordAtTheCardsTopRight() {
    mouseEvent(card, InputEvent.Type.enter);

    assertSame(stage, tooltip.getStage());
    assertEquals(4, tooltip.getChildren().size);
    assertEquals("Block", ((Label) tooltip.getChildren().get(0)).getText().toString());
    assertEquals("Strength", ((Label) tooltip.getChildren().get(2)).getText().toString());
    assertTrue(tooltip.getX() > card.getRight());
    assertEquals(card.getTop(), tooltip.getTop(), 0.01f);
    assertNull(tooltip.hit(10f, 10f, true));
  }

  @Test
  void shouldHideWhenTheMouseLeavesTheCard() {
    mouseEvent(card, InputEvent.Type.enter);
    mouseEvent(card, InputEvent.Type.exit);

    assertFalse(tooltip.isVisible());
    assertNull(tooltip.getStage());
  }

  @Test
  void shouldKeepTheNewCardsTooltipWhenThePreviousCardExitsLate() {
    Actor nextCard = new Actor();
    nextCard.setBounds(400f, 100f, 225f, 456f);
    stage.addActor(nextCard);
    tooltip.attachTo(nextCard, "Apply 3 Poison for 3 turns.");

    mouseEvent(card, InputEvent.Type.enter);
    mouseEvent(nextCard, InputEvent.Type.enter);
    mouseEvent(card, InputEvent.Type.exit);

    assertTrue(tooltip.isVisible());
    assertEquals("Poison", ((Label) tooltip.getChildren().get(0)).getText().toString());
  }

  @Test
  void shouldFollowTheCardsMovementAndStayBesideItsRotatedBounds() {
    card.setOrigin(card.getWidth() / 2f, card.getHeight() / 2f);
    card.setRotation(15f);
    mouseEvent(card, InputEvent.Type.enter);
    float panelX = tooltip.getX();
    float panelY = tooltip.getY();

    card.moveBy(40f, 30f);
    stage.act(0f);

    assertEquals(panelX + 40f, tooltip.getX(), 0.01f);
    assertEquals(panelY + 30f, tooltip.getY(), 0.01f);
    assertTrue(tooltip.getX() > card.getRight());
  }

  @Test
  void shouldUseTheLeftSideAndStayOnScreenNearTheRightEdge() {
    card.setPosition(950f, 700f);
    mouseEvent(card, InputEvent.Type.enter);

    assertTrue(tooltip.getRight() < card.getX());
    assertTrue(tooltip.getX() >= 0f);
    assertTrue(tooltip.getY() >= 0f);
    assertTrue(tooltip.getRight() <= stage.getWidth());
    assertTrue(tooltip.getTop() <= stage.getHeight());
  }

  @Test
  void shouldHideOnPressWithoutConsumingOtherCardInput() {
    AtomicBoolean receivedPress = new AtomicBoolean();
    card.addListener(
        new InputListener() {
          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            receivedPress.set(true);
            return false;
          }
        });
    mouseEvent(card, InputEvent.Type.enter);
    InputEvent press = new InputEvent();
    press.setType(InputEvent.Type.touchDown);
    press.setPointer(0);
    card.fire(press);

    assertTrue(receivedPress.get());
    assertFalse(press.isHandled());
    assertFalse(tooltip.isVisible());
  }

  @Test
  void shouldHideWhenTheCardIsRemovedHiddenOrDisabled() {
    mouseEvent(card, InputEvent.Type.enter);
    card.remove();
    stage.act(0f);
    assertNull(tooltip.getStage());

    stage.addActor(card);
    mouseEvent(card, InputEvent.Type.enter);
    card.setVisible(false);
    stage.act(0f);
    assertNull(tooltip.getStage());

    card.setVisible(true);
    mouseEvent(card, InputEvent.Type.enter);
    card.setTouchable(Touchable.disabled);
    stage.act(0f);
    assertNull(tooltip.getStage());
  }

  @Test
  void shouldNotShowAnEmptyPanelForCardsWithoutKeywords() {
    Actor strike = new Actor();
    stage.addActor(strike);
    tooltip.attachTo(strike, "Deal 6 damage.");
    mouseEvent(strike, InputEvent.Type.enter);

    assertFalse(tooltip.isVisible());
    assertNull(tooltip.getStage());
  }

  private static void mouseEvent(Actor target, InputEvent.Type type) {
    InputEvent event = new InputEvent();
    event.setType(type);
    event.setPointer(-1);
    target.fire(event);
  }
}
