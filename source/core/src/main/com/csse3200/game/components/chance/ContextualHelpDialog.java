package com.csse3200.game.components.chance;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.csse3200.game.services.ResourceService;
import com.csse3200.game.services.ServiceLocator;
import com.csse3200.game.ui.PixelButtonStyles;
import java.util.Objects;

/** A small, reusable rules dialog that does not interact with encounter state. */
final class ContextualHelpDialog {
  private final Dialog dialog;
  private final Label bodyLabel;
  private final TextButton closeButton;
  private boolean styledDialog;

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
    applyStyle(skin);
  }

  /** Applies the shared Event rules frame and button feedback. */
  private void applyStyle(Skin skin) {
    ResourceService resources = ServiceLocator.getResourceService();
    if (resources == null
        || !resources.containsAsset("images/context_help_rules_frame.png", Texture.class)
        || !resources.containsAsset("images/ancient_temple_choice_button.png", Texture.class)) {
      return;
    }

    Texture frameTexture = resources.getAsset("images/context_help_rules_frame.png", Texture.class);
    frameTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    TextureRegionDrawable frame = new TextureRegionDrawable(new TextureRegion(frameTexture));
    frame.setMinWidth(0f);
    frame.setMinHeight(0f);

    WindowStyle windowStyle = new WindowStyle(dialog.getStyle());
    windowStyle.background = frame;
    windowStyle.stageBackground = skin.newDrawable("white", new Color(0f, 0f, 0f, 0.55f));
    dialog.setStyle(windowStyle);
    dialog.pad(100f, 88f, 84f, 88f);

    Label titleLabel = dialog.getTitleLabel();
    LabelStyle titleStyle = new LabelStyle(skin.get("large", LabelStyle.class));
    titleStyle.fontColor = Color.valueOf("EFC26C");
    titleLabel.setStyle(titleStyle);
    titleLabel.setAlignment(Align.center);
    dialog.getTitleTable().clear();
    dialog.getContentTable().clear();
    dialog.getContentTable().pad(0f).top();
    dialog.getContentTable().add(titleLabel).growX().height(30f).padBottom(22f);
    dialog.getContentTable().row();

    LabelStyle bodyStyle = new LabelStyle(skin.get(LabelStyle.class));
    bodyStyle.fontColor = Color.valueOf("E8DCC4");
    bodyLabel.setStyle(bodyStyle);
    bodyLabel.setAlignment(Align.topLeft);
    dialog.getContentTable().add(bodyLabel).growX().top();

    Texture buttonTexture =
        resources.getAsset("images/ancient_temple_choice_button.png", Texture.class);
    closeButton.setStyle(PixelButtonStyles.create(skin, buttonTexture));
    dialog.getButtonTable().getCell(closeButton).width(220f).height(52f).pad(0f);
    dialog.getButtonTable().padTop(10f);
    styledDialog = true;
  }

  /** Creates the shared image help button, with a fallback for unloaded test assets. */
  static TextButton createHelpButton(Skin skin) {
    ResourceService resources = ServiceLocator.getResourceService();
    if (resources == null
        || !resources.containsAsset("images/context_help_icon.png", Texture.class)) {
      return new TextButton("?", skin);
    }
    Texture texture = resources.getAsset("images/context_help_icon.png", Texture.class);
    return new TextButton("", PixelButtonStyles.create(skin, texture));
  }

  void show(Stage stage) {
    Objects.requireNonNull(stage, "stage cannot be null");
    if (dialog.getStage() == null) {
      dialog.show(stage);
      if (styledDialog) {
        dialog.setSize(1040f, 520f);
        dialog.setPosition((stage.getWidth() - 1040f) / 2f, (stage.getHeight() - 520f) / 2f);
        dialog.validate();
      }
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
