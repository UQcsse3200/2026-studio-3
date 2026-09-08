package com.csse3200.game.cards.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.effects.CardEffectResolution;
import com.csse3200.game.cards.effects.CardEffectResolutionService;
import com.csse3200.game.cards.effects.ResolvedCardEffect;
import com.csse3200.game.components.player.EnergyComponent;
import java.util.List;
import org.junit.jupiter.api.Test;

class CardPlayServiceTest {
  @Test
  void shouldSpendEnergyResolveEffectsAndDiscardPlayedCard() {
    CardConfig strike =
        card("strike", 1, TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));
    CardLibrary cardLibrary = new CardLibrary(List.of(strike));
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike")));
    battleDeck.drawOne();
    EnergyComponent energyComponent = new EnergyComponent(3);
    CardEffectResolutionService resolutionService = new CardEffectResolutionService(cardLibrary);
    CardPlayService playService =
        new CardPlayService(cardLibrary, resolutionService, battleDeck, energyComponent);

    CardPlayResult result = playService.playCard("strike");

    assertTrue(result.successful());
    assertEquals(CardPlayFailureReason.NONE, result.failureReason());
    assertEquals(1, result.energyCost());
    assertEquals(2, energyComponent.getCurrentEnergy());
    assertTrue(battleDeck.getHand().isEmpty());
    assertIterableEquals(List.of("strike"), battleDeck.getDiscardPile());
    assertTrue(result.updatedHand().isEmpty());
    assertTrue(result.updatedDrawPile().isEmpty());
    assertIterableEquals(List.of("strike"), result.updatedDiscardPile());
    assertIterableEquals(
        List.of(
            new ResolvedCardEffect("strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 6, 0, 0)),
        result.enemyEffects());
    assertTrue(result.playerEffects().isEmpty());
    assertIterableEquals(List.of(result.resolution()), resolutionService.getResolutions());
  }

  @Test
  void shouldReturnOneCompleteResultForUnifiedCardPlayRequest() {
    CardConfig strike =
        card("strike", 1, TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));
    CardLibrary cardLibrary = new CardLibrary(List.of(strike));
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike", "defend")));
    battleDeck.drawCards(2);
    EnergyComponent energyComponent = new EnergyComponent(3);
    CardPlayService playService = new CardPlayService(cardLibrary, battleDeck, energyComponent);
    CardPlayRequest request = CardPlayRequest.singleEnemy("strike", "enemy-1");

    CardPlayResult result = playService.playCard(request);

    assertTrue(result.success());
    assertTrue(result.successful());
    assertEquals(request.target(), result.target());
    assertEquals(result.effectResolution(), result.resolution());
    assertEquals(1, result.energyCost());
    assertEquals(2, energyComponent.getCurrentEnergy());
    assertIterableEquals(List.of("defend"), result.updatedHand());
    assertTrue(result.updatedDrawPile().isEmpty());
    assertIterableEquals(List.of("strike"), result.updatedDiscardPile());
    assertFalse(playService.canPlay(CardPlayRequest.singleEnemy("defend", "enemy-1")));
  }

  @Test
  void shouldResolveDamageFromPlayerAndEnemyStateViews() {
    CardConfig strike =
        card("strike", 1, TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));
    CardLibrary cardLibrary = new CardLibrary(List.of(strike));
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike")));
    battleDeck.drawOne();
    EnergyComponent energyComponent = new EnergyComponent(3);
    PlayerStateView playerState = playerStateView(energyComponent, 2, 1);
    EnemyStateView enemyState = enemyStateView("enemy-1", 1);
    CardPlayService playService =
        new CardPlayService(cardLibrary, battleDeck, energyComponent, playerState, enemyState);

    CardPlayResult result = playService.playCard(CardPlayRequest.singleEnemy("strike", "enemy-1"));

    assertTrue(result.success());
    assertEquals(
        List.of(
            new ResolvedCardEffect("strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 9, 0, 0)),
        result.enemyEffects());
    assertEquals(2, energyComponent.getCurrentEnergy());
    assertIterableEquals(List.of("strike"), result.updatedDiscardPile());
  }

  @Test
  void shouldRejectUnavailableEnemyBeforeSpendingEnergy() {
    CardConfig strike =
        card("strike", 1, TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));
    CardLibrary cardLibrary = new CardLibrary(List.of(strike));
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike")));
    battleDeck.drawOne();
    EnergyComponent energyComponent = new EnergyComponent(3);
    PlayerStateView playerState = playerStateView(energyComponent, 0, 0);
    EnemyStateView enemyState = enemyStateView("enemy-1", 0);
    CardPlayService playService =
        new CardPlayService(cardLibrary, battleDeck, energyComponent, playerState, enemyState);

    CardPlayResult result =
        playService.playCard(CardPlayRequest.singleEnemy("strike", "missing-enemy"));

    assertFalse(result.success());
    assertEquals(CardPlayFailureReason.INVALID_TARGET, result.failureReason());
    assertEquals(3, energyComponent.getCurrentEnergy());
    assertIterableEquals(List.of("strike"), result.updatedHand());
  }

