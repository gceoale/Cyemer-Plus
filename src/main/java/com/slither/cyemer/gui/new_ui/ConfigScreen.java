package com.slither.cyemer.gui.new_ui;

import com.slither.cyemer.config.ConfigManager;
import com.slither.cyemer.theme.Theme;
import com.slither.cyemer.theme.ThemeManager;
import com.slither.cyemer.util.ICyemerRenderer;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11905;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_768;

@Environment(EnvType.CLIENT)
public class ConfigScreen extends class_437 {
    private final class_437 parent;
    private final ConfigManager configManager;
    private final Theme theme;
    private String configNameText = "";
    private boolean isTextFieldFocused = false;
    private long lastClickTime = 0L;
    private final List<String> configList = new ArrayList<>();
    private String selectedConfig = null;
    private double scrollY = 0.0;
    private double maxScrollY = 0.0;
    private final double entryHeight = 22.0;
    private class_768 listBounds;
    private class_768 textFieldBounds;
    private class_768 saveButtonBounds;
    private class_768 loadButtonBounds;
    private class_768 deleteButtonBounds;
    private class_768 backButtonBounds;
    private class_768 scrollbarBounds;
    private class_768 hubButtonBounds;

    public ConfigScreen(class_437 parent, ConfigManager configManager) {
        super(class_2561.method_43470("Configuration Manager"));
        this.parent = parent;
        this.configManager = configManager;
        this.theme = ThemeManager.getInstance().getCurrentTheme();
    }

    protected void method_25426() {
        int listWidth = 260;
        int listHeight = this.field_22790 - 140;
        int topY = 60;
        this.listBounds = new class_768((this.field_22789 - listWidth) / 2, topY, listWidth, listHeight);
        this.textFieldBounds = new class_768(
            this.listBounds.method_3321(), this.listBounds.method_3322() + this.listBounds.method_3320() + 10, listWidth - 65, 20
        );
        this.saveButtonBounds = new class_768(
            this.textFieldBounds.method_3321() + this.textFieldBounds.method_3319() + 5, this.textFieldBounds.method_3322(), 60, 20
        );
        int buttonWidth = (listWidth - 10) / 3;
        int buttonY = this.saveButtonBounds.method_3322() + this.saveButtonBounds.method_3320() + 5;
        this.loadButtonBounds = new class_768(this.listBounds.method_3321(), buttonY, buttonWidth, 20);
        this.deleteButtonBounds = new class_768(this.loadButtonBounds.method_3321() + buttonWidth + 5, buttonY, buttonWidth, 20);
        this.backButtonBounds = new class_768(this.deleteButtonBounds.method_3321() + buttonWidth + 5, buttonY, buttonWidth, 20);
        this.hubButtonBounds = new class_768(this.field_22789 - 110, 15, 100, 20);
        this.scrollbarBounds = new class_768(
            this.listBounds.method_3321() + this.listBounds.method_3319() - 6, this.listBounds.method_3322() + 2, 4, this.listBounds.method_3320() - 4
        );
        this.refreshConfigList();
    }

    private void refreshConfigList() {
        this.configList.clear();
        this.configList.addAll(this.configManager.getAvailableConfigs());
        this.calculateMaxScroll();
        if (this.selectedConfig != null && !this.configList.contains(this.selectedConfig)) {
            this.selectedConfig = null;
        }
    }

