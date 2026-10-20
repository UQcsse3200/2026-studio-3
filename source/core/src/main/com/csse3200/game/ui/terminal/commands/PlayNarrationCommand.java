package com.csse3200.game.ui.terminal.commands;

import com.badlogic.gdx.Gdx;
import com.csse3200.game.GdxGame;
import java.util.ArrayList;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debug command: previews a narration sequence from the map and returns to the map afterwards. Does
 * not start or end a run. Usage: playnarration &lt;opening|pre_boss|victory|defeat&gt;
 */
public class PlayNarrationCommand implements Command {
  private static final Logger logger = LoggerFactory.getLogger(PlayNarrationCommand.class);

  static final Set<String> SEQUENCE_IDS = Set.of("opening", "pre_boss", "victory", "defeat");

  private final GdxGame game;

  public PlayNarrationCommand(GdxGame game) {
    if (game == null) {
      throw new IllegalArgumentException("game must not be null");
    }
    this.game = game;
  }

  @Override
  public boolean action(ArrayList<String> args) {
    if (args.size() != 1 || !SEQUENCE_IDS.contains(args.get(0))) {
      logger.info("Usage: playnarration <opening|pre_boss|victory|defeat>");
      return false;
    }
    String sequenceId = args.get(0);
    // Defer the screen change until the terminal input callback has finished.
    Gdx.app.postRunnable(() -> game.showNarration(sequenceId, GdxGame.ScreenType.MAP));
    return true;
  }
}
