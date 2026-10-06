package com.csse3200.game.screens;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.GdxGame;
import com.csse3200.game.maps.RunState;

/** Temporary map-free preview of the real Campfire UI, using only an isolated run state. */
public final class DemoCampfireScreen extends CampfireScreen {
  private final GdxGame game;
  private boolean completionQueued;

  public DemoCampfireScreen(GdxGame game) {
    super(game, new RunState());
    this.game = game;
  }

  @Override
  protected void finishCampfire() {
    if (completionQueued) {
      return;
    }
    completionQueued = true;
    Gdx.app.postRunnable(() -> game.setScreen(GdxGame.ScreenType.MAIN_MENU));
  }
}
