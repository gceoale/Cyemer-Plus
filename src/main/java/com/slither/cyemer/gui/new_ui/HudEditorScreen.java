package com.slither.cyemer.gui.new_ui;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.hud.HUDElement;
import com.slither.cyemer.hud.HUDManager;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Setting;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.theme.Theme;
import com.slither.cyemer.theme.ThemeManager;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_768;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class HudEditorScreen extends class_437 {
    private final class_437 parent;
    private Theme theme;
    private HUDElement draggingElement = null;
    private HUDElement settingsElement = null;
    private final Map<Setting, Boolean> draggingSliders = new HashMap<>();

    public HudEditorScreen(class_437 parent) {
        super(class_2561.method_43470("HUD Editor"));
        this.parent = parent;
    }

    private boolean isShiftDown() {
        if (this.field_22787 == null) {
            return false;
        } else {
            long handle = this.field_22787.method_22683().method_4490();
            return GLFW.glfwGetKey(handle, 340) == 1 || GLFW.glfwGetKey(handle, 344) == 1;
        }
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        this.theme = ThemeManager.getInstance().getCurrentTheme();
        int screenWidth = this.field_22789;
        int screenHeight = this.field_22790;
        float pixelRatio = this.field_22787.method_22683().method_4495();
        if (Renderer.get().beginFrame(screenWidth, screenHeight, pixelRatio)) {
            try {
                Renderer.get().drawRect(context, 0.0F, 0.0F, this.field_22789, this.field_22790, new Color(0, 0, 0, 128));
                String instructions = "Left-click drag to move. Right-click to toggle. Shift + Right-click for settings.";
                float instrWidth = Renderer.get().getTextWidth(instructions, 14.0F);
                Renderer.get().drawText(context, instructions, (this.field_22789 - instrWidth) / 2.0F, 15.0F, 14.0F, Color.WHITE, true);

                for (HUDElement element : HUDManager.getInstance().getElements()) {
                    if (!element.isEnabled()) {
                        float x = (float)element.getX();
                        float y = (float)element.getY();
                        float w = (float)element.getWidth();
                        float h = (float)element.getHeight();
                        Renderer.get().drawRoundedRect(context, x, y, w, h, 4.0F, new Color(170, 170, 170, 80));
                        String name = element.getName();
                        float nameWidth = Renderer.get().getTextWidth(name, 12.0F);
                        Renderer.get().drawText(context, name, x + (w - nameWidth) / 2.0F, y + (h - 12.0F) / 2.0F, 12.0F, new Color(170, 170, 170), true);
                    }
                }
            } finally {
                Renderer.get().endFrame();
            }

            for (HUDElement elementx : HUDManager.getInstance().getElements()) {
                if (elementx.isEnabled()) {
                    elementx.render(context, delta);
                }
            }

            if (this.settingsElement != null) {
                this.renderSettingsPopup(context, mouseX, mouseY, pixelRatio);
            }
        }
    }

    private void renderSettingsPopup(class_332 context, int mouseX, int mouseY, float pixelRatio) {
        if (this.settingsElement != null) {
            List<Setting> settings = this.settingsElement.getSettings();
            if (settings != null && !settings.isEmpty()) {
                int toggleCount = 0;
                int sliderCount = 0;

                for (Setting setting : settings) {
                    if (setting instanceof BooleanSetting) {
                        toggleCount++;
                    } else if (setting instanceof SliderSetting) {
                        sliderCount++;
                    }
                }

                int popupWidth = 180;
                int itemHeight = 18;
                int sliderSectionHeight = 28;
                int popupHeight = 10 + toggleCount * itemHeight + 5 + sliderCount * sliderSectionHeight + 5;
                int popupX = (int)(this.settingsElement.getX() + this.settingsElement.getWidth() + 8.0);
                int popupY = (int)this.settingsElement.getY();
                if (popupX + popupWidth > this.field_22789) {
                    popupX = (int)(this.settingsElement.getX() - popupWidth - 8.0);
                }

                int screenWidth = this.field_22789;
                int screenHeight = this.field_22790;
                if (Renderer.get().beginFrame(screenWidth, screenHeight, pixelRatio)) {
                    try {
                        for (int i = 3; i >= 0; i--) {
                            float offset = i * 2;
                            float alpha = (60 - i * 15) / 255.0F;
                            Renderer.get()
                                .drawRoundedRect(
                                    context,
                                    popupX - offset,
                                    popupY - offset,
                                    popupWidth + offset * 2.0F,
                                    popupHeight + offset * 2.0F,
                                    8.0F,
                                    new Color(0, 0, 0, (int)(alpha * 255.0F))
                                );
                        }

                        Renderer.get().drawRoundedRect(context, popupX, popupY, popupWidth, popupHeight, 8.0F, this.theme.panelBg);
                        Color topGradient = new Color(100, 150, 255, 60);
                        Color bottomGradient = new Color(100, 150, 255, 0);
                        Renderer.get().drawRoundedRectGradient(context, popupX, popupY, popupWidth, popupHeight, 8.0F, topGradient, bottomGradient, true);
                        Renderer.get().drawRoundedRectOutline(context, popupX, popupY, popupWidth, popupHeight, 8.0F, 1.5F, new Color(255, 255, 255, 60));
                        float currentY = popupY + 8;

                        for (Setting settingx : settings) {
                            if (settingx instanceof BooleanSetting boolSetting) {
                                String label = settingx.getName();
                                boolean state = boolSetting.isEnabled();
                                Renderer.get().drawText(context, label, popupX + 10, currentY + 3.0F, 13.0F, Color.WHITE, true);
                                String stateText = state ? "ON" : "OFF";
                                Color stateColor = state ? new Color(100, 255, 100) : new Color(255, 100, 100);
                                float stateWidth = Renderer.get().getTextWidth(stateText, 13.0F);
                                Renderer.get().drawText(context, stateText, popupX + popupWidth - stateWidth - 10.0F, currentY + 3.0F, 13.0F, stateColor, true);
                                if (this.isMouseOver(mouseX, mouseY, new class_768(popupX + 5, (int)currentY, popupWidth - 10, itemHeight - 2))) {
                                    Renderer.get()
                                        .drawRoundedRect(context, popupX + 5, currentY, popupWidth - 10, itemHeight - 2, 4.0F, new Color(255, 255, 255, 20));
                                }

                                currentY += itemHeight;
                            }
                        }

                        currentY += 5.0F;

                        for (Setting settingxx : settings) {
                            if (settingxx instanceof SliderSetting sliderSetting) {
                                String label = settingxx.getName() + ":";
                                String valueText = this.formatSliderValue(sliderSetting);
                                Renderer.get().drawText(context, label, popupX + 10, currentY, 12.0F, Color.WHITE, true);
                                float valueWidth = Renderer.get().getTextWidth(valueText, 12.0F);
                                Renderer.get()
                                    .drawText(context, valueText, popupX + popupWidth - 10 - valueWidth, currentY, 12.0F, new Color(200, 200, 200), true);
                                currentY += 14.0F;
                                int sliderX = popupX + 10;
                                int sliderWidth = popupWidth - 20;
                                Renderer.get().drawRoundedRect(context, sliderX, currentY, sliderWidth, 4.0F, 2.0F, new Color(60, 60, 60));
                                double percent = (sliderSetting.getValue() - sliderSetting.getMin()) / (sliderSetting.getMax() - sliderSetting.getMin());
                                if (percent > 0.01) {
                                    Color fillColor = this.theme.moduleEnabledBg.brighter();
                                    Renderer.get().drawRoundedRect(context, sliderX, currentY, (float)(sliderWidth * percent), 4.0F, 2.0F, fillColor);
                                }

                                int handleX = (int)(sliderX + (sliderWidth - 6) * Math.max(0.0, Math.min(1.0, percent)));

                                for (int i = 3; i >= 1; i--) {
                                    float glowSize = 8 + i * 2;
                                    float glowAlpha = 0.3F - i * 0.08F;
                                    Renderer.get()
                                        .drawRoundedRect(
                                            context,
                                            handleX - glowSize / 2.0F + 3.0F,
                                            currentY - glowSize / 2.0F + 2.0F,
                                            glowSize,
                                            glowSize,
                                            glowSize / 2.0F,
                                            new Color(255, 255, 255, (int)(glowAlpha * 255.0F))
                                        );
                                }

                                Renderer.get().drawRoundedRect(context, handleX, currentY - 2.0F, 6.0F, 8.0F, 3.0F, Color.WHITE);
                                currentY += 14.0F;
                            }
                        }
                    } finally {
                        Renderer.get().endFrame();
                    }
                }
            }
        }
    }

    private String formatSliderValue(SliderSetting slider) {
        double value = slider.getValue();
        String name = slider.getName();
        if (name.contains("Opacity") || name.contains("Intensity")) {
            int percent = (int)Math.round(value * 100.0);
            return percent + "%";
        } else if (name.contains("Delay")) {
            return value < 0.01 ? "Never" : String.format("%.1f", value) + "s";
        } else {
            return !name.contains("Size") && !name.contains("Radius") && !name.contains("Font") ? slider.getValueAsString() : String.format("%.0f", value);
        }
    }

    public boolean method_25402(class_11909 click, boolean doubleClick) {
        double mouseX = click.comp_4798();
        double mouseY = click.comp_4799();
        int button = click.method_74245();
        if (this.settingsElement != null) {
            if (!this.isMouseOverSettings(mouseX, mouseY)) {
                this.settingsElement = null;
                return true;
            }

            if (this.handleSettingsPopupClick(mouseX, mouseY, button)) {
                return true;
            }
        }

        for (int i = HUDManager.getInstance().getElements().size() - 1; i >= 0; i--) {
            HUDElement element = HUDManager.getInstance().getElements().get(i);
            class_768 bounds = new class_768((int)element.getX(), (int)element.getY(), (int)element.getWidth(), (int)element.getHeight());
            if (this.isMouseOver(mouseX, mouseY, bounds)) {
                if (button == 0) {
                    this.draggingElement = element;
                    element.startDragging(mouseX, mouseY);
                } else if (button == 1) {
                    if (this.isShiftDown()) {
                        this.settingsElement = this.settingsElement == element ? null : element;
                    } else {
                        element.setEnabled(!element.isEnabled());
                    }
                }

                return true;
            }
        }

        return super.method_25402(click, doubleClick);
    }

    private boolean handleSettingsPopupClick(double mouseX, double mouseY, int button) {
        if (button == 0 && this.settingsElement != null) {
            List<Setting> settings = this.settingsElement.getSettings();
            if (settings != null && !settings.isEmpty()) {
                int popupWidth = 180;
                int popupX = (int)(this.settingsElement.getX() + this.settingsElement.getWidth() + 8.0);
                if (popupX + popupWidth > this.field_22789) {
                    popupX = (int)(this.settingsElement.getX() - popupWidth - 8.0);
                }

                int popupY = (int)this.settingsElement.getY();
                float currentY = popupY + 8;
                int itemHeight = 18;
                int sliderSectionHeight = 28;

                for (Setting setting : settings) {
                    if (setting instanceof BooleanSetting boolSetting) {
                        if (this.isMouseOver(mouseX, mouseY, new class_768(popupX + 5, (int)currentY, popupWidth - 10, itemHeight - 2))) {
                            boolSetting.setEnabled(!boolSetting.isEnabled());
                            return true;
                        }

                        currentY += itemHeight;
                    }
                }

                currentY += 5.0F;

                for (Setting settingx : settings) {
                    if (settingx instanceof SliderSetting sliderSetting) {
                        currentY += 14.0F;
                        class_768 sliderBounds = new class_768(popupX + 10, (int)currentY - 2, popupWidth - 20, 10);
                        if (this.isMouseOver(mouseX, mouseY, sliderBounds)) {
                            this.draggingSliders.put(settingx, true);
                            this.updateSlider(sliderSetting, mouseX);
                            return true;
                        }

                        currentY += 14.0F;
                    }
                }

                return false;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean method_25403(class_11909 click, double deltaX, double deltaY) {
        double mouseX = click.comp_4798();
        double mouseY = click.comp_4799();
        if (this.draggingElement != null) {
            this.draggingElement.mouseDragged(mouseX, mouseY);
            return true;
        } else {
            for (Entry<Setting, Boolean> entry : this.draggingSliders.entrySet()) {
                if (entry.getValue() && entry.getKey() instanceof SliderSetting sliderSetting) {
                    this.updateSlider(sliderSetting, mouseX);
                    return true;
                }
            }

            return super.method_25403(click, deltaX, deltaY);
        }
    }

    public boolean method_25406(class_11909 click) {
        double mouseX = click.comp_4798();
        double mouseY = click.comp_4799();
        int button = click.method_74245();
        if (this.draggingElement != null && button == 0) {
            this.draggingElement.stopDragging();
            this.draggingElement = null;
        }

        this.draggingSliders.clear();
        return super.method_25406(click);
    }

    private void updateSlider(SliderSetting slider, double mouseX) {
        if (this.settingsElement != null) {
            int popupWidth = 180;
            int popupX = (int)(this.settingsElement.getX() + this.settingsElement.getWidth() + 8.0);
            if (popupX + popupWidth > this.field_22789) {
                popupX = (int)(this.settingsElement.getX() - popupWidth - 8.0);
            }

            double percent = (mouseX - (popupX + 10)) / (popupWidth - 20);
            percent = Math.max(0.0, Math.min(1.0, percent));
            double range = slider.getMax() - slider.getMin();
            double newValue = slider.getMin() + percent * range;
            slider.setValue(newValue);
        }
    }

    public boolean method_25404(class_11908 keyInput) {
        int keyCode = keyInput.comp_4795();
        if (keyCode == 256) {
            this.method_25419();
            return true;
        } else {
            return super.method_25404(keyInput);
        }
    }

    public void method_25419() {
        Cyemer.INSTANCE.getConfigManager().save("default");
        this.field_22787.method_1507(this.parent);
    }

    public boolean method_25421() {
        return false;
    }

    private boolean isMouseOver(double mouseX, double mouseY, class_768 rect) {
        return rect != null
            && mouseX >= rect.method_3321()
            && mouseX <= rect.method_3321() + rect.method_3319()
            && mouseY >= rect.method_3322()
            && mouseY <= rect.method_3322() + rect.method_3320();
    }

    private boolean isMouseOverSettings(double mouseX, double mouseY) {
        if (this.settingsElement == null) {
            return false;
        } else {
            List<Setting> settings = this.settingsElement.getSettings();
            if (settings != null && !settings.isEmpty()) {
                int toggleCount = 0;
                int sliderCount = 0;

                for (Setting setting : settings) {
                    if (setting instanceof BooleanSetting) {
                        toggleCount++;
                    } else if (setting instanceof SliderSetting) {
                        sliderCount++;
                    }
                }

                int popupWidth = 180;
                int itemHeight = 18;
                int sliderSectionHeight = 28;
                int popupHeight = 10 + toggleCount * itemHeight + 5 + sliderCount * sliderSectionHeight + 5;
                int popupX = (int)(this.settingsElement.getX() + this.settingsElement.getWidth() + 8.0);
                int popupY = (int)this.settingsElement.getY();
                if (popupX + popupWidth > this.field_22789) {
                    popupX = (int)(this.settingsElement.getX() - popupWidth - 8.0);
                }

                return this.isMouseOver(mouseX, mouseY, new class_768(popupX, popupY, popupWidth, popupHeight));
            } else {
                return false;
            }
        }
    }
}
