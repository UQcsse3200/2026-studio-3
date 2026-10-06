package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.cards.runtime.ResolvedCard;
import com.csse3200.game.components.cards.CardWidget;
import com.csse3200.game.components.cards.CardWidgetAssets;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rewards.ItemFormatting;
import com.csse3200.game.rewards.ItemType;
import com.csse3200.game.rewards.RewardOption;
import com.csse3200.game.rewards.RewardService;
import com.csse3200.game.rewards.RewardType;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RewardDisplay extends Displaying {
  public static final String REWARD_CLAIMED_EVENT = "rewardClaimed";

  private static final Logger logger = LoggerFactory.getLogger(RewardDisplay.class);
  private static final String PANEL_TEXTURE = "images/ui/reward-panel.png";
  private static final String CARD_TEXTURE = "images/ui/reward-card.png";
  private static final String GOLD_TEXTURE = "images/ui/gold-reward.png";
  private static final String VICTORY_TITLE_TEXTURE = "images/ui/victory-title.png";
  private static final Color SCRIM = new Color(0.015f, 0.01f, 0.025f, 0.58f);
  private static final Color GOLD = new Color(0.96f, 0.78f, 0.38f, 1f);
  private static final Color CREAM = new Color(0.93f, 0.87f, 0.73f, 1f);
  private static final Map<ItemType, String> ITEM_ICONS =
      Map.of(
          ItemType.LUCKY_COIN, "images/ui/lucky-coin.png",
          ItemType.ENERGY_CRYSTAL, "images/ui/energy-crystal.png",
          ItemType.MERCHANTS_FAVOR, "images/ui/merchants-favor.png",
          ItemType.IRON_AEGIS, "images/ui/iron-aegis.png",
          ItemType.WARRIORS_CREST, "images/ui/warriors-crest.png");

  private final RunState runState;
  private final GdxGame game;
  private final RewardService rewardService;
  private final CardService cardService;
  private final CardDiscoveryService cardDiscoveryService;
  private final Runnable afterRewardApplied;
  private final CardResolver cardResolver = new CardResolver();
  private List<RewardOption> options;
  private boolean claimed;
  private boolean goldRewardUsesLuckyCoin;
  private boolean cardRewardCommitted;
  private boolean claimInProgress;
  private boolean created;
  private boolean disposed;
  private Actor background;
  private Actor scrim;
  private Actor rewardUi;
  private Table cardSelectionUi;
  private CardWidgetAssets cardWidgetAssets;
  private final List<Button> cardChoiceButtons = new ArrayList<>();
  private final List<CardWidget> cardWidgets = new ArrayList<>();
  private final List<Actor> rewardOptionCards = new ArrayList<>();

  public RewardDisplay(
      DisplayingRecord rec,
      RewardService rewardService,
      GdxGame game,
      CardService cardService,
      CardDiscoveryService cardDiscoveryService) {
    this(rec, rewardService, game, cardService, cardDiscoveryService, () -> {});
  }

  public RewardDisplay(
      DisplayingRecord rec,
      RewardService rewardService,
      GdxGame game,
      CardService cardService,
      CardDiscoveryService cardDiscoveryService,
      Runnable afterRewardApplied) {
    super(rec);
    this.rewardService = Objects.requireNonNull(rewardService, "rewardService cannot be null");
    this.game = Objects.requireNonNull(game, "game cannot be null");
    this.runState = game.getRunState();
    this.cardService = Objects.requireNonNull(cardService, "cardService cannot be null");
    this.cardDiscoveryService =
        Objects.requireNonNull(cardDiscoveryService, "cardDiscoveryService cannot be null");
    this.afterRewardApplied =
        Objects.requireNonNull(afterRewardApplied, "afterRewardApplied cannot be null");
  }

  @Override
  public void create() {
    if (created || disposed) {
      return;
    }
    created = true;
    super.create();

    if (runState != null) {
      PlayerRunState playerState = runState.getOrCreatePlayerState();
      goldRewardUsesLuckyCoin = playerState.hasOwnedItem(ItemType.LUCKY_COIN);
    }

    float multiplier =
        runState == null ? 0f : runState.getOrCreatePlayerState().getGoldBonusMultiplier();
    options = rewardService.generateRewardOptions(multiplier);
    cardWidgetAssets =
        CardWidgetAssets.fromManagedResources(skin, ServiceLocator.getResourceService());
    buildRewardScreen();
  }

  private void buildRewardScreen() {
    if (options == null || options.isEmpty()) {
      return;
    }

    Image scene =
        new Image(new Texture(Gdx.files.internal("images/" + game.getBackgroundId() + ".png")));
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
    rewardOptionCards.clear();
    for (RewardOption option : options) {
      if (option != null) {
        Stack rewardCard = createRewardCard(option, cardWidth, cardHeight);
        rewardOptionCards.add(rewardCard);
        cards.add(rewardCard).width(cardWidth).height(cardHeight);
      }
    }
    content.add(cards).expand().center();
    content.row();
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
            selectOption(option);
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
      case ITEM -> option.itemId == null ? "ITEM" : ItemFormatting.formatItemName(option.itemId);
      case CARD -> "CARD";
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
      case CARD -> "Choose one card to add to your deck";
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

  private Texture texture(String path) {
    return ServiceLocator.getResourceService().getAsset(path, Texture.class);
  }

  public void selectOption(RewardOption option) {
    if (disposed
        || claimed
        || claimInProgress
        || cardRewardCommitted
        || option == null
        || option.type == null) return;
    if (option.type == RewardType.CARD) {
      openCardSelection(option);
      return;
    }
    if (runState == null) {
      logger.error("Cannot claim {} reward without an active RunState", option.type);
      return;
    }
    claimInProgress = true;
    try {
      rewardService.claimRunReward(runState, option, null);
      completeReward();
    } catch (RuntimeException exception) {
      claimInProgress = false;
      logger.error("Could not claim {} reward", option.type, exception);
    }
  }

  private void openCardSelection(RewardOption option) {
    if (runState == null
        || option.cardSelection == null
        || option.cardSelection.cardIds().isEmpty()) {
      logger.error("Cannot open a Card reward without an active RunState and card choices");
      return;
    }
    List<CardConfig> offeredConfigs = new ArrayList<>();
    for (String cardId : option.cardSelection.cardIds()) {
      CardConfig config = cardService.getCard(cardId).orElse(null);
      if (config == null) {
        logger.error("Card reward references missing card {}", cardId);
        return;
      }
      offeredConfigs.add(config);
    }

    cardRewardCommitted = true;
    if (rewardUi != null) rewardUi.remove();

    float panelWidth = Math.min(stage.getWidth() * 0.82f, 1100f);
    float panelHeight = Math.min(panelWidth * 0.625f, stage.getHeight() * 0.9f);
    Stack panel = new Stack();
    Image frame = new Image(texture(PANEL_TEXTURE));
    frame.setScaling(Scaling.stretch);
    panel.add(frame);

    Table content = new Table();
    content.top();
    content.pad(panelHeight * 0.10f, panelWidth * 0.06f, panelHeight * 0.08f, panelWidth * 0.06f);
    Image victory = new Image(texture(VICTORY_TITLE_TEXTURE));
    victory.setScaling(Scaling.fit);
    content.add(victory).size(panelWidth * 0.34f, panelHeight * 0.12f).center();
    content.row();
    Label subtitle = new Label("CHOOSE ONE CARD", largeStyle(GOLD));
    subtitle.setFontScale(0.82f);
    content.add(subtitle).center().padTop(4f).padBottom(panelHeight * 0.04f);
    content.row();

    Table choices = new Table();
    choices.defaults().pad(8f);
    cardChoiceButtons.clear();
    cardWidgets.clear();
    for (CardConfig config : offeredConfigs) {
      String cardId = config.id;
      ResolvedCard resolved = cardResolver.resolveBasePreview(config, "reward-preview-" + cardId);
      CardWidget widget = new CardWidget(resolved, cardWidgetAssets);
      Button button = new Button(skin);
      button.add(widget).size(CardWidget.CARD_WIDTH, CardWidget.CARD_HEIGHT);
      button.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              claimCard(option, cardId);
            }
          });
      button.addListener(
          new ClickListener() {
            @Override
            public boolean touchDown(
                InputEvent event, float x, float y, int pointer, int buttonCode) {
              event.stop();
              return super.touchDown(event, x, y, pointer, buttonCode);
            }
          });
      cardChoiceButtons.add(button);
      cardWidgets.add(widget);
      choices.add(button);
    }
    content.add(choices).expand().center();
    panel.add(content);
    Table root = new Table();
    root.setFillParent(true);
    root.add(panel).width(panelWidth).height(panelHeight);
    cardSelectionUi = root;
    stage.addActor(root);
    cardDiscoveryService.recordSeenAll(option.cardSelection.cardIds());
  }

  private void claimCard(RewardOption option, String cardId) {
    if (disposed || claimed || claimInProgress || runState == null) return;
    claimInProgress = true;
    if (cardSelectionUi != null) cardSelectionUi.setTouchable(Touchable.disabled);
    try {
      rewardService.claimRunReward(runState, option, cardId);
      completeReward();
    } catch (RuntimeException exception) {
      claimInProgress = false;
      if (cardSelectionUi != null) cardSelectionUi.setTouchable(Touchable.enabled);
      logger.error("Could not claim card reward {}", cardId, exception);
    }
  }

  private void completeReward() {
    if (claimed) return;
    claimed = true;
    if (rewardUi != null) rewardUi.setTouchable(Touchable.disabled);
    if (cardSelectionUi != null) cardSelectionUi.setTouchable(Touchable.disabled);
    afterRewardApplied.run();
    entity.getEvents().trigger(REWARD_CLAIMED_EVENT);
    entity.getEvents().trigger(EndBattleDisplay.RETURN_TO_MENU_EVENT);
  }

  @Override
  public void dispose() {
    disposed = true;
    if (background != null) {
      background.remove();
    }
    if (scrim != null) {
      scrim.remove();
    }
    if (rewardUi != null) rewardUi.remove();
    if (cardSelectionUi != null) cardSelectionUi.remove();
    super.dispose();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Positioning is handled by the table layout.
  }

  public List<Actor> getRewardOptionCards() {
    return List.copyOf(rewardOptionCards);
  }

  List<RewardOption> getOptions() {
    return options == null ? List.of() : List.copyOf(options);
  }

  boolean isCardRewardCommitted() {
    return cardRewardCommitted;
  }

  boolean isClaimed() {
    return claimed;
  }

  List<Button> getCardChoiceButtons() {
    return List.copyOf(cardChoiceButtons);
  }

  List<CardWidget> getCardWidgets() {
    return List.copyOf(cardWidgets);
  }
}
