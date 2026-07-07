package com.slither.cyemer.gui.new_ui;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.module.implementation.ClickGUIModule;
import com.slither.cyemer.rendering.LiquidMetalBackground;
import com.slither.cyemer.rendering.font.MSDFFontRenderer;
import com.slither.cyemer.util.GuiShaderStyle;
import com.slither.cyemer.util.VanillaRendererImpl;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11908;
import net.minecraft.class_156;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_437;
import net.minecraft.class_442;

@Environment(EnvType.CLIENT)
public class AuthenticationScreen extends class_437 {
    private static final int PANEL_W = 300;
    private static final int PANEL_H = 176;
    private static final class_2960 FONT_TITLE = class_2960.method_60655("dynamic_fps", "cyemer");
    private static final class_2960 FONT_UI = class_2960.method_60655("dynamic_fps", "sans");
    private CyemerButton loginButton;
    private CyemerButton pasteButton;
    private CyemerButton discordButton;
    private CyemerButton freeButton;
    private class_342 keyInputField;
    private String statusMessage = "Enter your license key";
    private int statusColor = -6641988;
    private boolean authenticating = false;
    private final LiquidMetalBackground background = new LiquidMetalBackground();
    private final VanillaRendererImpl renderer = new VanillaRendererImpl();

    public AuthenticationScreen() {
        super(class_2561.method_43470("Cyemer Authentication"));
    }

    protected void method_25426() {
        super.method_25426();
        int cx = this.field_22789 / 2;
        int cy = this.field_22790 / 2;
        int px = cx - 150;
        int py = cy - 88;
        int fieldW = 260;
        int fieldX = cx - fieldW / 2;
        int fieldY = py + 68;
        this.keyInputField = new class_342(this.field_22793, fieldX, fieldY, fieldW, 22, class_2561.method_43470("License Key"));
        this.keyInputField.method_1880(128);
        this.keyInputField.method_1858(false);
        this.keyInputField
            .method_47404(class_2561.method_43470("License key").method_27694(s -> s.method_36139(ClickGUIModule.getSearchBoxPlaceholder().getRGB())));
        this.method_37063(this.keyInputField);
        int btnY = fieldY + 30;
        int btnW = (fieldW - 8) / 2;
        this.loginButton = new CyemerButton(fieldX, btnY, btnW, 24, "Login", this.renderer, btn -> this.attemptLogin());
        this.pasteButton = new CyemerButton(fieldX + btnW + 8, btnY, btnW, 24, "Paste", this.renderer, btn -> {
            String clip = class_310.method_1551().field_1774.method_1460();
            if (clip != null && !clip.isBlank()) {
                this.keyInputField.method_1852(clip.trim());
                this.keyInputField.method_25365(true);
                this.setStatus("Key pasted.", ClickGUIModule.getModuleEnabledGradientStart().getRGB());
            } else {
                this.setStatus("Clipboard is empty.", -3390396);
            }
        });
        this.discordButton = new CyemerButton(
            fieldX, btnY + 32, btnW, 24, "Discord", this.renderer, btn -> class_156.method_668().method_670("https://discord.gg/cyemer")
        );
        this.freeButton = new CyemerButton(fieldX + btnW + 8, btnY + 32, btnW, 24, "Free Version", this.renderer, btn -> this.attemptFreeVersion());
        this.method_37063(this.loginButton);
        this.method_37063(this.pasteButton);
        this.method_37063(this.discordButton);
        this.method_37063(this.freeButton);
    }

    public boolean method_25404(class_11908 input) {
        if (this.keyInputField.method_25370()) {
            if (input.method_74243()) {
                String clip = class_310.method_1551().field_1774.method_1460();
                if (clip != null && !clip.isBlank()) {
                    this.keyInputField.method_1852(clip.trim());
                    return true;
                }
            }

            if (input.comp_4795() == 257) {
                this.attemptLogin();
                return true;
            }
        }

        return super.method_25404(input);
    }

