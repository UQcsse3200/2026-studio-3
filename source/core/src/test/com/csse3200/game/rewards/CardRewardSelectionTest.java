package com.csse3200.game.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CardRewardSelectionTest {
  @Test
  void shouldKeepAnImmutableOrderedSnapshot() {
    List<String> source = new ArrayList<>(List.of("strike", "defend", "bandage"));
    CardRewardSelection selection = new CardRewardSelection(source);
    source.clear();

    assertEquals(List.of("strike", "defend", "bandage"), selection.cardIds());
    assertTrue(selection.contains("defend"));
    assertFalse(selection.contains("poison_dagger"));
    List<String> ids = selection.cardIds();
    assertThrows(UnsupportedOperationException.class, () -> ids.add("poison_dagger"));
  }

  @Test
  void shouldAcceptSelectionsWithOneToThreeCards() {
    assertEquals(1, new CardRewardSelection(List.of("strike")).cardIds().size());
    assertEquals(2, new CardRewardSelection(List.of("strike", "defend")).cardIds().size());
    assertEquals(
        3, new CardRewardSelection(List.of("strike", "defend", "bandage")).cardIds().size());
  }

  @Test
  void shouldRejectInvalidSelections() {
    List<String> empty = List.of();
    List<String> tooMany = List.of("strike", "defend", "bandage", "expose");
    List<String> duplicates = List.of("strike", "strike");
    List<String> blank = List.of(" ");
    assertThrows(IllegalArgumentException.class, () -> new CardRewardSelection(null));
    assertThrows(IllegalArgumentException.class, () -> new CardRewardSelection(empty));
    assertThrows(IllegalArgumentException.class, () -> new CardRewardSelection(tooMany));
    assertThrows(IllegalArgumentException.class, () -> new CardRewardSelection(duplicates));
    assertThrows(IllegalArgumentException.class, () -> new CardRewardSelection(blank));
  }
}
