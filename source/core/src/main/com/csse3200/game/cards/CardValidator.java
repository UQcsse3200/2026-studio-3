package com.csse3200.game.cards;

import com.csse3200.game.cards.configs.CardConfig;
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
    validateEffects(card.effects, errors);

    return List.copyOf(errors);
  }

  /**
   * @param card card configuration to validate, may be null
   * @return true if the card has no validation problems
   */
  public static boolean isValid(CardConfig card) {
    return validate(card).isEmpty();
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
    if (effect.type.usesDuration()) {
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

  private static void validateEffects(EffectConfig[] effects, List<String> errors) {
    if (effects == null || effects.length == 0) {
      errors.add("a card must define at least one effect");
      return;
    }

    for (int i = 0; i < effects.length; i++) {
      validateEffect(effects[i], i, errors);
    }
  }
}
