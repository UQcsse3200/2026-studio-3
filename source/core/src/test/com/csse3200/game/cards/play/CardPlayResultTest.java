package com.csse3200.game.cards.play;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.cards.effects.CardEffectResolution;
import java.util.List;
import org.junit.jupiter.api.Test;

class CardPlayResultTest {

  private static final String CARD_ID = "strike";
  private static final int ENERGY_COST = 1;

  @Test
  void shouldCreateSuccessfulResult() {
    CardEffectResolution resolution = createResolution();
    CardPlayTarget target = CardPlayTarget.singleEnemy(CARD_ID);
    DeckSnapshot snapshot =
        new DeckSnapshot(List.of("defend"), List.of("block"), List.of("strike"));

    CardPlayResult result =
        CardPlayResult.success(CARD_ID, target, ENERGY_COST, resolution, snapshot);

    assertEquals(CARD_ID, result.cardId());
    assertEquals(target, result.target());
    assertTrue(result.success());
    assertEquals(ENERGY_COST, result.energyCost());
    assertEquals(resolution, result.effectResolution());
    assertEquals(resolution, result.resolution());
    assertEquals(CardPlayFailureReason.NONE, result.failureReason());
    assertEquals(snapshot, result.deckSnapshot());
  }

  @Test
  void shouldCreateFailedResult() {
    CardPlayTarget target = CardPlayTarget.singleEnemy(CARD_ID);
    DeckSnapshot snapshot = new DeckSnapshot(List.of("strike"), List.of("defend"), List.of());

    CardPlayResult result =
        CardPlayResult.failure(
            CARD_ID, target, ENERGY_COST, CardPlayFailureReason.NOT_ENOUGH_ENERGY, snapshot);

    assertEquals(CARD_ID, result.cardId());
    assertEquals(target, result.target());
    assertFalse(result.success());
    assertEquals(ENERGY_COST, result.energyCost());
    assertNull(result.effectResolution());
    assertEquals(CardPlayFailureReason.NOT_ENOUGH_ENERGY, result.failureReason());
    assertEquals(snapshot, result.deckSnapshot());
  }

  @Test
  void shouldRejectNullCardId() {
    CardEffectResolution resolution = createResolution();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardPlayResult(
                null,
                null,
                true,
                ENERGY_COST,
                resolution,
                CardPlayFailureReason.NONE,
                DeckSnapshot.empty()));
  }

  @Test
  void shouldRejectBlankCardId() {
    CardEffectResolution resolution = createResolution();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardPlayResult(
                " ",
                null,
                true,
                ENERGY_COST,
                resolution,
                CardPlayFailureReason.NONE,
                DeckSnapshot.empty()));
  }

  @Test
  void shouldRejectNegativeEnergyCost() {
    CardEffectResolution resolution = createResolution();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardPlayResult(
                CARD_ID,
                null,
                true,
                -1,
                resolution,
                CardPlayFailureReason.NONE,
                DeckSnapshot.empty()));
  }

  @Test
  void shouldRejectNullFailureReason() {
    CardEffectResolution resolution = createResolution();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardPlayResult(
                CARD_ID, null, true, ENERGY_COST, resolution, null, DeckSnapshot.empty()));
  }

  @Test
  void shouldRejectNullDeckSnapshot() {
    CardEffectResolution resolution = createResolution();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardPlayResult(
                CARD_ID, null, true, ENERGY_COST, resolution, CardPlayFailureReason.NONE, null));
  }

  @Test
  void shouldRejectSuccessfulResultWithFailureReason() {
    CardEffectResolution resolution = createResolution();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardPlayResult(
                CARD_ID,
                null,
                true,
                ENERGY_COST,
                resolution,
                CardPlayFailureReason.NOT_ENOUGH_ENERGY,
                DeckSnapshot.empty()));
  }

  @Test
  void shouldRejectSuccessfulResultWithoutResolution() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardPlayResult(
                CARD_ID,
                null,
                true,
                ENERGY_COST,
                null,
                CardPlayFailureReason.NONE,
                DeckSnapshot.empty()));
  }

  @Test
  void shouldRejectFailedResultWithoutFailureReason() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardPlayResult(
                CARD_ID,
                null,
                false,
                ENERGY_COST,
                null,
                CardPlayFailureReason.NONE,
                DeckSnapshot.empty()));
  }

  @Test
  void shouldRejectFailedResultWithResolution() {
    CardEffectResolution resolution = createResolution();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardPlayResult(
                CARD_ID,
                null,
                false,
                ENERGY_COST,
                resolution,
                CardPlayFailureReason.NOT_ENOUGH_ENERGY,
                DeckSnapshot.empty()));
  }

  @Test
  void shouldReturnEmptyEffectListsWhenResultHasNoResolution() {
    CardPlayResult result =
        CardPlayResult.failure(CARD_ID, ENERGY_COST, CardPlayFailureReason.NOT_ENOUGH_ENERGY);

    assertTrue(result.enemyEffects().isEmpty());
    assertTrue(result.playerEffects().isEmpty());
  }

  @Test
  void shouldReturnEnemyEffectsFromResolution() {
    CardEffectResolution resolution = createResolution();

    CardPlayResult result = CardPlayResult.success(CARD_ID, ENERGY_COST, resolution);

    assertEquals(resolution.enemyEffects(), result.enemyEffects());
  }

  @Test
  void shouldReturnPlayerEffectsFromResolution() {
    CardEffectResolution resolution = createResolution();

    CardPlayResult result = CardPlayResult.success(CARD_ID, ENERGY_COST, resolution);

    assertEquals(resolution.playerEffects(), result.playerEffects());
  }

  @Test
  void shouldReturnDeckSnapshots() {
    DeckSnapshot snapshot =
        new DeckSnapshot(List.of("defend"), List.of("strike"), List.of("block"));

    CardPlayResult result =
        CardPlayResult.success(CARD_ID, null, ENERGY_COST, createResolution(), snapshot);

    assertEquals(List.of("defend"), result.updatedHand());
    assertEquals(List.of("strike"), result.updatedDrawPile());
    assertEquals(List.of("block"), result.updatedDiscardPile());
  }

  @Test
  void shouldCreateBackwardsCompatibleSuccessfulResult() {
    CardEffectResolution resolution = createResolution();

    CardPlayResult result =
        new CardPlayResult(CARD_ID, true, ENERGY_COST, resolution, CardPlayFailureReason.NONE);

    assertEquals(CARD_ID, result.cardId());
    assertTrue(result.success());
    assertEquals(ENERGY_COST, result.energyCost());
    assertEquals(resolution, result.resolution());
    assertEquals(CardPlayFailureReason.NONE, result.failureReason());
    assertEquals(DeckSnapshot.empty(), result.deckSnapshot());
  }

  @Test
  void shouldCreateBackwardsCompatibleFailedResult() {
    CardPlayResult result =
        new CardPlayResult(
            CARD_ID, false, ENERGY_COST, null, CardPlayFailureReason.CARD_NOT_IN_HAND);

    assertEquals(CARD_ID, result.cardId());
    assertFalse(result.success());
    assertNull(result.resolution());
    assertEquals(CardPlayFailureReason.CARD_NOT_IN_HAND, result.failureReason());
    assertEquals(DeckSnapshot.empty(), result.deckSnapshot());
  }

  private CardEffectResolution createResolution() {
    return new CardEffectResolution("test", List.of());
  }
}
