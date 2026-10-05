package com.csse3200.game.components.chance;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.cards.CardService;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.fusion.CardFusionFailureReason;
import com.csse3200.game.cards.fusion.CardFusionResult;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.encounters.integration.CardFusionEncounterFlow;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Pixel-art presentation of the existing delegated Card Fusion flow. */
final class CardFusionSelectionView {
  private static final int REQUIRED_CARDS = 3;
  private static final Color CREAM = new Color(0.91f, 0.86f, 0.76f, 1f);
  private static final Color MUTED = new Color(0.71f, 0.66f, 0.63f, 1f);
  private static final Color GOLD = new Color(0.94f, 0.68f, 0.34f, 1f);
  private static final Color TITLE_GOLD = new Color(0.98f, 0.74f, 0.42f, 1f);
  private static final Color DARK_GOLD = new Color(0.43f, 0.31f, 0.17f, 0.98f);
  private static final Color SELECTED = new Color(0.96f, 0.65f, 0.29f, 0.98f);
  private static final Color REWARD_GOLD = new Color(0.78f, 0.70f, 0.46f, 1f);

  private final CardFusionEncounterFlow flow;
  private final CardService cardService;
  private final Skin skin;
  private final Stage stage;
  private final Set<String> selectedInstanceIds = new LinkedHashSet<>();
  private final Map<String, CardInstance> eligibleInstances = new LinkedHashMap<>();
  private final Map<String, TextButton> cardButtons = new LinkedHashMap<>();
  private final Map<String, Image> cardGlows = new LinkedHashMap<>();

