package com.slither.cyemer.gui.new_ui;

import com.slither.cyemer.module.ColorSetting;
import com.slither.cyemer.module.implementation.ClickGUIModule;
import com.slither.cyemer.theme.Theme;
import com.slither.cyemer.theme.ThemeManager;
import com.slither.cyemer.util.ICyemerRenderer;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_768;

@Environment(EnvType.CLIENT)
public class ColorPickerScreen extends class_437 {
    private final class_437 parent;
    private final ColorSetting colorSetting;
    private final Consumer<Color> onColorChange;
    private final Theme theme;
    private float hue;
    private float saturation;
    private float brightness;
    private float alpha;
    private class_768 panelBounds;
    private class_768 sbBox;
    private class_768 hueStrip;
    private class_768 alphaStrip;
    private class_768 previewBox;
    private boolean draggingSB;
    private boolean draggingHue;
    private boolean draggingAlpha;

    public ColorPickerScreen(class_437 parent, ColorSetting setting, Consumer<Color> onColorChange) {
        super(class_2561.method_43470("Select Color"));
        this.parent = parent;
        this.colorSetting = setting;
        this.onColorChange = onColorChange;
        this.theme = ThemeManager.getInstance().getCurrentTheme();
        Color initialColor = setting.getValue();
        float[] hsb = Color.RGBtoHSB(initialColor.getRed(), initialColor.getGreen(), initialColor.getBlue(), null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        this.alpha = initialColor.getAlpha() / 255.0F;
    }

    protected void method_25426() {
        int panelW = 260;
        int panelH = 200;
        int x = (this.field_22789 - panelW) / 2;
        int y = (this.field_22790 - panelH) / 2;
        this.panelBounds = new class_768(x, y, panelW, panelH);
        int padding = 15;
        int stripWidth = 15;
        int bottomHeight = 25;
        int alphaHeight = 8;
        int sbWidth = panelW - padding * 3 - stripWidth;
        int sbHeight = panelH - padding * 3 - alphaHeight - bottomHeight - 10;
        this.sbBox = new class_768(x + padding, y + padding + 15, sbWidth, sbHeight);
        this.hueStrip = new class_768(x + padding + sbWidth + padding, y + padding + 15, stripWidth, sbHeight);
        this.alphaStrip = new class_768(x + padding, this.sbBox.method_3322() + sbHeight + 10, panelW - padding * 2, alphaHeight);
        this.previewBox = new class_768(x + padding, this.alphaStrip.method_3322() + alphaHeight + 15, panelW - padding * 2, 15);
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        ICyemerRenderer renderer = Renderer.get();
        renderer.beginFrame(this.field_22789, this.field_22790, this.field_22787.method_22683().method_4495());
        renderer.drawRect(context, 0.0F, 0.0F, this.field_22789, this.field_22790, new Color(0, 0, 0, 100));
        this.drawPanel(renderer, context);
        this.drawSatBriBox(renderer, context);
        this.drawHueStrip(renderer, context);
        this.drawAlphaStrip(renderer, context);
        this.drawPreviewAndText(renderer, context);
        renderer.endFrame();
    }

    private void drawPanel(ICyemerRenderer renderer, class_332 context) {
        Color bg = new Color(25, 25, 25, 250);
        Color border = new Color(60, 60, 60);
        renderer.drawRoundedRect(
            context, this.panelBounds.method_3321(), this.panelBounds.method_3322(), this.panelBounds.method_3319(), this.panelBounds.method_3320(), 8.0F, bg
        );
        renderer.drawRoundedRectOutline(
            context,
            this.panelBounds.method_3321(),
            this.panelBounds.method_3322(),
            this.panelBounds.method_3319(),
            this.panelBounds.method_3320(),
            8.0F,
            1.0F,
            border
        );
        renderer.drawCenteredText(
            context,
            "Color Picker",
            this.panelBounds.method_3321() + this.panelBounds.method_3319() / 2.0F,
            this.panelBounds.method_3322() + 8,
            10.0F,
            Color.LIGHT_GRAY,
            ClickGUIModule.useShadows()
        );
    }

    private void drawSatBriBox(ICyemerRenderer renderer, class_332 context) {
        float x = this.sbBox.method_3321();
        float y = this.sbBox.method_3322();
        float w = this.sbBox.method_3319();
        float h = this.sbBox.method_3320();
        Color hueColor = Color.getHSBColor(this.hue, 1.0F, 1.0F);
        renderer.drawRect(context, x, y, w, h, Color.WHITE);
        renderer.drawRoundedRectGradient(context, x, y, w, h, 0.0F, new Color(255, 255, 255, 0), hueColor, false);
        renderer.drawRoundedRectGradient(context, x, y, w, h, 0.0F, new Color(0, 0, 0, 0), Color.BLACK, true);
        renderer.drawRectOutline(context, x, y, w, h, 1.0F, new Color(0, 0, 0, 100));
        float cursorX = x + this.saturation * w;
        float cursorY = y + (1.0F - this.brightness) * h;
        float r = 2.5F;
        renderer.drawRoundedRect(context, cursorX - r, cursorY - r, r * 2.0F, r * 2.0F, r, Color.WHITE);
        renderer.drawRoundedRectOutline(context, cursorX - r, cursorY - r, r * 2.0F, r * 2.0F, r, 1.0F, Color.BLACK);
    }

    private void drawHueStrip(ICyemerRenderer renderer, class_332 context) {
        float x = this.hueStrip.method_3321();
        float y = this.hueStrip.method_3322();
        float w = this.hueStrip.method_3319();
        float h = this.hueStrip.method_3320();
        Color[] rainbow = new Color[]{Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED};
        float segmentH = h / (rainbow.length - 1);

        for (int i = 0; i < rainbow.length - 1; i++) {
            float curY = y + i * segmentH;
            renderer.drawRoundedRectGradient(context, x, curY, w, segmentH + 1.0F, 0.0F, rainbow[i], rainbow[i + 1], true);
        }

        renderer.drawRectOutline(context, x, y, w, h, 1.0F, new Color(0, 0, 0, 100));
        float cursorY = y + this.hue * h;
        renderer.scissor(context, x, y, w, h);
        renderer.drawRect(context, x, cursorY - 1.0F, w, 2.0F, new Color(0, 0, 0, 150));
        renderer.drawRect(context, x, cursorY - 0.5F, w, 1.0F, Color.WHITE);
        renderer.resetScissor();
    }

    private void drawAlphaStrip(ICyemerRenderer renderer, class_332 context) {
        float x = this.alphaStrip.method_3321();
        float y = this.alphaStrip.method_3322();
        float w = this.alphaStrip.method_3319();
        float h = this.alphaStrip.method_3320();
        Color fullColor = Color.getHSBColor(this.hue, this.saturation, this.brightness);
        Color transColor = new Color(fullColor.getRed(), fullColor.getGreen(), fullColor.getBlue(), 0);
        renderer.drawRoundedRectGradient(context, x, y, w, h, 2.0F, transColor, fullColor, false);
        float cursorX = Math.max(x + 0.5F, Math.min(x + w - 0.5F, x + this.alpha * w));
        renderer.scissor(context, x, y, w, h);
        renderer.drawRect(context, cursorX - 0.5F, y, 1.0F, h, Color.WHITE);
        renderer.resetScissor();
    }

    private void drawPreviewAndText(ICyemerRenderer renderer, class_332 context) {
        Color c = this.getColor();
        String hex = String.format("#%02x%02x%02x%02x", c.getAlpha(), c.getRed(), c.getGreen(), c.getBlue()).toUpperCase();
        renderer.drawText(context, hex, this.previewBox.method_3321(), this.previewBox.method_3322() - 9, 8.0F, Color.GRAY, ClickGUIModule.useShadows());
        renderer.drawText(context, "Alpha", this.alphaStrip.method_3321(), this.alphaStrip.method_3322() - 8, 7.0F, Color.GRAY, ClickGUIModule.useShadows());
        renderer.drawRoundedRect(
            context,
            this.previewBox.method_3321(),
            this.previewBox.method_3322(),
            this.previewBox.method_3319(),
            this.previewBox.method_3320(),
            4.0F,
            new Color(50, 50, 50)
        );
        renderer.drawRoundedRect(
            context, this.previewBox.method_3321(), this.previewBox.method_3322(), this.previewBox.method_3319(), this.previewBox.method_3320(), 4.0F, c
        );
        renderer.drawRoundedRectOutline(
            context,
            this.previewBox.method_3321(),
            this.previewBox.method_3322(),
            this.previewBox.method_3319(),
            this.previewBox.method_3320(),
            4.0F,
            1.0F,
            new Color(100, 100, 100)
        );
    }

    private Color getColor() {
        Color c = Color.getHSBColor(this.hue, this.saturation, this.brightness);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(this.alpha * 255.0F));
    }

