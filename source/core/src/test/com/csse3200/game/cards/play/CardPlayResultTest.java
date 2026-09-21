package com.csse3200.game.cards.play;

import static org.junit.jupiter.api.Assertions.*;

import com.csse3200.game.cards.effects.CardEffectResolution;
import com.csse3200.game.cards.runtime.CardInstance;
import java.util.List;
import org.junit.jupiter.api.Test;

class CardPlayResultTest {

  private static final String INSTANCE_ID = "instance-1";
  private static final String CARD_ID = "strike";
  private static final int ENERGY_COST = 1;

  @Test
  void shouldCreateSuccessfulResult() {
    CardEffectResolution resolution = createResolution();
    CardPlayTarget target = CardPlayTarget.singleEnemy(CARD_ID);

    DeckSnapshot snapshot =
        new DeckSnapshot(
            List.of(new CardInstance("instance-2", "defend", CardInstance.BASE_LEVEL)),
            List.of(new CardInstance("instance-3", "block", CardInstance.BASE_LEVEL)),
            List.of(new CardInstance(INSTANCE_ID, CARD_ID, CardInstance.BASE_LEVEL)));

    CardPlayResult result =
        CardPlayResult.success(INSTANCE_ID, CARD_ID, target, ENERGY_COST, resolution, snapshot);

    assertEquals(INSTANCE_ID, result.instanceId());
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

    DeckSnapshot snapshot =
        new DeckSnapshot(
            List.of(new CardInstance(INSTANCE_ID, CARD_ID, CardInstance.BASE_LEVEL)),
            List.of(new CardInstance("instance-2", "defend", CardInstance.BASE_LEVEL)),
            List.of());

    CardPlayResult result =
        CardPlayResult.failure(
            INSTANCE_ID,
            CARD_ID,
            target,
            ENERGY_COST,
            CardPlayFailureReason.NOT_ENOUGH_ENERGY,
            snapshot);

    assertEquals(INSTANCE_ID, result.instanceId());
    assertEquals(CARD_ID, result.cardId());
    assertEquals(target, result.target());
    assertFalse(result.success());
    assertEquals(ENERGY_COST, result.energyCost());
    assertNull(result.effectResolution());
    assertEquals(CardPlayFailureReason.NOT_ENOUGH_ENERGY, result.failureReason());
    assertEquals(snapshot, result.deckSnapshot());
  }