    private void calculateMaxScroll() {
        this.maxScrollY = Math.max(0.0, this.configList.size() * 22.0 - this.listBounds.method_3320());
        if (this.scrollY > this.maxScrollY) {
            this.scrollY = this.maxScrollY;
        }
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        ICyemerRenderer renderer = Renderer.get();
        boolean success = renderer.beginFrame(this.field_22789, this.field_22790, this.field_22787.method_22683().method_4495());
        if (success) {
            renderer.drawRect(context, 0.0F, 0.0F, this.field_22789, this.field_22790, new Color(0, 0, 0, 150));
            renderer.drawCenteredText(context, "Configuration Manager", this.field_22789 / 2.0F, 25.0F, 14.0F, Color.WHITE, true);
            renderer.drawText(context, "Available Configs", this.listBounds.method_3321(), this.listBounds.method_3322() - 15, 9.0F, Color.LIGHT_GRAY, true);
            this.renderConfigList(renderer, context, mouseX, mouseY);
            this.renderTextField(renderer, context);
            this.renderButtons(renderer, context, mouseX, mouseY);
            renderer.endFrame();
        }
    }

    private void renderConfigList(ICyemerRenderer renderer, class_332 context, int mouseX, int mouseY) {
        renderer.drawRoundedRect(
            context,
            this.listBounds.method_3321(),
            this.listBounds.method_3322(),
            this.listBounds.method_3319(),
            this.listBounds.method_3320(),
            6.0F,
            new Color(20, 20, 20, 200)
        );
        renderer.drawRoundedRectOutline(
            context,
            this.listBounds.method_3321(),
            this.listBounds.method_3322(),
            this.listBounds.method_3319(),
            this.listBounds.method_3320(),
            6.0F,
            1.0F,
            new Color(60, 60, 60)
        );
        renderer.scissor(
            context, this.listBounds.method_3321(), this.listBounds.method_3322() + 1, this.listBounds.method_3319(), this.listBounds.method_3320() - 2
        );
        int currentY = this.listBounds.method_3322() - (int)this.scrollY + 5;

        for (String configName : this.configList) {
            if (currentY + 22.0 > this.listBounds.method_3322() && currentY < this.listBounds.method_3322() + this.listBounds.method_3320()) {
                class_768 entryBounds = new class_768(this.listBounds.method_3321() + 4, currentY, this.listBounds.method_3319() - 14, 20);
                boolean isSelected = configName.equals(this.selectedConfig);
                boolean isHovered = this.isMouseOver(mouseX, mouseY, entryBounds) && this.isMouseOver(mouseX, mouseY, this.listBounds);
                Color bgColor;
                if (isSelected) {
                    bgColor = this.theme.moduleEnabledBg;
                } else if (isHovered) {
                    bgColor = new Color(50, 50, 50);
                } else {
                    bgColor = new Color(0, 0, 0, 0);
                }

                if (isSelected || isHovered) {
                    renderer.drawRoundedRect(
                        context, entryBounds.method_3321(), entryBounds.method_3322(), entryBounds.method_3319(), entryBounds.method_3320(), 4.0F, bgColor
                    );
                }

                Color textColor = isSelected ? Color.WHITE : (isHovered ? Color.WHITE : new Color(180, 180, 180));
                renderer.drawText(context, configName, entryBounds.method_3321() + 5, entryBounds.method_3322() + 6, 9.0F, textColor, true);
            }

            currentY = (int)(currentY + 22.0);
        }

        renderer.resetScissor();
        if (this.maxScrollY > 0.0) {
            float scrollbarX = this.scrollbarBounds.method_3321();
            float scrollbarY = this.scrollbarBounds.method_3322();
            float scrollbarW = this.scrollbarBounds.method_3319();
            float scrollbarH = this.scrollbarBounds.method_3320();
            renderer.drawRoundedRect(context, scrollbarX, scrollbarY, scrollbarW, scrollbarH, 2.0F, new Color(30, 30, 30));
            double scrollPercentage = this.scrollY / this.maxScrollY;
            float viewportRatio = (float)(this.listBounds.method_3320() / (this.configList.size() * 22.0));
            float handleHeight = Math.max(20.0F, scrollbarH * viewportRatio);
            float handleY = scrollbarY + (float)(scrollPercentage * (scrollbarH - handleHeight));
            Color scrollColor = this.isMouseOver(mouseX, mouseY, this.scrollbarBounds) ? new Color(120, 120, 120) : new Color(80, 80, 80);
            renderer.drawRoundedRect(context, scrollbarX, handleY, scrollbarW, handleHeight, 2.0F, scrollColor);
        }
    }

