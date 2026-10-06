package com.csse3200.game.components.cards;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.runtime.ResolvedCard;
import java.util.Objects;

/** Shared, input-free renderer for any authored frame and its configured placeholders. */
class FramedCardFace extends WidgetGroup {
  private final CardWidgetAssets assets;
  private final CardFrameLayout frameLayout;
  private final Image backdrop;
  private final Image artwork;
  private final Image frame;
  private final Label cost;
  private final Label name;
  private final Label description;
  private final Label type;
  private final Label target;
  private final Label upgrade;
  private ResolvedCard card;

  FramedCardFace(CardWidgetAssets assets, Drawable frameDrawable, CardFrameLayout frameLayout) {
    this.assets = Objects.requireNonNull(assets, "assets cannot be null");
    this.frameLayout = Objects.requireNonNull(frameLayout, "frame layout cannot be null");
    Objects.requireNonNull(frameDrawable, "frame drawable cannot be null");
    setTouchable(Touchable.disabled);
    setSize(CardWidget.CARD_WIDTH, CardWidget.CARD_HEIGHT);
    backdrop = new Image(assets.artworkBackdrop());
    artwork = new Image();
    artwork.setName("card-artwork");
    artwork.setScaling(CardWidget.ARTWORK_SCALING);
    frame = new Image(frameDrawable);
    frame.setName("card-frame");
    frame.setScaling(Scaling.stretch);
    cost = new FaceLabel(assets.costStyle());
    name = new FaceLabel(assets.nameStyle());
    description = new FaceLabel(assets.descriptionStyle());
    description.setWrap(true);
    type = new FaceLabel(assets.targetStyle());
    type.setName("card-type");
    target = new FaceLabel(assets.targetStyle());
    target.setName("card-target");
    upgrade = new FaceLabel(assets.upgradeStyle());
    upgrade.setText("+");
    // Transparent windows need the frame over the art; opaque windows need inset art over the
    // frame. This is a layout property, not a rarity-specific rule. Text stays above both.
    Actor[] imageLayers =
        frameLayout.artworkAboveFrame()
            ? new Actor[] {frame, backdrop, artwork}
            : new Actor[] {backdrop, artwork, frame};
    for (Actor actor : imageLayers) {
      actor.setTouchable(Touchable.disabled);
      addActor(actor);
    }
    for (Actor actor : new Actor[] {cost, name, description, type, target, upgrade}) {
      actor.setTouchable(Touchable.disabled);
      addActor(actor);
    }
  }

  /** Rebinds the complete presentation without changing the card's identity or gameplay. */
  public void setCard(ResolvedCard card) {
    this.card = Objects.requireNonNull(card, "card cannot be null");
    cost.setText(Integer.toString(card.cost()));
    name.setText(card.name());
    description.setText(card.description());
    type.setText(formatType(card.type()));
    target.setText(formatTarget(card.target()));
    Drawable drawable = assets.artwork(card.texturePath());
    artwork.setDrawable(drawable);
    artwork.setVisible(drawable != null);
    upgrade.setVisible(card.upgraded());
    invalidateHierarchy();
  }

  public ResolvedCard getCard() {
    return card;
  }

  CardFrameLayout frameLayout() {
    return frameLayout;
  }

  @Override
  public void layout() {
    float scale =
        Math.min(getWidth() / CardWidget.CARD_WIDTH, getHeight() / CardWidget.CARD_HEIGHT);
    if (scale <= 0f) {
      return;
    }
    float offsetX = (getWidth() - CardWidget.CARD_WIDTH * scale) / 2f;
    float offsetY = (getHeight() - CardWidget.CARD_HEIGHT * scale) / 2f;
    frame.setBounds(
        offsetX, offsetY, CardWidget.CARD_WIDTH * scale, CardWidget.CARD_HEIGHT * scale);
    place(backdrop, frameLayout.artwork(), scale, offsetX, offsetY);
    place(artwork, frameLayout.artwork(), scale, offsetX, offsetY);
    place(cost, frameLayout.cost(), scale, offsetX, offsetY);
    place(name, frameLayout.name(), scale, offsetX, offsetY);
    place(description, frameLayout.description(), scale, offsetX, offsetY);
    place(type, frameLayout.type(), scale, offsetX, offsetY);
    place(target, frameLayout.target(), scale, offsetX, offsetY);
    place(upgrade, frameLayout.upgrade(), scale, offsetX, offsetY);
    CardTextFitter.fit(cost, frameLayout.costScale() * scale);
    CardTextFitter.fit(name, frameLayout.nameScale() * scale);
    CardTextFitter.fit(type, frameLayout.typeScale() * scale);
    CardTextFitter.fit(target, frameLayout.targetScale() * scale);
    CardTextFitter.fit(upgrade, 0.60f * scale);
    // One wrapped Label and one fitted scale for the entire paragraph, never per-line scaling.
    CardTextFitter.fit(description, frameLayout.descriptionScale() * scale);
    for (Label label : new Label[] {cost, name, description, type, target, upgrade}) {
      label.validate();
    }
  }

  @Override
  public float getPrefWidth() {
    return CardWidget.CARD_WIDTH;
  }

  @Override
  public float getPrefHeight() {
    return CardWidget.CARD_HEIGHT;
  }

  String displayedCost() {
    return cost.getText().toString();
  }

  String displayedName() {
    return name.getText().toString();
  }

  String displayedDescription() {
    return description.getText().toString();
  }

  String displayedTarget() {
    return target.getText().toString();
  }

  String displayedType() {
    return type.getText().toString();
  }

  Drawable displayedArtwork() {
    return artwork.getDrawable();
  }

  Drawable displayedFrame() {
    return frame.getDrawable();
  }

  boolean isArtworkVisible() {
    return artwork.isVisible();
  }

  boolean displaysUpgradeMarker() {
    return upgrade.isVisible();
  }

  float displayedNameScale() {
    return name.getFontScaleX();
  }

  static String formatTarget(TargetType target) {
    return switch (target) {
      case SELF -> "Self";
      case SINGLE_ENEMY -> "One Enemy";
      case ALL_ENEMIES -> "All Enemies";
    };
  }

  static String formatType(CardType type) {
    return switch (type) {
      case ATTACK -> "Attack";
      case SKILL -> "Skill";
      case POWER -> "Power";
      case STATUS -> "Status";
      case CURSE -> "Curse";
    };
  }

  private static void place(
      Actor actor, CardFrameLayout.Bounds bounds, float scale, float x, float y) {
    actor.setBounds(
        x + bounds.x() * scale,
        y + bounds.y() * scale,
        bounds.width() * scale,
        bounds.height() * scale);
  }

  private static final class FaceLabel extends Label {
    private FaceLabel(LabelStyle style) {
      super("", style);
      setAlignment(Align.center);
      // Keep fractional glyph geometry centered at every consumer's scale. This is local to the
      // label cache: the shared skin font and other screens are not modified.
      getBitmapFontCache().setUseIntegerPositions(false);
    }
  }
}
