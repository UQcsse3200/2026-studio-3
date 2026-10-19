package com.csse3200.game.components.cards;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.csse3200.game.cards.runtime.ResolvedCard;
import com.csse3200.game.input.InputComponent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Explicit F-key card inspection through the existing InputService and Stage.
 *
 * <p>Sources retain their own play/drag input and assets. Inspection is modal and pinned until F or
 * the close control is used. Escape keeps the screen's existing pause/popup behaviour.
 */
public final class CardInspectionController extends InputComponent {
  private final Stage stage;
  private final Skin skin;
  private final CardWidgetAssets assets;
  private final BooleanSupplier blocked;
  private final BooleanSupplier dragging;
  private final Map<Actor, Supplier<ResolvedCard>> sources = new LinkedHashMap<>();
  private CardInspectionPanel panel;
  private Actor source;
  private Actor previousKeyboardFocus;
  private Actor previousScrollFocus;
  private boolean fHeld;
  private boolean pointerPreferred = true;

  /**
   * @param stage screen's existing UI stage
   * @param skin externally owned UI skin
   * @param assets externally owned card visuals
   * @param blocked whether a popup, pause menu or text console owns input
   * @param dragging whether the screen is currently dragging/targeting a card
   */
  public CardInspectionController(
      Stage stage,
      Skin skin,
      CardWidgetAssets assets,
      BooleanSupplier blocked,
      BooleanSupplier dragging) {
    // Observe F and releases before pause/popup handlers; leave Escape to those handlers.
    super(120);
    this.stage = Objects.requireNonNull(stage);
    this.skin = Objects.requireNonNull(skin);
    this.assets = Objects.requireNonNull(assets);
    this.blocked = Objects.requireNonNull(blocked);
    this.dragging = Objects.requireNonNull(dragging);
  }

  /** Register an outer card actor and a resolver for its current exact owned copy. */
  public void registerSource(Actor actor, Supplier<ResolvedCard> card) {
    sources.put(Objects.requireNonNull(actor), Objects.requireNonNull(card));
  }

  /** Called before the screen rebuilds its hand, releasing obsolete source actors. */
  public void clearSources() {
    close();
    sources.clear();
  }

  /** Whether a card is currently inspected. */
  public boolean isOpen() {
    return source != null;
  }

  @Override
  public boolean keyDown(int keycode) {
    if (keycode == Keys.ESCAPE || keycode == Keys.F1) {
      close();
      return false;
    }
    if (blocked.getAsBoolean() || textInputFocused()) {
      close();
      return false;
    }
    if (keycode == Keys.F) {
      return toggleInspection();
    }
    if (isOpen()) {
      if (keycode == Keys.ENTER || keycode == Keys.SPACE) close();
      else stage.keyDown(keycode);
      return true;
    }
    pointerPreferred = false;
    return false;
  }

  private boolean toggleInspection() {
    if (fHeld) return true;
    fHeld = true;
    if (isOpen()) {
      close();
      return true;
    }
    if (dragging.getAsBoolean()) return false;
    Actor target = findTarget();
    if (target == null) return false;
    ResolvedCard card = sources.get(target).get();
    if (card == null) return false;
    open(target, card);
    return true;
  }

  @Override
  public boolean keyUp(int keycode) {
    if (keycode == Keys.F) {
      boolean handled = fHeld;
      fHeld = false;
      return handled && !blocked.getAsBoolean() && !textInputFocused();
    }
    if (!isOpen()) return false;
    stage.keyUp(keycode);
    return true;
  }

  @Override
  public boolean keyTyped(char character) {
    if (blocked.getAsBoolean() || textInputFocused()) return false;
    return isOpen() || (fHeld && (character == 'f' || character == 'F'));
  }

  @Override
  public boolean mouseMoved(int screenX, int screenY) {
    pointerPreferred = true;
    if (!isOpen()) return false;
    stage.mouseMoved(screenX, screenY);
    return true;
  }

  @Override
  public boolean touchDown(int screenX, int screenY, int pointer, int button) {
    if (!isOpen()) return false;
    stage.touchDown(screenX, screenY, pointer, button);
    return true;
  }

