package com.csse3200.game.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.extensions.GameExtension;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ShopInventoryGeneratorTest {
  @Test
  void shouldGenerateDistinctOffersFromCardService() {
    CardService cards = createCardService();
    ShopInventoryGenerator generator = new ShopInventoryGenerator(cards, new Random(42));

    ShopService shop = generator.createShop(4, 1, 3);

    assertEquals(4, shop.getItems().size());
    Set<String> cardIds = new HashSet<>();
    for (ShopItem item : shop.getItems()) {
      assertTrue(cardIds.add(item.cardId));
      assertTrue(cards.getCard(item.cardId).isPresent());
      assertTrue(item.stock >= 1 && item.stock <= 3);
      assertPriceMatchesRarity(item, cards.getCard(item.cardId).orElseThrow().rarity);
    }
  }

  @Test
  void shouldProduceRepeatableShopWithSeededRandom() {
    CardService cards = createCardService();
    ShopInventoryGenerator first = new ShopInventoryGenerator(cards, new Random(7));
    ShopInventoryGenerator second = new ShopInventoryGenerator(cards, new Random(7));

    List<ShopItem> firstItems = new ArrayList<>(first.createShop(5, 1, 4).getItems());
    List<ShopItem> secondItems = new ArrayList<>(second.createShop(5, 1, 4).getItems());

    assertEquals(firstItems.size(), secondItems.size());
    for (int index = 0; index < firstItems.size(); index++) {
      ShopItem firstItem = firstItems.get(index);
      ShopItem secondItem = secondItems.get(index);
      assertEquals(firstItem.cardId, secondItem.cardId);
      assertEquals(firstItem.price, secondItem.price);
      assertEquals(firstItem.stock, secondItem.stock);
    }
  }

  @Test
  void shouldReturnAllCardsWhenOfferCountExceedsLibrarySize() {
    CardService cards = createCardService();
    ShopInventoryGenerator generator = new ShopInventoryGenerator(cards, new Random(3));

    ShopService shop = generator.createShop(100, 1, 1);

    assertFalse(cards.getAllCards().isEmpty());
    assertEquals(cards.getAllCards().size(), shop.getItems().size());
  }

  @Test
  void shouldRejectInvalidGenerationSettings() {
    ShopInventoryGenerator generator =
        new ShopInventoryGenerator(createCardService(), new Random(1));

    assertThrows(IllegalArgumentException.class, () -> generator.createShop(0, 1, 2));
    assertThrows(IllegalArgumentException.class, () -> generator.createShop(2, 0, 2));
    assertThrows(IllegalArgumentException.class, () -> generator.createShop(2, 3, 2));
  }

  private CardService createCardService() {
    return new CardLibrary(CardConfigLoader.loadCards());
  }

  private void assertPriceMatchesRarity(ShopItem item, Rarity rarity) {
    int expectedBasePrice;
    switch (rarity) {
      case RARE:
        expectedBasePrice = 50;
        break;
      case UNCOMMON:
        expectedBasePrice = 35;
        break;
      case COMMON:
      default:
        expectedBasePrice = 20;
        break;
    }
    assertTrue(item.price >= expectedBasePrice - 5);
    assertTrue(item.price <= expectedBasePrice + 5);
  }
}
