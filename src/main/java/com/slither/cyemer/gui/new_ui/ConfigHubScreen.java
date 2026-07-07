package com.slither.cyemer.gui.new_ui;

import com.slither.cyemer.config.ConfigManager;
import com.slither.cyemer.config.hub.ConfigHubManager;
import com.slither.cyemer.config.hub.RemoteConfig;
import com.slither.cyemer.theme.Theme;
import com.slither.cyemer.theme.ThemeManager;
import com.slither.cyemer.util.ICyemerRenderer;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11909;
import net.minecraft.class_156;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_768;

@Environment(EnvType.CLIENT)
public class ConfigHubScreen extends class_437 {
    private final class_437 parent;
    private final ConfigHubManager hubManager;
    private final Theme theme;
    private String statusText = "Loading...";
    private List<RemoteConfig> displayedConfigs;
    private RemoteConfig selectedConfig = null;
    private double scrollY = 0.0;
    private double maxScrollY = 0.0;
    private final double entryHeight = 35.0;
    private class_768 listBounds;
    private class_768 downloadButtonBounds;
    private class_768 backButtonBounds;
    private class_768 uploadButtonBounds;

    public ConfigHubScreen(class_437 parent) {
        super(class_2561.method_43470("Config Hub"));
        this.parent = parent;
        this.hubManager = new ConfigHubManager();
        this.theme = ThemeManager.getInstance().getCurrentTheme();
        this.displayedConfigs = this.hubManager.getCachedConfigs();
    }

    protected void method_25426() {
        this.hubManager.fetchConfigs(configs -> {
            this.displayedConfigs = configs;
            this.statusText = "";
            this.calculateMaxScroll();
        }, error -> this.statusText = "Error: " + error);
        int listWidth = 300;
        int listHeight = this.field_22790 - 100;
        this.listBounds = new class_768((this.field_22789 - listWidth) / 2, 50, listWidth, listHeight);
        int buttonWidth = 90;
        int buttonY = this.listBounds.method_3322() + this.listBounds.method_3320() + 10;
        int startX = (this.field_22789 - (buttonWidth * 3 + 20)) / 2;
        this.backButtonBounds = new class_768(startX, buttonY, buttonWidth, 20);
        this.uploadButtonBounds = new class_768(startX + buttonWidth + 10, buttonY, buttonWidth, 20);
        this.downloadButtonBounds = new class_768(startX + (buttonWidth + 10) * 2, buttonY, buttonWidth, 20);
    }

    private void calculateMaxScroll() {
        if (this.displayedConfigs != null) {
            this.maxScrollY = Math.max(0.0, this.displayedConfigs.size() * 35.0 - this.listBounds.method_3320());
        }
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        ICyemerRenderer renderer = Renderer.get();
        renderer.beginFrame(this.field_22789, this.field_22790, this.field_22787.method_22683().method_4495());
        renderer.drawRect(context, 0.0F, 0.0F, this.field_22789, this.field_22790, new Color(15, 15, 20, 200));
        renderer.drawCenteredText(context, "Community Config Hub", this.field_22789 / 2.0F, 20.0F, 16.0F, this.theme.headerBg, true);
        if (!this.statusText.isEmpty()) {
            renderer.drawCenteredText(context, this.statusText, this.field_22789 / 2.0F, this.field_22790 / 2.0F, 10.0F, Color.GRAY, true);
        } else {
            this.renderList(renderer, context, mouseX, mouseY);
        }

        this.renderButton(renderer, context, "Back", this.backButtonBounds, mouseX, mouseY, true);
        this.renderButton(renderer, context, "Submit Config", this.uploadButtonBounds, mouseX, mouseY, true);
        this.renderButton(renderer, context, "Download", this.downloadButtonBounds, mouseX, mouseY, this.selectedConfig != null);
        renderer.endFrame();
    }