    private void renderTextField(ICyemerRenderer renderer, class_332 context) {
        Color bgColor = this.isTextFieldFocused ? new Color(28, 28, 28, 200) : new Color(22, 22, 22, 170);
        Color borderColor = this.isTextFieldFocused ? new Color(210, 210, 210, 160) : new Color(80, 80, 80, 120);
        renderer.drawRoundedRect(
            context,
            this.textFieldBounds.method_3321(),
            this.textFieldBounds.method_3322(),
            this.textFieldBounds.method_3319(),
            this.textFieldBounds.method_3320(),
            4.0F,
            bgColor
        );
        renderer.drawRectOutline(
            context,
            this.textFieldBounds.method_3321(),
            this.textFieldBounds.method_3322(),
            this.textFieldBounds.method_3319(),
            this.textFieldBounds.method_3320(),
            1.0F,
            borderColor
        );
        String renderText = this.configNameText;
        if (this.isTextFieldFocused && System.currentTimeMillis() / 500L % 2L == 0L) {
            renderText = renderText + "_";
        }

        if (this.configNameText.isEmpty() && !this.isTextFieldFocused) {
            renderer.drawText(
                context, "Enter config name...", this.textFieldBounds.method_3321() + 5, this.textFieldBounds.method_3322() + 6, 9.0F, Color.GRAY, true
            );
        } else {
            renderer.drawText(context, renderText, this.textFieldBounds.method_3321() + 5, this.textFieldBounds.method_3322() + 6, 9.0F, Color.WHITE, true);
        }
    }

    private void renderButtons(ICyemerRenderer renderer, class_332 context, int mouseX, int mouseY) {
        this.renderButton(renderer, context, "Save", this.saveButtonBounds, mouseX, mouseY, true);
        this.renderButton(renderer, context, "Load", this.loadButtonBounds, mouseX, mouseY, this.selectedConfig != null);
        this.renderButton(renderer, context, "Delete", this.deleteButtonBounds, mouseX, mouseY, this.selectedConfig != null);
        this.renderButton(renderer, context, "Back", this.backButtonBounds, mouseX, mouseY, true);
        this.renderButton(renderer, context, "Config Hub", this.hubButtonBounds, mouseX, mouseY, true);
    }

    private void renderButton(ICyemerRenderer renderer, class_332 context, String text, class_768 bounds, int mouseX, int mouseY, boolean active) {
        boolean isHovered = active && this.isMouseOver(mouseX, mouseY, bounds);
        Color bg;
        Color textC;
        if (!active) {
            bg = new Color(30, 30, 30);
            textC = new Color(80, 80, 80);
        } else if (isHovered) {
            bg = this.theme.headerBg.brighter();
            textC = Color.WHITE;
        } else {
            bg = this.theme.headerBg;
            textC = new Color(200, 200, 200);
        }

        renderer.drawRoundedRect(context, bounds.method_3321(), bounds.method_3322(), bounds.method_3319(), bounds.method_3320(), 4.0F, bg);
        renderer.drawRoundedRectOutline(
            context, bounds.method_3321(), bounds.method_3322(), bounds.method_3319(), bounds.method_3320(), 4.0F, 1.0F, new Color(0, 0, 0, 50)
        );
        renderer.drawCenteredText(
            context, text, bounds.method_3321() + bounds.method_3319() / 2.0F, bounds.method_3322() + bounds.method_3320() / 2.0F, 9.0F, textC, true
        );
    }

