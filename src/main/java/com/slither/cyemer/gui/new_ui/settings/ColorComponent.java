package com.slither.cyemer.gui.new_ui.settings;

import com.slither.cyemer.module.ColorSetting;
import com.slither.cyemer.module.implementation.ClickGUIModule;
import com.slither.cyemer.util.GuiShaderStyle;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;
import net.minecraft.class_3532;

@Environment(EnvType.CLIENT)
public class ColorComponent extends SettingComponent {
    private final ColorSetting colorSetting;
    private boolean expanded = false;
    private double expandAnimation = 0.0;
    private boolean draggingH = false;
    private boolean draggingS = false;
    private boolean draggingV = false;
    private boolean draggingA = false;
    private float hue;
    private float saturation;
    private float brightness;
    private float alpha;

    public ColorComponent(ColorSetting setting) {
        super(setting);
        this.colorSetting = setting;
        Color c = setting.getValue();
        float[] hsv = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.brightness = hsv[2];
        this.alpha = c.getAlpha() / 255.0F;
    }

    @Override
    public void render(class_332 context, int mouseX, int mouseY, float delta, double alpha) {
        double dt = Math.max(0.001, Math.min((double)delta, 0.05));
        if (!this.draggingH && !this.draggingS && !this.draggingV && !this.draggingA) {
            this.syncFromSettingColor();
        }

        double target = this.expanded ? 1.0 : 0.0;
        this.expandAnimation = this.expandAnimation + (target - this.expandAnimation) * (1.0 - Math.exp(-24.0 * dt));
        if (Math.abs(this.expandAnimation - target) < 0.002) {
            this.expandAnimation = target;
        }

        Color currentColor = this.getColor();
        Color borderColor = ClickGUIModule.getColor(ClickGUIModule.getSettingsBackground(), alpha * 0.8);
        Renderer.get().drawRoundedRectOutline(context, (float)this.x, (float)this.y, (float)this.width, 16.0F, 4.0F, 1.0F, borderColor);
        Renderer.get().drawRoundedRectStyled(context, (float)this.x, (float)this.y, (float)this.width, 16.0F, 4.0F, currentColor, GuiShaderStyle.CONTROL);
        if (this.expandAnimation > 0.01) {
            double pickerY = this.y + 18.0;
            double pickerHeight = 108.0 * this.expandAnimation;
            Renderer.get().scissor(context, (float)this.x, (float)pickerY, (float)this.width, (float)pickerHeight);
            this.renderSBBox(context, pickerY, mouseX, mouseY, alpha * this.expandAnimation);
            this.renderHueStrip(context, pickerY, mouseX, mouseY, alpha * this.expandAnimation);
            this.renderAlphaStrip(context, pickerY + 90.0, mouseX, mouseY, alpha * this.expandAnimation);
            Renderer.get().resetScissor();
        }
    }

    private void renderSBBox(class_332 context, double baseY, int mouseX, int mouseY, double alpha) {
        float boxWidth = (float)this.width - 20.0F;
        float boxHeight = 85.0F;
        float boxX = (float)this.x;
        float boxY = (float)baseY;
        if (this.draggingS || this.draggingV) {
            this.saturation = class_3532.method_15363((mouseX - boxX) / boxWidth, 0.0F, 1.0F);
            this.brightness = 1.0F - class_3532.method_15363((mouseY - boxY) / boxHeight, 0.0F, 1.0F);
            this.updateColor();
        }

        Color hueColor = Color.getHSBColor(this.hue, 1.0F, 1.0F);
        Color satTrans = new Color(255, 255, 255, 0);
        Color briTrans = new Color(0, 0, 0, 0);
        Renderer.get().drawRoundedRectOutline(context, boxX, boxY, boxWidth, boxHeight, 3.0F, 1.0F, new Color(0, 0, 0, (int)(100.0 * alpha)));
        Renderer.get().drawRoundedRectStyled(context, boxX, boxY, boxWidth, boxHeight, 3.0F, Color.WHITE, GuiShaderStyle.CONTROL);
        Renderer.get().drawRoundedRectGradient(context, boxX, boxY, boxWidth, boxHeight, 3.0F, satTrans, hueColor, false);
        Renderer.get().drawRoundedRectGradient(context, boxX, boxY, boxWidth, boxHeight, 3.0F, briTrans, Color.BLACK, true);
        float cursorX = boxX + this.saturation * boxWidth;
        float cursorY = boxY + (1.0F - this.brightness) * boxHeight;
        float r = 3.5F;
        Renderer.get().drawRoundedRectStyled(context, cursorX - r, cursorY - r, r * 2.0F, r * 2.0F, r, Color.WHITE, GuiShaderStyle.CONTROL);
        Renderer.get().drawRoundedRectOutline(context, cursorX - r, cursorY - r, r * 2.0F, r * 2.0F, r, 1.0F, Color.BLACK);
    }