    private void renderList(ICyemerRenderer renderer, class_332 context, int mouseX, int mouseY) {
        renderer.drawRoundedRect(
            context,
            this.listBounds.method_3321(),
            this.listBounds.method_3322(),
            this.listBounds.method_3319(),
            this.listBounds.method_3320(),
            5.0F,
            new Color(30, 30, 30, 150)
        );
        renderer.scissor(context, this.listBounds.method_3321(), this.listBounds.method_3322(), this.listBounds.method_3319(), this.listBounds.method_3320());
        int currentY = this.listBounds.method_3322() - (int)this.scrollY + 5;

        for (RemoteConfig config : this.displayedConfigs) {
            if (currentY + 35.0 > this.listBounds.method_3322() && currentY < this.listBounds.method_3322() + this.listBounds.method_3320()) {
                class_768 entryBounds = new class_768(this.listBounds.method_3321() + 5, currentY, this.listBounds.method_3319() - 10, 31);
                boolean isSelected = config == this.selectedConfig;
                boolean isHovered = this.isMouseOver(mouseX, mouseY, entryBounds) && this.isMouseOver(mouseX, mouseY, this.listBounds);
                Color bg = isSelected ? this.theme.moduleEnabledBg : (isHovered ? new Color(60, 60, 60) : new Color(40, 40, 40));
                renderer.drawRoundedRect(
                    context, entryBounds.method_3321(), entryBounds.method_3322(), entryBounds.method_3319(), entryBounds.method_3320(), 4.0F, bg
                );
                renderer.drawText(context, config.name, entryBounds.method_3321() + 5, entryBounds.method_3322() + 5, 10.0F, Color.WHITE, true);
                renderer.drawText(
                    context,
                    "by " + config.author,
                    entryBounds.method_3321() + 5 + renderer.getTextWidth(config.name, 10.0F) + 5.0F,
                    entryBounds.method_3322() + 6,
                    8.0F,
                    Color.GRAY,
                    true
                );
                renderer.drawText(
                    context, config.description, entryBounds.method_3321() + 5, entryBounds.method_3322() + 18, 8.0F, new Color(180, 180, 180), true
                );
            }

            currentY = (int)(currentY + 35.0);
        }

        renderer.resetScissor();
    }

    private void renderButton(ICyemerRenderer renderer, class_332 context, String text, class_768 bounds, int mouseX, int mouseY, boolean active) {
        Color bg = active ? (this.isMouseOver(mouseX, mouseY, bounds) ? this.theme.headerBg.brighter() : this.theme.headerBg) : new Color(30, 30, 30);
        renderer.drawRoundedRect(context, bounds.method_3321(), bounds.method_3322(), bounds.method_3319(), bounds.method_3320(), 4.0F, bg);
        renderer.drawCenteredText(
            context, text, bounds.method_3321() + bounds.method_3319() / 2.0F, bounds.method_3322() + bounds.method_3320() / 2.0F, 9.0F, Color.WHITE, true
        );
    }

    public boolean method_25402(class_11909 click, boolean doubleClick) {
        double mouseX = click.comp_4798();
        double mouseY = click.comp_4799();
        if (this.isMouseOver(mouseX, mouseY, this.backButtonBounds)) {
            this.method_25419();
            return true;
        } else if (this.isMouseOver(mouseX, mouseY, this.uploadButtonBounds)) {
            class_156.method_668().method_670("https://github.com/Cyemer/Cyemer-ConfigHub");
            return true;
        } else if (this.isMouseOver(mouseX, mouseY, this.downloadButtonBounds) && this.selectedConfig != null) {
            this.statusText = "Downloading...";
            this.hubManager.downloadConfig(this.selectedConfig, () -> {
                this.statusText = "";
                ConfigManager.getInstance().load(this.selectedConfig.fileName.replace(".json", ""));
                this.method_25419();
            }, err -> this.statusText = "Failed: " + err);
            return true;
        } else {
            if (this.isMouseOver(mouseX, mouseY, this.listBounds)) {
                int currentY = this.listBounds.method_3322() - (int)this.scrollY + 5;

                for (RemoteConfig config : this.displayedConfigs) {
                    if (mouseY >= currentY && mouseY < currentY + 35.0) {
                        this.selectedConfig = config;
                        return true;
                    }

                    currentY = (int)(currentY + 35.0);
                }
            }

            return super.method_25402(click, doubleClick);
        }
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.scrollY -= verticalAmount * 15.0;
        this.scrollY = Math.max(0.0, Math.min(this.scrollY, this.maxScrollY));
        return true;
    }

    public void method_25419() {
        this.field_22787.method_1507(this.parent);
    }

    private boolean isMouseOver(double mouseX, double mouseY, class_768 rect) {
        return mouseX >= rect.method_3321()
            && mouseX <= rect.method_3321() + rect.method_3319()
            && mouseY >= rect.method_3322()
            && mouseY <= rect.method_3322() + rect.method_3320();
    }
}
