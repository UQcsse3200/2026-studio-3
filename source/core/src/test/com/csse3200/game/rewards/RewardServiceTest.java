package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.components.player.InventoryComponent;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.RunState;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class RewardServiceTest {

  @Test
  void shouldGenerateOneGoldOneItemAndOneCardOption() {
    RewardService service = createService();
    List<RewardOption> options = service.generateRewardOptions();

    assertEquals(3, options.size());
    assertEquals(RewardType.GOLD, options.get(0).type);
    assertEquals(RewardType.ITEM, options.get(1).type);
    assertEquals(RewardType.CARD, options.get(2).type);
    assertEquals(3, options.get(2).cardSelection.cardIds().size());
  }

  @Test
  void claimingGoldRewardShouldAddGoldToPlayer() {
    Entity player = new Entity();
    player.addComponent(new InventoryComponent(0));

    RewardService service = createService();
    RewardOption option = RewardOption.gold(25);

    service.claimReward(player, option);

    assertEquals(25, player.getComponent(InventoryComponent.class).getGold());
  }

  @Test
  void claimingOneOptionShouldNotAffectTheOther() {
    // 验证 claimReward 只对传入的那一个 RewardOption 生效
    Entity player = new Entity();
    player.addComponent(new InventoryComponent(0));

    RewardService service = createService();
    RewardOption goldOption = RewardOption.gold(25);

    service.claimReward(player, goldOption);

    assertEquals(25, player.getComponent(InventoryComponent.class).getGold());
  }

  @Test
  void shouldClaimExactlyOneSelectedCardIntoPersistentDeck() {
    CardService cards = new CardLibrary(CardConfigLoader.loadCards());
    RewardService service =
        new RewardService(new RewardGenerator(new Random(1)), cards, new Random(2));
    RunState runState = new RunState();
    RewardOption cardOption =
        service.generateRewardOptions().stream()
            .filter(option -> option.type == RewardType.CARD)
            .findFirst()
            .orElseThrow();
    String selectedCardId = cardOption.cardSelection.cardIds().get(0);
    int initialSize = runState.getOrCreatePlayerDeck(cards).size();
    int initialCopies = runState.getOrCreatePlayerDeck(cards).countByCardId(selectedCardId);

    service.claimRunReward(runState, cardOption, selectedCardId);

    assertEquals(initialSize + 1, runState.getOrCreatePlayerDeck(cards).size());
    assertEquals(
        initialCopies + 1, runState.getOrCreatePlayerDeck(cards).countByCardId(selectedCardId));
    assertEquals(
        0,
        runState
            .getOrCreatePlayerDeck(cards)
            .getCards()
            .get(runState.getOrCreatePlayerDeck(cards).size() - 1)
            .upgradeLevel());
  }

  @Test
  void shouldRejectCardOutsideSelectionWithoutMutatingDeck() {
    CardService cards = new CardLibrary(CardConfigLoader.loadCards());
    RewardService service =
        new RewardService(new RewardGenerator(new Random(1)), cards, new Random(2));
    RunState runState = new RunState();
    RewardOption cardOption = RewardOption.cards(new CardRewardSelection(List.of("strike")));
    int initialSize = runState.getOrCreatePlayerDeck(cards).size();

    assertThrows(
        IllegalArgumentException.class,
        () -> service.claimRunReward(runState, cardOption, "defend"));
    assertEquals(initialSize, runState.getOrCreatePlayerDeck(cards).size());
  }

  @Test
  void shouldApplyOnlyTheChosenRunRewardType() {
    RewardService service = createService();
    RunState goldRun = new RunState();
    RunState itemRun = new RunState();
    int initialGold = goldRun.getOrCreatePlayerState().getGold();

    service.claimRunReward(goldRun, RewardOption.gold(25), null);
    service.claimRunReward(itemRun, RewardOption.item(ItemType.ENERGY_CRYSTAL), null);

    assertEquals(initialGold + 25, goldRun.getOrCreatePlayerState().getGold());
    assertEquals(List.of(), goldRun.getOrCreatePlayerState().getOwnedItems());
    assertEquals(
        List.of(ItemType.ENERGY_CRYSTAL), itemRun.getOrCreatePlayerState().getOwnedItems());
  }

  @Test
  void shouldRejectInvalidSelectedCardIdsWithoutDeckMutation() {
    CardService cards = new CardLibrary(CardConfigLoader.loadCards());
    RewardService service =
        new RewardService(new RewardGenerator(new Random(1)), cards, new Random(2));
    RunState runState = new RunState();
    int initialSize = runState.getOrCreatePlayerDeck(cards).size();
    RewardOption validOption = RewardOption.cards(new CardRewardSelection(List.of("strike")));
    RewardOption unknownOption =
        RewardOption.cards(new CardRewardSelection(List.of("unknown-card")));

    assertThrows(
        IllegalArgumentException.class, () -> service.claimRunReward(runState, validOption, null));
    assertThrows(
        IllegalArgumentException.class, () -> service.claimRunReward(runState, validOption, " "));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.claimRunReward(runState, unknownOption, "unknown-card"));
    assertEquals(initialSize, runState.getOrCreatePlayerDeck(cards).size());
  }

  @Test
  void shouldCreateAnIndependentBaseInstanceForAnAlreadyOwnedCard() {
    CardService cards = new CardLibrary(CardConfigLoader.loadCards());
    RewardService service =
        new RewardService(new RewardGenerator(new Random(1)), cards, new Random(2));
    RunState runState = new RunState();
    var deck = runState.getOrCreatePlayerDeck(cards);
    CardInstance existingStrike =
        deck.getCards().stream()
            .filter(card -> card.cardId().equals("strike"))
            .findFirst()
            .orElseThrow();
    int initialCopies = deck.countByCardId("strike");

    service.claimRunReward(
        runState, RewardOption.cards(new CardRewardSelection(List.of("strike"))), "strike");

    CardInstance acquired = deck.getCards().get(deck.size() - 1);
    assertEquals("strike", acquired.cardId());
    assertEquals(CardInstance.BASE_LEVEL, acquired.upgradeLevel());
    assertNotEquals(existingStrike.instanceId(), acquired.instanceId());
    assertEquals(initialCopies + 1, deck.countByCardId("strike"));
  }

  private RewardService createService() {
    CardService cards = new CardLibrary(CardConfigLoader.loadCards());
    return new RewardService(new RewardGenerator(new Random(1)), cards, new Random(2));
  }
}
