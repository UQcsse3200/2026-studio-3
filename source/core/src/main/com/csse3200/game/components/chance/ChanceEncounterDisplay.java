package com.csse3200.game.components.chance;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.csse3200.game.chance.CardFusionEncounterBehaviour;
import com.csse3200.game.chance.ChanceChoice;
import com.csse3200.game.chance.ChanceEncounter;
import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.chance.DiceEncounterBehaviour;
import com.csse3200.game.chance.DiceRoll;
import com.csse3200.game.components.shop.ShopDisplay;
import com.csse3200.game.encounters.integration.CardFusionEncounterFlow;
import com.csse3200.game.encounters.integration.ChanceEncounterSession;
import com.csse3200.game.encounters.integration.ChanceResolution;
import com.csse3200.game.maps.EncounterCallback;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
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
  public static final String DICE_GAME_BACKGROUND_TEXTURE = "images/chance/dice_game_scene_v1.png";
  public static final String ABANDONED_MINE_BACKGROUND_TEXTURE =
      "images/chance/abandoned_mine_scene_v1.png";
  public static final String FUSION_BACKGROUND_TEXTURE = "images/chance/card_fusion_forge_v1.png";
  public static final String FUSION_CARD_BACK_TEXTURE =
      "images/chance/card_fusion_back_balanced.png";

  private static final float Z_INDEX = 3f;
  private static final float PANEL_WIDTH = 1080f;
  private static final float CONTENT_WIDTH = 980f;
  private static final float SCENE_WIDTH = 1280f;
  private static final float SCENE_HEIGHT = 800f;

  private static final Color BACKDROP_COLOUR = new Color(0.02f, 0.015f, 0.025f, 1f);
  private static final Color PANEL_COLOUR = new Color(0.11f, 0.075f, 0.08f, 0.98f);
  private static final Color RESULT_COLOUR = new Color(0.07f, 0.045f, 0.05f, 1f);
  private static final Color GOLD_COLOUR = new Color(0.94f, 0.72f, 0.3f, 1f);
  private static final Color BODY_COLOUR = new Color(0.9f, 0.84f, 0.73f, 1f);
  private static final Color MUTED_COLOUR = new Color(0.66f, 0.59f, 0.53f, 1f);

  private static final String WHITE = "white";
  private static final String LARGE = "large";
  private static final String SMALL = "small";
  private static final String DEFAULT = "default";
  private static final String BUTTON = "button";

  private final ChanceEncounter encounter;
  private final ChanceEncounterSession encounterSession;
  private final CardFusionEncounterFlow cardFusionFlow;
  private final boolean scenicDiceGame;
  private final boolean scenicAbandonedMine;
  private final boolean scenicCardFusion;
  private final EncounterCallback completionCallback;
  private final Integer nodeId;
  private final List<TextButton> choiceButtons = new ArrayList<>();

  private Table rootTable;
  private Table choicesTable;
  private Label promptLabel;
  private Label resultLabel;
  private TextButton continueButton;
  private TextButtonStyle choiceStyle;
  private TextButtonStyle selectedChoiceStyle;
  private DiceRollDisplay diceRollDisplay;
  private CardFusionSelectionView cardFusionView;
  private ContextualHelpDialog helpDialog;
  private TextButton helpButton;
  private int lastDisplayedRollSequence;
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
    this(encounter, completionCallback, nodeId, null, null);
  }

  /**
   * Creates a display backed by a fully integrated Chance Encounter session.
   *
   * @param encounterSession session that applies outcomes and reports completion to the map
   */
  public ChanceEncounterDisplay(ChanceEncounterSession encounterSession) {
    this(encounterSession, null);
  }

  /** Creates a display that can hand Card Fusion choices to the run-scoped Fusion flow. */
  public ChanceEncounterDisplay(
      ChanceEncounterSession encounterSession, CardFusionEncounterFlow cardFusionFlow) {
    this(
        Objects.requireNonNull(encounterSession, "encounterSession cannot be null").getEncounter(),
        null,
        encounterSession.getNodeId(),
        encounterSession,
        cardFusionFlow);
  }

  private ChanceEncounterDisplay(
      ChanceEncounter encounter,
      EncounterCallback completionCallback,
      Integer nodeId,
      ChanceEncounterSession encounterSession,
      CardFusionEncounterFlow cardFusionFlow) {
    this.encounter = Objects.requireNonNull(encounter, "encounter cannot be null");
    this.encounterSession = encounterSession;
    this.cardFusionFlow = cardFusionFlow;
    this.scenicDiceGame =
        encounterSession != null && DiceEncounterBehaviour.ENCOUNTER_ID.equals(encounter.getId());
    this.scenicAbandonedMine = "abandoned-mine".equals(encounter.getId());
    this.scenicCardFusion =
        cardFusionFlow != null
            && CardFusionEncounterBehaviour.ENCOUNTER_ID.equals(encounter.getId());
    this.completionCallback = completionCallback;
    this.nodeId = Objects.requireNonNull(nodeId, "nodeId cannot be null");
  }

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    if (scenicDiceGame) {
      addDiceGameActors();
      return;
    }
    if (scenicAbandonedMine) {
      addAbandonedMineActors();
      return;
    }
    if (scenicCardFusion) {
      addFusionIntroActors();
      return;
    }

    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.setBackground(skin.newDrawable(WHITE, BACKDROP_COLOUR));
    rootTable.setTouchable(Touchable.enabled);
    rootTable.center();
    rootTable.getColor().a = 0f;

    Table encounterTable = new Table();
    encounterTable.setBackground(skin.newDrawable("window-w", PANEL_COLOUR));
    encounterTable.pad(30f, 40f, 34f, 40f);
    encounterTable.defaults().spaceBottom(14f);

    Label eyebrowLabel = new Label("CHANCE ENCOUNTER", createLabelStyle(SMALL, GOLD_COLOUR));
    Label titleLabel =
        new Label(formatTitle(encounter.getId()), createLabelStyle(LARGE, BODY_COLOUR));
    Label descriptionLabel =
        new Label(encounter.getDescription(), createLabelStyle(DEFAULT, BODY_COLOUR));
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

    promptLabel = new Label("CHOOSE YOUR RESPONSE", createLabelStyle(SMALL, MUTED_COLOUR));
    promptLabel.setFontScale(1.25f);
    encounterTable.add(promptLabel).left().width(CONTENT_WIDTH).padBottom(2f);
    encounterTable.row();

    choiceStyle = createChoiceStyle();
    selectedChoiceStyle = createSelectedChoiceStyle(choiceStyle);
    choicesTable = new Table();
    encounterTable.add(choicesTable).left().width(CONTENT_WIDTH);
    encounterTable.row();
    refreshChoices();

    Table resultTable = new Table();
    resultTable.setBackground(skin.newDrawable(WHITE, RESULT_COLOUR));
    resultTable.pad(14f, 18f, 14f, 18f);
    resultLabel =
        new Label(
            "Your decision will determine the outcome.", createLabelStyle(DEFAULT, MUTED_COLOUR));
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

  /** A dedicated forge scene; the Event choices and session still own all gameplay effects. */
  private void addFusionIntroActors() {
    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.setBackground(skin.newDrawable(WHITE, BACKDROP_COLOUR));
    rootTable.setTouchable(Touchable.enabled);
    rootTable.getColor().a = 0f;

    Group scene = new Group();
    scene.setSize(FusionSceneAssets.WIDTH, FusionSceneAssets.HEIGHT);
    scene.addActor(FusionSceneAssets.background(skin));
    addFusionAltarLighting(scene);
    addFusionAltarEmbers(scene);

    Label eyebrow = new Label("CHANCE ENCOUNTER", createLabelStyle(SMALL, GOLD_COLOUR));
    eyebrow.setBounds(68f, 721f, 520f, 30f);
    scene.addActor(eyebrow);
    Label title = new Label("CARD FUSION", createLabelStyle(LARGE, BODY_COLOUR));
    title.setFontScale(1.35f);
    title.setBounds(68f, 657f, 660f, 57f);
    scene.addActor(title);

    addFusionPreviewCard(scene, 395f, 354f, 12f, 0f);
    addFusionPreviewCard(scene, 570f, 370f, 0f, 0.55f);
    addFusionPreviewCard(scene, 745f, 354f, -12f, 1.1f);

    Label descriptionShadow =
        new Label(
            encounter.getDescription(),
            createLabelStyle(DEFAULT, new Color(0.02f, 0.02f, 0.04f, 0.85f)));
    descriptionShadow.setAlignment(com.badlogic.gdx.utils.Align.center);
    descriptionShadow.setWrap(true);
    descriptionShadow.setBounds(182f, 157f, 920f, 58f);
    scene.addActor(descriptionShadow);
    Label description =
        new Label(encounter.getDescription(), createLabelStyle(DEFAULT, BODY_COLOUR));
    description.setAlignment(com.badlogic.gdx.utils.Align.center);
    description.setWrap(true);
    description.setBounds(180f, 159f, 920f, 58f);
    scene.addActor(description);

    promptLabel = new Label("CHOOSE YOUR RESPONSE", createLabelStyle(SMALL, MUTED_COLOUR));
    promptLabel.setVisible(false);
    scene.addActor(promptLabel);
    choiceStyle = FusionSceneAssets.buttonStyle(skin, true);
    selectedChoiceStyle = new TextButtonStyle(choiceStyle);
    choicesTable = new Table();
    choicesTable.setBounds(137f, 54f, 1006f, 98f);
    scene.addActor(choicesTable);
    refreshChoices();

    Table resultPanel = new Table();
    resultPanel.setBackground(
        FusionSceneAssets.plaque(skin, new Color(0.53f, 0.50f, 0.54f, 0.97f)));
    resultPanel.pad(14f, 25f, 14f, 25f);
    resultLabel = new Label("", createLabelStyle(DEFAULT, BODY_COLOUR));
    resultLabel.setWrap(true);
    resultPanel.add(resultLabel).grow();
    resultPanel.setBounds(200f, 157f, 880f, 112f);
    resultPanel.setVisible(false);
    scene.addActor(resultPanel);

    continueButton = FusionSceneAssets.button("Continue", skin, true);
    continueButton.setBounds(492f, 48f, 296f, 76f);
    continueButton.setVisible(false);
    continueButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            completeEncounter();
          }
        });
    scene.addActor(continueButton);

    helpDialog =
        new ContextualHelpDialog(
            skin, EventHelpContent.FUSION_TITLE, EventHelpContent.FUSION_RULES);
    helpButton = new TextButton("?", FusionSceneAssets.buttonStyle(skin, false));
    helpButton.setName("fusion-intro-help-button");
    helpButton.setBounds(1150f, 685f, 68f, 58f);
    helpButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            helpDialog.show(stage);
          }
        });
    scene.addActor(helpButton);

    rootTable.add(scene).size(FusionSceneAssets.WIDTH, FusionSceneAssets.HEIGHT);
    stage.addActor(rootTable);
    rootTable.addAction(Actions.fadeIn(0.25f));
  }

  private void addFusionAltarLighting(Group scene) {
    Image distantWarmth = FusionSceneAssets.warmGlow(skin, 0.028f);
    distantWarmth.setBounds(420f, 365f, 440f, 230f);
    scene.addActor(distantWarmth);

    Image risingHeat = FusionSceneAssets.warmGlow(skin, 0.24f);
    risingHeat.setBounds(360f, 340f, 560f, 145f);
    scene.addActor(risingHeat);

    Image lightBridge = FusionSceneAssets.warmGlow(skin, 0.20f);
    lightBridge.setBounds(440f, 342f, 400f, 155f);
    scene.addActor(lightBridge);

    Image altarEdge = FusionSceneAssets.warmGlow(skin, 0.44f);
    altarEdge.setBounds(315f, 315f, 650f, 105f);
    scene.addActor(altarEdge);

    Image rimCore = FusionSceneAssets.warmGlow(skin, 0.42f);
    rimCore.setBounds(486f, 335f, 308f, 62f);
    scene.addActor(rimCore);

    Image altarSymbol = FusionSceneAssets.warmGlow(skin, 0.43f);
    altarSymbol.setBounds(568f, 239f, 144f, 125f);
    scene.addActor(altarSymbol);

    Image symbolCore = FusionSceneAssets.warmGlow(skin, 0.36f);
    symbolCore.setBounds(602f, 267f, 76f, 75f);
    scene.addActor(symbolCore);

    Image leftStone = FusionSceneAssets.warmGlow(skin, 0.06f);
    leftStone.setBounds(295f, 251f, 245f, 140f);
    scene.addActor(leftStone);
    Image rightStone = FusionSceneAssets.warmGlow(skin, 0.06f);
    rightStone.setBounds(740f, 251f, 245f, 140f);
    scene.addActor(rightStone);

    float[][] rimMarks = {
      {456f, 352f, 26f},
      {510f, 352f, 30f},
      {568f, 353f, 36f},
      {620f, 353f, 40f},
      {682f, 353f, 30f},
      {748f, 352f, 27f},
      {804f, 352f, 20f}
    };
    for (float[] mark : rimMarks) {
      addFusionHeatPixel(scene, mark[0], mark[1], mark[2], 2f, 0.52f);
    }

    float[][] cracks = {
      {564f, 300f, 8f, 2f}, {572f, 296f, 2f, 5f},
      {704f, 301f, 9f, 2f}, {710f, 296f, 2f, 6f},
      {611f, 255f, 7f, 2f}, {665f, 256f, 8f, 2f}
    };
    for (float[] crack : cracks) {
      addFusionHeatPixel(scene, crack[0], crack[1], crack[2], crack[3], 0.44f);
    }
  }

  private void addFusionHeatPixel(
      Group scene, float x, float y, float width, float height, float alpha) {
    Image mark = new Image(skin.newDrawable(WHITE, new Color(1f, 0.56f, 0.22f, alpha)));
    mark.setBounds(x, y, width, height);
    mark.setTouchable(Touchable.disabled);
    scene.addActor(mark);
  }

  private void addFusionAltarEmbers(Group scene) {
    float[][] positions = {{525f, 354f}, {562f, 382f}, {642f, 359f}, {704f, 382f}, {748f, 354f}};
    for (int i = 0; i < positions.length; i++) {
      Image ember = new Image(skin.newDrawable(WHITE, new Color(1f, 0.55f, 0.24f, 0.72f)));
      ember.setBounds(positions[i][0], positions[i][1], 2f, 2f);
      ember.setTouchable(Touchable.disabled);
      ember.getColor().a = 0f;
      ember.addAction(
          Actions.forever(
              Actions.sequence(
                  Actions.delay(i * 0.23f),
                  Actions.parallel(
                      Actions.alpha(0.55f, 0.8f, Interpolation.sine),
                      Actions.moveBy(0f, 4f, 0.8f, Interpolation.sine)),
                  Actions.parallel(
                      Actions.alpha(0f, 1.1f, Interpolation.sine),
                      Actions.moveBy(i % 2 == 0 ? 1f : -1f, 7f, 1.1f, Interpolation.sine)),
                  Actions.moveTo(positions[i][0], positions[i][1]))));
      scene.addActor(ember);
    }
  }

  private void addFusionPreviewCard(
      Group scene, float x, float y, float rotation, float floatDelay) {
    Group card = new Group();
    card.setBounds(x, y, 150f, 210f);
    card.setOrigin(75f, 105f);
    card.setRotation(rotation);
    Image reflectedLight = FusionSceneAssets.warmGlow(skin, 0.06f);
    reflectedLight.setBounds(-14f, -16f, 178f, 120f);
    card.addActor(reflectedLight);
    Image back = FusionSceneAssets.cardBack(skin);
    back.setBounds(0f, 0f, 150f, 210f);
    card.addActor(back);
    Image coreLight = FusionSceneAssets.warmGlow(skin, 0.07f);
    coreLight.setBounds(55f, 75f, 40f, 60f);
    card.addActor(coreLight);
    addFusionCardSparkles(card);
    scene.addActor(card);
    card.addAction(
        Actions.sequence(
            Actions.delay(floatDelay),
            Actions.forever(
                Actions.sequence(
                    Actions.moveBy(0f, 3f, 1.65f, Interpolation.sine),
                    Actions.moveBy(0f, -3f, 1.65f, Interpolation.sine)))));
  }

  private void addFusionCardSparkles(Group card) {
    float[][] positions = {{-2f, 36f}, {145f, 58f}, {119f, 9f}};
    for (int i = 0; i < positions.length; i++) {
      Group spark = new Group();
      spark.setBounds(positions[i][0], positions[i][1], 6f, 6f);
      spark.setTouchable(Touchable.disabled);
      Color warm = new Color(0.96f, 0.69f, 0.38f, 0.65f);
      Image vertical = new Image(skin.newDrawable(WHITE, warm));
      vertical.setBounds(2f, 0f, 2f, 6f);
      spark.addActor(vertical);
      Image horizontal = new Image(skin.newDrawable(WHITE, warm));
      horizontal.setBounds(0f, 2f, 6f, 2f);
      spark.addActor(horizontal);
      Image center = new Image(skin.newDrawable(WHITE, new Color(1f, 0.91f, 0.68f, 0.8f)));
      center.setBounds(2f, 2f, 2f, 2f);
      spark.addActor(center);
      spark.getColor().a = 0.1f;
      spark.addAction(
          Actions.forever(
              Actions.sequence(
                  Actions.delay(i * 0.16f),
                  Actions.parallel(
                      Actions.alpha(0.55f, 0.8f, Interpolation.sine),
                      Actions.moveBy(0f, 2f, 0.8f, Interpolation.sine)),
                  Actions.parallel(
                      Actions.alpha(0.1f, 1.1f, Interpolation.sine),
                      Actions.moveBy(0f, 5f, 1.1f, Interpolation.sine)),
                  Actions.moveTo(positions[i][0], positions[i][1]))));
      card.addActor(spark);
    }
  }

  private void addDiceGameActors() {
    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.setBackground(skin.newDrawable(WHITE, BACKDROP_COLOUR));
    rootTable.setTouchable(Touchable.enabled);
    rootTable.getColor().a = 0f;

    Group scene = new Group();
    scene.setSize(SCENE_WIDTH, SCENE_HEIGHT);
    ResourceService resources = ServiceLocator.getResourceService();
    if (resources != null && resources.containsAsset(DICE_GAME_BACKGROUND_TEXTURE, Texture.class)) {
      Texture texture = resources.getAsset(DICE_GAME_BACKGROUND_TEXTURE, Texture.class);
      texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      Image background = new Image(new TextureRegionDrawable(new TextureRegion(texture)));
      background.setBounds(0f, 0f, SCENE_WIDTH, SCENE_HEIGHT);
      background.setTouchable(Touchable.disabled);
      scene.addActor(background);
    }

    Label eyebrowLabel = new Label("CHANCE ENCOUNTER", createLabelStyle(SMALL, GOLD_COLOUR));
    eyebrowLabel.setFontScale(1.2f);
    eyebrowLabel.setBounds(165f, 682f, 470f, 32f);
    scene.addActor(eyebrowLabel);

    Label titleLabel =
        new Label(formatTitle(encounter.getId()), createLabelStyle(LARGE, BODY_COLOUR));
    titleLabel.setFontScale(1.45f);
    titleLabel.setBounds(165f, 622f, 470f, 56f);
    scene.addActor(titleLabel);

    Label descriptionLabel =
        new Label(encounter.getDescription(), createLabelStyle(DEFAULT, BODY_COLOUR));
    descriptionLabel.setFontScale(1.13f);
    descriptionLabel.setWrap(true);
    descriptionLabel.setBounds(165f, 540f, 470f, 75f);
    scene.addActor(descriptionLabel);

    diceRollDisplay = new DiceRollDisplay(skin, createLabelStyle(DEFAULT, GOLD_COLOUR), true);
    diceRollDisplay.getTable().setBounds(168f, 365f, 505f, 178f);
    scene.addActor(diceRollDisplay.getTable());

    promptLabel = new Label("CHOOSE YOUR RESPONSE", createLabelStyle(SMALL, BODY_COLOUR));
    promptLabel.setFontScale(1.04f);
    Table promptPlaque = new Table();
    promptPlaque.setBackground(createDicePlaqueDrawable(new Color(0.62f, 0.45f, 0.41f, 0.94f)));
    promptPlaque.pad(6f, 22f, 6f, 22f);
    promptPlaque.add(promptLabel).left().grow();
    promptPlaque.setBounds(125f, 326f, 550f, 50f);
    scene.addActor(promptPlaque);

    choiceStyle = createDiceChoiceStyle();
    selectedChoiceStyle = new TextButtonStyle(choiceStyle);
    selectedChoiceStyle.disabled = createDicePlaqueDrawable(new Color(0.78f, 0.49f, 0.42f, 1f));
    selectedChoiceStyle.disabledFontColor = Color.WHITE;
    choicesTable = new Table();
    choicesTable.setBounds(170f, 140f, 940f, 80f);
    scene.addActor(choicesTable);
    refreshChoices();

    Table resultTable = new Table();
    resultTable.setBackground(createDicePlaqueDrawable(new Color(0.68f, 0.48f, 0.47f, 0.93f)));
    resultTable.pad(8f, 25f, 8f, 25f);
    resultLabel =
        new Label(
            "Your decision will determine the outcome.", createLabelStyle(DEFAULT, BODY_COLOUR));
    resultLabel.setFontScale(1.08f);
    resultLabel.setWrap(true);
    resultTable.add(resultLabel).left().grow();
    resultTable.setBounds(245f, 38f, 790f, 70f);
    scene.addActor(resultTable);

    continueButton = new TextButton("Continue", createDiceChoiceStyle());
    continueButton.getLabel().setFontScale(1.1f);
    continueButton.setBounds(1038f, 42f, 205f, 65f);
    continueButton.setVisible(false);
    continueButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            completeEncounter();
          }
        });
    scene.addActor(continueButton);

    helpDialog =
        new ContextualHelpDialog(skin, EventHelpContent.DICE_TITLE, EventHelpContent.DICE_RULES);
    helpButton = new TextButton("?", skin);
    helpButton.setName("dice-help-button");
    helpButton.setBounds(1150f, 685f, 68f, 58f);
    helpButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            helpDialog.show(stage);
          }
        });
    scene.addActor(helpButton);

    rootTable.add(scene).size(SCENE_WIDTH, SCENE_HEIGHT);
    stage.addActor(rootTable);
    rootTable.addAction(Actions.fadeIn(0.25f));
  }

  /** The mine scene changes only presentation; choices still use the shared encounter lifecycle. */
  private void addAbandonedMineActors() {
    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.setBackground(skin.newDrawable(WHITE, BACKDROP_COLOUR));
    rootTable.setTouchable(Touchable.enabled);
    rootTable.getColor().a = 0f;

    Group scene = new Group();
    scene.setSize(SCENE_WIDTH, SCENE_HEIGHT);
    ResourceService resources = ServiceLocator.getResourceService();
    if (resources != null
        && resources.containsAsset(ABANDONED_MINE_BACKGROUND_TEXTURE, Texture.class)) {
      Texture texture = resources.getAsset(ABANDONED_MINE_BACKGROUND_TEXTURE, Texture.class);
      texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      Image background = new Image(new TextureRegionDrawable(new TextureRegion(texture)));
      background.setBounds(0f, 0f, SCENE_WIDTH, SCENE_HEIGHT);
      background.setTouchable(Touchable.disabled);
      scene.addActor(background);
    }

    Label eyebrowLabel = new Label("CHANCE ENCOUNTER", createLabelStyle(SMALL, GOLD_COLOUR));
    eyebrowLabel.setFontScale(1.16f);
    eyebrowLabel.setBounds(60f, 710f, 530f, 34f);
    scene.addActor(eyebrowLabel);

    Label titleLabel =
        new Label(formatTitle(encounter.getId()), createLabelStyle(LARGE, BODY_COLOUR));
    titleLabel.setFontScale(1.55f);
    titleLabel.setBounds(60f, 650f, 560f, 56f);
    scene.addActor(titleLabel);

    Image divider = new Image(skin.newDrawable(WHITE, new Color(0.48f, 0.30f, 0.20f, 0.85f)));
    divider.setBounds(60f, 638f, 525f, 2f);
    divider.setTouchable(Touchable.disabled);
    scene.addActor(divider);

    Label descriptionLabel =
        new Label(encounter.getDescription(), createLabelStyle(DEFAULT, BODY_COLOUR));
    descriptionLabel.setFontScale(1.13f);
    descriptionLabel.setWrap(true);
    descriptionLabel.setBounds(60f, 515f, 540f, 108f);
    scene.addActor(descriptionLabel);

    // The original prompt remains available to refreshChoices, but the scene and buttons
    // provide the visual invitation without another opaque panel over the mine cart.
    promptLabel = new Label("CHOOSE YOUR RESPONSE", createLabelStyle(SMALL, MUTED_COLOUR));
    promptLabel.setVisible(false);
    scene.addActor(promptLabel);

    choiceStyle = createMineChoiceStyle(new Color(0.83f, 0.65f, 0.48f, 1f));
    selectedChoiceStyle = new TextButtonStyle(choiceStyle);
    choicesTable = new Table();
    choicesTable.setBounds(55f, 23f, 1170f, 105f);
    scene.addActor(choicesTable);
    refreshChoices();

    Table resultTable = new Table();
    resultTable.setBackground(createDicePlaqueDrawable(new Color(0.55f, 0.45f, 0.48f, 0.96f)));
    resultTable.pad(12f, 24f, 12f, 24f);
    resultLabel =
        new Label(
            "Your decision will determine the outcome.", createLabelStyle(DEFAULT, BODY_COLOUR));
    resultLabel.setFontScale(1.06f);
    resultLabel.setWrap(true);
    resultTable.add(resultLabel).left().grow();
    resultTable.setBounds(170f, 138f, 940f, 100f);
    resultTable.setVisible(false);
    scene.addActor(resultTable);

    continueButton =
        new TextButton("Continue", createMineChoiceStyle(new Color(0.64f, 0.55f, 0.55f, 1f)));
    continueButton.getLabel().setFontScale(1.12f);
    continueButton.setBounds(490f, 25f, 300f, 90f);
    continueButton.setVisible(false);
    continueButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            completeEncounter();
          }
        });
    scene.addActor(continueButton);

    rootTable.add(scene).size(SCENE_WIDTH, SCENE_HEIGHT);
    stage.addActor(rootTable);
    rootTable.addAction(Actions.fadeIn(0.25f));
  }

  private TextButtonStyle createMineChoiceStyle(Color tint) {
    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));
    style.up = createDicePlaqueDrawable(tint);
    style.over = createDicePlaqueDrawable(new Color(tint).mul(1.13f, 1.13f, 1.13f, 1f));
    style.down = createDicePlaqueDrawable(new Color(tint).mul(0.82f, 0.82f, 0.82f, 1f));
    style.disabled = createDicePlaqueDrawable(new Color(0.35f, 0.32f, 0.35f, 0.94f));
    style.fontColor = BODY_COLOUR;
    style.overFontColor = Color.WHITE;
    style.downFontColor = BODY_COLOUR;
    style.disabledFontColor = MUTED_COLOUR;
    return style;
  }

  private TextButtonStyle createDiceChoiceStyle() {
    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));
    style.up = createDicePlaqueDrawable(new Color(0.72f, 0.42f, 0.43f, 0.96f));
    style.over = createDicePlaqueDrawable(new Color(0.85f, 0.5f, 0.48f, 1f));
    style.down = createDicePlaqueDrawable(new Color(0.55f, 0.31f, 0.35f, 1f));
    style.disabled = createDicePlaqueDrawable(new Color(0.42f, 0.34f, 0.35f, 0.92f));
    style.fontColor = BODY_COLOUR;
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.WHITE;
    style.disabledFontColor = MUTED_COLOUR;
    return style;
  }

  private Drawable createDicePlaqueDrawable(Color tint) {
    ResourceService resources = ServiceLocator.getResourceService();
    if (resources == null
        || !resources.containsAsset(ShopDisplay.PLAQUE_FRAME_TEXTURE, Texture.class)) {
      return skin.newDrawable(BUTTON, tint);
    }
    Texture texture = resources.getAsset(ShopDisplay.PLAQUE_FRAME_TEXTURE, Texture.class);
    TextureRegion region = new TextureRegion(texture, 72, 152, 2031, 409);
    NinePatch patch = new NinePatch(region, 105, 105, 78, 78);
    patch.scale(0.13f, 0.13f);
    patch.setColor(tint);
    return new NinePatchDrawable(patch);
  }

  private Table createDivider() {
    Table divider = new Table();
    divider.setBackground(skin.newDrawable(WHITE, GOLD_COLOUR));
    return divider;
  }

  private LabelStyle createLabelStyle(String baseStyle, Color colour) {
    LabelStyle labelStyle = new LabelStyle(skin.get(baseStyle, LabelStyle.class));
    labelStyle.fontColor = colour;
    return labelStyle;
  }

  private TextButtonStyle createChoiceStyle() {
    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));
    style.up = skin.newDrawable(BUTTON, new Color(0.27f, 0.17f, 0.14f, 1f));
    style.over = skin.newDrawable(BUTTON, new Color(0.48f, 0.29f, 0.16f, 1f));
    style.down = skin.newDrawable("button-pressed", new Color(0.62f, 0.4f, 0.2f, 1f));
    style.disabled = skin.newDrawable(BUTTON, new Color(0.12f, 0.1f, 0.11f, 1f));
    style.fontColor = BODY_COLOUR;
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.WHITE;
    style.disabledFontColor = MUTED_COLOUR;
    return style;
  }

  private TextButtonStyle createSelectedChoiceStyle(TextButtonStyle choiceStyle) {
    TextButtonStyle style = new TextButtonStyle(choiceStyle);
    style.disabled = skin.newDrawable(BUTTON, new Color(0.53f, 0.34f, 0.16f, 1f));
    style.disabledFontColor = Color.WHITE;
    return style;
  }

  private TextButtonStyle createContinueStyle() {
    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));
    style.up = skin.newDrawable(BUTTON, new Color(0.52f, 0.3f, 0.11f, 1f));
    style.over = skin.newDrawable(BUTTON, new Color(0.75f, 0.48f, 0.18f, 1f));
    style.down = skin.newDrawable("button-pressed", new Color(0.4f, 0.22f, 0.08f, 1f));
    style.fontColor = Color.WHITE;
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.WHITE;
    return style;
  }

  private void refreshChoices() {
    choicesTable.clearChildren();
    choiceButtons.clear();
    promptLabel.setText(
        encounterSession == null ? "CHOOSE YOUR RESPONSE" : encounterSession.getChoicePrompt());

    List<ChanceChoice> availableChoices =
        encounterSession == null ? encounter.getChoices() : encounterSession.getAvailableChoices();
    int choiceNumber = 1;
    for (ChanceChoice choice : availableChoices) {
      addChoiceButton(choice, choiceNumber);
      choiceNumber++;
    }
    choicesTable.invalidateHierarchy();
  }

  List<TextButton> getChoiceButtons() {
    return List.copyOf(choiceButtons);
  }

  String getResultText() {
    return resultLabel.getText().toString();
  }

  TextButton getContinueButton() {
    return continueButton;
  }

  DiceRollDisplay getDiceRollDisplay() {
    return diceRollDisplay;
  }

  TextButton getHelpButton() {
    return helpButton;
  }

  ContextualHelpDialog getHelpDialog() {
    return helpDialog;
  }

  CardFusionSelectionView getCardFusionView() {
    return cardFusionView;
  }

  /** True once the delegated success/leave scene has been acknowledged by the player. */
  public boolean isCardFusionPresentationComplete() {
    return cardFusionView == null || cardFusionView.isExitRequested();
  }

  private void addChoiceButton(ChanceChoice choice, int choiceNumber) {
    String buttonText = String.format("%d.  %s", choiceNumber, choice.getDescription());
    TextButton choiceButton = new TextButton(buttonText, choiceStyle);
    choiceButton
        .getLabel()
        .setFontScale(scenicDiceGame || scenicAbandonedMine || scenicCardFusion ? 1.02f : 1.3f);
    choiceButton.getLabel().setWrap(true);
    choiceButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            resolveChoice(choice, choiceButton, buttonText);
          }
        });

    choiceButtons.add(choiceButton);
    if (scenicDiceGame) {
      choicesTable.add(choiceButton).width(455f).height(76f).padRight(30f);
    } else if (scenicAbandonedMine) {
      if (choiceNumber == 2) {
        choiceButton.setStyle(createMineChoiceStyle(new Color(0.48f, 0.50f, 0.57f, 1f)));
      }
      choicesTable.add(choiceButton).width(560f).height(100f).padRight(25f);
    } else if (scenicCardFusion) {
      if (choiceNumber == 2) {
        choiceButton.setStyle(FusionSceneAssets.buttonStyle(skin, false));
      }
      choicesTable.add(choiceButton).width(490f).height(86f).padRight(20f);
    } else {
      choicesTable.add(choiceButton).left().width(CONTENT_WIDTH).minHeight(80f).padBottom(14f);
      choicesTable.row();
    }
  }

  private void resolveChoice(
      ChanceChoice choice, TextButton selectedButton, String originalButtonText) {
    if (choiceResolved || (diceRollDisplay != null && diceRollDisplay.isRolling())) {
      return;
    }

    if (encounterSession == null) {
      showOutcome(encounter.resolveChoice(choice.getId()), selectedButton, originalButtonText);
      return;
    }

    ChanceResolution resolution = encounterSession.resolveChoice(choice.getId());
    if (diceRollDisplay != null) {
      DiceRoll roll = encounterSession.getLastDiceRoll().orElse(null);
      if (roll != null && roll.sequence() > lastDisplayedRollSequence) {
        lastDisplayedRollSequence = roll.sequence();
        for (TextButton button : choiceButtons) {
          button.setDisabled(true);
        }
        resultLabel.setStyle(createLabelStyle(DEFAULT, BODY_COLOUR));
        resultLabel.setText("ROLLING...");
        diceRollDisplay.play(
            roll, () -> handleSessionResolution(resolution, selectedButton, originalButtonText));
        return;
      }
    }

    handleSessionResolution(resolution, selectedButton, originalButtonText);
  }

  private void handleSessionResolution(
      ChanceResolution resolution, TextButton selectedButton, String originalButtonText) {
    if (resolution.getStatus() == ChanceResolution.Status.DELEGATED && cardFusionFlow != null) {
      rootTable.setVisible(false);
      cardFusionView =
          new CardFusionSelectionView(cardFusionFlow, ServiceLocator.getCardLibrary(), skin, stage);
      cardFusionView.show();
      return;
    }
    if (resolution.getStatus() == ChanceResolution.Status.AWAITING_CHOICE) {
      refreshChoices();
      resultLabel.setStyle(createLabelStyle(DEFAULT, BODY_COLOUR));
      resultLabel.setText(encounterSession.getStageResultText());
      return;
    }
    if (!resolution.isSuccess()) {
      for (TextButton button : choiceButtons) {
        button.setDisabled(false);
      }
      resultLabel.setStyle(createLabelStyle(DEFAULT, new Color(0.9f, 0.35f, 0.3f, 1f)));
      resultLabel.setText("OUTCOME\n" + resolution.getMessage());
      if (scenicAbandonedMine || scenicCardFusion) {
        resultLabel.getParent().setVisible(true);
      }
      return;
    }
    showOutcome(resolution.getOutcome(), selectedButton, originalButtonText);
  }

  private void showOutcome(
      ChanceOutcome outcome, TextButton selectedButton, String originalButtonText) {
    if (outcome == null) {
      for (TextButton button : choiceButtons) {
        button.setDisabled(false);
      }
      resultLabel.setStyle(createLabelStyle(DEFAULT, new Color(0.9f, 0.35f, 0.3f, 1f)));
      resultLabel.setText(
          "OUTCOME\nThis choice could not be resolved. Please select another option.");
      if (scenicAbandonedMine || scenicCardFusion) {
        resultLabel.getParent().setVisible(true);
      }
      return;
    }

    choiceResolved = true;
    for (TextButton button : choiceButtons) {
      button.setDisabled(true);
    }
    selectedButton.setStyle(selectedChoiceStyle);
    selectedButton.setText("SELECTED  -  " + originalButtonText);

    resultLabel.setStyle(createLabelStyle(DEFAULT, BODY_COLOUR));
    resultLabel.setText("OUTCOME\n" + formatOutcome(outcome));
    if (scenicAbandonedMine || scenicCardFusion) {
      resultLabel.getParent().setVisible(true);
      choicesTable.setVisible(false);
    }
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
    if (outcome.getCardRewardId() != null) {
      changes.add(
          String.format("You receive the %s card.", formatTitle(outcome.getCardRewardId())));
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
    if (helpDialog != null) {
      helpDialog.remove();
    }
    if (cardFusionView != null) {
      cardFusionView.dispose();
    }
    if (rootTable != null) {
      rootTable.remove();
    }
    super.dispose();
  }
}
