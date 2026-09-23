package com.csse3200.game.components.chance;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.fusion.CardFusionFailureReason;
import com.csse3200.game.cards.fusion.CardFusionResult;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.encounters.integration.CardFusionEncounterFlow;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** A functional Card Fusion selection screen; its appearance can be replaced independently. */
final class CardFusionSelectionView {
  private static final int REQUIRED_CARDS = 3;
  private static final Color PANEL_COLOUR = new Color(0.12f, 0.08f, 0.09f, 0.98f);
  private static final Color CARD_COLOUR = new Color(0.28f, 0.18f, 0.16f, 1f);
  private static final Color SELECTED_COLOUR = new Color(0.54f, 0.36f, 0.18f, 1f);

  private final CardFusionEncounterFlow flow;
  private final CardService cardService;
  private final Skin skin;
  private final Stage stage;
  private final Set<String> selectedInstanceIds = new LinkedHashSet<>();
  private final Map<String, TextButton> cardButtons = new LinkedHashMap<>();

  private Table root;
  private Table cardsGrid;
  private Label selectionLabel;
  private Label feedbackLabel;
  private TextButton fuseButton;
  private TextButton leaveButton;
  private boolean unavailable;
  private boolean completed;

  CardFusionSelectionView(
      CardFusionEncounterFlow flow, CardService cardService, Skin skin, Stage stage) {
    this.flow = Objects.requireNonNull(flow, "flow cannot be null");
    this.cardService = Objects.requireNonNull(cardService, "cardService cannot be null");
    this.skin = Objects.requireNonNull(skin, "skin cannot be null");
    this.stage = Objects.requireNonNull(stage, "stage cannot be null");
  }

