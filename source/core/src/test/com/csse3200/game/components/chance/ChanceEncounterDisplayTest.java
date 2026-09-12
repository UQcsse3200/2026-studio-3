package com.csse3200.game.components.chance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.extensions.GameExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class ChanceEncounterDisplayTest {

  @Test
  void shouldFormatEncounterIdAsTitle() {
    assertEquals("Mysterious Shrine", ChanceEncounterDisplay.formatTitle("mysterious-shrine"));
    assertEquals("Healing Spring", ChanceEncounterDisplay.formatTitle("healing_spring"));
  }

  @Test
  void shouldDescribeHealthAndGoldOutcome() {
    assertEquals(
        "You lose 10 health.\nYou gain 25 gold.",
        ChanceEncounterDisplay.formatOutcome(new ChanceOutcome(-10, 25)));
  }

  @Test
  void shouldDescribeNoEffectOutcome() {
    assertEquals(
        "Nothing happens. You continue on your way.",
        ChanceEncounterDisplay.formatOutcome(new ChanceOutcome(0, 0)));
  }

  @Test
  void shouldDescribeUnresolvedChoice() {
    assertEquals("This choice could not be resolved.", ChanceEncounterDisplay.formatOutcome(null));
  }
}
