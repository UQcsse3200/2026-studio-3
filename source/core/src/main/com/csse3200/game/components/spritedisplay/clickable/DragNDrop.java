package com.csse3200.game.components.spritedisplay.clickable;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.csse3200.game.services.ServiceLocator;

public class DragNDrop extends InOutOnTrigger {

  private DragAndDrop.Source dragSource;

  public DragNDrop(ClickableRecord rec) {
    super(rec);
  }

  /** Also unregister this card's drag source so a removed widget can't still start a drag. */
  @Override
  public void remove() {
    if (dragSource != null) {
      ServiceLocator.getDragAndDropService().getDragAndDrop().removeSource(dragSource);
    }
    super.remove();
  }

  /**
   * Drag cards fire their configured trigger on the source entity once the drop target is known.
   * The base Clickable.onClick() fires on the button's ChangeEvent, which libGDX raises on click
   * AND on drag release — for a drag card that trigger only has the cardId baked in (no target
   * yet), so firing it here duplicates the drop-time trigger with the wrong arity and crashes.
   * Suppress it entirely for this variant.
   */
  @Override
  protected void onClick() {
    // Intentionally empty — see javadoc above.
  }

  @Override
  protected void init(String trigger) {
    super.init(trigger);

    DragAndDrop dragAndDrop = ServiceLocator.getDragAndDropService().getDragAndDrop();

    this.dragSource =
        new DragAndDrop.Source(this.getBtn()) {
          private boolean actuallyHidden = false;

          @Override
          public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
            actuallyHidden = false;

            DragAndDrop.Payload payload = new DragAndDrop.Payload();
            // Carry trigger + args + label together so dragStop can fire the
            // right event with the target ID, without needing to know what a
            // "card" is or hardcode any specific event name.
            payload.setObject(new TriggerPayload(trigger, getArgs(), getLabel()));

            Button original = DragNDrop.this.getBtn();
            Button dragVisual = createDragVisual(original);
            dragVisual.setSize(original.getWidth(), original.getHeight());
            payload.setDragActor(dragVisual);

            dragAndDrop.setDragActorPosition(x, -y);

            return payload;
          }

          @Override
          public void drag(InputEvent event, float x, float y, int pointer) {
            Button original = DragNDrop.this.getBtn();
            boolean outsideBounds =
                x < 0 || y < 0 || x > original.getWidth() || y > original.getHeight();

            if (outsideBounds && original.isVisible()) {
              original.clearActions();
              original.setVisible(false);
              actuallyHidden = true;
            }
          }

          @Override
          public void dragStop(
              InputEvent event,
              float x,
              float y,
              int pointer,
              DragAndDrop.Payload payload,
              DragAndDrop.Target target) {
            if (actuallyHidden) {
              Button btn = DragNDrop.this.getBtn();
              btn.clearActions();
              btn.setVisible(true);
              btn.addAction(Actions.fadeIn(0.15f));
              btn.addAction(Actions.moveTo(targetX, targetY, 0.3f, Interpolation.sineIn));
            }

            if (target != null
                && payload.getObject() instanceof TriggerPayload card
                && target.getActor().getUserObject() instanceof String targetId
                && card.args().length == 1) {
              entity.getEvents().trigger(card.trigger(), card.args()[0], targetId);
            }
          }
        };

    dragAndDrop.addSource(this.dragSource);
  }

  private Button createDragVisual(Button original) {
    if (original instanceof ImageButton ib) {
      return new ImageButton(ib.getStyle());
    } else if (original instanceof ImageTextButton itb) {
      return new ImageTextButton(itb.getText().toString(), itb.getStyle());
    } else if (original instanceof TextButton tb) {
      return new TextButton(tb.getText().toString(), tb.getStyle());
    }
    return new Button(original.getStyle());
  }
}
