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

public class RewardDisplay extends Displaying {
  public static final String REWARD_CLAIMED_EVENT = "rewardClaimed";

  private static final int NUM_OPTIONS = 2;

  private final RunState runState;
  private final RewardService rewardService;
  private List<RewardOption> options;
  private boolean claimed = false;

  public RewardDisplay(DisplayingRecord rec, RewardService rewardService, RunState runState) {
    super(rec);
    this.rewardService = rewardService;
    this.runState = runState;
  }

  @Override
  public void create() {
    super.create();
    options = rewardService.generateRewardOptions();
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
    if (option == null) {
      return "";
    }
    return switch (option.type) {
      case GOLD -> {
        float multiplier = 0f;
        if (runState != null && runState.getOrCreatePlayerState() != null) {
          multiplier = runState.getOrCreatePlayerState().getGoldBonusMultiplier();
        }
        int finalGold = Math.round(option.goldAmount * (1 + multiplier));
        yield multiplier > 0
            ? finalGold + " Gold (+" + Math.round(multiplier * 100) + "%)"
            : finalGold + " Gold";
      }
      case ITEM -> {
        if (option.itemId == null) {
          yield "Item";
        }
        yield switch (option.itemId) {
          case LUCKY_COIN -> "Lucky Coin (+10% Gold)";
          case MERCHANTS_FAVOR -> "Merchant's Favor (10% Off)";
          case ENERGY_CRYSTAL -> "Energy Crystal (+20 HP)";
        };
      }
    };
  }

  private void claimOption(RewardOption option) {
    if (claimed || option == null) {
      return;
    }
    claimed = true;

    if (runState != null) {
      PlayerRunState playerState = runState.getOrCreatePlayerState();
      if (playerState != null) {
        switch (option.type) {
          case GOLD -> {
            int finalGold =
                Math.round(option.goldAmount * (1 + playerState.getGoldBonusMultiplier()));
            playerState.restore(
                playerState.getCurrentHealth(),
                playerState.getMaxHealth(),
                playerState.getGold() + finalGold,
                playerState.getGoldBonusMultiplier(),
                playerState.getShopDiscount());
          }
          case ITEM -> {
            if (option.itemId != null) {
              switch (option.itemId) {
                case LUCKY_COIN ->
                    playerState.restore(
                        playerState.getCurrentHealth(),
                        playerState.getMaxHealth(),
                        playerState.getGold(),
                        playerState.getGoldBonusMultiplier() + 0.1f,
                        playerState.getShopDiscount());

                case MERCHANTS_FAVOR ->
                    playerState.restore(
                        playerState.getCurrentHealth(),
                        playerState.getMaxHealth(),
                        playerState.getGold(),
                        playerState.getGoldBonusMultiplier(),
                        playerState.getShopDiscount() + 0.1f);

                case ENERGY_CRYSTAL -> {
                  int healAmount = 20;
                  int newHealth =
                      Math.min(
                          playerState.getCurrentHealth() + healAmount, playerState.getMaxHealth());
                  playerState.restore(
                      newHealth,
                      playerState.getMaxHealth(),
                      playerState.getGold(),
                      playerState.getGoldBonusMultiplier(),
                      playerState.getShopDiscount());
                }
              }
            }
          }
        }
      }
    }

    entity.getEvents().trigger(REWARD_CLAIMED_EVENT);
    entity.getEvents().trigger(EndBattleDisplay.RETURN_TO_MENU_EVENT);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // positioning handled by the table layout
  }
}
