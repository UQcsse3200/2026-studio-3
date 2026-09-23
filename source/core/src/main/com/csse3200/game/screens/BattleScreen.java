package com.csse3200.game.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
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
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.battle.*;
import com.csse3200.game.components.cards.CardEffectHandler;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.pausemenu.PauseMenuFactory;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.components.save.SaveLoadPanel;
import com.csse3200.game.components.spritedisplay.clickable.CardImageSkins;
import com.csse3200.game.components.spritedisplay.clickable.ClickableFactory;
import com.csse3200.game.components.spritedisplay.clickable.ClickableRecord;
import com.csse3200.game.components.spritedisplay.displaying.DisplayingFactory;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.DragNDropService;
import com.csse3200.game.services.GamePauseService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.PopupDisplay;
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
  private static final Logger logger = LoggerFactory.getLogger(BattleScreen.class);
  private final Renderer renderer;
  private final ForestGameArea gameArea;

  private static final String[] mainGameTextures = {
    "images/heart.png",
    "images/energy.png",
    "images/money.png",
    "images/piety.png",
    "images/enemy.png",
    "images/armour.png"
  };
  private static final Vector2 CAMERA_POSITION = new Vector2(7.5f, 7.5f);

  private static final float HAND_START_X = 25f;
  private static final float HAND_Y = 1000f;
  private static final float HAND_SPACING = 250f;
  private static final float CARD_WIDTH = 225;
  private static final float CARD_HEIGHT = 456;
  private static final float CARD_INVENTORY_MIN_WIDTH = 800f;
  private static final float CARD_INVENTORY_MIN_HEIGHT = 600f;
  private static final int AMOUNT_OF_CARDS_IN_DECK = 5;

  private final BattleController controller;
  private final CardLibrary library;
  private final BattleDeck battleDeck;
  // Shared with the debug dialog so it reflects real, live resolutions instead of a
  // separate copy.
  private final CardEffectResolutionService cardEffects;
  private final CardPlayService cardPlayService;
  private ClickableFactory uiFactory;
  private final PlayerRunState playerState;
  private List<ClickableRecord> staticUiRecords;

  // Fixed left-to-right slot order for the on-screen row: each entry is the exact physical card
  // (see CardInstance) occupying that slot. Captured at deal time, and reset wholesale whenever the
  // player deliberately rearranges their hand via the deck editor — otherwise left untouched, so a
  // played card's slot just toggles disabled in place (its instance shows up in the discard pile)
  // instead of the row reflowing and making it look like a new card was drawn.
  private List<CardInstance> handRowOrder = new ArrayList<>();

  public BattleScreen(GdxGame game) {
    this.game = game;

    ServiceLocator.registerDragNDropService(new DragNDropService());

    logger.debug("Initialising main game screen services");
    GameTime gameTime = new GameTime();
    ServiceLocator.registerTimeSource(gameTime);
    ServiceLocator.registerPauseService(new GamePauseService(gameTime));

    PhysicsService physicsService = new PhysicsService();
    ServiceLocator.registerPhysicsService(physicsService);
    PhysicsEngine physicsEngine = physicsService.getPhysics();

    ServiceLocator.registerInputService(new InputService());
    ServiceLocator.registerResourceService(new ResourceService());

    ServiceLocator.registerEntityService(new EntityService());
    ServiceLocator.registerRenderService(new RenderService());

    loadAssets(); // <-- MOVED UP: load "images/heart.png" before anything uses it

    renderer = RenderFactory.createRenderer();
    renderer.getCamera().getEntity().setPosition(CAMERA_POSITION);
    renderer.getDebug().renderPhysicsWorld(physicsEngine.getWorld());

    ServiceLocator.registerCamera(renderer.getCamera().getCamera());

    // Integer for scaling difficulty level
    Integer mapProgression = game.getRunState().getMapProgression();

    logger.debug("Initialising main game screen entities");
    TerrainFactory terrainFactory = new TerrainFactory(renderer.getCamera());
    BattleGameArea forestGameArea =
        new BattleGameArea(terrainFactory, mapProgression, game.getRunState(), "dungeon");
    this.gameArea = forestGameArea;
    forestGameArea.create();
    RunState runState = game.getRunState();
    playerState = runState.getOrCreatePlayerState();

    // Card + deck state has to exist before the controller so it can be handed the single
    // card-play entry point and the deck it mutates.
    List<CardConfig> configs = CardConfigLoader.loadCards(); // reads configs/cards.json
    library = new CardLibrary(configs);
    ServiceLocator.registerCardLibrary(library);

    PlayerDeck playerDeck = runState.getOrCreatePlayerDeck(library);
    battleDeck = new BattleDeck(playerDeck);
    battleDeck.shuffleDrawPile();
    battleDeck.drawCards(AMOUNT_OF_CARDS_IN_DECK);
    handRowOrder = new ArrayList<>(battleDeck.getHandInstances());

    Entity player = forestGameArea.getPlayer();
    EnergyComponent energy = player.getComponent(EnergyComponent.class);

    Map<String, Entity> enemyTargets = forestGameArea.getEnemyTargets();
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

    controller.addBattleEndListener(
        won -> {
          if (won) {
            int currentHealth =
                forestGameArea.getPlayer().getComponent(CombatStatsComponent.class).getHealth();
            int maxHealth =
                forestGameArea.getPlayer().getComponent(CombatStatsComponent.class).getMaxHealth();
            int maxEnergy =
                forestGameArea.getPlayer().getComponent(EnergyComponent.class).getMaxEnergy();

            game.getRunState().setPlayerHealth(currentHealth);
            game.getRunState().setPlayerMaxHealth(maxHealth);
            game.getRunState().setPlayerMaxEnergy(maxEnergy);
          }
        });
    createUI();
    controller.start();
  }

  public void createUI() {
    // sprites/BattleUi.json holds both the static "Clickable" UI (exit/up/down/end turn) and the
    // "Displaying" text overlays (the card-label prompt and the between-turns battle log). The card
    // hand itself is dealt dynamically from the battle deck, so it is merged in below rather than
    // living in JSON.
    Path battleUiJson = Path.of("sprites/BattleUi.json");

    DisplayingFactory displays = new DisplayingFactory(battleUiJson);

    staticUiRecords = ClickableFactory.loadRecordsFromJson(battleUiJson);

    uiFactory = new ClickableFactory(buildAllRecords());

    Team3CardPlayAdapter cardPlayAdapter = new Team3CardPlayAdapter(cardPlayService, controller);

    // PROPOSED: debug terminal for cheats/commands during battle (skip battle, give gold, etc.
    // — commands added separately). Same Terminal/KeyboardTerminalInputComponent/TerminalDisplay
    // trio MainGameScreen already wires up; F1 toggles it open/closed.
    Terminal terminal = new Terminal();
    terminal.addCommand("skipbattle", new SkipBattleCommand(controller));
    terminal.addCommand("givegold", new GiveGoldCommand(gameArea.getPlayer()));
    terminal.addCommand("sethealth", new SetHealthCommand(gameArea.getPlayer()));
    terminal.addCommand("giveitem", new GiveItemCommand(gameArea.getPlayer()));

    PopupDisplay cardInventory = new PopupDisplay("Card Inventory");
    cardInventory.setMinSize(CARD_INVENTORY_MIN_WIDTH, CARD_INVENTORY_MIN_HEIGHT);

    Stage stage = ServiceLocator.getRenderService().getStage();
    Entity battleUi =
        new Entity()
            .addComponent(new InputDecorator(stage, 10))
            .addComponent(uiFactory)
            .addComponent(displays)
            .addComponent(new BattleActions(controller, game))
            .addComponent(cardPlayAdapter)
            .addComponent(cardInventory)
            .addComponent(
                new DamageOnCardPlayComponent(
                    gameArea.getPlayer().getComponent(CombatStatsComponent.class)))
            .addComponent(new CardEffectDebugComponent(cardEffects))
            .addComponent(new KeyboardCardEffectDebugInputComponent())
            .addComponent(new CardEffectDebugDisplay())
            .addComponent(terminal)
            .addComponent(new KeyboardTerminalInputComponent())
            .addComponent(new TerminalDisplay());

    // Keep the on-screen row in sync with the deck: whenever the hand changes (a card played, or
    // one retrieved from the discard pile after its cooldown elapses) rebuild from the live deck,
    // so the affected slot's disabled/shaded state updates in place.
    battleUi
        .getEvents()
        .addListener(
            BattleActions.HAND_CHANGED_EVENT,
            (List<CardInstance> hand) -> uiFactory.rebuildHand(buildHandRecords()));

    // Pause menu + in-place save/load overlay (added before the entity is created).
    SaveLoadPanel savePanel = PauseMenuFactory.attach(battleUi, game);

    // battleUi must be registered (and so cardInventory.create() must have run, giving it a
    // content table) before the deck editor's create() tries to add widgets to that table below.
    gameArea.displayUI(battleUi);
    savePanel.hide(); // save overlay starts hidden, opened by the Save & Load button

    // The deck editor's own per-card toggle buttons need a ClickableFactory of their own — an
    // entity can only hold one component of a given class, and battleUi already has uiFactory.
    ClickableFactory deckPoolFactory = new ClickableFactory(new ArrayList<>());
    DeckEditorComponent deckEditor =
        new DeckEditorComponent(
            cardPlayService, library, cardInventory, deckPoolFactory, this::onDeckRearranged);
    Entity deckEditorEntity = new Entity().addComponent(deckPoolFactory).addComponent(deckEditor);
    ServiceLocator.getEntityService().register(deckEditorEntity);

    battleUi.getEvents().addListener("openMenu", deckEditor::open);
  }

  /**
   * Called after the deck editor commits a hand rearrange, with the full confirmed selection —
   * including any still-on-cooldown picks {@link CardPlayService#rearrangeHand} left in the discard
   * pile untouched. Unlike a normal play/cooldown-retrieval hand change, the whole set of slots may
   * now be different, so — unlike {@link #buildHandRecords()}'s usual in-place toggling — the row's
   * slots themselves are reset to match. A slot holding a still-discarded pick simply renders
   * disabled (same as any other discarded card) until its cooldown naturally elapses and it's
   * retrieved into the real hand, at which point the normal HAND_CHANGED_EVENT listener un-dims it.
   */
  private void onDeckRearranged(List<CardInstance> newHandRow) {
    handRowOrder = new ArrayList<>(newHandRow);
    uiFactory.rebuildHand(buildHandRecords());
  }

  @Override
  public void render(float delta) {
    ServiceLocator.getEntityService().update();
    renderer.render();
  }

  @Override
  public void resize(int width, int height) {
    renderer.resize(width, height);
    logger.trace("Resized renderer: ({} x {})", width, height);
  }

  @Override
  public void dispose() {
    playerState.captureFrom(gameArea.getPlayer());
    renderer.dispose();
    ServiceLocator.getRenderService().dispose();
    ServiceLocator.getEntityService().dispose();
    ServiceLocator.clear();
  }

  private void loadAssets() {
    logger.debug("Loading assets");
    ResourceService resourceService = ServiceLocator.getResourceService();
    resourceService.loadTextures(mainGameTextures);
    ServiceLocator.getResourceService().loadAll();
  }

  private List<ClickableRecord> buildAllRecords() {
    List<ClickableRecord> records = new ArrayList<>(buildHandRecords());
    records.addAll(staticUiRecords);
    return records;
  }

  /**
   * Builds one widget per card slot in {@link #handRowOrder} — a fixed left-to-right layout that
   * only changes wholesale via {@link #onDeckRearranged}. Each slot renders the exact {@link
   * CardInstance} dealt to it: still in hand, it's normal and playable; currently sitting in the
   * discard pile (played, or on cooldown), it renders {@code disabled(true)} (shaded, inert to
   * clicks/drags — see {@link com.csse3200.game.components.spritedisplay.clickable.Clickable}) in
   * that SAME slot. Checking discard-pile membership by exact instance — not by card ID — is what
   * lets duplicate copies of the same card (e.g. two "strike"s) be dimmed independently of each
   * other. Positions never reflow and the row never grows/shrinks, so playing a card reads as "this
   * slot went dull", not as a new card being dealt.
   */
  private List<ClickableRecord> buildHandRecords() {
    Set<CardInstance> discardedInstances = new HashSet<>(battleDeck.getDiscardPileInstances());

    List<ClickableRecord> records = new ArrayList<>();
    float x = HAND_START_X;
    for (CardInstance instance : handRowOrder) {
      String cardId = instance.cardId();
      boolean disabled = discardedInstances.contains(instance);

      Optional<CardConfig> maybeCard = library.getCard(cardId);
      if (maybeCard.isEmpty()) {
        logger.warn("Card ID {} not found in library, skipping", cardId);
        continue;
      }
      CardConfig card = maybeCard.get();
      boolean selfTarget = card.target == TargetType.SELF;
      String variant = selfTarget ? "inout" : "drag";

      Skin cardSkin = CardImageSkins.forTexturePath(card.texturePath);

      ClickableRecord.Builder builder =
          ClickableRecord.builder("playCard")
              .label(card.name)
              .variant(variant)
              .position(x, HAND_Y)
              .size(CARD_WIDTH, CARD_HEIGHT)
              .skin(cardSkin)
              .disabled(disabled);

      if (selfTarget) {
        // No drop target involved — target is fixed at "player".
        builder.args(instance.instanceId(), "player");
      } else {
        // Enemy id isn't known yet; EnemyDropTargetComponent appends it at drop-time.
        builder.args(instance.instanceId());
      }

      records.add(builder.build());
      x += HAND_SPACING;
    }
    return records;
  }
}
