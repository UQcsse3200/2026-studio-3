package com.csse3200.game.components.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Scaling;
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
class CardWidgetTest {
  private static final String STRIKE_ART = "images/cards/strike.png";

  private Skin skin;
  private Drawable strikeArtwork;
  private CardWidgetAssets assets;

  @BeforeEach
  void setUp() {
    skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
    strikeArtwork = skin.newDrawable("white", Color.NAVY);
    assets =
        CardWidgetAssets.fromSkin(
            skin, texturePath -> STRIKE_ART.equals(texturePath) ? strikeArtwork : null);
  }

  @AfterEach
  void tearDown() {
    skin.dispose();
  }

  @Test
  void shouldRenderResolvedCardFields() {
    ResolvedCard strike = card("Strike", "Deal 6 damage.", 1, Rarity.COMMON, false);

    CardWidget widget = new CardWidget(strike, assets);

    assertEquals("Strike", widget.displayedName());
    assertEquals("1", widget.displayedCost());
    assertEquals("Attack", widget.displayedType());
    assertEquals("One Enemy", widget.displayedTarget());
    assertEquals("Deal 6 damage.", widget.displayedDescription());
    assertSame(strikeArtwork, widget.displayedArtwork());
    assertTrue(widget.isArtworkVisible());
    assertFalse(widget.displaysUpgradeMarker());
    assertSame(strike, widget.getCard());
  }

  @Test
  void shouldRefreshEveryDynamicFieldForUpgradedCopy() {
    ResolvedCard base = card("Strike", "Deal 6 damage.", 1, Rarity.COMMON, false);
    ResolvedCard upgraded = card("Strike+", "Deal 12 damage.", 0, Rarity.RARE, true);
    CardWidget widget = new CardWidget(base, assets);
    Drawable baseFrame = widget.displayedFrame();

    widget.setCard(upgraded);

    assertEquals("Strike+", widget.displayedName());
    assertEquals("0", widget.displayedCost());
    assertEquals("Attack", widget.displayedType());
    assertEquals("One Enemy", widget.displayedTarget());
    assertEquals("Deal 12 damage.", widget.displayedDescription());
    assertSame(strikeArtwork, widget.displayedArtwork());
    assertTrue(widget.displaysUpgradeMarker());
    assertNotSame(baseFrame, widget.displayedFrame());
    assertSame(upgraded, widget.getCard());
  }

  @Test
  void shouldRemainPresentationOnlyAndUseIntendedDimensions() {
    CardWidget widget =
        new CardWidget(card("Strike", "Deal 6 damage.", 1, Rarity.COMMON, false), assets);

    assertEquals(Touchable.disabled, widget.getTouchable());
    assertTrue(widget.getListeners().isEmpty());
    assertEquals(CardWidget.CARD_WIDTH, widget.getPrefWidth());
    assertEquals(CardWidget.CARD_HEIGHT, widget.getPrefHeight());
    assertEquals(Scaling.fit, CardWidget.ARTWORK_SCALING);
  }

  @Test
  void shouldKeepTypeAndTargetSeparateWhenFallbackFaceIsRebound() {
    ResolvedCard attack = card("Strike", "Deal 6 damage.", 1, Rarity.COMMON, false);
    CardWidget widget = new CardWidget(attack, assets);
    widget.validate();
    Label type = widget.findActor("card-type");
    Label target = widget.findActor("card-target");
    assertEquals("Attack", type.getText().toString());
    assertEquals("One Enemy", target.getText().toString());
    assertNotSame(type, target);

    ResolvedCard skill =
        new ResolvedCard(
            attack.instanceId(),
            attack.cardId(),
            "Defend",
            "Gain 5 Block.",
            attack.cost(),
            CardType.SKILL,
            attack.rarity(),
            TargetType.SELF,
            attack.effects(),
            attack.texturePath(),
            false);
    widget.setCard(skill);
    widget.validate();
    assertEquals("Skill", widget.displayedType());
    assertEquals("Self", widget.displayedTarget());
    assertEquals("Skill", type.getText().toString());
    assertEquals("Self", target.getText().toString());
    assertEquals("Gain 5 Block.", widget.displayedDescription());
  }