  private Table root;
  private Group scene;
  private Group selectionLayer;
  private Group sequenceLayer;
  private Table cardsGrid;
  private Table selectedPreview;
  private Image statusPanel;
  private Label instructionLabel;
  private Label selectionLabel;
  private Label feedbackLabel;
  private TextButton fuseButton;
  private TextButton leaveButton;
  private TextButton continueButton;
  private ContextualHelpDialog helpDialog;
  private TextButton helpButton;
  private boolean unavailable;
  private boolean completed;
  private boolean exitRequested;

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
    root.setBackground(skin.newDrawable("white", new Color(0.02f, 0.02f, 0.03f, 1f)));
    scene = new Group();
    scene.setSize(FusionSceneAssets.WIDTH, FusionSceneAssets.HEIGHT);
    Image background = FusionSceneAssets.background(skin);
    background.setColor(0.82f, 0.80f, 0.84f, 1f);
    scene.addActor(background);
    selectionLayer = new Group();
    selectionLayer.setSize(FusionSceneAssets.WIDTH, FusionSceneAssets.HEIGHT);
    scene.addActor(selectionLayer);
    buildSelectionLayer();
    sequenceLayer = new Group();
    sequenceLayer.setSize(FusionSceneAssets.WIDTH, FusionSceneAssets.HEIGHT);
    sequenceLayer.setVisible(false);
    scene.addActor(sequenceLayer);
    root.add(scene).size(FusionSceneAssets.WIDTH, FusionSceneAssets.HEIGHT);
    stage.addActor(root);
    refreshCards();
  }

  private void buildSelectionLayer() {
    Image dim = new Image(skin.newDrawable("white", new Color(0.025f, 0.024f, 0.045f, 0.62f)));
    dim.setBounds(0f, 0f, FusionSceneAssets.WIDTH, FusionSceneAssets.HEIGHT);
    dim.setTouchable(Touchable.disabled);
    selectionLayer.addActor(dim);

    Image titlePanel =
        new Image(
            FusionSceneAssets.pixelFrame(
                skin,
                new Color(0.07f, 0.055f, 0.075f, 0.91f),
                DARK_GOLD,
                FusionSceneAssets.FrameKind.TITLE));
    titlePanel.setBounds(210f, 656f, 860f, 94f);
    selectionLayer.addActor(titlePanel);
    Image titleRule = new Image(skin.newDrawable("white", new Color(0.76f, 0.51f, 0.25f, 0.85f)));
    titleRule.setBounds(365f, 667f, 550f, 2f);
    selectionLayer.addActor(titleRule);
    addSmallDiamond(selectionLayer, 640f, 668f, 6f, GOLD);
    LabelStyle titleStyle = new LabelStyle(skin.get("title", LabelStyle.class));
    titleStyle.fontColor = new Color(TITLE_GOLD);
    Label title = new Label("CARD FUSION", titleStyle);
    title.setAlignment(Align.center);
    title.setFontScale(1.36f);
    title.setBounds(262f, 677f, 756f, 64f);
    selectionLayer.addActor(title);

    Image instructionPanel =
        new Image(
            FusionSceneAssets.pixelFrame(
                skin,
                new Color(0.07f, 0.055f, 0.075f, 0.88f),
                DARK_GOLD,
                FusionSceneAssets.FrameKind.INSCRIPTION));
    instructionPanel.setBounds(166f, 605f, 948f, 41f);
    selectionLayer.addActor(instructionPanel);
    String instructionText =
        "Select three Common card copies. Different copies of the same card are allowed.";
    instructionLabel = label(instructionText, CREAM);
    instructionLabel.setAlignment(Align.center);
    instructionLabel.setWrap(false);
    instructionLabel.setBounds(190f, 610f, 900f, 31f);
    GlyphLayout instructionMeasure =
        new GlyphLayout(instructionLabel.getStyle().font, instructionText);
    instructionLabel.setFontScale(Math.min(1f, 888f / instructionMeasure.width));
    selectionLayer.addActor(instructionLabel);

    Image cardsPanel =
        new Image(
            FusionSceneAssets.pixelFrame(
                skin,
                new Color(0.055f, 0.045f, 0.065f, 0.88f),
                DARK_GOLD,
                FusionSceneAssets.FrameKind.DISPLAY));
    cardsPanel.setBounds(150f, 188f, 980f, 407f);
    selectionLayer.addActor(cardsPanel);

    cardsGrid = new Table();
    cardsGrid.top().left();
    ScrollPane scrollPane = new ScrollPane(cardsGrid, skin);
    scrollPane.setFadeScrollBars(false);
    scrollPane.setScrollbarsOnTop(true);
    scrollPane.setScrollingDisabled(true, false);
    scrollPane.setBounds(200f, 205f, 880f, 374f);
    selectionLayer.addActor(scrollPane);

    statusPanel =
        new Image(
            FusionSceneAssets.pixelFrame(
                skin,
                new Color(0.075f, 0.06f, 0.075f, 0.92f),
                DARK_GOLD,
                FusionSceneAssets.FrameKind.STATUS));
    statusPanel.setBounds(142f, 35f, 626f, 135f);
    selectionLayer.addActor(statusPanel);
    selectionLabel = label("Selected: 0 / 3", GOLD);
    selectionLabel.setFontScale(1.02f);
    selectionLabel.setBounds(170f, 105f, 245f, 38f);
    selectionLayer.addActor(selectionLabel);
    selectedPreview = new Table();
    selectedPreview.left();
    selectedPreview.setBounds(442f, 100f, 188f, 50f);
    selectionLayer.addActor(selectedPreview);
    Image statusDivider =
        new Image(skin.newDrawable("white", new Color(0.40f, 0.29f, 0.17f, 0.75f)));
    statusDivider.setBounds(168f, 96f, 574f, 1f);
    statusDivider.setTouchable(Touchable.disabled);
    selectionLayer.addActor(statusDivider);
    addSmallDiamond(selectionLayer, 455f, 96.5f, 4f, DARK_GOLD);
    feedbackLabel = label("Choose three cards, or leave the forge.", MUTED);
    feedbackLabel.setAlignment(Align.left);
    feedbackLabel.setWrap(true);
    feedbackLabel.setBounds(170f, 49f, 570f, 42f);
    selectionLayer.addActor(feedbackLabel);

    fuseButton = new TextButton("Fuse 3 Cards", createActionStyle(true));
    fuseButton.pad(0f);
    fuseButton.getLabel().setAlignment(Align.center);
    fuseButton.getLabel().setFontScale(1.05f);
    fuseButton.setBounds(810f, 105f, 330f, 64f);
    fuseButton.setDisabled(true);
    fuseButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            attemptFusion();
          }
        });
    selectionLayer.addActor(fuseButton);
    leaveButton = new TextButton("Leave", createActionStyle(false));
    leaveButton.pad(0f);
    leaveButton.getLabel().setAlignment(Align.center);
    leaveButton.getLabel().setFontScale(1.02f);
    leaveButton.setBounds(810f, 37f, 330f, 61f);
    leaveButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            leave();
          }
        });
    selectionLayer.addActor(leaveButton);

    helpDialog =
        new ContextualHelpDialog(
            skin, EventHelpContent.FUSION_TITLE, EventHelpContent.FUSION_RULES);
    helpButton = new TextButton("?", FusionSceneAssets.buttonStyle(skin, false));
    helpButton.setName("fusion-selection-help-button");
    helpButton.setBounds(1150f, 685f, 68f, 58f);
    helpButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            helpDialog.show(stage);
          }
        });
    selectionLayer.addActor(helpButton);
  }

  private void addSmallDiamond(
      Group parent, float centerX, float centerY, float size, Color color) {
    Image diamond = new Image(skin.newDrawable("white", color));
    diamond.setBounds(centerX - size / 2f, centerY - size / 2f, size, size);
    diamond.setOrigin(size / 2f, size / 2f);
    diamond.setRotation(45f);
    diamond.setTouchable(Touchable.disabled);
    parent.addActor(diamond);
  }

  private void refreshCards() {
    selectedInstanceIds.clear();
    eligibleInstances.clear();
    cardButtons.clear();
    cardGlows.clear();
    cardsGrid.clearChildren();
    List<CardInstance> eligibleCards = flow.getEligibleCards();
    Map<String, Integer> copyNumbers = new LinkedHashMap<>();
    TextButtonStyle cardStyle = createCardStyle();
    for (int i = 0; i < eligibleCards.size(); i++) {
      CardInstance card = eligibleCards.get(i);
      int copyNumber = copyNumbers.merge(card.cardId(), 1, Integer::sum);
      TextButton cardButton = new TextButton("", cardStyle);
      cardButton.clearChildren();
      cardButton.setName(card.instanceId());
      cardButton.setProgrammaticChangeEvents(false);
      cardButton.pad(4f);
      cardButton
          .add(FusionSceneAssets.cardArt(skin, cardService, card.cardId()))
          .size(166f, 103f)
          .padBottom(3f);
      cardButton.row();
      Label name = label(cardName(card.cardId()) + (card.isUpgraded() ? "+" : ""), CREAM);
      name.setAlignment(Align.center);
      name.setWrap(true);
      cardButton.add(name).width(174f).height(23f);
      cardButton.row();
      Label copy = label("Copy " + copyNumber, MUTED);
      copy.setAlignment(Align.center);
      cardButton.add(copy).height(17f);
      cardButton.addListener(
          new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
              updateCardSelection(cardButton, card.instanceId());
            }
          });
      eligibleInstances.put(card.instanceId(), card);
      cardButtons.put(card.instanceId(), cardButton);
      Stack slot = new Stack();
      Image slotBacking =
          new Image(
              FusionSceneAssets.pixelFrame(
                  skin,
                  new Color(0.08f, 0.065f, 0.09f, 0.97f),
                  new Color(0.34f, 0.25f, 0.16f, 0.94f),
                  FusionSceneAssets.FrameKind.CARD));
      slotBacking.setTouchable(Touchable.disabled);
      slot.add(slotBacking);
      Image glow =
          new Image(
              FusionSceneAssets.pixelFrame(
                  skin, null, SELECTED, FusionSceneAssets.FrameKind.CARD_GLOW));
      glow.setVisible(false);
      glow.setTouchable(Touchable.disabled);
      slot.add(glow);
      Container<TextButton> inset = new Container<>(cardButton);
      inset.pad(3f).fill();
      slot.add(inset);
      cardGlows.put(card.instanceId(), glow);
      // Four 220px cells fit the viewport; two 184px rows leave vertical breathing room.
      cardsGrid.add(slot).size(204f, 168f).pad(8f);
      if ((i + 1) % 4 == 0) {
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
    Color cardFill = new Color(0.075f, 0.06f, 0.08f, 0.98f);
    style.up =
        FusionSceneAssets.pixelFrame(
            skin,
            cardFill,
            new Color(0.35f, 0.27f, 0.19f, 0.96f),
            FusionSceneAssets.FrameKind.CARD);
    style.over =
        FusionSceneAssets.pixelFrame(
            skin,
            cardFill,
            new Color(0.64f, 0.43f, 0.23f, 0.98f),
            FusionSceneAssets.FrameKind.CARD);
    style.down =
        FusionSceneAssets.pixelFrame(
            skin, cardFill, new Color(0.72f, 0.49f, 0.26f, 1f), FusionSceneAssets.FrameKind.CARD);
    style.checked = style.up;
    style.checkedOver = style.checked;
    style.disabled = style.up;
    style.fontColor = CREAM;
    return style;
  }

  private TextButtonStyle createActionStyle(boolean primary) {
    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));
    Color fill =
        primary ? new Color(0.19f, 0.12f, 0.09f, 0.97f) : new Color(0.075f, 0.09f, 0.13f, 0.97f);
    Color edge = primary ? new Color(0.68f, 0.44f, 0.22f, 1f) : DARK_GOLD;
    style.up = FusionSceneAssets.pixelFrame(skin, fill, edge, FusionSceneAssets.FrameKind.BUTTON);
    style.over =
        FusionSceneAssets.pixelFrame(
            skin,
            fill,
            new Color(edge).mul(1.18f, 1.18f, 1.18f, 1f),
            FusionSceneAssets.FrameKind.BUTTON);
    style.down =
        FusionSceneAssets.pixelFrame(
            skin,
            fill,
            new Color(edge).mul(0.82f, 0.82f, 0.82f, 1f),
            FusionSceneAssets.FrameKind.BUTTON);
    style.disabled =
        FusionSceneAssets.pixelFrame(
            skin,
            new Color(0.08f, 0.075f, 0.09f, 0.97f),
            new Color(0.39f, 0.30f, 0.20f, 0.95f),
            FusionSceneAssets.FrameKind.BUTTON);
    style.fontColor = CREAM;
    style.overFontColor = Color.WHITE;
    style.downFontColor = CREAM;
    style.disabledFontColor = new Color(0.53f, 0.49f, 0.44f, 1f);
    return style;
  }

  private void updateCardSelection(TextButton button, String instanceId) {
    if (completed || unavailable) {
      return;
    }
    if (button.isChecked()) {
      if (selectedInstanceIds.size() == REQUIRED_CARDS) {
        button.setChecked(false);
        cardGlows.get(instanceId).setVisible(false);
        feedbackLabel.setText("Only three card copies can be selected.");
        return;
      }
      selectedInstanceIds.add(instanceId);
    } else {
      selectedInstanceIds.remove(instanceId);
    }
    cardGlows.get(instanceId).setVisible(button.isChecked());
    updateSelectionLabel();
  }

  private void updateSelectionLabel() {
    selectionLabel.setText("Selected: " + selectedInstanceIds.size() + " / 3");
    fuseButton.setDisabled(
        completed || unavailable || selectedInstanceIds.size() != REQUIRED_CARDS);
    selectedPreview.clearChildren();
    List<String> previewIds = List.copyOf(selectedInstanceIds);
    for (int i = 0; i < REQUIRED_CARDS; i++) {
      CardInstance card = i < previewIds.size() ? eligibleInstances.get(previewIds.get(i)) : null;
      Stack preview = new Stack();
      preview.add(
          new Image(
              FusionSceneAssets.pixelFrame(
                  skin,
                  new Color(0.09f, 0.07f, 0.08f, 0.98f),
                  new Color(0.51f, 0.36f, 0.19f, 0.98f),
                  FusionSceneAssets.FrameKind.SLOT)));
      if (card != null) {
        Container<Image> art =
            new Container<>(FusionSceneAssets.cardArt(skin, cardService, card.cardId()));
        art.pad(5f).fill();
        preview.add(art);
      }
      selectedPreview.add(preview).size(56f, 47f).padRight(i < REQUIRED_CARDS - 1 ? 10f : 0f);
    }
  }

  private void attemptFusion() {
    if (completed || unavailable || selectedInstanceIds.size() != REQUIRED_CARDS) {
      return;
    }
    List<String> consumedCardIds = new ArrayList<>();
    for (String id : selectedInstanceIds) {
      consumedCardIds.add(eligibleInstances.get(id).cardId());
    }
    CardFusionResult result = flow.fuse(List.copyOf(selectedInstanceIds));
    if (result.successful()) {
      completed = true;
      String rewardId = result.rewardedCardId().orElseThrow();
      feedbackLabel.setText(
          "Fusion complete! You received the Rare card " + cardName(rewardId) + ".");
      disableSelections();
      leaveButton.setDisabled(true);
      showFusionAnimation(consumedCardIds, rewardId);
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

  private void showFusionAnimation(List<String> consumedCardIds, String rewardId) {
    float altarCoreX = FusionSceneAssets.WIDTH / 2f;
    float altarCoreY = 296f;
    selectionLayer.setVisible(false);
    sequenceLayer.setVisible(true);
    sequenceLayer.clearChildren();
    sequenceLayer.clearActions();
    sequenceLayer.getColor().a = 1f;

    Label caption = label("Fusing cards...", CREAM);
    caption.setAlignment(Align.center);
    caption.setBounds(430f, 96f, 420f, 55f);
    sequenceLayer.addActor(caption);

    float[] startX = {395f, 570f, 745f};
    float[] startY = {354f, 370f, 354f};
    float[] rotations = {12f, 0f, -12f};
    for (int i = 0; i < consumedCardIds.size(); i++) {
      Group card = makeFloatingCard();
      card.setBounds(startX[i], startY[i], 150f, 210f);
      card.setOrigin(75f, 105f);
      card.setRotation(rotations[i]);
      sequenceLayer.addActor(card);
      card.addAction(
          Actions.sequence(
              Actions.delay(0.22f + i * 0.06f),
              Actions.parallel(
                  Actions.moveBy(0f, 5f, 0.22f, Interpolation.sine), Actions.alpha(0.96f, 0.22f)),
              Actions.parallel(
                  Actions.moveTo(565f, 338f, 0.72f, Interpolation.sine),
                  Actions.scaleTo(0.24f, 0.24f, 0.72f, Interpolation.sine),
                  Actions.fadeOut(0.72f, Interpolation.sine))));
    }

    Image spark = new Image(skin.newDrawable("white", new Color(1f, 0.47f, 0.16f, 1f)));
    spark.setBounds(altarCoreX - 5f, altarCoreY - 5f, 10f, 10f);
    spark.setOrigin(5f, 5f);
    spark.setRotation(45f);
    spark.getColor().a = 0f;
    spark.addAction(
        Actions.sequence(
            Actions.delay(0.92f),
            Actions.fadeIn(0.10f),
            Actions.scaleTo(3.2f, 3.2f, 0.28f, Interpolation.pow2Out),
            Actions.fadeOut(0.25f)));
    sequenceLayer.addActor(spark);

    Image sparkCore = new Image(skin.newDrawable("white", new Color(1f, 0.82f, 0.43f, 1f)));
    sparkCore.setBounds(altarCoreX - 2.5f, altarCoreY - 2.5f, 5f, 5f);
    sparkCore.setOrigin(2.5f, 2.5f);
    sparkCore.setRotation(45f);
    sparkCore.getColor().a = 0f;
    sparkCore.addAction(
        Actions.sequence(
            Actions.delay(0.97f),
            Actions.fadeIn(0.10f),
            Actions.scaleTo(2.4f, 2.4f, 0.22f, Interpolation.pow2Out),
            Actions.fadeOut(0.22f)));
    sequenceLayer.addActor(sparkCore);

    // Soft, uneven shafts grow out of the altar core rather than eight identical hard lines.
    float[] rayAngles = {8f, 36f, 71f, 103f, 139f, 169f, 204f, 239f, 272f, 306f, 337f};
    float[] rayLengths = {540f, 425f, 575f, 490f, 555f, 445f, 515f, 430f, 580f, 470f, 535f};
    for (int i = 0; i < rayAngles.length; i++) {
      Group ray = makeFusionLightRay(rayLengths[i]);
      ray.setPosition(altarCoreX, altarCoreY - 25f);
      ray.setOrigin(0f, 25f);
      ray.setRotation(rayAngles[i]);
      ray.setScale(0.03f, 0.75f);
      ray.getColor().a = 0f;
      ray.addAction(
          Actions.sequence(
              Actions.delay(1.05f + (i % 3) * 0.025f),
              Actions.parallel(
                  Actions.fadeIn(0.18f), Actions.scaleTo(1f, 1f, 0.42f, Interpolation.pow2Out)),
              Actions.fadeOut(0.25f)));
      sequenceLayer.addActor(ray);
    }

    Image flash = new Image(skin.newDrawable("white", new Color(1f, 0.86f, 0.68f, 1f)));
    flash.setBounds(0f, 0f, FusionSceneAssets.WIDTH, FusionSceneAssets.HEIGHT);
    flash.setTouchable(Touchable.disabled);
    flash.getColor().a = 0f;
    flash.addAction(Actions.sequence(Actions.delay(1.34f), Actions.fadeIn(0.38f)));
    sequenceLayer.addActor(flash);

    sequenceLayer.addAction(
        Actions.sequence(Actions.delay(1.74f), Actions.run(() -> showResult(rewardId))));
  }

  private Group makeFusionLightRay(float length) {
    Group ray = new Group();
    ray.setName("fusion-light-ray");
    ray.setSize(length, 50f);
    for (int segment = 0; segment < 10; segment++) {
      float progress = (segment + 0.5f) / 10f;
      float x = segment * length / 10f;
      float segmentLength = length / 10f + 1f;
      float outerWidth = 5f + 41f * progress;
      Image softEdge =
          new Image(
              skin.newDrawable(
                  "white", new Color(1f, 0.62f, 0.30f, 0.11f * (1f - 0.55f * progress))));
      softEdge.setBounds(x, 25f - outerWidth / 2f, segmentLength, outerWidth);
      ray.addActor(softEdge);

      float innerWidth = 2f + 12f * progress;
      Image warmCenter =
          new Image(
              skin.newDrawable(
                  "white", new Color(1f, 0.84f, 0.58f, 0.19f * (1f - 0.6f * progress))));
      warmCenter.setBounds(x, 25f - innerWidth / 2f, segmentLength, innerWidth);
      ray.addActor(warmCenter);
    }
    ray.setTouchable(Touchable.disabled);
    return ray;
  }

  private Group makeFloatingCard() {
    Group card = new Group();
    Image reflectedLight = FusionSceneAssets.warmGlow(skin, 0.06f);
    reflectedLight.setBounds(-14f, -16f, 178f, 120f);
    card.addActor(reflectedLight);
    Image back = FusionSceneAssets.cardBack(skin);
    back.setBounds(0f, 0f, 150f, 210f);
    card.addActor(back);
    return card;
  }

  private void showResult(String rewardId) {
    sequenceLayer.clearChildren();
    Image shade = new Image(skin.newDrawable("white", new Color(0.02f, 0.02f, 0.03f, 0.48f)));
    shade.setBounds(0f, 0f, FusionSceneAssets.WIDTH, FusionSceneAssets.HEIGHT);
    shade.setTouchable(Touchable.disabled);
    sequenceLayer.addActor(shade);
    Label title = label("FUSION COMPLETE", CREAM);
    title.setAlignment(Align.center);
    title.setFontScale(1.5f);
    title.setBounds(350f, 728f, 580f, 52f);
    sequenceLayer.addActor(title);
    sequenceLayer.addActor(makeRewardCardFront(rewardId));
    addFinalMessage("A new Rare card joins your deck.", true);
    Image flash = new Image(skin.newDrawable("white", new Color(1f, 0.82f, 0.66f, 1f)));
    flash.setBounds(0f, 0f, FusionSceneAssets.WIDTH, FusionSceneAssets.HEIGHT);
    flash.setTouchable(Touchable.disabled);
    flash.addAction(Actions.sequence(Actions.fadeOut(0.45f), Actions.removeActor()));
    sequenceLayer.addActor(flash);
  }

  private Group makeRewardCardFront(String rewardId) {
    CardConfig config = cardService.getCard(rewardId).orElse(null);
    Group card = new Group();
    card.setBounds(462f, 196f, 356f, 520f);
    Image face =
        new Image(
            FusionSceneAssets.pixelFrame(
                skin,
                new Color(0.065f, 0.065f, 0.095f, 0.98f),
                new Color(0.57f, 0.49f, 0.30f, 1f),
                FusionSceneAssets.FrameKind.REWARD_CARD));
    face.setBounds(0f, 0f, 356f, 520f);
    card.addActor(face);

    Image descriptionPanel =
        new Image(
            FusionSceneAssets.pixelFrame(
                skin,
                new Color(0.035f, 0.04f, 0.065f, 1f),
                new Color(0.48f, 0.41f, 0.27f, 1f),
                FusionSceneAssets.FrameKind.REWARD_ART));
    descriptionPanel.setBounds(16f, 16f, 324f, 132f);
    card.addActor(descriptionPanel);

    Image artFrame =
        new Image(
            FusionSceneAssets.pixelFrame(
                skin,
                new Color(0.035f, 0.04f, 0.065f, 1f),
                new Color(0.48f, 0.41f, 0.27f, 1f),
                FusionSceneAssets.FrameKind.REWARD_ART));
    artFrame.setBounds(16f, 153f, 324f, 268f);
    card.addActor(artFrame);
    Image artwork = FusionSceneAssets.cardArt(skin, cardService, rewardId);
    artwork.setBounds(20f, 157f, 316f, 260f);
    card.addActor(artwork);

    Label name = label(cardName(rewardId), CREAM);
    name.setAlignment(Align.left);
    name.setFontScale(cardName(rewardId).length() > 17 ? 1.02f : 1.23f);
    name.setBounds(96f, 459f, 247f, 44f);
    card.addActor(name);
    Image headerDivider = new Image(skin.newDrawable("white", new Color(0.61f, 0.50f, 0.30f, 1f)));
    headerDivider.setBounds(96f, 452f, 240f, 1f);
    card.addActor(headerDivider);
    Image headerNode = new Image(skin.newDrawable("white", new Color(0.74f, 0.39f, 0.18f, 1f)));
    headerNode.setBounds(214f, 451f, 3f, 3f);
    card.addActor(headerNode);
    Image costBadge =
        new Image(
            FusionSceneAssets.pixelFrame(
                skin,
                new Color(0.13f, 0.10f, 0.10f, 1f),
                REWARD_GOLD,
                FusionSceneAssets.FrameKind.REWARD_ART));
    costBadge.setBounds(22f, 456f, 62f, 47f);
    card.addActor(costBadge);
    Label cost = label(config == null ? "?" : Integer.toString(config.cost), REWARD_GOLD);
    cost.setAlignment(Align.center);
    cost.setFontScale(1.35f);
    cost.setBounds(22f, 461f, 62f, 38f);
    card.addActor(cost);
    Label energy = label("ENERGY", MUTED);
    energy.setAlignment(Align.center);
    energy.setFontScale(0.80f);
    energy.setBounds(18f, 425f, 70f, 23f);
    card.addActor(energy);

    Label rarity = label(config == null ? "RARE" : config.rarity.toString(), REWARD_GOLD);
    rarity.setAlignment(Align.right);
    rarity.setFontScale(0.94f);
    rarity.setBounds(177f, 425f, 69f, 23f);
    card.addActor(rarity);
    Label separator = label("|", REWARD_GOLD);
    separator.setAlignment(Align.center);
    separator.setFontScale(0.94f);
    separator.setBounds(248f, 425f, 16f, 23f);
    card.addActor(separator);
    Label type = label(config == null ? "" : config.type.toString(), MUTED);
    type.setAlignment(Align.left);
    type.setFontScale(0.94f);
    type.setBounds(268f, 425f, 73f, 23f);
    card.addActor(type);

    String rulesText = config == null || config.description == null ? "" : config.description;
    Label description = label(rulesText, CREAM);
    description.setAlignment(Align.topLeft);
    description.setWrap(true);
    description.setFontScale(
        rulesText.length() > 80 ? 0.89f : rulesText.length() > 65 ? 0.98f : 1.08f);
    description.setBounds(29f, 37f, 298f, 94f);
    card.addActor(description);
    return card;
  }

  private void showLeave() {
    selectionLayer.setVisible(false);
    sequenceLayer.setVisible(true);
    sequenceLayer.clearChildren();
    Label title = label("CARD FUSION", CREAM);
    title.setFontScale(1.45f);
    title.setBounds(95f, 704f, 600f, 55f);
    sequenceLayer.addActor(title);
    addFinalMessage("You leave the forge without fusing any cards.");
  }

  private void addFinalMessage(String message) {
    addFinalMessage(message, false);
  }

  private void addFinalMessage(String message, boolean highlightRare) {
    float messageY = highlightRare ? 122f : 134f;
    Label shadow = label(message, new Color(0.02f, 0.02f, 0.04f, 0.9f));
    shadow.setAlignment(Align.center);
    shadow.setWrap(true);
    shadow.setBounds(222f, messageY - 2f, 840f, 58f);
    sequenceLayer.addActor(shadow);
    if (highlightRare) {
      String[] parts = {"A new ", "Rare", " card joins your deck."};
      float[] widths = new float[parts.length];
      float totalWidth = 0f;
      for (int i = 0; i < parts.length; i++) {
        widths[i] = new GlyphLayout(skin.get(LabelStyle.class).font, parts[i]).width;
        totalWidth += widths[i];
      }
      float x = (FusionSceneAssets.WIDTH - totalWidth) / 2f;
      for (int i = 0; i < parts.length; i++) {
        Label part = label(parts[i], i == 1 ? REWARD_GOLD : CREAM);
        part.setBounds(x, messageY, widths[i], 58f);
        sequenceLayer.addActor(part);
        x += widths[i];
      }
    } else {
      Label result = label(message, CREAM);
      result.setAlignment(Align.center);
      result.setWrap(true);
      result.setBounds(220f, messageY, 840f, 58f);
      sequenceLayer.addActor(result);
    }
    continueButton = FusionSceneAssets.button("Continue", skin, true);
    continueButton.setBounds(501f, 34f, 278f, 72f);
    continueButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            exitRequested = true;
          }
        });
    sequenceLayer.addActor(continueButton);
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
    showLeave();
  }

  private Label label(String text, Color color) {
    LabelStyle style = new LabelStyle(skin.get(LabelStyle.class));
    style.fontColor = new Color(color);
    return new Label(text, style);
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

  boolean isExitRequested() {
    return exitRequested;
  }

  void dispose() {
    if (helpDialog != null) {
      helpDialog.remove();
    }
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

  TextButton getHelpButton() {
    return helpButton;
  }

  ContextualHelpDialog getHelpDialog() {
    return helpDialog;
  }

  TextButton getContinueButton() {
    return continueButton;
  }

  String getFeedbackText() {
    return feedbackLabel.getText().toString();
  }

  List<String> getSelectedInstanceIds() {
    return List.copyOf(selectedInstanceIds);
  }

  boolean isCardGlowVisible(String instanceId) {
    Image glow = cardGlows.get(instanceId);
    return glow != null && glow.isVisible();
  }

  Label getInstructionLabel() {
    return instructionLabel;
  }

  Table getCardsGrid() {
    return cardsGrid;
  }

  Table getSelectedPreview() {
    return selectedPreview;
  }

  Image getStatusPanel() {
    return statusPanel;
  }

  Group getSequenceLayer() {
    return sequenceLayer;
  }

  Label getFeedbackLabel() {
    return feedbackLabel;
  }

  Label getSelectionLabel() {
    return selectionLabel;
  }
}
