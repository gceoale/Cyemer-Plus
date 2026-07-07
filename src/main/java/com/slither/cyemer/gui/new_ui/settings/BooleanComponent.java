package com.slither.cyemer.gui.new_ui.settings;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.implementation.ClickGUIModule;
import com.slither.cyemer.util.GuiShaderStyle;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public class BooleanComponent extends SettingComponent {
    private double toggleAnimation = 0.0;

    public BooleanComponent(BooleanSetting setting) {
        super(setting);
        this.toggleAnimation = setting.isEnabled() ? 1.0 : 0.0;
    }

    @Override
    public void render(class_332 context, int mouseX, int mouseY, float delta, double alpha) {
        BooleanSetting boolSet = (BooleanSetting)this.setting;
        double dt = Math.max(0.001, Math.min((double)delta, 0.05));
        double target = boolSet.isEnabled() ? 1.0 : 0.0;
        this.toggleAnimation = this.toggleAnimation + (target - this.toggleAnimation) * (1.0 - Math.exp(-30.0 * dt));
        if (Math.abs(this.toggleAnimation - target) < 0.001) {
            this.toggleAnimation = target;
        }

        Color bgColor = ClickGUIModule.getColor(ClickGUIModule.getSettingsBackground(), alpha * 0.3);
        Renderer.get().drawRoundedRectStyled(context, (float)this.x, (float)this.y, (float)this.width, (float)this.height, 4.0F, bgColor, GuiShaderStyle.ROW);
        Color textColor = ClickGUIModule.getColor(ClickGUIModule.getSettingsText(), alpha);
        float fontSize = 9.0F;
        float textY = (float)(this.y + (this.height - Renderer.get().getTextHeight(fontSize)) / 2.0);
        Renderer.get().drawText(context, this.setting.getName(), (float)this.x + 6.0F, textY, fontSize, textColor, false);
        float trackWidth = 28.0F;
        float trackHeight = 11.0F;
        float trackX = (float)(this.x + this.width - trackWidth - 6.0);
        float trackY = (float)(this.y + (this.height - trackHeight) / 2.0);
        float trackRadius = trackHeight / 2.0F;
        Color trackOff = ClickGUIModule.getColor(ClickGUIModule.getSettingsBackground(), alpha * 0.8);
        Color trackOn = ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientStart(), alpha);
        Color trackColor = lerpColor(trackOff, trackOn, this.toggleAnimation);
        Renderer.get().drawRoundedRectStyled(context, trackX, trackY, trackWidth, trackHeight, trackRadius, trackColor, GuiShaderStyle.CONTROL);
        float handleSize = 7.0F;
        float handlePadding = 2.0F;
        float handleX = trackX + handlePadding + (float)(this.toggleAnimation * (trackWidth - handleSize - handlePadding * 2.0F));
        float handleY = trackY + (trackHeight - handleSize) / 2.0F;
        float handleRadius = handleSize / 2.0F;
        if (ClickGUIModule.useShadows() && this.toggleAnimation > 0.1 && alpha > 0.1) {
            Color glowBase = ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientEnd(), alpha);
            Color glowColor = new Color(glowBase.getRed(), glowBase.getGreen(), glowBase.getBlue(), clampAlpha((int)(40.0 * alpha * this.toggleAnimation)));
            Renderer.get().drawGlowingRect(context, handleX, handleY, handleSize, handleSize, handleRadius, glowColor, new Color(0, 0, 0, 0), 4.0F);
        }

        Color handle = new Color(255, 255, 255, clampAlpha((int)(255.0 * alpha)));
        Renderer.get().drawRoundedRectStyled(context, handleX, handleY, handleSize, handleSize, handleRadius, handle, GuiShaderStyle.CONTROL);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isHovered(mouseX, mouseY) && button == 0) {
            ((BooleanSetting)this.setting).toggle();
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    @Override
    public double getComponentHeight() {
        return 16.0;
    }

    private static Color lerpColor(Color a, Color b, double t) {
        double c = Math.max(0.0, Math.min(1.0, t));
        return new Color(
            clampAlpha((int)(a.getRed() + (b.getRed() - a.getRed()) * c)),
            clampAlpha((int)(a.getGreen() + (b.getGreen() - a.getGreen()) * c)),
            clampAlpha((int)(a.getBlue() + (b.getBlue() - a.getBlue()) * c)),
            clampAlpha((int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * c))
        );
    }

    private static int clampAlpha(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
