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

    // Hand cards should be visible and playable straight away rather than waiting for an "up".
    if (HAND_TRIGGER.equals(rec.trigger())) {
      clickable.showNow();
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
    Iterator<Clickable> iterator = clickables.iterator();
    while (iterator.hasNext()) {
      Clickable clickable = iterator.next();
      if (HAND_TRIGGER.equals(clickable.getTrigger())) {
        clickable.remove();
        iterator.remove();
      }
    }
    for (ClickableRecord rec : handRecords) {
      clickables.add(buildClickable(rec));
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
