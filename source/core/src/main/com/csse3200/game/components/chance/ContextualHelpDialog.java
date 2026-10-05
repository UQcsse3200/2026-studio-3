package com.csse3200.game.components.chance;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import java.util.Objects;

/** A small, reusable rules dialog that does not interact with encounter state. */
final class ContextualHelpDialog {
  private final Dialog dialog;
  private final Label bodyLabel;
  private final TextButton closeButton;

  ContextualHelpDialog(Skin skin, String title, String body) {
    Objects.requireNonNull(skin, "skin cannot be null");
    dialog = new Dialog(Objects.requireNonNull(title, "title cannot be null"), skin);
    dialog.setName("contextual-help-dialog");
    dialog.setModal(true);
    dialog.setMovable(false);
    dialog.getContentTable().pad(18f);
    bodyLabel = new Label(Objects.requireNonNull(body, "body cannot be null"), skin);
    bodyLabel.setWrap(true);
    dialog.getContentTable().add(bodyLabel).width(760f).left();
    dialog.getButtonTable().defaults().width(180f).height(52f).pad(10f);
    closeButton = new TextButton("Close", skin);
    closeButton.setName("contextual-help-close");
    dialog.button(closeButton);
  }

  void show(Stage stage) {
    Objects.requireNonNull(stage, "stage cannot be null");
    if (dialog.getStage() == null) {
      dialog.show(stage);
    }
  }

  void remove() {
    dialog.remove();
  }

  Dialog getDialog() {
    return dialog;
  }

  Label getBodyLabel() {
    return bodyLabel;
  }

  TextButton getCloseButton() {
    return closeButton;
  }
}