  @Test
  void shouldScaleLongNameToProtectHeaderLayout() {
    CardWidget widget =
        new CardWidget(
            card("Warden's Judgement", "Deal 9 damage.", 2, Rarity.UNCOMMON, false), assets);

    assertEquals("Warden's Judgement", widget.displayedName());
    assertTrue(widget.displayedNameScale() < 1f);
    assertTrue(widget.displayedNameScale() >= 0.72f);
  }

  @Test
  void shouldHideArtworkWhenProviderHasNoLoadedTexture() {
    ResolvedCard missingArtwork =
        new ResolvedCard(
            "missing-instance",
            "missing",
            "Missing",
            "No artwork is loaded.",
            2,
            CardType.SKILL,
            Rarity.UNCOMMON,
            TargetType.SELF,
            List.of(new EffectConfig(EffectType.BLOCK, 4)),
            "images/cards/missing.png",
            false);

    CardWidget widget = new CardWidget(missingArtwork, assets);

    assertFalse(widget.isArtworkVisible());
  }

  @Test
  void shouldRejectNullCardAndAssets() {
    ResolvedCard strike = card("Strike", "Deal 6 damage.", 1, Rarity.COMMON, false);

    assertThrows(NullPointerException.class, () -> new CardWidget(strike, null));
    CardWidget widget = new CardWidget(strike, assets);
    assertThrows(NullPointerException.class, () -> widget.setCard(null));
  }

  @Test
  void shouldBindEveryOfficialBaseDefinition() {
    List<CardConfig> configs = CardConfigLoader.loadCards();
    CardResolver resolver = new CardResolver();
    CardConfig firstConfig = configs.getFirst();
    CardWidget widget =
        new CardWidget(
            resolver.resolve(
                firstConfig,
                new CardInstance(
                    "preview-" + firstConfig.id, firstConfig.id, CardInstance.BASE_LEVEL)),
            assets);

    for (CardConfig config : configs) {
      ResolvedCard resolved =
          resolver.resolve(
              config, new CardInstance("preview-" + config.id, config.id, CardInstance.BASE_LEVEL));

      widget.setCard(resolved);

      assertEquals(resolved.name(), widget.displayedName(), config.id);
      assertEquals(Integer.toString(resolved.cost()), widget.displayedCost(), config.id);
      assertEquals(FramedCardFace.formatType(resolved.type()), widget.displayedType(), config.id);
      assertEquals(resolved.description(), widget.displayedDescription(), config.id);
      assertFalse(widget.displaysUpgradeMarker(), config.id);
    }
  }

  @Test
  void shouldBindEveryConfiguredUpgradeWithoutChangingArtworkPath() {
    List<CardConfig> configs =
        CardConfigLoader.loadCards().stream().filter(config -> config.upgrade != null).toList();
    CardResolver resolver = new CardResolver();
    CardConfig firstConfig = configs.getFirst();
    CardWidget widget =
        new CardWidget(
            resolver.resolve(
                firstConfig,
                new CardInstance(
                    "upgrade-preview-" + firstConfig.id,
                    firstConfig.id,
                    CardInstance.UPGRADED_LEVEL)),
            assets);

    for (CardConfig config : configs) {
      CardInstance upgradedInstance =
          new CardInstance("upgrade-preview-" + config.id, config.id, CardInstance.UPGRADED_LEVEL);
      ResolvedCard upgraded = resolver.resolve(config, upgradedInstance);

      widget.setCard(upgraded);

      assertEquals(config.texturePath, upgraded.texturePath(), config.id);
      assertEquals(upgraded.name(), widget.displayedName(), config.id);
      assertEquals(Integer.toString(upgraded.cost()), widget.displayedCost(), config.id);
      assertEquals(upgraded.description(), widget.displayedDescription(), config.id);
      assertTrue(widget.displaysUpgradeMarker(), config.id);
    }
  }