    private void updateColor() {
        this.onColorChange.accept(this.getColor());
    }

    public boolean method_25402(class_11909 click, boolean doubleClick) {
        double mouseX = click.comp_4798();
        double mouseY = click.comp_4799();
        int button = click.method_74245();
        if (button == 0) {
            if (this.sbBox.method_3318((int)mouseX, (int)mouseY)) {
                this.draggingSB = true;
                this.updateSB(mouseX, mouseY);
                return true;
            }

            if (this.hueStrip.method_3318((int)mouseX, (int)mouseY)) {
                this.draggingHue = true;
                this.updateHue(mouseY);
                return true;
            }

            if (this.alphaStrip.method_3318((int)mouseX, (int)mouseY)) {
                this.draggingAlpha = true;
                this.updateAlpha(mouseX);
                return true;
            }
        }

        return super.method_25402(click, doubleClick);
    }

    public boolean method_25403(class_11909 click, double deltaX, double deltaY) {
        double mouseX = click.comp_4798();
        double mouseY = click.comp_4799();
        if (this.draggingSB) {
            this.updateSB(mouseX, mouseY);
        } else if (this.draggingHue) {
            this.updateHue(mouseY);
        } else if (this.draggingAlpha) {
            this.updateAlpha(mouseX);
        }

        return super.method_25403(click, deltaX, deltaY);
    }

