package com.csse3200.game.components.status;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.csse3200.game.components.CombatStatsComponent;
import com.csse3200.game.components.StatusEffect;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.UIComponent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shows a row of status icons beside a combatant. Each icon has an outlined count beside it and
 * explains the status in a tooltip when hovered.
 *
 * <p>The row is a separate component from the health display, so layout changes there never
 * conflict with it. It is anchored to a point relative to the entity and grows either upwards or
 * downwards from that point.
 *
 * <p>Statuses are read from {@link CombatStatsComponent} every frame and the row is rebuilt only
 * when a status is added, removed, or changes its value or duration. Reading every frame is needed
 * because a status ticking down does not fire an event.
 */
public class StatusIconDisplay extends UIComponent {
  private static final float ICON_SIZE = 28f;
  private static final float ICON_GAP = 8f;
  private static final float COUNT_GAP = 3f;
  private static final float OUTLINE_WIDTH = 1f;
  private static final float ROW_GAP = 4f;

  /** Offsets of the dark copies drawn behind the count, forming a one-pixel outline. */
  private static final int[][] OUTLINE_OFFSETS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

  /** Statuses whose value (stacks or points) matters enough to show in the tooltip title. */
  private static final Set<StatusInfo> SHOWS_VALUE =
      EnumSet.of(StatusInfo.POISON, StatusInfo.STRENGTH);

  /** The value and duration last drawn for one status, used to spot changes. */
  private record Shown(int value, int duration) {}

  private final float worldOffsetY;
  private final boolean above;
  private final Vector3 screenPosition = new Vector3();
  private Map<String, Shown> shown = Map.of();
  private Table row;
  private StatusTooltip tooltip;
  private Label.LabelStyle countStyle;
  private Label.LabelStyle outlineStyle;

  /**
   * @param worldOffsetY vertical offset of the anchor point from the entity's position, in world
   *     units
   * @param above true to grow the row upwards from the anchor, false to grow it downwards
   */
  public StatusIconDisplay(float worldOffsetY, boolean above) {
    this.worldOffsetY = worldOffsetY;
    this.above = above;
  }

  @Override
  public void create() {
    super.create();
    row = new Table();
    row.setVisible(false);
    tooltip = new StatusTooltip(skin);
    countStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    countStyle.fontColor = new Color(Color.WHITE);
    outlineStyle = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
    outlineStyle.fontColor = new Color(Color.BLACK);
    stage.addActor(row);
    refresh();
  }

  @Override
  public void update() {
    refresh();
    updatePosition();
  }

  @Override
  public void draw(SpriteBatch batch) {
    // Drawing is handled by the stage.
  }

  @Override
  public void dispose() {
    if (tooltip != null) {
      tooltip.hide();
    }
    if (row != null) {
      row.remove();
    }
    super.dispose();
  }

  /** Rebuilds the icons when the set of statuses, or any value or duration, has changed. */
  private void refresh() {
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    if (stats == null || row == null) {
      return;
    }
    Map<String, Shown> current = readStatuses(stats);
    if (current.equals(shown)) {
      return;
    }
    shown = current;

    row.clearChildren();
    for (Map.Entry<String, Shown> status : current.entrySet()) {
      addIcon(status.getKey(), status.getValue());
    }
    row.pack();
    row.setVisible(!current.isEmpty());
  }

  /** Collects the known statuses in a stable order: by display order, then by key. */
  private static Map<String, Shown> readStatuses(CombatStatsComponent stats) {
    List<String> keys = new ArrayList<>();
    for (String key : stats.getStatusEffectDurations().keySet()) {
      if (StatusInfo.forKey(key).isPresent()) {
        keys.add(key);
      }
    }
    keys.sort(
        Comparator.comparing((String key) -> StatusInfo.forKey(key).orElseThrow().ordinal())
            .thenComparing(Comparator.naturalOrder()));

    Map<String, Shown> statuses = new LinkedHashMap<>();
    for (String key : keys) {
      StatusEffect effect = stats.getStatusEffect(key);
      if (effect != null) {
        statuses.put(key, new Shown(effect.getValue(), effect.getDuration()));
      }
    }
    return statuses;
  }

