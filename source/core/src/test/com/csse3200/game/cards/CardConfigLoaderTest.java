package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CardConfigLoaderTest {
  private static final String TEST_DIRECTORY = "test/cards/";

  @Test
  void shouldLoadCardsFromDefaultFile() {
    List<CardConfig> cards = CardConfigLoader.loadCards();

    assertFalse(cards.isEmpty());
    assertTrue(cards.stream().anyMatch(card -> "strike".equals(card.id)));
    assertTrue(cards.stream().anyMatch(card -> "defend".equals(card.id)));
  }

  @Test
  void shouldIntegrateWithCardLibrary() {
    List<CardConfig> cards = CardConfigLoader.loadCards();
    CardLibrary library = new CardLibrary(cards);

    assertFalse(library.getAllCards().isEmpty());
    assertTrue(library.getCard("strike").isPresent());
    assertEquals("Strike", library.getCard("strike").orElseThrow().name);
  }

  @Test
  void shouldReturnImmutableCardList() {
    List<CardConfig> cards = CardConfigLoader.loadCards();

    assertThrows(UnsupportedOperationException.class, () -> cards.add(new CardConfig()));
  }

  @Test
  void shouldRejectNullAndBlankFilenames() {
    assertAll(
        () -> assertThrows(CardLoadingException.class, () -> CardConfigLoader.loadCards(null)),
        () -> assertThrows(CardLoadingException.class, () -> CardConfigLoader.loadCards("  ")));
  }

  @Test
  void shouldRejectMissingFile() {
    CardLoadingException exception =
        assertThrows(
            CardLoadingException.class,
            () -> CardConfigLoader.loadCards(TEST_DIRECTORY + "missing.json"));

    assertTrue(exception.getMessage().contains("does not exist"));
  }

  @Test
  void shouldRejectMalformedJson() {
    CardLoadingException exception =
        assertThrows(
            CardLoadingException.class,
            () -> CardConfigLoader.loadCards(TEST_DIRECTORY + "malformed.json"));

    assertTrue(exception.getMessage().contains("Malformed"));
  }

  @Test
  void shouldRejectMissingCardsArray() {
    CardLoadingException exception =
        assertThrows(
            CardLoadingException.class,
            () -> CardConfigLoader.loadCards(TEST_DIRECTORY + "missing_cards_array.json"));

    assertTrue(exception.getMessage().contains("'cards' array"), exception.getMessage());
  }

  @Test
  void shouldRejectEmptyCardsArray() {
    CardLoadingException exception =
        assertThrows(
            CardLoadingException.class,
            () -> CardConfigLoader.loadCards(TEST_DIRECTORY + "empty_cards.json"));

    assertTrue(exception.getMessage().contains("at least one card"));
  }

  @Test
  void shouldRejectDuplicateIds() {
    CardLoadingException exception =
        assertThrows(
            CardLoadingException.class,
            () -> CardConfigLoader.loadCards(TEST_DIRECTORY + "duplicate_ids.json"));

    assertTrue(exception.getMessage().contains("duplicate card ID 'duplicate'"));
  }

  @Test
  void shouldReportInvalidCardValues() {
    CardLoadingException exception =
        assertThrows(
            CardLoadingException.class,
            () -> CardConfigLoader.loadCards(TEST_DIRECTORY + "invalid_card.json"));

    String message = exception.getMessage();

    assertAll(
        () -> assertTrue(message.contains("id must not be null or blank")),
        () -> assertTrue(message.contains("name must not be blank")),
        () -> assertTrue(message.contains("cost must not be negative")),
        () -> assertTrue(message.contains("texturePath must not be blank")),
        () -> assertTrue(message.contains("at least one effect")));
  }

  @Test
  void shouldRejectMissingRequiredFields() {
    CardLoadingException exception =
        assertThrows(
            CardLoadingException.class,
            () -> CardConfigLoader.loadCards(TEST_DIRECTORY + "missing_fields.json"));

    String message = exception.getMessage();

    assertAll(
        () -> assertTrue(message.contains("missing required field 'name'")),
        () -> assertTrue(message.contains("missing required field 'cost'")),
        () -> assertTrue(message.contains("missing required field 'type'")),
        () -> assertTrue(message.contains("missing required field 'effects'")));
  }

  @Test
  void shouldRejectUnsupportedCardType() {
    CardLoadingException exception =
        assertThrows(
            CardLoadingException.class,
            () -> CardConfigLoader.loadCards(TEST_DIRECTORY + "unsupported_type.json"));

    assertTrue(exception.getMessage().contains("could not be parsed"), exception.getMessage());
  }

  @Test
  void shouldRejectNullCardEntry() {
    CardLoadingException exception =
        assertThrows(
            CardLoadingException.class,
            () -> CardConfigLoader.loadCards(TEST_DIRECTORY + "null_card.json"));

    assertTrue(exception.getMessage().contains("must be a JSON object"));
  }
}