    public boolean method_25406(class_11909 click) {
        this.draggingSB = false;
        this.draggingHue = false;
        this.draggingAlpha = false;
        return super.method_25406(click);
    }

    private void updateSB(double mouseX, double mouseY) {
        this.saturation = (float)((mouseX - this.sbBox.method_3321()) / this.sbBox.method_3319());
        this.brightness = 1.0F - (float)((mouseY - this.sbBox.method_3322()) / this.sbBox.method_3320());
        this.clamp();
        this.updateColor();
    }

    private void updateHue(double mouseY) {
        this.hue = (float)((mouseY - this.hueStrip.method_3322()) / this.hueStrip.method_3320());
        this.clamp();
        this.updateColor();
    }

    private void updateAlpha(double mouseX) {
        this.alpha = (float)((mouseX - this.alphaStrip.method_3321()) / this.alphaStrip.method_3319());
        this.clamp();
        this.updateColor();
    }

    private void clamp() {
        this.hue = Math.max(0.0F, Math.min(1.0F, this.hue));
        this.saturation = Math.max(0.0F, Math.min(1.0F, this.saturation));
        this.brightness = Math.max(0.0F, Math.min(1.0F, this.brightness));
        this.alpha = Math.max(0.0F, Math.min(1.0F, this.alpha));
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
        this.field_22787.method_1507(this.parent);
    }
}