  private void addIcon(String key, Shown status) {
    StatusInfo info = StatusInfo.forKey(key).orElseThrow();
    ResourceService resources = ServiceLocator.getResourceService();
    if (resources == null || !resources.containsAsset(info.iconPath(), Texture.class)) {
      return;
    }

    Table icon = new Table();
    icon.add(new Image(resources.getAsset(info.iconPath(), Texture.class))).size(ICON_SIZE);
    icon.add(outlinedCount(countText(info, status))).padLeft(COUNT_GAP).bottom();
    row.add(icon).space(ICON_GAP);
    tooltip.attachTo(icon, () -> content(key));
  }

  /**
   * White text with a thin black outline, so the count stays readable over both light and dark
   * backgrounds without a box behind it. The outline is four black copies of the text, each nudged
   * one pixel in a different direction, with the white text drawn on top.
   */
  private Stack outlinedCount(String text) {
    Stack outlined = new Stack();
    for (int[] offset : OUTLINE_OFFSETS) {
      outlined.add(shifted(new Label(text, outlineStyle), offset[0], offset[1]));
    }
    outlined.add(shifted(new Label(text, countStyle), 0, 0));
    return outlined;
  }

  /** Wraps a label in padding that moves it dx pixels right and dy pixels up. */
  private static Table shifted(Label label, int dx, int dy) {
    Table cell = new Table();
    cell.add(label)
        .padLeft(OUTLINE_WIDTH + dx)
        .padRight(OUTLINE_WIDTH - dx)
        .padTop(OUTLINE_WIDTH - dy)
        .padBottom(OUTLINE_WIDTH + dy);
    return cell;
  }

  /**
   * Builds the tooltip text from the live status, so it stays correct while the tooltip is open.
   *
   * @return the text to show, or null once the status has ended
   */
  private StatusTooltip.Content content(String key) {
    CombatStatsComponent stats = entity.getComponent(CombatStatsComponent.class);
    StatusInfo info = StatusInfo.forKey(key).orElse(null);
    StatusEffect effect = stats == null ? null : stats.getStatusEffect(key);
    if (info == null || effect == null) {
      return null;
    }
    String title =
        SHOWS_VALUE.contains(info) ? info.title() + " " + effect.getValue() : info.title();
    String duration =
        info == StatusInfo.POISON
            ? StatusInfo.poisonDurationText(stats.getPoisonStacksByDuration())
            : StatusInfo.durationText(effect.getDuration());
    return new StatusTooltip.Content(title, info.describe(effect.getValue()), duration);
  }

  /**
   * Total stacks for Poison, since that is the damage it deals next turn; the tooltip lists how
   * long each group lasts. Other statuses show the turns remaining if they expire, their value if
   * it matters (such as Strength points), and nothing otherwise.
   */
  private static String countText(StatusInfo info, Shown status) {
    if (info == StatusInfo.POISON) {
      return Integer.toString(status.value());
    }
    if (status.duration() > 0) {
      return Integer.toString(status.duration());
    }
    return SHOWS_VALUE.contains(info) ? Integer.toString(status.value()) : "";
  }

  /** Keeps the row centred on the entity, just above or below the anchor point. */
  private void updatePosition() {
    Camera camera = ServiceLocator.getCamera();
    if (camera == null || row == null) {
      return;
    }
    Vector2 position = entity.getPosition();
    Vector2 scale = entity.getScale();
    screenPosition.set(position.x + scale.x / 2f, position.y + worldOffsetY, 0f);
    camera.project(screenPosition);

    float rowY = above ? screenPosition.y + ROW_GAP : screenPosition.y - ROW_GAP - row.getHeight();
    row.setPosition(screenPosition.x - row.getWidth() / 2f, rowY);
  }
}
