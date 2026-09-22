package com.csse3200.game.cards;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.CardUpgradeConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import java.util.ArrayList;
import java.util.List;

/** Validates individual card configurations independently of loading and storage. */
public final class CardValidator {
  private CardValidator() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Checks that a card is internally consistent and safe for gameplay systems to use.
   * Collection-level checks, such as duplicate IDs, belong to the card library or loader.
   *
   * @param card card configuration to validate, may be null
   * @return an immutable list of human-readable problems, empty if the card is valid
   */
  public static List<String> validate(CardConfig card) {
    List<String> errors = new ArrayList<>();

    if (card == null) {
      return List.of("card config must not be null");
    }

    validateBasicFields(card, errors);
    validateEffects(card.effects, card.target, errors);
    validateUpgrade(card.upgrade, card.target, errors);

    return List.copyOf(errors);
  }

  /**
   * @param card card configuration to validate, may be null
   * @return true if the card has no validation problems
   */
  public static boolean isValid(CardConfig card) {
    return validate(card).isEmpty();
  }

  /** Validates the selected runtime values before gameplay or effect calculation. */
  public static List<String> validateResolved(com.csse3200.game.cards.runtime.ResolvedCard card) {
    if (card == null) {
      return List.of("resolved card must not be null");
    }
    List<String> errors = new ArrayList<>();
    List<EffectConfig> effects = card.effects();
    for (int i = 0; i < effects.size(); i++) {
      validateEffect(effects.get(i), i, errors);
      if (effects.get(i).type != null
          && !isCompatibleWithTarget(effects.get(i).type, card.target())) {
        errors.add("effects[" + i + "].type is not compatible with target " + card.target());
      }
    }
    return List.copyOf(errors);
  }

  private static void validateEffect(EffectConfig effect, int index, List<String> errors) {
    String prefix = "effect " + index + ": ";
    if (effect == null) {
      errors.add(prefix + "must not be null");
      return;
    }
    if (effect.type == null) {
      errors.add(prefix + "type must not be null");
      return;
    }
    if (effect.value <= 0) {
      errors.add(prefix + effect.type + " value must be positive, was " + effect.value);
    }
    if (effect.type == EffectType.HEAL) {
      if (effect.duration < 0) {
        errors.add(prefix + "HEAL duration must not be negative");
      }
    } else if (effect.type.usesDuration()) {
      if (effect.duration <= 0) {
        errors.add(prefix + effect.type + " requires a positive duration, was " + effect.duration);
      }
    } else if (effect.duration != 0) {
      errors.add(prefix + effect.type + " must not set a duration, was " + effect.duration);
    }
  }

  private static void validateBasicFields(CardConfig card, List<String> errors) {
    String id = card.id;
    if (id == null || id.isBlank()) {
      errors.add("id must not be null or blank");
    } else if (!id.equals(id.strip())) {
      errors.add("id must not have surrounding whitespace");
    }
    if (card.name == null || card.name.isBlank()) {
      errors.add("name must not be blank");
    }
    if (card.cost < 0) {
      errors.add("cost must not be negative, was " + card.cost);
    }
    if (card.type == null) {
      errors.add("type must not be null");
    }
    if (card.rarity == null) {
      errors.add("rarity must not be null");
    }
    if (card.target == null) {
      errors.add("target must not be null");
    }
    if (card.texturePath == null || card.texturePath.isBlank()) {
      errors.add("texturePath must not be blank");
    }
  }

  private static void validateEffects(
      EffectConfig[] effects, TargetType inheritedTarget, List<String> errors) {
    if (effects == null || effects.length == 0) {
      errors.add("a card must define at least one effect");
      return;
    }

    for (int i = 0; i < effects.length; i++) {
      validateEffect(effects[i], i, errors);
      if (effects[i] != null
          && effects[i].type != null
          && inheritedTarget != null
          && !isCompatibleWithTarget(effects[i].type, inheritedTarget)) {
        errors.add(
            "effects["
                + i
                + "].type "
                + effects[i].type
                + " is not compatible with inherited target "
                + inheritedTarget);
      }
    }
  }

  private static void validateUpgrade(
      CardUpgradeConfig upgrade, TargetType inheritedTarget, List<String> errors) {
    if (upgrade == null) {
      return;
    }
    if (upgrade.name == null || upgrade.name.isBlank()) {
      errors.add("upgrade.name must not be blank");
    }
    if (upgrade.description == null || upgrade.description.isBlank()) {
      errors.add("upgrade.description must not be blank");
    }
    if (upgrade.cost < 0) {
      errors.add("upgrade.cost must not be negative, was " + upgrade.cost);
    }
    if (upgrade.rarity == null) {
      errors.add("upgrade.rarity must not be null");
    }
    if (upgrade.effects == null || upgrade.effects.length == 0) {
      errors.add("upgrade.effects must define at least one effect");
      return;
    }

    for (int i = 0; i < upgrade.effects.length; i++) {
      validateUpgradeEffect(upgrade.effects[i], i, inheritedTarget, errors);
    }
  }

  private static void validateUpgradeEffect(
      EffectConfig effect, int index, TargetType inheritedTarget, List<String> errors) {
    String path = "upgrade.effects[" + index + "]";
    if (effect == null) {
      errors.add(path + " must not be null");
      return;
    }
    if (effect.type == null) {
      errors.add(path + ".type must not be null");
      return;
    }
    if (effect.value <= 0) {
      errors.add(path + ".value must be positive, was " + effect.value);
    }
    if (effect.type == EffectType.HEAL) {
      if (effect.duration < 0) {
        errors.add(path + ".duration must not be negative for HEAL");
      }
    } else if (effect.type.usesDuration()) {
      if (effect.duration <= 0) {
        errors.add(
            path + ".duration must be positive for " + effect.type + ", was " + effect.duration);
      }
    } else if (effect.duration != 0) {
      errors.add(path + ".duration must be zero for " + effect.type + ", was " + effect.duration);
    }

    if (inheritedTarget != null && !isCompatibleWithTarget(effect.type, inheritedTarget)) {
      errors.add(
          path
              + ".type "
              + effect.type
              + " is not compatible with inherited target "
              + inheritedTarget);
    }
  }

  /**
   * Target compatibility used for upgrade effects (and documented for future base-effect checks).
   *
   * <p>{@code FORTIFY} is SELF-only. Instant non-DAMAGE enemy effects must be listed here or they
   * are rejected inside upgrade blocks.
   */
  static boolean isCompatibleWithTarget(EffectType effectType, TargetType target) {
    if (target == TargetType.SELF) {
      return effectType == EffectType.BLOCK
          || effectType == EffectType.HEAL
          || effectType == EffectType.STRENGTH
          || effectType == EffectType.ENERGY_GAIN
          || effectType == EffectType.CLEANSE
          || effectType == EffectType.FORTIFY;
    }
    return effectType == EffectType.DAMAGE
        || effectType == EffectType.PIERCE
        || effectType == EffectType.SUNDER
        || effectType.usesDuration();
  }
}
