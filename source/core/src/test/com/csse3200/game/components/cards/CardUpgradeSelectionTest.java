package com.csse3200.game.components.cards;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.TestCardService;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.components.cards.CardUpgradeSelection.UpgradeOption;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CardUpgradeSelectionTest {
  private static final int MAX_SELECTIONS = 2;
  private CardUpgradeSelection selection;
  private CardService cardService;
  private List<CardInstance> deck;

  @BeforeEach
  void setUp() {
    cardService = new CardLibrary(CardConfigLoader.loadCards());
    deck =
        List.of(
            instance("strike-1", "strike"),
            instance("strike-2", "strike"),
            instance("strike-3", "strike"),
            instance("defend-1", "defend"),
            instance("defend-2", "defend"),
            instance("defend-3", "defend"),
            instance("bandage-1", "bandage"));
    selection = new CardUpgradeSelection(deck, cardService, MAX_SELECTIONS);
  }

  @Test
  void shouldBuildOptionsForEachUpgradableCardWithItsExactInstance() {
    List<UpgradeOption> options = selection.getCardUpgradeOption();
    List<String> instanceIds =
        options.stream().map(option -> option.instance().instanceId()).toList();

    assertAll(
        () -> assertEquals(3, options.size()),
        () -> assertEquals(List.of("strike-1", "strike-2", "strike-3"), instanceIds));
  }

  @Test
  void shouldReturnNoOptionsWhenNoCardDefinesAnUpgrade() {
    CardService withoutUpgrades = TestCardService.withCards("strike", "defend", "bandage");
    CardUpgradeSelection noUpgrades =
        new CardUpgradeSelection(deck, withoutUpgrades, MAX_SELECTIONS);
    assertTrue(noUpgrades.getCardUpgradeOption().isEmpty());
  }

  @Test
  void shouldSkipUnknownAndAlreadyUpgradedCards() {
    CardUpgradeSelection filtered =
        new CardUpgradeSelection(
            List.of(
                instance("banana-1", "banana"),
                instance("strike-base", "strike"),
                new CardInstance("strike-plus", "strike", CardInstance.UPGRADED_LEVEL)),
            cardService,
            MAX_SELECTIONS);

    assertEquals(
        List.of("strike-base"),
        filtered.getCardUpgradeOption().stream()
            .map(option -> option.instance().instanceId())
            .toList());
  }

  @Test
  void shouldReturnSelectedInstanceIdsInSelectionOrder() {
    selection.toggle(id(0));
    selection.toggle(id(2));
    assertEquals(List.of(id(0), id(2)), selection.getSelectedInstanceIds());
  }

  @Test
  void shouldRejectInstanceThatIsNotAnUpgradableOption() {
    assertThrows(IllegalArgumentException.class, () -> selection.toggle(id(3)));
  }

  @Test
  void shouldRestoreStateWhenTogglingTheSameCardTwice() {
    selection.toggle(id(1));
    boolean selectedAfterSecondToggle = selection.toggle(id(1));

    assertAll(
        () -> assertFalse(selectedAfterSecondToggle),
        () -> assertFalse(selection.isSelected(id(1))),
        () -> assertEquals(MAX_SELECTIONS, selection.remainingSelectable()));
  }

  @Test
  void shouldIgnoreNewSelectionsOnceCapIsReached() {
    selection.toggle(id(0));
    selection.toggle(id(1));
    boolean selectedAtCap = selection.toggle(id(2));

    assertAll(
        () -> assertFalse(selectedAtCap),
        () -> assertFalse(selection.isSelected(id(2))),
        () -> assertEquals(0, selection.remainingSelectable()),
        () -> assertEquals(List.of(id(0), id(1)), selection.getSelectedInstanceIds()));
  }

  @Test
  void shouldKeepAlreadySelectedCardsToggleableAtCap() {
    selection.toggle(id(0));
    selection.toggle(id(1));

    assertAll(
        () -> assertTrue(selection.canSelect(id(0))),
        () -> assertFalse(selection.canSelect(id(2))),
        () -> assertFalse(selection.toggle(id(0))),
        () -> assertEquals(1, selection.remainingSelectable()));
  }

  @Test
  void shouldOnlyAllowConfirmWhenAtLeastOneCardIsSelected() {
    assertFalse(selection.canConfirm());
    selection.toggle(id(0));
    assertTrue(selection.canConfirm());
  }

  @Test
  void shouldReturnAnImmutableSelectionList() {
    selection.toggle(id(0));
    List<String> result = selection.getSelectedInstanceIds();
    assertThrows(UnsupportedOperationException.class, () -> result.add("other"));
  }

  @Test
  void shouldCommitUpgradeToOnlyTheSelectedInstance() {
    CardInstance first = instance("strike-base", "strike");
    CardInstance second = instance("strike-selected", "strike");
    PlayerDeck playerDeck = PlayerDeck.fromInstances(cardService, List.of(first, second));

    new PlayerDeckCardUpgradeCommitter(playerDeck).commitUpgrades(List.of(second.instanceId()));

    assertAll(
        () -> assertEquals(CardInstance.BASE_LEVEL, playerDeck.getCards().get(0).upgradeLevel()),
        () ->
            assertEquals(CardInstance.UPGRADED_LEVEL, playerDeck.getCards().get(1).upgradeLevel()),
        () -> assertEquals(first.instanceId(), playerDeck.getCards().get(0).instanceId()),
        () -> assertEquals(second.instanceId(), playerDeck.getCards().get(1).instanceId()));
  }

  @Test
  void shouldRejectInvalidConstructorArguments() {
    assertAll(
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> new CardUpgradeSelection(null, cardService, MAX_SELECTIONS)),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> new CardUpgradeSelection(deck, null, MAX_SELECTIONS)),
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> new CardUpgradeSelection(deck, cardService, 0)));
  }

  private String id(int index) {
    return deck.get(index).instanceId();
  }

  private static CardInstance instance(String instanceId, String cardId) {
    return new CardInstance(instanceId, cardId, CardInstance.BASE_LEVEL);
  }
}
