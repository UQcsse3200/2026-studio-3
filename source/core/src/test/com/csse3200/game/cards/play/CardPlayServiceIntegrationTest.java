package com.csse3200.game.cards.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Exercises the unified card-play transaction against Team 6's real Sprint 1 card data. */
@ExtendWith(GameExtension.class)
class CardPlayServiceIntegrationTest {
  private static final List<String> CARD_IDS =
      List.of("strike", "defend", "poison_dagger", "expose", "inner_focus", "bandage");

  @Test
  void shouldPlayEveryInitialTeamSixCardThroughTheUnifiedEntryPoint() {
    CardLibrary library = new CardLibrary(CardConfigLoader.loadCards());
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(library, CARD_IDS));

    // Draw every card into hand up front — playing a card no longer draws a replacement.
    battleDeck.drawCards(CARD_IDS.size());

    EnergyComponent energy = new EnergyComponent(10);
    CardPlayService service = new CardPlayService(library, battleDeck, energy);

    CardPlayResult strike =
        service.playCard(CardPlayRequest.singleEnemy(instanceId(battleDeck, "strike"), "enemy-1"));
    CardPlayResult defend =
        service.playCard(CardPlayRequest.self(instanceId(battleDeck, "defend")));
    CardPlayResult poisonDagger =
        service.playCard(
            CardPlayRequest.singleEnemy(instanceId(battleDeck, "poison_dagger"), "enemy-1"));
    CardPlayResult expose =
        service.playCard(CardPlayRequest.allEnemies(instanceId(battleDeck, "expose")));
    CardPlayResult innerFocus =
        service.playCard(CardPlayRequest.self(instanceId(battleDeck, "inner_focus")));
    CardPlayResult bandage =
        service.playCard(CardPlayRequest.self(instanceId(battleDeck, "bandage")));

    assertTrue(
        Stream.of(strike, defend, poisonDagger, expose, innerFocus, bandage)
            .allMatch(CardPlayResult::success));

    assertEquals(List.of(EffectType.DAMAGE), types(strike.enemyEffects()));
    assertEquals(List.of(EffectType.BLOCK), types(defend.playerEffects()));
    assertEquals(List.of(EffectType.DAMAGE, EffectType.POISON), types(poisonDagger.enemyEffects()));
    assertEquals(List.of(EffectType.VULNERABLE), types(expose.enemyEffects()));
    assertEquals(List.of(EffectType.STRENGTH), types(innerFocus.playerEffects()));
    assertEquals(List.of(EffectType.HEAL), types(bandage.playerEffects()));

    assertEquals(3, energy.getCurrentEnergy());
    assertTrue(bandage.updatedHand().isEmpty());
    assertEquals(
        CARD_IDS, bandage.updatedDiscardPile().stream().map(CardInstance::cardId).toList());
  }

  private static String instanceId(BattleDeck deck, String cardId) {
    return deck.getHand().stream()
        .filter(card -> card.cardId().equals(cardId))
        .findFirst()
        .orElseThrow()
        .instanceId();
  }

  private static List<EffectType> types(
      List<com.csse3200.game.cards.effects.ResolvedCardEffect> effects) {
    return effects.stream().map(com.csse3200.game.cards.effects.ResolvedCardEffect::type).toList();
  }
}
