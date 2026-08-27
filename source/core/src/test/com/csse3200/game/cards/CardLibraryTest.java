package com.csse3200.game.cards;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CardLibraryTest {
  private CardLibrary library;

  @BeforeEach
  void setUp() {
    library = new CardLibrary();
  }

  @Test
  void shouldRegisterAndRetrieveCardById() {
    CardConfig strike = card("strike");
    library.register(strike);

    Optional<CardConfig> result = library.getCard("strike");
    assertTrue(result.isPresent());
    assertEquals(strike, result.get());
  }

  @Test
  void shouldRetrieveAllRegisteredCards() {
    CardConfig strike = card("strike");
    CardConfig defend = card("defend");
    library.register(strike);
    library.register(defend);

    List<CardConfig> allCards = library.getAllCards();
    assertEquals(2, allCards.size());
    assertTrue(allCards.contains(strike));
    assertTrue(allCards.contains(defend));
  }

  @Test
  void shouldRegisterCardsFromConstructor() {
    CardConfig strike = card("strike");
    CardConfig defend = card("defend");

    CardLibrary loaded = new CardLibrary(List.of(strike, defend));

    assertEquals(strike, loaded.getCard("strike").orElseThrow());
    assertEquals(defend, loaded.getCard("defend").orElseThrow());
    assertEquals(2, loaded.getAllCards().size());
  }

  @Test
  void shouldRejectDuplicateIdsWithoutOverwriting() {
    CardConfig original = card("strike");
    CardConfig duplicate = card("strike");
    library.register(original);

    IllegalArgumentException error =
        assertThrows(IllegalArgumentException.class, () -> library.register(duplicate));

    assertTrue(error.getMessage().contains("strike"));
    assertEquals(original, library.getCard("strike").orElseThrow());
    assertEquals(1, library.getAllCards().size());
  }

  @Test
  void shouldRejectDuplicateIdsWhenConstructedFromCollection() {
    CardConfig original = card("strike");
    CardConfig duplicate = card("strike");
    List<CardConfig> configs = List.of(original, duplicate);

    assertThrows(IllegalArgumentException.class, () -> new CardLibrary(configs));
  }

  @Test
  void shouldReturnEmptyOptionalForUnknownId() {
    library.register(card("strike"));

    Optional<CardConfig> result = library.getCard("missing");
    assertTrue(result.isEmpty());
  }

  @Test
  void shouldReturnEmptyOptionalForNullId() {
    assertTrue(library.getCard(null).isEmpty());
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
    assertThrows(IllegalArgumentException.class, () -> library.register(card(null)));
    assertThrows(IllegalArgumentException.class, () -> library.register(card("")));
    assertThrows(IllegalArgumentException.class, () -> library.register(card("  ")));
  }

  @Test
  void shouldPreventModificationOfReturnedCollection() {
    CardConfig strike = card("strike");
    library.register(strike);

    List<CardConfig> allCards = library.getAllCards();
    assertThrows(UnsupportedOperationException.class, () -> allCards.add(card("defend")));
    assertThrows(UnsupportedOperationException.class, () -> allCards.remove(strike));

    assertEquals(1, library.getAllCards().size());
    assertEquals(strike, library.getCard("strike").orElseThrow());
  }

  @Test
  void shouldReturnEmptyListWhenNoCardsAreRegistered() {
    List<CardConfig> allCards = library.getAllCards();
    assertNotNull(allCards);
    assertTrue(allCards.isEmpty());
  }

  private static CardConfig card(String id) {
    CardConfig config = new CardConfig();
    config.id = id;
    return config;
  }
}
