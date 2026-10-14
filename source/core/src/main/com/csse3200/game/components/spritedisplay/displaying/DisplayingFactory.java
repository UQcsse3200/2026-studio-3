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
 */
public class DisplayingFactory extends UIComponent {

  private static final String DEFAULT_VARIANT = "Displaying";

  // Read-only defaults; every factory gets its own copy so per-screen registrations stay local.
  // Variants that need a popup ("popupText", "cardPreview", "popupPanel") are deliberately absent:
  // the screen that owns the popup registers them with registerInstanceVariant.
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
  private final VariantRegistry<DisplayingRecord, Displaying> variants =
      VariantRegistry.copyOf(DEFAULT_VARIANTS);

  public DisplayingFactory(Path file) {
    this(loadRecordsFromJson(file));
  }

  public DisplayingFactory(List<DisplayingRecord> records) {
    this.records.addAll(records);
  }

  /**
   * Registers a variant for THIS factory instance only, overriding a default variant of the same
   * name if present. Use it for variants that need screen-specific dependencies (e.g. a popup).
   */
  public void registerInstanceVariant(String name, DisplayingSupplier supplier) {
    variants.register(name, supplier::create);
  }

  @Override
  public void create() {
    super.create();

    for (DisplayingRecord rec : records) {
      Displaying displaying = variants.create(rec.variant(), rec);
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
    List<T> matches = new ArrayList<>();
    for (Displaying displaying : displayings) {
      if (type.isInstance(displaying)) {
        matches.add(type.cast(displaying));
      }
    }
    return matches;
  }
}