  @Override
  public boolean touchUp(int screenX, int screenY, int pointer, int button) {
    if (!isOpen()) return false;
    stage.touchUp(screenX, screenY, pointer, button);
    return true;
  }

  @Override
  public boolean touchDragged(int screenX, int screenY, int pointer) {
    if (!isOpen()) return false;
    stage.touchDragged(screenX, screenY, pointer);
    return true;
  }

  @Override
  public boolean scrolled(float amountX, float amountY) {
    if (!isOpen()) return false;
    stage.scrolled(amountX, amountY);
    return true;
  }

  private Actor findTarget() {
    Actor focused = sourceAncestor(stage.getKeyboardFocus());
    Vector2 pointer =
        stage.screenToStageCoordinates(
            new Vector2((float) Gdx.input.getX(), (float) Gdx.input.getY()));
    Actor pointed = sourceAncestor(stage.hit(pointer.x, pointer.y, true));
    Actor target = pointerPreferred ? pointed : focused;
    if (target == null) target = pointerPreferred ? focused : pointed;
    return eligible(target) ? target : null;
  }

  private Actor sourceAncestor(Actor actor) {
    for (; actor != null; actor = actor.getParent()) {
      if (sources.containsKey(actor)) return actor;
    }
    return null;
  }

  private boolean eligible(Actor actor) {
    return actor != null
        && actor.getStage() == stage
        && actor.isVisible()
        && actor.ascendantsVisible()
        && actor.getTouchable() == Touchable.enabled;
  }

  private boolean textInputFocused() {
    for (Actor actor = stage.getKeyboardFocus(); actor != null; actor = actor.getParent()) {
      if (actor instanceof TextField) return true;
    }
    return false;
  }

  private void open(Actor actor, ResolvedCard card) {
    stage.cancelTouchFocus();
    previousKeyboardFocus = stage.getKeyboardFocus();
    previousScrollFocus = stage.getScrollFocus();
    source = actor;
    if (panel == null) panel = new CardInspectionPanel(skin, assets, card, this::close);
    else panel.setCard(card);
    stage.addActor(panel);
    panel.setSize(stage.getWidth(), stage.getHeight());
    panel.validate();
    stage.setKeyboardFocus(panel.closeControl());
    stage.setScrollFocus(panel.scrollControl());
  }

  /** Dismiss inspection and restore only focus that still belongs to this stage. */
  public void close() {
    if (!isOpen()) return;
    if (stage.getKeyboardFocus() != null && stage.getKeyboardFocus().isDescendantOf(panel)) {
      stage.setKeyboardFocus(attached(previousKeyboardFocus));
    }
    if (stage.getScrollFocus() != null && stage.getScrollFocus().isDescendantOf(panel)) {
      stage.setScrollFocus(attached(previousScrollFocus));
    }
    panel.remove();
    source = null;
    previousKeyboardFocus = null;
    previousScrollFocus = null;
  }

  private Actor attached(Actor actor) {
    return actor != null && actor.getStage() == stage ? actor : null;
  }

  @Override
  public void update() {
    if (fHeld && !Gdx.input.isKeyPressed(Keys.F)) fHeld = false;
    if (!isOpen()) return;
    if (blocked.getAsBoolean() || dragging.getAsBoolean() || !eligible(source)) {
      close();
      return;
    }
    ResolvedCard current = sources.get(source).get();
    if (current == null) close();
    else if (!samePresentation(current, panel.face().getCard())) panel.setCard(current);
  }

  private static boolean samePresentation(ResolvedCard a, ResolvedCard b) {
    // EffectConfig is mutable and uses identity equality; compare the fields this view displays.
    return a.instanceId().equals(b.instanceId())
        && a.cardId().equals(b.cardId())
        && a.name().equals(b.name())
        && a.description().equals(b.description())
        && a.cost() == b.cost()
        && a.type() == b.type()
        && a.rarity() == b.rarity()
        && a.target() == b.target()
        && a.texturePath().equals(b.texturePath())
        && a.upgraded() == b.upgraded();
  }

  /** Reset physical key state when the application or screen loses focus. */
  public void reset() {
    close();
    fHeld = false;
    pointerPreferred = true;
  }

  @Override
  public void dispose() {
    reset();
    sources.clear();
    panel = null;
    super.dispose();
  }
}
