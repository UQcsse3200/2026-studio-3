package com.csse3200.game.components.cards;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.csse3200.game.cards.CardConfigLoader;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.cards.runtime.ResolvedCard;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class UncommonCardLibraryWidgetTest {
  private Skin skin;
  private Drawable artwork;
  private CardWidgetAssets assets;

  @BeforeEach
  void setUp() {
    skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    artwork = skin.newDrawable("white", Color.NAVY);
    assets = CardWidgetAssets.fromSkin(skin, path -> artwork);
  }

  @AfterEach
  void tearDown() {
    skin.dispose();
  }

  @Test
  void shouldFillEveryAuthoredPlaceholderFromResolvedCard() {
    ResolvedCard poisonDagger =
        card(
            "Poison Dagger",
            "Deal 4 damage. Apply 3 Poison for 3 turns.",
            1,
            CardType.ATTACK,
            Rarity.UNCOMMON,
            TargetType.SINGLE_ENEMY);
    UncommonCardLibraryWidget widget =
        new UncommonCardLibraryWidget(poisonDagger, assets, skin.newDrawable("white", Color.SLATE));
    widget.validate();

    assertAll(
        () -> assertEquals("1", widget.displayedCost()),
        () -> assertEquals("Poison Dagger", widget.displayedName()),
        () -> assertEquals("Attack  |  Uncommon", widget.displayedMeta()),
        () -> assertEquals(poisonDagger.description(), widget.displayedDescription()),
        () -> assertEquals("One Enemy", widget.displayedTarget()),
        () -> assertSame(artwork, widget.displayedArtwork()),
        () -> assertSame(poisonDagger, widget.getCard()),
        () -> assertEquals(CardWidget.CARD_WIDTH, widget.getPrefWidth()),
        () -> assertEquals(CardWidget.CARD_HEIGHT, widget.getPrefHeight()));

    // Backdrop + artwork + frame, followed by one actor for each of the five text placeholders.
    assertEquals(8, widget.getChildren().size);
    for (Actor placeholder : widget.getChildren()) {
      assertAll(
          () -> assertTrue(placeholder.getWidth() > 0f),
          () -> assertTrue(placeholder.getHeight() > 0f));
    }
  }

  @Test
  void shouldRefreshAllFieldsForAnotherUncommonCard() {
    ResolvedCard first =
        card(
            "Expose",
            "Apply 2 Vulnerable.",
            1,
            CardType.SKILL,
            Rarity.UNCOMMON,
            TargetType.ALL_ENEMIES);
    UncommonCardLibraryWidget widget =
        new UncommonCardLibraryWidget(first, assets, skin.newDrawable("white", Color.SLATE));
    ResolvedCard next =
        card("Iron Oath", "Gain 4 Armour.", 2, CardType.POWER, Rarity.UNCOMMON, TargetType.SELF);

    widget.setCard(next);

    assertAll(
        () -> assertEquals("2", widget.displayedCost()),
        () -> assertEquals("Iron Oath", widget.displayedName()),
        () -> assertEquals("Power  |  Uncommon", widget.displayedMeta()),
        () -> assertEquals("Gain 4 Armour.", widget.displayedDescription()),
        () -> assertEquals("Self", widget.displayedTarget()),
        () -> assertSame(next, widget.getCard()),
        () -> assertNotNull(widget.displayedArtwork()));
  }

  @Test
  void shouldRenderEveryConfiguredUncommonCard() {
    List<CardConfig> uncommonCards =
        CardConfigLoader.loadCards().stream()
            .filter(card -> card.rarity == Rarity.UNCOMMON)
            .toList();
    assertFalse(uncommonCards.isEmpty());

    CardResolver resolver = new CardResolver();
    ResolvedCard first = resolve(uncommonCards.getFirst(), resolver);
    UncommonCardLibraryWidget widget =
        new UncommonCardLibraryWidget(first, assets, skin.newDrawable("white", Color.SLATE));

    for (CardConfig config : uncommonCards) {
      ResolvedCard resolved = resolve(config, resolver);
      widget.setCard(resolved);

      assertAll(
          config.id,
          () -> assertEquals(resolved.name(), widget.displayedName()),
          () -> assertEquals(Integer.toString(resolved.cost()), widget.displayedCost()),
          () -> assertTrue(widget.displayedMeta().contains("Uncommon")),
          () -> assertEquals(resolved.description(), widget.displayedDescription()),
          () -> assertNotNull(widget.displayedArtwork()));
    }
  }

  @Test
  void shouldRejectNonUncommonCard() {
    ResolvedCard common =
        card(
            "Strike", "Deal 6 damage.", 1, CardType.ATTACK, Rarity.COMMON, TargetType.SINGLE_ENEMY);
    Drawable frame = skin.newDrawable("white", Color.SLATE);

    assertThrows(
        IllegalArgumentException.class, () -> new UncommonCardLibraryWidget(common, assets, frame));
  }

  private static ResolvedCard card(
      String name, String description, int cost, CardType type, Rarity rarity, TargetType target) {
    return new ResolvedCard(
        "library-instance",
        "card-id",
        name,
        description,
        cost,
        type,
        rarity,
        target,
        List.of(new EffectConfig(EffectType.DAMAGE, 1)),
        "images/cards/example.png",
        false);
  }

  private static ResolvedCard resolve(CardConfig config, CardResolver resolver) {
    CardInstance instance =
        new CardInstance("library-" + config.id, config.id, CardInstance.BASE_LEVEL);
    return resolver.resolve(config, instance);
  }
}
