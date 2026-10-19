package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import com.csse3200.game.components.spritedisplay.clickable.AimSession;
import com.csse3200.game.components.spritedisplay.clickable.Clickable;
import com.csse3200.game.components.spritedisplay.clickable.ClickableRecord;
import com.csse3200.game.components.spritedisplay.clickable.DragNDrop;
import com.csse3200.game.components.spritedisplay.clickable.GatedClickable;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.services.DragNDropService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Checks the factory method's routing only; aiming behaviour belongs to DragNDrop's aim session.
 */
@ExtendWith(GameExtension.class)
class BattleClickableFactoryTest {

  private BattleClickableFactory factory;

  @BeforeEach
  void setUp() {
    // DragNDrop registers itself as a drag source on construction.
    ServiceLocator.registerDragNDropService(new DragNDropService());
    factory =
        new BattleClickableFactory(
            List.of(), mock(AimSession.class), mock(AimSession.class), mock(AimSession.class));
  }

  @Test
  void aimDragCreatesADragWidget() {
    Clickable card = factory.createClickable(handCard(BattleClickableFactory.AIM_DRAG));

    assertInstanceOf(DragNDrop.class, card);
  }

  @Test
  void selfAimDragCreatesADragWidget() {
    Clickable card = factory.createClickable(handCard(BattleClickableFactory.SELF_AIM_DRAG));

    assertInstanceOf(DragNDrop.class, card);
  }

  @Test
  void allEnemiesAimDragCreatesADragWidget() {
    Clickable card = factory.createClickable(handCard(BattleClickableFactory.ALL_ENEMIES_AIM_DRAG));

    assertInstanceOf(DragNDrop.class, card);
  }

  @Test
  void otherVariantsFallBackToTheDefaults() {
    Clickable button =
        factory.createClickable(ClickableRecord.builder("endTurn").variant("gated").build());

    assertInstanceOf(GatedClickable.class, button);
  }

  private static ClickableRecord handCard(String variant) {
    return ClickableRecord.builder("playCard").text("card").variant(variant).build();
  }
}
