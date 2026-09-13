package com.csse3200.game.cards;

import com.csse3200.game.cards.configs.CardConfig;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Lightweight card catalogue for tests that do not exercise JSON loading. */
public final class TestCardService implements CardService {
  private final Map<String, CardConfig> cardsById;

  private TestCardService(Map<String, CardConfig> cardsById) {
    this.cardsById = cardsById;
  }

  /**
   * Creates a catalogue containing the supplied IDs.
   *
   * @param cardIds IDs that should be considered valid
   * @return test card catalogue
   */
  public static CardService withCards(String... cardIds) {
    Map<String, CardConfig> cards = new LinkedHashMap<>();
    Arrays.stream(cardIds)
        .filter(id -> id != null && !id.isBlank())
        .forEach(
            id -> {
              CardConfig config = new CardConfig();
              config.id = id;
              cards.put(id, config);
            });
    return new TestCardService(cards);
  }

  @Override
  public Optional<CardConfig> getCard(String cardId) {
    if (cardId == null || cardId.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(cardsById.get(cardId));
  }

  @Override
  public List<CardConfig> getAllCards() {
    return List.copyOf(cardsById.values());
  }
}
