package com.csse3200.game.components.pausemenu;

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
    ensurePauseService();
    loadMenuAssets();

    SaveLoadPanel savePanel = buildSavePanel(game);

    uiEntity
        .addComponent(new PauseMenuDisplay())
        .addComponent(new PauseMenuInput())
        .addComponent(new PauseMenuActions(game))
        .addComponent(savePanel);

    // Open the save overlay when the pause menu's Save & Load button fires its event.
    uiEntity.getEvents().addListener(PauseMenuDisplay.SAVE_LOAD_EVENT, savePanel::show);
    return savePanel;
  }

  private static void ensurePauseService() {
    if (ServiceLocator.getPauseService() == null) {
      ServiceLocator.registerPauseService(new GamePauseService(ServiceLocator.getTimeSource()));
    }
  }

  private static void loadMenuAssets() {
    ResourceService resources = ServiceLocator.getResourceService();
    resources.loadTextures(
        new String[] {MainMenuDisplay.BACKGROUND_TEXTURE, MainMenuDisplay.BUTTON_FRAME_TEXTURE});
    resources.loadAll();
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
                playerState, playerDeck, runState, game.getBestiaryService()));
    SaveGameRestoreService restoreService =
        new SaveGameRestoreService(playerState, playerDeck, runState, game.getBestiaryService());

    // The Back button hides the overlay (returns to the pause menu). Held via a one-element array
    // so the lambda can reference the panel that is being constructed.
    SaveLoadPanel[] holder = new SaveLoadPanel[1];
    SaveLoadPanel panel =
        new SaveLoadPanel(saveGameService, SAVE_SLOT_IDS, restoreService, () -> holder[0].hide());
    holder[0] = panel;
    return panel;
  }
}
