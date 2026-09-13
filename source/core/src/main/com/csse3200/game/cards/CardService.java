package com.csse3200.game.cards;

import com.csse3200.game.cards.configs.CardConfig;
import java.util.List;
import java.util.Optional;

/**
 * Public retrieval API for card definitions.
 *
 * <p>Other game systems should depend on this interface rather than the Card Library's internal
 * storage. Unknown IDs are reported as an empty {@link Optional} instead of {@code null}.
 */
public interface CardService {
  /**
   * Retrieves the configuration of the card with the given identifier.
   *
   * @param cardId the unique identifier of the card to retrieve
   * @return the card configuration, or an empty {@link Optional} if the ID is unknown, blank or
   *     null
   */
  Optional<CardConfig> getCard(String cardId);

  /**
   * Retrieves all currently registered card configurations.
   *
   * @return an unmodifiable snapshot of all registered card configurations. Modifications to the
   *     returned list do not affect the library.
   */
  List<CardConfig> getAllCards();
}
