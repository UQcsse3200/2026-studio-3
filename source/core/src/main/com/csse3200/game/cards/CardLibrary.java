package com.csse3200.game.cards;

import com.csse3200.game.cards.configs.CardConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CardLibrary implements CardService {
  private final Map<String, CardConfig> cards = new HashMap<>();

  @Override
  public Optional<CardConfig> getCard(String cardId) {
    return Optional.ofNullable(cards.get(cardId));
  }

  @Override
  public List<CardConfig> getAllCards() {
    return List.copyOf(cards.values());
  }
}
