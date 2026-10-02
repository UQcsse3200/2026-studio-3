package com.csse3200.game.components.battle;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.csse3200.game.cards.EffectType;
import com.csse3200.game.cards.effects.ResolvedCardEffect;
import com.csse3200.game.cards.play.CardPlayRequest;
import com.csse3200.game.components.Component;
import com.csse3200.game.components.cards.CardEffectHandler;
import com.csse3200.game.components.combat.BattleController;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.ServiceLocator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns resolved card effects into short visuals, without touching battle logic.
 *
 * <p>Subscribes to {@link BattleController}'s existing listeners. Damage is still applied by the
 * controller exactly as before; this only reacts to what already happened, so it can never delay or
 * reorder a turn. Every listener is wrapped so a visual failure never breaks the battle.
 *
 * <p>Visuals are separate entities, spawned into {@link com.csse3200.game.entities.EntityService}.
 * They are disposed from this component's own {@link #update()} rather than from their own, because
 * an entity must not dispose itself while its components are being iterated.
 */
public class BattleAnimationCoordinator extends Component {
  private static final Logger logger = LoggerFactory.getLogger(BattleAnimationCoordinator.class);
  private static final float SIZE_FACTOR = 0.8f;
  private static final float MIN_SIZE = 0.6f;

  private final BattleController controller;
  private final CardEffectHandler effectHandler;
  private final List<Entity> enemies;
  private final Entity player;
  private final EffectVisualRegistry registry;
  private final List<Entity> activeVisuals = new ArrayList<>();

  /**
   * @param controller the battle controller to listen to
   * @param effectHandler used to find which enemies a played card targets
   * @param enemies the same enemy list the controller was built with
   * @param player the player entity
   * @param registry effect-type-to-visual lookup, shared with teammates' registrations
   */
  public BattleAnimationCoordinator(
      BattleController controller,
      CardEffectHandler effectHandler,
      List<Entity> enemies,
      Entity player,
      EffectVisualRegistry registry) {
    this.controller = Objects.requireNonNull(controller, "controller cannot be null");
    this.effectHandler = Objects.requireNonNull(effectHandler, "effectHandler cannot be null");
    this.enemies = List.copyOf(Objects.requireNonNull(enemies, "enemies cannot be null"));
    this.player = Objects.requireNonNull(player, "player cannot be null");
    this.registry = Objects.requireNonNull(registry, "registry cannot be null");
  }

  @Override
  public void create() {
    super.create();
    controller.addEnemyEffectsListener(
        effects -> safely("enemy effects", () -> onEnemyEffects(effects)));
    controller.addPlayerEffectsListener(
        effects -> safely("player effects", () -> onPlayerEffects(effects)));
  }

  @Override
  public void update() {
    Iterator<Entity> iterator = activeVisuals.iterator();
    while (iterator.hasNext()) {
      Entity visual = iterator.next();
      EffectVisualComponent component = visual.getComponent(EffectVisualComponent.class);
      if (component == null || component.isExpired()) {
        iterator.remove();
        visual.dispose();
      }
    }
  }

  private static final float EFFECT_STAGGER_SECONDS = 0.15f;

  /** Fires before the controller applies the effects, so the targets are still alive to read. */
  private void onEnemyEffects(List<ResolvedCardEffect> effects) {
    CardPlayRequest request = controller.getCardPlayRequest();
    if (request == null || effects == null || effects.isEmpty()) {
      return;
    }
    List<EffectType> orderedTypes = distinctTypesInOrder(effects);
    for (Entity target : effectHandler.getLivingEnemyTargets(request, enemies)) {
      for (int i = 0; i < orderedTypes.size(); i++) {
        spawnVisual(orderedTypes.get(i), target, i * EFFECT_STAGGER_SECONDS);
      }
    }
  }

  private void onPlayerEffects(List<ResolvedCardEffect> effects) {
    List<EffectType> orderedTypes = distinctTypesInOrder(effects);
    for (int i = 0; i < orderedTypes.size(); i++) {
      spawnVisual(orderedTypes.get(i), player, i * EFFECT_STAGGER_SECONDS);
    }
  }

  private void spawnVisual(EffectType type, Entity target, float startDelay) {
    EffectVisualStyle style = registry.lookup(type);
    Vector2 scale = target.getScale();
    float baseSize = Math.max(MIN_SIZE, Math.max(scale.x, scale.y) * SIZE_FACTOR);

    Entity visual =
        new Entity()
            .addComponent(
                new EffectVisualComponent(textureFor(style), style, baseSize, startDelay));
    visual.setPosition(target.getCenterPosition());
    ServiceLocator.getEntityService().register(visual);
    activeVisuals.add(visual);
  }

  private Texture textureFor(EffectVisualStyle style) {
    if (style.iconPath() == null) {
      return null;
    }
    try {
      return ServiceLocator.getResourceService().getAsset(style.iconPath(), Texture.class);
    } catch (RuntimeException e) {
      logger.warn("Effect icon {} is not loaded, skipping the texture", style.iconPath());
      return null;
    }
  }

  /** Preserves each effect's first appearance order, dropping later duplicates of the same type. */
  private static List<EffectType> distinctTypesInOrder(List<ResolvedCardEffect> effects) {
    Set<EffectType> seen = new LinkedHashSet<>();
    if (effects != null) {
      for (ResolvedCardEffect effect : effects) {
        seen.add(effect.type());
      }
    }
    return new ArrayList<>(seen);
  }

  private static void safely(String what, Runnable action) {
    try {
      action.run();
    } catch (RuntimeException e) {
      logger.warn("Battle visual '{}' failed and was skipped", what, e);
    }
  }
}
