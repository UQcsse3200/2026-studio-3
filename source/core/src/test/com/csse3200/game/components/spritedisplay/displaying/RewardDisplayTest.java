package com.csse3200.game.components.spritedisplay.displaying;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
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
  private ResourceService resources;

  @BeforeEach
  void setUp() {
    RenderService renderService = new RenderService();
    stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerEntityService(new EntityService());

    resources = mock(ResourceService.class);
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
  }

  @AfterEach
  void tearDown() {
    if (entity != null) {
      entity.dispose();
    }
    stage.dispose();
  }

  @Test
  void createsPanelRewardCardsAndKeepsCardCandidatesHiddenUntilSelected() {
    RewardDisplay display = createDisplay(defaultService());

    assertEquals(3, display.getOptions().size());
    assertEquals(3, display.getRewardOptionCards().size());
    assertEquals(
            List.of(RewardType.GOLD, RewardType.ITEM, RewardType.CARD),
            display.getOptions().stream().map(option -> option.type).toList());

    RewardOption cardOption = cardOption(display);
    assertTrue(cardOption.cardSelection.cardIds().stream()
            .allMatch(id -> discovery.getProgressSnapshot().get(id) == CardUnlockState.LOCKED));
    assertFalse(display.isCardRewardCommitted());
  }

  @Test
  void openingCardRewardShowsWidgetsAndMarksCandidatesSeen() {
    RewardDisplay display = createDisplay(defaultService());
    RewardOption option = cardOption(display);
    PlayerDeck deck = runState.getOrCreatePlayerDeck(cardService);
    int initialDeckSize = deck.size();
    AtomicInteger discoveryEvents = new AtomicInteger();
    discovery.getEvents().addListener(
            CardDiscoveryService.ENTRY_UPDATED_EVENT, ignored -> discoveryEvents.incrementAndGet());

    display.selectOption(option);

    assertTrue(display.isCardRewardCommitted());
    assertFalse(display.isClaimed());
    assertEquals(initialDeckSize, deck.size());
    assertEquals(option.cardSelection.cardIds().size(), display.getCardChoiceButtons().size());
    assertEquals(option.cardSelection.cardIds().size(), display.getCardWidgets().size());
    assertTrue(option.cardSelection.cardIds().stream()
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

    RewardDisplay display = createDisplay(defaultService(), () -> {
      assertEquals(initialDeckSize + 1, deck.size());
      checkpointCalls.incrementAndGet();
      checkpointComplete.set(true);
    });
    entity.getEvents().addListener(
            RewardDisplay.REWARD_CLAIMED_EVENT,
            () -> {
              assertTrue(checkpointComplete.get());
              claimEvents.incrementAndGet();
            });
    entity.getEvents().addListener(
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
    assertEquals(1, navigationEvents.get());
  }

  @Test
  void selectingGoldOrItemClaimsRewardWithoutRevealingCardCandidates() {
    RewardDisplay display = createDisplay(defaultService());
    List<String> candidateIds = cardOption(display).cardSelection.cardIds();

    display.selectOption(display.getOptions().getFirst());

    assertTrue(display.isClaimed());
    assertTrue(candidateIds.stream()
            .allMatch(id -> discovery.getProgressSnapshot().get(id) == CardUnlockState.LOCKED));
  }

  @Test
  void emptyCardPoolLeavesOnlyGoldAndItemPanelCards() {
    CardAcquisitionPool emptyPool = new CardAcquisitionPool(cardService, List.of());
    RewardService service =
            new RewardService(fixedRewardGenerator(), cardService, emptyPool, new Random(7));
    RewardDisplay display = createDisplay(service);

    assertEquals(2, display.getOptions().size());
    assertEquals(2, display.getRewardOptionCards().size());
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
    entity.getEvents().addListener(
            EndBattleDisplay.RETURN_TO_MENU_EVENT, navigationEvents::incrementAndGet);

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
    DisplayingRecord record = DisplayingRecord.builder("").variant("reward").build();
    RewardService service = defaultService();

    assertThrows(NullPointerException.class,
            () -> new RewardDisplay(record, null, game, cardService, discovery));
    assertThrows(NullPointerException.class,
            () -> new RewardDisplay(record, service, null, cardService, discovery));
    assertThrows(NullPointerException.class,
            () -> new RewardDisplay(record, service, game, null, discovery));
    assertThrows(NullPointerException.class,
            () -> new RewardDisplay(record, service, game, cardService, null));
  }

  private RewardDisplay createDisplay(RewardService service) {
    return createDisplay(service, () -> {});
  }

  private RewardDisplay createDisplay(RewardService service, Runnable afterRewardApplied) {
    RewardDisplay display = new RewardDisplay(
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