    public boolean method_25402(class_11909 click, boolean doubleClick) {
        double mouseX = click.comp_4798();
        double mouseY = click.comp_4799();
        this.isTextFieldFocused = this.isMouseOver(mouseX, mouseY, this.textFieldBounds);
        if (this.isMouseOver(mouseX, mouseY, this.saveButtonBounds)) {
            this.onSave();
            return true;
        } else if (this.isMouseOver(mouseX, mouseY, this.loadButtonBounds) && this.selectedConfig != null) {
            this.onLoad();
            return true;
        } else if (this.isMouseOver(mouseX, mouseY, this.deleteButtonBounds) && this.selectedConfig != null) {
            this.onDelete();
            return true;
        } else if (this.isMouseOver(mouseX, mouseY, this.backButtonBounds)) {
            this.method_25419();
            return true;
        } else {
            if (this.isMouseOver(mouseX, mouseY, this.listBounds)) {
                int currentY = this.listBounds.method_3322() - (int)this.scrollY + 5;

                for (String configName : this.configList) {
                    if (mouseY >= currentY && mouseY < currentY + 22.0) {
                        if (!(mouseY < this.listBounds.method_3322()) && !(mouseY > this.listBounds.method_3322() + this.listBounds.method_3320())) {
                            this.selectedConfig = configName;
                            this.configNameText = configName;
                            long now = System.currentTimeMillis();
                            if (now - this.lastClickTime < 250L) {
                                this.onLoad();
                            }

                            this.lastClickTime = now;
                            return true;
                        }
                        break;
                    }

                    currentY = (int)(currentY + 22.0);
                }
            }

            if (this.isMouseOver(mouseX, mouseY, this.hubButtonBounds)) {
                this.field_22787.method_1507(new ConfigHubScreen(this));
                return true;
            } else {
                return super.method_25402(click, doubleClick);
            }
        }
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.isMouseOver(mouseX, mouseY, this.listBounds)) {
            this.scrollY -= verticalAmount * 15.0;
            this.scrollY = Math.max(0.0, Math.min(this.scrollY, this.maxScrollY));
            return true;
        } else {
            return false;
        }
    }

    public boolean method_25404(class_11908 keyInput) {
        int keyCode = keyInput.comp_4795();
        if (this.isTextFieldFocused) {
            if (keyCode == 257) {
                this.onSave();
                return true;
            }

            if (keyCode == 259 && !this.configNameText.isEmpty()) {
                this.configNameText = this.configNameText.substring(0, this.configNameText.length() - 1);
                return true;
            }

            if (keyCode == 256) {
                this.isTextFieldFocused = false;
                return true;
            }
        }

        if (keyCode == 256) {
            this.method_25419();
            return true;
        } else {
            return super.method_25404(keyInput);
        }
    }

    public boolean method_25400(class_11905 input) {
        if (this.isTextFieldFocused && input.method_74227() && Character.isBmpCodePoint(input.comp_4793())) {
            char chr = (char)input.comp_4793();
            if (this.isValidFileNameChar(chr)) {
                this.configNameText = this.configNameText + chr;
                return true;
            }
        }

        return super.method_25400(input);
    }

    private void onSave() {
        String name = this.configNameText.trim();
        if (!name.isEmpty()) {
            this.configManager.save(name);
            this.refreshConfigList();
        }
    }

    private void onLoad() {
        if (this.selectedConfig != null) {
            this.configManager.load(this.selectedConfig);
            this.method_25419();
        }
    }

    private void onDelete() {
        if (this.selectedConfig != null) {
            this.configManager.delete(this.selectedConfig);
            this.refreshConfigList();
            this.configNameText = "";
            this.selectedConfig = null;
        }
    }

    public void method_25419() {
        Objects.requireNonNull(this.field_22787).method_1507(this.parent);
    }

    private boolean isMouseOver(double mouseX, double mouseY, class_768 rect) {
        return mouseX >= rect.method_3321()
            && mouseX <= rect.method_3321() + rect.method_3319()
            && mouseY >= rect.method_3322()
            && mouseY <= rect.method_3322() + rect.method_3320();
    }

    private boolean isValidFileNameChar(char c) {
        return Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.' || c == ' ';
    }
}
