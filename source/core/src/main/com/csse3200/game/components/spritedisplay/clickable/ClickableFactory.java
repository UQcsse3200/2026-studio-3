// ClickableFactory.java
package com.csse3200.game.components.spritedisplay.clickable;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.ui.UIComponent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Builds and manages the live {@link Clickable} widgets for a set of {@link ClickableRecord}s:
 * resolves each record's variant, constructs and draws the widget, and disposes it. JSON parsing
 * lives separately in {@link ClickableJsonLoader} — this class only cares about the runtime widget
 * lifecycle, not where the records came from.
 */
public class ClickableFactory extends UIComponent {

  private static final String DEFAULT_VARIANT = "Clickable";
  private static final String HAND_TRIGGER = "playCard";
  private static final Map<String, ClickableSupplier> STATIC_VARIANTS = new HashMap<>();

  static {
    registerVariant(DEFAULT_VARIANT, rec -> new Clickable(rec) {});
    registerVariant("inout", InOutOnTrigger::new);
    registerVariant("drag", DragNDrop::new);
    registerVariant("gated", GatedClickable::new);
  }

  public static void registerVariant(String name, ClickableSupplier supplier) {
    STATIC_VARIANTS.put(name, supplier);
  }

  /**
   * @see ClickableJsonLoader#loadRecordsFromJson(Path)
   */
  public static List<ClickableRecord> loadRecordsFromJson(Path file) {
    return ClickableJsonLoader.loadRecordsFromJson(file);
  }

  private final List<ClickableRecord> records = new ArrayList<>();
  private final List<Clickable> clickables = new ArrayList<>();
  private final Map<String, ClickableSupplier> instanceVariants = new HashMap<>();

  // Tracks whether the hand row is currently supposed to be up (visible/interactable) or down
  // (hidden mid-enemy-turn), by listening to the same "up"/"down" events InOutOnTrigger widgets
  // do. A freshly built/rebuilt hand widget (e.g. from a cooldown retrieval firing mid the enemy's
  // "thinking" pause, before "up" has fired yet) must match this instead of always snapping
  // visible, or the row pops into view early and the later real "up" animation looks broken.
  private boolean handVisible = true;

  public ClickableFactory(Path file) {
    this(loadRecordsFromJson(file));
  }

  public ClickableFactory(List<ClickableRecord> records) {
    this.records.addAll(records);
  }

  /**
   * Registers a variant for THIS factory instance only, overriding a static variant of the same
   * name if present. Optional — most variants should go in the static block above instead.
   */
  public void registerInstanceVariant(String name, ClickableSupplier supplier) {
    instanceVariants.put(name, supplier);
  }

  private ClickableSupplier resolveVariant(String name) {
    ClickableSupplier supplier = instanceVariants.get(name);
    if (supplier != null) {
      return supplier;
    }
    return STATIC_VARIANTS.get(name);
  }

  @Override
  public void create() {
    super.create();
    entity.getEvents().addListener("up", () -> handVisible = true);
    entity.getEvents().addListener("down", () -> handVisible = false);
    for (ClickableRecord rec : records) {
      clickables.add(buildClickable(rec));
    }
  }

  private Clickable buildClickable(ClickableRecord rec) {
    ClickableSupplier supplier = resolveVariant(rec.variant());
    if (supplier == null) {
      Gdx.app.error(
          "ClickableFactory",
          "Unknown clickable variant \"" + rec.variant() + "\", falling back to default");
      supplier = STATIC_VARIANTS.get(DEFAULT_VARIANT);
    }

    Clickable clickable = supplier.create(rec);
    clickable.setEntity(this.entity);
    clickable.create();
    stage.addActor(clickable.getBtn());
    clickable.onAddedToStage(stage);

    // Hand cards snap straight to whichever state ("up"/visible or "down"/hidden) the rest of the
    // hand is currently in, rather than always popping up — matters when a rebuild happens while
    // the row is meant to be down (e.g. a cooldown retrieval mid the enemy's "thinking" pause).
    if (HAND_TRIGGER.equals(rec.trigger())) {
      if (handVisible) {
        clickable.showNow();
      } else {
        clickable.hideNow();
      }
    }
    return clickable;
  }

  /**
   * Rebuilds the on-screen hand: drops every {@code "playCard"} widget and builds fresh ones from
   * {@code handRecords}, leaving the static UI widgets untouched. Called when the hand changes (a
   * card played and a replacement drawn), so the new hand — including the drawn card — shows
   * immediately.
   *
   * @param handRecords one record per card currently in the player's hand
   */
  public void rebuildHand(List<ClickableRecord> handRecords) {
    rebuildByTrigger(HAND_TRIGGER, handRecords);
  }

  /**
   * Rebuilds every widget with the given trigger name: drops the old ones and builds fresh ones
   * from {@code records}, leaving every other widget this factory owns untouched. Generalization of
   * {@link #rebuildHand} for other dynamically-rebuilt widget groups (e.g. a popup's per-card toggle
   * buttons).
   *
   * @param trigger trigger name identifying which widgets to replace
   * @param records replacement records, built in order
   */
  public void rebuildByTrigger(String trigger, List<ClickableRecord> records) {
    Iterator<Clickable> iterator = clickables.iterator();
    while (iterator.hasNext()) {
      Clickable clickable = iterator.next();
      if (trigger.equals(clickable.getTrigger())) {
        clickable.remove();
        iterator.remove();
      }
    }
    for (ClickableRecord rec : records) {
      clickables.add(buildClickable(rec));
    }
  }

  /**
   * @param trigger trigger name to filter by
   * @return the currently built widgets for that trigger, in build order — lets callers
   *     post-process widgets they just (re)built (e.g. tinting a subset to reflect selection state)
   *     without this factory needing to know about that concern itself
   */
  public List<Clickable> getByTrigger(String trigger) {
    List<Clickable> result = new ArrayList<>();
    for (Clickable clickable : clickables) {
      if (trigger.equals(clickable.getTrigger())) {
        result.add(clickable);
      }
    }
    return List.copyOf(result);
  }

  /**
   * Re-asserts front-to-back stacking order for every widget this factory currently owns. A libGDX
   * {@link com.badlogic.gdx.scenes.scene2d.ui.Window} unconditionally calls {@code toFront()} on
   * itself on every touch down inside it (baked into its constructor, unrelated to {@code
   * setMovable}) — this undoes that for widgets that need to render on top of one, such as a popup's
   * own per-card buttons, which sit outside the window's actor hierarchy as stage siblings rather
   * than children.
   */
  public void bringToFront() {
    for (Clickable clickable : clickables) {
      clickable.getBtn().toFront();
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    for (Clickable clickable : clickables) {
      clickable.draw();
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    for (Clickable clickable : clickables) {
      clickable.remove();
    }
    clickables.clear();
  }
}
