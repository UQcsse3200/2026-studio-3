package com.csse3200.game.cards.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.effects.CardEffectResolution;
import com.csse3200.game.cards.effects.CardEffectResolutionContext;
import com.csse3200.game.cards.effects.CardEffectResolutionService;
import com.csse3200.game.cards.effects.CardEffectResolver;
import com.csse3200.game.cards.effects.PlayerEffectState;
import com.csse3200.game.cards.effects.TurnEffectStore;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.cards.runtime.CardInstanceFactory;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.cards.runtime.ResolvedCard;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.enemy.EnemyIntent;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.components.player.PlayerIntent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Checkpoint C: production entry points must select a copy, not the first matching definition. */
@ExtendWith(GameExtension.class)
class InstanceCardPlayTest {
  private final CardInstance base = new CardInstance("strike-base", "strike", 0);
  private final CardInstance plus = new CardInstance("strike-plus", "strike", 1);
  private final CardInstance other = new CardInstance("strike-other", "strike", 0);
  private CardLibrary library;
  private CardConfig strike;
  private BattleDeck deck;
  private EnergyComponent energy;
  private CardPlayService service;

  @BeforeEach
  void setUp() {
    library = new CardLibrary(CardConfigLoader.loadCards());
    strike = library.getCard("strike").orElseThrow();
    // Test-only balance data: distinguish both cost and duration from the shipped base card.
    strike.upgrade.cost = 2;
    strike.upgrade.effects =
        new EffectConfig[] {
          new EffectConfig(EffectType.DAMAGE, 12, 0), new EffectConfig(EffectType.POISON, 4, 5)
        };
    deck =
        new BattleDeck(
            PlayerDeck.fromInstances(
                library, List.of(base, plus, other, new CardInstance("spare", "defend", 0))));
    deck.drawCards(3);
    energy = new EnergyComponent(3);
    service = new CardPlayService(library, deck, energy);
  }

  @Test
  void playsSelectedUpgradeAndReportsIdentityCostEffectsAndImmutableSnapshots() {
    var preview = service.resolveInHand(plus.instanceId()).orElseThrow();
    var request = CardPlayRequest.singleEnemy(plus.instanceId(), "enemy");
    assertTrue(service.canPlay(request));
    CardPlayResult result = service.playCard(request);
    assertTrue(result.success());
    assertEquals(plus.instanceId(), result.instanceId());
    assertEquals("strike", result.cardId());
    assertEquals(preview.cost(), result.energyCost());
    assertEquals(1, energy.getCurrentEnergy());
    assertEquals(List.of(12, 4), result.enemyEffects().stream().map(e -> e.value()).toList());
    assertEquals(List.of(0, 5), result.enemyEffects().stream().map(e -> e.duration()).toList());
    assertEquals(List.of(0, 1), result.enemyEffects().stream().map(e -> e.sequence()).toList());
    assertEquals(List.of(base, other), result.updatedHand());
    assertEquals(List.of(plus), result.updatedDiscardPile());
    assertEquals(6, strike.effects[0].value);
    assertEquals(1, strike.cost);
    assertFalse(base.isUpgraded());
    assertThrows(UnsupportedOperationException.class, () -> result.updatedHand().clear());
    service.playCard(CardPlayRequest.singleEnemy(other.instanceId(), "enemy"));
    assertEquals(List.of(base, other), result.updatedHand());
  }

  @Test
  void baseAndUpgradeUseDifferentCostsAndBaseStillDealsSix() {
    energy.setCurrentEnergy(1);
    assertTrue(service.canPlay(base.instanceId()));
    assertFalse(service.canPlay(plus.instanceId()));
    var before = DeckSnapshot.from(deck);
    var denied = service.playCard(CardPlayRequest.singleEnemy(plus.instanceId(), "enemy"));
    assertEquals(CardPlayFailureReason.NOT_ENOUGH_ENERGY, denied.failureReason());
    assertEquals(before, denied.deckSnapshot());
    assertEquals(1, energy.getCurrentEnergy());
    var played = service.playCard(CardPlayRequest.singleEnemy(base.instanceId(), "enemy"));
    assertEquals(6, played.enemyEffects().get(0).value());
    assertEquals(1, played.energyCost());
    assertEquals(List.of(base), deck.getDiscardPile());
    assertTrue(deck.getHand().contains(plus));
  }

