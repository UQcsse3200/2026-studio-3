package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.csse3200.game.GdxGame;
import com.csse3200.game.areas.ForestGameArea;
import com.csse3200.game.areas.terrain.TerrainFactory;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.deck.BattleDeck;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.cards.play.CardPlayService;
import com.csse3200.game.cards.play.integration.Team3CardPlayAdapter;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.battle.*;
import com.csse3200.game.components.cards.CardEffectHandler;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.player.EnergyComponent;
import com.csse3200.game.components.spritedisplay.clickable.ClickableFactory;
import com.csse3200.game.components.spritedisplay.clickable.ClickableRecord;
import com.csse3200.game.components.spritedisplay.displaying.DisplayingFactory;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.entities.EntityService;
import com.csse3200.game.entities.factories.RenderFactory;
import com.csse3200.game.input.InputDecorator;
import com.csse3200.game.input.InputService;
import com.csse3200.game.physics.PhysicsEngine;
import com.csse3200.game.physics.PhysicsService;
import com.csse3200.game.rendering.RenderService;
import com.csse3200.game.rendering.Renderer;
import com.csse3200.game.services.DragNDropService;
import com.csse3200.game.services.GameTime;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.PopupDisplay;
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
    "images/enemy.png"
  };
  private static final Vector2 CAMERA_POSITION = new Vector2(7.5f, 7.5f);

  private static final float HAND_START_X = 25f;
  private static final float HAND_Y = 1000f;
  private static final float HAND_SPACING = 250f;
  private static final float CARD_WIDTH = 225;
  private static final float CARD_HEIGHT = 456;

  private final PhysicsEngine physicsEngine;
  private static final Map<String, Skin> textureSkinCache = new HashMap<>();
  private final BattleController controller;
  private final CardLibrary library;
  private final BattleDeck battleDeck;
  private List<ClickableRecord> staticUiRecords;

  // Fixed left-to-right slot order for the on-screen row, captured once at deal time so a played
  // card's slot just toggles disabled in place instead of the row reflowing and making it look
  // like a new card was drawn. See buildHandRecords().
  private final List<String> handRowOrder = new ArrayList<>();

  // handRowOrder indices grouped by card ID, computed once — the initial contents of
  // availableSlotsByCardId below.
  private final Map<String, List<Integer>> slotIndicesByCardId = new HashMap<>();

  // Per card ID, two FIFO queues that mirror BattleDeck's own hand/discardPile list order exactly:
  // availableSlotsByCardId is "which slot gets played next" (BattleDeck.hand.remove(id) always
  // removes the first/earliest match), disabledSlotsByCardId is "which slot gets retrieved next"
  // (BattleDeck.discardPile.remove(id) likewise). Discarding pops the front of available and
  // appends to the back of disabled; retrieving does the reverse. This is deliberately NOT just
  // "the lowest still-available original index" — retrieveFromDiscard() appends a returned card to
  // the END of the model's hand list, so once a slot has been retrieved once, the model no longer
  // prefers it over an untouched original slot. Picking by fixed index instead of this FIFO order
  // could dim/undim the wrong duplicate (e.g. among the starter deck's 3 Strikes).
  private final Map<String, Deque<Integer>> availableSlotsByCardId = new HashMap<>();
  private final Map<String, Deque<Integer>> disabledSlotsByCardId = new HashMap<>();

  public BattleScreen(GdxGame game) {
    this.game = game;

    ServiceLocator.registerDragNDropService(new DragNDropService());

    logger.debug("Initialising main game screen services");
    ServiceLocator.registerTimeSource(new GameTime());

    PhysicsService physicsService = new PhysicsService();
    ServiceLocator.registerPhysicsService(physicsService);
    physicsEngine = physicsService.getPhysics();

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
    ForestGameArea forestGameArea =
        new ForestGameArea(terrainFactory, mapProgression, game.getRunState());
    this.gameArea = forestGameArea;
    forestGameArea.create();

    // Card + deck state has to exist before the controller so it can be handed the single
    // card-play entry point and the deck it mutates.
    List<CardConfig> configs = CardConfigLoader.loadCards(); // reads configs/cards.json
    library = new CardLibrary(configs);
    ServiceLocator.registerCardLibrary(library);

    PlayerDeck playerDeck = game.getRunState().getOrCreatePlayerDeck(library);
    battleDeck = new BattleDeck(playerDeck);
    battleDeck.shuffleDrawPile();
    battleDeck.drawCards(5);
    handRowOrder.addAll(battleDeck.getHand());
    for (int i = 0; i < handRowOrder.size(); i++) {
      slotIndicesByCardId.computeIfAbsent(handRowOrder.get(i), id -> new ArrayList<>()).add(i);
    }

    Entity player = forestGameArea.getPlayer();
    EnergyComponent energy = player.getComponent(EnergyComponent.class);

    CardPlayService cardPlayService = new CardPlayService(library, battleDeck, energy);
    CardEffectHandler effectHandler = new CardEffectHandler();
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

    ClickableFactory uiFactory = new ClickableFactory(buildAllRecords());

    Team3CardPlayAdapter cardPlayAdapter = new Team3CardPlayAdapter(library, controller);

    PopupDisplay cardInventory = new PopupDisplay("Card Inventory");
    cardInventory.setMinSize(800f, 600f);

    Stage stage = ServiceLocator.getRenderService().getStage();
    Entity battleUi =
        new Entity()
            .addComponent(new InputDecorator(stage, 10))
            .addComponent(uiFactory)
            .addComponent(displays)
            .addComponent(new BattleActions(controller, game, library))
            .addComponent(cardPlayAdapter)
            .addComponent(cardInventory);

    // Keep the on-screen row in sync with the deck: whenever the hand changes (a card played, or
    // one retrieved from the discard pile after its cooldown elapses) rebuild from the live deck,
    // so the affected slot's disabled/shaded state updates in place.
    battleUi
        .getEvents()
        .addListener(
            BattleActions.HAND_CHANGED_EVENT,
            (List<String> hand) -> uiFactory.rebuildHand(buildHandRecords()));

    battleUi.getEvents().addListener("open-menu", cardInventory::show);

    gameArea.displayUI(battleUi);
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

  private Skin skinFromTexturePath(String texturePath) {
    return textureSkinCache.computeIfAbsent(
        texturePath,
        path -> {
          Texture texture = new Texture(Gdx.files.internal(path));
          TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));

          ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
          style.imageUp = drawable;

          Skin skin = new Skin();
          skin.add("default", style, ImageButton.ImageButtonStyle.class);
          return skin;
        });
  }

  private List<ClickableRecord> buildAllRecords() {
    List<ClickableRecord> records = new ArrayList<>(buildHandRecords());
    records.addAll(staticUiRecords);
    return records;
  }

  /**
   * Builds one widget per card slot in {@link #handRowOrder} — a fixed left-to-right layout
   * captured once when the hand was dealt. A card still in hand renders normal and playable; one
   * that has moved to the discard pile renders {@code disabled(true)} (shaded, inert to
   * clicks/drags — see {@link com.csse3200.game.components.spritedisplay.clickable.Clickable}) in
   * that SAME slot. Positions never reflow and the row never grows/shrinks, so playing a card
   * reads as "this slot went dull", not as a new card being dealt.
   */
  private List<ClickableRecord> buildHandRecords() {
    syncDisabledSlots();

    List<ClickableRecord> records = new ArrayList<>();
    float x = HAND_START_X;
    for (int i = 0; i < handRowOrder.size(); i++) {
      String cardId = handRowOrder.get(i);
      boolean disabled = disabledSlotsByCardId.getOrDefault(cardId, new ArrayDeque<>()).contains(i);

      Optional<CardConfig> maybeCard = library.getCard(cardId);
      if (maybeCard.isEmpty()) {
        logger.warn("Card ID {} not found in library, skipping", cardId);
        continue;
      }
      CardConfig card = maybeCard.get();
      boolean selfTarget = card.target == TargetType.SELF;
      String variant = selfTarget ? "inout" : "drag";

      Skin cardSkin = skinFromTexturePath(card.texturePath);

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
        builder.args(card.id, "player");
      } else {
        // Enemy id isn't known yet; EnemyDropTargetComponent appends it at drop-time.
        builder.args(card.id);
      }

      records.add(builder.build());
      x += HAND_SPACING;
    }
    return records;
  }

  /**
   * Keeps {@link #availableSlotsByCardId}/{@link #disabledSlotsByCardId} in sync with the deck's
   * actual discard pile by diffing the target disabled-count per card ID against the current one,
   * moving exactly one slot between the two queues per net discard/retrieval — see the field
   * comment above for why this has to mirror BattleDeck's FIFO list order rather than just picking
   * by fixed index.
   */
  private void syncDisabledSlots() {
    Map<String, Integer> discardCounts = new HashMap<>();
    for (String discardedId : battleDeck.getDiscardPile()) {
      discardCounts.merge(discardedId, 1, Integer::sum);
    }

    for (Map.Entry<String, List<Integer>> slotEntry : slotIndicesByCardId.entrySet()) {
      String cardId = slotEntry.getKey();
      Deque<Integer> availableSlots =
          availableSlotsByCardId.computeIfAbsent(
              cardId, ignored -> new ArrayDeque<>(slotEntry.getValue()));
      Deque<Integer> disabledSlots =
          disabledSlotsByCardId.computeIfAbsent(cardId, ignored -> new ArrayDeque<>());
      int targetDisabledCount = discardCounts.getOrDefault(cardId, 0);

      // A new discard: the slot the model would actually remove next (front of available) goes
      // dull, and joins the back of the discard queue (matches discardPile.add appending).
      while (disabledSlots.size() < targetDisabledCount && !availableSlots.isEmpty()) {
        disabledSlots.addLast(availableSlots.pollFirst());
      }
      // A retrieval: the oldest-discarded slot (front of disabled, matches discardPile.remove
      // taking the first/oldest match) comes back, and joins the back of available (matches
      // hand.add appending) — lowest priority for the next play, same as the real retrieved card.
      while (disabledSlots.size() > targetDisabledCount && !disabledSlots.isEmpty()) {
        availableSlots.addLast(disabledSlots.pollFirst());
      }
    }
  }
}
