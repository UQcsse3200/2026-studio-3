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

  static final CardFrameLayout UNCOMMON =
      standardFrame(
          CardWidgetAssets.UNCOMMON_FRAME_TEXTURE,
          new Bounds(23f, 39f, 179f, 105f),
          new Bounds(75f, 153.5f, 75f, 17f),
          new Bounds(80f, 17f, 65f, 16f));

  static final CardFrameLayout RARE =
      standardFrame(
          CardWidgetAssets.RARE_FRAME_TEXTURE,
          COMMON.description(),
          COMMON.type(),
          COMMON.target());

  // These frames share the orb, nameplate and opaque artwork window. Their ribbons and paper
  // panels may differ slightly, so each rarity retains its own placeholder geometry.
  private static CardFrameLayout standardFrame(
      String texturePath, Bounds description, Bounds type, Bounds target) {
    return new CardFrameLayout(
        texturePath,
        COMMON.artwork(),
        COMMON.cost(),
        COMMON.name(),
        description,
        type,
        target,
        COMMON.upgrade(),
        COMMON.costScale(),
        COMMON.nameScale(),
        COMMON.descriptionScale(),
        COMMON.typeScale(),
        COMMON.targetScale(),
        true);
  }

  record Bounds(float x, float y, float width, float height) {}
}
