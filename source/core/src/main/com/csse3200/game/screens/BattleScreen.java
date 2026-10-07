package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.csse3200.game.GdxGame;
import com.csse3200.game.areas.ForestGameArea;
import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.debug.CardEffectDebugComponent;
import com.csse3200.game.cards.debug.CardEffectDebugDisplay;
import com.csse3200.game.cards.debug.KeyboardCardEffectDebugInputComponent;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.effects.CardEffectResolutionService;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.cards.play.integration.Team1EnemyStateAdapter;
import com.csse3200.game.cards.play.integration.Team3CardPlayAdapter;
import com.csse3200.game.cards.play.integration.Team7PlayerStateAdapter;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.cards.runtime.ResolvedCard;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.battle.*;
import com.csse3200.game.components.cards.CardEffectHandler;
import com.csse3200.game.components.cards.CardWidget;
import com.csse3200.game.components.cards.CardWidgetAssets;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.enemy.EnemyBehaviourComponent;
import com.csse3200.game.components.enemy.Memory.EnemyMemoryComponent;
import com.csse3200.game.components.enemy.Memory.PlayerTrackerComponent;
import com.csse3200.game.components.pausemenu.PauseMenuActions;
import com.csse3200.game.components.pausemenu.PauseMenuDisplay;
import com.csse3200.game.components.pausemenu.PauseMenuFactory;
import com.csse3200.game.components.pausemenu.PauseMenuInput;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.components.save.SaveLoadPanel;
import com.csse3200.game.components.spritedisplay.clickable.BattleMenuSkins;
import com.csse3200.game.components.spritedisplay.clickable.CardAimController;
import com.csse3200.game.components.spritedisplay.clickable.Clickable;
import com.csse3200.game.components.spritedisplay.clickable.ClickableFactory;
import com.csse3200.game.components.spritedisplay.clickable.ClickableRecord;
import com.csse3200.game.components.spritedisplay.clickable.DragNDrop;
import com.csse3200.game.components.spritedisplay.displaying.CardBadgesDisplay;
import com.csse3200.game.components.spritedisplay.displaying.CardPreviewDisplay;
import com.csse3200.game.components.spritedisplay.displaying.DisplayingFactory;
import com.csse3200.game.components.spritedisplay.displaying.DisplayingRecord;
import com.csse3200.game.components.spritedisplay.displaying.PopupTextDisplay;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.AnimationRenderComponent;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.DragNDropService;
import com.csse3200.game.services.GamePauseService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.tutorial.BattleTutorialComponent;
import com.csse3200.game.tutorial.BattleTutorialController;
import com.csse3200.game.tutorial.BattleTutorialGuidanceView;
import com.csse3200.game.tutorial.BattleTutorialObservation;
import com.csse3200.game.ui.PopupDisplay;
import com.csse3200.game.ui.PopupInputComponent;
import com.csse3200.game.ui.terminal.KeyboardTerminalInputComponent;
import com.csse3200.game.ui.terminal.Terminal;
import com.csse3200.game.ui.terminal.TerminalDisplay;
import com.csse3200.game.ui.terminal.commands.GiveGoldCommand;
import com.csse3200.game.ui.terminal.commands.GiveItemCommand;
import com.csse3200.game.ui.terminal.commands.SetHealthCommand;
import com.csse3200.game.ui.terminal.commands.SkipBattleCommand;
import java.nio.file.Path;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The battle screen: the forest arena plus the card-hand UI, driven by {@link BattleController}.
 * Entered from a combat/boss map node (or the debug shortcut).
 */
public class BattleScreen extends ScreenAdapter {
  private final GdxGame game;
  private final RunState runState;
  private final boolean tutorialBattle;
  private BattleTutorialGuidanceView tutorialView;
  private BattleActions battleActions;
  private static final Logger logger = LoggerFactory.getLogger(BattleScreen.class);
  private final Renderer renderer;
  private final ForestGameArea gameArea;

