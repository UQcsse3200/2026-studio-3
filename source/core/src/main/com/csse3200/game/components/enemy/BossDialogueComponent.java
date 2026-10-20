package com.csse3200.game.components.enemy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.components.enemy.BossDialogueConfig.Dialogue;
import com.csse3200.game.components.enemy.EnemyAI.BossAI;
import com.csse3200.game.components.enemy.EnemyAI.BossAI.BossPhase;
import com.csse3200.game.events.listeners.EventListener1;
import com.csse3200.game.services.ResourceService;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Supplier;

/** Displays the supplied illustrated dialogue at the Boss opening and phase transitions. */
public class BossDialogueComponent extends Component {
  private static final float IMAGE_WIDTH = 582f;
  private static final float IMAGE_HEIGHT = 156f;
  private static final float EDGE_MARGIN = 20f;
  private static final float BOSS_GAP = 16f;

  private final Stage stage;
  private final BossAI bossAI;
  private final BossDialogueConfig config;
  private final BattleController battleController;
  private final Supplier<Rectangle> bossBounds;
  private final DialogueFramesProvider framesProvider;
  private final Group overlay = new Group();
  private final Image dialogueImage = new Image();
  private final EnumSet<BossPhase> shownPhases = EnumSet.noneOf(BossPhase.class);
  private final EventListener1<BossPhase> phaseListener = this::showPhase;

  private BossPhase displayedPhase;
  private Drawable[] animationFrames = new Drawable[0];
  private float animationTime;
  private float frameDuration;
  private int displayedFrameIndex;

  @FunctionalInterface
  interface DialogueFramesProvider {
    Drawable[] load(String imagePath, int frameCount);
  }

  /** Creates a dialogue component using textures managed by the battle resource service. */
  public BossDialogueComponent(
      Stage stage,
      BossAI bossAI,
      BossDialogueConfig config,
      BattleController battleController,
      Supplier<Rectangle> bossBounds,
      ResourceService resources) {
    this(
        stage,
        bossAI,
        config,
        battleController,
        bossBounds,
        (path, frameCount) -> createFrames(resources.getAsset(path, Texture.class), frameCount));
  }

