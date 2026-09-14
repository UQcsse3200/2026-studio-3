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
import com.csse3200.game.cards.TestCardService;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.CardInstance;
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
    CardConfig defend = card("defend", 1, TargetType.SELF, new EffectConfig(EffectType.BLOCK, 3));

    CardLibrary cardLibrary = new CardLibrary(List.of(strike, defend));

    BattleDeck battleDeck =
        new BattleDeck(new PlayerDeck(cardLibrary, List.of("strike", "defend")));

    battleDeck.drawOne();

    EnergyComponent energyComponent = new EnergyComponent(3);
    CardEffectResolutionService resolutionService = new CardEffectResolutionService(cardLibrary);

    CardPlayService playService =
        new CardPlayService(cardLibrary, resolutionService, battleDeck, energyComponent);

    CardPlayResult result = playService.playCard("strike");

    assertTrue(result.successful());
    assertEquals(CardPlayFailureReason.NONE, result.failureReason());

    // Energy was spent for the played card.
    assertEquals(1, result.energyCost());
    assertEquals(2, energyComponent.getCurrentEnergy());

    // The played card was discarded.
    assertIterableEquals(List.of("strike"), battleDeck.getDiscardPile());
    assertIterableEquals(List.of("strike"), result.updatedDiscardPile());

    // No replacement is drawn — the hand just shrinks by the played card.
    assertTrue(battleDeck.getHand().isEmpty());
    assertTrue(result.updatedHand().isEmpty());

    // The draw pile is untouched.
    assertIterableEquals(List.of("defend"), battleDeck.getDrawPile());
    assertIterableEquals(List.of("defend"), result.updatedDrawPile());

    // The card's effects were resolved correctly.
    assertIterableEquals(
        List.of(
            new ResolvedCardEffect("strike", EffectType.DAMAGE, TargetType.SINGLE_ENEMY, 6, 0, 0)),
        result.enemyEffects());

    assertTrue(result.playerEffects().isEmpty());

    // The resolution was recorded by the resolution service.
    assertIterableEquals(List.of(result.resolution()), resolutionService.getResolutions());
  }

  @Test
  void shouldReturnOneCompleteResultForUnifiedCardPlayRequest() {
    CardConfig strike =
        card("strike", 1, TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));
    CardConfig defend = card("defend", 1, TargetType.SELF, new EffectConfig(EffectType.BLOCK, 3));

    CardLibrary cardLibrary = new CardLibrary(List.of(strike, defend));

    BattleDeck battleDeck =
        new BattleDeck(
            new PlayerDeck(
                TestCardService.withCards("strike", "defend"), List.of("strike", "defend")));

    battleDeck.drawOne();

    EnergyComponent energyComponent = new EnergyComponent(3);
    CardPlayService playService = new CardPlayService(cardLibrary, battleDeck, energyComponent);

    CardPlayRequest request = CardPlayRequest.singleEnemy("strike", "enemy-1");

    CardPlayResult result = playService.playCard(request);

    assertTrue(result.success());
    assertEquals(request.target(), result.target());

    assertEquals(result.effectResolution(), result.resolution());
    assertEquals(1, result.energyCost());
    assertEquals(2, energyComponent.getCurrentEnergy());

    // strike was played and not replaced — the hand is now empty.
    assertTrue(result.updatedHand().isEmpty());

    // The draw pile is untouched.
    assertIterableEquals(List.of("defend"), result.updatedDrawPile());

    // strike was moved to the discard pile.
    assertIterableEquals(List.of("strike"), result.updatedDiscardPile());

    // defend is not playable because it was never drawn into the hand.
    assertFalse(playService.canPlay(CardPlayRequest.singleEnemy("defend", "enemy-1")));
  }

  @Test
  void shouldResolveDamageFromPlayerAndEnemyStateViews() {
    CardConfig strike =
        card("strike", 1, TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));
    CardConfig defend = card("defend", 1, TargetType.SELF, new EffectConfig(EffectType.BLOCK, 3));
    CardLibrary cardLibrary = new CardLibrary(List.of(strike, defend));
    BattleDeck battleDeck =
        new BattleDeck(
            new PlayerDeck(
                TestCardService.withCards("strike", "defend"), List.of("strike", "defend")));
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
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(cardLibrary, List.of("strike")));
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
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(cardLibrary, List.of("strike")));
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
    BattleDeck battleDeck =
        new BattleDeck(new PlayerDeck(TestCardService.withCards("strike"), List.of("strike")));
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
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(cardLibrary, List.of("strike")));
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
        new BattleDeck(new PlayerDeck(cardLibrary, List.of("strike"))) {
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
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(cardLibrary, List.of("inner_focus")));
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
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(cardLibrary, List.of("strike")));
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
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(cardLibrary, List.of("strike")));
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
    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(cardLibrary, List.of("strike")));
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

  @Test
  void shouldTrackCooldownPerInstanceNotPerCardId() {
    CardConfig strike =
        card("strike", 1, TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 4));
    CardLibrary cardLibrary = new CardLibrary(List.of(strike));

    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(cardLibrary, List.of("strike", "strike")));
    battleDeck.drawCards(2);

    EnergyComponent energyComponent = new EnergyComponent(3);
    CardEffectResolutionService resolutionService = new CardEffectResolutionService(cardLibrary);
    CardPlayService playService =
        new CardPlayService(cardLibrary, resolutionService, battleDeck, energyComponent);

    CardPlayResult result = playService.playCard("strike");
    assertTrue(result.successful());

    List<CardInstance> remainingHand = battleDeck.getHandInstances();
    List<CardInstance> discarded = battleDeck.getDiscardPileInstances();

    assertEquals(1, remainingHand.size());
    assertEquals(1, discarded.size());

    // Only the copy that was actually played is on cooldown; the other "strike" still sitting in
    // hand must not be affected just because it shares the same card ID.
    assertTrue(playService.isOnCooldown(discarded.get(0)));
    assertFalse(playService.isOnCooldown(remainingHand.get(0)));
  }

  @Test
  void shouldOnlyRetrieveTheSpecificInstanceOnCooldown() {
    // Total effect value 6 -> 2 rounds of cooldown (see CardCooldown.roundsFor).
    CardConfig strike =
        card("strike", 1, TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 6));
    CardLibrary cardLibrary = new CardLibrary(List.of(strike));

    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(cardLibrary, List.of("strike", "strike")));
    battleDeck.drawCards(2);

    EnergyComponent energyComponent = new EnergyComponent(3);
    CardEffectResolutionService resolutionService = new CardEffectResolutionService(cardLibrary);
    CardPlayService playService =
        new CardPlayService(cardLibrary, resolutionService, battleDeck, energyComponent);

    playService.playCard("strike");
    // Discard the remaining copy directly (not through play), so the discard pile ends up with
    // two "strike" copies but only the played one is tracked for cooldown.
    battleDeck.discardCard("strike");
    assertEquals(2, battleDeck.getDiscardPileSize());

    assertTrue(playService.onPlayerRoundStart().isEmpty());
    List<String> secondTick = playService.onPlayerRoundStart();

    // Only the tracked (played) instance comes back; the manually-discarded copy stays put.
    assertEquals(List.of("strike"), secondTick);
    assertEquals(1, battleDeck.getHandSize());
    assertEquals(1, battleDeck.getDiscardPileSize());
  }

  @Test
  void shouldRearrangeHandToExactSelectedInstancesAndChargeOneEnergy() {
    CardConfig strike =
        card("strike", 1, TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 4));
    CardLibrary cardLibrary = new CardLibrary(List.of(strike));

    PlayerDeck playerDeck =
        new PlayerDeck(cardLibrary, List.of("strike", "strike", "strike", "strike", "strike"));
    List<CardInstance> owned = playerDeck.getCards();
    BattleDeck battleDeck = new BattleDeck(playerDeck);
    battleDeck.drawCards(3);

    EnergyComponent energyComponent = new EnergyComponent(3);
    CardPlayService playService =
        new CardPlayService(
            cardLibrary,
            new CardEffectResolutionService(cardLibrary),
            battleDeck,
            energyComponent);

    // Pick two specific instances currently in the draw pile instead of the dealt hand.
    List<CardInstance> chosenHand = List.of(owned.get(3), owned.get(4));
    boolean changed = playService.rearrangeHand(chosenHand);

    assertTrue(changed);
    assertEquals(2, energyComponent.getCurrentEnergy());
    assertIterableEquals(chosenHand, battleDeck.getHandInstances());
  }

  @Test
  void shouldNotChargeEnergyWhenRearrangedHandMatchesCurrentHand() {
    CardConfig strike =
        card("strike", 1, TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 4));
    CardLibrary cardLibrary = new CardLibrary(List.of(strike));

    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(cardLibrary, List.of("strike", "strike")));
    battleDeck.drawCards(2);

    EnergyComponent energyComponent = new EnergyComponent(3);
    CardPlayService playService =
        new CardPlayService(
            cardLibrary,
            new CardEffectResolutionService(cardLibrary),
            battleDeck,
            energyComponent);

    boolean changed = playService.rearrangeHand(battleDeck.getHandInstances());

    assertFalse(changed);
    assertEquals(3, energyComponent.getCurrentEnergy());
  }

  @Test
  void shouldAllowSelectingAnInstanceOnCooldownWithoutMovingItOrItsSiblingCard() {
    CardConfig strike =
        card("strike", 1, TargetType.SINGLE_ENEMY, new EffectConfig(EffectType.DAMAGE, 4));
    CardLibrary cardLibrary = new CardLibrary(List.of(strike));

    BattleDeck battleDeck = new BattleDeck(new PlayerDeck(cardLibrary, List.of("strike", "strike")));
    battleDeck.drawCards(2);

    EnergyComponent energyComponent = new EnergyComponent(3);
    CardPlayService playService =
        new CardPlayService(
            cardLibrary,
            new CardEffectResolutionService(cardLibrary),
            battleDeck,
            energyComponent);

    playService.playCard("strike");
    List<CardInstance> discarded = playService.discardedInstances();
    CardInstance remainingInHand = battleDeck.getHandInstances().get(0);

    // Selecting the on-cooldown copy alongside the one still in hand must not move the cooldown
    // copy (it stays exactly where it was, cooldown untouched) and must not disturb its sibling.
    boolean changed = playService.rearrangeHand(List.of(discarded.get(0), remainingInHand));

    assertFalse(changed);
    assertIterableEquals(discarded, playService.discardedInstances());
    assertIterableEquals(List.of(remainingInHand), battleDeck.getHandInstances());
    // A no-op rearrange must not have spent energy.
    assertEquals(2, energyComponent.getCurrentEnergy());
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
