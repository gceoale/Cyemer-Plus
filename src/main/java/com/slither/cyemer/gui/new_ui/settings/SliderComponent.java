package com.slither.cyemer.gui.new_ui.settings;

import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.module.implementation.ClickGUIModule;
import com.slither.cyemer.util.GuiShaderStyle;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;
import net.minecraft.class_3532;

@Environment(EnvType.CLIENT)
public class SliderComponent extends SettingComponent {
    private boolean dragging = false;
    private final SliderSetting sliderSetting;
    private double displayPos;
    private double hoverAnimation = 0.0;

    public SliderComponent(SliderSetting setting) {
        super(setting);
        this.sliderSetting = setting;
        double range = setting.getMax() - setting.getMin();
        this.displayPos = range > 0.0 ? (setting.getPreciseValue() - setting.getMin()) / range : 0.0;
    }

    @Override
    public void render(class_332 context, int mouseX, int mouseY, float delta, double alpha) {
        double dt = Math.max(0.001, Math.min((double)delta, 0.05));
        boolean hovered = mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y - 2.0 && mouseY <= this.y + this.getComponentHeight() + 2.0;
        double hoverTarget = !hovered && !this.dragging ? 0.0 : 1.0;
        this.hoverAnimation = this.hoverAnimation + (hoverTarget - this.hoverAnimation) * (1.0 - Math.exp(-24.0 * dt));
        float handleSize = 7.0F + (float)(this.hoverAnimation * 1.6F);
        float handleRadius = handleSize / 2.0F;
        double paddedWidth = this.width - handleSize;
        if (this.dragging && paddedWidth > 0.0) {
            double pct = (mouseX - (this.x + handleSize / 2.0)) / paddedWidth;
            double val = this.sliderSetting.getMin() + class_3532.method_15350(pct, 0.0, 1.0) * (this.sliderSetting.getMax() - this.sliderSetting.getMin());
            this.sliderSetting.setValue(val);
        }

        double range = this.sliderSetting.getMax() - this.sliderSetting.getMin();
        double trueFill = range > 0.0 ? (this.sliderSetting.getPreciseValue() - this.sliderSetting.getMin()) / range : 0.0;
        double animSpeed = this.dragging ? 45.0 : 30.0;
        this.displayPos = this.displayPos + (trueFill - this.displayPos) * (1.0 - Math.exp(-animSpeed * dt));
        float centerY = (float)(this.y + this.getComponentHeight() / 2.0);
        float trackHeight = 3.5F + (float)(this.hoverAnimation * 1.0);
        float trackRadius = trackHeight / 2.0F;
        float trackY = centerY - trackHeight / 2.0F;
        Color trackColor = ClickGUIModule.getColor(ClickGUIModule.getSettingsBackground(), alpha * (0.72 + this.hoverAnimation * 0.08));
        Renderer.get().drawRoundedRectStyled(context, (float)this.x, trackY, (float)this.width, trackHeight, trackRadius, trackColor, GuiShaderStyle.CONTROL);
        double fillW = handleSize / 2.0 + paddedWidth * this.displayPos;
        if (fillW > 1.0) {
            Color gradStart = ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientStart(), alpha);
            Color gradEnd = ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientEnd(), alpha);
            Renderer.get()
                .drawRoundedRectGradientStyled(
                    context, (float)this.x, trackY, (float)fillW, trackHeight, trackRadius, gradStart, gradEnd, false, GuiShaderStyle.CONTROL
                );
        }

        double handleX = this.x + paddedWidth * this.displayPos;
        float handleY = centerY - handleSize / 2.0F;
        if ((this.hoverAnimation > 0.15 || this.dragging) && alpha > 0.1) {
            Color glowBase = ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientEnd(), alpha);
            int glowA = Math.max(0, Math.min(255, (int)(60.0 * alpha * this.hoverAnimation)));
            Color glowColor = new Color(glowBase.getRed(), glowBase.getGreen(), glowBase.getBlue(), glowA);
            Renderer.get().drawGlowingRect(context, (float)handleX, handleY, handleSize, handleSize, handleRadius, glowColor, new Color(0, 0, 0, 0), 5.0F);
        }

        Color handle = new Color(255, 255, 255, Math.max(0, Math.min(255, (int)(255.0 * alpha))));
        Renderer.get().drawRoundedRectStyled(context, (float)handleX, handleY, handleSize, handleSize, handleRadius, handle, GuiShaderStyle.CONTROL);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isHovered(mouseX, mouseY) && button == 0) {
            this.dragging = true;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.dragging = false;
        }
    }

    @Override
    public double getComponentHeight() {
        return 7.0;
    }
}
