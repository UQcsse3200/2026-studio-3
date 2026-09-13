package com.csse3200.game.cards.runtime;

import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.EffectConfig;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable, runtime-ready card values shared by presentation and gameplay consumers.
 *
 * <p>Effect data is defensively copied both when this record is created and when it is read. This
 * prevents consumers from mutating either this snapshot or the source {@code CardConfig}.
 *
 * @param instanceId stable ID of the exact owned copy
 * @param cardId shared card definition ID
 * @param name resolved display name
 * @param description resolved rules text
 * @param cost resolved energy cost
 * @param type inherited card category
 * @param rarity resolved rarity
 * @param target inherited target type
 * @param effects resolved effects in execution order
 * @param texturePath inherited artwork path
 * @param upgraded whether upgraded values were selected
 */
public record ResolvedCard(
    String instanceId,
    String cardId,
    String name,
    String description,
    int cost,
    CardType type,
    Rarity rarity,
    TargetType target,
    List<EffectConfig> effects,
    String texturePath,
    boolean upgraded) {

  /** Validates and takes defensive copies of resolved values. */
  public ResolvedCard {
    requireNonBlank(instanceId, "instanceId");
    requireNonBlank(cardId, "cardId");
    requireNonBlank(name, "name");
    if (description == null) {
      throw new IllegalArgumentException("description must not be null");
    }
    if (cost < 0) {
      throw new IllegalArgumentException("cost must not be negative");
    }
    if (type == null) {
      throw new IllegalArgumentException("type must not be null");
    }
    if (rarity == null) {
      throw new IllegalArgumentException("rarity must not be null");
    }
    if (target == null) {
      throw new IllegalArgumentException("target must not be null");
    }
    requireNonBlank(texturePath, "texturePath");
    effects = copyEffects(effects);
  }

  /**
   * Returns an immutable deep copy so mutable configuration objects cannot leak to consumers.
   *
   * @return resolved effects in execution order
   */
  @Override
  public List<EffectConfig> effects() {
    return copyEffects(effects);
  }

  private static List<EffectConfig> copyEffects(List<EffectConfig> source) {
    if (source == null || source.isEmpty()) {
      throw new IllegalArgumentException("effects must not be null or empty");
    }

    List<EffectConfig> copies = new ArrayList<>(source.size());
    for (int index = 0; index < source.size(); index++) {
      EffectConfig effect = source.get(index);
      if (effect == null) {
        throw new IllegalArgumentException("effects[" + index + "] must not be null");
      }
      copies.add(new EffectConfig(effect.type, effect.value, effect.duration));
    }
    return List.copyOf(copies);
  }

  private static void requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be null or blank");
    }
  }
}
