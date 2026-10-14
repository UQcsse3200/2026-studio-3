package com.csse3200.game.components.spritedisplay;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Maps the {@code "variant"} name on a sprite-display record to the constructor that builds its
 * widget. Shared by {@link com.csse3200.game.components.spritedisplay.clickable.ClickableFactory}
 * and {@link com.csse3200.game.components.spritedisplay.displaying.DisplayingFactory}.
 *
 * <p>Each factory owns its own registry, seeded from a fixed set of defaults via {@link
 * #copyOf(VariantRegistry)}, so a screen registering a variant never leaks into another screen or
 * test.
 *
 * @param <R> record type (the parsed JSON entry)
 * @param <W> widget type built from a record
 */
public final class VariantRegistry<R, W> {

  private final String kind;
  private final Map<String, Function<R, W>> variants = new HashMap<>();

  /**
   * @param kind human-readable widget kind (e.g. "clickable"), used in error messages
   */
  public VariantRegistry(String kind) {
    this.kind = kind;
  }

  /** Returns a new registry holding the same variants as {@code source}. */
  public static <R, W> VariantRegistry<R, W> copyOf(VariantRegistry<R, W> source) {
    VariantRegistry<R, W> copy = new VariantRegistry<>(source.kind);
    copy.variants.putAll(source.variants);
    return copy;
  }

  /** Registers {@code name}, replacing any existing variant of the same name. */
  public VariantRegistry<R, W> register(String name, Function<R, W> constructor) {
    variants.put(name, constructor);
    return this;
  }

  /**
   * Builds the widget for {@code name}.
   *
   * @throws IllegalArgumentException if no variant of that name is registered, so a typo in a JSON
   *     file fails at screen load instead of quietly rendering a plain default widget
   */
  public W create(String name, R rec) {
    Function<R, W> constructor = variants.get(name);
    if (constructor == null) {
      throw new IllegalArgumentException(
          "Unknown "
              + kind
              + " variant \""
              + name
              + "\". Registered variants: "
              + new TreeSet<>(variants.keySet()));
    }
    return constructor.apply(rec);
  }
}
