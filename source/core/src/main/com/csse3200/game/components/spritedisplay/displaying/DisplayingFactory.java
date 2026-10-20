package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.csse3200.game.components.spritedisplay.VariantRegistry;
import com.csse3200.game.ui.UIComponent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds and manages the live {@link Displaying} widgets for a set of {@link DisplayingRecord}s.
 * JSON parsing lives separately in {@link DisplayingJsonLoader}.
 *
 * <p>Uses the GoF Factory Method pattern: every widget is made by {@link
 * #createDisplaying(DisplayingRecord)}, which a screen-specific subclass overrides to build
 * variants that need that screen's dependencies (see {@code DeckEditorDisplayingFactory}).
 */
public class DisplayingFactory extends UIComponent {

  private static final String DEFAULT_VARIANT = "Displaying";

  // Variants that need nothing but their record. Read-only after class initialisation. Variants
  // that need a popup ("popupText", "cardPreview") are deliberately absent: the subclass for the
  // screen that owns the popup creates them.
  private static final VariantRegistry<DisplayingRecord, Displaying> DEFAULT_VARIANTS =
      new VariantRegistry<DisplayingRecord, Displaying>("displaying")
          .register(DEFAULT_VARIANT, rec -> new Displaying(rec) {})
          .register("battleLog", BattleLogDisplay::new)
          .register("playerTurn", PlayerTurnDisplay::new)
          .register("enemyTurn", EnemyTurnDisplay::new)
          .register("endBattle", EndBattleDisplay::new)
          .register("cardBadges", CardBadgesDisplay::new);

  /**
   * Parses a JSON file into records without creating a factory. Use this when you want to filter /
   * re-inject the records before building the factory.
   *
   * @see DisplayingJsonLoader#loadRecordsFromJson(Path)
   */
  public static List<DisplayingRecord> loadRecordsFromJson(Path file) {
    return DisplayingJsonLoader.loadRecordsFromJson(file);
  }

  private final List<DisplayingRecord> records = new ArrayList<>();
  private final List<Displaying> displayings = new ArrayList<>();

  public DisplayingFactory(Path file) {
    this(loadRecordsFromJson(file));
  }

  public DisplayingFactory(List<DisplayingRecord> records) {
    this.records.addAll(records);
  }

  /**
   * Factory method: creates the widget for one record. The default looks the record's variant up in
   * {@link #DEFAULT_VARIANTS}. Subclasses override this to handle variants that need extra
   * dependencies, and call {@code super.createDisplaying(rec)} for everything else.
   *
   * @throws IllegalArgumentException if the variant is not known
   */
  protected Displaying createDisplaying(DisplayingRecord rec) {
    return DEFAULT_VARIANTS.create(rec.variant(), rec);
  }

  @Override
  public void create() {
    super.create();

    for (DisplayingRecord rec : records) {
      Displaying displaying = createDisplaying(rec);
      displaying.setEntity(this.entity);
      displaying.create();
      displayings.add(displaying);
    }
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Each Displaying registers itself with the render service in create() and draws itself.
  }

  @Override
  public void dispose() {
    super.dispose();
    for (Displaying displaying : displayings) {
      displaying.dispose();
    }
    displayings.clear();
  }

  /**
   * Returns every displaying of the given type created by this factory. Used by callers (e.g.
   * {@link com.csse3200.game.screens.EndBattleScreen}) that need to interact with a specific
   * displaying variant after construction — a JSON config may define more than one instance of the
   * same variant (e.g. the victory heading and the "click anywhere" hint both use "endBattle").
   *
   * @param type the Displaying subclass to look for
   * @return every matching instance, in the order they were created; empty if none exist
   */
  public <T extends Displaying> List<T> getDisplayings(Class<T> type) {
    return displayings.stream().filter(type::isInstance).map(type::cast).toList();
  }
}
