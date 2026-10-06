package com.csse3200.game;

import static com.badlogic.gdx.Gdx.app;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.csse3200.game.bestiary.BestiaryService;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.cards.CardDiscoveryStore;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.chance.CardFusionEncounterBehaviour;
import com.csse3200.game.chance.ChanceEncounterFactory;
import com.csse3200.game.chance.ChanceEncounterSelector;
import com.csse3200.game.files.UserSettings;
import com.csse3200.game.maps.MapNode;
import com.csse3200.game.maps.RoomType;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.narration.NarrationConfigLoader;
import com.csse3200.game.narration.NarrationLoadingException;
import com.csse3200.game.save.AutosaveCoordinator;
import com.csse3200.game.save.GameStateSnapshotProvider;
import com.csse3200.game.save.JsonSaveGameRepository;
import com.csse3200.game.save.SaveGameService;
import com.csse3200.game.screens.AncientTempleScreen;
import com.csse3200.game.screens.BattleScreen;
import com.csse3200.game.screens.BestiaryScreen;
import com.csse3200.game.screens.CampfireScreen;
import com.csse3200.game.screens.CardLibraryScreen;
import com.csse3200.game.screens.DemoCampfireScreen;
import com.csse3200.game.screens.DemoEventScreen;
import com.csse3200.game.screens.DemoShopScreen;
import com.csse3200.game.screens.ElitePortalScreen;
import com.csse3200.game.screens.EncounterScreen;
import com.csse3200.game.screens.EndBattleScreen;
import com.csse3200.game.screens.LibraryScreen;
import com.csse3200.game.screens.MainGameScreen;
import com.csse3200.game.screens.MainMenuScreen;
import com.csse3200.game.screens.MapScreen;
import com.csse3200.game.screens.NarrationScreen;
import com.csse3200.game.screens.SaveLoadScreen;
import com.csse3200.game.screens.SettingsScreen;
import com.csse3200.game.screens.TempleCardSelectionScreen;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.services.audio.AudioService;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point of the non-platform-specific game logic. Controls which screen is currently running.
 * The current screen triggers transitions to other screens. This works similarly to a finite state
 * machine (See the State Pattern).
 */
public class GdxGame extends Game {
  private static final Logger logger = LoggerFactory.getLogger(GdxGame.class);
  private static final String[] BACKGROUND_IDS =
      new String[] {
        "dungeon",
        "forest",
        "temple",
        "stones_ruins",
        "autumn_forest",
        "inside_castle",
        "mystical_tree",
        "underwater",
        "cemetery",
        "forest_with_sun"
      };
  private BestiaryService bestiaryService;
  private CardDiscoveryService cardDiscoveryService;

  /**
   * Gets discovery progress shared by all screens in this game session.
   *
   * @return process-lifetime Bestiary service
   */
  public BestiaryService getBestiaryService() {
    return bestiaryService;
  }

  /**
   * Gets card discovery progress shared by all screens in this game session.
   *
   * @return process-lifetime card discovery service
   */
  public CardDiscoveryService getCardDiscoveryService() {
    return cardDiscoveryService;
  }

  /**
   * Returns the background id of the current battle instance
   *
   * @return String background id for the current battle
   */
  public String getBackgroundId() {
    String backgroundId =
        BACKGROUND_IDS[(getRunState().getMapProgression() - 1) % BACKGROUND_IDS.length];
    logger.debug("Background Id: {}", backgroundId);
    return backgroundId;
  }

  // Lives here rather than on a screen, since setScreen() disposes the outgoing screen.
  private final RunState runState = new RunState();
  private final AutosaveCoordinator autosaveCoordinator =
      new AutosaveCoordinator(runState, this::newAutosaveService);

  public RunState getRunState() {
    return runState;
  }

  /** Schedules one autosave for the current run after a completed encounter. */
  public void requestAutosaveAfterEncounter() {
    autosaveCoordinator.requestAfterSuccessfulEncounter();
  }

  /** Flushes a scheduled autosave once the completed encounter's screen has been disposed. */
  public void autosaveOnMapReady() {
    autosaveCoordinator.saveIfPending();
  }

