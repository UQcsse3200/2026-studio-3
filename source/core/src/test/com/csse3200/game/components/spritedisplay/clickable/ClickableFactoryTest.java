package com.csse3200.game.components.spritedisplay.clickable;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** The default factory method builds the registered variants and rejects unknown ones. */
@ExtendWith(GameExtension.class)
class ClickableFactoryTest {

  private final ClickableFactory factory = new ClickableFactory(List.of());

  @Test
  void createsDefaultVariantWhenNoneGiven() {
    Clickable clickable = factory.createClickable(ClickableRecord.builder("endTurn").build());

    assertInstanceOf(Clickable.class, clickable);
  }

  @Test
  void createsRegisteredVariant() {
    Clickable clickable =
        factory.createClickable(ClickableRecord.builder("endTurn").variant("gated").build());

    assertInstanceOf(GatedClickable.class, clickable);
  }

  @Test
  void unknownVariantFailsLoudly() {
    ClickableRecord rec = ClickableRecord.builder("endTurn").variant("gatd").build();

    IllegalArgumentException error =
        assertThrows(IllegalArgumentException.class, () -> factory.createClickable(rec));
    assertTrue(error.getMessage().contains("gatd"));
  }
}
