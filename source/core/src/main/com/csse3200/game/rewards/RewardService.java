package com.csse3200.game.rewards;

import com.csse3200.game.cards.CardAcquisitionPool;
import com.csse3200.game.cards.CardAcquisitionPoolLoader;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.maps.RunState;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class RewardService {
  private final RewardGenerator generator;
  private final CardRewardSelectionGenerator cardRewardGenerator;
  private final CardService cardService;
  private final CardAcquisitionPool acquisitionPool;

  public RewardService() {
    this(new RewardGenerator(), createDefaultCardService(), new Random());
  }

  public RewardService(RewardGenerator generator) {
    this(generator, createDefaultCardService(), new Random());
  }

  /** Creates a reward service with deterministic injectable generation dependencies. */
  public RewardService(RewardGenerator generator, CardService cardService, Random cardRandom) {
    this(generator, cardService, CardAcquisitionPoolLoader.loadDefault(cardService), cardRandom);
  }

  /** Creates a reward service with an explicit shared acquisition policy. */
  public RewardService(
      RewardGenerator generator,
      CardService cardService,
      CardAcquisitionPool acquisitionPool,
      Random cardRandom) {
    this.generator = Objects.requireNonNull(generator, "generator must not be null");
    this.cardService = Objects.requireNonNull(cardService, "cardService must not be null");
    this.acquisitionPool =
        Objects.requireNonNull(acquisitionPool, "acquisitionPool must not be null");
    this.cardRewardGenerator =
        new CardRewardSelectionGenerator(
            acquisitionPool, Objects.requireNonNull(cardRandom, "cardRandom must not be null"));
  }

  /** Generates rewards for a player without a gold bonus. */
  public List<RewardOption> generateRewardOptions() {
    return generateRewardOptions(0f);
  }

  /**
   * Generates mutually exclusive Gold, Item and (when eligible) Card options.
   *
   * @param goldBonusMultiplier current Lucky Coin multiplier
   * @return Gold and Item options, followed by a Card option when the acquisition pool is non-empty
   */
  public List<RewardOption> generateRewardOptions(float goldBonusMultiplier) {
    List<RewardOption> options = new ArrayList<>();
    options.add(generator.generateGoldRewardOption(goldBonusMultiplier));
    options.add(generator.generateItemRewardOption());
    cardRewardGenerator.generate().map(RewardOption::cards).ifPresent(options::add);
    return List.copyOf(options);
  }

  /**
   * Applies one mutually exclusive reward to the persistent run state.
   *
   * @param runState active run receiving the reward
   * @param selected chosen top-level reward
   * @param selectedCardId chosen card ID for CARD rewards; ignored for Gold and Item
   */
  public void claimRunReward(RunState runState, RewardOption selected, String selectedCardId) {
    Objects.requireNonNull(runState, "runState must not be null");
    validateOption(selected);

    switch (selected.type) {
      case GOLD -> {
        var playerState = runState.getOrCreatePlayerState();
        playerState.claimGoldReward(
            selected.goldAmount, playerState.hasOwnedItem(ItemType.LUCKY_COIN));
      }
      case ITEM -> runState.getOrCreatePlayerState().addOwnedItem(selected.itemId);
      case CARD -> {
        if (!selected.cardSelection.contains(selectedCardId)) {
          throw new IllegalArgumentException("selected card is not part of this reward");
        }
        if (!acquisitionPool.isEligible(selectedCardId)) {
          throw new IllegalArgumentException("selected card is not eligible: " + selectedCardId);
        }
        runState.getOrCreatePlayerDeck(cardService).addCard(selectedCardId);
      }
    }
  }

  /**
   * Applies a generated reward to a live player entity.
   *
   * <p>The gold amount has already been finalised by RewardGenerator and must not be multiplied
   * again here.
   */
  public void claimReward(Entity player, RewardOption selected) {
    if (player == null) {
      throw new IllegalArgumentException("player must not be null");
    }
    if (selected == null || selected.type == null) {
      throw new IllegalArgumentException("selected reward must not be null");
    }

    validateOption(selected);
    switch (selected.type) {
      case GOLD -> {
        InventoryComponent inventory = player.getComponent(InventoryComponent.class);

        if (inventory == null) {
          throw new IllegalArgumentException("player must have InventoryComponent");
        }

        inventory.addGold(selected.goldAmount);
      }

      case ITEM -> ItemEffectApplier.applyItemEffect(selected.itemId, player);
      case CARD -> throw new IllegalArgumentException("card rewards require a persistent RunState");
    }
  }

  private static void validateOption(RewardOption selected) {
    if (selected == null || selected.type == null) {
      throw new IllegalArgumentException("selected reward must not be null");
    }
    String error =
        switch (selected.type) {
          case GOLD -> selected.goldAmount < 0 ? "gold reward must not be negative" : null;
          case ITEM -> selected.itemId == null ? "item reward must have an itemId" : null;
          case CARD -> selected.cardSelection == null ? "card reward must have a selection" : null;
        };
    if (error != null) {
      throw new IllegalArgumentException(error);
    }
  }

  private static CardService createDefaultCardService() {
    return new CardLibrary(CardConfigLoader.loadCards());
  }
}
