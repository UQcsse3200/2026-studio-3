package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rewards.RewardOption;
import com.csse3200.game.rewards.RewardService;
import java.util.List;
import java.util.Objects;

public class RewardDisplay extends Displaying {
  public static final String REWARD_CLAIMED_EVENT = "rewardClaimed";

  private final RunState runState;
  private final RewardService rewardService;
  private final Runnable afterRewardApplied;

  private List<RewardOption> options;
  private boolean claimed;

  public RewardDisplay(DisplayingRecord rec, RewardService rewardService, RunState runState) {
    this(rec, rewardService, runState, () -> {});
  }

  /**
   * Creates the reward picker.
   *
   * @param afterRewardApplied callback run after the selected reward mutates persistent run state
   *     and before any navigation event is fired
   */
  public RewardDisplay(
      DisplayingRecord rec,
      RewardService rewardService,
      RunState runState,
      Runnable afterRewardApplied) {
    super(rec);
    this.rewardService = Objects.requireNonNull(rewardService, "rewardService must not be null");
    this.runState = runState;
    this.afterRewardApplied =
        Objects.requireNonNull(afterRewardApplied, "afterRewardApplied must not be null");
  }

  @Override
  public void create() {
    super.create();

    float multiplier = 0f;
    if (runState != null) {
      multiplier = runState.getOrCreatePlayerState().getGoldBonusMultiplier();
    }

    options = rewardService.generateRewardOptions(multiplier);
    buildOptionButtons();
  }

  private void buildOptionButtons() {
    if (options == null || options.isEmpty()) {
      return;
    }

    Table optionsTable = new Table();
    optionsTable.setFillParent(true);
    optionsTable.top();
    optionsTable.padTop(400f);

    for (RewardOption option : options) {
      if (option == null) {
        continue;
      }

      TextButton button = new TextButton(describeOption(option), skin);

      button.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
              claimOption(option);
            }
          });

      button.addListener(
          new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(
                com.badlogic.gdx.scenes.scene2d.InputEvent event,
                float x,
                float y,
                int pointer,
                int buttonCode) {
              event.stop();
              return true;
            }
          });

      optionsTable.add(button).pad(10f).width(320f).height(80f);
      optionsTable.row();
    }

    stage.addActor(optionsTable);
  }

  private String describeOption(RewardOption option) {
    if (option == null || option.type == null) {
      return "";
    }

    return switch (option.type) {
      case GOLD -> option.goldAmount + " Gold";

      case ITEM -> {
        if (option.itemId == null) {
          yield "Item";
        }

        yield switch (option.itemId) {
          case LUCKY_COIN -> "Lucky Coin (+10% Gold)";
          case ENERGY_CRYSTAL -> "Energy Crystal (+1 Max Energy)";
          case MERCHANTS_FAVOR -> "Merchant's Favor (+5% Shop Discount)";
        };
      }
    };
  }

  void claimOption(RewardOption option) {
    if (claimed || option == null || option.type == null) {
      return;
    }

    claimed = true;

    if (runState != null) {
      PlayerRunState playerState = runState.getOrCreatePlayerState();

      switch (option.type) {
        case GOLD -> playerState.addGold(option.goldAmount);

        case ITEM -> {
          if (option.itemId != null) {
            playerState.addOwnedItem(option.itemId);
          }
        }
      }
    }

    // This is deliberately before the navigation events below. A durable checkpoint therefore
    // contains the claimed reward, and a subsequent MapScreen flush is a no-op.
    afterRewardApplied.run();
    entity.getEvents().trigger(REWARD_CLAIMED_EVENT);
    entity.getEvents().trigger(EndBattleDisplay.RETURN_TO_MENU_EVENT);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Positioning is handled by the table layout.
  }
}