    private void renderHueStrip(class_332 context, double baseY, int mouseX, int mouseY, double alpha) {
        float stripWidth = 15.0F;
        float stripHeight = 85.0F;
        float stripX = (float)(this.x + this.width - stripWidth);
        float stripY = (float)baseY;
        if (this.draggingH) {
            this.hue = class_3532.method_15363((mouseY - stripY) / stripHeight, 0.0F, 1.0F);
            this.updateColor();
        }

        Renderer.get().drawRoundedRectOutline(context, stripX, stripY, stripWidth, stripHeight, 2.0F, 1.0F, new Color(0, 0, 0, (int)(100.0 * alpha)));
        Color[] rainbow = new Color[]{Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED};
        float segmentH = stripHeight / (rainbow.length - 1);

        for (int i = 0; i < rainbow.length - 1; i++) {
            float curY = stripY + i * segmentH;
            Renderer.get().drawRoundedRectGradient(context, stripX, curY, stripWidth, segmentH + 1.0F, 0.0F, rainbow[i], rainbow[i + 1], true);
        }

        float cursorY = class_3532.method_15363(stripY + this.hue * stripHeight, stripY + 1.0F, stripY + stripHeight - 1.0F);
        Renderer.get().drawRect(context, stripX, cursorY - 1.0F, stripWidth, 2.0F, new Color(0, 0, 0, (int)(150.0 * alpha)));
        Renderer.get().drawRect(context, stripX, cursorY - 0.5F, stripWidth, 1.0F, new Color(255, 255, 255, (int)(255.0 * alpha)));
    }

    private void renderAlphaStrip(class_332 context, double baseY, int mouseX, int mouseY, double alpha) {
        float stripWidth = (float)this.width;
        float stripHeight = 8.0F;
        float stripX = (float)this.x;
        float stripY = (float)baseY;
        if (this.draggingA) {
            this.alpha = class_3532.method_15363((mouseX - stripX) / stripWidth, 0.0F, 1.0F);
            this.updateColor();
        }

        Color fullColor = Color.getHSBColor(this.hue, this.saturation, this.brightness);
        Color transColor = new Color(fullColor.getRed(), fullColor.getGreen(), fullColor.getBlue(), 0);
        Renderer.get().drawRoundedRectGradient(context, stripX, stripY, stripWidth, stripHeight, 2.0F, transColor, fullColor, false);
        float cursorX = class_3532.method_15363(stripX + this.alpha * stripWidth, stripX + 0.5F, stripX + stripWidth - 0.5F);
        Renderer.get().drawRect(context, cursorX - 0.5F, stripY, 1.0F, stripHeight, new Color(255, 255, 255, (int)(255.0 * alpha)));
    }

    private Color getColor() {
        Color c = Color.getHSBColor(this.hue, this.saturation, this.brightness);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(this.alpha * 255.0F));
    }

    private void syncFromSettingColor() {
        Color settingColor = this.colorSetting.getValue();
        if (settingColor != null) {
            Color current = this.getColor();
            if (current.getRGB() != settingColor.getRGB() || current.getAlpha() != settingColor.getAlpha()) {
                float[] hsv = Color.RGBtoHSB(settingColor.getRed(), settingColor.getGreen(), settingColor.getBlue(), null);
                this.hue = hsv[0];
                this.saturation = hsv[1];
                this.brightness = hsv[2];
                this.alpha = settingColor.getAlpha() / 255.0F;
            }
        }
    }

    private void updateColor() {
        this.colorSetting.setValue(this.getColor());
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + 16.0) {
                this.expanded = !this.expanded;
            } else {
                if (this.expanded && this.expandAnimation > 0.5) {
                    double pickerY = this.y + 18.0;
                    float boxWidth = (float)this.width - 20.0F;
                    float boxHeight = 85.0F;
                    if (mouseX >= this.x && mouseX <= this.x + boxWidth && mouseY >= pickerY && mouseY <= pickerY + boxHeight) {
                        this.draggingS = true;
                        this.draggingV = true;
                        return;
                    }

                    float stripX = (float)(this.x + this.width - 15.0);
                    if (mouseX >= stripX && mouseX <= stripX + 15.0F && mouseY >= pickerY && mouseY <= pickerY + boxHeight) {
                        this.draggingH = true;
                        return;
                    }

                    double alphaY = pickerY + 90.0;
                    if (mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= alphaY && mouseY <= alphaY + 8.0) {
                        this.draggingA = true;
                    }
                }
            }
        }
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isAnimating() {
        return this.expandAnimation > 0.001 && this.expandAnimation < 0.999;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingH = false;
        this.draggingS = false;
        this.draggingV = false;
        this.draggingA = false;
    }

    @Override
    public double getComponentHeight() {
        return 16.0 + 108.0 * this.expandAnimation;
    }
}