  @Test
  void shouldRejectNullInstanceId() {
    CardEffectResolution resolution = createResolution();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardPlayResult(
                null,
                CARD_ID,
                null,
                true,
                ENERGY_COST,
                resolution,
                CardPlayFailureReason.NONE,
                DeckSnapshot.empty()));
  }

  @Test
  void shouldRejectBlankInstanceId() {
    CardEffectResolution resolution = createResolution();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardPlayResult(
                " ",
                CARD_ID,
                null,
                true,
                ENERGY_COST,
                resolution,
                CardPlayFailureReason.NONE,
                DeckSnapshot.empty()));
  }

  @Test
  void shouldRejectSuccessfulResultWithNullCardId() {
    CardEffectResolution resolution = createResolution();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardPlayResult(
                INSTANCE_ID,
                null,
                null,
                true,
                ENERGY_COST,
                resolution,
                CardPlayFailureReason.NONE,
                DeckSnapshot.empty()));
  }

  @Test
  void shouldRejectSuccessfulResultWithBlankCardId() {
    CardEffectResolution resolution = createResolution();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardPlayResult(
                INSTANCE_ID,
                " ",
                null,
                true,
                ENERGY_COST,
                resolution,
                CardPlayFailureReason.NONE,
                DeckSnapshot.empty()));
  }

  @Test
  void shouldAllowFailedResultWithNullCardId() {
    CardPlayResult result =
        new CardPlayResult(
            INSTANCE_ID,
            null,
            null,
            false,
            ENERGY_COST,
            null,
            CardPlayFailureReason.NOT_ENOUGH_ENERGY,
            DeckSnapshot.empty());

    assertEquals(INSTANCE_ID, result.instanceId());
    assertNull(result.cardId());
    assertFalse(result.success());
  }

  @Test
  void shouldRejectNegativeEnergyCost() {
    CardEffectResolution resolution = createResolution();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardPlayResult(
                INSTANCE_ID,
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
                INSTANCE_ID,
                CARD_ID,
                null,
                true,
                ENERGY_COST,
                resolution,
                null,
                DeckSnapshot.empty()));
  }

  @Test
  void shouldRejectNullDeckSnapshot() {
    CardEffectResolution resolution = createResolution();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardPlayResult(
                INSTANCE_ID,
                CARD_ID,
                null,
                true,
                ENERGY_COST,
                resolution,
                CardPlayFailureReason.NONE,
                null));
  }

  @Test
  void shouldRejectSuccessfulResultWithFailureReason() {
    CardEffectResolution resolution = createResolution();

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new CardPlayResult(
                INSTANCE_ID,
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
                INSTANCE_ID,
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
                INSTANCE_ID,
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
                INSTANCE_ID,
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
        CardPlayResult.failure(
            INSTANCE_ID,
            CARD_ID,
            null,
            ENERGY_COST,
            CardPlayFailureReason.NOT_ENOUGH_ENERGY,
            DeckSnapshot.empty());

    assertTrue(result.enemyEffects().isEmpty());
    assertTrue(result.playerEffects().isEmpty());
  }

  @Test
  void shouldReturnEnemyEffectsFromResolution() {
    CardEffectResolution resolution = createResolution();

    CardPlayResult result =
        CardPlayResult.success(
            INSTANCE_ID, CARD_ID, null, ENERGY_COST, resolution, DeckSnapshot.empty());

    assertEquals(resolution.enemyEffects(), result.enemyEffects());
  }

  @Test
  void shouldReturnPlayerEffectsFromResolution() {
    CardEffectResolution resolution = createResolution();

    CardPlayResult result =
        CardPlayResult.success(
            INSTANCE_ID, CARD_ID, null, ENERGY_COST, resolution, DeckSnapshot.empty());

    assertEquals(resolution.playerEffects(), result.playerEffects());
  }

  @Test
  void shouldReturnDeckSnapshots() {
    CardInstance handCard = new CardInstance("instance-2", "defend", CardInstance.BASE_LEVEL);
    CardInstance drawCard = new CardInstance("instance-3", "strike", CardInstance.BASE_LEVEL);
    CardInstance discardCard = new CardInstance("instance-4", "block", CardInstance.BASE_LEVEL);

    DeckSnapshot snapshot =
        new DeckSnapshot(List.of(handCard), List.of(drawCard), List.of(discardCard));

    CardPlayResult result =
        CardPlayResult.success(
            INSTANCE_ID, CARD_ID, null, ENERGY_COST, createResolution(), snapshot);

    assertEquals(List.of(handCard), result.updatedHand());
    assertEquals(List.of(drawCard), result.updatedDrawPile());
    assertEquals(List.of(discardCard), result.updatedDiscardPile());
  }

  @Test
  void shouldCreateBackwardsCompatibleSuccessfulResult() {
    CardEffectResolution resolution = createResolution();

    CardPlayResult result =
        new CardPlayResult(
            INSTANCE_ID,
            CARD_ID,
            null,
            true,
            ENERGY_COST,
            resolution,
            CardPlayFailureReason.NONE,
            DeckSnapshot.empty());

    assertEquals(INSTANCE_ID, result.instanceId());
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
            INSTANCE_ID,
            CARD_ID,
            null,
            false,
            ENERGY_COST,
            null,
            CardPlayFailureReason.CARD_NOT_IN_HAND,
            DeckSnapshot.empty());

    assertEquals(INSTANCE_ID, result.instanceId());
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
