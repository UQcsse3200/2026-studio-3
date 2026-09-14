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
import com.csse3200.game.components.cards.CardUpgradeSelection.UpgradeOption;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CardUpgradeSelectionTest {
  private CardUpgradeSelection selection;
  private static final int MAX_SELECTIONS = 2;
  private CardService cardService;
  private List<String> deck;

  @BeforeEach
  void setUp() {
    cardService = new CardLibrary(CardConfigLoader.loadCards());
    deck = List.of("strike", "strike", "strike", "defend", "defend", "defend", "bandage");
    selection = new CardUpgradeSelection(deck, cardService, MAX_SELECTIONS);
  }

  @Test
  void shouldBuildOptionsForEachUpgradableCardWithItsDeckIndex() {
    List<UpgradeOption> options = selection.getCardUpgradeOption();
    List<Integer> deckIndices = options.stream().map(UpgradeOption::deckIndex).toList();

    assertAll(
        () -> assertEquals(3, options.size()), () -> assertEquals(List.of(0, 1, 2), deckIndices));
  }

  @Test
  void shouldReturnNoOptionsWhenNoCardDefinesAnUpgrade() {
    CardService withoutUpgrades = TestCardService.withCards("strike", "defend", "bandage");

    CardUpgradeSelection noUpgrades =
        new CardUpgradeSelection(deck, withoutUpgrades, MAX_SELECTIONS);

    assertTrue(noUpgrades.getCardUpgradeOption().isEmpty());
  }

  @Test
  void shouldSkipUnknownCardIdsWithoutFailing() {
    CardUpgradeSelection withUnknownCard =
        new CardUpgradeSelection(List.of("banana", "strike"), cardService, MAX_SELECTIONS);

    List<Integer> deckIndices =
        withUnknownCard.getCardUpgradeOption().stream().map(UpgradeOption::deckIndex).toList();

    assertEquals(List.of(1), deckIndices);
  }

  @Test
  void shouldReturnSelectedIndicesInDescendingOrder() {
    selection.toggle(0);
    selection.toggle(2);

    List<Integer> result = selection.getSelectedDeckIndices();

    assertEquals(List.of(2, 0), result);
  }

  @Test
  void shouldRejectDeckIndexThatIsNotAnUpgradableOption() {
    assertThrows(IllegalArgumentException.class, () -> selection.toggle(3));
  }

  @Test
  void shouldRestoreStateWhenTogglingTheSameCardTwice() {
    selection.toggle(1);

    boolean selectedAfterSecondToggle = selection.toggle(1);

    assertAll(
        () -> assertFalse(selectedAfterSecondToggle),
        () -> assertFalse(selection.isSelected(1)),
        () -> assertEquals(MAX_SELECTIONS, selection.remainingSelectable()));
  }

  @Test
  void shouldIgnoreNewSelectionsOnceCapIsReached() {
    selection.toggle(0);
    selection.toggle(1);

    boolean selectedAtCap = selection.toggle(2);

    assertAll(
        () -> assertFalse(selectedAtCap),
        () -> assertFalse(selection.isSelected(2)),
        () -> assertEquals(0, selection.remainingSelectable()),
        () -> assertEquals(List.of(1, 0), selection.getSelectedDeckIndices()));
  }

  @Test
  void shouldKeepAlreadySelectedCardsToggleableAtCap() {
    selection.toggle(0);
    selection.toggle(1);

    assertAll(
        () -> assertTrue(selection.canSelect(0)),
        () -> assertFalse(selection.canSelect(2)),
        () -> assertFalse(selection.toggle(0)),
        () -> assertEquals(1, selection.remainingSelectable()));
  }

  @Test
  void shouldOnlyAllowConfirmWhenAtLeastOneCardIsSelected() {
    boolean confirmableWhenEmpty = selection.canConfirm();

    selection.toggle(0);

    assertAll(() -> assertFalse(confirmableWhenEmpty), () -> assertTrue(selection.canConfirm()));
  }

  @Test
  void shouldReturnAnImmutableSelectionList() {
    selection.toggle(0);

    List<Integer> result = selection.getSelectedDeckIndices();

    assertThrows(UnsupportedOperationException.class, () -> result.add(9));
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
}
