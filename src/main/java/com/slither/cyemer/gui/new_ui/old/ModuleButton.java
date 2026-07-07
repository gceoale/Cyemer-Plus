package com.slither.cyemer.gui.new_ui.old;

import com.slither.cyemer.gui.new_ui.settings.BooleanComponent;
import com.slither.cyemer.gui.new_ui.settings.ColorComponent;
import com.slither.cyemer.gui.new_ui.settings.KeybindComponent;
import com.slither.cyemer.gui.new_ui.settings.ModeComponent;
import com.slither.cyemer.gui.new_ui.settings.SettingComponent;
import com.slither.cyemer.gui.new_ui.settings.SliderComponent;
import com.slither.cyemer.gui.new_ui.settings.StringComponent;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.ColorSetting;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.module.StringSetting;
import com.slither.cyemer.module.implementation.ClickGUIModule;
import com.slither.cyemer.module.implementation.KeybindSetting;
import com.slither.cyemer.util.GuiShaderStyle;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public class ModuleButton {
    public final Module module;
    private final List<SettingComponent> components = new ArrayList<>();
    public double x;
    public double y;
    public double width;
    public double height;
    private boolean expanded = false;
    public boolean isLastButton = false;
    private double currentSettingHeight = 0.0;
    private double hoverAnimation = 0.0;
    private long lastAnimationNanos = -1L;

    public ModuleButton(Module module) {
        this.module = module;
        module.getSettings().forEach(s -> {
            if (s instanceof BooleanSetting bs) {
                this.components.add(new BooleanComponent(bs));
            } else if (s instanceof ModeSetting ms) {
                this.components.add(new ModeComponent(ms));
            } else if (s instanceof SliderSetting ss) {
                this.components.add(new SliderComponent(ss));
            } else if (s instanceof ColorSetting cs) {
                this.components.add(new ColorComponent(cs));
            } else if (s instanceof StringSetting sts) {
                this.components.add(new StringComponent(sts));
            } else if (s instanceof KeybindSetting ks) {
                this.components.add(new KeybindComponent(ks));
            }
        });
    }

    public String render(class_332 context, int mouseX, int mouseY, float delta, double alpha) {
        String descriptionToRender = null;
        double realContentHeight = this.getSettingsHeight();
        boolean hovered = this.isButtonHovered(mouseX, mouseY);
        double targetHeight = this.expanded ? realContentHeight : 0.0;
        double frameDeltaSeconds = this.getAnimationDeltaSeconds();
        boolean hoverEffectsEnabled = ClickGUIModule.useHover();
        double hoverTarget = hoverEffectsEnabled && hovered ? 1.0 : 0.0;
        this.hoverAnimation = this.hoverAnimation + (hoverTarget - this.hoverAnimation) * (1.0 - Math.exp(-20.0 * frameDeltaSeconds));
        if (Math.abs(this.hoverAnimation - hoverTarget) < 0.001) {
            this.hoverAnimation = hoverTarget;
        }

        if (Math.abs(this.currentSettingHeight - targetHeight) > 0.01) {
            this.currentSettingHeight = this.currentSettingHeight + (targetHeight - this.currentSettingHeight) * (1.0 - Math.exp(-16.0 * frameDeltaSeconds));
        } else {
            this.currentSettingHeight = targetHeight;
        }

        double clampedSettingHeight = Math.max(0.0, this.currentSettingHeight);
        if (hovered) {
            descriptionToRender = this.module.getDescription();
        }

        boolean bottomRoundedRow = this.isLastButton && clampedSettingHeight <= 0.5;
        if (this.module.isEnabled()) {
            this.drawHorizontalGradient(context, alpha, bottomRoundedRow, this.hoverAnimation);
        } else {
            this.drawDisabledGlass(context, alpha, bottomRoundedRow, this.hoverAnimation);
        }

        float fontSize = 10.0F;
        float textHeight = Renderer.get().getTextHeight(fontSize);
        float textY = (float)(this.y + (this.height - textHeight) / 2.0);
        String moduleName = this.module.getName();
        Color textColor = this.module.isEnabled()
            ? ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledText(), alpha)
            : ClickGUIModule.getColor(ClickGUIModule.getModuleDisabledText(), alpha);
        Renderer.get().drawText(context, moduleName, (float)this.x + 8.0F, textY, fontSize, textColor, false);
        if (clampedSettingHeight > 0.5) {
            double settingY = this.y + this.height + 3.0;
            Color settingTextColor = ClickGUIModule.getColor(ClickGUIModule.getSettingsText(), alpha);
            double visibleHeight = Math.min(clampedSettingHeight, realContentHeight);
            double settingsAlpha = alpha * Math.min(1.0, clampedSettingHeight / Math.max(1.0, realContentHeight));
            Renderer.get().scissor(context, (float)this.x, (float)(this.y + this.height), (float)this.width, (float)visibleHeight);
            this.drawSettingsGlass(context, settingsAlpha, visibleHeight, this.isLastButton);

            for (SettingComponent comp : this.components) {
                if (!(comp instanceof BooleanComponent)) {
                    String title = comp.setting.getName();
                    if (comp.setting instanceof SliderSetting sliderSetting) {
                        title = title + ": " + round(sliderSetting.getPreciseValue(), 2);
                    }

                    if (settingY < this.y + this.height + visibleHeight) {
                        Renderer.get().drawText(context, title, (float)(this.x + 10.0), (float)settingY, 10.0F, settingTextColor, false);
                    }

                    settingY += 14.0;
                }

                comp.x = this.x + 5.0;
                comp.y = settingY;
                comp.width = this.width - 10.0;
                comp.height = comp.getComponentHeight();
                if (settingY < this.y + this.height + visibleHeight) {
                    Renderer.get().scissor(context, (float)this.x, (float)(this.y + this.height), (float)this.width, (float)visibleHeight);
                    comp.render(context, mouseX, mouseY, (float)frameDeltaSeconds, settingsAlpha);
                }

                settingY += comp.height + 4.0;
            }

            Renderer.get().resetScissor();
        }

        return descriptionToRender;
    }

    private void drawSettingsGlass(class_332 context, double alpha, double customHeight, boolean bottomRounded) {
        if (!(customHeight <= 0.0)) {
            float settingsX = this.snap(this.x);
            float settingsY = this.snap(this.y + this.height);
            float settingsWidth = this.snapSize(this.x, this.width, settingsX);
            float settingsHeight = this.snapSize(this.y + this.height, customHeight, settingsY);
            if (!(settingsWidth <= 0.0F) && !(settingsHeight <= 0.0F)) {
                boolean gradientMenus = ClickGUIModule.useGradientMenus() && this.module.isEnabled();
                Color lightBg = ClickGUIModule.getColor(ClickGUIModule.getSettingsBackground(), alpha);
                Color gradientStart = ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientStart(), alpha * 0.92);
                Color gradientEnd = ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientEnd(), alpha * 0.92);
                Color glassOverlay = new Color(255, 255, 255, (int)(6.0 * alpha));
                if (bottomRounded) {
                    float bottomRadius = this.computeBottomRadius(settingsWidth, settingsHeight);
                    if (gradientMenus) {
                        Renderer.get()
                            .drawRoundedRectGradientStyled(
                                context,
                                settingsX,
                                settingsY,
                                settingsWidth,
                                settingsHeight,
                                bottomRadius,
                                gradientStart,
                                gradientEnd,
                                false,
                                GuiShaderStyle.ROW_LAST
                            );
                    } else {
                        Renderer.get()
                            .drawRoundedRectStyled(context, settingsX, settingsY, settingsWidth, settingsHeight, bottomRadius, lightBg, GuiShaderStyle.ROW_LAST);
                    }
                } else {
                    if (gradientMenus) {
                        Renderer.get()
                            .drawRoundedRectGradient(context, settingsX, settingsY, settingsWidth, settingsHeight, 0.0F, gradientStart, gradientEnd, false);
                    } else {
                        Renderer.get().drawRect(context, settingsX, settingsY, settingsWidth, settingsHeight, lightBg);
                    }

                    Renderer.get().drawRect(context, settingsX, settingsY, settingsWidth, settingsHeight, glassOverlay);
                }
            }
        }
    }

    private void drawHorizontalGradient(class_332 context, double alpha, boolean bottomRounded, double hoverAnim) {
        Color startColor = brighten(ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientStart(), alpha), hoverAnim * 0.15);
        Color endColor = brighten(ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientEnd(), alpha), hoverAnim * 0.15);
        boolean useGradient = !ClickGUIModule.useCustomHighlight() || ClickGUIModule.useHighlightGradient();
        float rowX = this.snap(this.x);
        float rowY = this.snap(this.y);
        float rowWidth = this.snapSize(this.x, this.width, rowX);
        float rowHeight = this.snapSize(this.y, this.height, rowY);
        if (!(rowWidth <= 0.0F) && !(rowHeight <= 0.0F)) {
            if (bottomRounded) {
                float bottomRadius = this.computeBottomRadius(rowWidth, rowHeight);
                if (useGradient) {
                    Renderer.get()
                        .drawRoundedRectGradientStyled(
                            context, rowX, rowY, rowWidth, rowHeight, bottomRadius, startColor, endColor, false, GuiShaderStyle.ROW_LAST
                        );
                } else {
                    Renderer.get().drawRoundedRectStyled(context, rowX, rowY, rowWidth, rowHeight, bottomRadius, startColor, GuiShaderStyle.ROW_LAST);
                }
            } else {
                if (useGradient) {
                    Renderer.get().drawRoundedRectGradient(context, rowX, rowY, rowWidth, rowHeight, 0.0F, startColor, endColor, false);
                } else {
                    Renderer.get().drawRect(context, rowX, rowY, rowWidth, rowHeight, startColor);
                }
            }
        }
    }

    private void drawDisabledGlass(class_332 context, double alpha, boolean bottomRounded, double hoverAnim) {
        float hover = (float)Math.max(0.0, Math.min(1.0, hoverAnim));
        if (!(hover < 0.025F) || this.expanded) {
            Color base = brighten(ClickGUIModule.getColor(ClickGUIModule.getPanelBackground(), alpha), hover * 0.12);
            int softAlpha = (int)(base.getAlpha() * (0.18F + hover * 0.45F));
            Color panelColor = new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.max(0, Math.min(255, softAlpha)));
            float rowX = this.snap(this.x);
            float rowY = this.snap(this.y);
            float rowWidth = this.snapSize(this.x, this.width, rowX);
            float rowHeight = this.snapSize(this.y, this.height, rowY);
            if (!(rowWidth <= 0.0F) && !(rowHeight <= 0.0F)) {
                if (bottomRounded) {
                    float bottomRadius = this.computeBottomRadius(rowWidth, rowHeight);
                    Renderer.get().drawRoundedRectStyled(context, rowX, rowY, rowWidth, rowHeight, bottomRadius, panelColor, GuiShaderStyle.ROW_LAST);
                } else {
                    Renderer.get().drawRect(context, rowX, rowY, rowWidth, rowHeight, panelColor);
                }
            }
        }
    }

    private float computeBottomRadius(float drawW, float drawH) {
        float maxRadius = Math.max(0.0F, Math.min(drawW * 0.5F, drawH * 0.55F));
        return Math.max(0.0F, Math.min(ClickGUIModule.getGuiCornerRadiusScaled(1.0), maxRadius));
    }

    public static Color brighten(Color c, double amount) {
        if (amount <= 0.001) {
            return c;
        } else {
            int r = Math.min(255, (int)(c.getRed() + (255 - c.getRed()) * amount));
            int g = Math.min(255, (int)(c.getGreen() + (255 - c.getGreen()) * amount));
            int b = Math.min(255, (int)(c.getBlue() + (255 - c.getBlue()) * amount));
            return new Color(r, g, b, c.getAlpha());
        }
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isButtonHovered(mouseX, mouseY)) {
            if (button == 0) {
                this.module.forceToggle();
            } else if (button == 1 && !this.components.isEmpty()) {
                this.expanded = !this.expanded;
                if (!this.expanded) {
                    for (SettingComponent component : this.components) {
                        if (component instanceof ColorComponent colorComponent) {
                            colorComponent.setExpanded(false);
                        }
                    }
                }
            }
        } else if (this.expanded && this.currentSettingHeight > 0.0) {
            this.components.forEach(c -> c.mouseClicked(mouseX, mouseY, button));
        }
    }

    public double getTotalHeight() {
        return this.height + Math.max(0.0, this.currentSettingHeight);
    }

    public double getSettingsHeight() {
        if (this.components.isEmpty()) {
            return 0.0;
        } else {
            double h = 5.0;

            for (SettingComponent c : this.components) {
                if (!(c instanceof BooleanComponent)) {
                    h += 13.0;
                }

                h += c.getComponentHeight() + 3.0;
            }

            return h + 3.0;
        }
    }

    private boolean isButtonHovered(double mouseX, double mouseY) {
        if (mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height) {
            boolean bottomRoundedRow = this.isLastButton && this.currentSettingHeight <= 2.0;
            if (!bottomRoundedRow) {
                return true;
            } else {
                double radius = Math.min((double)ClickGUIModule.getGuiCornerRadiusScaled(1.0), Math.min(this.width * 0.5, this.height * 0.55));
                if (radius <= 0.5) {
                    return true;
                } else {
                    double relX = mouseX - this.x;
                    double relY = mouseY - this.y;
                    double cornerStartY = this.height - radius;
                    if (relY <= cornerStartY) {
                        return true;
                    } else if (relX < radius) {
                        double dx = relX - radius;
                        double dy = relY - cornerStartY;
                        return dx * dx + dy * dy <= radius * radius;
                    } else if (relX > this.width - radius) {
                        double dx = relX - (this.width - radius);
                        double dy = relY - cornerStartY;
                        return dx * dx + dy * dy <= radius * radius;
                    } else {
                        return true;
                    }
                }
            }
        } else {
            return false;
        }
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.expanded) {
            this.components.forEach(c -> c.keyPressed(keyCode, scanCode, modifiers));
        }
    }

    public void charTyped(char chr, int modifiers) {
        if (this.expanded) {
            this.components.forEach(c -> c.charTyped(chr, modifiers));
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (this.expanded) {
            this.components.forEach(c -> c.mouseReleased(mouseX, mouseY, button));
        }
    }

    private static String round(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        } else {
            BigDecimal bd = new BigDecimal(Double.toString(value));
            bd = bd.setScale(places, RoundingMode.HALF_UP);
            return bd.toString();
        }
    }

    public Module getModule() {
        return this.module;
    }

    private double getAnimationDeltaSeconds() {
        long now = System.nanoTime();
        if (this.lastAnimationNanos < 0L) {
            this.lastAnimationNanos = now;
            return 0.008333333333333333;
        } else {
            double dt = (now - this.lastAnimationNanos) / 1.0E9;
            this.lastAnimationNanos = now;
            return Math.max(0.004166666666666667, Math.min(0.05, dt <= 0.0 ? 0.004166666666666667 : dt));
        }
    }

    private float snap(double value) {
        return (float)Math.round(value);
    }

    private float snapSize(double start, double size, float snappedStart) {
        float snappedEnd = (float)Math.round(start + size);
        return Math.max(0.0F, snappedEnd - snappedStart);
    }

    public List<SettingComponent> getComponents() {
        return this.components;
    }
}
