package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.extensions.GameExtension;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CardLibraryTest {
  private final CardLibrary library = new CardLibrary();
  private final CardConfig strike = card("strike");
  private final CardConfig defend = card("defend");

  private static CardConfig card(String id) {
    CardConfig config = new CardConfig();
    config.id = id;
    config.name = "Test Card";
    config.description = "Test card description.";
    config.cost = 1;
    config.effects = new EffectConfig[] {new EffectConfig(EffectType.DAMAGE, 1)};
    config.texturePath = "images/cards/test.png";
    return config;
  }

  @BeforeEach
  void setUp() {
    library.register(strike);
    library.register(defend);
  }

  @Test
  void shouldRegisterAndRetrieveCardById() {
    Optional<CardConfig> result = library.getCard("strike");
    assertTrue(result.isPresent());
    assertEquals(strike, result.get());
  }

  @Test
  void shouldRetrieveAllRegisteredCards() {
    List<CardConfig> allCards = library.getAllCards();
    assertEquals(2, allCards.size());
    assertTrue(allCards.contains(strike));
    assertTrue(allCards.contains(defend));
  }

  @Test
  void shouldRegisterCardsFromConstructor() {
    CardLibrary loaded = new CardLibrary(List.of(strike, defend));

    assertEquals(strike, loaded.getCard("strike").orElseThrow());
    assertEquals(defend, loaded.getCard("defend").orElseThrow());
    assertEquals(2, loaded.getAllCards().size());
  }

  @Test
  void shouldCreateEmptyLibraryFromEmptyCollection() {
    CardLibrary loaded = new CardLibrary(List.of());
    assertTrue(loaded.getAllCards().isEmpty());
  }

  @Test
  void shouldRejectDuplicateIdsWithoutOverwriting() {
    CardConfig duplicate = card("strike");
    IllegalArgumentException error =
        assertThrows(IllegalArgumentException.class, () -> library.register(duplicate));

    assertTrue(error.getMessage().contains("strike"));
    assertEquals(strike, library.getCard("strike").orElseThrow());
    assertEquals(2, library.getAllCards().size());
  }

  @Test
  void shouldRejectDuplicateIdsWhenConstructedFromCollection() {
    CardConfig duplicate = card("strike");
    List<CardConfig> configs = List.of(strike, duplicate);
    assertThrows(IllegalArgumentException.class, () -> new CardLibrary(configs));
  }

  @Test
  void shouldReturnEmptyOptionalForUnknownId() {
    Optional<CardConfig> result = library.getCard("missing");
    assertTrue(result.isEmpty());
  }

  @Test
  void shouldReturnEmptyOptionalForNullId() {
    assertTrue(library.getCard(null).isEmpty());
  }

  @Test
  void shouldReturnEmptyOptionalForEmptyId() {
    assertTrue(library.getCard("").isEmpty());
  }

  @Test
  void shouldReturnEmptyOptionalForWhitespaceOnlyId() {
    assertTrue(library.getCard(" \t\n").isEmpty());
  }

  @Test
  void shouldMatchIdsCaseSensitively() {
    assertTrue(library.getCard("Strike").isEmpty());
    assertEquals(strike, library.getCard("strike").orElseThrow());
  }

  @Test
  void shouldRejectNullConfig() {
    assertThrows(IllegalArgumentException.class, () -> library.register(null));
  }

  @Test
  void shouldRejectNullCardConfigsInConstructor() {
    assertThrows(IllegalArgumentException.class, () -> new CardLibrary(null));
  }

  @Test
  void shouldRejectBlankCardId() {
    for (String id : new String[] {null, "", "  "}) {
      CardConfig config = card(id);
      assertThrows(IllegalArgumentException.class, () -> library.register(config));
    }
  }

  @Test
  void shouldRejectRegisteringSameCardObjectTwice() {
    assertThrows(IllegalArgumentException.class, () -> library.register(strike));
  }

  @Test
  void shouldRejectCardIdsWithSurroundingWhitespace() {
    CardConfig leadingWhitespace = card(" strike");
    CardConfig trailingWhitespace = card("strike ");

    assertAll(
        () ->
            assertThrows(IllegalArgumentException.class, () -> library.register(leadingWhitespace)),
        () ->
            assertThrows(
                IllegalArgumentException.class, () -> library.register(trailingWhitespace)));
  }

  @Test
  void shouldRejectNullElementInConstructorCollection() {
    List<CardConfig> configs = Collections.singletonList(null);

    assertThrows(IllegalArgumentException.class, () -> new CardLibrary(configs));
  }

  @Test
  void shouldRejectNullIdInConstructorCollection() {
    List<CardConfig> configs = List.of(card(null));

    assertThrows(IllegalArgumentException.class, () -> new CardLibrary(configs));
  }

  @Test
  void shouldRejectEmptyOrWhitespaceOnlyIdInConstructorCollection() {
    List<CardConfig> emptyIdConfigs = List.of(card(""));
    List<CardConfig> whitespaceIdConfigs = List.of(card("  "));

    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> new CardLibrary(emptyIdConfigs)),
        () ->
            assertThrows(
                IllegalArgumentException.class, () -> new CardLibrary(whitespaceIdConfigs)));
  }

  @Test
  void shouldPreventModificationOfReturnedCollection() {
    List<CardConfig> allCards = library.getAllCards();
    CardConfig additionalCard = card("defend");

    assertThrows(UnsupportedOperationException.class, () -> allCards.add(additionalCard));
    assertThrows(UnsupportedOperationException.class, () -> allCards.remove(strike));

    assertEquals(2, library.getAllCards().size());
    assertEquals(strike, library.getCard("strike").orElseThrow());
  }

  @Test
  void shouldReturnEmptyListWhenNoCardsAreRegistered() {
    List<CardConfig> allCards = library.getAllCards();
    assertNotNull(allCards);
    assertFalse(allCards.isEmpty());
  }
}
