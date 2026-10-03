package com.csse3200.game.components.cards;

/**
 * Immutable placeholder geometry and typography for an authored card frame.
 *
 * <p>Coordinates use the standard 225 x 456 card footprint, with the origin at the bottom left.
 * They are independent of the PNG's resolution. A new frame needs only a layout here and a rarity
 * registration in {@link CardWidgetAssets}; {@link FramedCardFace} handles every screen and size.
 */
record CardFrameLayout(
    String texturePath,
    Bounds artwork,
    Bounds cost,
    Bounds name,
    Bounds description,
    Bounds type,
    Bounds target,
    Bounds upgrade,
    float costScale,
    float nameScale,
    float descriptionScale,
    float typeScale,
    float targetScale,
    boolean artworkAboveFrame) {

  static final CardFrameLayout COMMON =
      new CardFrameLayout(
          CardWidgetAssets.COMMON_FRAME_TEXTURE,
          // The PNG's window is opaque black. Keep overlaid art inside its clear rectangular
          // interior, away from the cost orb, rounded corners and target ribbon.
          new Bounds(16f, 174f, 193f, 222f),
          new Bounds(13f, 402f, 43f, 43f),
          new Bounds(60f, 414f, 143f, 28f),
          new Bounds(23f, 36f, 179f, 111f),
          // Type belongs on the ribbon between the art and rules; target on the bottom plate.
          new Bounds(75f, 150f, 75f, 17f),
          new Bounds(80f, 15f, 65f, 16f),
          new Bounds(203f, 420f, 10f, 14f),
          0.90f,
          0.90f,
          0.90f,
          0.72f,
          0.72f,
          true);

  // Retain the existing Library-only frame until its replacement artwork is supplied.
  static final CardFrameLayout LIBRARY_UNCOMMON =
      new CardFrameLayout(
          UncommonCardLibraryWidget.FRAME_TEXTURE,
          new Bounds(32f, 198f, 161f, 164f),
          new Bounds(13f, 369f, 36f, 29f),
          new Bounds(55f, 371f, 134f, 27f),
          new Bounds(36f, 69f, 153f, 97f),
          new Bounds(46f, 174f, 134f, 18f),
          new Bounds(79f, 47f, 68f, 16f),
          new Bounds(189f, 377f, 10f, 14f),
          0.78f,
          0.82f,
          0.72f,
          0.72f,
          0.72f,
          false);

  record Bounds(float x, float y, float width, float height) {}
}
