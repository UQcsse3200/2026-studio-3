package com.csse3200.game.areas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.encounters.integration.EncounterFlowController;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.shop.ShopInventoryGenerator;
import com.csse3200.game.shop.ShopService;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class EncounterGameAreaTest {
  @Test
  void shouldRouteEventNodeToChanceEncounter() {
    assertEquals(
        EncounterFlowController.EncounterType.CHANCE,
        EncounterGameArea.encounterTypeFor(RoomType.EVENT));
  }

  @Test
  void shouldRouteShopNodeToShopEncounter() {
    assertEquals(
        EncounterFlowController.EncounterType.SHOP,
        EncounterGameArea.encounterTypeFor(RoomType.SHOP));
  }

  @Test
  void shouldRejectCombatAndMissingRoomTypes() {
    assertThrows(
        IllegalArgumentException.class, () -> EncounterGameArea.encounterTypeFor(RoomType.COMBAT));
    assertThrows(IllegalArgumentException.class, () -> EncounterGameArea.encounterTypeFor(null));
  }

  @Test
  void shouldBuildMapShopFromTheCardLibrary() {
    CardService cardLibrary = new CardLibrary(CardConfigLoader.loadCards());

    ShopService shop = EncounterGameArea.createMapShop(cardLibrary);

    assertEquals(ShopInventoryGenerator.DEFAULT_OFFER_COUNT, shop.getItems().size());
    Set<String> cardIds = new HashSet<>();
    shop.getItems()
        .forEach(
            item -> {
              assertTrue(cardIds.add(item.cardId));
              assertTrue(cardLibrary.getCard(item.cardId).isPresent());
              assertTrue(
                  !item.cardId.equals("poison_blade") && !item.cardId.equals("unseal_the_breach"));
            });
  }
}
