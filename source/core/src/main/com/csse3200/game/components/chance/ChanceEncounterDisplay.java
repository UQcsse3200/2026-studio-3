package com.csse3200.game.components.chance;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.chance.ChanceChoice;
import com.csse3200.game.chance.ChanceEncounter;
import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.encounters.integration.ChanceEncounterSession;
import com.csse3200.game.encounters.integration.ChanceResolution;
import com.csse3200.game.maps.EncounterCallback;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Displays a Chance Encounter using data supplied by the Chance Encounter Core system.
 *
 * <p>The UI displays the encounter description and available choices, sends the selected choice to
 * {@link ChanceEncounter#resolveChoice(String)}, and displays the returned outcome. This component
 * does not directly modify player health, gold, cards, or map state.
 *
 * <p>When the player presses Continue, the optional {@link EncounterCallback} is notified that the
 * encounter has finished.
 */
public class ChanceEncounterDisplay extends UIComponent {
  private static final float Z_INDEX = 3f;
  private static final float PANEL_WIDTH = 1080f;
  private static final float CONTENT_WIDTH = 980f;

  private static final Color BACKDROP_COLOUR = new Color(0.02f, 0.015f, 0.025f, 0.82f);
  private static final Color PANEL_COLOUR = new Color(0.11f, 0.075f, 0.08f, 0.98f);
  private static final Color RESULT_COLOUR = new Color(0.07f, 0.045f, 0.05f, 1f);
  private static final Color GOLD_COLOUR = new Color(0.94f, 0.72f, 0.3f, 1f);
  private static final Color BODY_COLOUR = new Color(0.9f, 0.84f, 0.73f, 1f);
  private static final Color MUTED_COLOUR = new Color(0.66f, 0.59f, 0.53f, 1f);

  private final ChanceEncounter encounter;
  private final ChanceEncounterSession encounterSession;
  private final EncounterCallback completionCallback;
  private final Integer nodeId;
  private final List<TextButton> choiceButtons = new ArrayList<>();

  private Table rootTable;
  private Label resultLabel;
  private TextButton continueButton;
  private TextButtonStyle selectedChoiceStyle;
  private boolean choiceResolved;
  private boolean completionSent;

  /**
   * Creates a display without a map-completion callback, suitable for UI previews.
   *
   * @param encounter encounter information and choices displayed by the UI
   */
  public ChanceEncounterDisplay(ChanceEncounter encounter) {
    this(encounter, null, null);
  }

  /**
   * Creates a display connected to the shared encounter lifecycle.
   *
   * @param encounter encounter information and choices displayed by the UI
   * @param completionCallback callback notified when Continue is selected, or null for a preview
   * @param nodeId map node associated with this encounter
   */
  public ChanceEncounterDisplay(
      ChanceEncounter encounter, EncounterCallback completionCallback, Integer nodeId) {
    this(encounter, completionCallback, nodeId, null);
  }

  /**
   * Creates a display backed by a fully integrated Chance Encounter session.
   *
   * @param encounterSession session that applies outcomes and reports completion to the map
   */
  public ChanceEncounterDisplay(ChanceEncounterSession encounterSession) {
    this(
        Objects.requireNonNull(encounterSession, "encounterSession cannot be null").getEncounter(),
        null,
        encounterSession.getNodeId(),
        encounterSession);
  }

  private ChanceEncounterDisplay(
      ChanceEncounter encounter,
      EncounterCallback completionCallback,
      Integer nodeId,
      ChanceEncounterSession encounterSession) {
    this.encounter = Objects.requireNonNull(encounter, "encounter cannot be null");
    this.encounterSession = encounterSession;
    this.completionCallback = completionCallback;
    this.nodeId = Objects.requireNonNull(nodeId, "nodeId cannot be null");
  }

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.setBackground(skin.newDrawable("white", BACKDROP_COLOUR));
    rootTable.setTouchable(Touchable.enabled);
    rootTable.center();
    rootTable.getColor().a = 0f;

    Table encounterTable = new Table();
    encounterTable.setBackground(skin.newDrawable("window-w", PANEL_COLOUR));
    encounterTable.pad(30f, 40f, 34f, 40f);
    encounterTable.defaults().spaceBottom(14f);

    Label eyebrowLabel = new Label("CHANCE ENCOUNTER", createLabelStyle("small", GOLD_COLOUR));
    Label titleLabel =
        new Label(formatTitle(encounter.getId()), createLabelStyle("large", BODY_COLOUR));
    Label descriptionLabel =
        new Label(encounter.getDescription(), createLabelStyle("default", BODY_COLOUR));
    eyebrowLabel.setFontScale(1.25f);
    titleLabel.setFontScale(1.4f);
    descriptionLabel.setFontScale(1.3f);
    descriptionLabel.setWrap(true);

    encounterTable.add(eyebrowLabel).center().width(CONTENT_WIDTH);
    encounterTable.row();
    encounterTable.add(titleLabel).center().width(CONTENT_WIDTH);
    encounterTable.row();
    encounterTable.add(createDivider()).height(2f).width(CONTENT_WIDTH).padTop(2f).padBottom(8f);
    encounterTable.row();
    encounterTable.add(descriptionLabel).left().width(CONTENT_WIDTH).padBottom(12f);
    encounterTable.row();

    Label promptLabel = new Label("CHOOSE YOUR RESPONSE", createLabelStyle("small", MUTED_COLOUR));
    promptLabel.setFontScale(1.25f);
    encounterTable.add(promptLabel).left().width(CONTENT_WIDTH).padBottom(2f);
    encounterTable.row();

    TextButtonStyle choiceStyle = createChoiceStyle();
    selectedChoiceStyle = createSelectedChoiceStyle(choiceStyle);
    int choiceNumber = 1;
    for (ChanceChoice choice : encounter.getChoices()) {
      addChoiceButton(encounterTable, choice, choiceNumber, choiceStyle);
      choiceNumber++;
    }

    Table resultTable = new Table();
    resultTable.setBackground(skin.newDrawable("white", RESULT_COLOUR));
    resultTable.pad(14f, 18f, 14f, 18f);
    resultLabel =
        new Label(
            "Your decision will determine the outcome.", createLabelStyle("default", MUTED_COLOUR));
    resultLabel.setFontScale(1.3f);
    resultLabel.setWrap(true);
    resultTable.add(resultLabel).left().width(CONTENT_WIDTH - 36f);

    encounterTable.add(resultTable).left().width(CONTENT_WIDTH).padTop(6f);
    encounterTable.row();

    continueButton = new TextButton("Continue", createContinueStyle());
    continueButton.getLabel().setFontScale(1.3f);
    continueButton.setVisible(false);
    continueButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            completeEncounter();
          }
        });
    encounterTable.add(continueButton).right().width(280f).height(70f).padTop(10f);

    rootTable.add(encounterTable).width(PANEL_WIDTH);
    stage.addActor(rootTable);
    rootTable.addAction(Actions.fadeIn(0.25f));
  }

  private Table createDivider() {
    Table divider = new Table();
    divider.setBackground(skin.newDrawable("white", GOLD_COLOUR));
    return divider;
  }

  private LabelStyle createLabelStyle(String baseStyle, Color colour) {
    LabelStyle labelStyle = new LabelStyle(skin.get(baseStyle, LabelStyle.class));
    labelStyle.fontColor = colour;
    return labelStyle;
  }

  private TextButtonStyle createChoiceStyle() {
    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));
    style.up = skin.newDrawable("button", new Color(0.27f, 0.17f, 0.14f, 1f));
    style.over = skin.newDrawable("button", new Color(0.48f, 0.29f, 0.16f, 1f));
    style.down = skin.newDrawable("button-pressed", new Color(0.62f, 0.4f, 0.2f, 1f));
    style.disabled = skin.newDrawable("button", new Color(0.12f, 0.1f, 0.11f, 1f));
    style.fontColor = BODY_COLOUR;
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.WHITE;
    style.disabledFontColor = MUTED_COLOUR;
    return style;
  }

  private TextButtonStyle createSelectedChoiceStyle(TextButtonStyle choiceStyle) {
    TextButtonStyle style = new TextButtonStyle(choiceStyle);
    style.disabled = skin.newDrawable("button", new Color(0.53f, 0.34f, 0.16f, 1f));
    style.disabledFontColor = Color.WHITE;
    return style;
  }

  private TextButtonStyle createContinueStyle() {
    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));
    style.up = skin.newDrawable("button", new Color(0.52f, 0.3f, 0.11f, 1f));
    style.over = skin.newDrawable("button", new Color(0.75f, 0.48f, 0.18f, 1f));
    style.down = skin.newDrawable("button-pressed", new Color(0.4f, 0.22f, 0.08f, 1f));
    style.fontColor = Color.WHITE;
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.WHITE;
    return style;
  }

  private void addChoiceButton(
      Table encounterTable, ChanceChoice choice, int choiceNumber, TextButtonStyle choiceStyle) {
    String buttonText = String.format("%d.  %s", choiceNumber, choice.getDescription());
    TextButton choiceButton = new TextButton(buttonText, choiceStyle);
    choiceButton.getLabel().setFontScale(1.3f);
    choiceButton.getLabel().setWrap(true);
    choiceButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            resolveChoice(choice, choiceButton, buttonText);
          }
        });

    choiceButtons.add(choiceButton);
    encounterTable.add(choiceButton).left().width(CONTENT_WIDTH).minHeight(80f);
    encounterTable.row();
  }

  private void resolveChoice(
      ChanceChoice choice, TextButton selectedButton, String originalButtonText) {
    if (choiceResolved) {
      return;
    }

    ChanceOutcome outcome;
    if (encounterSession == null) {
      outcome = encounter.resolveChoice(choice.getId());
    } else {
      ChanceResolution resolution = encounterSession.resolveChoice(choice.getId());
      if (!resolution.isSuccess()) {
        resultLabel.setStyle(createLabelStyle("default", new Color(0.9f, 0.35f, 0.3f, 1f)));
        resultLabel.setText("OUTCOME\n" + resolution.getMessage());
        return;
      }
      outcome = resolution.getOutcome();
    }
    if (outcome == null) {
      resultLabel.setStyle(createLabelStyle("default", new Color(0.9f, 0.35f, 0.3f, 1f)));
      resultLabel.setText(
          "OUTCOME\nThis choice could not be resolved. Please select another option.");
      return;
    }

    choiceResolved = true;
    for (TextButton button : choiceButtons) {
      button.setDisabled(true);
    }
    selectedButton.setStyle(selectedChoiceStyle);
    selectedButton.setText("SELECTED  -  " + originalButtonText);

    resultLabel.setStyle(createLabelStyle("default", BODY_COLOUR));
    resultLabel.setText("OUTCOME\n" + formatOutcome(outcome));
    continueButton.setVisible(true);
  }

  private void completeEncounter() {
    if (!choiceResolved || completionSent) {
      return;
    }

    completionSent = true;
    rootTable.addAction(
        Actions.sequence(
            Actions.fadeOut(0.2f), Actions.run(this::notifyCompletion), Actions.removeActor()));
  }

  private void notifyCompletion() {
    if (encounterSession != null) {
      encounterSession.complete();
    } else if (completionCallback != null) {
      completionCallback.onEncounterComplete(nodeId, true);
    }
  }

  /**
   * Converts a Chance Outcome into player-facing result text.
   *
   * @param outcome outcome returned by the Chance Encounter Core system
   * @return formatted result text
   */
  static String formatOutcome(ChanceOutcome outcome) {
    if (outcome == null) {
      return "This choice could not be resolved.";
    }
    if (outcome.isNoEffect()) {
      return "Nothing happens. You continue on your way.";
    }

    List<String> changes = new ArrayList<>();
    if (outcome.getHealthDelta() > 0) {
      changes.add(String.format("You recover %d health.", outcome.getHealthDelta()));
    } else if (outcome.getHealthDelta() < 0) {
      changes.add(String.format("You lose %d health.", -outcome.getHealthDelta()));
    }
    if (outcome.getCurrencyDelta() > 0) {
      changes.add(String.format("You gain %d gold.", outcome.getCurrencyDelta()));
    } else if (outcome.getCurrencyDelta() < 0) {
      changes.add(String.format("You lose %d gold.", -outcome.getCurrencyDelta()));
    }
    return String.join("\n", changes);
  }

  /**
   * Converts an encounter identifier such as {@code mysterious-shrine} into a display title.
   *
   * @param id stable encounter identifier
   * @return player-facing encounter title
   */
  static String formatTitle(String id) {
    String[] words = id.replace('_', '-').split("-");
    StringBuilder title = new StringBuilder();
    for (String word : words) {
      if (word.isEmpty()) {
        continue;
      }
      if (!title.isEmpty()) {
        title.append(' ');
      }
      title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
    }
    return title.toString();
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Drawing is handled by the stage.
  }

  @Override
  public float getZIndex() {
    return Z_INDEX;
  }

  @Override
  public void dispose() {
    if (rootTable != null) {
      rootTable.remove();
    }
    super.dispose();
  }
}
