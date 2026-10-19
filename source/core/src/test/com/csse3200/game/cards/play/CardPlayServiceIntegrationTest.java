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
import java.util.concurrent.atomic.AtomicInteger;
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

    EnergyComponent energy = new EnergyComponent(EnergyComponent.MAX_ENERGY_CAP);
    CardPlayService service = new CardPlayService(library, battleDeck, energy);
    AtomicInteger energySpent = new AtomicInteger();

    // Max energy is capped at 5, so each card is played on a fresh turn and the total cost is
    // tracked across all plays.
    CardPlayResult strike =
        playOnFreshTurn(
            service,
            energy,
            energySpent,
            CardPlayRequest.singleEnemy(instanceId(battleDeck, "strike"), "enemy-1"));
    CardPlayResult defend =
        playOnFreshTurn(
            service, energy, energySpent, CardPlayRequest.self(instanceId(battleDeck, "defend")));
    CardPlayResult poisonDagger =
        playOnFreshTurn(
            service,
            energy,
            energySpent,
            CardPlayRequest.singleEnemy(instanceId(battleDeck, "poison_dagger"), "enemy-1"));
    CardPlayResult expose =
        playOnFreshTurn(
            service,
            energy,
            energySpent,
            CardPlayRequest.allEnemies(instanceId(battleDeck, "expose")));
    CardPlayResult innerFocus =
        playOnFreshTurn(
            service,
            energy,
            energySpent,
            CardPlayRequest.self(instanceId(battleDeck, "inner_focus")));
    CardPlayResult bandage =
        playOnFreshTurn(
            service, energy, energySpent, CardPlayRequest.self(instanceId(battleDeck, "bandage")));
    assertTrue(
        Stream.of(strike, defend, poisonDagger, expose, innerFocus, bandage)
            .allMatch(CardPlayResult::success));

    assertEquals(List.of(EffectType.DAMAGE), types(strike.enemyEffects()));
    assertEquals(List.of(EffectType.BLOCK), types(defend.playerEffects()));
    assertEquals(List.of(EffectType.DAMAGE, EffectType.POISON), types(poisonDagger.enemyEffects()));
    assertEquals(List.of(EffectType.VULNERABLE), types(expose.enemyEffects()));
    assertEquals(List.of(EffectType.STRENGTH), types(innerFocus.playerEffects()));
    assertEquals(List.of(EffectType.HEAL), types(bandage.playerEffects()));

    assertEquals(7, energySpent.get());
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

  private static CardPlayResult playOnFreshTurn(
      CardPlayService service,
      EnergyComponent energy,
      AtomicInteger energySpent,
      CardPlayRequest request) {
    energy.onTurnStart();
    int before = energy.getCurrentEnergy();
    CardPlayResult result = service.playCard(request);
    energySpent.addAndGet(before - energy.getCurrentEnergy());
    return result;
  }
}
