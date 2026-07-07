package com.slither.cyemer.gui.new_ui.old;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.implementation.ClickGUIModule;
import com.slither.cyemer.util.GuiShaderStyle;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3298;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public class Panel {
    public double x;
    public double y;
    public double width;
    public double headerHeight;
    private final Category category;
    private final List<ModuleButton> buttons = new ArrayList<>();
    public boolean dragging = false;
    private double dragX;
    private double dragY;
    private boolean collapsed = false;
    private double collapseAnimation = 1.0;
    private boolean isCollapsing = false;
    private boolean isExpanding = false;
    private String searchQuery = "";
    private boolean searchFocused = false;
    private double searchHoverAnim = 0.0;
    private static final double MODULE_ROW_HEIGHT = 21.0;
    private static final double BOTTOM_CORNER_GAP = 2.5;
    private double searchBoxHeight = 21.0;
    private int categoryIconImageId = -1;
    private int categoryIconTextureWidth = 256;
    private int categoryIconTextureHeight = 256;
    private String lastIconColorMode = null;
    private long lastAnimationNanos = -1L;
    private static final Map<String, Integer> iconCache = new HashMap<>();

    public Panel(Category category, double x, double y, double width, double headerHeight) {
        this.category = category;
        this.x = x;
        this.y = y;
        this.width = width;
        this.headerHeight = headerHeight;
        Cyemer.getInstance()
            .getModuleManager()
            .getModules()
            .stream()
            .filter(m -> m.getCategory() == category)
            .forEach(m -> this.buttons.add(new ModuleButton(m)));
        if (category == Category.CLIENT) {
            this.moveModuleToBottom("SelfDestruct");
            this.moveModuleToBottom("ClickGUI");
        } else if (category == Category.COMBAT) {
            this.moveModuleToBottom("InstaCart");
        } else if (category == Category.RENDER) {
            this.moveModuleToBottom("Nametags");
        }

        this.loadCategoryIcon();
    }

    private void moveModuleToBottom(String moduleName) {
        this.buttons.sort((a, b) -> {
            boolean aMatches = moduleName.equalsIgnoreCase(a.getModule().getName());
            boolean bMatches = moduleName.equalsIgnoreCase(b.getModule().getName());
            if (aMatches == bMatches) {
                return 0;
            } else {
                return aMatches ? 1 : -1;
            }
        });
    }

    private void loadCategoryIcon() {
        try {
            String preferredFile = null;
            switch (this.category) {
                case CLIENT:
                    preferredFile = "client.png";
                    break;
                case MISC:
                    preferredFile = "misc.png";
                    break;
                case RENDER:
                    preferredFile = "render.png";
                    break;
                case COMBAT:
                    preferredFile = "combat.png";
                    break;
                case MOVEMENT:
                    preferredFile = "movement.png";
                    break;
                case PLAYER:
                    preferredFile = "player.png";
            }

            int iconId = -1;
            if (preferredFile != null) {
                iconId = this.tryLoadGuiIcon(preferredFile);
            }

            if (iconId == -1) {
                String lowerName = this.category.name().toLowerCase() + ".png";
                iconId = this.tryLoadGuiIcon(lowerName);
            }

            if (iconId == -1) {
                String exactName = this.category.name() + ".png";
                iconId = this.tryLoadGuiIcon(exactName);
            }

            this.categoryIconImageId = iconId;
        } catch (Exception var4) {
            this.categoryIconImageId = -1;
        }

        this.lastIconColorMode = ClickGUIModule.getIconColorMode();
    }

    private int tryLoadGuiIcon(String fileName) {
        if (iconCache.containsKey(fileName)) {
            return iconCache.get(fileName);
        } else {
            try {
                String url = "https://raw.githubusercontent.com/Cyemer/cyemer.github.io/main/icons/" + fileName;
                HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                int var9;
                try (InputStream stream = conn.getInputStream()) {
                    class_1011 img = class_1011.method_4309(stream);

                    try {
                        this.categoryIconTextureWidth = img.method_4307();
                        this.categoryIconTextureHeight = img.method_4323();
                        class_2960 id = class_2960.method_60655("dynamic_fps", "textures/gui/web/" + fileName);
                        class_1043 tex = new class_1043(() -> "cyemer_panel_icon_" + fileName, img);
                        class_310.method_1551().execute(() -> class_310.method_1551().method_1531().method_4616(id, tex));
                        int iconId = Renderer.get().createImageFromFile(id.toString());
                        iconCache.put(fileName, iconId);
                        var9 = iconId;
                    } catch (Throwable var12) {
                        if (img != null) {
                            try {
                                img.close();
                            } catch (Throwable var11) {
                                var12.addSuppressed(var11);
                            }
                        }

                        throw var12;
                    }

                    if (img != null) {
                        img.close();
                    }
                }

                return var9;
            } catch (Exception var14) {
                return -1;
            }
        }
    }

    private void updateIconTextureSize(String fileName) {
        this.categoryIconTextureWidth = 256;
        this.categoryIconTextureHeight = 256;
        class_310 client = class_310.method_1551();
        if (client != null && client.method_1478() != null) {
            class_2960 id = this.resolveGuiTextureIdentifier(fileName);
            if (id != null) {
                Optional<class_3298> optional = client.method_1478().method_14486(id);
                if (!optional.isEmpty()) {
                    try (InputStream stream = optional.get().method_14482()) {
                        class_1011 image = class_1011.method_4309(stream);

                        try {
                            this.categoryIconTextureWidth = Math.max(1, image.method_4307());
                            this.categoryIconTextureHeight = Math.max(1, image.method_4323());
                        } catch (Throwable var11) {
                            if (image != null) {
                                try {
                                    image.close();
                                } catch (Throwable var10) {
                                    var11.addSuppressed(var10);
                                }
                            }

                            throw var11;
                        }

                        if (image != null) {
                            image.close();
                        }
                    } catch (IOException var13) {
                    }
                }
            }
        }
    }

    private class_2960 resolveGuiTextureIdentifier(String fileName) {
        if (fileName != null && !fileName.isEmpty()) {
            String path = "textures/gui/" + fileName;

            try {
                return class_2960.method_60655("dynamic_fps", path);
            } catch (Exception var6) {
                String lowerPath = "textures/gui/" + fileName.toLowerCase(Locale.ROOT);

                try {
                    return class_2960.method_60655("dynamic_fps", lowerPath);
                } catch (Exception var5) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    public String render(class_332 context, int mouseX, int mouseY, float delta, double alpha) {
        if (this.lastIconColorMode == null || !this.lastIconColorMode.equalsIgnoreCase(ClickGUIModule.getIconColorMode())) {
            this.loadCategoryIcon();
        }

        if (alpha < 0.01) {
            return null;
        } else {
            String tooltip = null;
            double frameDeltaSeconds = this.getAnimationDeltaSeconds();
            Renderer.get().resetScissor();
            if (this.isCollapsing || this.isExpanding) {
                double target = this.isExpanding ? 1.0 : 0.0;
                this.collapseAnimation = this.collapseAnimation + (target - this.collapseAnimation) * (1.0 - Math.exp(-18.0 * frameDeltaSeconds));
                if (Math.abs(this.collapseAnimation - target) < 0.001) {
                    this.collapseAnimation = target;
                    this.isCollapsing = false;
                    this.isExpanding = false;
                }
            }

            this.renderPanelBody(context, alpha);
            this.renderHeaderWithGlass(context, alpha);
            Renderer.get().resetScissor();
            String titleString = this.collapsed ? this.category.name + " [" + this.buttons.size() + "]" : this.category.name;
            float titleX = (float)(this.x + 10.0);
            float fontSize = 12.0F;
            float titleY = (float)(this.y + (this.headerHeight - Renderer.get().getTextHeight(fontSize)) / 2.0);
            Color titleColor = new Color(255, 255, 255, (int)(255.0 * alpha));
            Renderer.get().drawText(context, titleString, titleX, titleY, fontSize, titleColor, false);
            float logoSize = 15.0F;
            float logoX = (float)(this.x + this.width - logoSize - 9.0);
            float logoY = (float)(this.y + (this.headerHeight - logoSize) / 2.0);
            if (this.categoryIconImageId != -1) {
                Renderer.get()
                    .drawTexture(
                        context,
                        this.categoryIconImageId,
                        logoX,
                        logoY,
                        logoSize,
                        logoSize,
                        0.0F,
                        0.0F,
                        this.categoryIconTextureWidth,
                        this.categoryIconTextureHeight,
                        this.categoryIconTextureWidth,
                        this.categoryIconTextureHeight
                    );
            }

            boolean searchEnabled = ClickGUIModule.useSearch();
            if (!searchEnabled) {
                this.searchFocused = false;
                if (!this.searchQuery.isEmpty()) {
                    this.searchQuery = "";
                }
            }

            if (this.collapseAnimation > 0.0) {
                double contentY = this.y + this.headerHeight;
                double contentAlpha = alpha * this.collapseAnimation;
                if (!this.collapsed && searchEnabled) {
                    this.renderSearchBox(context, mouseX, mouseY, contentY, contentAlpha, frameDeltaSeconds);
                    contentY += this.searchBoxHeight;
                }

                List<ModuleButton> visibleButtons = new ArrayList<>();

                for (ModuleButton button : this.buttons) {
                    if (!searchEnabled || this.searchQuery.isEmpty() || button.getModule().getName().toLowerCase().contains(this.searchQuery.toLowerCase())) {
                        visibleButtons.add(button);
                    }
                }

                double totalContentHeight = this.getTotalHeightExpanded() - this.headerHeight - (!this.collapsed && searchEnabled ? this.searchBoxHeight : 0.0);
                double panelTotalHeight = this.getTotalHeight();
                double scissorHeight = panelTotalHeight - this.headerHeight + 2.0;
                Renderer.get().scissor(context, (float)this.x, (float)(this.y + this.headerHeight), (float)this.width, (float)scissorHeight);
                double buttonY = contentY;

                for (int i = 0; i < visibleButtons.size(); i++) {
                    ModuleButton buttonx = visibleButtons.get(i);
                    buttonx.isLastButton = i == visibleButtons.size() - 1;
                    buttonx.x = this.x;
                    buttonx.y = buttonY;
                    buttonx.width = this.width;
                    buttonx.height = 21.0;
                    Renderer.get().scissor(context, (float)this.x, (float)(this.y + this.headerHeight), (float)this.width, (float)scissorHeight);
                    String btnTooltip = buttonx.render(context, mouseX, mouseY, delta, contentAlpha);
                    if (btnTooltip != null && !btnTooltip.isEmpty()) {
                        tooltip = btnTooltip;
                    }

                    buttonY += Math.max(1.0, buttonx.getTotalHeight());
                }

                Renderer.get().resetScissor();
            }

            return tooltip;
        }
    }

    private void renderPanelBody(class_332 context, double alpha) {
        float radius = ClickGUIModule.getGuiCornerRadiusScaled(1.0);
        float panelX = (float)this.x;
        float panelY = (float)this.y;
        float panelWidth = (float)this.width;
        float panelHeight = (float)this.getTotalHeight();
        if (ClickGUIModule.useBlurBackground()) {
            float blur = ClickGUIModule.getBlurStrength();
            if (blur > 0.0F) {
                Renderer.get().scissor(context, panelX, panelY, panelWidth, panelHeight);
                Renderer.get().drawBlur(context, panelX, panelY, panelWidth, panelHeight, blur);
                Renderer.get().resetScissor();
            }
        }

        Color panelBg = ClickGUIModule.getColor(ClickGUIModule.getPanelBackground(), alpha);
        Color glassOverlay = new Color(255, 255, 255, (int)(8.0 * alpha));
        Renderer.get().drawRoundedRectStyled(context, panelX, panelY, panelWidth, panelHeight, radius, panelBg, GuiShaderStyle.PANEL);
        Renderer.get().drawRoundedRectStyled(context, panelX, panelY, panelWidth, panelHeight, radius, glassOverlay, GuiShaderStyle.PANEL_GLASS);
    }

    private void renderShadow(class_332 context, double alpha) {
        float totalHeight = (float)this.getTotalHeight();
        float cornerRadius = ClickGUIModule.getCornerRadiusScaled(1.0);
        float shadowRadius = Math.min(8.0F, cornerRadius);
        int layers = 7;
        float maxOffset = 5.5F;

        for (int i = 0; i < layers; i++) {
            float progress = (float)i / layers;
            float layerOffset = 1.5F + progress * maxOffset;
            float layerAlpha = (float)(alpha * 0.06 * (1.0 - progress * 0.8));
            Color shadowColor = new Color(0, 0, 0, (int)(layerAlpha * 255.0F));
            Renderer.get()
                .drawRoundedRect(
                    context, (float)(this.x + layerOffset), (float)(this.y + layerOffset), (float)this.width, totalHeight, shadowRadius, shadowColor
                );
        }
    }

    private void renderHeaderWithGlass(class_332 context, double alpha) {
        Color headerBg = ClickGUIModule.getColor(ClickGUIModule.getHeaderBackground(), alpha);
        Color glassOverlay = new Color(255, 255, 255, (int)(14.0 * alpha));
        float radius = ClickGUIModule.getGuiCornerRadiusScaled(1.0);
        float extendedHeight = (float)this.headerHeight + Math.max(2.0F, radius);
        Renderer.get().scissor(context, (float)this.x, (float)this.y, (float)this.width, (float)this.headerHeight);
        Renderer.get().drawRoundedRectStyled(context, (float)this.x, (float)this.y, (float)this.width, extendedHeight, radius, headerBg, GuiShaderStyle.HEADER);
        Renderer.get()
            .drawRoundedRectStyled(context, (float)this.x, (float)this.y, (float)this.width, extendedHeight, radius, glassOverlay, GuiShaderStyle.HEADER_GLASS);
        Renderer.get().resetScissor();
        if (ClickGUIModule.useSeparator()) {
            Color divider = ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), alpha * 0.2);
            Renderer.get().drawRect(context, (float)this.x + 1.0F, (float)(this.y + this.headerHeight), (float)this.width - 2.0F, 1.0F, divider);
        }
    }

    private void drawTopRoundedFill(class_332 context, float drawX, float drawY, float drawW, float drawH, float radius, Color color) {
        if (!(drawW <= 0.0F) && !(drawH <= 0.0F)) {
            float clampedRadius = Math.min(radius, drawW * 0.5F);
            if (clampedRadius <= 0.5F) {
                Renderer.get().drawRect(context, drawX, drawY, drawW, drawH, color);
            } else {
                float capH = Math.min(drawH, clampedRadius);
                if (capH > 0.0F) {
                    Renderer.get().scissor(context, drawX, drawY, drawW, capH);
                    Renderer.get().drawRoundedRect(context, drawX, drawY, drawW, Math.max(drawH, clampedRadius * 2.0F), clampedRadius, color);
                    Renderer.get().resetScissor();
                }

                float bodyY = drawY + capH;
                float bodyH = drawH - capH;
                if (bodyH > 0.0F) {
                    Renderer.get().drawRect(context, drawX, bodyY, drawW, bodyH, color);
                }
            }
        }
    }

    private void renderSearchBox(class_332 context, int mouseX, int mouseY, double searchY, double alpha, double frameDeltaSeconds) {
        double searchX = this.x + 2.0;
        double searchW = this.width - 4.0;
        boolean hovered = mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + this.searchBoxHeight;
        this.searchHoverAnim = hovered ? 1.0 : 0.0;
        Color bgBase = ClickGUIModule.getColor(ClickGUIModule.getModuleButtonBackground(), alpha);
        Color bgHover = ClickGUIModule.getColor(ClickGUIModule.getModuleButtonHoverBackground(), alpha);
        Color searchBg = this.blendColors(bgBase, bgHover, this.searchFocused ? 1.0 : this.searchHoverAnim);
        Color searchGlass = new Color(255, 255, 255, (int)((6.0 + 3.0 * (this.searchFocused ? 1.0 : this.searchHoverAnim)) * alpha));
        float searchRadius = Math.min(ClickGUIModule.getGuiCornerRadiusScaled(0.5), (float)this.searchBoxHeight * 0.5F);
        Renderer.get()
            .drawRoundedRectStyled(
                context, (float)searchX, (float)searchY, (float)searchW, (float)this.searchBoxHeight, searchRadius, searchBg, GuiShaderStyle.SEARCH
            );
        Renderer.get()
            .drawRoundedRectStyled(
                context, (float)searchX, (float)searchY, (float)searchW, (float)this.searchBoxHeight, searchRadius, searchGlass, GuiShaderStyle.SEARCH
            );
        if (this.searchFocused || hovered) {
            Color border = ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), alpha * 0.7);
            Renderer.get()
                .drawRoundedRectOutline(context, (float)searchX, (float)searchY, (float)searchW, (float)this.searchBoxHeight, searchRadius, 1.0F, border);
        }

        String displayText = this.searchQuery.isEmpty() ? "Search modules..." : this.searchQuery;
        Color textColor = this.searchQuery.isEmpty()
            ? ClickGUIModule.getColor(ClickGUIModule.getSearchBoxPlaceholder(), alpha)
            : ClickGUIModule.getColor(ClickGUIModule.getSearchBoxText(), alpha);
        float fontSize = 10.0F;
        float textHeight = Renderer.get().getTextHeight(fontSize);
        float textY = (float)(searchY + (this.searchBoxHeight - textHeight) / 2.0);
        Renderer.get().drawText(context, displayText, (float)(searchX + 6.0), textY, fontSize, textColor, false);
    }

    private double getAnimationDeltaSeconds() {
        long now = System.nanoTime();
        if (this.lastAnimationNanos < 0L) {
            this.lastAnimationNanos = now;
            return 0.008333333333333333;
        } else {
            double dt = (now - this.lastAnimationNanos) / 1.0E9;
            this.lastAnimationNanos = now;
            return dt <= 0.0 ? 0.004166666666666667 : Math.max(0.004166666666666667, Math.min(0.05, dt));
        }
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isHeaderHovered(mouseX, mouseY)) {
            if (!this.collapsed && this.isSearchBoxHovered(mouseX, mouseY)) {
                this.searchFocused = true;
            } else {
                this.searchFocused = false;
                if (!this.collapsed && this.collapseAnimation > 0.5) {
                    this.buttons.forEach(b -> b.mouseClicked(mouseX, mouseY, button));
                }
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.collapsed) {
            this.buttons.forEach(b -> b.mouseReleased(mouseX, mouseY, button));
        }
    }

    private void toggleCollapse() {
        this.collapsed = !this.collapsed;
        if (this.collapsed) {
            this.isCollapsing = true;
            this.isExpanding = false;
        } else {
            this.isExpanding = true;
            this.isCollapsing = false;
        }
    }

    double getTotalHeight() {
        return !this.collapsed && this.collapseAnimation != 0.0
            ? this.headerHeight + (this.getTotalHeightExpanded() - this.headerHeight) * this.collapseAnimation
            : this.headerHeight;
    }

    private double getTotalHeightExpanded() {
        double h = this.headerHeight;
        if (!this.collapsed && ClickGUIModule.useSearch()) {
            h += this.searchBoxHeight;
        }

        for (ModuleButton b : this.buttons) {
            if (!ClickGUIModule.useSearch() || this.searchQuery.isEmpty() || b.getModule().getName().toLowerCase().contains(this.searchQuery.toLowerCase())) {
                h += b.getTotalHeight();
            }
        }

        return h + 2.5;
    }

    private boolean isHeaderHovered(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.headerHeight;
    }

    private boolean isSearchBoxHovered(double mouseX, double mouseY) {
        if (!ClickGUIModule.useSearch()) {
            return false;
        } else {
            double searchY = this.y + this.headerHeight;
            double searchX = this.x + 2.0;
            double searchW = this.width - 4.0;
            return mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + this.searchBoxHeight;
        }
    }

    public List<ModuleButton> getButtons() {
        return this.buttons;
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchFocused && ClickGUIModule.useSearch()) {
            if (keyCode == 259 && !this.searchQuery.isEmpty()) {
                this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
                return;
            }

            if (keyCode == 256) {
                this.searchFocused = false;
                return;
            }
        }

        if (!this.collapsed) {
            this.buttons.forEach(b -> b.keyPressed(keyCode, scanCode, modifiers));
        }
    }

    public void charTyped(char chr, int modifiers) {
        if (this.searchFocused && ClickGUIModule.useSearch()) {
            if (chr >= ' ' && chr <= '~') {
                this.searchQuery = this.searchQuery + chr;
            }
        } else {
            if (!this.collapsed) {
                this.buttons.forEach(b -> b.charTyped(chr, modifiers));
            }
        }
    }

    private Color blendColors(Color a, Color b, double t) {
        float clamped = (float)Math.max(0.0, Math.min(1.0, t));
        int r = (int)(a.getRed() + (b.getRed() - a.getRed()) * clamped);
        int g = (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * clamped);
        int bl = (int)(a.getBlue() + (b.getBlue() - a.getBlue()) * clamped);
        int al = (int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * clamped);
        return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, bl)), Math.max(0, Math.min(255, al)));
    }
}
