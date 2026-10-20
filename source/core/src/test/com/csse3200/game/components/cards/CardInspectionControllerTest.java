package com.csse3200.game.components.cards;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.cards.runtime.ResolvedCard;
import com.csse3200.game.components.pausemenu.PauseMenuInput;
import com.csse3200.game.components.spritedisplay.clickable.ClickableRecord;
import com.csse3200.game.components.spritedisplay.clickable.InOutOnTrigger;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.terminal.KeyboardTerminalInputComponent;
import com.csse3200.game.ui.terminal.Terminal;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CardInspectionControllerTest {
  private Graphics previousGraphics;
  private Input previousInput;
  private Input pointer;
  private Stage stage;
  private Skin skin;
  private CardWidgetAssets assets;
  private InputService input;
  private Entity ui;
  private CardInspectionController inspection;
  private final AtomicBoolean blocked = new AtomicBoolean();
  private final AtomicBoolean dragging = new AtomicBoolean();
  private ResolvedCard base;
  private ResolvedCard upgraded;

  @BeforeEach
  void setUp() {
    previousGraphics = Gdx.graphics;
    previousInput = Gdx.input;
    Gdx.graphics = mock(Graphics.class);
    when(Gdx.graphics.getWidth()).thenReturn(1280);
    when(Gdx.graphics.getHeight()).thenReturn(800);
    pointer = mock(Input.class);
    Gdx.input = pointer;
    when(pointer.isKeyPressed(Keys.F)).thenReturn(true);
    stage = new Stage(new ScreenViewport(), mock(Batch.class));
    stage.getViewport().update(1280, 800, true);
    skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    assets = CardWidgetAssets.fromSkin(skin, path -> null);
    var strike =
        CardConfigLoader.loadCards().stream()
            .filter(card -> "strike".equals(card.id))
            .findFirst()
            .orElseThrow();
    CardResolver resolver = new CardResolver();
    base = resolver.resolve(strike, new CardInstance("base-copy", "strike", 0));
    upgraded = resolver.resolve(strike, new CardInstance("upgraded-copy", "strike", 1));
    input = new InputService();
    ServiceLocator.registerInputService(input);
    ServiceLocator.registerEntityService(new EntityService());
    inspection = new CardInspectionController(stage, skin, assets, blocked::get, dragging::get);
    ui = new Entity().addComponent(inspection).addComponent(new InputDecorator(stage, 10));
    ui.create();
  }

  @AfterEach
  void tearDown() {
    ui.dispose();
    stage.dispose();
    skin.dispose();
    Gdx.input = previousInput;
    Gdx.graphics = previousGraphics;
  }

  @Test
  void opensOnlyOnFAndStaysPinnedAfterPointerLeavesThenFTogglesClosed() {
    source(base, 100, 100);
    pointAt(150, 150);
    input.mouseMoved(150, 649);
    assertFalse(inspection.isOpen());
    assertTrue(input.keyDown(Keys.F));
    assertTrue(inspection.isOpen());
    assertEquals(base.instanceId(), panel().face().getCard().instanceId());
    input.keyUp(Keys.F);
    pointAt(1000, 700);
    input.mouseMoved(1000, 99);
    inspection.update();
    assertTrue(inspection.isOpen());
    assertTrue(input.keyDown(Keys.F));
    assertFalse(inspection.isOpen());
    assertNull(stage.getRoot().findActor("card-inspection-overlay"));
  }

  @Test
  void physicalPressTogglesOnceUntilReleaseIncludingAfterClosing() {
    source(base, 100, 100);
    pointAt(150, 150);
    input.keyDown(Keys.F);
    for (int repeat = 0; repeat < 5; repeat++) input.keyDown(Keys.F);
    assertTrue(inspection.isOpen());
    input.keyUp(Keys.F);
    input.keyDown(Keys.F);
    input.keyDown(Keys.F);
    assertFalse(inspection.isOpen());
    input.keyUp(Keys.F);
    input.keyDown(Keys.F);
    assertTrue(inspection.isOpen());
  }

  @Test
  void overlappingCardsUseTopmostExactCopyAndFreshFaceWithoutPlaying() {
    var strike =
        CardConfigLoader.loadCards().stream()
            .filter(card -> "strike".equals(card.id))
            .findFirst()
            .orElseThrow();
    CardLibrary library = new CardLibrary(List.of(strike));
    BattleDeck deck =
        new BattleDeck(
            PlayerDeck.fromInstances(
                library,
                List.of(
                    new CardInstance(base.instanceId(), "strike", 0),
                    new CardInstance(upgraded.instanceId(), "strike", 1))));
    deck.drawCards(2);
    List<CardInstance> before = deck.getHand();
    AtomicInteger plays = new AtomicInteger();
    InOutOnTrigger clickable =
        new InOutOnTrigger(
            ClickableRecord.builder("playCard")
                .skin(skin)
                .text("Strike+")
                .args(upgraded.instanceId())
                .size(CardWidget.CARD_WIDTH, CardWidget.CARD_HEIGHT)
                .build());
    clickable.setEntity(ui);
    clickable.setVisualContent(() -> new CardWidget(upgraded, assets));
    clickable.getBtn().setBounds(150, 100, 225, 456);
    ui.getEvents().addListener("playCard", (String id) -> plays.incrementAndGet());
    source(base, 100, 100);
    stage.addActor(clickable.getBtn());
    inspection.registerSource(clickable.getBtn(), () -> upgraded);
    CardWidget handFace = (CardWidget) clickable.getBtn().getChildren().first();
    pointAt(200, 200);
    input.keyDown(Keys.F);
    assertEquals(upgraded.instanceId(), panel().face().getCard().instanceId());
    assertTrue(panel().face().getCard().upgraded());
    assertNotSame(handFace, panel().face());
    assertSame(clickable.getBtn(), handFace.getParent());
    // Background clicks cannot reach the battle card or its play trigger.
    input.touchDown(200, 599, 0, Input.Buttons.LEFT);
    input.touchUp(200, 599, 0, Input.Buttons.LEFT);
    assertEquals(0, plays.get());
    assertEquals(before, deck.getHand());
    assertTrue(deck.getDiscardPileInstances().isEmpty());
  }

  @Test
  void deliberateKeyboardFocusWorksAndPointerMovementOverridesOldFocus() {
    Actor first = source(base, 100, 100);
    source(upgraded, 500, 100);
    stage.setKeyboardFocus(first);
    pointAt(550, 150);
    input.keyDown(Keys.RIGHT);
    input.keyDown(Keys.F);
    assertEquals(base.instanceId(), panel().face().getCard().instanceId());
    input.keyUp(Keys.F);
    input.keyDown(Keys.F);
    assertSame(first, stage.getKeyboardFocus());
    input.keyUp(Keys.F);
    input.mouseMoved(550, 649);
    input.keyDown(Keys.F);
    assertEquals(upgraded.instanceId(), panel().face().getCard().instanceId());
  }

  @Test
  void noTargetHiddenDisabledOrDetachedCardsAndDraggingCannotOpenInspection() {
    pointAt(150, 150);
    assertFalse(input.keyDown(Keys.F));
    input.keyUp(Keys.F);
    Actor actor = source(base, 100, 100);
    actor.setVisible(false);
    assertFalse(input.keyDown(Keys.F));
    input.keyUp(Keys.F);
    actor.setVisible(true);
    actor.setTouchable(Touchable.disabled);
    assertFalse(input.keyDown(Keys.F));
    input.keyUp(Keys.F);
    actor.setTouchable(Touchable.enabled);
    dragging.set(true);
    assertFalse(input.keyDown(Keys.F));
    input.keyUp(Keys.F);
    dragging.set(false);
    actor.remove();
    assertFalse(input.keyDown(Keys.F));
    assertFalse(inspection.isOpen());
  }

  @Test
  void textFieldReceivesBothFKeyAndTypedCharacter() {
    source(base, 100, 100);
    pointAt(150, 150);
    TextField field = new TextField("", skin);
    stage.addActor(field);
    stage.setKeyboardFocus(field);
    input.keyDown(Keys.F);
    input.keyTyped('f');
    input.keyUp(Keys.F);
    assertEquals("f", field.getText());
    assertFalse(inspection.isOpen());
  }

  @Test
  void terminalReceivesFAndOpensWithoutCompetingWithInspection() {
    Terminal terminal = new Terminal();
    Entity console =
        new Entity().addComponent(terminal).addComponent(new KeyboardTerminalInputComponent());
    console.create();
    try {
      ui.dispose();
      inspection =
          new CardInspectionController(stage, skin, assets, terminal::isOpen, dragging::get);
      ui = new Entity().addComponent(inspection).addComponent(new InputDecorator(stage, 10));
      ui.create();
      source(base, 100, 100);
      pointAt(150, 150);
      input.keyDown(Keys.F);
      input.keyUp(Keys.F);
      assertTrue(inspection.isOpen());
      input.keyDown(Keys.F1);
      assertFalse(inspection.isOpen());
      assertTrue(terminal.isOpen());
      input.keyDown(Keys.F);
      input.keyTyped('F');
      input.keyUp(Keys.F);
      assertEquals("F", terminal.getEnteredMessage());
      assertFalse(inspection.isOpen());
    } finally {
      console.dispose();
    }
  }

  @Test
  void escapeRetainsExistingPauseHandlerInsteadOfAddingInspectionDismissalPriority() {
    Entity pause = new Entity().addComponent(new PauseMenuInput());
    AtomicInteger pauses = new AtomicInteger();
    pause.getEvents().addListener("pause", pauses::incrementAndGet);
    pause.create();
    try {
      source(base, 100, 100);
      pointAt(150, 150);
      input.keyDown(Keys.F);
      assertTrue(input.keyDown(Keys.ESCAPE));
      assertEquals(1, pauses.get());
      assertFalse(inspection.isOpen());
    } finally {
      pause.dispose();
    }
  }

  @Test
  void blockingOverlayClosesAndSuppressesInspection() {
    source(base, 100, 100);
    pointAt(150, 150);
    input.keyDown(Keys.F);
    blocked.set(true);
    inspection.update();
    assertFalse(inspection.isOpen());
    input.keyUp(Keys.F);
    assertFalse(input.keyDown(Keys.F));
    assertFalse(inspection.isOpen());
  }

  @Test
  void removalRebuildAndFocusLossClearPinnedSourceAndHeldKeyState() {
    Actor actor = source(base, 100, 100);
    pointAt(150, 150);
    input.keyDown(Keys.F);
    actor.remove();
    inspection.update();
    assertFalse(inspection.isOpen());
    source(base, 100, 100);
    inspection.reset();
    input.keyDown(Keys.F);
    assertTrue(inspection.isOpen());
    inspection.clearSources();
    assertFalse(inspection.isOpen());
    input.keyUp(Keys.F);
    assertFalse(input.keyDown(Keys.F));
  }

  @Test
  void currentSnapshotRefreshesWithoutRebuildingAndNullResolutionCloses() {
    AtomicReference<ResolvedCard> card = new AtomicReference<>(base);
    Actor actor = source(base, 100, 100);
    inspection.registerSource(actor, card::get);
    pointAt(150, 150);
    input.keyDown(Keys.F);
    CardInspectionPanel original = panel();
    CardWidget face = original.face();
    card.set(upgraded);
    inspection.update();
    assertSame(original, panel());
    assertSame(face, panel().face());
    assertEquals(upgraded.name(), face.displayedName());
    card.set(null);
    inspection.update();
    assertFalse(inspection.isOpen());
  }

  @Test
  void longRulesRemainScrollableAndUnchangedResolutionsKeepScrollPosition() {
    String longRules = "Complete card rules stay readable and scrollable. ".repeat(80);
    Actor actor = source(base, 100, 100);
    inspection.registerSource(
        actor,
        () ->
            new ResolvedCard(
                base.instanceId(),
                base.cardId(),
                base.name(),
                longRules,
                base.cost(),
                base.type(),
                base.rarity(),
                base.target(),
                base.effects(),
                base.texturePath(),
                base.upgraded()));
    pointAt(150, 150);
    input.keyDown(Keys.F);
    ScrollPane scroll = (ScrollPane) panel().scrollControl();
    scroll.validate();
    assertTrue(scroll.getMaxY() > 100f);
    scroll.setScrollY(100f);
    panel().invalidate();
    panel().validate();
    inspection.update();
    assertEquals(100f, scroll.getScrollY());
    assertSame(scroll, stage.getScrollFocus());
    assertSame(panel().closeControl(), stage.getKeyboardFocus());
    assertEquals(longRules, panel().face().getCard().description());
  }

  @Test
  void closeControlDismissesAndDisposalUnregistersInput() {
    source(base, 100, 100);
    pointAt(150, 150);
    input.keyDown(Keys.F);
    Actor close = panel().closeControl();
    Vector2 screen =
        stage.stageToScreenCoordinates(
            close.localToStageCoordinates(
                new Vector2(close.getWidth() / 2f, close.getHeight() / 2f)));
    input.touchDown((int) screen.x, (int) screen.y, 0, Input.Buttons.LEFT);
    input.touchUp((int) screen.x, (int) screen.y, 0, Input.Buttons.LEFT);
    assertFalse(inspection.isOpen());
    ui.dispose();
    assertFalse(input.keyDown(Keys.F));
    // Recreate a harmless entity for the shared cleanup.
    ui = new Entity();
    ui.create();
  }

  @Test
  void resizeClampsWholeFaceAndProvidesScrollableCompleteRules() {
    Actor actor = source(base, 100, 100);
    pointAt(150, 150);
    input.keyDown(Keys.F);
    for (int[] size : new int[][] {{1280, 720}, {1920, 1080}, {480, 640}}) {
      when(Gdx.graphics.getWidth()).thenReturn(size[0]);
      when(Gdx.graphics.getHeight()).thenReturn(size[1]);
      stage.getViewport().update(size[0], size[1], true);
      panel().validate();
      CardWidget face = panel().face();
      Vector2 lower = face.localToStageCoordinates(new Vector2());
      Vector2 upper = face.localToStageCoordinates(new Vector2(face.getWidth(), face.getHeight()));
      assertTrue(lower.x >= 0 && lower.y >= 0);
      assertTrue(upper.x <= size[0] && upper.y <= size[1]);
      assertEquals(base.description(), face.displayedDescription());
      assertSame(panel().closeControl(), stage.getKeyboardFocus());
      assertSame(panel().scrollControl(), stage.getScrollFocus());
    }
    assertSame(stage, actor.getStage());
  }

  private Actor source(ResolvedCard card, float x, float y) {
    Actor actor = new Actor();
    actor.setBounds(x, y, 225, 456);
    stage.addActor(actor);
    inspection.registerSource(actor, () -> card);
    return actor;
  }

  private void pointAt(int stageX, int stageY) {
    int screenY = Gdx.graphics.getHeight() - 1 - stageY;
    when(pointer.getX()).thenReturn(stageX);
    when(pointer.getY()).thenReturn(screenY);
  }

  private CardInspectionPanel panel() {
    return stage.getRoot().findActor("card-inspection-overlay");
  }
}