  @Test
  void shouldKeepEveryCardFaceAndDescriptionInsideItsFixedBounds() {
    CardResolver resolver = new CardResolver();
    for (CardConfig config : CardConfigLoader.loadCards()) {
      assertFixedLayout(resolver.resolveBasePreview(config, "preview-" + config.id));
      if (config.upgrade != null) {
        assertFixedLayout(
            resolver.resolve(
                config,
                new CardInstance("upgrade-" + config.id, config.id, CardInstance.UPGRADED_LEVEL)));
      }
    }
  }

  @Test
  void shouldRestoreDescriptionSizeWhenReboundFromLongToShortText() {
    ResolvedCard longCard =
        card(
            "Poison Mark",
            "A quiet sign reveals where the corruption has taken hold. Apply 2 Vulnerable and "
                + "2 Poison to an enemy for 2 turns.",
            1,
            Rarity.RARE,
            false);
    CardWidget widget = new CardWidget(longCard, assets);
    widget.validate();
    Label description = findDescription(widget, longCard.description());
    assertTrue(description.getFontScaleY() < 1f);

    widget.setCard(card("Strike", "Deal 6 damage.", 1, Rarity.COMMON, false));
    widget.validate();

    assertEquals(1f, description.getFontScaleY());
    assertEquals("Deal 6 damage.", description.getText().toString());
  }

  private void assertFixedLayout(ResolvedCard resolved) {
    CardWidget widget = new CardWidget(resolved, assets);
    Table container = new Table();
    container.add(widget).size(CardWidget.CARD_WIDTH, CardWidget.CARD_HEIGHT);
    container.setSize(CardWidget.CARD_WIDTH, CardWidget.CARD_HEIGHT);
    container.validate();

    assertEquals(CardWidget.CARD_WIDTH, widget.getWidth(), resolved.cardId());
    assertEquals(CardWidget.CARD_HEIGHT, widget.getHeight(), resolved.cardId());
    assertTrue(widget.getMinHeight() <= CardWidget.CARD_HEIGHT, resolved.cardId());
    assertChildrenInside(widget, resolved.cardId());
    Label description = findDescription(widget, resolved.description());
    assertEquals(resolved.description(), description.getText().toString());
    assertTrue(description.getPrefHeight() <= description.getHeight() + 1f, resolved.cardId());
    assertTrue(
        description.getGlyphLayout().width <= description.getWidth() + 1f, resolved.cardId());
  }

  private static void assertChildrenInside(Group parent, String cardId) {
    for (Actor child : parent.getChildren()) {
      assertTrue(child.getX() >= -1f, cardId + ": left overflow");
      assertTrue(child.getY() >= -1f, cardId + ": bottom overflow");
      assertTrue(
          child.getX() + child.getWidth() <= parent.getWidth() + 1f, cardId + ": right overflow");
      assertTrue(
          child.getY() + child.getHeight() <= parent.getHeight() + 1f, cardId + ": top overflow");
      if (child instanceof Group group) {
        assertChildrenInside(group, cardId);
      }
    }
  }

  private static Label findDescription(Group parent, String description) {
    for (Actor child : parent.getChildren()) {
      if (child instanceof Label label && description.contentEquals(label.getText())) {
        return label;
      }
      if (child instanceof Group group) {
        Label found = findDescription(group, description);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static ResolvedCard card(
      String name, String description, int cost, Rarity rarity, boolean upgraded) {
    return new ResolvedCard(
        "strike-instance",
        "strike",
        name,
        description,
        cost,
        CardType.ATTACK,
        rarity,
        TargetType.SINGLE_ENEMY,
        List.of(new EffectConfig(EffectType.DAMAGE, upgraded ? 12 : 6)),
        STRIKE_ART,
        upgraded);
  }
}