  private static final String[] mainGameTextures = {
    "images/heart.png",
    "images/energy.png",
    "images/money.png",
    "images/level.png",
    "images/enemy.png",
    "images/armour.png",
    "images/enemy_release/heavens_grace.png",
    "images/ui/inventory-panel.png",
    "images/effects/shield.png",
    "images/effects/fortify.png",
    "images/ui/lucky-coin.png",
    "images/ui/energy-crystal.png",
    "images/ui/merchants-favor.png",
    "images/ui/iron-aegis.png",
    "images/ui/warriors-crest.png",
    "images/effects/heal.png",
    "images/enemies/intents/buff.png"
  };
  private static final Vector2 CAMERA_POSITION = new Vector2(7.5f, 8.5f);

  private static final float HAND_Y = 1000f;
  private static final float HAND_SPACING = 90f;
  private static final float HAND_ROTATION_STEP_DEGREES = 6f;
  private static final float HAND_ARC_DROP_PER_CARD = 18f;
  private static final float CARD_WIDTH = CardWidget.CARD_WIDTH;
  private static final float CARD_HEIGHT = CardWidget.CARD_HEIGHT;
  private static final float CARD_INVENTORY_MIN_WIDTH = 800f;
  private static final float CARD_INVENTORY_MIN_HEIGHT = 600f;
  private static final float ITEM_INVENTORY_MIN_WIDTH = 470f;
  private static final float ITEM_INVENTORY_MIN_HEIGHT = 360f;
  private static final int AMOUNT_OF_CARDS_IN_DECK = 5;
  private static final String CARD_WIDGET_SKIN = "flat-earth/skin/flat-earth-ui.json";

  private static final String BATTLE_UI_JSON = "sprites/BattleUi.json";
  private static final String DECK_EDITOR_UI_JSON = "sprites/DeckEditorUi.json";

  private final BattleController controller;
  private final CardLibrary library;
  private final BattleDeck battleDeck;
  private final CardResolver cardResolver = new CardResolver();
  private final Skin cardWidgetSkin;
  private final Skin cardInteractionSkin;
  private final CardWidgetAssets cardWidgetAssets;
  private final CardEffectResolutionService cardEffects;
  private final CardPlayService cardPlayService;
  private final CardAimController enemyCardAim;
  private final CardAimController playerCardAim;
  private final CardAimController allEnemiesCardAim;
  private ClickableFactory uiFactory;
  private final PlayerRunState playerState;
  private List<ClickableRecord> staticUiRecords;

  private List<CardInstance> handRowOrder = new ArrayList<>();

  public BattleScreen(GdxGame game) {
    this(game, false);
  }

  /** A tutorial has a disposable run and one fixed opponent instead of a map encounter. */
  public BattleScreen(GdxGame game, boolean tutorialBattle) {
    this.game = game;
    this.tutorialBattle = tutorialBattle;
    this.runState = tutorialBattle ? new RunState() : game.getRunState();

    ServiceLocator.registerDragNDropService(new DragNDropService());

    logger.debug("Initialising main game screen services");
    GameTime gameTime = new GameTime();
    ServiceLocator.registerTimeSource(gameTime);
    ServiceLocator.registerPauseService(new GamePauseService(gameTime));

    PhysicsService physicsService = new PhysicsService();
    ServiceLocator.registerPhysicsService(physicsService);
    PhysicsEngine physicsEngine = physicsService.getPhysics();

    ServiceLocator.registerInputService(new InputService());

    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());

    loadAssets();

    renderer = RenderFactory.createRenderer();
    renderer.getCamera().getEntity().setPosition(CAMERA_POSITION);
    renderer.getDebug().renderPhysicsWorld(physicsEngine.getWorld());

    ServiceLocator.registerCamera(renderer.getCamera().getCamera());

    Integer mapProgression = tutorialBattle ? 1 : runState.getMapProgression();

    logger.debug("Initialising main game screen entities");
    TerrainFactory terrainFactory = new TerrainFactory(renderer.getCamera());
    BattleGameArea forestGameArea =
        new BattleGameArea(
            terrainFactory,
            mapProgression,
            runState,
            tutorialBattle ? "dungeon" : game.getBackgroundId(),
            tutorialBattle
                ? List.of("bone_crawler")
                : BattleEncounterSelector.enemiesFor(runState));
    this.gameArea = forestGameArea;
    forestGameArea.create();

