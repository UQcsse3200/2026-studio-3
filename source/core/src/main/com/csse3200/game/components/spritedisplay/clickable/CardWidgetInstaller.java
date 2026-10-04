package com.csse3200.game.components.spritedisplay.clickable;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.cards.CardLibrary;
import com.csse3200.game.cards.configs.CardConfig;
import com.csse3200.game.cards.runtime.CardInstance;
import com.csse3200.game.cards.runtime.CardResolver;
import com.csse3200.game.cards.runtime.ResolvedCard;
import com.csse3200.game.components.cards.CardWidget;
import com.csse3200.game.components.cards.CardWidgetAssets;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CardWidgetInstaller {
    private static final Logger logger = LoggerFactory.getLogger(CardWidgetInstaller.class);
    private static final CardResolver RESOLVER = new CardResolver();

    private CardWidgetInstaller() {}

    public static void install(
            ClickableFactory factory,
            String trigger,
            CardLibrary library,
            CardWidgetAssets assets,
            Map<String, CardInstance> instancesById) {
        for (Clickable clickable : factory.getByTrigger(trigger)) {
            Object[] args = clickable.getArgs();
            if (args.length == 0 || !(args[0] instanceof String id)) {
                continue;
            }
            CardInstance instance = instancesById.get(id);
            if (instance == null) {
                continue;
            }
            Optional<CardConfig> cfg = library.getCard(instance.cardId());
            if (cfg.isEmpty()) {
                continue;
            }
            try {
                ResolvedCard resolved = RESOLVER.resolve(cfg.get(), instance);
                clickable.setVisualContent(() -> buildFitted(resolved, assets, clickable));
            } catch (IllegalArgumentException | IllegalStateException ex) {
                logger.warn("Could not install card widget for {}", id, ex);
            }
        }
    }

    /**
     * Wraps a {@link CardWidget} so it keeps its natural 225×456 proportions inside whatever slot the
     * clickable was given, instead of being stretched by the button's {@code expand().fill()} cell.
     * The widget is scaled uniformly to fit and centred in a {@link Group} of the slot's size.
     */
    private static Actor buildFitted(
            ResolvedCard resolved, CardWidgetAssets assets, Clickable clickable) {
        float slotW = clickable.getWidth();
        float slotH = clickable.getHeight();
        float scale = Math.min(slotW / CardWidget.CARD_WIDTH, slotH / CardWidget.CARD_HEIGHT);

        CardWidget widget = new CardWidget(resolved, assets);
        widget.setTransform(true);
        widget.setOrigin(Align.bottomLeft);
        widget.setScale(scale);
        widget.setPosition(
                (slotW - CardWidget.CARD_WIDTH * scale) / 2f,
                (slotH - CardWidget.CARD_HEIGHT * scale) / 2f);

        Group box = new Group();
        box.addActor(widget);
        return box;
    }
}