    private void attemptFreeVersion() {
        if (!this.authenticating) {
            this.setStatus("Starting Free Version...", -3368704);
            this.authenticating = true;
            this.loginButton.field_22763 = false;
            this.pasteButton.field_22763 = false;
            this.discordButton.field_22763 = false;
            if (this.freeButton != null) {
                this.freeButton.field_22763 = false;
            }

            this.setStatus("Free Version active", -12259704);

            try {
                if (Cyemer.getInstance() != null && Cyemer.getInstance().getConfigManager() != null) {
                    Cyemer.getInstance().getConfigManager().load("default");
                }
            } catch (Throwable var2) {
            }

            this.field_22787.execute(() -> {
                if (this.field_22787.field_1755 == this) {
                    this.field_22787.method_1507(null);
                } else {
                    this.field_22787.method_1507(new class_442());
                }
            });
        }
    }

    private void attemptLogin() {
        if (!this.authenticating) {
            String key = this.keyInputField.method_1882();
            if (key != null && !key.isBlank()) {
                this.setStatus("Authenticating...", -3368704);
                this.authenticating = true;
                this.loginButton.field_22763 = false;
                this.pasteButton.field_22763 = false;
            } else {
                this.setStatus("License key cannot be empty.", -3390396);
            }
        }
    }

    private void setStatus(String message, int color) {
        this.statusMessage = message;
        this.statusColor = color;
    }

    public void method_25420(class_332 context, int mouseX, int mouseY, float delta) {
        this.background.render(context, this.field_22789, this.field_22790, delta);
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        int cx = this.field_22789 / 2;
        int cy = this.field_22790 / 2;
        int px = cx - 150;
        int py = cy - 88;
        float radius = Math.max(8.0F, Math.min(18.0F, ClickGUIModule.getGuiCornerRadiusScaled(0.85F)));
        float pixelRatio = this.field_22787 == null ? 1.0F : this.field_22787.method_22683().method_4495() * ClickGUIModule.getUiResolutionScale();
        this.renderer.beginFrame(this.field_22789, this.field_22790, pixelRatio);
        Color panelColor = ClickGUIModule.getColor(ClickGUIModule.getPanelBackground(), 0.95);
        Color borderColor = ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), 0.55);
        this.renderer.drawRoundedRectStyled(context, px, py, 300.0F, 176.0F, radius, panelColor, GuiShaderStyle.PANEL);
        this.renderer.drawRoundedRectOutline(context, px, py, 300.0F, 176.0F, radius, 1.0F, borderColor);
        float fieldRadius = Math.max(4.0F, radius * 0.45F);
        Color inputBg = this.keyInputField.method_25370()
            ? ClickGUIModule.getColor(ClickGUIModule.getSearchBoxFocusedBackground(), 0.95)
            : ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBackground(), 0.92);
        Color inputBorder = this.keyInputField.method_25370()
            ? ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientStart(), 0.9)
            : ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), 0.5);
        this.renderer
            .drawRoundedRectStyled(
                context,
                this.keyInputField.method_46426(),
                this.keyInputField.method_46427(),
                this.keyInputField.method_25368(),
                this.keyInputField.method_25364(),
                fieldRadius,
                inputBg,
                GuiShaderStyle.SEARCH
            );
        this.renderer
            .drawRoundedRectOutline(
                context,
                this.keyInputField.method_46426(),
                this.keyInputField.method_46427(),
                this.keyInputField.method_25368(),
                this.keyInputField.method_25364(),
                fieldRadius,
                1.0F,
                inputBorder
            );
        this.renderer.endFrame();
        float titleSize = 1.95F;
        String title = "CYEMER";
        float titleW = MSDFFontRenderer.getTextWidth(FONT_TITLE, title) * titleSize;
        MSDFFontRenderer.drawText(context, FONT_TITLE, title, cx - titleW / 2.0F, py + 18.0F, titleSize, ClickGUIModule.getModuleEnabledText().getRGB(), false);
        float labelSize = 1.15F;
        String label = "LICENSE KEY";
        float labelW = MSDFFontRenderer.getTextWidth(FONT_UI, label) * labelSize;
        MSDFFontRenderer.drawText(context, FONT_UI, label, cx - labelW / 2.0F, py + 54.0F, labelSize, ClickGUIModule.getSearchBoxPlaceholder().getRGB(), false);
        float statusSize = 1.15F;
        float statusW = MSDFFontRenderer.getTextWidth(FONT_UI, this.statusMessage) * statusSize;
        MSDFFontRenderer.drawText(context, FONT_UI, this.statusMessage, cx - statusW / 2.0F, py + 176 - 15.0F, statusSize, this.statusColor, false);
        super.method_25394(context, mouseX, mouseY, delta);
    }

    public boolean method_25422() {
        return false;
    }
}