  void show() {
    if (root != null) {
      return;
    }

    root = new Table();
    root.setFillParent(true);
    root.setBackground(skin.newDrawable("white", new Color(0.02f, 0.015f, 0.02f, 1f)));

    Table panel = new Table();
    panel.setBackground(skin.newDrawable("window-w", PANEL_COLOUR));
    panel.pad(22f);

    Label title = new Label("CARD FUSION", skin, "title");
    panel.add(title).left().padBottom(12f);
    panel.row();

    Label instructions =
        new Label(
            "Select three Common card copies. Different copies of the same card are allowed.",
            skin);
    instructions.setWrap(true);
    panel.add(instructions).left().width(970f).padBottom(15f);
    panel.row();

    cardsGrid = new Table();
    cardsGrid.top().left();
    ScrollPane scrollPane = new ScrollPane(cardsGrid, skin);
    scrollPane.setFadeScrollBars(false);
    panel.add(scrollPane).width(970f).height(360f).padBottom(12f);
    panel.row();

    selectionLabel = new Label("Selected: 0 / 3", skin);
    panel.add(selectionLabel).left().padBottom(8f);
    panel.row();

    feedbackLabel = new Label("Choose three cards, or leave the forge.", skin);
    feedbackLabel.setWrap(true);
    panel.add(feedbackLabel).left().width(970f).height(52f).padBottom(12f);
    panel.row();

    Table actions = new Table();
    fuseButton = new TextButton("Fuse 3 Cards", skin);
    fuseButton.setDisabled(true);
    fuseButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            attemptFusion();
          }
        });
    leaveButton = new TextButton("Leave", skin);
    leaveButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            leave();
          }
        });
    actions.add(fuseButton).width(300f).height(60f).padRight(20f);
    actions.add(leaveButton).width(220f).height(60f);
    panel.add(actions).right();

    root.add(panel).width(1040f).height(730f);
    stage.addActor(root);
    refreshCards();
  }

  private void refreshCards() {
    selectedInstanceIds.clear();
    cardButtons.clear();
    cardsGrid.clearChildren();

    List<CardInstance> eligibleCards = flow.getEligibleCards();
    Map<String, Integer> copyNumbers = new LinkedHashMap<>();
    TextButtonStyle cardStyle = createCardStyle();
    for (int i = 0; i < eligibleCards.size(); i++) {
      CardInstance card = eligibleCards.get(i);
      int copyNumber = copyNumbers.merge(card.cardId(), 1, Integer::sum);
      String name = cardName(card.cardId());
      String caption = name + "  (Copy " + copyNumber + ")";
      if (card.isUpgraded()) {
        caption += "  +";
      }

      TextButton cardButton = new TextButton(caption, cardStyle);
      cardButton.setName(card.instanceId());
      cardButton.setProgrammaticChangeEvents(false);
      cardButton.getLabel().setWrap(true);
      cardButton.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              updateCardSelection(cardButton, card.instanceId());
            }
          });
      cardButtons.put(card.instanceId(), cardButton);
      cardsGrid.add(cardButton).width(300f).height(84f).pad(6f);
      if ((i + 1) % 3 == 0) {
        cardsGrid.row();
      }
    }

    updateSelectionLabel();
    if (eligibleCards.size() < REQUIRED_CARDS) {
      feedbackLabel.setText("You need at least three Common cards. You can leave the forge.");
    }
  }

  private TextButtonStyle createCardStyle() {
    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));
    style.up = skin.newDrawable("button", CARD_COLOUR);
    style.over = skin.newDrawable("button", new Color(0.39f, 0.25f, 0.19f, 1f));
    style.down = skin.newDrawable("button", SELECTED_COLOUR);
    style.checked = skin.newDrawable("button", SELECTED_COLOUR);
    style.checkedOver = style.checked;
    style.fontColor = Color.WHITE;
    return style;
  }

  private void updateCardSelection(TextButton button, String instanceId) {
    if (completed || unavailable) {
      return;
    }
    if (button.isChecked()) {
      if (selectedInstanceIds.size() == REQUIRED_CARDS) {
        button.setChecked(false);
        feedbackLabel.setText("Only three card copies can be selected.");
        return;
      }
      selectedInstanceIds.add(instanceId);
    } else {
      selectedInstanceIds.remove(instanceId);
    }
    updateSelectionLabel();
  }

  private void updateSelectionLabel() {
    selectionLabel.setText("Selected: " + selectedInstanceIds.size() + " / 3");
    fuseButton.setDisabled(
        completed || unavailable || selectedInstanceIds.size() != REQUIRED_CARDS);
  }

  private void attemptFusion() {
    if (completed || unavailable || selectedInstanceIds.size() != REQUIRED_CARDS) {
      return;
    }

    CardFusionResult result = flow.fuse(List.copyOf(selectedInstanceIds));
    if (result.successful()) {
      completed = true;
      String reward = cardName(result.rewardedCardId().orElseThrow());
      feedbackLabel.setText("Fusion complete! You received the Rare card " + reward + ".");
      disableSelections();
      leaveButton.setDisabled(true);
      return;
    }

    CardFusionFailureReason reason = result.failureReason();
    feedbackLabel.setText(failureMessage(reason));
    if (reason == CardFusionFailureReason.FUSION_ALREADY_USED
        || reason == CardFusionFailureReason.NO_RARE_CARD_AVAILABLE) {
      unavailable = true;
      disableSelections();
    } else if (reason == CardFusionFailureReason.CARD_NOT_IN_DECK
        || reason == CardFusionFailureReason.CARD_NOT_COMMON) {
      refreshCards();
      feedbackLabel.setText(failureMessage(reason));
    }
  }

  private void disableSelections() {
    for (TextButton button : cardButtons.values()) {
      button.setDisabled(true);
    }
    fuseButton.setDisabled(true);
  }

  private void leave() {
    if (completed) {
      return;
    }
    completed = true;
    disableSelections();
    leaveButton.setDisabled(true);
    feedbackLabel.setText("You leave the forge without fusing any cards.");
    flow.leave();
  }

  private String cardName(String cardId) {
    return cardService
        .getCard(cardId)
        .map(card -> card.name == null || card.name.isBlank() ? card.id : card.name)
        .orElse(cardId);
  }

  private static String failureMessage(CardFusionFailureReason reason) {
    return switch (reason) {
      case INVALID_SELECTION -> "Choose exactly three different card copies and try again.";
      case CARD_NOT_IN_DECK -> "One selected card is no longer in your deck. Choose again.";
      case CARD_NOT_COMMON -> "One selected card is not Common. Choose again.";
      case FUSION_ALREADY_USED -> "You have already used Card Fusion this run. Leave the forge.";
      case NO_RARE_CARD_AVAILABLE -> "No Rare card is available. Leave the forge.";
      case NONE -> "Fusion completed.";
    };
  }

  void dispose() {
    if (root != null) {
      root.remove();
      root = null;
    }
  }

  Map<String, TextButton> getCardButtons() {
    return Map.copyOf(cardButtons);
  }

  TextButton getFuseButton() {
    return fuseButton;
  }

  TextButton getLeaveButton() {
    return leaveButton;
  }

  String getFeedbackText() {
    return feedbackLabel.getText().toString();
  }

  List<String> getSelectedInstanceIds() {
    return List.copyOf(selectedInstanceIds);
  }
}
