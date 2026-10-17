package com.csse3200.game.components.battle;

import com.csse3200.game.components.spritedisplay.clickable.AimSession;
import com.csse3200.game.components.spritedisplay.clickable.Clickable;
import com.csse3200.game.components.spritedisplay.clickable.ClickableFactory;
import com.csse3200.game.components.spritedisplay.clickable.ClickableRecord;
import com.csse3200.game.components.spritedisplay.clickable.DragNDrop;
import java.util.List;

/**
 * The battle screen's {@link ClickableFactory}. Overrides the factory method so hand cards are
 * built as draggable widgets wired to the aim session that matches their target type; every other
 * variant falls through to the defaults.
 */
public class BattleClickableFactory extends ClickableFactory {

  /** Hand card aimed at one enemy. */
  public static final String AIM_DRAG = "aimDrag";

  /** Hand card aimed at the player. */
  public static final String SELF_AIM_DRAG = "selfAimDrag";

  /** Hand card that hits every enemy. */
  public static final String ALL_ENEMIES_AIM_DRAG = "allEnemiesAimDrag";

  private final AimSession enemyAim;
  private final AimSession playerAim;
  private final AimSession allEnemiesAim;

  public BattleClickableFactory(
      List<ClickableRecord> records,
      AimSession enemyAim,
      AimSession playerAim,
      AimSession allEnemiesAim) {
    super(records);
    this.enemyAim = enemyAim;
    this.playerAim = playerAim;
    this.allEnemiesAim = allEnemiesAim;
  }

  @Override
  protected Clickable createClickable(ClickableRecord rec) {
    return switch (rec.variant()) {
      case AIM_DRAG -> new DragNDrop(rec, enemyAim);
      case SELF_AIM_DRAG -> new DragNDrop(rec, playerAim);
      case ALL_ENEMIES_AIM_DRAG -> new DragNDrop(rec, allEnemiesAim);
      default -> super.createClickable(rec);
    };
  }
}
