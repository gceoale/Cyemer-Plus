package com.slither.cyemer.gui.new_ui.settings;

import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.implementation.ClickGUIModule;
import com.slither.cyemer.util.GuiShaderStyle;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public class ModeComponent extends SettingComponent {
    private static final float MODE_FONT_SIZE = 9.0F;
    private static final double MODE_HORIZONTAL_PADDING = 5.0;
    private static final double MODE_VERTICAL_PADDING = 3.0;
    private static final double MODE_OPTION_GAP_X = 3.0;
    private static final double MODE_OPTION_GAP_Y = 2.0;
    private final Map<String, Double> hoverAnimations = new HashMap<>();
    private final Map<String, Double> selectAnimations = new HashMap<>();
    private double animPillX = -1.0;
    private double animPillY = -1.0;
    private double animPillWidth = 0.0;
    private double animPillHeight = 0.0;
    private boolean pillInitialized = false;

    public ModeComponent(ModeSetting setting) {
        super(setting);

        for (String mode : setting.getModes()) {
            this.hoverAnimations.put(mode, 0.0);
            this.selectAnimations.put(mode, mode.equals(setting.getCurrentMode()) ? 1.0 : 0.0);
        }
    }

    private double computeBoxHeight() {
        double calculatedHeight = 6.0;
        double currentX = 0.0;
        double availableWidth = this.width - 10.0;
        float rowHeight = 11.0F;
        if (!((ModeSetting)this.setting).getModes().isEmpty() && !(availableWidth <= 0.0)) {
            calculatedHeight += rowHeight;

            for (String mode : ((ModeSetting)this.setting).getModes()) {
                double modeWidth = Renderer.get().getTextWidth(mode, 9.0F) + 10.0F;
                if (currentX + modeWidth > availableWidth && currentX != 0.0) {
                    calculatedHeight += rowHeight + 2.0;
                    currentX = 0.0;
                }

                currentX += modeWidth + 3.0;
            }

            return calculatedHeight + 0.5;
        } else {
            return calculatedHeight + rowHeight;
        }
    }

    private float[][] computeLayout(ModeSetting modeSet) {
        float fontSize = 9.0F;
        float rowHeight = fontSize + 2.0F;
        double innerX = this.x + 5.0;
        double innerY = this.y + 3.0;
        double optionX = innerX;
        double optionY = innerY;
        double availableWidth = this.width - 10.0;
        String[] modes = modeSet.getModes().toArray(new String[0]);
        float[][] bounds = new float[modes.length][4];

        for (int i = 0; i < modes.length; i++) {
            double modeWidth = Renderer.get().getTextWidth(modes[i], fontSize) + 10.0F;
            if (optionX + modeWidth > innerX + availableWidth && optionX > innerX) {
                optionY += rowHeight + 2.0;
                optionX = innerX;
            }

            bounds[i] = new float[]{(float)optionX - 2.0F, (float)optionY, (float)modeWidth, rowHeight};
            optionX += modeWidth + 3.0;
        }

        return bounds;
    }

    @Override
    public void render(class_332 context, int mouseX, int mouseY, float delta, double alpha) {
        ModeSetting modeSet = (ModeSetting)this.setting;
        double dt = Math.max(0.001, Math.min((double)delta, 0.05));
        String currentMode = modeSet.getCurrentMode();
        String[] modes = modeSet.getModes().toArray(new String[0]);
        float[][] bounds = this.computeLayout(modeSet);
        float boxRadius = Math.max(3.0F, Math.min(6.0F, (float)(this.getComponentHeight() * 0.25)));
        Color boxColor = ClickGUIModule.getColor(ClickGUIModule.getSettingsBackground(), alpha * 0.6);
        Renderer.get()
            .drawRoundedRectStyled(
                context, (float)this.x, (float)this.y, (float)this.width, (float)this.getComponentHeight(), boxRadius, boxColor, GuiShaderStyle.CONTROL
            );
        Color boxOverlay = new Color(255, 255, 255, (int)(5.0 * alpha));
        Renderer.get()
            .drawRoundedRectStyled(
                context, (float)this.x, (float)this.y, (float)this.width, (float)this.getComponentHeight(), boxRadius, boxOverlay, GuiShaderStyle.PANEL_GLASS
            );
        float targetPillX = -1.0F;
        float targetPillY = -1.0F;
        float targetPillW = 0.0F;
        float targetPillH = 0.0F;

        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(currentMode)) {
                targetPillX = bounds[i][0];
                targetPillY = bounds[i][1];
                targetPillW = bounds[i][2];
                targetPillH = bounds[i][3];
                break;
            }
        }

        if (targetPillX != -1.0F) {
            if (!this.pillInitialized) {
                this.animPillX = targetPillX;
                this.animPillY = targetPillY;
                this.animPillWidth = targetPillW;
                this.animPillHeight = targetPillH;
                this.pillInitialized = true;
            } else {
                double speed = 1.0 - Math.exp(-32.0 * dt);
                this.animPillX = this.animPillX + (targetPillX - this.animPillX) * speed;
                this.animPillY = this.animPillY + (targetPillY - this.animPillY) * speed;
                this.animPillWidth = this.animPillWidth + (targetPillW - this.animPillWidth) * speed;
                this.animPillHeight = this.animPillHeight + (targetPillH - this.animPillHeight) * speed;
            }

            float pillRadius = (float)this.animPillHeight / 2.0F;
            float b = 0.75F;
            Color borderColor = ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledText(), alpha * 0.35);
            Renderer.get()
                .drawRoundedRectStyled(
                    context,
                    (float)this.animPillX - b,
                    (float)this.animPillY - b,
                    (float)this.animPillWidth + b * 2.0F,
                    (float)this.animPillHeight + b * 2.0F,
                    pillRadius + b,
                    borderColor,
                    GuiShaderStyle.ROW_ENABLED
                );
            Color bgColor = ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledText(), alpha * 0.25);
            Renderer.get()
                .drawRoundedRectStyled(
                    context,
                    (float)this.animPillX,
                    (float)this.animPillY,
                    (float)this.animPillWidth,
                    (float)this.animPillHeight,
                    pillRadius,
                    bgColor,
                    GuiShaderStyle.ROW_ENABLED
                );
        }

        float fontSize = 9.0F;

        for (int ix = 0; ix < modes.length; ix++) {
            float pillX = bounds[ix][0];
            float pillY = bounds[ix][1];
            float pillW = bounds[ix][2];
            float pillH = bounds[ix][3];
            boolean isSelected = modes[ix].equals(currentMode);
            boolean isHov = mouseX >= pillX && mouseX <= pillX + pillW && mouseY >= pillY && mouseY <= pillY + pillH;
            double hTarget = isHov ? 1.0 : 0.0;
            double hCur = this.hoverAnimations.getOrDefault(modes[ix], 0.0);
            hCur += (hTarget - hCur) * (1.0 - Math.exp(-24.0 * dt));
            this.hoverAnimations.put(modes[ix], hCur);
            double sTarget = isSelected ? 1.0 : 0.0;
            double sCur = this.selectAnimations.getOrDefault(modes[ix], isSelected ? 1.0 : 0.0);
            sCur += (sTarget - sCur) * (1.0 - Math.exp(-26.0 * dt));
            this.selectAnimations.put(modes[ix], sCur);
            if (!isSelected && hCur > 0.01) {
                Color hoverColor = ClickGUIModule.getColor(ClickGUIModule.getSettingsBackground(), alpha * 0.3 * hCur);
                Renderer.get().drawRoundedRectStyled(context, pillX, pillY, pillW, pillH, pillH / 2.0F, hoverColor, GuiShaderStyle.ROW);
            }

            double brightness = isSelected ? 1.0 : 0.6 + hCur * 0.3 + sCur * 0.4;
            Color baseTextColor = isSelected ? ClickGUIModule.getModuleEnabledText() : ClickGUIModule.getModuleDisabledText();
            Color textColor = ClickGUIModule.getColor(baseTextColor, alpha * Math.min(1.0, brightness));
            float textH = Renderer.get().getTextHeight(fontSize);
            float textY = pillY + (pillH - textH) / 2.0F;
            Renderer.get().drawText(context, modes[ix], pillX + 5.0F, textY, fontSize, textColor, false);
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.isHovered(mouseX, mouseY)) {
            ModeSetting modeSet = (ModeSetting)this.setting;
            String[] modes = modeSet.getModes().toArray(new String[0]);
            float[][] bounds = this.computeLayout(modeSet);

            for (int i = 0; i < modes.length; i++) {
                if (mouseX >= bounds[i][0] && mouseX <= bounds[i][0] + bounds[i][2] && mouseY >= bounds[i][1] && mouseY <= bounds[i][1] + bounds[i][3]) {
                    modeSet.setCurrentMode(modes[i]);
                    break;
                }
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    @Override
    public double getComponentHeight() {
        return this.computeBoxHeight();
    }
}
