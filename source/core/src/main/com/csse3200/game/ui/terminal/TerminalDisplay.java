package com.csse3200.game.ui.terminal;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.csse3200.game.ui.UIComponent;

/**
 * A ui component for displaying the debug terminal. The terminal is positioned at the bottom of the
 * screen.
 */
public class TerminalDisplay extends UIComponent {
  private static final float Z_INDEX = 10f;
  // Dark translucent backdrop so the help text stays readable over cards and map art.
  private static final Color HELP_BACKGROUND = new Color(0f, 0f, 0f, 0.75f);
  private static final float HELP_PADDING = 8f;
  private static final float HELP_INPUT_GAP = 4f;
  private Terminal terminal;
  private Label label;
  private Label helpLabel;
  private String helpText = "";

  @Override
  public void create() {
    super.create();
    addActors();
    terminal = entity.getComponent(Terminal.class);
  }

  private void addActors() {
    String message = "";
    Label.LabelStyle style = new Label.LabelStyle(skin.get("default", Label.LabelStyle.class));
    style.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
    label = new Label("> " + message, style);
    label.setPosition(5f, 0);
    stage.addActor(label);

    // Help panel: sits just above the input line and lists the commands for this screen.
    Label.LabelStyle helpStyle = new Label.LabelStyle(style);
    Drawable background = skin.newDrawable("white", HELP_BACKGROUND);
    if (background instanceof BaseDrawable padded) {
      padded.setLeftWidth(HELP_PADDING);
      padded.setRightWidth(HELP_PADDING);
      padded.setTopHeight(HELP_PADDING);
      padded.setBottomHeight(HELP_PADDING);
    }
    helpStyle.background = background;
    helpLabel = new Label("", helpStyle);
    helpLabel.setPosition(5f, label.getHeight() + HELP_INPUT_GAP);
    helpLabel.setVisible(false);
    stage.addActor(helpLabel);
  }

  /** Builds the help text from the terminal's context name and its registered commands. */
  private String buildHelpText() {
    StringBuilder text = new StringBuilder(terminal.getContextName()).append(" (F1 to close)");
    for (String line : terminal.getHelpLines()) {
      text.append("\n  ").append(line);
    }
    return text.toString();
  }

  @Override
  public void draw(SpriteBatch batch) {
    if (terminal.isOpen()) {
      label.setVisible(true);
      // Bring the label back above anything added to the stage since it was created — e.g. the
      // hand gets rebuilt (fresh actors added to the same stage) every time a card is played,
      // which otherwise renders those cards in front of the terminal text.
      label.toFront();
      String message = terminal.getEnteredMessage();
      label.setText("> " + message);

      // Commands are registered once per screen, so only rebuild the text when it changes.
      String latest = buildHelpText();
      if (!latest.equals(helpText)) {
        helpText = latest;
        helpLabel.setText(helpText);
        helpLabel.pack();
      }
      helpLabel.setVisible(true);
      helpLabel.toFront();
    } else {
      label.setVisible(false);
      helpLabel.setVisible(false);
    }
  }

  @Override
  public float getZIndex() {
    return Z_INDEX;
  }

  @Override
  public void dispose() {
    super.dispose();
    label.remove();
    helpLabel.remove();
  }
}
