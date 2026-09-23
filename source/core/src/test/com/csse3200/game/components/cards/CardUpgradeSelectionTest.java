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
import com.csse3200.game.cards.runtime.CardInstance;
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
  private List<CardInstance> deck;

  @BeforeEach
  void setUp() {
    cardService = new CardLibrary(CardConfigLoader.loadCards());
    deck =
        List.of(
            new CardInstance("s1", "strike", CardInstance.BASE_LEVEL),
            new CardInstance("s2", "strike", CardInstance.BASE_LEVEL),
            new CardInstance("s3", "strike", CardInstance.BASE_LEVEL),
            new CardInstance("d1", "defend", CardInstance.BASE_LEVEL),
            new CardInstance("d2", "defend", CardInstance.BASE_LEVEL),
            new CardInstance("d3", "defend", CardInstance.BASE_LEVEL),
            new CardInstance("b1", "bandage", CardInstance.BASE_LEVEL));
    selection = new CardUpgradeSelection(deck, cardService, MAX_SELECTIONS);
  }

  @Test
  void shouldBuildOptionsForEachUpgradableCard() {
    List<UpgradeOption> options = selection.getCardUpgradeOption();
    List<String> instanceIds = options.stream().map(UpgradeOption::instanceId).toList();

    assertAll(
        () -> assertEquals(3, options.size()),
        () -> assertEquals(List.of("s1", "s2", "s3"), instanceIds));
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
        new CardUpgradeSelection(
            List.of(
                new CardInstance("x1", "banana", CardInstance.BASE_LEVEL),
                new CardInstance("s1", "strike", CardInstance.BASE_LEVEL)),
            cardService,
            MAX_SELECTIONS);

    List<String> instanceIds =
        withUnknownCard.getCardUpgradeOption().stream().map(UpgradeOption::instanceId).toList();

    assertEquals(List.of("s1"), instanceIds);
  }

  @Test
  void shouldReturnSelectedInstanceIdInSelectionOrder() {
    selection.toggle("s1");
    selection.toggle("s2");

    List<String> result = selection.getSelectedInstanceIds();

    assertEquals(List.of("s1", "s2"), result);
  }

  @Test
  void shouldRejectInstanceIdThatIsNotAnUpgradableOption() {
    assertThrows(IllegalArgumentException.class, () -> selection.toggle("b1"));
  }

  @Test
  void shouldRestoreStateWhenTogglingTheSameCardTwice() {
    selection.toggle("s2");

    boolean selectedAfterSecondToggle = selection.toggle("s2");

    assertAll(
        () -> assertFalse(selectedAfterSecondToggle),
        () -> assertFalse(selection.isSelected("s2")),
        () -> assertEquals(MAX_SELECTIONS, selection.remainingSelectable()));
  }

  @Test
  void shouldIgnoreNewSelectionsOnceCapIsReached() {
    selection.toggle("s1");
    selection.toggle("s2");

    boolean selectedAtCap = selection.toggle("s3");

    assertAll(
        () -> assertFalse(selectedAtCap),
        () -> assertFalse(selection.isSelected("s3")),
        () -> assertEquals(0, selection.remainingSelectable()),
        () -> assertEquals(List.of("s1", "s2"), selection.getSelectedInstanceIds()));
  }

  @Test
  void shouldKeepAlreadySelectedCardsToggleableAtCap() {
    selection.toggle("s1");
    selection.toggle("s2");
    boolean canSelectSelected = selection.canSelect("s1");
    boolean canSelectUnselected = selection.canSelect("s3");
    boolean stillSelectedAfterToggle = selection.toggle("s1");

    assertAll(
        () -> assertTrue(canSelectSelected),
        () -> assertFalse(canSelectUnselected),
        () -> assertFalse(stillSelectedAfterToggle),
        () -> assertEquals(1, selection.remainingSelectable()));
  }

  @Test
  void shouldOnlyAllowConfirmWhenAtLeastOneCardIsSelected() {
    boolean confirmableWhenEmpty = selection.canConfirm();

    selection.toggle("s1");

    assertAll(() -> assertFalse(confirmableWhenEmpty), () -> assertTrue(selection.canConfirm()));
  }

  @Test
  void shouldReturnAnImmutableSelectionList() {
    selection.toggle("s1");

    List<String> result = selection.getSelectedInstanceIds();

    assertThrows(UnsupportedOperationException.class, () -> result.add("s2"));
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

  @Test
  void shouldNotAllowSelectingIneligibleCardWithRoomLeft() {
    boolean canSelect = selection.canSelect("b1");
    assertFalse(canSelect);
  }
}
