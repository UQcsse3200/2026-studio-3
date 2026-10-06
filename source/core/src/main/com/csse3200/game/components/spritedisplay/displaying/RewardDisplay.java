package com.csse3200.game.components.spritedisplay.displaying;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.cards.runtime.ResolvedCard;
import com.csse3200.game.components.cards.CardWidget;
import com.csse3200.game.components.cards.CardWidgetAssets;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rewards.RewardOption;
import com.csse3200.game.rewards.RewardService;
import com.csse3200.game.services.audio.AudioService;
import com.csse3200.game.services.audio.SoundId;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RewardDisplay extends Displaying {
  public static final String REWARD_CLAIMED_EVENT = "rewardClaimed";
  private static final Logger logger = LoggerFactory.getLogger(RewardDisplay.class);

  private final RunState runState;
  private final RewardService rewardService;
  private final CardService cardService;
  private final CardDiscoveryService cardDiscoveryService;
  private final Runnable afterRewardApplied;
  private final CardResolver cardResolver = new CardResolver();

  private List<RewardOption> options;
  private boolean claimed;
  private boolean cardRewardCommitted;
  private boolean claimInProgress;
  private boolean created;
  private boolean disposed;
  private Table optionsTable;
  private Table cardSelectionTable;
  private CardWidgetAssets cardWidgetAssets;
  private final List<TextButton> optionButtons = new ArrayList<>();
  private final List<Button> cardChoiceButtons = new ArrayList<>();
  private final List<CardWidget> cardWidgets = new ArrayList<>();

  public RewardDisplay(
      DisplayingRecord rec,
      RewardService rewardService,
      RunState runState,
      CardService cardService,
      CardDiscoveryService cardDiscoveryService) {
    this(rec, rewardService, runState, cardService, cardDiscoveryService, () -> {});
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
      CardService cardService,
      CardDiscoveryService cardDiscoveryService,
      Runnable afterRewardApplied) {
    super(rec);
    this.rewardService = Objects.requireNonNull(rewardService, "rewardService cannot be null");
    this.runState = runState;
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

    float multiplier = 0f;
    if (runState != null) {
      multiplier = runState.getOrCreatePlayerState().getGoldBonusMultiplier();
    }

    options = rewardService.generateRewardOptions(multiplier);
    cardWidgetAssets =
        CardWidgetAssets.fromManagedResources(skin, ServiceLocator.getResourceService());
    buildOptionButtons();
  }

  private void buildOptionButtons() {
    if (options == null || options.isEmpty()) {
      return;
    }

    optionsTable = new Table();
    optionsTable.setFillParent(true);
    optionsTable.top();
    optionsTable.padTop(400f);
    optionButtons.clear();

    for (RewardOption option : options) {
      if (option == null) {
        continue;
      }

      TextButton button = new TextButton(describeOption(option), skin);
      optionButtons.add(button);

      button.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
              selectOption(option);
            }
          });

      button.addListener(
          new InputListener() {
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
      case CARD -> "Choose a Card";
    };
  }

  private void selectOption(RewardOption option) {
    if (disposed
        || claimed
        || claimInProgress
        || cardRewardCommitted
        || option == null
        || option.type == null) {
      return;
    }

    if (option.type == com.csse3200.game.rewards.RewardType.CARD) {
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
    if (runState == null) {
      logger.error("Cannot open a Card reward without an active RunState");
      return;
    }
    if (option.cardSelection == null || option.cardSelection.cardIds().isEmpty()) {
      logger.error("Cannot open a Card reward without choices");
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
    if (optionsTable != null) {
      optionsTable.remove();
    }

    cardSelectionTable = new Table();
    cardSelectionTable.setFillParent(true);
    cardSelectionTable.bottom().padBottom(40f);
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

    // Hidden generation must not reveal cards. Advance discovery only once every face is ready to
    // be shown.
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
    if (optionsTable != null) {
      optionsTable.setTouchable(Touchable.disabled);
    }
    if (cardSelectionTable != null) {
      cardSelectionTable.setTouchable(Touchable.disabled);
    }
    // The reward has already mutated RunState at this point. Persist that updated state before
    // firing either event, because both can cause the reward screen to be left immediately.
    afterRewardApplied.run();
    entity.getEvents().trigger(REWARD_CLAIMED_EVENT);
    entity.getEvents().trigger(EndBattleDisplay.RETURN_TO_MENU_EVENT);
  }

  @Override
  public void dispose() {
    disposed = true;
    if (optionsTable != null) {
      optionsTable.remove();
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

  List<TextButton> getOptionButtons() {
    return List.copyOf(optionButtons);
  }

  List<Button> getCardChoiceButtons() {
    return List.copyOf(cardChoiceButtons);
  }

  List<CardWidget> getCardWidgets() {
    return List.copyOf(cardWidgets);
  }
}