    playerState = runState.getOrCreatePlayerState();
    Entity player = forestGameArea.getPlayer();

    List<CardConfig> configs = CardConfigLoader.loadCards();
    library = new CardLibrary(configs);
    ServiceLocator.registerCardLibrary(library);
    loadCardAssets(configs);
    cardWidgetSkin = new Skin(Gdx.files.internal(CARD_WIDGET_SKIN));
    cardWidgetAssets =
        CardWidgetAssets.fromManagedResources(cardWidgetSkin, ServiceLocator.getResourceService());
    cardInteractionSkin = createCardInteractionSkin();

    PlayerDeck playerDeck = runState.getOrCreatePlayerDeck(library);
    if (tutorialBattle) {
      var openingOrder = new ArrayList<>(playerDeck.getCards());
      java.util.Collections.shuffle(openingOrder);
      for (int i = 0; i < openingOrder.size(); i++) {
        if ("strike".equals(openingOrder.get(i).cardId())) {
          java.util.Collections.swap(
              openingOrder, Math.min(AMOUNT_OF_CARDS_IN_DECK, openingOrder.size()) / 2, i);
          break;
        }
      }
      playerDeck = PlayerDeck.fromInstances(library, openingOrder);
    }
    battleDeck = new BattleDeck(playerDeck);
    if (!tutorialBattle) {
      battleDeck.shuffleDrawPile();
    }
    battleDeck.drawCards(AMOUNT_OF_CARDS_IN_DECK);
    handRowOrder = new ArrayList<>(battleDeck.getHandInstances());

    EnergyComponent energy = player.getComponent(EnergyComponent.class);

    Map<String, Entity> enemyTargets = forestGameArea.getEnemyTargets();
    enemyCardAim =
        new CardAimController(
            ServiceLocator.getRenderService().getStage(), ServiceLocator.getCamera(), enemyTargets);
    playerCardAim =
        new CardAimController(
            ServiceLocator.getRenderService().getStage(),
            ServiceLocator.getCamera(),
            Map.of("player", player),
            TargetType.SELF,
            this::battlefieldBounds);
    allEnemiesCardAim =
        new CardAimController(
            ServiceLocator.getRenderService().getStage(),
            ServiceLocator.getCamera(),
            enemyTargets,
            TargetType.ALL_ENEMIES,
            this::battlefieldBounds);
    cardEffects = new CardEffectResolutionService(library);
    cardPlayService =
        new CardPlayService(
            library,
            cardEffects,
            battleDeck,
            energy,
            new Team7PlayerStateAdapter(player),
            new Team1EnemyStateAdapter(enemyTargets));
    CardEffectHandler effectHandler = new CardEffectHandler(enemyTargets);

    controller =
        new BattleController(player, forestGameArea.getEnemies(), effectHandler, cardPlayService);
    EffectVisualRegistry effectVisualRegistry = createEffectVisualRegistry();
    Entity animationCoordinatorEntity =
        new Entity()
            .addComponent(
                new BattleAnimationCoordinator(
                    controller,
                    effectHandler,
                    forestGameArea.getEnemies(),
                    player,
                    effectVisualRegistry));
    ServiceLocator.getEntityService().register(animationCoordinatorEntity);

    PlayerTrackerComponent playerTracker =
        Objects.requireNonNull(
            player.getComponent(PlayerTrackerComponent.class),
            "Battle player requires PlayerTrackerComponent");

    playerTracker.connect(controller, library, battleDeck);
    EnemyMemoryComponent enemyMemory =
        Objects.requireNonNull(
            player.getComponent(EnemyMemoryComponent.class),
            "Player entity must contain EnemyMemoryComponent");

    for (Entity enemy : forestGameArea.getEnemies()) {
      EnemyBehaviourComponent behaviour = enemy.getComponent(EnemyBehaviourComponent.class);

      if (behaviour != null) {
        behaviour.setEnemyMemory(enemyMemory);
      }
    }

