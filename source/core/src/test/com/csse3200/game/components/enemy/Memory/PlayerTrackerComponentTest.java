package com.csse3200.game.components.enemy.Memory;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.combat.BattlePhase;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.events.listeners.EventListener2;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PlayerTrackerComponentTest {
  private static final String TARGET_ID = "enemy-1";

  private BattleController battleController;
  private CardService cardService;
  private BattleDeck battleDeck;

  private EnemyMemoryComponent enemyMemory;
  private PlayerTrackerComponent tracker;

  private EventListener2<String, String> cardPlayedListener;
  private EventListener2<BattlePhase, BattlePhase> phaseChangeListener;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    battleController = mock(BattleController.class);
    cardService = mock(CardService.class);
    battleDeck = mock(BattleDeck.class);

    enemyMemory = new EnemyMemoryComponent();
    tracker = new PlayerTrackerComponent();

    Entity player = new Entity().addComponent(enemyMemory).addComponent(tracker);

    player.create();
    tracker.connect(battleController, cardService, battleDeck);

    ArgumentCaptor<EventListener2<String, String>> cardPlayedCaptor =
        ArgumentCaptor.forClass(EventListener2.class);
    ArgumentCaptor<EventListener2<BattlePhase, BattlePhase>> phaseChangeCaptor =
        ArgumentCaptor.forClass(EventListener2.class);

    verify(battleController).addCardPlayedListener(cardPlayedCaptor.capture());
    verify(battleController).addPhaseChangeListener(phaseChangeCaptor.capture());

    cardPlayedListener = cardPlayedCaptor.getValue();
    phaseChangeListener = phaseChangeCaptor.getValue();
  }

  @Test
  void shouldRequireEnemyMemoryOnSameEntity() {
    PlayerTrackerComponent trackerWithoutMemory = new PlayerTrackerComponent();

    Entity player = new Entity().addComponent(trackerWithoutMemory);

    IllegalStateException exception = assertThrows(IllegalStateException.class, player::create);

    assertEquals(
        "PlayerTrackerComponent requires EnemyMemoryComponent " + "on the same entity",
        exception.getMessage());
  }

  @Test
  void shouldRejectNullBattleController() {
    PlayerTrackerComponent unconnectedTracker = createUnconnectedTracker();

    assertThrows(
        NullPointerException.class,
        () -> unconnectedTracker.connect(null, cardService, battleDeck));
  }

  @Test
  void shouldRejectNullCardService() {
    PlayerTrackerComponent unconnectedTracker = createUnconnectedTracker();

    assertThrows(
        NullPointerException.class,
        () -> unconnectedTracker.connect(battleController, null, battleDeck));
  }

  @Test
  void shouldRejectNullBattleDeck() {
    PlayerTrackerComponent unconnectedTracker = createUnconnectedTracker();

    assertThrows(
        NullPointerException.class,
        () -> unconnectedTracker.connect(battleController, cardService, null));
  }

  @Test
  void shouldRejectSecondConnection() {
    assertThrows(
        IllegalStateException.class,
        () -> tracker.connect(battleController, cardService, battleDeck));
  }

  @Test
  void shouldRecordAttackCard() {
    playCard(
        "attack-instance",
        "strike",
        createCard(CardType.ATTACK, new EffectConfig(EffectType.DAMAGE, 6)));

    PlayerMemory memory = enemyMemory.snapshot();

    assertAll(
        () -> assertEquals(1, memory.attackCardsPlayed()),
        () -> assertEquals(0, memory.skillCardsPlayed()));
  }

  @Test
  void shouldRecordSkillCard() {
    playCard(
        "skill-instance",
        "quick-step",
        createCard(CardType.SKILL, new EffectConfig(EffectType.ENERGY_GAIN, 1)));

    PlayerMemory memory = enemyMemory.snapshot();

    assertAll(
        () -> assertEquals(0, memory.attackCardsPlayed()),
        () -> assertEquals(1, memory.skillCardsPlayed()));
  }

  @Test
  void shouldCountOtherCardTypesWithoutClassifyingThem() {
    playCard(
        "power-instance",
        "strength-power",
        createCard(CardType.POWER, new EffectConfig(EffectType.STRENGTH, 2)));

    endPlayerTurn();

    PlayerMemory memory = enemyMemory.snapshot();

    assertAll(
        () -> assertEquals(0, memory.attackCardsPlayed()),
        () -> assertEquals(0, memory.skillCardsPlayed()),
        () -> assertEquals(1, memory.cardsPlayedLastTurn()));
  }

  @Test
  void shouldDetectPositiveBlockEffect() {
    playCard(
        "block-instance",
        "defend",
        createCard(CardType.SKILL, new EffectConfig(EffectType.BLOCK, 5)));

    endPlayerTurn();

    PlayerMemory memory = enemyMemory.snapshot();

    assertAll(
        () -> assertEquals(1, memory.skillCardsPlayed()),
        () -> assertEquals(0, memory.consecutiveTurnsWithoutBlock()));
  }

  @Test
  void shouldFindBlockAmongMultipleEffects() {
    playCard(
        "multi-effect-instance",
        "attack-and-block",
        createCard(
            CardType.ATTACK,
            new EffectConfig(EffectType.DAMAGE, 5),
            new EffectConfig(EffectType.BLOCK, 3)));

    endPlayerTurn();

    PlayerMemory memory = enemyMemory.snapshot();

    assertAll(
        () -> assertEquals(1, memory.attackCardsPlayed()),
        () -> assertEquals(1, memory.cardsPlayedLastTurn()),
        () -> assertEquals(0, memory.consecutiveTurnsWithoutBlock()));
  }

  @Test
  void shouldNotTreatZeroBlockAsDefence() {
    playCard(
        "zero-block-instance",
        "zero-block",
        createCard(CardType.SKILL, new EffectConfig(EffectType.BLOCK, 0)));

    endPlayerTurn();

    assertEquals(1, enemyMemory.snapshot().consecutiveTurnsWithoutBlock());
  }

  @Test
  void shouldNotTreatNegativeBlockAsDefence() {
    playCard(
        "negative-block-instance",
        "negative-block",
        createCard(CardType.SKILL, new EffectConfig(EffectType.BLOCK, -2)));

    endPlayerTurn();

    assertEquals(1, enemyMemory.snapshot().consecutiveTurnsWithoutBlock());
  }

  @Test
  void shouldSafelyIgnoreNullEffects() {
    CardConfig config = createCard(CardType.SKILL);
    config.effects = new EffectConfig[] {null, new EffectConfig(EffectType.HEAL, 2)};

    playCard("null-effect-instance", "healing-skill", config);

    endPlayerTurn();

    PlayerMemory memory = enemyMemory.snapshot();

    assertAll(
        () -> assertEquals(1, memory.skillCardsPlayed()),
        () -> assertEquals(1, memory.consecutiveTurnsWithoutBlock()));
  }

  @Test
  void shouldIgnoreNullBlankAndUnknownInstanceIds() {
    cardPlayedListener.handle(null, TARGET_ID);
    cardPlayedListener.handle("", TARGET_ID);
    cardPlayedListener.handle("   ", TARGET_ID);

    when(battleDeck.getAllInstances()).thenReturn(List.of());

    cardPlayedListener.handle("missing-instance", TARGET_ID);

    assertEquals(PlayerMemory.empty(), enemyMemory.snapshot());
  }

  @Test
  void shouldIgnoreUnknownCardDefinition() {
    CardInstance instance =
        new CardInstance("unknown-instance", "unknown-card", CardInstance.BASE_LEVEL);

    when(battleDeck.getAllInstances()).thenReturn(List.of(instance));
    when(cardService.getCard("unknown-card")).thenReturn(Optional.empty());

    cardPlayedListener.handle("unknown-instance", TARGET_ID);

    assertEquals(PlayerMemory.empty(), enemyMemory.snapshot());
  }

  @Test
  void shouldIgnoreCardWithNullType() {
    CardConfig config = new CardConfig();
    config.type = null;
    config.effects = new EffectConfig[0];

    playCard("invalid-instance", "invalid-card", config);

    assertEquals(PlayerMemory.empty(), enemyMemory.snapshot());
  }

  @Test
  void shouldNotSettleDuringCardResolution() {
    playCard(
        "attack-instance",
        "strike",
        createCard(CardType.ATTACK, new EffectConfig(EffectType.DAMAGE, 6)));

    phaseChangeListener.handle(BattlePhase.PLAYER_TURN, BattlePhase.CARD_RESOLVING);

    phaseChangeListener.handle(BattlePhase.CARD_RESOLVING, BattlePhase.PLAYER_TURN);

    PlayerMemory memory = enemyMemory.snapshot();

    assertAll(
        () -> assertEquals(1, memory.attackCardsPlayed()),
        () -> assertEquals(0, memory.cardsPlayedLastTurn()),
        () -> assertEquals(0, memory.consecutiveTurnsWithoutBlock()));
  }

  @Test
  void shouldSettleWhenPlayerTurnEnds() {
    playCard(
        "first-instance",
        "strike",
        createCard(CardType.ATTACK, new EffectConfig(EffectType.DAMAGE, 6)));

    playCard(
        "second-instance",
        "quick-step",
        createCard(CardType.SKILL, new EffectConfig(EffectType.ENERGY_GAIN, 1)));

    endPlayerTurn();

    PlayerMemory memory = enemyMemory.snapshot();

    assertAll(
        () -> assertEquals(1, memory.attackCardsPlayed()),
        () -> assertEquals(1, memory.skillCardsPlayed()),
        () -> assertEquals(2, memory.cardsPlayedLastTurn()),
        () -> assertEquals(1, memory.consecutiveTurnsWithoutBlock()));
  }

  @Test
  void shouldIgnoreUnrelatedPhaseChanges() {
    phaseChangeListener.handle(BattlePhase.SETUP, BattlePhase.PLAYER_TURN);

    phaseChangeListener.handle(BattlePhase.PLAYER_END, BattlePhase.ENEMY_TURN);

    assertEquals(PlayerMemory.empty(), enemyMemory.snapshot());
  }

  private PlayerTrackerComponent createUnconnectedTracker() {
    EnemyMemoryComponent separateMemory = new EnemyMemoryComponent();
    PlayerTrackerComponent separateTracker = new PlayerTrackerComponent();

    Entity player = new Entity().addComponent(separateMemory).addComponent(separateTracker);

    player.create();
    return separateTracker;
  }

  private void playCard(String instanceId, String cardId, CardConfig config) {
    CardInstance instance = new CardInstance(instanceId, cardId, CardInstance.BASE_LEVEL);

    when(battleDeck.getAllInstances()).thenReturn(List.of(instance));
    when(cardService.getCard(cardId)).thenReturn(Optional.of(config));

    cardPlayedListener.handle(instanceId, TARGET_ID);
  }

  private void endPlayerTurn() {
    phaseChangeListener.handle(BattlePhase.PLAYER_TURN, BattlePhase.PLAYER_END);
  }

  private CardConfig createCard(CardType type, EffectConfig... effects) {
    CardConfig config = new CardConfig();
    config.type = type;
    config.effects = effects;
    return config;
  }
}
