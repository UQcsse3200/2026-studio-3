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
import com.csse3200.game.cards.deck.PlayerDeckFactory;
import com.csse3200.game.cards.effects.CardEffectResolver;
import com.csse3200.game.components.battle.*;
import com.csse3200.game.components.combat.BattleController;
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
  private ForestGameArea gameArea;

  private static final String[] mainGameTextures = {
    "images/heart.png",
    "images/energy.png",
    "images/money.png",
    "images/piety.png",
    "images/enemy.png"
  };
  private static final Vector2 CAMERA_POSITION = new Vector2(7.5f, 7.5f);

  private static final float HAND_START_X = 50f;
  private static final float HAND_Y = 1700f;
  private static final float HAND_SPACING = 350f;

  private final PhysicsEngine physicsEngine;
  private static final Map<String, Skin> textureSkinCache = new HashMap<>();
  private final BattleController controller;
  private CardLibrary library;
  private BattleDeck battleDeck;
  private List<ClickableRecord> staticUiRecords;

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

    logger.debug("Initialising main game screen entities");
    TerrainFactory terrainFactory = new TerrainFactory(renderer.getCamera());
    ForestGameArea forestGameArea = new ForestGameArea(terrainFactory);
    this.gameArea = forestGameArea;
    forestGameArea.create();

    // Card + deck state has to exist before the controller so it can be handed the single
    // card-play entry point and the deck it mutates.
    List<CardConfig> configs = CardConfigLoader.loadCards(); // reads configs/cards.json
    library = new CardLibrary(configs);
    ServiceLocator.registerCardLibrary(library);

    PlayerDeck playerDeck = PlayerDeckFactory.createStarterDeck();
    battleDeck = new BattleDeck(playerDeck);
    battleDeck.shuffleDrawPile();
    battleDeck.drawCards(5);

    CardEffectResolver effectResolver = new CardEffectResolver(library);
    controller =
        new BattleController(
            forestGameArea.getPlayer(),
            forestGameArea.getEnemies(),
            effectResolver,
            library,
            battleDeck);

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

    Stage stage = ServiceLocator.getRenderService().getStage();
    Entity battleUi =
        new Entity()
            .addComponent(new InputDecorator(stage, 10))
            .addComponent(uiFactory)
            .addComponent(displays)
            .addComponent(new BattleActions(controller, game, library));

    // Keep the on-screen hand in sync with the deck: after a card is played (and a replacement
    // drawn) rebuild the hand widgets from the live deck, so the played card's button is gone and
    // the drawn card's button appears.
    battleUi
        .getEvents()
        .addListener(
            BattleActions.HAND_CHANGED_EVENT,
            (java.util.List<String> hand) -> uiFactory.rebuildHand(buildHandRecords()));

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

  private List<ClickableRecord> buildHandRecords() {
    List<ClickableRecord> records = new ArrayList<>();
    float x = HAND_START_X;
    for (String cardId : battleDeck.getHand()) {
      Optional<CardConfig> maybeCard = library.getCard(cardId);
      if (maybeCard.isEmpty()) {
        logger.warn("Card ID {} in hand not found in library, skipping", cardId);
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
              .size(300, 456)
              .skin(cardSkin);

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
}
