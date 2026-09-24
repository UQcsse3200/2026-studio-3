package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rewards.ItemType;
import com.csse3200.game.rewards.RewardOption;
import com.csse3200.game.rewards.RewardService;
import com.csse3200.game.rewards.RewardType;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import java.util.Map;

public class RewardDisplay extends Displaying {
  public static final String REWARD_CLAIMED_EVENT = "rewardClaimed";

  private static final String BACKGROUND_TEXTURE = "images/battle_background.png";
  private static final String PANEL_TEXTURE = "images/ui/reward-panel.png";
  private static final String CARD_TEXTURE = "images/ui/reward-card.png";
  private static final String GOLD_TEXTURE = "images/ui/gold-reward.png";
  private static final String VICTORY_TITLE_TEXTURE = "images/ui/victory-title.png";
  private static final Color SCRIM = new Color(0.015f, 0.01f, 0.025f, 0.58f);
  private static final Color GOLD = new Color(0.96f, 0.78f, 0.38f, 1f);
  private static final Color CREAM = new Color(0.93f, 0.87f, 0.73f, 1f);
  private static final Color MUTED = new Color(0.72f, 0.66f, 0.58f, 1f);
  private static final Map<ItemType, String> ITEM_ICONS =
      Map.of(
          ItemType.LUCKY_COIN, "images/ui/lucky-coin.png",
          ItemType.ENERGY_CRYSTAL, "images/ui/energy-crystal.png",
          ItemType.MERCHANTS_FAVOR, "images/ui/merchants-favor.png",
          ItemType.IRON_AEGIS, "images/ui/iron-aegis.png",
          ItemType.WARRIORS_CREST, "images/ui/warriors-crest.png");

  private final RunState runState;
  private final RewardService rewardService;
  private List<RewardOption> options;
  private boolean claimed;
  private boolean goldRewardUsesLuckyCoin;
  private Actor background;
  private Actor scrim;
  private Actor rewardUi;

  public RewardDisplay(DisplayingRecord rec, RewardService rewardService, RunState runState) {
    super(rec);
    this.rewardService = rewardService;
    this.runState = runState;
  }

  @Override
  public void create() {
    super.create();

    if (runState != null) {
      PlayerRunState playerState = runState.getOrCreatePlayerState();
      goldRewardUsesLuckyCoin = playerState.hasOwnedItem(ItemType.LUCKY_COIN);
    }

    options = rewardService.generateRewardOptions();
    buildRewardScreen();
  }

  private void buildRewardScreen() {
    if (options == null || options.isEmpty()) {
      return;
    }

    Image scene = new Image(texture(BACKGROUND_TEXTURE));
    scene.setScaling(Scaling.fill);
    scene.setFillParent(true);
    background = scene;
    stage.addActor(scene);
    scene.toBack();

    Image dimmer = new Image(skin.newDrawable("white", SCRIM));
    dimmer.setFillParent(true);
    scrim = dimmer;
    stage.addActor(dimmer);

    float panelWidth = Math.min(stage.getWidth() * 0.7f, 900f);
    float panelHeight = panelWidth * 0.625f;
    if (panelHeight > stage.getHeight() * 0.9f) {
      panelHeight = stage.getHeight() * 0.9f;
      panelWidth = panelHeight / 0.625f;
    }

    Stack panel = new Stack();
    Image panelFrame = new Image(texture(PANEL_TEXTURE));
    panelFrame.setScaling(Scaling.stretch);
    panel.add(panelFrame);
    panel.add(createPanelContent(panelWidth, panelHeight));

    Table root = new Table();
    root.setFillParent(true);
    root.add(panel).width(panelWidth).height(panelHeight);
    rewardUi = root;
    stage.addActor(root);
  }