  /**
   * Flushes a victorious battle's autosave after its reward has been applied but before leaving the
   * reward screen. The coordinator owns idempotency, so the following map transition cannot write a
   * duplicate checkpoint.
   */
  public void autosaveAfterRewardClaimed() {
    autosaveCoordinator.saveIfPending();
  }

  private SaveGameService newAutosaveService() {
    CardLibrary cardLibrary = new CardLibrary(CardConfigLoader.loadCards());
    return new SaveGameService(
        new JsonSaveGameRepository(),
        new GameStateSnapshotProvider(
            runState.getOrCreatePlayerState(),
            runState.getOrCreatePlayerDeck(cardLibrary),
            runState,
            bestiaryService,
            cardDiscoveryService));
  }

  @Override
  public void create() {
    logger.info("Creating game");
    loadSettings();
    bestiaryService = BestiaryService.loadDefault();
    cardDiscoveryService = CardDiscoveryService.loadDefault();
    CardDiscoveryStore cardDiscoveryStore = CardDiscoveryStore.defaultStore();
    cardDiscoveryService.mergeProgress(cardDiscoveryStore.load());
    cardDiscoveryService
        .getEvents()
        .addListener(
            CardDiscoveryService.ENTRY_UPDATED_EVENT,
            ignored -> cardDiscoveryStore.save(cardDiscoveryService.getProgressSnapshot()));

    // Sets background to light yellow
    Gdx.gl.glClearColor(162f / 255f, 73 / 255f, 54 / 255f, 1);

    setScreen(ScreenType.MAIN_MENU);
  }

  /** Loads the game's settings. */
  private void loadSettings() {
    logger.debug("Loading game settings");
    UserSettings.Settings settings = UserSettings.get();
    UserSettings.applySettings(settings);
  }

  /**
   * Sets the game's screen to a new screen of the provided type.
   *
   * @param screenType screen type
   */
  public void setScreen(ScreenType screenType) {
    logger.info("Setting game screen to {}", screenType);
    prepareScreenTransition(screenType);
    setScreen(newScreen(screenType));
  }

  /**
   * Plays a story crawl, then continues to {@code next}. An unknown or empty sequence completes on
   * its first frame. Narration file errors skip the crawl and continue to {@code next}.
   *
   * @param sequenceId story sequence to play
   * @param next destination after completion or skipping
   */
  public void showNarration(String sequenceId, ScreenType next) {
    try {
      NarrationConfigLoader.loadSequence(sequenceId);
    } catch (NarrationLoadingException exception) {
      logger.error("Unable to load narration sequence {}", sequenceId, exception);
      setScreen(next);
      return;
    }
    prepareScreenTransition(next);
    super.setScreen(new NarrationScreen(sequenceId, () -> setScreen(next)));
  }

  private void prepareScreenTransition(ScreenType screenType) {
    Screen currentScreen = getScreen();
    if (currentScreen != null) {
      currentScreen.dispose();
    }
    ServiceLocator.registerBestiaryService(bestiaryService);
    ServiceLocator.registerCardDiscoveryService(cardDiscoveryService);
    setScreen(newScreen(screenType));
    AudioService.onScreenChanged(screenType, runState);
  }

  /** Opens the battle screen. */
  public void startBattle() {
    setScreen(ScreenType.BATTLE_SCREEN);
  }

  /**
   * Opens a specific configured Event for the active map node using the persistent run.
   *
   * <p>The caller must first enter a real EVENT node. Validation happens before the current screen
   * is disposed, so an invalid request leaves it in place.
   *
   * @param eventId stable ID from the Event catalogue
   * @throws IllegalArgumentException if the ID is blank or unknown
   * @throws IllegalStateException if no EVENT map node is active
   */
  public void startEvent(String eventId) {
    if (eventId == null || eventId.isBlank()) {
      throw new IllegalArgumentException("Event ID must not be null or blank");
    }
    Integer nodeId = runState.getActiveNodeId();
    MapNode activeNode =
        runState.getMapGraph() == null || nodeId == null
            ? null
            : runState.getMapGraph().getNode(nodeId);
    if (activeNode == null || activeNode.getRoomType() != RoomType.EVENT) {
      throw new IllegalStateException("startEvent requires an active EVENT node");
    }
    new ChanceEncounterSelector(ChanceEncounterFactory.createInitialEncounters(), new Random())
        .selectById(eventId);

    prepareScreenTransition(ScreenType.ENCOUNTER);
    setScreen(new EncounterScreen(this, eventId));
  }