  @Test
  void rejectsDefinitionIdUnknownInstanceAndCopyOutsideHandWithoutMutation() {
    var before = DeckSnapshot.from(deck);
    for (String id : List.of("strike", "unknown", "spare")) {
      assertFalse(service.canPlay(id));
      var result = service.playCard(CardPlayRequest.singleEnemy(id, "enemy"));
      assertEquals(id, result.instanceId());
      assertEquals(CardPlayFailureReason.CARD_NOT_IN_HAND, result.failureReason());
      assertEquals(before, result.deckSnapshot());
    }
    assertEquals(3, energy.getCurrentEnergy());
  }

  @Test
  void missingOrInvalidUpgradeAndMissingDefinitionAreNotPlayable() {
    var before = DeckSnapshot.from(deck);
    var request = CardPlayRequest.singleEnemy(plus.instanceId(), "enemy");
    var upgrade = strike.upgrade;
    strike.upgrade = null;
    assertFalse(service.canPlay(request));
    assertEquals(
        CardPlayFailureReason.INVALID_CARD_CONFIG, service.playCard(request).failureReason());
    strike.upgrade = upgrade;
    upgrade.effects[1].duration = 0;
    assertFalse(service.canPlay(request));
    assertEquals(
        CardPlayFailureReason.INVALID_CARD_CONFIG, service.playCard(request).failureReason());
    upgrade.effects[1].duration = 5;
    upgrade.cost = -1;
    assertFalse(service.canPlay(request));
    assertEquals(
        CardPlayFailureReason.INVALID_CARD_CONFIG, service.playCard(request).failureReason());
    var missingLibrary = new CardPlayService(new CardLibrary(), deck, energy);
    assertFalse(missingLibrary.canPlay(request));
    assertEquals(
        CardPlayFailureReason.UNKNOWN_CARD, missingLibrary.playCard(request).failureReason());
    assertEquals(before, DeckSnapshot.from(deck));
    assertEquals(3, energy.getCurrentEnergy());
  }

  @Test
  void mismatchedTargetDoesNotSpendEnergyOrMoveCard() {
    var before = DeckSnapshot.from(deck);
    var request = CardPlayRequest.self(plus.instanceId());
    assertFalse(service.canPlay(request));
    var result = service.playCard(request);
    assertEquals(CardPlayFailureReason.INVALID_TARGET, result.failureReason());
    assertEquals(before, result.deckSnapshot());
    assertEquals(3, energy.getCurrentEnergy());
  }

  @Test
  void unexpectedResolutionFailureRestoresUpgradedCost() {
    CardEffectResolver broken =
        new CardEffectResolver(library) {
          @Override
          public CardEffectResolution resolve(ResolvedCard card, PlayerEffectState state) {
            assertEquals(2, card.cost());
            assertEquals(1, energy.getCurrentEnergy());
            throw new IllegalStateException("resolution failed");
          }
        };
    var resolutions =
        new CardEffectResolutionService(broken, new PlayerEffectState(), new TurnEffectStore());
    var play = new CardPlayService(library, resolutions, deck, energy);
    var before = DeckSnapshot.from(deck);
    assertThrows(
        IllegalStateException.class,
        () -> play.playCard(CardPlayRequest.singleEnemy(plus.instanceId(), "enemy")));
    assertEquals(3, energy.getCurrentEnergy());
    assertEquals(before, DeckSnapshot.from(deck));
    assertTrue(resolutions.getResolutions().isEmpty());
  }