    controller.addBattleEndListener(
        won -> {
          if (Boolean.TRUE.equals(won)) {
            int currentHealth =
                forestGameArea.getPlayer().getComponent(CombatStatsComponent.class).getHealth();
            int maxHealth =
                forestGameArea.getPlayer().getComponent(CombatStatsComponent.class).getMaxHealth();
            int maxEnergy =
                forestGameArea.getPlayer().getComponent(EnergyComponent.class).getMaxEnergy();

            runState.setPlayerHealth(currentHealth);
            runState.setPlayerMaxHealth(maxHealth);
            runState.setPlayerMaxEnergy(maxEnergy);
          }
        });
    createUI();
    if (tutorialBattle) {
      String demonstrationId = handRowOrder.get(handRowOrder.size() / 2).instanceId();
      tutorialView =
          new BattleTutorialGuidanceView(
              ServiceLocator.getRenderService().getStage(),
              cardWidgetSkin,
              () ->
                  uiFactory.getByTrigger("playCard").stream()
                      .map(Clickable::getBtn)
                      .map(actor -> (com.badlogic.gdx.scenes.scene2d.Actor) actor)
                      .toList(),
              () ->
                  uiFactory.getByTrigger("playCard").stream()
                      .filter(c -> c.getArgs().length > 0 && demonstrationId.equals(c.getArgs()[0]))
                      .map(Clickable::getBtn)
                      .findFirst()
                      .orElse(null),
              () ->
                  uiFactory.getByTrigger("openMenu").stream()
                      .map(Clickable::getBtn)
                      .findFirst()
                      .orElse(null),
              () ->
                  uiFactory.getByTrigger("endTurn").stream()
                      .map(Clickable::getBtn)
                      .findFirst()
                      .orElse(null));
      tutorialView.setEnemyBounds(
          () -> forestGameArea.getEnemies().stream().map(this::tutorialEnemyBounds).toList());
      tutorialView.setDragActor(
          () -> ServiceLocator.getDragAndDropService().getDragAndDrop().getDragActor());
      tutorialView.setEnemyRenderer(
          batch -> {
            com.badlogic.gdx.math.Matrix4 previous =
                new com.badlogic.gdx.math.Matrix4(batch.getProjectionMatrix());
            batch.setProjectionMatrix(ServiceLocator.getCamera().combined);
            batch.setColor(com.badlogic.gdx.graphics.Color.WHITE);
            for (Entity enemy : forestGameArea.getEnemies()) {
              AnimationRenderComponent spriteRenderer =
                  enemy.getComponent(AnimationRenderComponent.class);
              if (spriteRenderer != null)
                spriteRenderer.render((com.badlogic.gdx.graphics.g2d.SpriteBatch) batch);
            }
            batch.setProjectionMatrix(previous);
            batch.setColor(com.badlogic.gdx.graphics.Color.WHITE);
          });
      BattleAnimationCoordinator visuals =
          animationCoordinatorEntity.getComponent(BattleAnimationCoordinator.class);
      BattleTutorialComponent guidance =
          new BattleTutorialComponent(
              controller, tutorialView, this::onTutorialFinished, demonstrationId);
      guidance.setObservation(
          new BattleTutorialObservation(
              () ->
                  forestGameArea.getEnemies().stream()
                      .mapToInt(e -> e.getComponent(CombatStatsComponent.class).getHealth())
                      .sum(),
              () -> player.getComponent(CombatStatsComponent.class).getHealth(),
              () ->
                  !visuals.hasActiveVisuals()
                      && !battleActions.hasPendingEnemyReveals()
                      && tutorialView.isPlayerHealthDisplayed(
                          player.getComponent(CombatStatsComponent.class).getHealth())
                      && forestGameArea.getEnemies().stream()
                          .allMatch(
                              e -> {
                                AnimationRenderComponent animator =
                                    e.getComponent(AnimationRenderComponent.class);
                                return animator == null
                                    || !"hurt".equals(animator.getCurrentAnimation())
                                    || animator.isFinished();
                              })));
      ServiceLocator.getEntityService().register(new Entity().addComponent(guidance));
      // Remains after guidance cleans itself up: existing tutorial battle completion is unchanged.
      controller.addBattleEndListener(
          won ->
              Gdx.app.postRunnable(
                  () ->
                      onTutorialFinished(
                          Boolean.TRUE.equals(won)
                              ? BattleTutorialController.Outcome.WON
                              : BattleTutorialController.Outcome.LOST)));
    }
    controller.start();
  }

  private Rectangle tutorialEnemyBounds(Entity enemy) {
    Stage stage = ServiceLocator.getRenderService().getStage();
    Vector2 pos = enemy.getPosition(), scale = enemy.getScale();
    Vector3 a = ServiceLocator.getCamera().project(new Vector3(pos.x, pos.y, 0));
    Vector3 b =
        ServiceLocator.getCamera().project(new Vector3(pos.x + scale.x, pos.y + scale.y, 0));
    Vector2 lower =
        stage.screenToStageCoordinates(new Vector2(a.x, Gdx.graphics.getHeight() - a.y));
    Vector2 upper =
        stage.screenToStageCoordinates(new Vector2(b.x, Gdx.graphics.getHeight() - b.y));
    return new Rectangle(
        Math.min(lower.x, upper.x) - 8,
        Math.min(lower.y, upper.y) - 8,
        Math.abs(upper.x - lower.x) + 16,
        Math.abs(upper.y - lower.y) + 16);
  }

  private void onTutorialFinished(BattleTutorialController.Outcome outcome) {
    // All tutorial changes live in this screen's disposable run. Joel's shared new-run path resets
    // the persistent run and opens the story/map on victory or voluntary exit.
    if (outcome == BattleTutorialController.Outcome.LOST) {
      game.setScreen(GdxGame.ScreenType.MAIN_MENU);
    } else {
      game.startNewRun();
    }
  }

  public void createUI() {
    Path battleUiJson = Path.of(BATTLE_UI_JSON);

    DisplayingFactory displays = new DisplayingFactory(battleUiJson);

    staticUiRecords = ClickableFactory.loadRecordsFromJson(battleUiJson);

    uiFactory = new ClickableFactory(buildAllRecords());
    uiFactory.registerInstanceVariant("aimDrag", rec -> new DragNDrop(rec, enemyCardAim));
    uiFactory.registerInstanceVariant("selfAimDrag", rec -> new DragNDrop(rec, playerCardAim));
    uiFactory.registerInstanceVariant(
        "allEnemiesAimDrag", rec -> new DragNDrop(rec, allEnemiesCardAim));

    Terminal terminal = new Terminal();
    terminal.addCommand("skipbattle", new SkipBattleCommand(controller));
    terminal.addCommand("givegold", new GiveGoldCommand(gameArea.getPlayer()));
    terminal.addCommand("sethealth", new SetHealthCommand(gameArea.getPlayer()));
    terminal.addCommand("giveitem", new GiveItemCommand(gameArea.getPlayer()));

    // Untitled: DeckEditorComponent draws its own "CARD INVENTORY" header, like the item inventory.
    PopupDisplay cardInventory = new PopupDisplay("");
    cardInventory.setMinSize(CARD_INVENTORY_MIN_WIDTH, CARD_INVENTORY_MIN_HEIGHT);

    PopupDisplay itemInventory = new PopupDisplay("Item Inventory");
    itemInventory.setMinSize(ITEM_INVENTORY_MIN_WIDTH, ITEM_INVENTORY_MIN_HEIGHT);
    InventoryPopupComponent inventoryPopup =
        new InventoryPopupComponent(
            game.getRunState(), itemInventory, gameArea.getPlayer(), controller::isPlayerTurn);

    Entity itemInventoryEntity =
        new Entity()
            .addComponent(itemInventory)
            .addComponent(new PopupInputComponent(itemInventory))
            .addComponent(inventoryPopup);
    ServiceLocator.getEntityService().register(itemInventoryEntity);

    Stage stage = ServiceLocator.getRenderService().getStage();
    battleActions = new BattleActions(controller, game, gameArea.getEnemies(), tutorialBattle);
    Entity battleUi =
        new Entity()
            .addComponent(new InputDecorator(stage, 10))
            .addComponent(uiFactory)
            .addComponent(displays)
            .addComponent(battleActions)
            .addComponent(new CardActions(controller, gameArea.getPlayer()))
            .addComponent(new Team3CardPlayAdapter(cardPlayService, controller))
            .addComponent(cardInventory)
            .addComponent(new PopupInputComponent(cardInventory))
            .addComponent(
                new DamageOnCardPlayComponent(
                    gameArea.getPlayer().getComponent(CombatStatsComponent.class)))
            .addComponent(new CardEffectDebugComponent(cardEffects))
            .addComponent(new KeyboardCardEffectDebugInputComponent())
            .addComponent(new CardEffectDebugDisplay())
            .addComponent(terminal)
            .addComponent(new KeyboardTerminalInputComponent())
            .addComponent(new TerminalDisplay());

    battleUi
        .getEvents()
        .addListener(
            BattleActions.HAND_CHANGED_EVENT,
            (List<CardInstance> hand) -> {
              uiFactory.rebuildHand(buildHandRecords());
              installHandCardWidgets();
            });
    // Pause menu + in-place save/load overlay (added before the entity is created).
    SaveLoadPanel savePanel = null;
    if (tutorialBattle) {
      battleUi
          .addComponent(new PauseMenuDisplay())
          .addComponent(new PauseMenuInput())
          .addComponent(new PauseMenuActions(game, true));
    } else {
      savePanel = PauseMenuFactory.attach(battleUi, game);
    }

    gameArea.displayUI(battleUi);
    installHandCardWidgets();

    Path deckEditorUiJson = Path.of(DECK_EDITOR_UI_JSON);
    List<ClickableRecord> deckEditorClickables =
        ClickableFactory.loadRecordsFromJson(deckEditorUiJson);
    ClickableFactory deckPoolFactory = new ClickableFactory(deckEditorClickables);
    if (savePanel != null) savePanel.hide(); // Tutorial previews must not save the real run.

    List<DisplayingRecord> deckEditorDisplayRecords =
        DisplayingFactory.loadRecordsFromJson(deckEditorUiJson);
    DisplayingFactory deckEditorDisplays =
        buildDeckEditorDisplays(cardInventory, deckEditorDisplayRecords);

    DeckEditorComponent deckEditor =
        new DeckEditorComponent(
            cardPlayService,
            library,
            cardInventory,
            deckPoolFactory,
            deckEditorDisplays,
            cardWidgetAssets,
            this::onDeckRearranged);

    Entity deckEditorEntity =
        new Entity()
            .addComponent(deckPoolFactory)
            .addComponent(deckEditorDisplays)
            .addComponent(deckEditor);
    ServiceLocator.getEntityService().register(deckEditorEntity);

    // Hide the deck editor's scroll buttons until the popup opens. DeckEditorComponent.create()
    // tries to do this itself, but runs before the JSON-loaded clickables are registered in the
    // factory, so nothing gets hidden. These two loops fix that.
    for (Clickable c : deckPoolFactory.getByTrigger("deckScrollUp")) {
      c.getBtn().setVisible(false);
    }
    for (Clickable c : deckPoolFactory.getByTrigger("deckScrollDown")) {
      c.getBtn().setVisible(false);
    }

    battleUi.getEvents().addListener("openMenu", deckEditor::open);
    battleUi.getEvents().addListener("openInventory", inventoryPopup::open);
  }

  private DisplayingFactory buildDeckEditorDisplays(
      PopupDisplay popup, List<DisplayingRecord> records) {

    DisplayingRecord badgesRec =
        DisplayingRecord.builder("").trigger(DeckEditorEvents.BADGES).variant("cardBadges").build();

    List<DisplayingRecord> all = new ArrayList<>(records);
    all.add(badgesRec);

    DisplayingFactory factory = new DisplayingFactory(all);

    factory.registerInstanceVariant(
        "popupText",
        rec -> {
          PopupTextDisplay d = new PopupTextDisplay(rec);
          d.addPopup(popup);
          return d;
        });
    factory.registerInstanceVariant(
        "cardPreview",
        rec -> {
          CardPreviewDisplay d = new CardPreviewDisplay(rec);
          d.addPopup(popup);
          return d;
        });

    factory.registerInstanceVariant("cardBadges", CardBadgesDisplay::new);
    return factory;
  }

  private void onDeckRearranged(List<CardInstance> newHandRow) {
    handRowOrder = new ArrayList<>(newHandRow);
    uiFactory.rebuildHand(buildHandRecords());
    installHandCardWidgets();
  }

  /** Builds the same registered visual groups for every battle. */
  static EffectVisualRegistry createEffectVisualRegistry() {
    EffectVisualRegistry registry = new EffectVisualRegistry();
    OffensiveEffectVisuals.registerAll(registry);
    PlayerEffectVisuals.registerAll(registry);
    EnemyStatusEffectVisuals.registerAll(registry);
    return registry;
  }

  @Override
  public void render(float delta) {
    ServiceLocator.getEntityService().update();
    if (tutorialView != null) tutorialView.alignWorldStatsToViewport();
    renderer.render();
    if (tutorialView != null) tutorialView.renderAboveBattle();
  }

  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
    logger.trace("Resized renderer: ({} x {})", width, height);
  }

  @Override
  public void dispose() {
    playerState.captureFrom(gameArea.getPlayer());
    enemyCardAim.dispose();
    playerCardAim.dispose();
    allEnemiesCardAim.dispose();
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.getResourceService().unloadAssets(EnemyStatusEffectVisuals.texturePaths());
    cardInteractionSkin.dispose();
    cardWidgetSkin.dispose();
    ServiceLocator.getResourceService().unloadAssets(mainGameTextures);
  }

  private void loadAssets() {
    logger.debug("Loading assets");
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(mainGameTextures);
    resourceService.loadTextures(EnemyStatusEffectVisuals.texturePaths());
    ServiceLocator.getResourceService().loadAll();
  }

  private void loadCardAssets(List<CardConfig> configs) {
    String[] texturePaths = CardWidgetAssets.collectTexturePaths(configs);
    ResourceService resources = ServiceLocator.getResourceService();
    resources.loadTextures(texturePaths);
    resources.loadAll();
  }

  private static Skin createCardInteractionSkin() {
    Skin skin = new Skin();
    skin.add("default", new ImageButton.ImageButtonStyle(), ImageButton.ImageButtonStyle.class);
    return skin;
  }

  private List<ClickableRecord> buildAllRecords() {
    List<ClickableRecord> records = new ArrayList<>(buildHandRecords());
    records.addAll(buildBattleMenuRecords());
    return records;
  }

  /** Applies the themed frame and matching icon without changing any button trigger or payload. */
  private List<ClickableRecord> buildBattleMenuRecords() {
    List<ClickableRecord> records = new ArrayList<>();
    for (ClickableRecord clickableRecord : staticUiRecords) {
      BattleMenuSkins.Icon icon =
          switch (clickableRecord.trigger()) {
            case "openMenu" -> BattleMenuSkins.Icon.CARD;
            case "openInventory" -> BattleMenuSkins.Icon.INVENTORY;
            case "endTurn" -> BattleMenuSkins.Icon.END_TURN;
            default -> null;
          };
      if (icon == null) {
        records.add(clickableRecord);
        continue;
      }

      records.add(
          ClickableRecord.builder(clickableRecord.trigger())
              .text(clickableRecord.text())
              .skin(BattleMenuSkins.forIcon(icon))
              .position(clickableRecord.x(), clickableRecord.y())
              .size(clickableRecord.width(), clickableRecord.height())
              .variant(clickableRecord.variant())
              .args(clickableRecord.args())
              .label(clickableRecord.label())
              .disabled(clickableRecord.disabled())
              .build());
    }
    return records;
  }

  /** The battle play area above the raised hand and below the top controls/battle log. */
  private Rectangle battlefieldBounds() {
    Stage stage = ServiceLocator.getRenderService().getStage();
    // Cards rise by 120 on hover; leave another 24 pixels before accepting a battlefield drop.
    float bottom = Math.max(0f, stage.getHeight() - HAND_Y + CARD_HEIGHT + 144f);
    float top = stage.getHeight() - 180f;
    return new Rectangle(0f, bottom, stage.getWidth(), Math.max(0f, top - bottom));
  }

  private List<ClickableRecord> buildHandRecords() {
    Set<CardInstance> discardedInstances = new HashSet<>(battleDeck.getDiscardPileInstances());

    List<ClickableRecord> records = new ArrayList<>();
    float stageWidth = ServiceLocator.getRenderService().getStage().getViewport().getWorldWidth();
    float handWidth = CARD_WIDTH + Math.max(0, handRowOrder.size() - 1) * HAND_SPACING;
    float x = (stageWidth - handWidth) / 2f;
    float centerIndex = (handRowOrder.size() - 1) / 2f;
    for (int i = 0; i < handRowOrder.size(); i++) {
      CardInstance instance = handRowOrder.get(i);
      boolean disabled = discardedInstances.contains(instance);

      ResolvedCard card = resolveHandCard(instance);
      if (card == null) {
        continue;
      }
      String variant =
          switch (card.target()) {
            case SELF -> "selfAimDrag";
            case SINGLE_ENEMY -> "aimDrag";
            case ALL_ENEMIES -> "allEnemiesAimDrag";
          };

      float offsetFromCenter = i - centerIndex;
      float rotation = -offsetFromCenter * HAND_ROTATION_STEP_DEGREES;
      float y = HAND_Y + Math.abs(offsetFromCenter) * HAND_ARC_DROP_PER_CARD;

      ClickableRecord.Builder builder =
          ClickableRecord.builder("playCard")
              .label(card.name())
              .variant(variant)
              .position(x, y)
              .size(CARD_WIDTH, CARD_HEIGHT)
              .skin(cardInteractionSkin)
              .rotation(rotation)
              .disabled(disabled);

      // The drag source supplies a player/enemy ID or an all-enemies marker on a valid drop.
      builder.args(instance.instanceId());

      records.add(builder.build());
      x += HAND_SPACING;
    }
    return records;
  }

  private ResolvedCard resolveHandCard(CardInstance instance) {
    Optional<CardConfig> config = library.getCard(instance.cardId());
    if (config.isEmpty()) {
      logger.warn("Card ID {} not found in library, skipping", instance.cardId());
      return null;
    }
    try {
      return cardResolver.resolve(config.get(), instance);
    } catch (IllegalArgumentException | IllegalStateException exception) {
      logger.warn("Could not resolve card instance {}, skipping", instance.instanceId(), exception);
      return null;
    }
  }

  private void installHandCardWidgets() {
    Map<String, CardInstance> instancesById = new HashMap<>();
    for (CardInstance instance : handRowOrder) {
      instancesById.put(instance.instanceId(), instance);
    }

    for (Clickable clickable : uiFactory.getByTrigger("playCard")) {
      installHandCardWidget(clickable, instancesById);
    }
  }

  private void installHandCardWidget(Clickable clickable, Map<String, CardInstance> instancesById) {
    Object[] args = clickable.getArgs();
    if (args.length == 0 || !(args[0] instanceof String instanceId)) {
      logger.warn("Play-card clickable is missing an instanceId payload");
      return;
    }
    CardInstance instance = instancesById.get(instanceId);
    if (instance == null) {
      logger.warn("No hand-row instance found for clickable payload {}", instanceId);
      return;
    }
    Optional<CardConfig> config = library.getCard(instance.cardId());
    if (config.isEmpty()) {
      return;
    }
    try {
      ResolvedCard resolved = cardResolver.resolve(config.get(), instance);
      clickable.setVisualContent(() -> new CardWidget(resolved, cardWidgetAssets));
    } catch (IllegalArgumentException | IllegalStateException exception) {
      logger.warn("Could not install card widget for instance {}", instanceId, exception);
    }
  }
}