  /** Opens a temporary Event preview without entering or changing the run map. */
  public void openDemoEvent() {
    openDemoEvent(null);
  }

  /** Opens the Card Fusion catalogue Event directly for UI work. */
  public void openDemoCardFusion() {
    openDemoEvent(CardFusionEncounterBehaviour.ENCOUNTER_ID);
  }

  private void openDemoEvent(String previewEncounterId) {
    prepareScreenTransition(ScreenType.ENCOUNTER);
    setScreen(new DemoEventScreen(this, previewEncounterId));
  }

  /** Opens a temporary Campfire preview with no map node or persistent run changes. */
  public void openDemoCampfire() {
    Screen currentScreen = getScreen();
    if (currentScreen != null) {
      currentScreen.dispose();
    }
    ServiceLocator.registerBestiaryService(bestiaryService);
    prepareScreenTransition(ScreenType.CAMPFIRE);
    setScreen(new DemoCampfireScreen(this));
  }

  /** Opens a temporary Shop preview using isolated player state and no map node. */
  public void openDemoShop() {
    Screen currentScreen = getScreen();
    if (currentScreen != null) {
      currentScreen.dispose();
    }
    ServiceLocator.registerBestiaryService(bestiaryService);
    setScreen(new DemoShopScreen(this));
  }

  /** Temporary development shortcut for previewing the Elite portal flow. */
  public void startElitePortalDebug() {
    runState.setPendingEliteTempleReward(true);
    setScreen(ScreenType.ELITE_PORTAL);
  }

  @Override
  public void dispose() {
    logger.debug("Disposing of current screen");
    getScreen().dispose();
  }

  /**
   * Create a new screen of the provided type.
   *
   * @param screenType screen type
   * @return new screen
   */
  private Screen newScreen(ScreenType screenType) {
    switch (screenType) {
      case MAIN_MENU:
        return new MainMenuScreen(this);
      case MAIN_GAME:
        return new MainGameScreen(this);
      case SETTINGS:
        return new SettingsScreen(this);
      case SAVE_LOAD:
        return new SaveLoadScreen(this);
      case LIBRARY:
        return new LibraryScreen(this);
      case CARD_LIBRARY:
        return new CardLibraryScreen(this);
      case MAP:
        return new MapScreen(this);
      case ENCOUNTER:
        return new EncounterScreen(this);
      case CAMPFIRE:
        return new CampfireScreen(this);
      case BATTLE_SCREEN:
        return new BattleScreen(this);
      case VICTORY:
        return new EndBattleScreen(this, true);
      case ELITE_PORTAL:
        return new ElitePortalScreen(this);
      case ANCIENT_TEMPLE:
        return new AncientTempleScreen(this);
      case TEMPLE_CARD_SELECTION:
        return new TempleCardSelectionScreen(this);
      case DEFEAT:
        return new EndBattleScreen(this, false);
      case BESTIARY:
        return new BestiaryScreen(this);
      default:
        return null;
    }
  }

  public enum ScreenType {
    MAIN_MENU,
    MAIN_GAME,
    SETTINGS,
    SAVE_LOAD,
    LIBRARY,
    CARD_LIBRARY,
    MAP,
    ENCOUNTER,
    CAMPFIRE,
    BATTLE_SCREEN,
    VICTORY,
    DEFEAT,
    ELITE_PORTAL,
    ANCIENT_TEMPLE,
    TEMPLE_CARD_SELECTION,
    BESTIARY
  }

  /** Exit the game. */
  public void exit() {
    app.exit();
  }
}
