package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
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
    Thread.dumpStack();
    options = rewardService.generateRewardOptions();
    buildOptionButtons();
  }

  private void buildOptionButtons() {
    Table optionsTable = new Table();
    optionsTable.setFillParent(true);
    optionsTable.top();
    optionsTable.padTop(400f);

    for (RewardOption option : options) {
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
      optionsTable.add(button).pad(10f).width(300f).height(80f);
      optionsTable.row();
    }

    stage.addActor(optionsTable);
  }

  private String describeOption(RewardOption option) {
    return switch (option.type) {
      case GOLD -> option.goldAmount + " Gold";
      case CARD_UPGRADE -> "Upgrade: " + option.cardId;
      case ITEM -> "Item: " + option.itemId;
    };
  }

  private void claimOption(RewardOption option) {
    if (claimed) {
      return;
    }
    claimed = true;
    runState.setPendingReward(option);
    entity.getEvents().trigger(REWARD_CLAIMED_EVENT);
    entity.getEvents().trigger(EndBattleDisplay.RETURN_TO_MENU_EVENT);
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // positioning handled by the table layout
  }
}
