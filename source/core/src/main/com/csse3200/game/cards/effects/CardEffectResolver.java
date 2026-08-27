package com.csse3200.game.cards.effects;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.CardValidator;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import java.util.List;

/** Looks up Team 6 card definitions, resolves their targets, and executes effects in order. */
public class CardEffectResolver {
  private final CardService cardService;
  private final EffectExecutor effectExecutor;

  /** Creates a resolver backed by Team 6's public card retrieval API. */
  public CardEffectResolver(CardService cardService) {
    this(cardService, new EffectExecutor());
  }

  /** Creates a resolver with explicit dependencies, primarily for integration and testing. */
  public CardEffectResolver(CardService cardService, EffectExecutor effectExecutor) {
    if (cardService == null) {
      throw new IllegalArgumentException("Card service cannot be null");
    }
    if (effectExecutor == null) {
      throw new IllegalArgumentException("Effect executor cannot be null");
    }
    this.cardService = cardService;
    this.effectExecutor = effectExecutor;
  }

  /**
   * Retrieves a card by ID through {@link CardService}, then resolves it.
   *
   * @throws IllegalArgumentException if the ID is blank or unknown, the card is invalid, or a
   *     required target is missing
   */
  public void resolve(
      String cardId,
      CharacterEffectGateway self,
      CharacterEffectGateway selectedEnemy,
      List<CharacterEffectGateway> allEnemies) {
    if (cardId == null || cardId.isBlank()) {
      throw new IllegalArgumentException("Card ID cannot be null or blank");
    }

    CardConfig card =
        cardService
            .getCard(cardId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown card ID: " + cardId));
    resolve(card, self, selectedEnemy, allEnemies);
  }

  /**
   * Resolves a card configuration that has already been retrieved by the calling system.
   *
   * <p>The target is declared once on {@link CardConfig}; every effect uses that target and is
   * executed in its array declaration order.
   */
  public void resolve(
      CardConfig card,
      CharacterEffectGateway self,
      CharacterEffectGateway selectedEnemy,
      List<CharacterEffectGateway> allEnemies) {
    validateCard(card);
    List<CharacterEffectGateway> targets =
        resolveTargets(card.target, self, selectedEnemy, allEnemies);

    for (EffectConfig effect : card.effects) {
      for (CharacterEffectGateway target : targets) {
        effectExecutor.execute(effect, target);
      }
    }
  }

  private void validateCard(CardConfig card) {
    List<String> errors = CardValidator.validate(card);
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException("Invalid card config: " + String.join("; ", errors));
    }
  }

  private List<CharacterEffectGateway> resolveTargets(
      TargetType targetType,
      CharacterEffectGateway self,
      CharacterEffectGateway selectedEnemy,
      List<CharacterEffectGateway> allEnemies) {
    return switch (targetType) {
      case SELF -> List.of(requireTarget(self, "Self target cannot be null"));
      case SINGLE_ENEMY ->
          List.of(requireTarget(selectedEnemy, "Selected enemy target cannot be null"));
      case ALL_ENEMIES -> copyEnemyTargets(allEnemies);
    };
  }

  private CharacterEffectGateway requireTarget(CharacterEffectGateway target, String errorMessage) {
    if (target == null) {
      throw new IllegalArgumentException(errorMessage);
    }
    return target;
  }

  private List<CharacterEffectGateway> copyEnemyTargets(List<CharacterEffectGateway> allEnemies) {
    if (allEnemies == null) {
      throw new IllegalArgumentException("Enemy target list cannot be null");
    }
    try {
      return List.copyOf(allEnemies);
    } catch (NullPointerException exception) {
      throw new IllegalArgumentException("Enemy target list cannot contain null", exception);
    }
  }
}