  private Table createPanelContent(float panelWidth, float panelHeight) {
    Table content = new Table();
    content.top();
    content.pad(panelHeight * 0.13f, panelWidth * 0.135f, panelHeight * 0.15f, panelWidth * 0.135f);

    Image victory = new Image(texture(VICTORY_TITLE_TEXTURE));
    victory.setScaling(Scaling.fit);
    content.add(victory).size(panelWidth * 0.38f, panelHeight * 0.1f).center();
    content.row();

    Label subtitle = new Label("CHOOSE ONE REWARD", largeStyle(GOLD));
    subtitle.setFontScale(0.82f);
    content.add(subtitle).center().padTop(2f).padBottom(panelHeight * 0.018f);
    content.row();

    Table cards = new Table();
    cards.defaults().padLeft(panelWidth * 0.012f).padRight(panelWidth * 0.012f);
    float cardWidth = panelWidth * 0.27f;
    float cardHeight = panelHeight * 0.54f;
    for (RewardOption option : options) {
      if (option != null) {
        cards
            .add(createRewardCard(option, cardWidth, cardHeight))
            .width(cardWidth)
            .height(cardHeight);
      }
    }
    content.add(cards).expand().center();
    content.row();

    Table footer = new Table();
    footer.add(line()).width(panelWidth * 0.11f).height(1f);
    footer
        .add(new Label("Choose one reward to continue", smallStyle(MUTED)))
        .padLeft(12f)
        .padRight(12f);
    footer.add(line()).width(panelWidth * 0.11f).height(1f);
    content.add(footer).center().padTop(panelHeight * -0.015f);
    return content;
  }

