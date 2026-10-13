package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
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
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.services.audio.AudioService;
import com.csse3200.game.services.audio.SoundId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RewardDisplay extends Displaying {
  public static final String REWARD_CLAIMED_EVENT = "rewardClaimed";

  private static final Logger logger = LoggerFactory.getLogger(RewardDisplay.class);

  private static final String BACKGROUND_TEXTURE = "images/ui/victory-background-approved.png";
  private static final String PANEL_TEXTURE = "images/ui/reward-panel-mythic-cords-aged.png";
  private static final String CARD_TEXTURE = "images/ui/reward-card-mythic-aged.png";
  private static final String GOLD_TEXTURE = "images/ui/gold-reward.png";
  private static final String VICTORY_TITLE_TEXTURE = "images/ui/victory-title-mythic.png";
  private static final float REFERENCE_WIDTH = 1672f;
  private static final float REFERENCE_HEIGHT = 941f;
  private static final float REFERENCE_PANEL_WIDTH = 1222f;
  private static final float REFERENCE_PANEL_HEIGHT = 876f;
  private static final int[] VICTORY_LETTER_X = {0, 252, 526, 817, 1120, 1390, 1730, 2170};
  private static final float PANEL_TEXTURE_WIDTH = 1584f;
  private static final float PANEL_TEXTURE_HEIGHT = 993f;
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
          ItemType.WARRIORS_CREST, "images/ui/warriors-crest.png",
          ItemType.ASHEN_EMBER, "images/ui/ashen-ember.png",
          ItemType.BRAND_OF_THE_DAMNED, "images/ui/brand-of-the-damned.png",
          ItemType.SHATTERED_AEGIS, "images/ui/shattered-aegis.png");

  private final RunState runState;
  private final GdxGame game;
  private final RewardService rewardService;
  private final CardService cardService;
  private final CardDiscoveryService cardDiscoveryService;
  private final Runnable afterRewardApplied;
  private final CardResolver cardResolver = new CardResolver();
  private final List<TextButton> optionButtons = new ArrayList<>();
  private final List<Button> cardChoiceButtons = new ArrayList<>();
  private final List<CardWidget> cardWidgets = new ArrayList<>();
  private List<RewardOption> options;
  private boolean claimed;
  private boolean goldRewardUsesLuckyCoin;
  private boolean cardRewardCommitted;
  private boolean claimInProgress;
  private boolean created;
  private boolean disposed;
  private Actor background;
  private Actor scrimActor;
  private Actor rewardUi;
  private Actor victoryTitle;
  private Actor victorySubtitle;
  private Actor sunPulse;
  private Table cardSelectionTable;
  private CardWidgetAssets cardWidgetAssets;
  private final List<Actor> rewardOptionCards = new ArrayList<>();
  private final List<Actor> victoryTitleLetters = new ArrayList<>();
  private final List<Actor> victoryParticles = new ArrayList<>();
  private VictoryAnimationController victoryAnimationController;

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

    options = rewardService.generateRewardOptions();
    cardWidgetAssets =
        CardWidgetAssets.fromManagedResources(skin, ServiceLocator.getResourceService());
    createCompatibilityButtons();
    buildRewardScreen();
  }

  /** Keeps the interaction API used by card reward tests while the visible UI remains themed. */
  private void createCompatibilityButtons() {
    optionButtons.clear();
    for (RewardOption option : options) {
      if (option == null) {
        continue;
      }
      TextButton button = new TextButton(describeOption(option), skin);
      button.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              switch (option.type) {
                case GOLD -> AudioService.playSound(SoundId.ITEM_PURCHASE, 0.5f);
                case ITEM -> AudioService.playSound(SoundId.ITEM_PICKUP, 0.5f);
                case CARD -> AudioService.playSound(SoundId.CARD_SHUFFLE, 0.5f);
              }
              selectOption(option);
            }
          });
      optionButtons.add(button);
    }
  }

  private void buildRewardScreen() {
    if (options == null || options.isEmpty()) {
      return;
    }

    Image scene =
        new Image(ServiceLocator.getResourceService().getAsset(BACKGROUND_TEXTURE, Texture.class));

    scene.setScaling(Scaling.fill);
    scene.setFillParent(true);
    background = scene;
    stage.addActor(scene);
    scene.toBack();

    Image dimmer = new Image(skin.newDrawable("white", SCRIM));
    dimmer.setFillParent(true);
    scrimActor = dimmer;
    stage.addActor(dimmer);

    // The approved mock-up is 1672 x 941. Scale both axes from that single reference scale so the
    // frame keeps the exact 1222 x 876 design proportion on every viewport.
    float referenceScale =
        Math.min(stage.getWidth() / REFERENCE_WIDTH, stage.getHeight() / REFERENCE_HEIGHT);
    float panelWidth = REFERENCE_PANEL_WIDTH * referenceScale;
    float panelHeight = REFERENCE_PANEL_HEIGHT * referenceScale;

    Stack panel = new Stack();
    Image panelFrame = image(PANEL_TEXTURE);
    panelFrame.setScaling(Scaling.stretch);
    panel.add(panelFrame);
    panel.add(createVictoryEffects(panelWidth, panelHeight));
    panel.add(createPanelContent(panelWidth, panelHeight));

    Table root = new Table();
    root.setFillParent(true);
    root.add(panel).width(panelWidth).height(panelHeight);
    rewardUi = root;
    stage.addActor(root);

    victoryAnimationController =
        new VictoryAnimationController(
            background,
            scrimActor,
            rewardUi,
            victoryTitle,
            victoryTitleLetters,
            victorySubtitle,
            sunPulse,
            victoryParticles,
            rewardOptionCards);
    victoryAnimationController.playEntrance();
  }

  private Group createVictoryEffects(float panelWidth, float panelHeight) {
    Group effects = new Group();
    effects.setSize(panelWidth, panelHeight);
    effects.setTouchable(Touchable.disabled);

    Texture panelTexture =
        ServiceLocator.getResourceService().getAsset(PANEL_TEXTURE, Texture.class);
    TextureRegion crestRegion = new TextureRegion(panelTexture, 620, 0, 344, 245);
    Image crestGlow = new Image(crestRegion);
    crestGlow.setBounds(
        panelWidth * 620f / PANEL_TEXTURE_WIDTH,
        panelHeight * (PANEL_TEXTURE_HEIGHT - 245f) / PANEL_TEXTURE_HEIGHT,
        panelWidth * 344f / PANEL_TEXTURE_WIDTH,
        panelHeight * 245f / PANEL_TEXTURE_HEIGHT);
    crestGlow.setTouchable(Touchable.disabled);
    sunPulse = crestGlow;
    effects.addActor(crestGlow);

    float[][] particlePositions = {
      {0.39f, 0.72f},
      {0.43f, 0.68f},
      {0.47f, 0.74f},
      {0.53f, 0.70f},
      {0.57f, 0.75f},
      {0.61f, 0.69f},
      {0.45f, 0.64f},
      {0.55f, 0.65f}
    };
    victoryParticles.clear();
    for (int index = 0; index < particlePositions.length; index++) {
      float size = index % 3 == 0 ? 4f : 3f;
      Image particle = new Image(skin.newDrawable("white", new Color(0.75f, 0.5f, 0.22f, 1f)));
      particle.setBounds(
          panelWidth * particlePositions[index][0],
          panelHeight * particlePositions[index][1],
          size,
          size);
      particle.setTouchable(Touchable.disabled);
      victoryParticles.add(particle);
      effects.addActor(particle);
    }
    return effects;
  }

  private Table createPanelContent(float panelWidth, float panelHeight) {
    Table content = new Table();
    content.top();
    content.pad(
        panelHeight * 0.215f, panelWidth * 0.115f, panelHeight * 0.16f, panelWidth * 0.115f);

    float titleWidth = panelWidth * 0.506f;
    float titleHeight = panelHeight * 0.114f;
    Group victory = createVictoryTitle(titleWidth, titleHeight);
    victoryTitle = victory;
    content.add(victory).size(titleWidth, titleHeight).center();
    content.row();

    Label subtitle = new Label("CHOOSE ONE REWARD", largeStyle(GOLD));
    subtitle.setFontScale(0.82f);
    victorySubtitle = subtitle;
    content.add(subtitle).center().padTop(2f).padBottom(panelHeight * 0.015f);
    content.row();

    Table cards = new Table();
    cards.defaults().padLeft(panelWidth * 0.011f).padRight(panelWidth * 0.011f);

    float cardWidth = panelWidth * (options.size() > 2 ? 0.215f : 0.25f);
    float cardHeight = panelHeight * 0.395f;

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

  private Group createVictoryTitle(float titleWidth, float titleHeight) {
    Texture texture =
        ServiceLocator.getResourceService().getAsset(VICTORY_TITLE_TEXTURE, Texture.class);
    Group title = new Group();
    title.setSize(titleWidth, titleHeight);
    title.setTouchable(Touchable.disabled);
    victoryTitleLetters.clear();

    for (int index = 0; index < VICTORY_LETTER_X.length - 1; index++) {
      int sourceX = VICTORY_LETTER_X[index];
      int sourceWidth = VICTORY_LETTER_X[index + 1] - sourceX;
      Image letter =
          new Image(new TextureRegion(texture, sourceX, 0, sourceWidth, texture.getHeight()));
      letter.setBounds(
          titleWidth * sourceX / texture.getWidth(),
          0f,
          titleWidth * sourceWidth / texture.getWidth(),
          titleHeight);
      letter.setTouchable(Touchable.disabled);
      victoryTitleLetters.add(letter);
      title.addActor(letter);
    }
    return title;
  }

  private Stack createRewardCard(RewardOption option, float cardWidth, float cardHeight) {
    Stack card = new Stack();
    Image frame = image(CARD_TEXTURE);
    frame.setScaling(Scaling.stretch);
    frame.setTouchable(Touchable.disabled);
    card.add(frame);

    Table details = new Table();
    details.top();
    details.setTouchable(Touchable.disabled);
    details.pad(cardHeight * 0.115f, cardWidth * 0.075f, cardHeight * 0.055f, cardWidth * 0.075f);

    Label name = new Label(rewardTitle(option), largeStyle(GOLD));
    name.setFontScale(isLongRewardName(option) ? 0.58f : 0.76f);
    name.setAlignment(com.badlogic.gdx.utils.Align.center);
    details.add(name).width(cardWidth * 0.86f).center();
    details.row();

    Image icon = image(rewardIcon(option));
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
        .width(cardWidth * 0.88f)
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
            if (victoryAnimationController != null) {
              victoryAnimationController.showHover(card);
            }
          }

          @Override
          public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
            frame.setColor(Color.WHITE);
            if (victoryAnimationController != null) {
              victoryAnimationController.clearHover(card);
            }
          }
        });
    return card;
  }

  private String describeOption(RewardOption option) {
    if (option == null || option.type == null) {
      return "";
    }
    return switch (option.type) {
      case GOLD -> option.goldAmount + " Gold";
      case ITEM ->
          option.itemId == null
              ? "Item"
              : ItemFormatting.formatItemName(option.itemId)
                  + switch (option.itemId) {
                    case LUCKY_COIN -> " (+10% Gold)";
                    case ENERGY_CRYSTAL -> " (+1 Max Energy)";
                    case MERCHANTS_FAVOR -> " (+10% Shop Discount)";
                    case IRON_AEGIS -> " (+5 Armour when used)";
                    case WARRIORS_CREST -> " (+1 Strength when used)";
                    case ASHEN_EMBER -> " (-1 Max Energy)";
                    case BRAND_OF_THE_DAMNED -> " (-1 from every Strength gain)";
                    case SHATTERED_AEGIS -> " (-1 from every Block gain)";
                    default -> "";
                  };
      case CARD -> "Choose a Card";
    };
  }

  private String rewardTitle(RewardOption option) {
    if (option == null || option.type == null) {
      return "";
    }
    return switch (option.type) {
      case GOLD -> option.goldAmount + luckyCoinBonus(option) + " GOLD";
      case ITEM -> option.itemId == null ? "ITEM" : ItemFormatting.formatItemName(option.itemId);
      case CARD -> "CARD REWARD";
    };
  }

  private String rewardDescription(RewardOption option) {
    if (option == null || option.type == null) {
      return "";
    }
    return switch (option.type) {
      case GOLD -> goldRewardDescription(option);
      case CARD -> "Choose one card\nto add to your deck";
      case ITEM ->
          option.itemId == null
              ? ""
              : switch (option.itemId) {
                case LUCKY_COIN -> "+10% Total Gold\nMaximum +20";
                case ENERGY_CRYSTAL -> "+1 Max Energy";
                case MERCHANTS_FAVOR -> "+10% Shop Discount\nMaximum 50%";
                case IRON_AEGIS -> "+5 Armour when used";
                case WARRIORS_CREST -> "+1 Strength when used";
                case ASHEN_EMBER -> "-1 Max Energy\nMinimum 1";
                case BRAND_OF_THE_DAMNED -> "Strength gains -1\nper copy | Minimum 0";
                case SHATTERED_AEGIS -> "Block gains -1\nper copy | Minimum 0";
                default -> "";
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
    return option.goldAmount + " Base + " + luckyBonus + " Shard\nTotal Gold: " + totalAfterClaim;
  }

  private int luckyCoinBonus(RewardOption option) {
    if (!goldRewardUsesLuckyCoin || runState == null || option == null) {
      return 0;
    }
    return runState.getOrCreatePlayerState().calculateLuckyCoinBonus(option.goldAmount);
  }

  private boolean isLongRewardName(RewardOption option) {
    return option != null
        && (option.type == RewardType.CARD
            || option.itemId == ItemType.MERCHANTS_FAVOR
            || option.itemId == ItemType.WARRIORS_CREST);
  }

  private String rewardIcon(RewardOption option) {
    if (option != null && option.type == RewardType.ITEM) {
      return ITEM_ICONS.getOrDefault(option.itemId, GOLD_TEXTURE);
    }
    if (option != null
        && option.type == RewardType.CARD
        && option.cardSelection != null
        && !option.cardSelection.cardIds().isEmpty()) {
      return cardService
          .getCard(option.cardSelection.cardIds().getFirst())
          .map(card -> card.texturePath)
          .filter(path -> path != null && !path.isBlank())
          .orElse(GOLD_TEXTURE);
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

  private Image image(String path) {
    ResourceService resources = ServiceLocator.getResourceService();
    Texture texture = resources == null ? null : resources.getAsset(path, Texture.class);
    return texture == null
        ? new Image(skin.newDrawable("white", new Color(0.08f, 0.06f, 0.08f, 1f)))
        : new Image(texture);
  }

  void selectOption(RewardOption option) {
    if (disposed
        || claimed
        || claimInProgress
        || cardRewardCommitted
        || option == null
        || option.type == null) {
      return;
    }
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
      goldRewardUsesLuckyCoin = false;
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
      logger.error("Cannot open a Card reward without an active run and choices");
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
    if (rewardUi != null) {
      rewardUi.remove();
    }

    cardSelectionTable = new Table();
    cardSelectionTable.setFillParent(true);
    cardSelectionTable.center();
    cardChoiceButtons.clear();
    cardWidgets.clear();
    for (CardConfig config : offeredConfigs) {
      String cardId = config.id;
      ResolvedCard resolved = cardResolver.resolveBasePreview(config, "reward-preview-" + cardId);
      CardWidget widget = new CardWidget(resolved, cardWidgetAssets);
      Button cardButton = new Button(skin);
      cardButton.setTouchable(Touchable.enabled);
      cardButton.add(widget).size(CardWidget.CARD_WIDTH, CardWidget.CARD_HEIGHT);
      cardButton.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              claimCard(option, cardId);
            }
          });
      cardButton.addListener(
          new InputListener() {
            @Override
            public boolean touchDown(
                InputEvent event, float x, float y, int pointer, int buttonCode) {
              event.stop();
              return true;
            }
          });
      cardChoiceButtons.add(cardButton);
      cardWidgets.add(widget);
      cardSelectionTable.add(cardButton).pad(12f);
    }

    cardDiscoveryService.recordSeenAll(option.cardSelection.cardIds());
    stage.addActor(cardSelectionTable);
  }

  private void claimCard(RewardOption option, String cardId) {
    if (disposed || claimed || claimInProgress || runState == null) {
      return;
    }
    claimInProgress = true;
    if (cardSelectionTable != null) {
      cardSelectionTable.setTouchable(Touchable.disabled);
    }
    try {
      rewardService.claimRunReward(runState, option, cardId);
      completeReward();
    } catch (RuntimeException exception) {
      claimInProgress = false;
      if (cardSelectionTable != null) {
        cardSelectionTable.setTouchable(Touchable.enabled);
      }
      logger.error("Could not claim card reward {}", cardId, exception);
    }
  }

  private void completeReward() {
    if (claimed) {
      return;
    }
    claimed = true;
    afterRewardApplied.run();
    entity.getEvents().trigger(REWARD_CLAIMED_EVENT);
    entity.getEvents().trigger(EndBattleDisplay.RETURN_TO_MENU_EVENT);
  }

  List<Actor> getRewardOptionCards() {
    return List.copyOf(rewardOptionCards);
  }

  @Override
  public void dispose() {
    disposed = true;
    if (victoryAnimationController != null) {
      victoryAnimationController.dispose();
    }
    if (background != null) {
      background.remove();
    }
    if (scrimActor != null) {
      scrimActor.remove();
    }
    if (rewardUi != null) {
      rewardUi.remove();
    }
    if (cardSelectionTable != null) {
      cardSelectionTable.remove();
    }
    super.dispose();
  }

  @Override
  protected void draw(SpriteBatch batch) {
    // Positioning is handled by the table layout.
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