  @Test
  void shouldRejectMismatchedTargetWithoutChangingEnergyOrDeck() {
    CardConfig strike =
        card("strike", 1, TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));
    CardLibrary cardLibrary = new CardLibrary(List.of(strike));
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike")));
    battleDeck.drawOne();
    EnergyComponent energyComponent = new EnergyComponent(3);
    CardEffectResolutionService resolutionService = new CardEffectResolutionService(cardLibrary);
    CardPlayService playService =
        new CardPlayService(cardLibrary, resolutionService, battleDeck, energyComponent);

    CardPlayResult result = playService.playCard(CardPlayRequest.self("strike"));

    assertFalse(result.success());
    assertEquals(CardPlayFailureReason.INVALID_TARGET, result.failureReason());
    assertEquals(3, energyComponent.getCurrentEnergy());
    assertIterableEquals(List.of("strike"), result.updatedHand());
    assertTrue(result.updatedDiscardPile().isEmpty());
    assertTrue(result.enemyEffects().isEmpty());
    assertTrue(resolutionService.getResolutions().isEmpty());
  }

  @Test
  void shouldReturnUnknownCardFailureFromUnifiedEntryPoint() {
    CardLibrary cardLibrary = new CardLibrary();
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike")));
    battleDeck.drawOne();
    EnergyComponent energyComponent = new EnergyComponent(3);
    CardPlayService playService = new CardPlayService(cardLibrary, battleDeck, energyComponent);

    CardPlayResult result = playService.playCard(CardPlayRequest.singleEnemy("missing", "enemy-1"));

    assertFalse(result.success());
    assertEquals(CardPlayFailureReason.UNKNOWN_CARD, result.failureReason());
    assertEquals(0, result.energyCost());
    assertEquals(3, energyComponent.getCurrentEnergy());
    assertIterableEquals(List.of("strike"), result.updatedHand());
  }

  @Test
  void shouldReturnInvalidCardConfigFailureWithoutSpendingEnergy() {
    CardConfig strike =
        card("strike", 1, TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));
    CardLibrary cardLibrary = new CardLibrary(List.of(strike));
    strike.effects = new EffectConfig[0];
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike")));
    battleDeck.drawOne();
    EnergyComponent energyComponent = new EnergyComponent(3);
    CardPlayService playService = new CardPlayService(cardLibrary, battleDeck, energyComponent);

    CardPlayResult result = playService.playCard(CardPlayRequest.singleEnemy("strike", "enemy-1"));

    assertFalse(result.success());
    assertEquals(CardPlayFailureReason.INVALID_CARD_CONFIG, result.failureReason());
    assertEquals(3, energyComponent.getCurrentEnergy());
    assertIterableEquals(List.of("strike"), result.updatedHand());
    assertTrue(result.enemyEffects().isEmpty());
  }

  @Test
  void shouldRestoreEnergyWhenACommitStepThrows() {
    CardConfig strike =
        card("strike", 1, TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));
    CardLibrary cardLibrary = new CardLibrary(List.of(strike));
    BattleDeck failingDeck =
        new BattleDeck(new PlayerDeck(List.of("strike"))) {
          @Override
          public List<String> getHand() {
            return List.of("strike");
          }

          @Override
          public boolean playCard(String cardId) {
            return false;
          }
        };
    EnergyComponent energyComponent = new EnergyComponent(3);
    CardPlayService playService = new CardPlayService(cardLibrary, failingDeck, energyComponent);

    assertThrows(
        IllegalStateException.class,
        () -> playService.playCard(CardPlayRequest.singleEnemy("strike", "enemy-1")));
    assertEquals(3, energyComponent.getCurrentEnergy());
  }

  @Test
  void shouldNotResolveOrDiscardWhenEnergyIsInsufficient() {
    CardConfig innerFocus =
        card("inner_focus", 2, TargetType.SELF, new EffectConfig(EffectType.STRENGTH, 2));
    CardLibrary cardLibrary = new CardLibrary(List.of(innerFocus));
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("inner_focus")));
    battleDeck.drawOne();
    EnergyComponent energyComponent = new EnergyComponent(3);
    energyComponent.spendEnergy(2);
    CardEffectResolutionService resolutionService = new CardEffectResolutionService(cardLibrary);
    CardPlayService playService =
        new CardPlayService(cardLibrary, resolutionService, battleDeck, energyComponent);

    CardPlayResult result = playService.playCard("inner_focus");

    assertFalse(result.successful());
    assertEquals(CardPlayFailureReason.NOT_ENOUGH_ENERGY, result.failureReason());
    assertEquals(1, energyComponent.getCurrentEnergy());
    assertIterableEquals(List.of("inner_focus"), battleDeck.getHand());
    assertTrue(battleDeck.getDiscardPile().isEmpty());
    assertTrue(result.enemyEffects().isEmpty());
    assertTrue(result.playerEffects().isEmpty());
    assertTrue(resolutionService.getResolutions().isEmpty());
  }

  @Test
  void shouldNotSpendEnergyWhenCardIsNotInHand() {
    CardConfig strike =
        card("strike", 1, TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));
    CardLibrary cardLibrary = new CardLibrary(List.of(strike));
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike")));
    EnergyComponent energyComponent = new EnergyComponent(3);
    CardEffectResolutionService resolutionService = new CardEffectResolutionService(cardLibrary);
    CardPlayService playService =
        new CardPlayService(cardLibrary, resolutionService, battleDeck, energyComponent);

    CardPlayResult result = playService.playCard("strike");

    assertFalse(result.successful());
    assertEquals(CardPlayFailureReason.CARD_NOT_IN_HAND, result.failureReason());
    assertEquals(3, energyComponent.getCurrentEnergy());
    assertIterableEquals(List.of("strike"), battleDeck.getDrawPile());
    assertTrue(battleDeck.getHand().isEmpty());
    assertTrue(battleDeck.getDiscardPile().isEmpty());
    assertTrue(resolutionService.getResolutions().isEmpty());
  }

  @Test
  void shouldPreviewCanPlayWithoutSpendingEnergy() {
    CardConfig strike =
        card("strike", 1, TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));
    CardLibrary cardLibrary = new CardLibrary(List.of(strike));
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike")));
    battleDeck.drawOne();
    EnergyComponent energyComponent = new EnergyComponent(3);
    CardPlayService playService = new CardPlayService(cardLibrary, battleDeck, energyComponent);

    assertTrue(playService.canPlay("strike"));
    assertEquals(3, energyComponent.getCurrentEnergy());
  }

  @Test
  void shouldRejectMissingDependencies() {
    CardConfig strike =
        card("strike", 1, TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));
    CardLibrary cardLibrary = new CardLibrary(List.of(strike));
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(List.of("strike")));
    EnergyComponent energyComponent = new EnergyComponent(3);
    CardEffectResolutionService resolutionService = new CardEffectResolutionService(cardLibrary);

    assertThrows(
        IllegalArgumentException.class,
        () -> new CardPlayService(null, battleDeck, energyComponent));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CardPlayService(cardLibrary, null, battleDeck, energyComponent));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CardPlayService(cardLibrary, resolutionService, null, energyComponent));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CardPlayService(cardLibrary, resolutionService, battleDeck, null));
  }

  @Test
  void shouldRejectInvalidCardPlayResults() {
    CardEffectResolution resolution =
        new CardEffectResolution(
            "strike",
            List.of(
                new ResolvedCardEffect(
                    "strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 6, 0, 0)));

    assertThrows(
        IllegalArgumentException.class,
        () -> new CardPlayResult("strike", true, 1, null, CardPlayFailureReason.NONE));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardPlayResult(
                "strike", true, 1, resolution, CardPlayFailureReason.NOT_ENOUGH_ENERGY));
    assertThrows(
        IllegalArgumentException.class,
        () -> new CardPlayResult("strike", false, 1, null, CardPlayFailureReason.NONE));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardPlayResult(
                "strike", false, 1, resolution, CardPlayFailureReason.CARD_NOT_IN_HAND));
  }

  @Test
  void shouldValidateCardPlayRequestsAndTargets() {
    assertThrows(IllegalArgumentException.class, () -> CardPlayRequest.self(" "));
    assertThrows(IllegalArgumentException.class, () -> CardPlayRequest.self(" strike "));
    assertThrows(IllegalArgumentException.class, () -> new CardPlayRequest("strike", null));
    assertThrows(IllegalArgumentException.class, () -> CardPlayTarget.singleEnemy(" "));
    assertThrows(IllegalArgumentException.class, () -> CardPlayTarget.singleEnemy(" enemy-1 "));
    assertThrows(
        IllegalArgumentException.class, () -> new CardPlayTarget(TargetType.SELF, "enemy-1"));
  }

  private static CardConfig card(String id, int cost, TargetType target, EffectConfig... effects) {
    CardConfig card = new CardConfig();
    card.id = id;
    card.name = id;
    card.description = "Test card";
    card.cost = cost;
    card.type = CardType.SKILL;
    card.rarity = Rarity.COMMON;
    card.target = target;
    card.effects = effects;
    card.texturePath = "images/cards/" + id + ".png";
    return card;
  }

  private static PlayerStateView playerStateView(
      EnergyComponent energyComponent, int strength, int feeble) {
    return new PlayerStateView() {
      @Override
      public int currentEnergy() {
        return energyComponent.getCurrentEnergy();
      }

      @Override
      public int statusValue(EffectType type) {
        return switch (type) {
          case STRENGTH -> strength;
          case FEEBLE -> feeble;
          default -> 0;
        };
      }
    };
  }

  private static EnemyStateView enemyStateView(String availableTargetId, int vulnerable) {
    return new EnemyStateView() {
      @Override
      public boolean isTargetAvailable(String targetId) {
        return availableTargetId.equals(targetId);
      }

      @Override
      public int statusValue(String targetId, EffectType type) {
        if (!isTargetAvailable(targetId)) {
          return 0;
        }
        return type == EffectType.VULNERABLE ? vulnerable : 0;
      }
    };
  }
}
