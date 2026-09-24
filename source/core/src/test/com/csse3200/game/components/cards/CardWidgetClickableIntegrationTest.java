package com.csse3200.game.components.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.csse3200.game.cards.CardType;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.Rarity;
import com.csse3200.game.cards.TargetType;
import com.csse3200.game.cards.configs.EffectConfig;
import com.csse3200.game.cards.runtime.ResolvedCard;
import com.csse3200.game.components.spritedisplay.clickable.ClickableRecord;
import com.csse3200.game.components.spritedisplay.clickable.InOutOnTrigger;
import com.csse3200.game.extensions.GameExtension;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GameExtension.class)
class CardWidgetClickableIntegrationTest {
  private Skin skin;

  @BeforeEach
  void setUp() {
    skin = new Skin(Gdx.files.internal("flat-earth/skin/flat-earth-ui.json"));
  }

  @AfterEach
  void tearDown() {
    skin.dispose();
  }

  @Test
  void shouldEmbedCardWidgetWithoutTakingInputOrChangingInstancePayload() {
    String instanceId = "battle-strike-instance";
    ResolvedCard strike =
        new ResolvedCard(
            instanceId,
            "strike",
            "Strike",
            "Deal 6 damage.",
            1,
            CardType.ATTACK,
            Rarity.COMMON,
            TargetType.SINGLE_ENEMY,
            List.of(new EffectConfig(EffectType.DAMAGE, 6)),
            "images/cards/strike.png",
            false);
    CardWidgetAssets assets =
        CardWidgetAssets.fromSkin(skin, path -> skin.newDrawable("white", Color.NAVY));
    InOutOnTrigger clickable =
        new InOutOnTrigger(
            ClickableRecord.builder("playCard")
                .text("legacy face")
                .args(instanceId)
                .size(CardWidget.CARD_WIDTH, CardWidget.CARD_HEIGHT)
                .build());

    clickable.setVisualContent(() -> new CardWidget(strike, assets));

    CardWidget embedded =
        assertInstanceOf(CardWidget.class, clickable.getBtn().getChildren().first());
    assertEquals(Touchable.enabled, clickable.getBtn().getTouchable());
    assertEquals(Touchable.disabled, embedded.getTouchable());
    assertEquals("Strike", embedded.displayedName());
    assertEquals("1", embedded.displayedCost());
    assertEquals(instanceId, clickable.getArgs()[0]);
  }
}
