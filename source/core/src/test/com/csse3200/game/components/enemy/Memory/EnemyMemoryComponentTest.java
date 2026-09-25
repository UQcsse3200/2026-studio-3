package com.csse3200.game.components.enemy;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
import com.csse3200.game.events.listeners.EventListener2;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EnemyMemoryComponentTest {
  private static final String TARGET_ID = "enemy-1";

  private BattleController battleController;
  private CardService cardService;
  private BattleDeck battleDeck;
  private EnemyMemoryComponent component;

  private EventListener2<String, String> cardPlayedListener;
  private EventListener2<BattlePhase, BattlePhase> phaseChangeListener;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    battleController = mock(BattleController.class);
    cardService = mock(CardService.class);
    battleDeck = mock(BattleDeck.class);

    component = new EnemyMemoryComponent(battleController, cardService, battleDeck);
    component.create();

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
  void shouldRejectNullDependencies() {
    assertAll(
        () ->
            assertThrows(
                NullPointerException.class,
                () -> new EnemyMemoryComponent(null, cardService, battleDeck)),
        () ->
            assertThrows(
                NullPointerException.class,
                () -> new EnemyMemoryComponent(battleController, null, battleDeck)),
        () ->
            assertThrows(
                NullPointerException.class,
                () -> new EnemyMemoryComponent(battleController, cardService, null)));
  }

  @Test
  void shouldStartWithEmptyMemory() {
    assertEquals(PlayerMemory.empty(), component.snapshot());
  }

  @Test
  void shouldCountAttackAndSkillCards() {
    playCard(
        "attack-instance",
        "strike",
        createCard(CardType.ATTACK, new EffectConfig(EffectType.DAMAGE, 6)));

    playCard(
        "skill-instance",
        "defend",
        createCard(CardType.SKILL, new EffectConfig(EffectType.BLOCK, 5)));

    PlayerMemory memory = component.snapshot();

    assertAll(
        () -> assertEquals(1, memory.attackCardsPlayed()),
        () -> assertEquals(1, memory.skillCardsPlayed()),
        () -> assertEquals(0, memory.cardsPlayedLastTurn()));
  }

  @Test
  void shouldCountOtherCardTypesWithoutClassifyingThemAsAttackOrSkill() {
    playCard(
        "power-instance",
        "strength-power",
        createCard(CardType.POWER, new EffectConfig(EffectType.STRENGTH, 2)));

    endPlayerTurn();

    PlayerMemory memory = component.snapshot();

    assertAll(
        () -> assertEquals(0, memory.attackCardsPlayed()),
        () -> assertEquals(0, memory.skillCardsPlayed()),
        () -> assertEquals(1, memory.cardsPlayedLastTurn()));
  }

  @Test
  void shouldIgnoreNullBlankAndUnknownCardInstances() {
    cardPlayedListener.handle(null, TARGET_ID);
    cardPlayedListener.handle("", TARGET_ID);
    cardPlayedListener.handle("   ", TARGET_ID);

    when(battleDeck.getAllInstances()).thenReturn(List.of());
    cardPlayedListener.handle("missing-instance", TARGET_ID);

    PlayerMemory memory = component.snapshot();

    assertEquals(PlayerMemory.empty(), memory);
  }

  @Test
  void shouldIgnoreCardInstanceWithUnknownDefinition() {
    CardInstance instance =
        new CardInstance("unknown-definition-instance", "unknown-card", CardInstance.BASE_LEVEL);

    when(battleDeck.getAllInstances()).thenReturn(List.of(instance));
    when(cardService.getCard("unknown-card")).thenReturn(Optional.empty());

    cardPlayedListener.handle("unknown-definition-instance", TARGET_ID);

    assertEquals(PlayerMemory.empty(), component.snapshot());
  }

  @Test
  void shouldNotSettleTurnDuringCardResolution() {
    playCard(
        "attack-instance",
        "strike",
        createCard(CardType.ATTACK, new EffectConfig(EffectType.DAMAGE, 6)));

    phaseChangeListener.handle(BattlePhase.PLAYER_TURN, BattlePhase.CARD_RESOLVING);
    phaseChangeListener.handle(BattlePhase.CARD_RESOLVING, BattlePhase.PLAYER_TURN);

    PlayerMemory memory = component.snapshot();

    assertAll(
        () -> assertEquals(1, memory.attackCardsPlayed()),
        () -> assertEquals(0, memory.cardsPlayedLastTurn()),
        () -> assertEquals(0, memory.consecutiveTurnsWithoutBlock()));
  }

  @Test
  void shouldSettleMemoryWhenPlayerTurnEnds() {
    playCard(
        "first-attack-instance",
        "strike",
        createCard(CardType.ATTACK, new EffectConfig(EffectType.DAMAGE, 6)));

    playCard(
        "second-attack-instance",
        "heavy-strike",
        createCard(CardType.ATTACK, new EffectConfig(EffectType.DAMAGE, 10)));

    endPlayerTurn();

    PlayerMemory memory = component.snapshot();

    assertAll(
        () -> assertEquals(2, memory.attackCardsPlayed()),
        () -> assertEquals(2, memory.cardsPlayedLastTurn()),
        () -> assertEquals(1, memory.consecutiveTurnsWithoutBlock()));
  }

  @Test
  void shouldReplaceCardsPlayedLastTurnAfterEachCompletedTurn() {
    playCard(
        "first-instance",
        "strike",
        createCard(CardType.ATTACK, new EffectConfig(EffectType.DAMAGE, 6)));
    endPlayerTurn();

    playCard(
        "second-instance",
        "defend",
        createCard(CardType.SKILL, new EffectConfig(EffectType.BLOCK, 5)));
    playCard(
        "third-instance",
        "quick-step",
        createCard(CardType.SKILL, new EffectConfig(EffectType.ENERGY_GAIN, 1)));
    endPlayerTurn();

    PlayerMemory memory = component.snapshot();

    assertAll(
        () -> assertEquals(1, memory.attackCardsPlayed()),
        () -> assertEquals(2, memory.skillCardsPlayed()),
        () -> assertEquals(2, memory.cardsPlayedLastTurn()));
  }

  @Test
  void shouldIncreaseNoBlockStreakAfterEachUndefendedTurn() {
    endPlayerTurn();
    endPlayerTurn();
    endPlayerTurn();

    PlayerMemory memory = component.snapshot();

    assertAll(
        () -> assertEquals(3, memory.consecutiveTurnsWithoutBlock()),
        () -> assertEquals(0, memory.cardsPlayedLastTurn()));
  }

  @Test
  void shouldResetNoBlockStreakAfterPositiveBlockCard() {
    endPlayerTurn();
    endPlayerTurn();

    playCard(
        "block-instance",
        "defend",
        createCard(CardType.SKILL, new EffectConfig(EffectType.BLOCK, 5)));
    endPlayerTurn();

    PlayerMemory memory = component.snapshot();

    assertAll(
        () -> assertEquals(0, memory.consecutiveTurnsWithoutBlock()),
        () -> assertEquals(1, memory.skillCardsPlayed()),
        () -> assertEquals(1, memory.cardsPlayedLastTurn()));
  }

  @Test
  void shouldNotTreatZeroOrNegativeBlockAsDefence() {
    playCard(
        "zero-block-instance",
        "zero-block",
        createCard(CardType.SKILL, new EffectConfig(EffectType.BLOCK, 0)));
    endPlayerTurn();

    playCard(
        "negative-block-instance",
        "negative-block",
        createCard(CardType.SKILL, new EffectConfig(EffectType.BLOCK, -1)));
    endPlayerTurn();

    PlayerMemory memory = component.snapshot();

    assertEquals(2, memory.consecutiveTurnsWithoutBlock());
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

    PlayerMemory memory = component.snapshot();

    assertAll(
        () -> assertEquals(1, memory.attackCardsPlayed()),
        () -> assertEquals(0, memory.consecutiveTurnsWithoutBlock()),
        () -> assertEquals(1, memory.cardsPlayedLastTurn()));
  }

  @Test
  void shouldSafelyIgnoreNullEffects() {
    CardConfig config = createCard(CardType.SKILL);
    config.effects = new EffectConfig[] {null, new EffectConfig(EffectType.HEAL, 2)};

    playCard("null-effect-instance", "healing-skill", config);
    endPlayerTurn();

    PlayerMemory memory = component.snapshot();

    assertAll(
        () -> assertEquals(1, memory.skillCardsPlayed()),
        () -> assertEquals(1, memory.consecutiveTurnsWithoutBlock()));
  }

  @Test
  void shouldReturnIndependentImmutableSnapshots() {
    PlayerMemory beforeCardPlayed = component.snapshot();

    playCard(
        "attack-instance",
        "strike",
        createCard(CardType.ATTACK, new EffectConfig(EffectType.DAMAGE, 6)));

    PlayerMemory afterCardPlayed = component.snapshot();

    assertAll(
        () -> assertNotSame(beforeCardPlayed, afterCardPlayed),
        () -> assertEquals(0, beforeCardPlayed.attackCardsPlayed()),
        () -> assertEquals(1, afterCardPlayed.attackCardsPlayed()));
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
