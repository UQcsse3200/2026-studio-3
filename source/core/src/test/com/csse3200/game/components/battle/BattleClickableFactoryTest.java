package com.csse3200.game.components.battle;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

@ExtendWith(GameExtension.class)
class BattleClickableFactoryTest {

  private final AimSession enemyAim = mock(AimSession.class);
  private final AimSession playerAim = mock(AimSession.class);
  private final AimSession allEnemiesAim = mock(AimSession.class);
  private BattleClickableFactory factory;

  @BeforeEach
  void setUp() {
    // DragNDrop registers itself as a drag source on construction.
    ServiceLocator.registerDragNDropService(new DragNDropService());
    factory = new BattleClickableFactory(List.of(), enemyAim, playerAim, allEnemiesAim);
  }

  @Test
  void aimDragUsesTheEnemyAim() {
    Clickable card = factory.createClickable(handCard(BattleClickableFactory.AIM_DRAG));

    assertInstanceOf(DragNDrop.class, card);
    assertWiredTo(card, enemyAim);
  }

  @Test
  void selfAimDragUsesThePlayerAim() {
    Clickable card = factory.createClickable(handCard(BattleClickableFactory.SELF_AIM_DRAG));

    assertInstanceOf(DragNDrop.class, card);
    assertWiredTo(card, playerAim);
  }

  @Test
  void allEnemiesAimDragUsesTheAllEnemiesAim() {
    Clickable card = factory.createClickable(handCard(BattleClickableFactory.ALL_ENEMIES_AIM_DRAG));

    assertInstanceOf(DragNDrop.class, card);
    assertWiredTo(card, allEnemiesAim);
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

  /** DragNDrop cancels its own aim session (and only that one) when it is removed. */
  private void assertWiredTo(Clickable card, AimSession expected) {
    card.remove();
    for (AimSession aim : List.of(enemyAim, playerAim, allEnemiesAim)) {
      if (aim == expected) {
        verify(aim).cancel();
      } else {
        verify(aim, never()).cancel();
      }
    }
  }
}
