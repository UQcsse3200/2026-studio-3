package com.csse3200.game.components.spritedisplay.displaying;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.CardAcquisitionPool;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.CardUnlockState;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.components.cards.CardWidget;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rewards.ItemType;
import com.csse3200.game.rewards.RewardGenerator;
import com.csse3200.game.rewards.RewardOption;
import com.csse3200.game.rewards.RewardService;
import com.csse3200.game.rewards.RewardType;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class RewardDisplayTest {
  private Stage stage;
  private Entity entity;
  private CardService cardService;
  private CardDiscoveryService discovery;
  private RunState runState;
  private GdxGame game;

  @BeforeEach
  void setUp() {
    RenderService renderService = new RenderService();
    stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    renderService.setStage(stage);

    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerEntityService(new EntityService());

    ResourceService resources = mock(ResourceService.class);
    Texture texture = mock(Texture.class);

    when(texture.getWidth()).thenReturn(450);
    when(texture.getHeight()).thenReturn(912);
    when(resources.containsAsset(anyString(), eq(Texture.class))).thenReturn(true);
    when(resources.getAsset(anyString(), eq(Texture.class))).thenReturn(texture);

    ServiceLocator.registerResourceService(resources);

    cardService = new CardLibrary(CardConfigLoader.loadCards());
    discovery = new CardDiscoveryService(CardConfigLoader.loadCards());

    runState = new RunState();
    game = mock(GdxGame.class);

    when(game.getRunState()).thenReturn(runState);
    when(game.getBackgroundId()).thenReturn("dungeon");
  }

  @AfterEach
  void tearDown() {
    if (entity != null) {
      entity.dispose();
    }

    if (stage != null) {
      stage.dispose();
    }
  }

  @Test
  void createsPanelRewardCardsAndKeepsCardCandidatesHiddenUntilSelected() {
    RewardDisplay display = createDisplay(defaultService());

    assertEquals(3, display.getOptions().size());
    assertEquals(3, display.getRewardOptionCards().size());

    assertEquals(
        List.of(RewardType.GOLD, RewardType.ITEM, RewardType.CARD),
        display.getOptions().stream().map(option -> option.type).toList());

    RewardOption option = cardOption(display);

    assertTrue(
        option.cardSelection.cardIds().stream()
            .allMatch(id -> discovery.getProgressSnapshot().get(id) == CardUnlockState.LOCKED));

    assertFalse(display.isCardRewardCommitted());
    assertFalse(display.isClaimed());
  }

  @Test
  void victoryPanelKeepsItsLogicalSizeAcrossViewportWidths() {
    stage.getViewport().update(1280, 800, true);
    RewardDisplay first = createDisplay(defaultService());
    Table firstRoot = (Table) first.getRewardUi();
    firstRoot.validate();
    float panelWidth = firstRoot.getCells().first().getActor().getWidth();
    float panelHeight = firstRoot.getCells().first().getActor().getHeight();
    float cardWidth = first.getRewardOptionCards().getFirst().getWidth();
    float cardHeight = first.getRewardOptionCards().getFirst().getHeight();
    assertTrue(cardWidth > 0f);
    assertTrue(cardHeight > 0f);
    assertEquals(1222f * 800f / 941f, panelWidth, 0.5f);
    assertEquals(876f * 800f / 941f, panelHeight, 0.5f);

    entity.dispose();
    entity = null;
    stage.getViewport().update(1920, 1080, true);
    RewardDisplay second = createDisplay(defaultService());
    Table secondRoot = (Table) second.getRewardUi();
    secondRoot.validate();
    assertEquals(panelWidth, secondRoot.getCells().first().getActor().getWidth(), 0.01f);
    assertEquals(panelHeight, secondRoot.getCells().first().getActor().getHeight(), 0.01f);
    assertEquals(cardWidth, second.getRewardOptionCards().getFirst().getWidth(), 0.01f);
    assertEquals(cardHeight, second.getRewardOptionCards().getFirst().getHeight(), 0.01f);
  }

  @Test
  void rewardDestinationIconsKeepTheirSizeAfterViewportResize() {
    stage.getViewport().update(1280, 800, true);
    RewardDisplay display = createDisplay(defaultService());
    RewardClaimFeedback feedback = display.getClaimFeedback();
    assertNotNull(feedback);
    float iconWidth = feedback.getDestination(RewardType.ITEM).getWidth();
    float iconHeight = feedback.getDestination(RewardType.ITEM).getHeight();

    stage.getViewport().update(1920, 1080, true);
    stage.act(0f);

    assertEquals(56f, iconWidth, 0.01f);
    assertEquals(56f, iconHeight, 0.01f);
    assertEquals(iconWidth, feedback.getDestination(RewardType.ITEM).getWidth(), 0.01f);
    assertEquals(iconHeight, feedback.getDestination(RewardType.ITEM).getHeight(), 0.01f);
    assertEquals(24f, feedback.getDestination(RewardType.ITEM).getX(), 0.01f);
    assertEquals(stage.getHeight() - 239f, feedback.getDestination(RewardType.ITEM).getY(), 0.01f);
  }

  @Test
  void itemClaimLeavesOnlyAThinAmberTrailWithoutChangingTheFlight() {
    stage.getViewport().update(1280, 800, true);
    RewardDisplay display = createDisplay(defaultService());
    RewardClaimFeedback feedback = display.getClaimFeedback();

    display.selectOption(display.getOptions().get(1));
    stage.act(0.13f);

    Actor trail = feedback.getOverlay().findActor("rewardTrailITEM");
    assertInstanceOf(Image.class, trail);
    assertTrue(trail.getWidth() <= 6f);
    assertTrue(trail.getHeight() <= 7f);
    Color tint = trail.getColor();
    Color amber = Color.valueOf("FFC27A");
    assertEquals(amber.r, tint.r, 0.01f);
    assertEquals(amber.g, tint.g, 0.01f);
    assertEquals(amber.b, tint.b, 0.01f);
    assertNotNull(feedback.getOverlay().findActor("rewardGlowITEM"));
    assertNotNull(feedback.getOverlay().findActor("rewardStarITEM"));
    assertTrue(feedback.getOverlay().getChildren().size > 20);
    assertEquals(56f, feedback.getDestination(RewardType.ITEM).getWidth(), 0.01f);
    assertTrue(display.isClaimed());
  }

  @Test
  void goldAndCardClaimsUseTheirOwnShortTrailColours() {
    RewardDisplay goldDisplay = createDisplay(defaultService());
    goldDisplay.selectOption(goldDisplay.getOptions().getFirst());
    stage.act(0.13f);
    Actor goldTrail = goldDisplay.getClaimFeedback().getOverlay().findActor("rewardTrailGOLD");
    assertInstanceOf(Image.class, goldTrail);
    assertTrailColour(goldTrail, "FFE7A1");

    entity.dispose();
    entity = null;
    RewardDisplay cardDisplay = createDisplay(defaultService());
    cardDisplay.selectOption(cardOption(cardDisplay));
    cardDisplay
        .getCardChoiceButtons()
        .getFirst()
        .fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());
    stage.act(0.13f);
    Actor cardTrail = cardDisplay.getClaimFeedback().getOverlay().findActor("rewardTrailCARD");
    assertInstanceOf(Image.class, cardTrail);
    assertTrailColour(cardTrail, "C6E9FF");
  }

  private static void assertTrailColour(Actor trail, String hex) {
    Color expected = Color.valueOf(hex);
    assertEquals(expected.r, trail.getColor().r, 0.01f);
    assertEquals(expected.g, trail.getColor().g, 0.01f);
    assertEquals(expected.b, trail.getColor().b, 0.01f);
  }

  @Test
  void openingCardRewardShowsWidgetsAndMarksCandidatesSeen() {
    RewardDisplay display = createDisplay(defaultService());
    RewardOption option = cardOption(display);

    RewardClaimFeedback feedback = display.getClaimFeedback();
    assertNotNull(feedback);
    assertEquals(24f, feedback.getDestination(RewardType.CARD).getX());
    assertEquals(56f, feedback.getDestination(RewardType.CARD).getWidth());
    assertEquals(56f, feedback.getDestination(RewardType.ITEM).getWidth());
    assertEquals(56f, feedback.getDestination(RewardType.GOLD).getWidth());
    assertEquals(stage.getHeight() - 180f, feedback.getDestination(RewardType.CARD).getY());
    assertEquals(stage.getHeight() - 239f, feedback.getDestination(RewardType.ITEM).getY());
    assertEquals(stage.getHeight() - 299f, feedback.getDestination(RewardType.GOLD).getY());

    PlayerDeck deck = runState.getOrCreatePlayerDeck(cardService);
    int initialDeckSize = deck.size();

    AtomicInteger discoveryEvents = new AtomicInteger();

    discovery
        .getEvents()
        .addListener(
            CardDiscoveryService.ENTRY_UPDATED_EVENT, ignored -> discoveryEvents.incrementAndGet());

    display.selectOption(option);

    assertSame(stage, feedback.getOverlay().getStage());

    assertTrue(display.isCardRewardCommitted());
    assertFalse(display.isClaimed());
    assertEquals(initialDeckSize, deck.size());

    assertEquals(option.cardSelection.cardIds().size(), display.getCardChoiceButtons().size());

    assertEquals(option.cardSelection.cardIds().size(), display.getCardWidgets().size());

    assertTrue(
        option.cardSelection.cardIds().stream()
            .allMatch(id -> discovery.getProgressSnapshot().get(id) == CardUnlockState.SEEN));

    assertEquals(option.cardSelection.cardIds().size(), discoveryEvents.get());

    for (CardWidget widget : display.getCardWidgets()) {
      assertFalse(widget.getCard().upgraded());

      assertEquals(
          cardService.getCard(widget.getCard().cardId()).orElseThrow().name,
          widget.getCard().name());
    }
  }

  @Test
  void claimingCardUpdatesDeckAndRunsCheckpointBeforeNavigation() {
    PlayerDeck deck = runState.getOrCreatePlayerDeck(cardService);
    int initialDeckSize = deck.size();

    AtomicInteger checkpointCalls = new AtomicInteger();
    AtomicBoolean checkpointComplete = new AtomicBoolean();
    AtomicInteger claimEvents = new AtomicInteger();
    AtomicInteger navigationEvents = new AtomicInteger();

    RewardDisplay display =
        createDisplay(
            defaultService(),
            () -> {
              assertEquals(initialDeckSize + 1, deck.size());
              checkpointCalls.incrementAndGet();
              checkpointComplete.set(true);
            });

    entity
        .getEvents()
        .addListener(
            RewardDisplay.REWARD_CLAIMED_EVENT,
            () -> {
              assertTrue(checkpointComplete.get());
              claimEvents.incrementAndGet();
            });

    entity
        .getEvents()
        .addListener(
            EndBattleDisplay.RETURN_TO_MENU_EVENT,
            () -> {
              assertTrue(checkpointComplete.get());
              navigationEvents.incrementAndGet();
            });

    display.selectOption(cardOption(display));

    Button selectedCard = display.getCardChoiceButtons().getFirst();

    selectedCard.fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());
    selectedCard.fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());

    assertTrue(display.isClaimed());
    assertEquals(initialDeckSize + 1, deck.size());
    assertEquals(1, checkpointCalls.get());
    assertEquals(1, claimEvents.get());
    assertEquals(0, navigationEvents.get());
    stage.act(0.7f);
    display.finishFeedbackAndNavigate();
    assertEquals(0, navigationEvents.get());
    stage.act(0.6f);
    stage.act(0.01f);
    display.update();
    assertEquals(0, navigationEvents.get());
    display.finishFeedbackAndNavigate();
    assertEquals(1, navigationEvents.get());
  }

  @Test
  void selectingGoldOrItemClaimsRewardWithoutRevealingCardCandidates() {
    RewardDisplay display = createDisplay(defaultService());
    List<String> candidateIds = cardOption(display).cardSelection.cardIds();

    display.selectOption(display.getOptions().getFirst());

    assertTrue(display.isClaimed());
    assertFalse(display.isCardRewardCommitted());

    assertTrue(
        candidateIds.stream()
            .allMatch(id -> discovery.getProgressSnapshot().get(id) == CardUnlockState.LOCKED));
  }

  @Test
  void goldClaimConsumesOneLuckyCoinAndSavesBeforeFeedbackCompletes() {
    runState.getOrCreatePlayerState().addGold(75);
    runState.getOrCreatePlayerState().addOwnedItem(ItemType.LUCKY_COIN);
    runState.getOrCreatePlayerState().addOwnedItem(ItemType.LUCKY_COIN);
    AtomicInteger checkpoints = new AtomicInteger();
    AtomicInteger navigationEvents = new AtomicInteger();
    RewardDisplay display =
        createDisplay(
            defaultService(),
            () -> {
              assertEquals(165, runState.getOrCreatePlayerState().getGold());
              assertEquals(
                  1, runState.getOrCreatePlayerState().getOwnedItemCount(ItemType.LUCKY_COIN));
              checkpoints.incrementAndGet();
            });
    entity
        .getEvents()
        .addListener(EndBattleDisplay.RETURN_TO_MENU_EVENT, navigationEvents::incrementAndGet);

    display.selectOption(display.getOptions().getFirst());
    display.selectOption(display.getOptions().get(1));
    display.finishFeedbackAndNavigate();

    assertTrue(display.isClaimed());
    assertEquals(165, runState.getOrCreatePlayerState().getGold());
    assertEquals(1, checkpoints.get());
    assertEquals(0, navigationEvents.get());
    stage.act(0.85f);
    stage.act(0.37f);
    stage.act(0.01f);
    display.finishFeedbackAndNavigate();
    assertEquals(1, navigationEvents.get());
  }

  @Test
  void itemClaimAddsOnlySelectedItemAndSavesBeforeNavigation() {
    AtomicInteger checkpoints = new AtomicInteger();
    RewardDisplay display =
        createDisplay(
            defaultService(),
            () -> {
              assertEquals(
                  1, runState.getOrCreatePlayerState().getOwnedItemCount(ItemType.ENERGY_CRYSTAL));
              checkpoints.incrementAndGet();
            });

    display.selectOption(display.getOptions().get(1));

    assertTrue(display.isClaimed());
    assertEquals(1, checkpoints.get());
    assertEquals(1, runState.getOrCreatePlayerState().getOwnedItemCount(ItemType.ENERGY_CRYSTAL));
    assertEquals(50, runState.getOrCreatePlayerState().getGold());
    assertFalse(display.isCardRewardCommitted());
  }

  @Test
  void failedGoldClaimDoesNotSaveOrNavigateAndCanBeRetried() {
    AtomicInteger attempts = new AtomicInteger();
    AtomicInteger checkpoints = new AtomicInteger();
    RewardService service =
        new RewardService(fixedRewardGenerator(), cardService, new Random(11)) {
          @Override
          public void claimRunReward(RunState state, RewardOption selected, String selectedCardId) {
            if (attempts.getAndIncrement() == 0) {
              throw new IllegalStateException("simulated gold claim failure");
            }
            super.claimRunReward(state, selected, selectedCardId);
          }
        };
    RewardDisplay display = createDisplay(service, checkpoints::incrementAndGet);
    RewardOption gold = display.getOptions().getFirst();
    int startingGold = runState.getOrCreatePlayerState().getGold();

    display.selectOption(gold);
    display.finishFeedbackAndNavigate();
    assertFalse(display.isClaimed());
    assertEquals(startingGold, runState.getOrCreatePlayerState().getGold());
    assertEquals(0, checkpoints.get());

    display.selectOption(gold);
    assertTrue(display.isClaimed());
    assertEquals(startingGold + gold.goldAmount, runState.getOrCreatePlayerState().getGold());
    assertEquals(1, checkpoints.get());
    assertEquals(2, attempts.get());
  }

  @Test
  void failedPostClaimCallbackStillFinishesWithoutDuplicatingReward() {
    AtomicInteger callbackCalls = new AtomicInteger();
    AtomicInteger claimEvents = new AtomicInteger();
    AtomicInteger navigationEvents = new AtomicInteger();
    RewardDisplay display =
        createDisplay(
            defaultService(),
            () -> {
              callbackCalls.incrementAndGet();
              throw new IllegalStateException("simulated checkpoint failure");
            });
    entity
        .getEvents()
        .addListener(RewardDisplay.REWARD_CLAIMED_EVENT, claimEvents::incrementAndGet);
    entity
        .getEvents()
        .addListener(EndBattleDisplay.RETURN_TO_MENU_EVENT, navigationEvents::incrementAndGet);

    RewardOption gold = display.getOptions().getFirst();
    int startingGold = runState.getOrCreatePlayerState().getGold();
    display.selectOption(gold);
    display.selectOption(gold);

    assertTrue(display.isClaimed());
    assertEquals(startingGold + gold.goldAmount, runState.getOrCreatePlayerState().getGold());
    assertEquals(1, callbackCalls.get());
    assertEquals(1, claimEvents.get());
    stage.act(0.85f);
    stage.act(0.37f);
    stage.act(0.01f);
    display.finishFeedbackAndNavigate();
    assertEquals(1, navigationEvents.get());
  }

  @Test
  void emptyCardPoolLeavesOnlyGoldAndItemPanelCards() {
    CardAcquisitionPool emptyPool = new CardAcquisitionPool(cardService, List.of());

    RewardService service =
        new RewardService(fixedRewardGenerator(), cardService, emptyPool, new Random(7));

    RewardDisplay display = createDisplay(service);

    assertEquals(2, display.getOptions().size());
    assertEquals(2, display.getRewardOptionCards().size());
    Table root = (Table) display.getRewardUi();
    root.validate();
    assertEquals(
        1222f * 800f / 941f * 0.215f, display.getRewardOptionCards().getFirst().getWidth(), 1f);

    assertTrue(display.getOptions().stream().noneMatch(option -> option.type == RewardType.CARD));
  }

  @Test
  void cardClaimFailureCanBeRetriedWithoutMutatingDeck() {
    AtomicInteger attempts = new AtomicInteger();

    RewardService failingService =
        new RewardService(fixedRewardGenerator(), cardService, new Random(11)) {
          @Override
          public void claimRunReward(RunState state, RewardOption selected, String selectedCardId) {
            attempts.incrementAndGet();
            throw new IllegalStateException("simulated card claim failure");
          }
        };

    RewardDisplay display = createDisplay(failingService);
    int initialDeckSize = runState.getOrCreatePlayerDeck(cardService).size();

    AtomicInteger navigationEvents = new AtomicInteger();

    entity
        .getEvents()
        .addListener(EndBattleDisplay.RETURN_TO_MENU_EVENT, navigationEvents::incrementAndGet);

    display.selectOption(cardOption(display));

    Button selectedCard = display.getCardChoiceButtons().getFirst();

    selectedCard.fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());
    selectedCard.fire(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent());

    assertEquals(2, attempts.get());
    assertFalse(display.isClaimed());
    assertEquals(initialDeckSize, runState.getOrCreatePlayerDeck(cardService).size());
    assertEquals(0, navigationEvents.get());
  }

  @Test
  void constructorRequiresGameAndRewardDependencies() {
    DisplayingRecord displayingRecord = DisplayingRecord.builder("").variant("reward").build();
    RewardService service = defaultService();

    assertThrows(
        NullPointerException.class,
        () -> new RewardDisplay(displayingRecord, null, game, cardService, discovery));
    assertThrows(
        NullPointerException.class,
        () -> new RewardDisplay(displayingRecord, service, null, cardService, discovery));
    assertThrows(
        NullPointerException.class,
        () -> new RewardDisplay(displayingRecord, service, game, null, discovery));
    assertThrows(
        NullPointerException.class,
        () -> new RewardDisplay(displayingRecord, service, game, cardService, null));

    assertThrows(
        NullPointerException.class,
        () -> new RewardDisplay(displayingRecord, service, game, cardService, discovery, null));
  }

  private RewardDisplay createDisplay(RewardService service) {
    return createDisplay(service, () -> {});
  }

  private RewardDisplay createDisplay(RewardService service, Runnable afterRewardApplied) {
    RewardDisplay display =
        new RewardDisplay(
            DisplayingRecord.builder("").variant("reward").build(),
            service,
            game,
            cardService,
            discovery,
            afterRewardApplied);

    entity = new Entity().addComponent(display);
    entity.create();

    return display;
  }

  private RewardService defaultService() {
    return new RewardService(fixedRewardGenerator(), cardService, new Random(11));
  }

  private RewardGenerator fixedRewardGenerator() {
    return new RewardGenerator(new Random(1)) {
      @Override
      public RewardOption generateGoldRewardOption(float goldBonusMultiplier) {
        return RewardOption.gold(25);
      }

      @Override
      public RewardOption generateItemRewardOption() {
        return RewardOption.item(ItemType.ENERGY_CRYSTAL);
      }
    };
  }

  private static RewardOption cardOption(RewardDisplay display) {
    return display.getOptions().stream()
        .filter(option -> option.type == RewardType.CARD)
        .findFirst()
        .orElseThrow();
  }
}
