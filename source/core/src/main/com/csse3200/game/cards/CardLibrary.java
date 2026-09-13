package com.csse3200.game.cards;

import com.csse3200.game.cards.configs.CardConfig;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central registry of valid {@link CardConfig} definitions.
 *
 * <p>Cards are stored by unique ID. Duplicate IDs are rejected instead of silently overwriting an
 * existing card. Callers receive snapshots of the registered cards and cannot modify the internal
 * collection through the public API.
 *
 * <p>Per-card field validation belongs in {@link CardValidator}. This class only enforces
 * collection-level rules such as unique IDs.
 */
public class CardLibrary implements CardService {
  private static final Logger logger = LoggerFactory.getLogger(CardLibrary.class);

  private final Map<String, CardConfig> cards = new HashMap<>();

  /** Creates an empty card library. */
  public CardLibrary() {}

  /**
   * Creates a library and registers every provided card.
   *
   * @param configs cards to register
   * @throws IllegalArgumentException if {@code configs} is null, a card is invalid, or an ID is
   *     duplicated
   */
  public CardLibrary(Collection<CardConfig> configs) {
    if (configs == null) {
      throw new IllegalArgumentException("Card configs must not be null");
    }
    for (CardConfig config : configs) {
      register(config);
    }
  }

  /**
   * Registers a valid card definition using its unique ID.
   *
   * @param config card to register
   * @throws IllegalArgumentException if {@code config} is null, its ID is null, blank, or has
   *     surrounding whitespace, or the ID is already registered
   */
  public void register(CardConfig config) {
    List<String> errors = CardValidator.validate(config);
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException("Card config is invalid: " + String.join("; ", errors));
    }

    String id = config.id;
    if (cards.containsKey(id)) {
      throw new IllegalArgumentException("Duplicate card ID: " + id);
    }

    cards.put(id, config);
    logger.debug("Registered card {}", id);
  }

  @Override
  public Optional<CardConfig> getCard(String cardId) {
    if (cardId == null || cardId.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(cards.get(cardId));
  }

  @Override
  public List<CardConfig> getAllCards() {
    return List.copyOf(cards.values());
  }
}
