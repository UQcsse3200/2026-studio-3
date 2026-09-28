package com.csse3200.game.components.cards;

import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.runtime.ResolvedCard;
import java.util.Locale;
import java.util.Objects;

/**
 * Card Library-only uncommon presentation that fills the authored placeholders in {@link
 * #FRAME_TEXTURE}. It remains a touch-disabled view: selection and gameplay stay with its consumer.
 */
public final class UncommonCardLibraryWidget extends WidgetGroup {
  public static final String FRAME_TEXTURE = "images/cards/uncommon_card_frame.png";

  private static final float WIDTH = CardWidget.CARD_WIDTH;
  private static final float HEIGHT = CardWidget.CARD_HEIGHT;

  // The source frame is exactly 450 x 912, so these half-scale bounds map its authored
  // placeholders onto the standard 225 x 456 CardWidget footprint.
  private static final Bounds ARTWORK_BOUNDS = new Bounds(32f, 198f, 161f, 164f);
  private static final Bounds COST_BOUNDS = new Bounds(13f, 369f, 36f, 29f);
  private static final Bounds NAME_BOUNDS = new Bounds(55f, 371f, 134f, 27f);
  private static final Bounds META_BOUNDS = new Bounds(46f, 174f, 134f, 18f);
  private static final Bounds DESCRIPTION_BOUNDS = new Bounds(36f, 69f, 153f, 97f);
  private static final Bounds TARGET_BOUNDS = new Bounds(79f, 47f, 68f, 16f);

  private static final float MAX_NAME_SCALE = 0.82f;
  private static final float MIN_NAME_SCALE = 0.48f;

  private final CardWidgetAssets assets;
  private final Image artworkBackdrop;
  private final Image artwork;
  private final Image frame;
  private final Label costLabel;
  private final Label nameLabel;
  private final Label metaLabel;
  private final Label descriptionLabel;
  private final Label targetLabel;
  private ResolvedCard card;

  /**
   * Creates an uncommon card face using an externally owned frame and managed artwork provider.
   *
   * @param card authoritative uncommon values to display
   * @param assets shared card styles and managed artwork provider
   * @param frameDrawable authored transparent uncommon frame
   */
  public UncommonCardLibraryWidget(
      ResolvedCard card, CardWidgetAssets assets, Drawable frameDrawable) {
    this.assets = Objects.requireNonNull(assets, "assets cannot be null");
    Objects.requireNonNull(frameDrawable, "frame drawable cannot be null");

    setTouchable(Touchable.disabled);
    setSize(WIDTH, HEIGHT);

    artworkBackdrop = new Image(assets.artworkBackdrop());
    artworkBackdrop.setTouchable(Touchable.disabled);
    artwork = new Image();
    artwork.setScaling(Scaling.fit);
    artwork.setTouchable(Touchable.disabled);

    frame = new Image(frameDrawable);
    frame.setScaling(Scaling.stretch);
    frame.setTouchable(Touchable.disabled);

    costLabel = label(assets.costStyle(), Align.center);
    costLabel.setFontScale(0.78f);

    nameLabel = label(assets.nameStyle(), Align.center);
    nameLabel.setEllipsis(true);

    metaLabel = label(assets.metaStyle(), Align.center);
    metaLabel.setFontScale(0.62f);

    descriptionLabel = label(assets.descriptionStyle(), Align.center);
    descriptionLabel.setWrap(true);
    descriptionLabel.setFontScale(0.72f);

    targetLabel = label(assets.metaStyle(), Align.center);
    targetLabel.setFontScale(0.48f);

    // Artwork sits behind the transparent opening; the authored frame and all text occupy the
    // layers above it.
    addActor(artworkBackdrop);
    addActor(artwork);
    addActor(frame);
    addActor(costLabel);
    addActor(nameLabel);
    addActor(metaLabel);
    addActor(descriptionLabel);
    addActor(targetLabel);

    setCard(card);
  }

  /** Refreshes every authored placeholder from one authoritative uncommon snapshot. */
  public void setCard(ResolvedCard card) {
    ResolvedCard next = Objects.requireNonNull(card, "card cannot be null");
    if (next.rarity() != Rarity.UNCOMMON) {
      throw new IllegalArgumentException("uncommon frame requires an UNCOMMON card");
    }
    this.card = next;

    costLabel.setText(Integer.toString(next.cost()));
    nameLabel.setText(next.name());
    fitName(next.name());
    metaLabel.setText(formatEnum(next.type().name()) + "  |  " + formatEnum(next.rarity().name()));
    descriptionLabel.setText(next.description());
    targetLabel.setText(formatTarget(next));

    Drawable artworkDrawable = assets.artwork(next.texturePath());
    artwork.setDrawable(artworkDrawable);
    artwork.setVisible(artworkDrawable != null);
    invalidateHierarchy();
  }

  /** Returns the immutable resolved snapshot currently displayed. */
  public ResolvedCard getCard() {
    return card;
  }

  @Override
  public void layout() {
    artworkBackdrop.setBounds(
        ARTWORK_BOUNDS.x(), ARTWORK_BOUNDS.y(), ARTWORK_BOUNDS.width(), ARTWORK_BOUNDS.height());
    artwork.setBounds(
        ARTWORK_BOUNDS.x(), ARTWORK_BOUNDS.y(), ARTWORK_BOUNDS.width(), ARTWORK_BOUNDS.height());
    frame.setBounds(0f, 0f, getWidth(), getHeight());
    setBounds(costLabel, COST_BOUNDS);
    setBounds(nameLabel, NAME_BOUNDS);
    setBounds(metaLabel, META_BOUNDS);
    setBounds(descriptionLabel, DESCRIPTION_BOUNDS);
    setBounds(targetLabel, TARGET_BOUNDS);
  }

  @Override
  public float getPrefWidth() {
    return WIDTH;
  }

  @Override
  public float getPrefHeight() {
    return HEIGHT;
  }

  String displayedCost() {
    return costLabel.getText().toString();
  }

  String displayedName() {
    return nameLabel.getText().toString();
  }

  String displayedMeta() {
    return metaLabel.getText().toString();
  }

  String displayedDescription() {
    return descriptionLabel.getText().toString();
  }

  String displayedTarget() {
    return targetLabel.getText().toString();
  }

  Drawable displayedArtwork() {
    return artwork.getDrawable();
  }

  private static Label label(Label.LabelStyle style, int alignment) {
    Label label = new Label("", style);
    label.setAlignment(alignment);
    label.setTouchable(Touchable.disabled);
    return label;
  }

  private void fitName(String name) {
    GlyphLayout layout = new GlyphLayout(nameLabel.getStyle().font, name);
    float scale =
        layout.width == 0f
            ? MAX_NAME_SCALE
            : Math.min(MAX_NAME_SCALE, NAME_BOUNDS.width() / layout.width);
    nameLabel.setFontScale(Math.max(MIN_NAME_SCALE, scale));
  }

  private static String formatTarget(ResolvedCard card) {
    return switch (card.target()) {
      case SELF -> "Self";
      case SINGLE_ENEMY -> "One Enemy";
      case ALL_ENEMIES -> "All Enemies";
    };
  }

  private static String formatEnum(String value) {
    String lowerCase = value.toLowerCase(Locale.ROOT).replace('_', ' ');
    return Character.toUpperCase(lowerCase.charAt(0)) + lowerCase.substring(1);
  }

  private static void setBounds(Label label, Bounds bounds) {
    label.setBounds(bounds.x(), bounds.y(), bounds.width(), bounds.height());
  }

  private record Bounds(float x, float y, float width, float height) {}
}
