package com.csse3200.game.tutorial;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Tutorial-only final pass: one dim layer, real highlighted targets and floating prompts. */
public final class BattleTutorialGuidanceView implements BattleTutorialView {
  private static final Color GOLD = new Color(0.92f, 0.73f, 0.33f, 1);

  enum Placement {
    ABOVE,
    RIGHT,
    LEFT,
    BELOW
  }

  private final Stage stage;
  private final Supplier<List<Actor>> hand;
  private final Supplier<Actor> demonstration, inventory, endTurn;
  private Supplier<Actor> itemInventory = () -> null;
  private Supplier<Actor> energy = () -> null;
  private Supplier<Label> health = () -> null;
  private Supplier<Actor> enemyStats = () -> null;
  private Supplier<Actor> enemyArmour = () -> null;
  private Supplier<Rectangle> energyBounds;

  public void setDetailedTargets(Supplier<Rectangle> energyBounds, Supplier<Actor> enemyArmour) {
    this.energyBounds = energyBounds;
    this.enemyArmour = enemyArmour;
  }

  /** Bind current UI components, not presentation strings or screenshot coordinates. */
  public void setStatTargets(
      Supplier<Actor> itemInventory,
      Supplier<Actor> energy,
      Supplier<Label> health,
      Supplier<Actor> enemyStats) {
    this.itemInventory = Objects.requireNonNull(itemInventory);
    this.energy = Objects.requireNonNull(energy);
    this.health = Objects.requireNonNull(health);
    this.enemyStats = Objects.requireNonNull(enemyStats);
  }

