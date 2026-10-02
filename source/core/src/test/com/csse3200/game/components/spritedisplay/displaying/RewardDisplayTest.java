package com.csse3200.game.components.spritedisplay.displaying;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.csse3200.game.cards.CardAcquisitionPool;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.CardUnlockState;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.components.cards.CardWidget;
import com.csse3200.game.components.cards.CardWidgetAssets;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.extensions.GameExtension;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rewards.RewardGenerator;
import com.csse3200.game.rewards.RewardOption;
import com.csse3200.game.rewards.RewardService;
import com.csse3200.game.rewards.RewardType;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import java.util.List;
import java.util.Random;
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

  @BeforeEach
  void setUp() {
    RenderService renderService = new RenderService();
    stage = new Stage(new ScreenViewport(), mock(SpriteBatch.class));
    renderService.setStage(stage);
    ServiceLocator.registerRenderService(renderService);
    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerResourceService(mock(ResourceService.class));

    cardService = new CardLibrary(CardConfigLoader.loadCards());
    discovery = new CardDiscoveryService(CardConfigLoader.loadCards());
    runState = new RunState();
  }

  @AfterEach
  void tearDown() {
    if (entity != null) {
      entity.dispose();
    }
    stage.dispose();
  }

  @Test
  void createsMutuallyExclusiveTopLevelOptionsWithoutRevealingHiddenCards() {
    RewardDisplay display = createDisplay(defaultService());

    assertEquals(
        List.of("25 Gold", "Energy Crystal (+1 Max Energy)", "Choose a Card"), labels(display));
    RewardOption cardOption = cardOption(display);
    assertTrue(
        cardOption.cardSelection.cardIds().stream()
            .allMatch(id -> discovery.getProgressSnapshot().get(id) == CardUnlockState.LOCKED));
    assertFalse(display.isCardRewardCommitted());
  }

  @Test
  void goldAndItemClaimsDoNotRevealHiddenCardCandidates() {
    RewardDisplay goldDisplay = createDisplay(defaultService());
    List<String> goldCandidateIds = cardOption(goldDisplay).cardSelection.cardIds();

    goldDisplay.getOptionButtons().get(0).fire(new ChangeEvent());

    assertTrue(
        goldCandidateIds.stream()
            .allMatch(id -> discovery.getProgressSnapshot().get(id) == CardUnlockState.LOCKED));

    entity.dispose();
    entity = null;
    discovery = new CardDiscoveryService(CardConfigLoader.loadCards());
    runState = new RunState();
    RewardDisplay itemDisplay = createDisplay(defaultService());
    List<String> itemCandidateIds = cardOption(itemDisplay).cardSelection.cardIds();

    itemDisplay.getOptionButtons().get(1).fire(new ChangeEvent());

    assertTrue(
        itemCandidateIds.stream()
            .allMatch(id -> discovery.getProgressSnapshot().get(id) == CardUnlockState.LOCKED));
  }

  @Test
  void openingCardShowsBaseWidgetsAndMarksEveryVisibleCandidateSeen() {
    RewardDisplay display = createDisplay(defaultService());
    RewardOption cardOption = cardOption(display);
    PlayerDeck deck = runState.getOrCreatePlayerDeck(cardService);
    int initialDeckSize = deck.size();
    AtomicInteger discoveryEvents = new AtomicInteger();
    discovery
        .getEvents()
        .addListener(
            CardDiscoveryService.ENTRY_UPDATED_EVENT, ignored -> discoveryEvents.incrementAndGet());

    cardButton(display).fire(new ChangeEvent());
    cardButton(display).fire(new ChangeEvent());

    assertTrue(display.isCardRewardCommitted());
    assertFalse(display.isClaimed());
    assertEquals(initialDeckSize, deck.size());
    assertEquals(cardOption.cardSelection.cardIds().size(), display.getCardChoiceButtons().size());
    assertEquals(cardOption.cardSelection.cardIds().size(), display.getCardWidgets().size());
    assertTrue(
        cardOption.cardSelection.cardIds().stream()
            .allMatch(id -> discovery.getProgressSnapshot().get(id) == CardUnlockState.SEEN));
    assertEquals(cardOption.cardSelection.cardIds().size(), discoveryEvents.get());

    for (CardWidget widget : display.getCardWidgets()) {
      String cardId = widget.getCard().cardId();
      var config = cardService.getCard(cardId).orElseThrow();
      assertEquals(config.name, widget.getCard().name());
      assertEquals(config.description, widget.getCard().description());
      assertEquals(config.cost, widget.getCard().cost());
      assertEquals(config.rarity, widget.getCard().rarity());
      assertEquals(config.type, widget.getCard().type());
      assertFalse(widget.getCard().upgraded());
    }
  }

  @Test
  void innerFocusRewardUsesUncommonRarityFromConfiguration() {
    CardAcquisitionPool pool = new CardAcquisitionPool(cardService, List.of("inner_focus"));
    RewardDisplay display =
        createDisplay(new RewardService(fixedRewardGenerator(), cardService, pool, new Random(7)));

    cardButton(display).fire(new ChangeEvent());

    assertEquals(1, display.getCardWidgets().size());
    var card = display.getCardWidgets().getFirst().getCard();
    assertEquals("inner_focus", card.cardId());
    assertEquals(Rarity.UNCOMMON, card.rarity());
    assertEquals("Inner Focus", card.name());
    assertEquals(2, card.cost());
    assertEquals("Gain 2 Strength for the rest of combat.", card.description());
    assertFalse(card.upgraded());
    assertEquals(CardUnlockState.SEEN, discovery.getProgressSnapshot().get("inner_focus"));
  }

  @Test
  void commonRewardsUseSharedManagedFrameAndStillKeepEqualBounds() {
    Texture frame = mock(Texture.class);
    when(frame.getWidth()).thenReturn(450);
    when(frame.getHeight()).thenReturn(912);
    ResourceService resources = ServiceLocator.getResourceService();
    when(resources.containsAsset(CardWidgetAssets.COMMON_FRAME_TEXTURE, Texture.class))
        .thenReturn(true);
    when(resources.getAsset(CardWidgetAssets.COMMON_FRAME_TEXTURE, Texture.class))
        .thenReturn(frame);
    CardAcquisitionPool pool =
        new CardAcquisitionPool(cardService, List.of("strike", "defend", "bandage"));
    RewardDisplay display =
        createDisplay(new RewardService(fixedRewardGenerator(), cardService, pool, new Random(7)));
    stage.getViewport().update(1280, 960, true);
    cardButton(display).fire(new ChangeEvent());
    for (Actor actor : stage.getActors()) {
      if (actor instanceof Layout layout) {
        layout.validate();
      }
    }

    assertEquals(3, display.getCardWidgets().size());
    for (CardWidget widget : display.getCardWidgets()) {
      assertEquals(CardWidget.CARD_WIDTH, widget.getWidth());
      assertEquals(CardWidget.CARD_HEIGHT, widget.getHeight());
      Image frameImage = ((Group) widget.getChildren().first()).findActor("card-frame");
      TextureRegionDrawable drawable =
          assertInstanceOf(TextureRegionDrawable.class, frameImage.getDrawable());
      assertSame(frame, drawable.getRegion().getTexture());
    }
  }

  @Test
  void mixedRarityRewardsWithLongDescriptionsShouldHaveEqualCardBounds() {
    CardAcquisitionPool pool =
        new CardAcquisitionPool(cardService, List.of("starfall", "poison_cloud", "poison_mark"));
    RewardDisplay display =
        createDisplay(new RewardService(fixedRewardGenerator(), cardService, pool, new Random(7)));
    stage.getViewport().update(1280, 960, true);

    cardButton(display).fire(new ChangeEvent());
    for (Actor actor : stage.getActors()) {
      if (actor instanceof Layout layout) {
        layout.validate();
      }
    }

    assertEquals(3, display.getCardWidgets().size());
    Button firstButton = display.getCardChoiceButtons().getFirst();
    for (int i = 0; i < display.getCardWidgets().size(); i++) {
      CardWidget widget = display.getCardWidgets().get(i);
      Button button = display.getCardChoiceButtons().get(i);
      assertEquals(CardWidget.CARD_WIDTH, widget.getWidth());
      assertEquals(CardWidget.CARD_HEIGHT, widget.getHeight());
      assertEquals(firstButton.getWidth(), button.getWidth());
      assertEquals(firstButton.getHeight(), button.getHeight());
      assertEquals(firstButton.getY(), button.getY());
      assertTrue(widget.getY() >= 0f);
      assertTrue(widget.getY() + widget.getHeight() <= button.getHeight());
    }
  }

  @Test
  void committedCardPathClaimsExactlyOnceAndCannotAlsoClaimGold() {
    RewardDisplay display = createDisplay(defaultService());
    PlayerDeck deck = runState.getOrCreatePlayerDeck(cardService);
    int initialDeckSize = deck.size();
    int initialGold = runState.getOrCreatePlayerState().getGold();
    AtomicInteger completionEvents = new AtomicInteger();
    entity
        .getEvents()
        .addListener(RewardDisplay.REWARD_CLAIMED_EVENT, completionEvents::incrementAndGet);

    cardButton(display).fire(new ChangeEvent());
    display.getOptionButtons().get(0).fire(new ChangeEvent());
    Button selectedCard = display.getCardChoiceButtons().get(0);
    selectedCard.fire(new ChangeEvent());
    selectedCard.fire(new ChangeEvent());

    assertTrue(display.isClaimed());
    assertEquals(initialDeckSize + 1, deck.size());
    assertEquals(initialGold, runState.getOrCreatePlayerState().getGold());
    assertEquals(1, completionEvents.get());
  }

  @Test
  void emptyPoolKeepsGoldAndItemAvailableWithoutCardChoice() {
    CardAcquisitionPool emptyPool = new CardAcquisitionPool(cardService, List.of());
    RewardService service =
        new RewardService(fixedRewardGenerator(), cardService, emptyPool, new Random(7));
    RewardDisplay display = createDisplay(service);

    assertEquals(List.of("25 Gold", "Energy Crystal (+1 Max Energy)"), labels(display));
    assertTrue(display.getOptions().stream().noneMatch(option -> option.type == RewardType.CARD));
  }

  @Test
  void smallPoolsShowOnlyTheAvailableDistinctCards() {
    assertVisibleChoiceCount(List.of("strike"), 1);
    entity.dispose();
    entity = null;
    assertVisibleChoiceCount(List.of("strike", "defend"), 2);
  }

  @Test
  void repeatedCreateDoesNotRerollOrDuplicateActors() {
    AtomicInteger generationCalls = new AtomicInteger();
    RewardService service =
        new RewardService(fixedRewardGenerator(), cardService, new Random(11)) {
          @Override
          public List<RewardOption> generateRewardOptions(float goldBonusMultiplier) {
            generationCalls.incrementAndGet();
            return super.generateRewardOptions(goldBonusMultiplier);
          }
        };
    RewardDisplay display = createDisplay(service);
    List<String> offeredIds = cardOption(display).cardSelection.cardIds();
    int actorCount = stage.getActors().size;

    display.create();

    assertEquals(1, generationCalls.get());
    assertEquals(offeredIds, cardOption(display).cardSelection.cardIds());
    assertEquals(actorCount, stage.getActors().size);
    assertEquals(3, display.getOptionButtons().size());
  }

  @Test
  void missingRunStateLeavesEveryRewardUnclaimed() {
    RewardDisplay display = createDisplay(defaultService(), null, discovery);
    AtomicInteger completionEvents = new AtomicInteger();
    entity
        .getEvents()
        .addListener(RewardDisplay.REWARD_CLAIMED_EVENT, completionEvents::incrementAndGet);

    display.getOptionButtons().get(0).fire(new ChangeEvent());
    cardButton(display).fire(new ChangeEvent());

    assertFalse(display.isClaimed());
    assertFalse(display.isCardRewardCommitted());
    assertEquals(0, completionEvents.get());
  }

  @Test
  void claimFailuresStayOnTheRewardScreenAndCanBeRetried() {
    AtomicInteger attempts = new AtomicInteger();
    RewardService failingService =
        new RewardService(fixedRewardGenerator(), cardService, new Random(11)) {
          @Override
          public void claimRunReward(RunState state, RewardOption selected, String selectedCardId) {
            attempts.incrementAndGet();
            throw new IllegalStateException("simulated claim failure");
          }
        };
    RewardDisplay display = createDisplay(failingService);
    int initialGold = runState.getOrCreatePlayerState().getGold();
    AtomicInteger returnEvents = new AtomicInteger();
    entity
        .getEvents()
        .addListener(EndBattleDisplay.RETURN_TO_MENU_EVENT, returnEvents::incrementAndGet);

    display.getOptionButtons().get(0).fire(new ChangeEvent());
    display.getOptionButtons().get(0).fire(new ChangeEvent());

    assertEquals(2, attempts.get());
    assertFalse(display.isClaimed());
    assertEquals(initialGold, runState.getOrCreatePlayerState().getGold());
    assertEquals(0, returnEvents.get());
  }

  @Test
  void cardClaimFailureDoesNotMutateDeckOrCompleteReward() {
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
    AtomicInteger returnEvents = new AtomicInteger();
    entity
        .getEvents()
        .addListener(EndBattleDisplay.RETURN_TO_MENU_EVENT, returnEvents::incrementAndGet);

    cardButton(display).fire(new ChangeEvent());
    Button selectedCard = display.getCardChoiceButtons().get(0);
    selectedCard.fire(new ChangeEvent());
    selectedCard.fire(new ChangeEvent());

    assertEquals(2, attempts.get());
    assertFalse(display.isClaimed());
    assertEquals(initialDeckSize, runState.getOrCreatePlayerDeck(cardService).size());
    assertEquals(0, returnEvents.get());
  }

  @Test
  void constructorRejectsMissingRequiredServices() {
    DisplayingRecord record = DisplayingRecord.builder("").variant("reward").build();
    RewardService service = defaultService();

    assertThrows(
        NullPointerException.class,
        () -> new RewardDisplay(record, null, runState, cardService, discovery));
    assertThrows(
        NullPointerException.class,
        () -> new RewardDisplay(record, service, runState, null, discovery));
    assertThrows(
        NullPointerException.class,
        () -> new RewardDisplay(record, service, runState, cardService, null));
  }

  @Test
  void disposeRemovesDynamicActorsAndStaleButtonsCannotClaim() {
    RewardDisplay display = createDisplay(defaultService());
    cardButton(display).fire(new ChangeEvent());
    Button staleCardButton = display.getCardChoiceButtons().get(0);
    int initialDeckSize = runState.getOrCreatePlayerDeck(cardService).size();

    entity.dispose();
    entity = null;
    staleCardButton.fire(new ChangeEvent());

    assertEquals(0, stage.getActors().size);
    assertEquals(initialDeckSize, runState.getOrCreatePlayerDeck(cardService).size());
  }

  private RewardDisplay createDisplay(RewardService service) {
    return createDisplay(service, runState, discovery);
  }

  private RewardDisplay createDisplay(
      RewardService service, RunState displayRunState, CardDiscoveryService displayDiscovery) {
    RewardDisplay display =
        new RewardDisplay(
            DisplayingRecord.builder("").variant("reward").build(),
            service,
            displayRunState,
            cardService,
            displayDiscovery);
    entity = new Entity().addComponent(display);
    entity.create();
    return display;
  }

  private void assertVisibleChoiceCount(List<String> eligibleIds, int expectedChoices) {
    CardAcquisitionPool pool = new CardAcquisitionPool(cardService, eligibleIds);
    RewardService service =
        new RewardService(fixedRewardGenerator(), cardService, pool, new Random(7));
    RewardDisplay display = createDisplay(service);

    cardButton(display).fire(new ChangeEvent());

    assertEquals(expectedChoices, display.getCardChoiceButtons().size());
    assertEquals(expectedChoices, display.getCardWidgets().size());
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
        return RewardOption.item(com.csse3200.game.rewards.ItemType.ENERGY_CRYSTAL);
      }
    };
  }

  private static List<String> labels(RewardDisplay display) {
    return display.getOptionButtons().stream()
        .map(TextButton::getText)
        .map(Object::toString)
        .toList();
  }

  private static RewardOption cardOption(RewardDisplay display) {
    return display.getOptions().stream()
        .filter(option -> option.type == RewardType.CARD)
        .findFirst()
        .orElseThrow();
  }

  private static TextButton cardButton(RewardDisplay display) {
    return display.getOptionButtons().stream()
        .filter(button -> "Choose a Card".contentEquals(button.getText()))
        .findFirst()
        .orElseThrow();
  }
}
