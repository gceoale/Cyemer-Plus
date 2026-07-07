package com.slither.cyemer.gui.new_ui;

import com.slither.cyemer.module.implementation.ClickGUIModule;
import com.slither.cyemer.rendering.font.MSDFFontRenderer;
import com.slither.cyemer.util.GuiShaderStyle;
import com.slither.cyemer.util.VanillaRendererImpl;
import java.awt.Color;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_339;
import net.minecraft.class_6382;

@Environment(EnvType.CLIENT)
public class CyemerButton extends class_339 {
    private static final class_2960 FONT = class_2960.method_60655("dynamic_fps", "sans");
    private static final float FONT_SIZE = 1.4F;
    private final VanillaRendererImpl renderer;
    private final Consumer<CyemerButton> onPress;
    private float hoverAnim = 0.0F;
    private float clickAnim = 0.0F;

    public CyemerButton(int x, int y, int width, int height, String label, VanillaRendererImpl renderer, Consumer<CyemerButton> onPress) {
        super(x, y, width, height, class_2561.method_43470(label));
        this.renderer = renderer;
        this.onPress = onPress;
    }

    public void method_25348(class_11909 click, boolean doubled) {
        if (this.field_22763 && this.onPress != null) {
            this.clickAnim = 1.0F;
            this.onPress.accept(this);
        }
    }

    public void method_48579(class_332 context, int mouseX, int mouseY, float delta) {
        float hoverTarget = this.method_49606() && this.field_22763 ? 1.0F : 0.0F;
        this.hoverAnim = this.hoverAnim + (hoverTarget - this.hoverAnim) * Math.min(1.0F, delta * 0.2F);
        this.clickAnim = this.clickAnim * (float)Math.pow(0.85, delta);
        float press = this.clickAnim;
        float radius = Math.max(4.0F, Math.min(8.5F, ClickGUIModule.getGuiCornerRadiusScaled(0.42F)));
        Color bgBase = this.withAlpha(ClickGUIModule.getSearchBoxBackground(), 0.86);
        Color bgHover = this.withAlpha(ClickGUIModule.getSearchBoxFocusedBackground(), 0.96);
        Color accentA = this.withAlpha(ClickGUIModule.getModuleEnabledGradientStart(), 0.75);
        Color accentB = this.withAlpha(ClickGUIModule.getModuleEnabledGradientEnd(), 0.88);
        Color textIdle = this.withAlpha(ClickGUIModule.getModuleDisabledText(), 0.92);
        Color textHover = this.withAlpha(ClickGUIModule.getModuleEnabledText(), 1.0);
        Color bg = this.field_22763 ? this.lerpColor(bgBase, bgHover, this.hoverAnim) : this.withAlpha(bgBase, 0.45);
        Color border = this.field_22763 ? this.lerpColor(accentA, accentB, this.hoverAnim) : this.withAlpha(accentA, 0.42);
        int textRGB = this.field_22763 ? this.lerpColor(textIdle, textHover, this.hoverAnim).getRGB() : this.withAlpha(textIdle, 0.5).getRGB();
        float pixelRatio = 1.0F;
        class_310 client = class_310.method_1551();
        if (client != null) {
            pixelRatio = client.method_22683().method_4495() * ClickGUIModule.getUiResolutionScale();
        }

        this.renderer.beginFrame(context.method_51421(), context.method_51443(), pixelRatio);
        this.renderer
            .drawRoundedRectStyled(
                context, this.method_46426(), this.method_46427(), this.method_25368(), this.method_25364(), radius, bg, GuiShaderStyle.CONTROL
            );
        if (this.field_22763 && this.hoverAnim > 0.01F) {
            this.renderer
                .drawRoundedRectStyled(
                    context,
                    this.method_46426() + 1,
                    this.method_46427() + 1,
                    this.method_25368() - 2,
                    this.method_25364() / 2 - 1,
                    Math.max(2.0F, radius - 2.0F),
                    this.withAlpha(ClickGUIModule.getModuleEnabledGradientStart(), 0.16 * this.hoverAnim),
                    GuiShaderStyle.ROW_ENABLED
                );
        }

        if (press > 0.05F) {
            this.renderer
                .drawRoundedRectStyled(
                    context,
                    this.method_46426(),
                    this.method_46427(),
                    this.method_25368(),
                    this.method_25364(),
                    radius,
                    this.withAlpha(ClickGUIModule.getModuleEnabledGradientEnd(), 0.26 * press),
                    GuiShaderStyle.GLOW
                );
        }

        this.renderer.drawRoundedRectOutline(context, this.method_46426(), this.method_46427(), this.method_25368(), this.method_25364(), radius, 1.0F, border);
        this.renderer.endFrame();
        String label = this.method_25369().getString();
        float textW = MSDFFontRenderer.getTextWidth(FONT, label) * 1.4F;
        float textH = MSDFFontRenderer.getTextHeight(FONT) * 1.4F;
        float textX = this.method_46426() + (this.method_25368() - textW) / 2.0F;
        float textY = this.method_46427() + (this.method_25364() - textH) / 2.0F;
        MSDFFontRenderer.drawText(context, FONT, label, textX, textY, 1.4F, textRGB, false);
    }

    protected void method_47399(class_6382 builder) {
        this.method_37021(builder);
    }

    private Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        return new Color(
            (int)(a.getRed() + (b.getRed() - a.getRed()) * t),
            (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int)(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
            (int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t)
        );
    }

    private Color withAlpha(Color color, double alphaMul) {
        double clamped = Math.max(0.0, Math.min(1.0, alphaMul));
        int alpha = Math.max(0, Math.min(255, (int)Math.round(color.getAlpha() * clamped)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
