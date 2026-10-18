// ClickableFactory.java
package com.csse3200.game.components.spritedisplay.clickable;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.components.spritedisplay.VariantRegistry;
import com.csse3200.game.ui.UIComponent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Builds and manages the live {@link Clickable} widgets for a set of {@link ClickableRecord}s:
 * resolves each record's variant, constructs and draws the widget, and disposes it. JSON parsing
 * lives separately in {@link ClickableJsonLoader} — this class only cares about the runtime widget
 * lifecycle, not where the records came from.
 *
 * <p>Uses the GoF Factory Method pattern: every widget is made by {@link
 * #createClickable(ClickableRecord)}, which a screen-specific subclass overrides to build variants
 * that need that screen's dependencies (see {@code BattleClickableFactory}).
 */
public class ClickableFactory extends UIComponent {

  private static final String DEFAULT_VARIANT = "Clickable";

  // Variants that need nothing but their record. Read-only after class initialisation.
  private static final VariantRegistry<ClickableRecord, Clickable> DEFAULT_VARIANTS =
      new VariantRegistry<ClickableRecord, Clickable>("clickable")
          .register(DEFAULT_VARIANT, rec -> new Clickable(rec) {})
          .register("inout", InOutOnTrigger::new)
          .register("drag", DragNDrop::new)
          .register("gated", GatedClickable::new)
          .register("framed", Framed::new);

  /**
   * @see ClickableJsonLoader#loadRecordsFromJson(Path)
   */
  public static List<ClickableRecord> loadRecordsFromJson(Path file) {
    return ClickableJsonLoader.loadRecordsFromJson(file);
  }

  private final List<ClickableRecord> records = new ArrayList<>();
  private final List<Clickable> clickables = new ArrayList<>();

  public ClickableFactory(Path file) {
    this(loadRecordsFromJson(file));
  }

  public ClickableFactory(List<ClickableRecord> records) {
    this.records.addAll(records);
  }

  /**
   * Factory method: creates the widget for one record. The default looks the record's variant up in
   * {@link #DEFAULT_VARIANTS}. Subclasses override this to handle variants that need extra
   * dependencies, and call {@code super.createClickable(rec)} for everything else.
   *
   * @throws IllegalArgumentException if the variant is not known
   */
  protected Clickable createClickable(ClickableRecord rec) {
    return DEFAULT_VARIANTS.create(rec.variant(), rec);
  }

  @Override
  public void create() {
    super.create();
    for (ClickableRecord rec : records) {
      clickables.add(buildClickable(rec));
    }
  }

  private Clickable buildClickable(ClickableRecord rec) {
    Clickable clickable = createClickable(rec);
    clickable.setEntity(this.entity);
    clickable.create();
    stage.addActor(clickable.getBtn());
    clickable.onAddedToStage(stage);
    return clickable;
  }

  /**
   * Rebuilds every widget with the given trigger name: drops the old ones and builds fresh ones
   * from {@code records}, leaving every other widget this factory owns untouched. Used for
   * dynamically rebuilt widget groups such as the battle hand or a popup's per-card toggle buttons.
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
   * setMovable}) — this undoes that for widgets that need to render on top of one, such as a
   * popup's own per-card buttons, which sit outside the window's actor hierarchy as stage siblings
   * rather than children.
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
