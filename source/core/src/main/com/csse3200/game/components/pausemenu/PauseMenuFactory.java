package com.csse3200.game.components.pausemenu;

import com.badlogic.gdx.utils.Align;
import com.csse3200.game.GdxGame;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.deck.PlayerDeck;
import com.csse3200.game.components.mainmenu.MainMenuDisplay;
import com.csse3200.game.components.save.SaveLoadPanel;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.maps.PlayerRunState;
import com.csse3200.game.maps.RunState;
import com.csse3200.game.save.GameStateSnapshotProvider;
import com.csse3200.game.save.JsonSaveGameRepository;
import com.csse3200.game.save.SaveGameRestoreService;
import com.csse3200.game.save.SaveGameService;
import com.csse3200.game.services.GamePauseService;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.services.audio.AudioService;
import java.util.List;

/**
 * Assembles the in-game pause menu cluster — {@link PauseMenuDisplay}, {@link PauseMenuInput},
 * {@link PauseMenuActions} and an in-place {@link SaveLoadPanel} overlay — and attaches it to a
 * screen's UI entity, so each gameplay screen wires the pause menu with a single call.
 *
 * <p>It registers a {@link GamePauseService} if the screen didn't, and loads the shared menu
 * textures the styled pause menu / save panel need.
 */
public final class PauseMenuFactory {
  private static final List<Integer> SAVE_SLOT_IDS = List.of(1, 2, 3);

  private PauseMenuFactory() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Adds the pause menu (and its save/load overlay) to {@code uiEntity}. Call before the screen
   * registers the entity; then call {@link SaveLoadPanel#hide()} on the returned panel once the
   * entity has been created, so the save overlay starts hidden.
   *
   * @param uiEntity the screen's UI entity to attach to
   * @param game the running game, used for run state and screen navigation
   * @return the save/load panel, so the caller can hide it after the entity is created
   */
  public static SaveLoadPanel attach(Entity uiEntity, GdxGame game) {
    return attach(uiEntity, game, Align.topRight);
  }

  /**
   * As {@link #attach(Entity, GdxGame)}, but places the on-screen pause button in the given corner
   * (an {@link Align} constant) so each screen can keep it clear of its own HUD.
   */
  public static SaveLoadPanel attach(Entity uiEntity, GdxGame game, int pauseButtonAlign) {
    return attach(uiEntity, game, new PauseButtonDisplay(pauseButtonAlign));
  }

  /**
   * As above, but also insets the pause button from the screen edges, to nudge it clear of nearby
   * HUD (e.g. beside the Map's own Main Menu button).
   */
  public static SaveLoadPanel attach(
      Entity uiEntity, GdxGame game, int pauseButtonAlign, float pauseButtonEdgePad) {
    return attach(uiEntity, game, new PauseButtonDisplay(pauseButtonAlign, pauseButtonEdgePad));
  }

  /**
   * Attaches the pause menu with Escape-only access (no on-screen pause button) — for screens where
   * the button doesn't fit the HUD, e.g. the map.
   */
  public static SaveLoadPanel attachWithoutButton(Entity uiEntity, GdxGame game) {
    return attach(uiEntity, game, (PauseButtonDisplay) null);
  }

  private static SaveLoadPanel attach(
      Entity uiEntity, GdxGame game, PauseButtonDisplay pauseButton) {
    ensurePauseService();
    loadMenuAssets();

    SaveLoadPanel savePanel = buildSavePanel(game);
    uiEntity
        .addComponent(new PauseMenuDisplay())
        .addComponent(new PauseMenuInput())
        .addComponent(new PauseMenuActions(game))
        .addComponent(savePanel);
    if (pauseButton != null) {
      uiEntity.addComponent(pauseButton);
    }

    // Open the save overlay when the pause menu's Save & Load button fires its event.
    uiEntity.getEvents().addListener(PauseMenuDisplay.SAVE_LOAD_EVENT, savePanel::show);

    // Swap to the pause music while the menu is open.
    uiEntity.getEvents().addListener(PauseMenuDisplay.PAUSE_EVENT, AudioService::onGamePaused);
    uiEntity.getEvents().addListener(PauseMenuDisplay.RESUME_EVENT, AudioService::onGameResumed);
    return savePanel;
  }

  private static void ensurePauseService() {
    if (ServiceLocator.getPauseService() == null) {
      ServiceLocator.registerPauseService(new GamePauseService(ServiceLocator.getTimeSource()));
    }
  }

  private static void loadMenuAssets() {
    ResourceService resources = ServiceLocator.getResourceService();
    resources.loadTextures(menuTexturePaths());
    resources.loadAll();
  }

  static String[] menuTexturePaths() {
    return new String[] {
      MainMenuDisplay.BACKGROUND_TEXTURE,
      MainMenuDisplay.BUTTON_FRAME_TEXTURE,
      SaveLoadPanel.BUTTON_TEXTURE
    };
  }

  private static SaveLoadPanel buildSavePanel(GdxGame game) {
    RunState runState = game.getRunState();
    PlayerRunState playerState = runState.getOrCreatePlayerState();

    CardLibrary cardLibrary = ServiceLocator.getCardLibrary();
    if (cardLibrary == null) {
      cardLibrary = new CardLibrary(CardConfigLoader.loadCards());
    }
    PlayerDeck playerDeck = runState.getOrCreatePlayerDeck(cardLibrary);

    SaveGameService saveGameService =
        new SaveGameService(
            new JsonSaveGameRepository(),
            new GameStateSnapshotProvider(
                playerState,
                playerDeck,
                runState,
                game.getBestiaryService(),
                game.getCardDiscoveryService()));
    SaveGameRestoreService restoreService =
        new SaveGameRestoreService(
            playerState,
            playerDeck,
            runState,
            game.getBestiaryService(),
            game.getCardDiscoveryService());

    // The Back button hides the overlay (returns to the pause menu). Held via a one-element array
    // so the lambda can reference the panel that is being constructed.
    SaveLoadPanel[] holder = new SaveLoadPanel[1];
    SaveLoadPanel panel =
        new SaveLoadPanel(saveGameService, SAVE_SLOT_IDS, restoreService, () -> holder[0].hide());
    holder[0] = panel;
    return panel;
  }
}
