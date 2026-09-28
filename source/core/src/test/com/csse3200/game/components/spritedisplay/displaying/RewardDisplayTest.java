package com.csse3200.game.components.spritedisplay.displaying;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
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
    RewardDisplay display =
        new RewardDisplay(
            DisplayingRecord.builder("").variant("reward").build(),
            service,
            runState,
            cardService,
            discovery);
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
