package com.csse3200.game.components.chance;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
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
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.csse3200.game.cards.CardDiscoveryService;
import com.csse3200.game.chance.CardFusionEncounterBehaviour;
import com.csse3200.game.chance.ChanceChoice;
import com.csse3200.game.chance.ChanceEncounter;
import com.csse3200.game.chance.ChanceOutcome;
import com.csse3200.game.chance.DiceEncounterBehaviour;
import com.csse3200.game.chance.DiceRoll;
import com.csse3200.game.chance.WishingFountainEncounterBehaviour;
import com.csse3200.game.components.encounters.AcquisitionResultDisplay;
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
  public static final String DICE_LOW_BUTTON_TEXTURE = "images/chance/dice_low_button_v2.png";
  public static final String DICE_HIGH_BUTTON_TEXTURE = "images/chance/dice_high_button_v2.png";
  public static final String DICE_CONTINUE_BUTTON_TEXTURE =
      "images/chance/dice_continue_button_v2.png";
  public static final String ABANDONED_MINE_BACKGROUND_TEXTURE =
      "images/chance/abandoned_mine_scene_v1.png";
  public static final String FUSION_BACKGROUND_TEXTURE = "images/chance/card_fusion_forge_v1.png";
  public static final String FUSION_FORGE_BUTTON_TEXTURE =
      "images/chance/card_fusion_forge_button_v1.png";
  public static final String FUSION_QUIET_BUTTON_TEXTURE =
      "images/chance/card_fusion_quiet_button_v1.png";
  public static final String FOUNTAIN_BACKGROUND_TEXTURE = WishingFountainScene.BACKGROUND_TEXTURE;
  public static final String FOUNTAIN_WISH_BUTTON_TEXTURE =
      "images/chance/wishing_fountain_wish_button_v1.png";
  public static final String FOUNTAIN_LEAVE_BUTTON_TEXTURE =
      "images/chance/wishing_fountain_leave_button_v1.png";
  public static final String SHRINE_BACKGROUND_TEXTURE = MysteriousShrineScene.BACKGROUND_TEXTURE;
  public static final String SHRINE_FLAME_TEXTURE = MysteriousShrineScene.FLAME_TEXTURE;
  public static final String SHRINE_OFFERING_BUTTON_TEXTURE =
      "images/chance/mysterious_shrine_offering_button_v1.png";
  public static final String SHRINE_LEAVE_BUTTON_TEXTURE =
      "images/chance/mysterious_shrine_leave_button_v1.png";
  public static final String FLOODED_BACKGROUND_TEXTURE =
      "images/chance/flooded_crossing_static_v1.png";
  public static final String SHRINE_BRAND_BUTTON_TEXTURE =
      "images/chance/shrine_brand_button_v2.png";
  public static final String SHRINE_BLOOD_BUTTON_TEXTURE =
      "images/chance/shrine_blood_button_v2.png";
  public static final String CROSSING_FERRY_BUTTON_TEXTURE =
      "images/chance/crossing_ferry_button_v2.png";
  public static final String CROSSING_WADE_BUTTON_TEXTURE =
      "images/chance/crossing_wade_button_v2.png";
  public static final String MINE_EXPLORE_BUTTON_TEXTURE =
      "images/chance/mine_explore_button_v2.png";
  public static final String MINE_LEAVE_BUTTON_TEXTURE = "images/chance/mine_leave_button_v2.png";
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

  private static final String STYLE_NAME_WHITE = "white";
  private static final String STYLE_NAME_LARGE = "large";
  private static final String STYLE_NAME_SMALL = "small";
  private static final String STYLE_NAME_DEFAULT = "default";
  private static final String BUTTON = "button";

  private final ChanceEncounter encounter;
  private final ChanceEncounterSession encounterSession;
  private final CardFusionEncounterFlow cardFusionFlow;
  private final boolean scenicDiceGame;
  private final boolean scenicAbandonedMine;
  private final boolean scenicCardFusion;
  private final boolean scenicFountain;
  private WishingFountainScene fountainScene;
  private MysteriousShrineScene shrineScene;
  private final boolean scenicShrine;
  private final boolean scenicFlooded;
  private final boolean scenicHealer;
  public static final String HEALER_BACKGROUND_TEXTURE =
      "images/chance/wandering_healer_static_v1.png";
  public static final String HEALER_HEALING_BUTTON_TEXTURE =
      "images/chance/wandering_healer_healing_button_v1.png";
  public static final String HEALER_LEAVE_BUTTON_TEXTURE =
      "images/chance/wandering_healer_leave_button_v1.png";
  private Group fusionIntroContent;
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
  private int visibleChoiceCount;
  private DiceRollDisplay diceRollDisplay;
  private CardFusionSelectionView cardFusionView;
  private AcquisitionResultDisplay acquisitionView;
  private ContextualHelpDialog helpDialog;
  private TextButton helpButton;
  private int lastDisplayedRollSequence;
  private boolean choiceResolved;
  private boolean completionSent;
  private static final String CHANCE_ENCOUNTER_LABEL = "CHANCE ENCOUNTER";
  private static final String CHOOSE_RESPONSE_LABEL = "CHOOSE YOUR RESPONSE";
  private static final String DETERMINE_OUTCOME_LABEL = "Your decision will determine the outcome.";
  private static final String CONTINUE_BUTTON = "Continue";
  private static final String OUTCOME_TEXT = "OUTCOME\n";

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
    this.scenicFountain = WishingFountainEncounterBehaviour.ENCOUNTER_ID.equals(encounter.getId());
    this.scenicShrine = "mysterious-shrine".equals(encounter.getId());
    this.scenicFlooded = "flooded-crossing".equals(encounter.getId());
    this.scenicHealer = "wandering-healer".equals(encounter.getId());
    this.scenicCardFusion =
        cardFusionFlow != null
            && CardFusionEncounterBehaviour.ENCOUNTER_ID.equals(encounter.getId());
    this.completionCallback = completionCallback;
    if ((completionCallback != null || encounterSession != null) && nodeId == null) {
      throw new IllegalArgumentException("nodeId cannot be null for an integrated encounter");
    }
    this.nodeId = nodeId;
  }

  @Override
  public void create() {
    super.create();
    addActors();
  }

  private void addActors() {
    if (scenicHealer) {
      addHealerActors();
      return;
    }
    if (scenicFlooded) {
      addFloodedActors();
      return;
    }
    if (scenicShrine) {
      addShrineActors();
      return;
    }
    if (scenicFountain) {
      addFountainActors();
      return;
    }
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
    rootTable.setBackground(skin.newDrawable(STYLE_NAME_WHITE, BACKDROP_COLOUR));
    rootTable.setTouchable(Touchable.enabled);
    rootTable.center();
    rootTable.getColor().a = 0f;

    Table encounterTable = new Table();
    encounterTable.setBackground(skin.newDrawable("window-w", PANEL_COLOUR));
    encounterTable.pad(30f, 40f, 34f, 40f);
    encounterTable.defaults().spaceBottom(14f);

    Label eyebrowLabel =
        new Label(CHANCE_ENCOUNTER_LABEL, createLabelStyle(STYLE_NAME_SMALL, GOLD_COLOUR));
    Label titleLabel =
        new Label(formatTitle(encounter.getId()), createLabelStyle(STYLE_NAME_LARGE, BODY_COLOUR));
    Label descriptionLabel =
        new Label(encounter.getDescription(), createLabelStyle(STYLE_NAME_DEFAULT, BODY_COLOUR));
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

    promptLabel =
        new Label(CHOOSE_RESPONSE_LABEL, createLabelStyle(STYLE_NAME_SMALL, MUTED_COLOUR));
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
    resultTable.setBackground(skin.newDrawable(STYLE_NAME_WHITE, RESULT_COLOUR));
    resultTable.pad(14f, 18f, 14f, 18f);
    resultLabel =
        new Label(DETERMINE_OUTCOME_LABEL, createLabelStyle(STYLE_NAME_DEFAULT, MUTED_COLOUR));
    resultLabel.setFontScale(1.3f);
    resultLabel.setWrap(true);
    resultTable.add(resultLabel).left().width(CONTENT_WIDTH - 36f);

    encounterTable.add(resultTable).left().width(CONTENT_WIDTH).padTop(6f);
    encounterTable.row();

    continueButton = new TextButton(CONTINUE_BUTTON, createContinueStyle());
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

  private void addHealerActors() {
    rootTable = new Table();
    rootTable.setFillParent(true);
    Stack scene = new Stack();
    Image background;
    ResourceService resources = ServiceLocator.getResourceService();
    if (resources != null && resources.containsAsset(HEALER_BACKGROUND_TEXTURE, Texture.class)) {
      Texture texture = resources.getAsset(HEALER_BACKGROUND_TEXTURE, Texture.class);
      texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      background = new Image(texture);
    } else {
      background = new Image(skin.newDrawable(STYLE_NAME_WHITE, new Color(.07f, .08f, .13f, 1)));
    }
    background.setScaling(com.badlogic.gdx.utils.Scaling.stretch);
    scene.add(background);
    Table information = new Table();
    information.setBackground(
        new EncounterInformationShade(information, skin.getDrawable(STYLE_NAME_WHITE)));
    information
        .top()
        .left()
        .padTop(com.badlogic.gdx.scenes.scene2d.ui.Value.percentHeight(.065f, rootTable))
        .padLeft(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(.032f, rootTable));
    information.defaults().left();
    information
        .add(new Label("+  CHANCE ENCOUNTER", createLabelStyle(STYLE_NAME_SMALL, GOLD_COLOUR)))
        .padBottom(12);
    information.row();
    Label title = new Label("Wandering Healer", createLabelStyle(STYLE_NAME_LARGE, BODY_COLOUR));
    title.setFontScale(.98f);
    information.add(title).padBottom(12);
    information.row();
    information
        .add(new Image(skin.newDrawable(STYLE_NAME_WHITE, new Color(.52f, .37f, .19f, 1))))
        .width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(.40f, rootTable))
        .height(1)
        .padBottom(12);
    information.row();
    Label description =
        new Label(encounter.getDescription(), createLabelStyle(STYLE_NAME_DEFAULT, BODY_COLOUR));
    description.setWrap(true);
    description.setFontScale(.88f);
    information
        .add(description)
        .width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(.40f, rootTable));
    scene.add(information);
    promptLabel = new Label("", createLabelStyle(STYLE_NAME_SMALL, MUTED_COLOUR));
    choiceStyle = healerButtonStyle(false);
    selectedChoiceStyle = new TextButtonStyle(choiceStyle);
    choicesTable = new Table();
    choicesTable
        .bottom()
        .left()
        .padBottom(com.badlogic.gdx.scenes.scene2d.ui.Value.percentHeight(.27f, rootTable))
        .padLeft(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(.06f, rootTable));
    scene.add(choicesTable);
    refreshChoices();
    Table result = new Table();
    result.setName("healer-result-narration");
    result
        .bottom()
        .left()
        .padBottom(com.badlogic.gdx.scenes.scene2d.ui.Value.percentHeight(.27f, rootTable))
        .padLeft(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(.06f, rootTable));
    resultLabel = new Label("", createLabelStyle(STYLE_NAME_DEFAULT, BODY_COLOUR));
    resultLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
    resultLabel.setWrap(true);
    result
        .add(resultLabel)
        .width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(.48f, rootTable))
        .minHeight(65)
        .padBottom(16);
    result.row();
    continueButton = new TextButton(CONTINUE_BUTTON, healerButtonStyle(false));
    continueButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            completeEncounter();
          }
        });
    continueButton.setVisible(false);
    result.add(continueButton).width(300).height(70);
    result.setVisible(false);
    scene.add(result);
    rootTable.add(scene).grow();
    stage.addActor(rootTable);
  }

  private void addFloodedActors() {
    rootTable = new Table();
    rootTable.setFillParent(true);
    Stack scene = new Stack();
    Image background;
    ResourceService resources = ServiceLocator.getResourceService();
    if (resources != null && resources.containsAsset(FLOODED_BACKGROUND_TEXTURE, Texture.class)) {
      Texture texture = resources.getAsset(FLOODED_BACKGROUND_TEXTURE, Texture.class);
      texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      background = new Image(texture);
    } else {
      background = new Image(skin.newDrawable(STYLE_NAME_WHITE, new Color(.07f, .08f, .13f, 1)));
    }
    background.setScaling(com.badlogic.gdx.utils.Scaling.stretch);
    scene.add(background);
    Table information = new Table();
    information.setBackground(
        new EncounterInformationShade(information, skin.getDrawable(STYLE_NAME_WHITE)));
    information.top().left().pad(36f, 42f, 0f, 0f);
    information.defaults().left();
    information
        .add(new Label(CHANCE_ENCOUNTER_LABEL, createLabelStyle(STYLE_NAME_SMALL, GOLD_COLOUR)))
        .padBottom(12);
    information.row();
    Label title = new Label("Flooded Crossing", createLabelStyle(STYLE_NAME_LARGE, BODY_COLOUR));
    title.setFontScale(.98f);
    information.add(title).padBottom(12);
    information.row();
    information
        .add(new Image(skin.newDrawable(STYLE_NAME_WHITE, new Color(.52f, .37f, .19f, 1))))
        .width(390)
        .height(1)
        .padBottom(12);
    information.row();
    Label description =
        new Label(encounter.getDescription(), createLabelStyle(STYLE_NAME_DEFAULT, BODY_COLOUR));
    description.setWrap(true);
    description.setFontScale(.88f);
    information.add(description).width(390);
    scene.add(information);
    promptLabel = new Label("", createLabelStyle(STYLE_NAME_SMALL, MUTED_COLOUR));
    choiceStyle = shrineButtonStyle(false);
    selectedChoiceStyle = new TextButtonStyle(choiceStyle);
    choicesTable = new Table();
    choicesTable.bottom().padBottom(30);
    scene.add(choicesTable);
    refreshChoices();
    Table result = new Table();
    result.setName("flooded-result-narration");
    result.bottom().padBottom(30);
    resultLabel = new Label("", createLabelStyle(STYLE_NAME_DEFAULT, BODY_COLOUR));
    resultLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
    resultLabel.setWrap(true);
    result
        .add(resultLabel)
        .width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(.7f, rootTable))
        .minHeight(65)
        .padBottom(16);
    result.row();
    continueButton = new TextButton(CONTINUE_BUTTON, shrineButtonStyle(false));
    continueButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            completeEncounter();
          }
        });
    continueButton.setVisible(false);
    result.add(continueButton).width(276).height(64);
    result.setVisible(false);
    scene.add(result);
    rootTable.add(scene).grow();
    stage.addActor(rootTable);
  }

  private void addShrineActors() {
    rootTable = new Table();
    rootTable.setFillParent(true);
    Stack scene = new Stack();
    shrineScene = new MysteriousShrineScene(skin);
    scene.add(shrineScene);
    Table information = new Table();
    information.setBackground(
        new EncounterInformationShade(information, skin.getDrawable(STYLE_NAME_WHITE)));
    information.top().left().pad(36f, 42f, 0f, 0f);
    information.defaults().left();
    information
        .add(new Label(CHANCE_ENCOUNTER_LABEL, createLabelStyle(STYLE_NAME_SMALL, GOLD_COLOUR)))
        .padBottom(12);
    information.row();
    Label title = new Label("Mysterious Shrine", createLabelStyle(STYLE_NAME_LARGE, BODY_COLOUR));
    title.setFontScale(.98f);
    information.add(title).padBottom(12);
    information.row();
    information
        .add(new Image(skin.newDrawable(STYLE_NAME_WHITE, new Color(.52f, .37f, .19f, 1))))
        .width(390)
        .height(1)
        .padBottom(12);
    information.row();
    Label description =
        new Label(encounter.getDescription(), createLabelStyle(STYLE_NAME_DEFAULT, BODY_COLOUR));
    description.setWrap(true);
    description.setFontScale(.88f);
    information.add(description).width(390);
    scene.add(information);
    promptLabel = new Label("", createLabelStyle(STYLE_NAME_SMALL, MUTED_COLOUR));
    choiceStyle = shrineScenicButtonStyle(false);
    selectedChoiceStyle = new TextButtonStyle(choiceStyle);
    choicesTable = new Table();
    choicesTable.bottom().padBottom(30);
    scene.add(choicesTable);
    refreshChoices();
    Table result = new Table();
    result.setName("shrine-result-narration");
    result.bottom().padBottom(30);
    resultLabel = new Label("", createLabelStyle(STYLE_NAME_DEFAULT, BODY_COLOUR));
    resultLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
    resultLabel.setWrap(true);
    result.add(resultLabel).width(800).minHeight(65).padBottom(16);
    result.row();
    continueButton = new TextButton(CONTINUE_BUTTON, shrineScenicButtonStyle(false));
    continueButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            completeEncounter();
          }
        });
    continueButton.setVisible(false);
    result.add(continueButton).width(300).height(76);
    result.setVisible(false);
    scene.add(result);
    rootTable.add(scene).grow();
    stage.addActor(rootTable);
  }

  private TextButtonStyle shrineButtonStyle(boolean secondary) {
    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));
    Color edge = new Color(.48f, .34f, .18f, 1);
    style.up =
        FusionSceneAssets.pixelFrame(
            skin,
            secondary ? new Color(.10f, .075f, .065f, .96f) : new Color(.15f, .10f, .065f, .96f),
            edge,
            FusionSceneAssets.FrameKind.BUTTON);
    style.over =
        FusionSceneAssets.pixelFrame(
            skin,
            new Color(.23f, .15f, .085f, .97f),
            new Color(.65f, .47f, .24f, 1),
            FusionSceneAssets.FrameKind.BUTTON);
    style.down =
        FusionSceneAssets.pixelFrame(
            skin, new Color(.09f, .065f, .05f, 1), edge, FusionSceneAssets.FrameKind.BUTTON);
    style.disabled = style.up;
    style.fontColor = BODY_COLOUR;
    style.disabledFontColor = MUTED_COLOUR;
    return style;
  }

  private TextButtonStyle healerButtonStyle(boolean secondary) {
    return scenicTextureButtonStyle(
        secondary ? HEALER_LEAVE_BUTTON_TEXTURE : HEALER_HEALING_BUTTON_TEXTURE,
        secondary ? 206 : 146,
        secondary ? 417 : 541,
        secondary ? 450 : 480,
        780,
        secondary ? new Color(0.82f, 0.79f, 0.70f, 1f) : BODY_COLOUR);
  }

  private TextButtonStyle shrineScenicButtonStyle(boolean secondary) {
    return scenicTextureButtonStyle(
        secondary ? SHRINE_LEAVE_BUTTON_TEXTURE : SHRINE_OFFERING_BUTTON_TEXTURE,
        secondary ? 76 : 8,
        secondary ? 657 : 609,
        secondary ? 430 : 450,
        secondary ? 700 : 650,
        BODY_COLOUR);
  }

  private TextButtonStyle scenicTextureButtonStyle(
      String texturePath,
      int cropTop,
      int cropHeight,
      int capSourceWidth,
      int fillSourceX,
      Color fontColor) {
    ResourceService resources = ServiceLocator.getResourceService();
    if (resources == null || !resources.containsAsset(texturePath, Texture.class)) {
      return shrineButtonStyle(false);
    }
    Texture texture = resources.getAsset(texturePath, Texture.class);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));
    style.up =
        scenicTextureButtonDrawable(
            texture,
            cropTop,
            cropHeight,
            capSourceWidth,
            fillSourceX,
            new Color(0.90f, 0.88f, 0.83f, 1f));
    style.over =
        scenicTextureButtonDrawable(
            texture, cropTop, cropHeight, capSourceWidth, fillSourceX, Color.WHITE);
    style.down =
        scenicTextureButtonDrawable(
            texture,
            cropTop,
            cropHeight,
            capSourceWidth,
            fillSourceX,
            new Color(0.70f, 0.69f, 0.67f, 1f));
    style.disabled = style.up;
    style.fontColor = fontColor;
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.WHITE;
    style.disabledFontColor = MUTED_COLOUR;
    return style;
  }

  /** Stretch only the quiet central stone, preserving the carved decorations at both ends. */
  private Drawable scenicTextureButtonDrawable(
      Texture texture, int top, int height, int capWidth, int fillX, Color tint) {
    TextureRegionDrawable left =
        new TextureRegionDrawable(new TextureRegion(texture, 0, top, capWidth, height));
    TextureRegionDrawable fill =
        new TextureRegionDrawable(new TextureRegion(texture, fillX, top, 64, height));
    TextureRegionDrawable right =
        new TextureRegionDrawable(
            new TextureRegion(texture, texture.getWidth() - capWidth, top, capWidth, height));
    return new BaseDrawable() {
      @Override
      public void draw(Batch batch, float x, float y, float width, float drawHeight) {
        Color original = batch.getColor();
        float r = original.r;
        float g = original.g;
        float b = original.b;
        float a = original.a;
        batch.setColor(r * tint.r, g * tint.g, b * tint.b, a * tint.a);
        float endWidth = Math.min(width * 0.25f, drawHeight * capWidth / height);
        left.draw(batch, x, y, endWidth, drawHeight);
        fill.draw(batch, x + endWidth, y, width - 2f * endWidth, drawHeight);
        right.draw(batch, x + width - endWidth, y, endWidth, drawHeight);
        batch.setColor(r, g, b, a);
      }
    };
  }

  /** Fountain visuals only; existing choices and session still own all gameplay effects. */
  private void addFountainActors() {
    rootTable = new Table();
    rootTable.setFillParent(true);
    Stack scene = new Stack();
    fountainScene = new WishingFountainScene(skin);
    scene.add(fountainScene);

    Table information = new Table();
    information.setBackground(
        new EncounterInformationShade(information, skin.getDrawable(STYLE_NAME_WHITE)));
    information.top().left().pad(40f, 48f, 0f, 0f);
    information.defaults().left();
    information
        .add(new Label(CHANCE_ENCOUNTER_LABEL, createLabelStyle(STYLE_NAME_SMALL, GOLD_COLOUR)))
        .padBottom(14f);
    information.row();
    Label title = new Label("Wishing Fountain", createLabelStyle(STYLE_NAME_LARGE, BODY_COLOUR));
    title.setFontScale(1.1f);
    information.add(title).padBottom(12f);
    information.row();
    Image rule = new Image(skin.newDrawable(STYLE_NAME_WHITE, GOLD_COLOUR));
    information.add(rule).width(380f).height(1f).padBottom(14f);
    information.row();
    Label description =
        new Label(encounter.getDescription(), createLabelStyle(STYLE_NAME_DEFAULT, BODY_COLOUR));
    description.setWrap(true);
    information.add(description).width(360f);
    scene.add(information);

    promptLabel = new Label("", createLabelStyle(STYLE_NAME_SMALL, MUTED_COLOUR));
    choiceStyle = fountainButtonStyle(true);
    selectedChoiceStyle = new TextButtonStyle(choiceStyle);
    choicesTable = new Table();
    scene.add(choicesTable);
    refreshChoices();

    Table result = new Table();
    result.setName("fountain-result-narration");
    result.bottom().padBottom(34f);
    resultLabel = new Label("", createLabelStyle(STYLE_NAME_DEFAULT, BODY_COLOUR));
    resultLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
    resultLabel.setWrap(true);
    result.add(resultLabel).width(850f).minHeight(50f).padBottom(22f);
    result.row();
    continueButton = new TextButton(CONTINUE_BUTTON, fountainButtonStyle(true));
    continueButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            fountainScene.complete();
            completeEncounter();
          }
        });
    continueButton.setVisible(false);
    result.add(continueButton).width(380f).height(102f);
    result.setVisible(false);
    scene.add(result);
    rootTable.add(scene).grow();
    stage.addActor(rootTable);
  }

  private TextButtonStyle fountainButtonStyle(boolean primary) {
    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));
    ResourceService resources = ServiceLocator.getResourceService();
    String texturePath = primary ? FOUNTAIN_WISH_BUTTON_TEXTURE : FOUNTAIN_LEAVE_BUTTON_TEXTURE;
    if (resources != null && resources.containsAsset(texturePath, Texture.class)) {
      Texture texture = resources.getAsset(texturePath, Texture.class);
      texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      TextureRegion region = new TextureRegion(texture);
      style.up = fountainButtonDrawable(region, Color.WHITE);
      style.over = fountainButtonDrawable(region, new Color(0.86f, 0.94f, 1f, 1f));
      style.down = fountainButtonDrawable(region, new Color(0.72f, 0.78f, 0.84f, 1f));
      style.disabled = style.up;
      style.fontColor = BODY_COLOUR;
      style.overFontColor = Color.WHITE;
      style.downFontColor = Color.WHITE;
      style.disabledFontColor = MUTED_COLOUR;
      return style;
    }

    Color edge = primary ? new Color(0.62f, 0.46f, 0.25f, 1) : new Color(0.38f, 0.34f, 0.27f, 1);
    style.up =
        FusionSceneAssets.pixelFrame(
            skin,
            primary
                ? new Color(0.22f, 0.13f, 0.08f, 0.96f)
                : new Color(0.075f, 0.08f, 0.09f, 0.96f),
            edge,
            FusionSceneAssets.FrameKind.BUTTON);
    style.over =
        FusionSceneAssets.pixelFrame(
            skin,
            new Color(0.29f, 0.20f, 0.11f, 0.98f),
            GOLD_COLOUR,
            FusionSceneAssets.FrameKind.BUTTON);
    style.down = style.up;
    style.disabled = style.up;
    style.fontColor = BODY_COLOUR;
    style.overFontColor = Color.WHITE;
    style.disabledFontColor = MUTED_COLOUR;
    return style;
  }

  private Drawable fountainButtonDrawable(TextureRegion region, Color tint) {
    Drawable drawable = new TextureRegionDrawable(region).tint(tint);
    if (drawable instanceof BaseDrawable baseDrawable) {
      baseDrawable.setMinWidth(0f);
      baseDrawable.setMinHeight(0f);
    }
    return drawable;
  }

  private void addFusionIntroActors() {
    rootTable = new Table();
    rootTable.setFillParent(true);
    rootTable.setBackground(skin.newDrawable(STYLE_NAME_WHITE, BACKDROP_COLOUR));
    rootTable.setTouchable(Touchable.enabled);
    rootTable.getColor().a = 0f;

    Group scene = new Group();
    scene.setSize(FusionSceneAssets.WIDTH, FusionSceneAssets.HEIGHT);
    scene.addActor(FusionSceneAssets.background(skin));
    addFusionAltarLighting(scene);
    addFusionAltarEmbers(scene);

    Label eyebrow =
        new Label(CHANCE_ENCOUNTER_LABEL, createLabelStyle(STYLE_NAME_SMALL, GOLD_COLOUR));
    eyebrow.setBounds(68f, 721f, 520f, 30f);
    scene.addActor(eyebrow);
    Label title = new Label("CARD FUSION", createLabelStyle(STYLE_NAME_LARGE, BODY_COLOUR));
    title.setFontScale(1.35f);
    title.setBounds(68f, 657f, 660f, 57f);
    scene.addActor(title);

    fusionIntroContent = new Group();
    fusionIntroContent.setName("fusion-intro-content");
    scene.addActor(fusionIntroContent);
    addFusionPreviewCard(fusionIntroContent, 395f, 354f, 12f, 0f);
    addFusionPreviewCard(fusionIntroContent, 570f, 370f, 0f, 0.55f);
    addFusionPreviewCard(fusionIntroContent, 745f, 354f, -12f, 1.1f);

    Label descriptionShadow =
        new Label(
            encounter.getDescription(),
            createLabelStyle(STYLE_NAME_DEFAULT, new Color(0.02f, 0.02f, 0.04f, 0.85f)));
    descriptionShadow.setAlignment(com.badlogic.gdx.utils.Align.center);
    descriptionShadow.setWrap(true);
    descriptionShadow.setBounds(182f, 157f, 920f, 58f);
    fusionIntroContent.addActor(descriptionShadow);
    Label description =
        new Label(encounter.getDescription(), createLabelStyle(STYLE_NAME_DEFAULT, BODY_COLOUR));
    description.setAlignment(com.badlogic.gdx.utils.Align.center);
    description.setWrap(true);
    description.setBounds(180f, 159f, 920f, 58f);
    fusionIntroContent.addActor(description);

    promptLabel =
        new Label(CHOOSE_RESPONSE_LABEL, createLabelStyle(STYLE_NAME_SMALL, MUTED_COLOUR));
    promptLabel.setVisible(false);
    scene.addActor(promptLabel);
    choiceStyle = FusionSceneAssets.buttonStyle(skin, true);
    selectedChoiceStyle = new TextButtonStyle(choiceStyle);
    choicesTable = new Table();
    choicesTable.setBounds(137f, 54f, 1006f, 98f);
    scene.addActor(choicesTable);
    refreshChoices();

    Table resultPanel = new Table();
    resultPanel.setName("fusion-result-narration");
    resultLabel = new Label("", createLabelStyle(STYLE_NAME_DEFAULT, BODY_COLOUR));
    resultLabel.setWrap(true);
    resultLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
    resultPanel.add(resultLabel).grow();
    resultPanel.setBounds(180f, 130f, 920f, 58f);
    resultPanel.setVisible(false);
    scene.addActor(resultPanel);

    continueButton = FusionSceneAssets.button(CONTINUE_BUTTON, skin, false);
    continueButton.setBounds(502f, 34f, 276f, 70f);
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
    helpButton = ContextualHelpDialog.createHelpButton(skin);
    helpButton.setName("fusion-intro-help-button");
    helpButton.setBounds(1152f, 682f, 64f, 64f);
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
    Image mark = new Image(skin.newDrawable(STYLE_NAME_WHITE, new Color(1f, 0.56f, 0.22f, alpha)));
    mark.setBounds(x, y, width, height);
    mark.setTouchable(Touchable.disabled);
    scene.addActor(mark);
  }

  private void addFusionAltarEmbers(Group scene) {
    float[][] positions = {{525f, 354f}, {562f, 382f}, {642f, 359f}, {704f, 382f}, {748f, 354f}};
    for (int i = 0; i < positions.length; i++) {
      Image ember =
          new Image(skin.newDrawable(STYLE_NAME_WHITE, new Color(1f, 0.55f, 0.24f, 0.72f)));
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
      Image vertical = new Image(skin.newDrawable(STYLE_NAME_WHITE, warm));
      vertical.setBounds(2f, 0f, 2f, 6f);
      spark.addActor(vertical);
      Image horizontal = new Image(skin.newDrawable(STYLE_NAME_WHITE, warm));
      horizontal.setBounds(0f, 2f, 6f, 2f);
      spark.addActor(horizontal);
      Image center =
          new Image(skin.newDrawable(STYLE_NAME_WHITE, new Color(1f, 0.91f, 0.68f, 0.8f)));
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
    rootTable.setBackground(skin.newDrawable(STYLE_NAME_WHITE, BACKDROP_COLOUR));
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

    Label eyebrowLabel =
        new Label(CHANCE_ENCOUNTER_LABEL, createLabelStyle(STYLE_NAME_SMALL, GOLD_COLOUR));
    eyebrowLabel.setFontScale(1.2f);
    eyebrowLabel.setBounds(165f, 682f, 470f, 32f);
    scene.addActor(eyebrowLabel);

    Label titleLabel =
        new Label(formatTitle(encounter.getId()), createLabelStyle(STYLE_NAME_LARGE, BODY_COLOUR));
    titleLabel.setFontScale(1.45f);
    titleLabel.setBounds(165f, 622f, 470f, 56f);
    scene.addActor(titleLabel);

    Label descriptionLabel =
        new Label(encounter.getDescription(), createLabelStyle(STYLE_NAME_DEFAULT, BODY_COLOUR));
    descriptionLabel.setFontScale(1.13f);
    descriptionLabel.setWrap(true);
    descriptionLabel.setBounds(165f, 540f, 470f, 75f);
    scene.addActor(descriptionLabel);

    diceRollDisplay =
        new DiceRollDisplay(skin, createLabelStyle(STYLE_NAME_DEFAULT, GOLD_COLOUR), true);
    diceRollDisplay.getTable().setBounds(168f, 365f, 505f, 178f);
    scene.addActor(diceRollDisplay.getTable());

    promptLabel = new Label(CHOOSE_RESPONSE_LABEL, createLabelStyle(STYLE_NAME_SMALL, BODY_COLOUR));
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
    resultTable.setBackground(createDiceResultDrawable(new Color(0.68f, 0.48f, 0.47f, 0.93f)));
    resultTable.pad(8f, 25f, 8f, 25f);
    resultLabel =
        new Label(DETERMINE_OUTCOME_LABEL, createLabelStyle(STYLE_NAME_DEFAULT, BODY_COLOUR));
    resultLabel.setFontScale(1.08f);
    resultLabel.setWrap(true);
    resultTable.add(resultLabel).left().grow();
    resultTable.setBounds(245f, 38f, 790f, 70f);
    scene.addActor(resultTable);

    continueButton =
        new TextButton(CONTINUE_BUTTON, illustratedButtonStyle(DICE_CONTINUE_BUTTON_TEXTURE));
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
    helpButton = ContextualHelpDialog.createHelpButton(skin);

    helpButton.setName("dice-help-button");
    helpButton.setColor(new Color(1f, 0.94f, 0.82f, 1f));
    helpButton.setBounds(1152f, 682f, 64f, 64f);
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
    rootTable.setBackground(skin.newDrawable(STYLE_NAME_WHITE, BACKDROP_COLOUR));
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

    Table information = new Table();
    information.setName("mine-information");
    information.setBounds(0, 0, SCENE_WIDTH, SCENE_HEIGHT);
    information.setBackground(
        new EncounterInformationShade(information, skin.getDrawable(STYLE_NAME_WHITE)));
    information.top().left().pad(36f, 42f, 0f, 0f);
    information.defaults().left();
    information
        .add(new Label(CHANCE_ENCOUNTER_LABEL, createLabelStyle(STYLE_NAME_SMALL, GOLD_COLOUR)))
        .padBottom(12)
        .row();
    Label titleLabel =
        new Label(formatTitle(encounter.getId()), createLabelStyle(STYLE_NAME_LARGE, BODY_COLOUR));
    titleLabel.setName("mine-information-title");
    titleLabel.setFontScale(.98f);
    information.add(titleLabel).padBottom(12).row();
    Image divider = new Image(skin.newDrawable(STYLE_NAME_WHITE, new Color(.52f, .37f, .19f, 1)));
    divider.setTouchable(Touchable.disabled);
    information.add(divider).width(390).height(1).padBottom(12).row();
    Label descriptionLabel =
        new Label(encounter.getDescription(), createLabelStyle(STYLE_NAME_DEFAULT, BODY_COLOUR));
    descriptionLabel.setName("mine-information-description");
    descriptionLabel.setFontScale(.88f);
    descriptionLabel.setWrap(true);
    information.add(descriptionLabel).width(390);
    scene.addActor(information);

    // The original prompt remains available to refreshChoices, but the scene and buttons
    // provide the visual invitation without another opaque panel over the mine cart.
    promptLabel =
        new Label(CHOOSE_RESPONSE_LABEL, createLabelStyle(STYLE_NAME_SMALL, MUTED_COLOUR));
    promptLabel.setVisible(false);
    scene.addActor(promptLabel);

    choiceStyle = createMineChoiceStyle(new Color(0.83f, 0.65f, 0.48f, 1f));
    selectedChoiceStyle = new TextButtonStyle(choiceStyle);
    choicesTable = new Table();
    choicesTable.setBounds(55f, 23f, 1170f, 105f);
    scene.addActor(choicesTable);
    refreshChoices();

    Table resultTable = new Table();
    resultTable.setBackground((Drawable) null);
    resultTable.pad(12f, 24f, 12f, 24f);
    resultLabel =
        new Label(DETERMINE_OUTCOME_LABEL, createLabelStyle(STYLE_NAME_DEFAULT, BODY_COLOUR));
    resultLabel.setFontScale(1.06f);
    resultLabel.setWrap(true);
    resultTable.add(resultLabel).left().grow();
    resultTable.setBounds(170f, 138f, 940f, 100f);
    resultTable.setVisible(false);
    scene.addActor(resultTable);

    continueButton =
        new TextButton(CONTINUE_BUTTON, createMineChoiceStyle(new Color(0.64f, 0.55f, 0.55f, 1f)));
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

  /** Preserve the decorated end caps while fitting an illustration to either choice or Continue. */
  private TextButtonStyle illustratedButtonStyle(String path) {
    ResourceService resources = ServiceLocator.getResourceService();
    if (resources == null || !resources.containsAsset(path, Texture.class)) {
      return shrineButtonStyle(false);
    }
    Texture texture = resources.getAsset(path, Texture.class);
    texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    TextButtonStyle style = new TextButtonStyle(skin.get(TextButtonStyle.class));
    style.up =
        scenicTextureButtonDrawable(
            texture, 0, texture.getHeight(), 450, 700, new Color(.85f, .83f, .80f, 1f));
    style.over =
        scenicTextureButtonDrawable(texture, 0, texture.getHeight(), 450, 700, Color.WHITE);
    style.down =
        scenicTextureButtonDrawable(
            texture, 0, texture.getHeight(), 450, 700, new Color(.67f, .66f, .64f, 1f));
    style.disabled = style.up;
    style.fontColor = BODY_COLOUR;
    style.overFontColor = Color.WHITE;
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

  /** Distinguishes the two predictions from the later dice decisions. */
  private TextButtonStyle createDicePredictionStyle(boolean high) {
    TextButtonStyle style = new TextButtonStyle(choiceStyle);
    Color accent;
    if (high) {
      accent = new Color(0.78f, 0.58f, 1f, 1f);
      style.up = createDicePredictionDrawable(new Color(0.61f, 0.43f, 0.83f, 1f), accent, true);
      style.over = createDicePredictionDrawable(new Color(0.78f, 0.58f, 1f, 1f), accent, true);
      style.down = createDicePredictionDrawable(new Color(0.42f, 0.29f, 0.63f, 1f), accent, true);
      style.disabled =
          createDicePredictionDrawable(new Color(0.43f, 0.34f, 0.53f, 0.92f), accent, true);
      style.fontColor = new Color(0.89f, 0.79f, 1f, 1f);
    } else {
      accent = new Color(1f, 0.78f, 0.32f, 1f);
      style.up = createDicePredictionDrawable(new Color(0.95f, 0.68f, 0.31f, 1f), accent, false);
      style.over = createDicePredictionDrawable(new Color(1f, 0.83f, 0.44f, 1f), accent, false);
      style.down = createDicePredictionDrawable(new Color(0.66f, 0.44f, 0.19f, 1f), accent, false);
      style.disabled =
          createDicePredictionDrawable(new Color(0.56f, 0.42f, 0.27f, 0.92f), accent, false);
      style.fontColor = new Color(1f, 0.88f, 0.57f, 1f);
    }
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.WHITE;
    style.disabledFontColor = MUTED_COLOUR;
    return style;
  }

  /** Take and cash out secure a reward; double down and continue risk it. */
  private TextButtonStyle createDiceDecisionStyle(boolean risk) {
    TextButtonStyle style = new TextButtonStyle(choiceStyle);
    Color accent = risk ? new Color(1f, 0.59f, 0.39f, 1f) : new Color(0.61f, 0.89f, 0.69f, 1f);
    Color mutedAccent =
        risk ? new Color(0.58f, 0.40f, 0.36f, 1f) : new Color(0.38f, 0.54f, 0.44f, 1f);
    style.up =
        createDiceButtonDrawable(
            risk ? new Color(0.83f, 0.44f, 0.38f, 1f) : new Color(0.50f, 0.68f, 0.53f, 1f),
            accent,
            0);
    style.over =
        createDiceButtonDrawable(
            risk ? new Color(1f, 0.57f, 0.46f, 1f) : new Color(0.65f, 0.83f, 0.65f, 1f), accent, 0);
    style.down =
        createDiceButtonDrawable(
            risk ? new Color(0.58f, 0.31f, 0.30f, 1f) : new Color(0.36f, 0.49f, 0.39f, 1f),
            accent,
            0);
    style.disabled =
        createDiceButtonDrawable(
            risk ? new Color(0.43f, 0.33f, 0.32f, 0.92f) : new Color(0.36f, 0.42f, 0.36f, 0.92f),
            mutedAccent,
            0);
    style.fontColor = risk ? new Color(1f, 0.85f, 0.74f, 1f) : new Color(0.82f, 1f, 0.84f, 1f);
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.WHITE;
    style.disabledFontColor = MUTED_COLOUR;
    return style;
  }

  /** The result-screen Continue button keeps the same style for every dice outcome. */
  private TextButtonStyle createDiceContinueStyle() {
    TextButtonStyle style = new TextButtonStyle(choiceStyle);
    Color accent = new Color(0.95f, 0.72f, 0.34f, 1f);
    style.up = createDiceButtonDrawable(new Color(0.73f, 0.52f, 0.43f, 1f), accent, 0);
    style.over = createDiceButtonDrawable(new Color(0.90f, 0.65f, 0.51f, 1f), accent, 0);
    style.down = createDiceButtonDrawable(new Color(0.53f, 0.36f, 0.32f, 1f), accent, 0);
    style.disabled =
        createDiceButtonDrawable(
            new Color(0.42f, 0.34f, 0.33f, 0.92f), new Color(0.53f, 0.44f, 0.32f, 1f), 0);
    style.fontColor = BODY_COLOUR;
    style.overFontColor = Color.WHITE;
    style.downFontColor = Color.WHITE;
    return style;
  }

  private Drawable createDicePredictionDrawable(Color tint, Color accent, boolean high) {
    return createDiceButtonDrawable(tint, accent, high ? 1 : -1);
  }

  private Drawable createDiceButtonDrawable(Color tint, Color accent, int arrowDirection) {
    Drawable plaque = createDicePlaqueDrawable(tint);
    Drawable marker = skin.newDrawable(STYLE_NAME_WHITE, accent);
    return new BaseDrawable() {
      @Override
      public void draw(Batch batch, float x, float y, float width, float height) {
        plaque.draw(batch, x, y, width, height);
        marker.draw(batch, x + 16f, y + 15f, 4f, height - 30f);
        marker.draw(batch, x + width - 20f, y + 15f, 4f, height - 30f);
        if (arrowDirection == 0) {
          marker.draw(batch, x + 30f, y + height / 2f - 2f, 18f, 4f);
          marker.draw(batch, x + width - 48f, y + height / 2f - 2f, 18f, 4f);
        } else {
          for (int row = 0; row < 3; row++) {
            float barWidth = arrowDirection > 0 ? 9f + row * 8f : 25f - row * 8f;
            marker.draw(batch, x + 39f - barWidth / 2f, y + 20f + row * 10f, barWidth, 6f);
          }
        }
      }
    };
  }

  private Drawable createDiceResultDrawable(Color tint) {
    Drawable plaque = createDicePlaqueDrawable(tint);
    Drawable trim = skin.newDrawable(STYLE_NAME_WHITE, new Color(0.77f, 0.56f, 0.31f, 1f));
    return new BaseDrawable() {
      @Override
      public void draw(Batch batch, float x, float y, float width, float height) {
        plaque.draw(batch, x, y, width, height);
        trim.draw(batch, x + 24f, y + height - 11f, width - 48f, 2f);
      }
    };
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
    divider.setBackground(skin.newDrawable(STYLE_NAME_WHITE, GOLD_COLOUR));
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
        encounterSession == null ? CHOOSE_RESPONSE_LABEL : encounterSession.getChoicePrompt());

    List<ChanceChoice> availableChoices =
        encounterSession == null ? encounter.getChoices() : encounterSession.getAvailableChoices();
    visibleChoiceCount = availableChoices.size();
    int choiceNumber = 1;
    for (ChanceChoice choice : availableChoices) {
      addChoiceButton(choice, choiceNumber);
      choiceNumber++;
    }
    if (scenicShrine) {
      float longestText = 0f;
      for (TextButton button : choiceButtons) {
        var label = button.getLabel();
        var measured =
            new com.badlogic.gdx.graphics.g2d.GlyphLayout(label.getStyle().font, label.getText());
        longestText = Math.max(longestText, measured.width * label.getFontScaleX());
      }
      // Share one compact width, with room for the illustrated end caps on both sides.
      for (TextButton button : choiceButtons) {
        choicesTable.getCell(button).width((float) Math.ceil(longestText + 96f));
      }
      if (choiceButtons.size() == 4) {
        float width = (float) Math.ceil(longestText + 96f);
        choicesTable.clearChildren();
        for (String id : List.of("make-offering", "accept-brand", "swear-blood-oath")) {
          for (int i = 0; i < availableChoices.size(); i++) {
            if (id.equals(availableChoices.get(i).getId())) {
              choicesTable.add(choiceButtons.get(i)).width(width).height(82f).pad(0f, 8f, 8f, 8f);
            }
          }
        }
        choicesTable.row();
        for (int i = 0; i < availableChoices.size(); i++) {
          if (!List.of("make-offering", "accept-brand", "swear-blood-oath")
              .contains(availableChoices.get(i).getId())) {
            choicesTable
                .add(choiceButtons.get(i))
                .colspan(3)
                .width(width)
                .height(82f)
                .padBottom(8f)
                .center();
          }
        }
      }
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
    final String originalButtonText = resolveChoiceButtonText(choice);
    TextButton choiceButton = new TextButton(originalButtonText, choiceStyle);
    choiceButton.getLabel().setFontScale(isScenicEncounter() ? 1.02f : 1.3f);
    choiceButton.getLabel().setWrap(true);
    choiceButton.getLabel().setAlignment(com.badlogic.gdx.utils.Align.center);
    choiceButton.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent changeEvent, Actor actor) {
            handleChoiceClicked(choice, choiceButton, originalButtonText);
          }
        });

    choiceButtons.add(choiceButton);
    layoutChoiceButton(choice, choiceNumber, choiceButton);
  }

  private boolean isScenicEncounter() {
    return scenicDiceGame
        || scenicAbandonedMine
        || scenicCardFusion
        || scenicShrine
        || scenicFlooded
        || scenicHealer;
  }

  private boolean hasScenicResultOverlay() {
    return scenicAbandonedMine
        || scenicCardFusion
        || scenicFountain
        || scenicShrine
        || scenicFlooded
        || scenicHealer;
  }

  private String resolveChoiceButtonText(ChanceChoice choice) {
    if (scenicFountain) {
      return WishingFountainEncounterBehaviour.MAKE_WISH_CHOICE_ID.equals(choice.getId())
          ? "Make a Wish"
          : "Leave >";
    }
    return choice.getDescription();
  }

  private void handleChoiceClicked(
      ChanceChoice choice, TextButton choiceButton, String originalButtonText) {
    if (scenicShrine) {
      handleShrineChoiceClicked(choice, choiceButton, originalButtonText);
      return;
    }
    if (scenicFountain) {
      handleFountainChoiceClicked(choice, choiceButton, originalButtonText);
      return;
    }
    resolveChoice(choice, choiceButton, originalButtonText);
  }

  private void handleShrineChoiceClicked(
      ChanceChoice choice, TextButton choiceButton, String originalButtonText) {
    if (shrineScene.isResponding() || choiceResolved) {
      return;
    }
    if ("make-offering".equals(choice.getId())) {
      if (shrineScene.playResponse(() -> resolveChoice(choice, choiceButton, originalButtonText))) {
        for (TextButton button : choiceButtons) {
          button.setDisabled(true);
        }
      }
    } else {
      shrineScene.leave();
      resolveChoice(choice, choiceButton, originalButtonText);
    }
  }

  private void handleFountainChoiceClicked(
      ChanceChoice choice, TextButton choiceButton, String originalButtonText) {
    if (fountainScene.isPlayingWish()) {
      return;
    }
    if (WishingFountainEncounterBehaviour.MAKE_WISH_CHOICE_ID.equals(choice.getId())) {
      if (choiceResolved) {
        return;
      }
      if (fountainScene.playWishAnimation(
          () -> resolveChoice(choice, choiceButton, originalButtonText))) {
        for (TextButton button : choiceButtons) {
          button.setDisabled(true);
        }
      }
    } else {
      resolveChoice(choice, choiceButton, originalButtonText);
    }
  }

  private void layoutChoiceButton(ChanceChoice choice, int choiceNumber, TextButton choiceButton) {
    if (scenicHealer) {
      choiceButton.setStyle(healerButtonStyle("decline".equals(choice.getId())));
      choicesTable
          .add(choiceButton)
          .width(com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(.52f, rootTable))
          .height(66)
          .padBottom(8);
      choicesTable.row();
    } else if (scenicFlooded) {
      choiceButton.setStyle(
          illustratedButtonStyle(
              "hire-ferryman".equals(choice.getId())
                  ? CROSSING_FERRY_BUTTON_TEXTURE
                  : CROSSING_WADE_BUTTON_TEXTURE));
      choicesTable
          .add(choiceButton)
          .width(
              com.badlogic.gdx.scenes.scene2d.ui.Value.percentWidth(
                  "hire-ferryman".equals(choice.getId()) ? .42f : .34f, rootTable))
          .height(94)
          .pad(0, 8, 0, 8);
    } else if (scenicShrine) {
      layoutShrineChoiceButton(choice, choiceButton);
    } else if (scenicFountain) {
      layoutFountainChoiceButton(choice, choiceButton);
    } else if (scenicDiceGame) {
      String choiceId = choice.getId();
      if (DiceEncounterBehaviour.LOW_CHOICE_ID.equals(choiceId)
          || DiceEncounterBehaviour.HIGH_CHOICE_ID.equals(choiceId)) {
        choiceButton.setStyle(
            illustratedButtonStyle(
                DiceEncounterBehaviour.HIGH_CHOICE_ID.equals(choiceId)
                    ? DICE_HIGH_BUTTON_TEXTURE
                    : DICE_LOW_BUTTON_TEXTURE));
      } else if (DiceEncounterBehaviour.TAKE_CHOICE_ID.equals(choiceId)
          || DiceEncounterBehaviour.CASH_OUT_CHOICE_ID.equals(choiceId)) {
        choiceButton.setStyle(createDiceDecisionStyle(false));
      } else if (DiceEncounterBehaviour.DOUBLE_DOWN_CHOICE_ID.equals(choiceId)
          || DiceEncounterBehaviour.CONTINUE_CHOICE_ID.equals(choiceId)) {
        choiceButton.setStyle(createDiceDecisionStyle(true));
      }
      choicesTable.add(choiceButton).width(455f).height(76f).padRight(30f);
    } else if (scenicAbandonedMine) {
      boolean explore = "search-tunnels".equals(choice.getId());
      choiceButton.setStyle(
          illustratedButtonStyle(
              explore ? MINE_EXPLORE_BUTTON_TEXTURE : MINE_LEAVE_BUTTON_TEXTURE));
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
    float opticalOffset = 0f;
    if (scenicShrine) {
      opticalOffset =
          "make-offering".equals(choice.getId())
              ? -.045f
              : "accept-brand".equals(choice.getId())
                  ? .055f
                  : "swear-blood-oath".equals(choice.getId()) ? .085f : .10f;
    } else if (scenicFountain) {
      opticalOffset =
          WishingFountainEncounterBehaviour.MAKE_WISH_CHOICE_ID.equals(choice.getId())
              ? -.075f
              : 0f;
    } else if (scenicFlooded) {
      opticalOffset = .04f;
    } else if (scenicAbandonedMine) {
      opticalOffset = .025f;
    } else if (scenicHealer) {
      opticalOffset = "decline".equals(choice.getId()) ? .025f : .05f;
    } else if (scenicDiceGame
        && (DiceEncounterBehaviour.LOW_CHOICE_ID.equals(choice.getId())
            || DiceEncounterBehaviour.HIGH_CHOICE_ID.equals(choice.getId()))) {
      opticalOffset = .04f;
    }
    centerButtonText(choiceButton, opticalOffset);
  }

  private void centerButtonText(TextButton button, float verticalOffset) {
    button.pad(0f);
    button.center();
    button.getLabel().setAlignment(com.badlogic.gdx.utils.Align.center);
    button
        .getLabelCell()
        .padTop(com.badlogic.gdx.scenes.scene2d.ui.Value.percentHeight(-verticalOffset, button));
    button
        .getLabelCell()
        .padBottom(com.badlogic.gdx.scenes.scene2d.ui.Value.percentHeight(verticalOffset, button));
    button.setUserObject(verticalOffset);
  }

  private void layoutShrineChoiceButton(ChanceChoice choice, TextButton choiceButton) {
    choiceButton.setStyle(
        "accept-brand".equals(choice.getId())
            ? illustratedButtonStyle(SHRINE_BRAND_BUTTON_TEXTURE)
            : "swear-blood-oath".equals(choice.getId())
                ? illustratedButtonStyle(SHRINE_BLOOD_BUTTON_TEXTURE)
                : shrineScenicButtonStyle(!"make-offering".equals(choice.getId())));
    if ("make-offering".equals(choice.getId()))
      choiceButton.addListener(
          new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public void enter(
                com.badlogic.gdx.scenes.scene2d.InputEvent event,
                float x,
                float y,
                int pointer,
                Actor from) {
              if (pointer == -1 && !choiceResolved && !choiceButton.isDisabled())
                shrineScene.setOfferingHovered(true);
            }

            @Override
            public void exit(
                com.badlogic.gdx.scenes.scene2d.InputEvent event,
                float x,
                float y,
                int pointer,
                Actor to) {
              if (pointer == -1) shrineScene.setOfferingHovered(false);
            }
          });
    if (visibleChoiceCount >= 4) {
      choiceButton.getLabel().setFontScale(.87f);
      choicesTable.add(choiceButton).width(600f).height(82f).padBottom(8f);
    } else {
      choicesTable.add(choiceButton).width(650f).height(76f).padBottom(8f);
    }
    choicesTable.row();
  }

  private void layoutFountainChoiceButton(ChanceChoice choice, TextButton choiceButton) {
    boolean wish = WishingFountainEncounterBehaviour.MAKE_WISH_CHOICE_ID.equals(choice.getId());
    choiceButton.setStyle(fountainButtonStyle(wish));
    choiceButton.getLabel().setFontScale(1.02f);
    Table anchor = new Table();
    anchor.setFillParent(true);
    if (wish) {
      anchor.bottom().padBottom(38f);
    } else {
      anchor.bottom().right().pad(38f);
    }
    anchor.add(choiceButton).width(wish ? 400f : 180f).height(wish ? 108f : 62f);
    choicesTable.addActor(anchor);
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
        resultLabel.setStyle(createLabelStyle(STYLE_NAME_DEFAULT, BODY_COLOUR));
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
      resultLabel.setStyle(createLabelStyle(STYLE_NAME_DEFAULT, BODY_COLOUR));
      resultLabel.setText(encounterSession.getStageResultText());
      return;
    }
    if (!resolution.isSuccess()) {
      showResolutionFailure(resolution.getMessage());
      return;
    }
    showOutcome(resolution.getOutcome(), selectedButton, originalButtonText);
    if (acquisitionView == null) {
      acquisitionView =
          new AcquisitionResultDisplay(
              stage, skin, ServiceLocator.getCardLibrary(), ServiceLocator.getResourceService());
    }
    if (encounterSession
        .getAcquisitionSession(ServiceLocator.getCardLibrary())
        .show(acquisitionView, this::completeEncounter, () -> continueButton.setVisible(true))) {
      continueButton.setVisible(false);
    }
  }

  private void showResolutionFailure(String errorMessage) {
    for (TextButton button : choiceButtons) {
      button.setDisabled(false);
    }
    resultLabel.setStyle(createLabelStyle(STYLE_NAME_DEFAULT, new Color(0.9f, 0.35f, 0.3f, 1f)));
    String prefix = (scenicCardFusion || scenicFountain) ? "" : OUTCOME_TEXT;
    resultLabel.setText(prefix + errorMessage);
    if (scenicShrine) {
      choicesTable.padBottom(155);
    }
    if (hasScenicResultOverlay()) {
      resultLabel.getParent().setVisible(true);
    }
  }

  private void showOutcome(
      ChanceOutcome outcome, TextButton selectedButton, String originalButtonText) {
    if (outcome == null) {
      showResolutionFailure("This choice could not be resolved. Please select another option.");
      return;
    }

    choiceResolved = true;
    if (scenicShrine || scenicFlooded || scenicAbandonedMine) {
      continueButton.setStyle(new TextButtonStyle(selectedButton.getStyle()));
    }
    centerButtonText(
        continueButton,
        scenicFountain
            ? -.075f
            : scenicHealer
                ? .05f
                : selectedButton.getUserObject() instanceof Float offset ? offset : 0f);
    for (TextButton button : choiceButtons) {
      button.setDisabled(true);
    }
    if (scenicDiceGame) {
      TextButtonStyle selectedStyle = new TextButtonStyle(selectedButton.getStyle());
      selectedStyle.disabled = selectedStyle.up;
      selectedStyle.disabledFontColor = Color.WHITE;
      selectedButton.setStyle(selectedStyle);
    } else {
      selectedButton.setStyle(selectedChoiceStyle);
    }
    selectedButton.setText("SELECTED  -  " + originalButtonText);

    resultLabel.setStyle(createLabelStyle(STYLE_NAME_DEFAULT, BODY_COLOUR));
    resultLabel.setText(resolveOutcomeDisplayText(outcome));
    LabelStyle outcomeStyle = new LabelStyle(resultLabel.getStyle());
    outcomeStyle.background =
        new EncounterResultShade(
            resultLabel,
            skin.getDrawable(STYLE_NAME_WHITE),
            scenicHealer ? .30f : scenicFountain ? .38f : .60f);
    resultLabel.setStyle(outcomeStyle);
    resultLabel.setName("encounter-result-text");
    resultLabel.setFontScale(.9f);
    resultLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
    if (scenicCardFusion && fusionIntroContent != null) {
      fusionIntroContent.setVisible(false);
    }
    recordVisibleCard(outcome);
    if (scenicShrine) {
      shrineScene.setOfferingHovered(false);
    }
    if (hasScenicResultOverlay()) {
      resultLabel.getParent().setVisible(true);
      choicesTable.setVisible(false);
    }
    continueButton.setVisible(true);
  }

  private String resolveOutcomeDisplayText(ChanceOutcome outcome) {
    if (scenicCardFusion) {
      return outcome.isNoEffect()
          ? "You leave the forge without fusing any cards."
          : formatOutcome(outcome);
    }
    return formatOutcome(outcome);
  }

  private void recordVisibleCard(ChanceOutcome outcome) {
    CardDiscoveryService discovery = ServiceLocator.getCardDiscoveryService();
    if (discovery != null) {
      discovery.recordSeenAll(outcome.getCardRewardIds());
    }
  }

  private void completeEncounter() {
    if (!choiceResolved || completionSent) {
      return;
    }

    completionSent = true;
    if (scenicFlooded || scenicHealer) {
      notifyCompletion();
      rootTable.remove();
      return;
    }
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
    changes.addAll(
        outcome.getCardRewardIds().stream()
            .map(cardId -> String.format("You receive the %s card.", formatTitle(cardId)))
            .toList());
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
    if (acquisitionView != null && encounterSession != null) {
      encounterSession.getAcquisitionSession(ServiceLocator.getCardLibrary()).close();
    }
    if (shrineScene != null) shrineScene.cancel();
    if (fountainScene != null) {
      fountainScene.cancel();
    }
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