  @Test
  void resolvedEntryPointUsesUpgradedValuesWithCombatModifiers() {
    ResolvedCard resolved = new CardResolver().resolve(strike, plus);
    var result =
        new CardEffectResolver(library).resolve(resolved, new CardEffectResolutionContext(2, 0, 0));
    assertEquals(14, result.enemyEffects().get(0).value());
    assertEquals(5, result.enemyEffects().get(1).duration());
    assertEquals(6, strike.effects[0].value);
  }

  @Test
  void rejectedDeckCommitRestoresUpgradedCostAndLeavesPilesUnchanged() {
    BattleDeck failingDeck =
        new BattleDeck(PlayerDeck.fromInstances(library, List.of(base, plus))) {
          @Override
          public boolean playCard(String instanceId) {
            return false;
          }
        };
    failingDeck.drawCards(2);
    var before = DeckSnapshot.from(failingDeck);
    var play = new CardPlayService(library, failingDeck, energy);
    assertThrows(
        IllegalStateException.class,
        () -> play.playCard(CardPlayRequest.singleEnemy(plus.instanceId(), "enemy")));
    assertEquals(3, energy.getCurrentEnergy());
    assertEquals(before, DeckSnapshot.from(failingDeck));
  }

  @Test
  void activeBattleRejectsUnaffordableUpgradeWithoutDrawingReplacement() {
    Entity enemy = enemy();
    var controller = controller(List.of(enemy));
    controller.start();
    energy.setCurrentEnergy(1);
    var before = DeckSnapshot.from(deck);
    assertFalse(
        controller.submitCardPlayRequest(
            CardPlayRequest.singleEnemy(plus.instanceId(), Integer.toString(enemy.getId())),
            PlayerIntent.ATTACK));
    assertEquals(before, DeckSnapshot.from(deck));
    assertEquals(1, energy.getCurrentEnergy());
    assertEquals(30, enemy.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void activeBattleUsesUpgradedSelfEffectValueAndDuration() {
    CardConfig resurrection = library.getCard("resurrection").orElseThrow();
    var upgrade = new com.csse3200.game.cards.configs.CardUpgradeConfig();
    upgrade.name = "Resurrection+";
    upgrade.description = "Heal 8 for 2 turns";
    upgrade.cost = 2;
    upgrade.rarity = resurrection.rarity;
    upgrade.effects = new EffectConfig[] {new EffectConfig(EffectType.HEAL, 8, 2)};
    resurrection.upgrade = upgrade;
    CardInstance heal = new CardInstance("heal-plus", "resurrection", 1);
    deck = new BattleDeck(PlayerDeck.fromInstances(library, List.of(heal, base)));
    deck.drawOne();
    CombatStatsComponent stats = new CombatStatsComponent(100, 0);
    stats.setHealth(20);
    var controller =
        new BattleController(
            new Entity().addComponent(stats).addComponent(energy),
            List.of(enemy()),
            new CardEffectResolver(library),
            library,
            deck);
    controller.start();
    assertTrue(
        controller.submitCardPlayRequest(
            CardPlayRequest.self(heal.instanceId()), PlayerIntent.OTHER));
    assertEquals(20, stats.getHealth());
    assertEquals(8, stats.getStatusEffect(EffectType.HEAL.name()).getValue());
    assertEquals(2, stats.getStatusEffect(EffectType.HEAL.name()).getDuration());
    assertEquals(1, energy.getCurrentEnergy());
    assertEquals(List.of(heal), deck.getDiscardPile());
    assertEquals(6, resurrection.effects[0].value);
    assertEquals(3, resurrection.effects[0].duration);
  }

  @Test
  void activeBattleAppliesUpgradeOnlyToSelectedEnemyAndPublishesInstanceHand() {
    Entity first = enemy();
    Entity second = enemy();
    var controller = controller(List.of(first, second));
    AtomicReference<List<CardInstance>> hand = new AtomicReference<>();
    controller.addHandChangedListener(hand::set);
    controller.start();
    assertTrue(
        controller.submitCardPlayRequest(
            CardPlayRequest.singleEnemy(plus.instanceId(), Integer.toString(second.getId())),
            PlayerIntent.ATTACK));
    assertEquals(30, first.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(18, second.getComponent(CombatStatsComponent.class).getHealth());
    var poison = second.getComponent(CombatStatsComponent.class).getStatusEffect("poison");
    assertEquals(4, poison.getValue());
    assertEquals(5, poison.getDuration());
    assertEquals(1, energy.getCurrentEnergy());
    assertEquals(List.of(plus), deck.getDiscardPile());
    assertEquals(deck.getHand(), hand.get());
    assertTrue(hand.get().containsAll(List.of(base, other)));
    assertFalse(hand.get().contains(plus));
  }

  @Test
  void activeBattleRejectsUnknownDeadAndMismatchedTargetsBeforeSpending() {
    Entity living = enemy();
    Entity dead = enemy();
    dead.getComponent(CombatStatsComponent.class).setHealth(0);
    var controller = controller(List.of(living, dead));
    controller.start();
    var before = DeckSnapshot.from(deck);
    for (var target :
        List.of(
            CardPlayTarget.singleEnemy("missing"),
            CardPlayTarget.singleEnemy(Integer.toString(dead.getId())),
            CardPlayTarget.self())) {
      assertFalse(
          controller.submitCardPlayRequest(
              new CardPlayRequest(plus.instanceId(), target), PlayerIntent.ATTACK));
      assertEquals(before, DeckSnapshot.from(deck));
      assertEquals(3, energy.getCurrentEnergy());
    }
    assertEquals(30, living.getComponent(CombatStatsComponent.class).getHealth());
  }

  @Test
  void battleUiEventRoutesExactInstanceAndUsesUpgradedDisplayName() {
    Entity enemy = enemy();
    var controller = controller(List.of(enemy));
    var ui =
        new Entity()
            .addComponent(
                new com.csse3200.game.components.battle.BattleActions(
                    controller, mock(com.csse3200.game.GdxGame.class)));
    AtomicReference<String> displayedName = new AtomicReference<>();
    ui.getEvents()
        .addListener("cardPlayed", (String name, String target) -> displayedName.set(name));
    var entities = new com.csse3200.game.entities.EntityService();
    com.csse3200.game.services.ServiceLocator.registerEntityService(entities);
    entities.register(ui);
    controller.start();
    ui.getEvents().trigger("playCard", plus.instanceId(), Integer.toString(enemy.getId()));
    assertEquals("Strike+", displayedName.get());
    assertEquals(List.of(plus), deck.getDiscardPile());
    assertEquals(18, enemy.getComponent(CombatStatsComponent.class).getHealth());
    assertEquals(1, energy.getCurrentEnergy());
    ui.dispose();
  }

  @Test
  void acquisitionCreatesNewIdentityForEachCopyWithoutChangingOfferId() {
    PlayerDeck owned = new PlayerDeck(library);
    CardInstanceFactory factory = new CardInstanceFactory(library);
    owned.addCard(factory.create("strike"));
    owned.addCard(factory.create("strike"));
    assertEquals(2, owned.countByCardId("strike"));
    assertEquals(2, owned.getCards().stream().map(CardInstance::instanceId).distinct().count());
    assertTrue(owned.getCards().stream().noneMatch(CardInstance::isUpgraded));
  }

  private BattleController controller(List<Entity> enemies) {
    Entity player =
        new Entity().addComponent(new CombatStatsComponent(100, 0)).addComponent(energy);
    return new BattleController(player, enemies, new CardEffectResolver(library), library, deck);
  }

  private Entity enemy() {
    EnemyBehaviourComponent behaviour = mock(EnemyBehaviourComponent.class);
    when(behaviour.getCurrentIntent()).thenReturn(EnemyIntent.defend(1));
    when(behaviour.rollIntent()).thenReturn(EnemyIntent.defend(1));
    return new Entity().addComponent(new CombatStatsComponent(30, 0)).addComponent(behaviour);
  }
}