  private Supplier<List<Rectangle>> enemies = List::of;
  private Consumer<Batch> enemyRenderer = batch -> {};
  private Supplier<Actor> dragActor = () -> null;
  private final List<Actor> brightActors = new ArrayList<>();
  private Actor raisedCard;
  private int originalCardZ;
  private final List<Rectangle> highlights = new ArrayList<>();
  private final Table narration = new Table();
  private final Label message, advanceHint;
  private final TextButton exit;
  private final TextureRegion pixel;
  private boolean finalPass, attached, tapArmed;
  private float pulse;
  private Runnable continueAction, exitAction;
  private BattleTutorialPrompt prompt;
  private Rectangle dragPrompt;
  private final Label damageNumber;
  private float damageElapsed = 1;
  private final Group overlay =
      new Group() {
        @Override
        public void draw(Batch batch, float alpha) {
          if (finalPass) super.draw(batch, alpha);
        }
      };
  private final InputListener taps =
      new InputListener() {
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
          if (prompt == null || isWithin(event.getTarget(), exit)) return false;
          if (prompt.step() == BattleTutorialController.Step.FREE_PLAY) return false;
          if (prompt.step() == BattleTutorialController.Step.PLAY_A_CARD
              && isWithin(event.getTarget(), demonstration.get())) return false;
          if (prompt.step() == BattleTutorialController.Step.END_TURN
              && isWithin(event.getTarget(), endTurn.get())) return false;
          tapArmed = prompt.canContinue() && pointer == 0;
          event.stop();
          return true;
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
          if (tapArmed && pointer == 0) {
            tapArmed = false;
            if (prompt != null && prompt.canContinue() && continueAction != null)
              continueAction.run();
          }
        }
      };

  public BattleTutorialGuidanceView(
      Stage stage,
      Skin skin,
      Supplier<List<Actor>> hand,
      Supplier<Actor> demonstration,
      Supplier<Actor> inventory,
      Supplier<Actor> endTurn) {
    this.stage = Objects.requireNonNull(stage);
    this.hand = Objects.requireNonNull(hand);
    this.demonstration = Objects.requireNonNull(demonstration);
    this.inventory = Objects.requireNonNull(inventory);
    this.endTurn = Objects.requireNonNull(endTurn);
    pixel = skin.getRegion("white");
    overlay.setName("battle-tutorial-guidance");
    overlay.setTouchable(Touchable.childrenOnly);
    Actor cues =
        new Actor() {
          @Override
          public void act(float delta) {
            pulse += delta;
            damageElapsed += delta;
            damageNumber.setVisible(damageElapsed < 0.75f);
            layoutCues();
          }

          @Override
          public void draw(Batch batch, float alpha) {
            drawHighlights(batch);
          }
        };
    cues.setTouchable(Touchable.disabled);
    overlay.addActor(cues);
    Label.LabelStyle labelStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    labelStyle.fontColor = new Color(0.96f, 0.91f, 0.78f, 1);
    message = new Label("", labelStyle);
    damageNumber = new Label("", labelStyle);
    damageNumber.setTouchable(Touchable.disabled);
    damageNumber.setFontScale(1.5f);
    damageNumber.setColor(1, 0.85f, 0.65f, 1);
    damageNumber.setVisible(false);
    message.setWrap(true);
    message.setAlignment(Align.left);
    advanceHint = new Label("Tap anywhere to continue", labelStyle);
    advanceHint.setFontScale(0.8f);
    advanceHint.setColor(0.80f, 0.73f, 0.59f, 1);
    narration.setName("tutorial-floating-prompt");
    narration.setTouchable(Touchable.disabled);
    narration.setBackground(new PixelFrame());
    narration.pad(18);
    narration.add(message).growX();
    overlay.addActor(narration);
    overlay.addActor(damageNumber);
    // Reuse the original beige pixel style without modifying the shared Skin.
    exit =
        new TextButton(
            "Exit Tutorial",
            new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class)));
    exit.setName("tutorial-exit");
    TextButton.TextButtonStyle exitStyle = exit.getStyle();
    exitStyle.up = new PixelFrame();
    exitStyle.over = new PixelFrame();
    exitStyle.down = new PixelFrame();
    exitStyle.fontColor = new Color(0.96f, 0.91f, 0.78f, 1);
    exitStyle.overFontColor = new Color(1, 0.82f, 0.38f, 1);
    exitStyle.downFontColor = Color.WHITE;
    exit.getLabel().setFontScale(0.78f);
    exit.addListener(
        new ChangeListener() {
          @Override
          public void changed(ChangeEvent event, Actor actor) {
            if (exitAction != null) exitAction.run();
          }
        });
    overlay.addActor(exit);
  }

  public void setEnemyBounds(Supplier<List<Rectangle>> enemies) {
    this.enemies = Objects.requireNonNull(enemies);
  }

  /** Share battle-menu drawables without owning or disposing their skin. */
  public void setExitStyle(TextButton.TextButtonStyle style) {
    exit.setStyle(new TextButton.TextButtonStyle(style));
    exit.getLabel().setFontScale(0.78f);
  }

  public void setEnemyRenderer(Consumer<Batch> renderer) {
    enemyRenderer = Objects.requireNonNull(renderer);
  }

  public void setDragActor(Supplier<Actor> dragActor) {
    this.dragActor = Objects.requireNonNull(dragActor);
  }

  @Override
  public void showResolvedDamage(int amount) {
    if (amount <= 0) return;
    damageElapsed = 0;
    damageNumber.setText("-" + amount);
    damageNumber.pack();
    damageNumber.setVisible(true);
  }

  @Override
  public boolean areTransientEffectsFinished() {
    return damageElapsed >= 0.75f;
  }

  @Override
  public void bindActions(Runnable continueAction, Runnable exitAction) {
    this.continueAction = Objects.requireNonNull(continueAction);
    this.exitAction = Objects.requireNonNull(exitAction);
    if (!attached) {
      stage.addActor(overlay);
      stage.getRoot().addCaptureListener(taps);
      attached = true;
    }
  }

  @Override
  public void show(BattleTutorialPrompt prompt) {
    if (!attached) throw new IllegalStateException("Bind tutorial actions first");
    this.prompt = Objects.requireNonNull(prompt);
    restoreCardOrder();
    if (prompt.step() == BattleTutorialController.Step.PLAY_A_CARD
        || prompt.step() == BattleTutorialController.Step.USED_CARD) {
      raisedCard = demonstration.get();
      if (raisedCard != null) {
        originalCardZ = raisedCard.getZIndex();
        raisedCard.toFront();
      }
    }
    tapArmed = false;
    dragPrompt = null;
    if (prompt.step() == BattleTutorialController.Step.CANCELLED
        || prompt.step() == BattleTutorialController.Step.BATTLE_ENDED) {
      clear();
      return;
    }
    message.setText(prompt.step() == BattleTutorialController.Step.FREE_PLAY ? "" : prompt.text());
    message.setFontScale(prompt.step() == BattleTutorialController.Step.INTRO ? 1.5f : 1f);
    narration.clearChildren();
    narration.add(message).growX();
    if (prompt.step() == BattleTutorialController.Step.INTRO
        || prompt.step() == BattleTutorialController.Step.HAND) {
      narration.row();
      narration.add(advanceHint).padTop(12).left();
    }
    narration.setVisible(
        prompt.step() != BattleTutorialController.Step.FREE_PLAY && !prompt.text().isBlank());
    layoutCues();
  }

  /** Called after Stage.draw; card hover/drag toFront cannot obscure the prompt or Exit. */
  public void renderAboveBattle() {
    if (!attached || prompt == null) return;
    layoutCues();
    overlay.toFront();
    Batch batch = stage.getBatch();
    batch.setProjectionMatrix(stage.getCamera().combined);
    Color original = new Color(batch.getColor());
    batch.setColor(Color.WHITE);
    batch.begin();
    finalPass = true;
    try {
      overlay.draw(batch, 1);
    } finally {
      finalPass = false;
      batch.end();
      batch.setColor(original);
    }
  }

  private void layoutCues() {
    float w = stage.getWidth(), h = stage.getHeight();
    overlay.setSize(w, h);
    List<Rectangle> enemyBoxes = enemies.get();
    if (!enemyBoxes.isEmpty()) {
      Rectangle enemy = enemyBoxes.get(0);
      damageNumber.setPosition(
          enemy.x + enemy.width / 2 - damageNumber.getWidth() / 2,
          enemy.y + enemy.height + 10 + Math.min(0.75f, damageElapsed) * 40);
    }
    exit.setBounds(Math.max(12, w - 174), h - 54, 158, 38);
    highlights.clear();
    brightActors.clear();
    if (prompt == null) return;
    switch (prompt.highlight()) {
      case HAND -> {
        if (prompt.step() == BattleTutorialController.Step.PLAY_A_CARD) {
          Actor dragging = dragActor.get();
          add(dragging != null ? dragging : demonstration.get());
          highlights.addAll(enemies.get());
          Actor stats = findEnemyStats();
          if (stats != null) brightActors.add(stats);
        } else {
          Rectangle union = null;
          for (Actor actor : hand.get()) {
            if (actor == null || actor.getStage() != stage || !actor.isVisible()) continue;
            brightActors.add(actor);
            if (union == null) union = bounds(actor);
            else union.merge(bounds(actor));
          }
          if (union != null) {
            union.x -= 18;
            union.y -= 18;
            union.width += 36;
            union.height += 36;
            highlights.add(union);
          }
        }
      }
      case CARD_COST -> {
        if (demonstration.get() instanceof Group group)
          descendants(group).stream()
              .filter(a -> a instanceof Label l && l.getText().toString().matches("\\d+"))
              .findFirst()
              .ifPresent(this::add);
      }
      case USED_CARD -> {
        add(demonstration.get());
        // Compare real available and inactive appearances; only the used card gets a frame.
        for (Actor card : hand.get()) {
          if (card != null
              && card.getStage() == stage
              && card.isVisible()
              && !brightActors.contains(card)) brightActors.add(card);
        }
      }
      case CARD_INVENTORY, DRAW_PILE -> add(inventory.get());
      case ITEM_INVENTORY -> add(itemInventory.get());
      case END_TURN -> add(endTurn.get());
      case ENERGY -> {
        add(energy.get());
        if (energyBounds != null && !highlights.isEmpty()) highlights.set(0, energyBounds.get());
      }
      case HEALTH -> add(health.get() == null ? null : health.get().getParent());
      case ENEMIES, ENEMY_STATS -> add(enemyStats.get());
      case ENEMY_ARMOUR -> add(enemyArmour.get());
      case STATUS_EFFECTS -> add(findStat("Energy:", true));
      case NONE -> {}
    }
    message.setFontScale(Math.min(1, Math.max(0.65f, w / 900f)));
    float textWidth = 0;
    for (String line : prompt.text().split("\n")) {
      textWidth =
          Math.max(
              textWidth,
              new com.badlogic.gdx.graphics.g2d.GlyphLayout(message.getStyle().font, line).width
                  * message.getFontScaleX());
    }
    float width =
        prompt.step() == BattleTutorialController.Step.INTRO
            ? Math.min(w - 64, w * 0.65f)
            : Math.min(w - 32, Math.min(440, Math.max(220, textWidth + 40)));
    narration.setWidth(width);
    narration.invalidateHierarchy();
    float height =
        Math.min(
            h - 90,
            prompt.step() == BattleTutorialController.Step.INTRO
                ? Math.max(h * 0.23f, narration.getPrefHeight())
                : narration.getPrefHeight());
    Rectangle target =
        highlights.isEmpty() ? new Rectangle(w / 2, h * 0.6f, 0, 0) : highlights.get(0);
    Placement preferred =
        switch (prompt.highlight()) {
          case CARD_INVENTORY, ITEM_INVENTORY, END_TURN, ENERGY, HEALTH -> Placement.RIGHT;
          case ENEMY_STATS, ENEMY_ARMOUR, ENEMIES -> Placement.LEFT;
          default -> Placement.ABOVE;
        };
    Rectangle box;
    if (prompt.step() == BattleTutorialController.Step.PLAY_A_CARD) {
      if (dragPrompt == null) dragPrompt = new Rectangle((w - width) / 2, h * 0.72f, width, height);
      box = clamp(new Rectangle(dragPrompt), w, h);
    } else if (prompt.step() == BattleTutorialController.Step.INTRO) {
      box = clamp(new Rectangle((w - width) / 2, h * 0.6f, width, height), w, h);
    } else {
      box = place(target, width, height, w, h, preferred);
      if (prompt.highlight() == BattleTutorialPrompt.HighlightTarget.CARD_INVENTORY
          || prompt.highlight() == BattleTutorialPrompt.HighlightTarget.ITEM_INVENTORY
          || prompt.highlight() == BattleTutorialPrompt.HighlightTarget.END_TURN) {
        // Keep the prompt below the button's top edge, away from the game title.
        box.y = Math.min(box.y, target.y + target.height - box.height);
        box = clamp(box, w, h);
      }
    }
    narration.setBounds(box.x, box.y, box.width, box.height);
    narration.validate();
  }

  static Rectangle place(
      Rectangle target, float width, float height, float w, float h, Placement preferred) {
    List<Placement> order = new ArrayList<>(List.of(preferred));
    for (Placement direction : Placement.values())
      if (!order.contains(direction)) order.add(direction);
    Rectangle first = null;
    for (Placement direction : order) {
      float x = target.x + (target.width - width) / 2, y = target.y + (target.height - height) / 2;
      switch (direction) {
        case ABOVE -> y = target.y + target.height + 26;
        case BELOW -> y = target.y - height - 26;
        case RIGHT -> x = target.x + target.width + 26;
        case LEFT -> x = target.x - width - 26;
      }
      Rectangle candidate = new Rectangle(x, y, width, height);
      if (first == null) first = candidate;
      Rectangle adjusted = clamp(new Rectangle(candidate), w, h);
      if (!adjusted.overlaps(target)) return adjusted;
    }
    return clamp(first, w, h);
  }

  private static Rectangle clamp(Rectangle box, float w, float h) {
    box.width = Math.min(box.width, w - 32);
    box.height = Math.min(box.height, h - 80);
    box.x = Math.max(16, Math.min(box.x, w - 16 - box.width));
    box.y = Math.max(16, Math.min(box.y, h - 64 - box.height));
    return box;
  }

  private Actor findEnemyStats() {
    for (Actor actor : descendants(stage.getRoot()))
      if (actor instanceof Label l
          && l.getText().toString().startsWith("Armour:")
          && !isWithin(actor, overlay)) return actor.getParent();
    return null;
  }

  private Actor findStat(String prefix, boolean parent) {
    for (Actor actor : descendants(stage.getRoot())) {
      if (actor instanceof Label label
          && !isWithin(actor, overlay)
          && label.getText().toString().startsWith(prefix)) {
        if (prefix.equals("Health:")
            && actor.getParent() instanceof Group group
            && descendants(group).stream()
                .noneMatch(
                    a -> a instanceof Label l && l.getText().toString().startsWith("Energy:")))
          continue;
        return parent ? actor.getParent() : actor;
      }
    }
    return null;
  }

  public boolean isPlayerHealthDisplayed(int health) {
    Actor actor = this.health.get();
    if (!(actor instanceof Label label)) return false;
    String[] values = label.getText().toString().trim().split("\\D+");
    return values.length > 0 && values[0].equals(Integer.toString(health));
  }

  /** Convert world-stat screen anchors to stage coordinates only in the tutorial battle. */
  public void alignWorldStatsToViewport() {
    java.util.Set<Group> adjusted = new java.util.HashSet<>();
    List<Group> groups = new ArrayList<>();
    Label healthLabel = health.get();
    if (healthLabel != null && healthLabel.getParent() != null)
      groups.add(healthLabel.getParent().getParent());
    if (enemyStats.get() instanceof Group stats) groups.add(stats.getParent());
    for (Group group : groups) {
      if (group == null || !adjusted.add(group)) continue;
      Vector2 anchor =
          new Vector2(
              group.getX() + group.getWidth() / 2f,
              com.badlogic.gdx.Gdx.graphics.getHeight() - group.getY());
      stage.screenToStageCoordinates(anchor);
      group.setPosition(anchor.x - group.getWidth() / 2f, anchor.y);
    }
  }

  private static List<Actor> descendants(Group group) {
    List<Actor> result = new ArrayList<>();
    for (Actor actor : group.getChildren()) {
      result.add(actor);
      if (actor instanceof Group nested) result.addAll(descendants(nested));
    }
    return result;
  }

  private void add(Actor actor) {
    if (actor != null && actor.getStage() == stage && actor.isVisible()) {
      highlights.add(bounds(actor));
      brightActors.add(actor);
    }
  }

  static Rectangle bounds(Actor actor) {
    float minX = Float.MAX_VALUE,
        minY = Float.MAX_VALUE,
        maxX = -Float.MAX_VALUE,
        maxY = -Float.MAX_VALUE;
    for (Vector2 corner :
        List.of(
            new Vector2(),
            new Vector2(actor.getWidth(), 0),
            new Vector2(0, actor.getHeight()),
            new Vector2(actor.getWidth(), actor.getHeight()))) {
      actor.localToStageCoordinates(corner);
      minX = Math.min(minX, corner.x);
      minY = Math.min(minY, corner.y);
      maxX = Math.max(maxX, corner.x);
      maxY = Math.max(maxY, corner.y);
    }
    return new Rectangle(minX - 6, minY - 6, maxX - minX + 12, maxY - minY + 12);
  }

  private static boolean isWithin(Actor actor, Actor ancestor) {
    return ancestor != null
        && actor != null
        && (actor == ancestor || actor.isDescendantOf(ancestor));
  }

  private void drawHighlights(Batch batch) {
    if (prompt == null || prompt.step() == BattleTutorialController.Step.FREE_PLAY) return;
    Color original = new Color(batch.getColor());
    // During actual resolution leave damage visuals unobscured, retaining only the Exit button.
    if (prompt.step() != BattleTutorialController.Step.CARD_ANIMATION
        && prompt.step() != BattleTutorialController.Step.ENEMY_ANIMATION) drawDim(batch);
    brightActors.stream()
        .sorted(java.util.Comparator.comparingInt(Actor::getZIndex))
        .forEach(actor -> redrawTarget(batch, actor));
    if (prompt.step() == BattleTutorialController.Step.PLAY_A_CARD) enemyRenderer.accept(batch);
    for (Rectangle r : highlights) {
      batch.setColor(GOLD.r, GOLD.g, GOLD.b, 0.15f + 0.05f * (float) Math.sin(pulse * 3));
      outline(batch, r, 6);
      batch.setColor(GOLD);
      outline(batch, r, 2);
    }
    if (narration.isVisible() && !highlights.isEmpty()) {
      Rectangle target = highlights.get(0);
      Vector2 start =
          new Vector2(
              narration.getX() + narration.getWidth() / 2,
              narration.getY() + narration.getHeight() / 2);
      Vector2 finish =
          new Vector2(
              Math.max(target.x, Math.min(start.x, target.x + target.width)),
              Math.max(target.y, Math.min(start.y, target.y + target.height)));
      Vector2 direction = new Vector2(finish).sub(start).nor();
      Vector2 tip = new Vector2(finish).sub(new Vector2(direction).scl(10));
      for (int i = 0; i < 10; i++) {
        Vector2 point = new Vector2(tip).sub(new Vector2(direction).scl(i * 2));
        rect(batch, point.x, point.y, 3, 3);
      }
      for (int i = 0; i < 5; i++) {
        Vector2 base = new Vector2(tip).sub(new Vector2(direction).scl(i * 2));
        rect(batch, base.x - direction.y * i * 2, base.y + direction.x * i * 2, 3, 3);
        rect(batch, base.x + direction.y * i * 2, base.y - direction.x * i * 2, 3, 3);
      }
      if (prompt.step() == BattleTutorialController.Step.PLAY_A_CARD && highlights.size() > 1)
        drawDragArc(batch, target, highlights.get(1));
    }
    batch.setColor(original);
  }

  private void drawDragArc(Batch batch, Rectangle card, Rectangle enemy) {
    float sx = card.x + card.width / 2,
        sy = card.y + card.height,
        ex = enemy.x + enemy.width / 2,
        ey = enemy.y + enemy.height / 2;
    for (int i = 0; i <= 35; i++) {
      float t = i / 35f;
      rect(batch, sx + (ex - sx) * t, sy + (ey - sy) * t + 360 * t * (1 - t), 4, 4);
    }
    rect(batch, ex - 10, ey + 4, 14, 3);
    rect(batch, ex, ey - 8, 3, 15);
  }

  private void drawDim(Batch batch) {
    // A single full-screen quad. Actual targets are drawn above it, not rectangular holes.
    batch.setColor(0, 0, 0, dimAlpha());
    rect(batch, 0, 0, stage.getWidth(), stage.getHeight());
    batch.setColor(GOLD);
  }

  float dimAlpha() {
    return prompt != null && prompt.step() == BattleTutorialController.Step.USED_CARD
        ? 0.28f
        : 0.52f;
  }

  private void redrawTarget(Batch batch, Actor actor) {
    com.badlogic.gdx.math.Matrix4 previous =
        new com.badlogic.gdx.math.Matrix4(batch.getTransformMatrix());
    Group parent = actor.getParent();
    if (parent == null) return;
    Vector2 origin = parent.localToStageCoordinates(new Vector2());
    Vector2 xAxis = parent.localToStageCoordinates(new Vector2(1, 0)).sub(origin);
    Vector2 yAxis = parent.localToStageCoordinates(new Vector2(0, 1)).sub(origin);
    com.badlogic.gdx.math.Matrix4 transform = new com.badlogic.gdx.math.Matrix4();
    transform.val[com.badlogic.gdx.math.Matrix4.M00] = xAxis.x;
    transform.val[com.badlogic.gdx.math.Matrix4.M10] = xAxis.y;
    transform.val[com.badlogic.gdx.math.Matrix4.M01] = yAxis.x;
    transform.val[com.badlogic.gdx.math.Matrix4.M11] = yAxis.y;
    transform.val[com.badlogic.gdx.math.Matrix4.M03] = origin.x;
    transform.val[com.badlogic.gdx.math.Matrix4.M13] = origin.y;
    float alpha = 1;
    for (Group ancestor = parent; ancestor != null; ancestor = ancestor.getParent())
      alpha *= ancestor.getColor().a;
    batch.setTransformMatrix(transform);
    batch.setColor(Color.WHITE);
    actor.draw(batch, alpha);
    batch.setTransformMatrix(previous);
    batch.setColor(GOLD);
  }

  private void restoreCardOrder() {
    if (raisedCard != null && raisedCard.getParent() != null) raisedCard.setZIndex(originalCardZ);
    raisedCard = null;
  }

  List<Rectangle> highlightBounds() {
    layoutCues();
    return highlights.stream().map(Rectangle::new).toList();
  }

  Rectangle promptBounds() {
    return new Rectangle(
        narration.getX(), narration.getY(), narration.getWidth(), narration.getHeight());
  }

  private void outline(Batch batch, Rectangle r, float thickness) {
    rect(batch, r.x, r.y, r.width, thickness);
    rect(batch, r.x, r.y + r.height - thickness, r.width, thickness);
    rect(batch, r.x, r.y, thickness, r.height);
    rect(batch, r.x + r.width - thickness, r.y, thickness, r.height);
  }

  private void rect(Batch batch, float x, float y, float width, float height) {
    if (width > 0 && height > 0) batch.draw(pixel, x, y, width, height);
  }

  private final class PixelFrame extends BaseDrawable {
    @Override
    public void draw(Batch batch, float x, float y, float width, float height) {
      Color original = new Color(batch.getColor());
      batch.setColor(0.025f, 0.045f, 0.075f, 0.92f);
      rect(batch, x, y, width, height);
      batch.setColor(GOLD);
      outline(batch, new Rectangle(x + 2, y + 2, width - 4, height - 4), 1);
      for (float cx : new float[] {x + 5, x + width - 9})
        for (float cy : new float[] {y + 5, y + height - 9}) rect(batch, cx, cy, 3, 3);
      batch.setColor(original);
    }
  }

  @Override
  public void clear() {
    restoreCardOrder();
    stage.getRoot().removeCaptureListener(taps);
    overlay.remove();
    attached = false;
    tapArmed = false;
    prompt = null;
    highlights.clear();
    brightActors.clear();
    dragPrompt = null;
    continueAction = null;
    exitAction = null;
    damageElapsed = 1;
    damageNumber.setVisible(false);
  }
}