  private Stack createRewardCard(RewardOption option, float cardWidth, float cardHeight) {
    Stack card = new Stack();
    Image frame = new Image(texture(CARD_TEXTURE));
    frame.setScaling(Scaling.stretch);
    frame.setTouchable(Touchable.disabled);
    card.add(frame);

    Table details = new Table();
    details.top();
    details.setTouchable(Touchable.disabled);
    details.pad(cardHeight * 0.115f, cardWidth * 0.095f, cardHeight * 0.055f, cardWidth * 0.095f);

    Label name = new Label(rewardTitle(option), largeStyle(GOLD));
    name.setFontScale(isLongItemName(option) ? 0.68f : 0.82f);
    name.setAlignment(com.badlogic.gdx.utils.Align.center);
    details.add(name).width(cardWidth * 0.8f).center();
    details.row();

    Image icon = new Image(texture(rewardIcon(option)));
    icon.setScaling(Scaling.fit);
    details
        .add(icon)
        .size(cardWidth * 0.44f, cardHeight * 0.34f)
        .center()
        .padTop(cardHeight * 0.025f);
    details.row();

    Label description = new Label(rewardDescription(option), smallStyle(CREAM));
    description.setWrap(true);
    description.setAlignment(com.badlogic.gdx.utils.Align.center);
    details
        .add(description)
        .width(cardWidth * 0.84f)
        .height(cardHeight * 0.13f)
        .center()
        .padTop(cardHeight * 0.008f);
    details.row();

    details.add().expandY();
    details.row();
    Label select = new Label("SELECT", largeStyle(GOLD));
    select.setFontScale(0.82f);
    select.setAlignment(com.badlogic.gdx.utils.Align.center);
    details
        .add(select)
        .width(cardWidth * 0.78f)
        .height(cardHeight * 0.12f)
        .center()
        .padBottom(cardHeight * 0.055f);
    card.add(details);

    card.setTouchable(Touchable.enabled);
    card.addListener(
        new ClickListener() {
          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            event.stop();
            return super.touchDown(event, x, y, pointer, button);
          }

          @Override
          public void clicked(InputEvent event, float x, float y) {
            claimOption(option);
          }

          @Override
          public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            frame.setColor(1f, 0.88f, 0.58f, 1f);
          }

          @Override
          public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
            frame.setColor(Color.WHITE);
          }
        });
    return card;
  }

  private String rewardTitle(RewardOption option) {
    if (option == null || option.type == null) {
      return "";
    }
    return switch (option.type) {
      case GOLD -> option.goldAmount + luckyCoinBonus(option) + " GOLD";
      case ITEM ->
          option.itemId == null
              ? "ITEM"
              : switch (option.itemId) {
                case LUCKY_COIN -> "Lucky Coin";
                case ENERGY_CRYSTAL -> "Energy Crystal";
                case MERCHANTS_FAVOR -> "Merchant's Favor";
                case IRON_AEGIS -> "Iron Aegis";
                case WARRIORS_CREST -> "Warrior's Crest";
              };
    };
  }

  private String rewardDescription(RewardOption option) {
    if (option == null || option.type == null) {
      return "";
    }
    return switch (option.type) {
      case GOLD -> goldRewardDescription(option);
      case ITEM ->
          option.itemId == null
              ? ""
              : switch (option.itemId) {
                case LUCKY_COIN -> "+10% Total Gold\nMaximum +20";
                case ENERGY_CRYSTAL -> "+1 Max Energy";
                case MERCHANTS_FAVOR -> "+10% Shop Discount\nMaximum 50%";
                case IRON_AEGIS -> "+5 Armour when used";
                case WARRIORS_CREST -> "+1 Strength when used";
              };
    };
  }

  private String goldRewardDescription(RewardOption option) {
    int luckyBonus = luckyCoinBonus(option);
    if (luckyBonus == 0 || runState == null) {
      return "Added to your run";
    }

    int totalAfterClaim =
        runState.getOrCreatePlayerState().getGold() + option.goldAmount + luckyBonus;
    return option.goldAmount
        + " Base + "
        + luckyBonus
        + " Lucky Coin\nTotal Gold: "
        + totalAfterClaim;
  }

  private int luckyCoinBonus(RewardOption option) {
    if (!goldRewardUsesLuckyCoin || runState == null || option == null) {
      return 0;
    }
    return runState.getOrCreatePlayerState().calculateLuckyCoinBonus(option.goldAmount);
  }

  private boolean isLongItemName(RewardOption option) {
    return option != null
        && option.type == RewardType.ITEM
        && (option.itemId == ItemType.MERCHANTS_FAVOR || option.itemId == ItemType.WARRIORS_CREST);
  }

  private String rewardIcon(RewardOption option) {
    if (option != null && option.type == RewardType.ITEM) {
      return ITEM_ICONS.getOrDefault(option.itemId, GOLD_TEXTURE);
    }
    return GOLD_TEXTURE;
  }

  private LabelStyle largeStyle(Color colour) {
    LabelStyle style = new LabelStyle(skin.get("large", LabelStyle.class));
    style.fontColor = colour;
    return style;
  }

  private LabelStyle smallStyle(Color colour) {
    LabelStyle style = new LabelStyle(skin.get("small", LabelStyle.class));
    style.fontColor = colour;
    return style;
  }

  private Image line() {
    return new Image(skin.newDrawable("white", new Color(0.48f, 0.32f, 0.17f, 1f)));
  }

  private Texture texture(String path) {
    return ServiceLocator.getResourceService().getAsset(path, Texture.class);
  }

  private void claimOption(RewardOption option) {
    if (claimed || option == null || option.type == null) {
      return;
    }

    claimed = true;

    if (runState != null) {
      PlayerRunState playerState = runState.getOrCreatePlayerState();

      switch (option.type) {
        case GOLD -> {
          playerState.claimGoldReward(option.goldAmount, goldRewardUsesLuckyCoin);
          goldRewardUsesLuckyCoin = false;
        }
        case ITEM -> {
          if (option.itemId != null) {
            playerState.addOwnedItem(option.itemId);
          }
        }
      }
    }

    entity.getEvents().trigger(REWARD_CLAIMED_EVENT);
    entity.getEvents().trigger(EndBattleDisplay.RETURN_TO_MENU_EVENT);
  }

  @Override
  public void dispose() {
    if (background != null) {
      background.remove();
    }
    if (scrim != null) {
      scrim.remove();
    }
    if (rewardUi != null) {
      rewardUi.remove();
    }
    super.dispose();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Positioning is handled by the table layout.
  }
}