  BossDialogueComponent(
      Stage stage,
      BossAI bossAI,
      BossDialogueConfig config,
      BattleController battleController,
      Supplier<Rectangle> bossBounds,
      DialogueFramesProvider framesProvider) {
    this.stage = Objects.requireNonNull(stage, "stage cannot be null");
    this.bossAI = Objects.requireNonNull(bossAI, "bossAI cannot be null");
    this.config = Objects.requireNonNull(config, "config cannot be null");
    this.battleController =
        Objects.requireNonNull(battleController, "battleController cannot be null");
    this.bossBounds = Objects.requireNonNull(bossBounds, "bossBounds cannot be null");
    this.framesProvider = Objects.requireNonNull(framesProvider, "framesProvider cannot be null");

    overlay.setName("boss-dialogue-overlay");
    dialogueImage.setName("boss-dialogue-image");
    dialogueImage.setScaling(Scaling.fit);
    overlay.addActor(dialogueImage);
    overlay.addListener(
        new ClickListener() {
          @Override
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            dismiss();
            return true;
          }
        });
  }

  @Override
  public void create() {
    bossAI.addPhaseChangeListener(phaseListener);
    showPhase(BossPhase.PHASE_ONE);
  }

  @Override
  public void update() {
    if (isShowing()) {
      advanceAnimation(Gdx.graphics.getDeltaTime());
      resizeOverlay();
      positionNearBoss();
    }
  }

  @Override
  public void dispose() {
    bossAI.removePhaseChangeListener(phaseListener);
    dismiss();
  }

  private void showPhase(BossPhase phase) {
    if (phase == null || !shownPhases.add(phase)) {
      return;
    }

    Dialogue dialogue = config.dialogueFor(phase);
    displayedPhase = phase;
    animationFrames = framesProvider.load(dialogue.image, dialogue.frameCount);
    if (animationFrames == null || animationFrames.length != dialogue.frameCount) {
      throw new IllegalStateException("Invalid frame count for Boss dialogue " + phase);
    }
    animationTime = 0f;
    frameDuration = dialogue.durationSeconds / dialogue.frameCount;
    displayedFrameIndex = 0;
    dialogueImage.setDrawable(animationFrames[displayedFrameIndex]);
    dialogueImage.setUserObject(dialogue.text);
    dialogueImage.setSize(IMAGE_WIDTH, IMAGE_HEIGHT);
    overlay.clearActions();
    resizeOverlay();
    positionNearBoss();
    if (overlay.getStage() == null) {
      stage.addActor(overlay);
    }
    overlay.toFront();
    battleController.setCardPlayBlocked(true);
    overlay.addAction(
        Actions.sequence(Actions.delay(dialogue.durationSeconds), Actions.run(this::dismiss)));
  }

  /** Closes the visible dialogue and restores card input. */
  public void dismiss() {
    if (!isShowing()) {
      return;
    }
    overlay.clearActions();
    overlay.remove();
    displayedPhase = null;
    animationFrames = new Drawable[0];
    animationTime = 0f;
    displayedFrameIndex = 0;
    battleController.setCardPlayBlocked(false);
  }

  void advanceAnimation(float delta) {
    if (!isShowing() || animationFrames.length == 0 || delta <= 0f) {
      return;
    }

    animationTime += delta;
    int nextFrame = Math.min((int) (animationTime / frameDuration), animationFrames.length - 1);
    if (nextFrame != displayedFrameIndex) {
      displayedFrameIndex = nextFrame;
      dialogueImage.setDrawable(animationFrames[displayedFrameIndex]);
    }
  }

  private static Drawable[] createFrames(Texture texture, int frameCount) {
    TextureRegion[][] regions = TextureRegion.split(texture, (int) IMAGE_WIDTH, (int) IMAGE_HEIGHT);
    Drawable[] frames = new Drawable[frameCount];
    int index = 0;
    for (TextureRegion[] row : regions) {
      for (TextureRegion region : row) {
        if (index == frameCount) {
          return frames;
        }
        frames[index++] = new TextureRegionDrawable(region);
      }
    }
    if (index != frameCount) {
      throw new IllegalStateException(
          "Dialogue sprite sheet contains " + index + " frames; expected " + frameCount);
    }
    return frames;
  }

  private void resizeOverlay() {
    overlay.setBounds(0f, 0f, stage.getWidth(), stage.getHeight());
  }

  private void positionNearBoss() {
    Rectangle bounds = bossBounds.get();
    float x = (stage.getWidth() - IMAGE_WIDTH) / 2f;
    float y = (stage.getHeight() - IMAGE_HEIGHT) / 2f;

    if (bounds != null) {
      x = bounds.x + (bounds.width - IMAGE_WIDTH) / 2f;
      y = bounds.y + bounds.height + BOSS_GAP;
      if (y + IMAGE_HEIGHT > stage.getHeight() - EDGE_MARGIN) {
        y = bounds.y - IMAGE_HEIGHT - BOSS_GAP;
      }
    }

    dialogueImage.setPosition(
        MathUtils.clamp(
            x, EDGE_MARGIN, Math.max(EDGE_MARGIN, stage.getWidth() - IMAGE_WIDTH - EDGE_MARGIN)),
        MathUtils.clamp(
            y, EDGE_MARGIN, Math.max(EDGE_MARGIN, stage.getHeight() - IMAGE_HEIGHT - EDGE_MARGIN)));
  }

  boolean isShowing() {
    return overlay.getStage() != null;
  }

  BossPhase getDisplayedPhase() {
    return displayedPhase;
  }

  int getDisplayedFrameIndex() {
    return displayedFrameIndex;
  }

  String getDisplayedText() {
    Object text = dialogueImage.getUserObject();
    return text == null ? null : text.toString();
  }

  Group getOverlay() {
    return overlay;
  }
}
