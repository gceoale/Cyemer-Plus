package com.slither.cyemer.gui.new_ui.settings;

import com.slither.cyemer.module.StringSetting;
import com.slither.cyemer.module.implementation.ClickGUIModule;
import com.slither.cyemer.util.GuiShaderStyle;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public class StringComponent extends SettingComponent {
    private final StringSetting stringSetting;
    private long lastInteractionTime = 0L;
    private double focusAnimation = 0.0;
    private static StringComponent activeComponent = null;

    public StringComponent(StringSetting setting) {
        super(setting);
        this.stringSetting = setting;
    }

    private boolean isEditing() {
        return activeComponent == this;
    }

    private void startEditing() {
        if (activeComponent != null && activeComponent != this) {
            activeComponent.lastInteractionTime = 0L;
        }

        activeComponent = this;
        this.lastInteractionTime = System.currentTimeMillis();
    }

    private void stopEditing() {
        if (activeComponent == this) {
            activeComponent = null;
        }
    }

    @Override
    public void render(class_332 context, int mouseX, int mouseY, float delta, double alpha) {
        double dt = Math.max(0.001, Math.min((double)delta, 0.05));
        double targetFocus = this.isEditing() ? 1.0 : 0.0;
        this.focusAnimation = this.focusAnimation + (targetFocus - this.focusAnimation) * (1.0 - Math.exp(-20.0 * dt));
        float fontSize = 8.5F;
        float boxRadius = 6.0F;
        Color bgColor = ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBackground(), alpha * (0.5 + this.focusAnimation * 0.3));
        Renderer.get()
            .drawRoundedRectStyled(context, (float)this.x, (float)this.y, (float)this.width, (float)this.height, boxRadius, bgColor, GuiShaderStyle.SEARCH);
        if (this.focusAnimation > 0.01) {
            Color borderColor = ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientStart(), alpha * this.focusAnimation);
            float borderWidth = 1.0F + (float)(this.focusAnimation * 0.5);
            Renderer.get()
                .drawRoundedRectOutline(context, (float)this.x, (float)this.y, (float)this.width, (float)this.height, boxRadius, borderWidth, borderColor);
        }

        String textToDisplay = this.stringSetting.getValue();
        if (textToDisplay.isEmpty() && !this.isEditing()) {
            textToDisplay = "Enter text...";
        } else if (this.isEditing() && (System.currentTimeMillis() - this.lastInteractionTime) % 1000L < 500L) {
            textToDisplay = textToDisplay + "|";
        }

        double padding = 3.0;
        double availableWidth = this.width - padding * 2.0;

        while (Renderer.get().getTextWidth(textToDisplay, fontSize) > availableWidth && !textToDisplay.isEmpty()) {
            textToDisplay = textToDisplay.substring(1);
        }

        float textX = (float)(this.x + padding);
        float textHeightVal = Renderer.get().getTextHeight(fontSize);
        float textY = (float)(this.y + (this.height - textHeightVal) / 2.0);
        Color textColor;
        if (this.stringSetting.getValue().isEmpty() && !this.isEditing()) {
            textColor = ClickGUIModule.getColor(ClickGUIModule.getModuleDisabledText(), alpha * 0.5);
        } else {
            textColor = ClickGUIModule.getColor(ClickGUIModule.getSearchBoxText(), alpha);
        }

        Renderer.get().drawText(context, textToDisplay, textX, textY, fontSize, textColor, false);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (this.isHovered(mouseX, mouseY)) {
                this.startEditing();
            } else {
                this.stopEditing();
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        if (this.isEditing() && chr >= ' ' && chr <= '~') {
            this.stringSetting.setValue(this.stringSetting.getValue() + chr);
            this.lastInteractionTime = System.currentTimeMillis();
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.isEditing()) {
            this.lastInteractionTime = System.currentTimeMillis();
            String current = this.stringSetting.getValue();
            if (keyCode == 259) {
                if (!current.isEmpty()) {
                    this.stringSetting.setValue(current.substring(0, current.length() - 1));
                }
            } else if (keyCode == 86 && (modifiers & 10) != 0) {
                String clipboard = class_310.method_1551().field_1774.method_1460();
                this.stringSetting.setValue(current + clipboard);
            } else if (keyCode == 256 || keyCode == 257) {
                this.stopEditing();
            }
        }
    }

    @Override
    public double getComponentHeight() {
        return 11.0;
    }
}
