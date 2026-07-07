package com.slither.cyemer.gui.new_ui;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.friend.Friend;
import com.slither.cyemer.friend.FriendKeybindManager;
import com.slither.cyemer.friend.FriendManager;
import com.slither.cyemer.friend.UUIDFetcher;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.implementation.ClickGUIModule;
import com.slither.cyemer.util.GuiShaderStyle;
import com.slither.cyemer.util.ModuleAccess;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1011;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3298;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public class FriendlistPanel {
    public double x;
    public double y;
    public double width;
    public double headerHeight;
    private final List<FriendlistPanel.FriendEntry> friendEntries = new ArrayList<>();
    public boolean dragging = false;
    private double dragX;
    private double dragY;
    private boolean collapsed = false;
    private double collapseAnimation = 1.0;
    private boolean isCollapsing = false;
    private boolean isExpanding = false;
    private String searchQuery = "";
    private boolean searchFocused = false;
    private double searchBoxHeight = 26.0;
    private double keybindHeight = 18.0;
    private double addFriendBoxHeight = 20.0;
    private FriendlistPanel.FriendEntry hoveredEntry = null;
    private FriendlistPanel.FriendEntry editingEntry = null;
    private boolean waitingForKeybind = false;
    private String addFriendInput = "";
    private boolean addFriendFocused = false;
    private int friendIconImageId = -1;
    private int friendIconTextureWidth = 256;
    private int friendIconTextureHeight = 256;
    private String lastIconColorMode = null;
    private static final float CONTROL_RADIUS = 2.0F;
    private static final float ENTRY_RADIUS = 2.0F;
    private static final float TRASH_RADIUS = 2.0F;
    private static final double BOTTOM_CORNER_GAP = 1.0;

    public FriendlistPanel(double x, double y, double width, double headerHeight) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.headerHeight = headerHeight;
        this.refreshFriendEntries();
        this.loadFriendIcon();
    }

    private void loadFriendIcon() {
        try {
            String preferred = "friend.png";
            String fallback = "customer.png";
            this.friendIconImageId = this.tryLoadFriendIcon(preferred);
            if (this.friendIconImageId == -1) {
                this.friendIconImageId = this.tryLoadFriendIcon(fallback);
            }
        } catch (Exception var3) {
            this.friendIconImageId = -1;
        }

        this.lastIconColorMode = ClickGUIModule.getIconColorMode();
    }

    private int tryLoadFriendIcon(String fileName) {
        if (fileName != null && !fileName.isEmpty()) {
            int iconId = Renderer.get().createImageFromFile("dynamic_fps:textures/gui/" + fileName);
            if (iconId == -1) {
                String lower = fileName.toLowerCase(Locale.ROOT);
                if (!lower.equals(fileName)) {
                    iconId = Renderer.get().createImageFromFile("dynamic_fps:textures/gui/" + lower);
                }
            }

            if (iconId != -1) {
                this.updateFriendIconTextureSize(fileName);
                return iconId;
            } else {
                iconId = Renderer.get().createImageFromFile("assets/dynamic_fps/textures/gui/" + fileName);
                if (iconId == -1) {
                    String lower = fileName.toLowerCase(Locale.ROOT);
                    if (!lower.equals(fileName)) {
                        iconId = Renderer.get().createImageFromFile("assets/dynamic_fps/textures/gui/" + lower);
                    }
                }

                if (iconId != -1) {
                    this.updateFriendIconTextureSize(fileName);
                }

                return iconId;
            }
        } else {
            return -1;
        }
    }

    private void updateFriendIconTextureSize(String fileName) {
        this.friendIconTextureWidth = 256;
        this.friendIconTextureHeight = 256;
        class_310 client = class_310.method_1551();
        if (client != null && client.method_1478() != null) {
            class_2960 id = this.resolveGuiTextureIdentifier(fileName);
            if (id != null) {
                Optional<class_3298> optional = client.method_1478().method_14486(id);
                if (!optional.isEmpty()) {
                    try (InputStream stream = optional.get().method_14482()) {
                        class_1011 image = class_1011.method_4309(stream);

                        try {
                            this.friendIconTextureWidth = Math.max(1, image.method_4307());
                            this.friendIconTextureHeight = Math.max(1, image.method_4323());
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

    public void refreshFriendEntries() {
        this.friendEntries.clear();
        List<Friend> friends = FriendManager.getInstance().getFriends();
        friends.sort(Comparator.comparing(Friend::getName, String.CASE_INSENSITIVE_ORDER));

        for (Friend friend : friends) {
            this.friendEntries.add(new FriendlistPanel.FriendEntry(friend));
        }
    }

    public String render(class_332 context, int mouseX, int mouseY, float delta, double alpha) {
        if (this.lastIconColorMode == null || !this.lastIconColorMode.equalsIgnoreCase(ClickGUIModule.getIconColorMode())) {
            this.loadFriendIcon();
        }

        String tooltip = null;
        float panelRadius = ClickGUIModule.getGuiCornerRadiusScaled(1.0);
        Renderer.get().resetScissor();
        if (this.isCollapsing && this.collapseAnimation > 0.0) {
            this.collapseAnimation -= delta * 6.0;
            if (this.collapseAnimation <= 0.0) {
                this.collapseAnimation = 0.0;
                this.isCollapsing = false;
            }
        } else if (this.isExpanding && this.collapseAnimation < 1.0) {
            this.collapseAnimation += delta * 6.0;
            if (this.collapseAnimation >= 1.0) {
                this.collapseAnimation = 1.0;
                this.isExpanding = false;
            }
        }

        Color panelBg = ClickGUIModule.getColor(ClickGUIModule.getPanelBackground(), alpha);
        Color glassOverlay = new Color(255, 255, 255, (int)(10.0 * alpha));
        if (ClickGUIModule.useBlurBackground()) {
            float blur = ClickGUIModule.getBlurStrength();
            if (blur > 0.0F) {
                Renderer.get()
                    .drawBlur(context, (float)this.x + 1.0F, (float)this.y + 1.0F, (float)this.width - 2.0F, (float)this.getTotalHeight() - 2.0F, blur);
            }
        }

        Renderer.get()
            .drawRoundedRectStyled(
                context, (float)this.x, (float)this.y, (float)this.width, (float)this.getTotalHeight(), panelRadius, panelBg, GuiShaderStyle.PANEL
            );
        Renderer.get()
            .drawRoundedRectStyled(
                context, (float)this.x, (float)this.y, (float)this.width, (float)this.getTotalHeight(), panelRadius, glassOverlay, GuiShaderStyle.PANEL_GLASS
            );
        this.renderHeaderWithGlass(context, alpha);
        String titleString = this.collapsed ? "Friends [" + this.friendEntries.size() + "]" : "Friends";
        float titleX = (float)(this.x + 10.0);
        float titleY = (float)(this.y + (this.headerHeight - 13.0) / 2.0);
        Color titleColor = new Color(255, 255, 255, (int)(255.0 * alpha));
        Renderer.get().drawText(context, titleString, titleX, titleY, 13.0F, titleColor, false);
        float logoSize = 17.0F;
        float logoX = (float)(this.x + this.width - logoSize - 9.0);
        float logoY = (float)(this.y + (this.headerHeight - logoSize) / 2.0);
        if (this.friendIconImageId != -1) {
            Renderer.get()
                .drawTexture(
                    context,
                    this.friendIconImageId,
                    logoX,
                    logoY,
                    logoSize,
                    logoSize,
                    0.0F,
                    0.0F,
                    this.friendIconTextureWidth,
                    this.friendIconTextureHeight,
                    this.friendIconTextureWidth,
                    this.friendIconTextureHeight
                );
        }

        if (this.collapseAnimation > 0.0) {
            double contentY = this.y + this.headerHeight + 1.0;
            double contentAlpha = alpha * this.collapseAnimation;
            if (!this.collapsed) {
                this.renderKeybindBox(context, mouseX, mouseY, contentAlpha);
                contentY += this.keybindHeight + 4.0;
                this.renderSearchBox(context, mouseX, mouseY, contentAlpha);
                contentY += this.searchBoxHeight;
                this.renderAddFriendInput(context, mouseX, mouseY, contentY, contentAlpha);
                contentY += this.addFriendBoxHeight + 4.0;
            }

            double availableHeight = (
                    this.getTotalHeightExpanded() - this.headerHeight - 1.0 - this.keybindHeight - this.searchBoxHeight - this.addFriendBoxHeight - 12.0
                )
                * this.collapseAnimation;
            Renderer.get().scissor(context, (float)this.x, (float)contentY, (float)this.width, (float)availableHeight);
            double entryY = contentY;
            this.hoveredEntry = null;

            for (FriendlistPanel.FriendEntry entry : this.friendEntries) {
                if (this.searchQuery.isEmpty() || entry.friend.getName().toLowerCase().contains(this.searchQuery.toLowerCase())) {
                    entry.x = this.x + 3.0;
                    entry.y = entryY;
                    entry.width = this.width - 6.0;
                    entry.height = 18.0;
                    String entryTooltip = entry.render(context, mouseX, mouseY, delta, contentAlpha);
                    if (entryTooltip != null && !entryTooltip.isEmpty()) {
                        tooltip = entryTooltip;
                    }

                    if (entry.isHovered(mouseX, mouseY)) {
                        this.hoveredEntry = entry;
                    }

                    entryY += entry.height + 2.0;
                }
            }

            Renderer.get().resetScissor();
        }

        return tooltip;
    }

    private void renderShadow(class_332 context, double alpha) {
        float totalHeight = (float)this.getTotalHeight();
        float panelRadius = ClickGUIModule.getCornerRadiusScaled(1.0);
        float shadowRadius = Math.min(8.0F, panelRadius);
        int layers = 8;
        float maxOffset = 6.0F;

        for (int i = 0; i < layers; i++) {
            float progress = (float)i / layers;
            float layerOffset = 2.0F + progress * maxOffset;
            float layerAlpha = (float)(alpha * 0.08 * (1.0 - progress * 0.7));
            Color shadowColor = new Color(0, 0, 0, (int)(layerAlpha * 255.0F));
            Renderer.get()
                .drawRoundedRect(
                    context, (float)(this.x + layerOffset), (float)(this.y + layerOffset), (float)this.width, totalHeight, shadowRadius, shadowColor
                );
        }
    }

    private void renderHeaderWithGlass(class_332 context, double alpha) {
        Color headerBg = ClickGUIModule.getColor(ClickGUIModule.getHeaderBackground(), alpha);
        Color glassOverlay = new Color(255, 255, 255, (int)(12.0 * alpha));
        float radius = ClickGUIModule.getGuiCornerRadiusScaled(1.0);
        float extendedHeight = (float)this.headerHeight + 8.0F;
        Renderer.get().scissor(context, (float)this.x, (float)this.y, (float)this.width, (float)this.headerHeight);
        Renderer.get().drawRoundedRectStyled(context, (float)this.x, (float)this.y, (float)this.width, extendedHeight, radius, headerBg, GuiShaderStyle.HEADER);
        Renderer.get()
            .drawRoundedRectStyled(context, (float)this.x, (float)this.y, (float)this.width, extendedHeight, radius, glassOverlay, GuiShaderStyle.HEADER_GLASS);
        Renderer.get().resetScissor();
        if (ClickGUIModule.useSeparator()) {
            Renderer.get()
                .drawRect(
                    context,
                    (float)(this.x + 1.0),
                    (float)(this.y + this.headerHeight),
                    (float)this.width - 2.0F,
                    1.0F,
                    ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), alpha * 0.22)
                );
        }
    }

    private void renderKeybindBox(class_332 context, int mouseX, int mouseY, double alpha) {
        float inputRadius = 2.0F;
        double keybindY = this.y + this.headerHeight + 3.0;
        double keybindX = this.x + 5.0;
        double keybindW = this.width - 10.0;
        boolean hovered = mouseX >= keybindX && mouseX <= keybindX + keybindW && mouseY >= keybindY && mouseY <= keybindY + this.keybindHeight;
        Color keybindBg = hovered
            ? ClickGUIModule.getColor(ClickGUIModule.getSearchBoxFocusedBackground(), alpha)
            : ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBackground(), alpha);
        Renderer.get()
            .drawRoundedRectStyled(
                context, (float)keybindX, (float)keybindY, (float)keybindW, (float)this.keybindHeight, inputRadius, keybindBg, GuiShaderStyle.CONTROL
            );
        if (hovered) {
            Color borderColor = ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), alpha);
            Renderer.get()
                .drawRoundedRectOutline(context, (float)keybindX, (float)keybindY, (float)keybindW, (float)this.keybindHeight, inputRadius, 1.5F, borderColor);
        }

        String displayText = FriendKeybindManager.getInstance().isBinding() ? "Key:" : "Key: " + FriendKeybindManager.getInstance().getKeyDisplayName();
        Color textColor = FriendKeybindManager.getInstance().isBinding()
            ? ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledText(), alpha)
            : ClickGUIModule.getColor(ClickGUIModule.getSearchBoxText(), alpha);
        Renderer.get().drawText(context, displayText, (float)(keybindX + 5.0), (float)(keybindY + 4.0), 10.0F, textColor, false);
    }

    private void renderSearchBox(class_332 context, int mouseX, int mouseY, double alpha) {
        float inputRadius = 2.0F;
        double searchY = this.y + this.headerHeight + this.keybindHeight + 5.0;
        Color searchBg = ClickGUIModule.getColor(
            this.searchFocused ? ClickGUIModule.getSearchBoxFocusedBackground() : ClickGUIModule.getSearchBoxBackground(), alpha
        );
        Renderer.get()
            .drawRoundedRectStyled(
                context,
                (float)this.x + 5.0F,
                (float)searchY,
                (float)this.width - 10.0F,
                (float)this.searchBoxHeight,
                inputRadius,
                searchBg,
                GuiShaderStyle.SEARCH
            );
        if (this.searchFocused || !this.searchQuery.isEmpty()) {
            Color borderColor = ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), alpha);
            Renderer.get()
                .drawRoundedRectOutline(
                    context, (float)this.x + 5.0F, (float)searchY, (float)this.width - 10.0F, (float)this.searchBoxHeight, inputRadius, 1.5F, borderColor
                );
        }

        String displayText = this.searchQuery.isEmpty() ? "Search friends" : this.searchQuery;
        Color textColor = this.searchQuery.isEmpty()
            ? ClickGUIModule.getColor(ClickGUIModule.getSearchBoxPlaceholder(), alpha)
            : ClickGUIModule.getColor(ClickGUIModule.getSearchBoxText(), alpha);
        Renderer.get().drawText(context, displayText, (float)(this.x + 10.0), (float)(searchY + 7.0), 11.0F, textColor, false);
    }

    private void renderAddFriendInput(class_332 context, int mouseX, int mouseY, double inputY, double alpha) {
        float inputRadius = 2.0F;
        double inputX = this.x + 5.0;
        double inputW = this.width - 10.0;
        boolean hovered = mouseX >= inputX && mouseX <= inputX + inputW && mouseY >= inputY && mouseY <= inputY + this.addFriendBoxHeight;
        Color inputBg = this.addFriendFocused
            ? ClickGUIModule.getColor(ClickGUIModule.getSearchBoxFocusedBackground(), alpha)
            : ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBackground(), alpha);
        Renderer.get()
            .drawRoundedRectStyled(
                context, (float)inputX, (float)inputY, (float)inputW, (float)this.addFriendBoxHeight, inputRadius, inputBg, GuiShaderStyle.CONTROL
            );
        if (this.addFriendFocused || hovered) {
            Color borderColor = ClickGUIModule.getColor(this.addFriendFocused ? ClickGUIModule.getSearchBoxBorder() : new Color(100, 100, 100, 100), alpha);
            Renderer.get()
                .drawRoundedRectOutline(context, (float)inputX, (float)inputY, (float)inputW, (float)this.addFriendBoxHeight, inputRadius, 1.5F, borderColor);
        }

        String displayText = this.addFriendInput.isEmpty() ? "+ Add friend name" : this.addFriendInput;
        Color textColor = this.addFriendInput.isEmpty()
            ? ClickGUIModule.getColor(ClickGUIModule.getSearchBoxPlaceholder(), alpha)
            : ClickGUIModule.getColor(ClickGUIModule.getSearchBoxText(), alpha);
        Renderer.get().drawText(context, displayText, (float)(inputX + 5.0), (float)(inputY + 5.0), 10.0F, textColor, false);
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isHeaderHovered(mouseX, mouseY)) {
            if (!this.collapsed && this.isKeybindBoxHovered(mouseX, mouseY)) {
                if (FriendKeybindManager.getInstance().isBinding() && this.waitingForKeybind) {
                    FriendKeybindManager.getInstance().setKeyCode(button - 100);
                    FriendKeybindManager.getInstance().setBinding(false);
                    this.waitingForKeybind = false;
                } else {
                    if (button == 0) {
                        FriendKeybindManager.getInstance().setBinding(true);
                        FriendKeybindManager.getInstance().setBindStartTime(System.currentTimeMillis());
                        this.waitingForKeybind = true;
                    } else if (button == 1) {
                        FriendKeybindManager.getInstance().setKeyCode(-1);
                        FriendKeybindManager.getInstance().setBinding(false);
                        this.waitingForKeybind = false;
                    }
                }
            } else {
                if (FriendKeybindManager.getInstance().isBinding()) {
                    FriendKeybindManager.getInstance().setBinding(false);
                    this.waitingForKeybind = false;
                }

                if (!this.collapsed && this.isSearchBoxHovered(mouseX, mouseY)) {
                    this.searchFocused = true;
                    this.addFriendFocused = false;
                } else {
                    this.searchFocused = false;
                    if (!this.collapsed && this.isAddFriendInputHovered(mouseX, mouseY)) {
                        this.addFriendFocused = true;
                        this.searchFocused = false;
                    } else {
                        this.addFriendFocused = false;
                        if (!this.collapsed && this.collapseAnimation > 0.5) {
                            for (FriendlistPanel.FriendEntry entry : new ArrayList<>(this.friendEntries)) {
                                if (this.searchQuery.isEmpty() || entry.friend.getName().toLowerCase().contains(this.searchQuery.toLowerCase())) {
                                    entry.mouseClicked(mouseX, mouseY, button);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.collapsed) {
            for (FriendlistPanel.FriendEntry entry : this.friendEntries) {
                entry.mouseReleased(mouseX, mouseY, button);
            }
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

    public double getTotalHeight() {
        return !this.collapsed && this.collapseAnimation != 0.0
            ? this.headerHeight + (this.getTotalHeightExpanded() - this.headerHeight) * this.collapseAnimation
            : this.headerHeight;
    }

    private double getTotalHeightExpanded() {
        double h = this.headerHeight + 1.0 + this.keybindHeight + this.searchBoxHeight + this.addFriendBoxHeight + 12.0;

        for (FriendlistPanel.FriendEntry entry : this.friendEntries) {
            if (this.searchQuery.isEmpty() || entry.friend.getName().toLowerCase().contains(this.searchQuery.toLowerCase())) {
                h += entry.height + 2.0;
            }
        }

        return h + 1.0;
    }

    private boolean isHeaderHovered(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.headerHeight;
    }

    private boolean isKeybindBoxHovered(double mouseX, double mouseY) {
        double keybindY = this.y + this.headerHeight + 3.0;
        double keybindX = this.x + 5.0;
        double keybindW = this.width - 10.0;
        return mouseX >= keybindX && mouseX <= keybindX + keybindW && mouseY >= keybindY && mouseY <= keybindY + this.keybindHeight;
    }

    private boolean isSearchBoxHovered(double mouseX, double mouseY) {
        double searchY = this.y + this.headerHeight + this.keybindHeight + 5.0;
        double searchX = this.x + 5.0;
        double searchW = this.width - 10.0;
        return mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + this.searchBoxHeight;
    }

    private boolean isAddFriendInputHovered(double mouseX, double mouseY) {
        double inputY = this.y + this.headerHeight + this.keybindHeight + this.searchBoxHeight + 5.0;
        double inputX = this.x + 5.0;
        double inputW = this.width - 10.0;
        return mouseX >= inputX && mouseX <= inputX + inputW && mouseY >= inputY && mouseY <= inputY + this.addFriendBoxHeight;
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (FriendKeybindManager.getInstance().isBinding()) {
            if (keyCode == 256) {
                FriendKeybindManager.getInstance().setKeyCode(-1);
            } else {
                FriendKeybindManager.getInstance().setKeyCode(keyCode);
            }

            FriendKeybindManager.getInstance().setBinding(false);
            this.waitingForKeybind = false;
        } else {
            if (this.addFriendFocused) {
                if (keyCode == 259 && !this.addFriendInput.isEmpty()) {
                    this.addFriendInput = this.addFriendInput.substring(0, this.addFriendInput.length() - 1);
                    return;
                }

                if (keyCode == 257 || keyCode == 335) {
                    if (!this.addFriendInput.isEmpty()) {
                        String username = this.addFriendInput;
                        this.addFriendInput = "";
                        UUIDFetcher.fetchUUID(username)
                            .thenAccept(
                                uuid -> {
                                    if (uuid != null) {
                                        FriendManager.getInstance().addFriend(username, uuid);
                                        this.refreshFriendEntries();
                                        Module notifications = Cyemer.getInstance().getModuleManager().getModule("Notifications");
                                        if (notifications != null) {
                                            ModuleAccess.invoke(
                                                notifications, "success", new Class[]{String.class, String.class}, "Friends", "Added " + username + "!"
                                            );
                                        }
                                    } else {
                                        Module notifications = Cyemer.getInstance().getModuleManager().getModule("Notifications");
                                        if (notifications != null) {
                                            ModuleAccess.invoke(
                                                notifications,
                                                "error",
                                                new Class[]{String.class, String.class},
                                                "Friends",
                                                "Player '" + username + "' not found!"
                                            );
                                        }
                                    }
                                }
                            );
                    }

                    return;
                }

                if (keyCode == 256) {
                    this.addFriendFocused = false;
                    this.addFriendInput = "";
                    return;
                }
            }

            if (this.searchFocused) {
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
                for (FriendlistPanel.FriendEntry entry : this.friendEntries) {
                    entry.keyPressed(keyCode, scanCode, modifiers);
                }
            }
        }
    }

    public void charTyped(char chr, int modifiers) {
        if (this.addFriendFocused) {
            if (chr >= ' ' && chr <= '~') {
                this.addFriendInput = this.addFriendInput + chr;
            }
        } else if (this.searchFocused) {
            if (chr >= ' ' && chr <= '~') {
                this.searchQuery = this.searchQuery + chr;
            }
        } else {
            if (!this.collapsed) {
                for (FriendlistPanel.FriendEntry entry : this.friendEntries) {
                    entry.charTyped(chr, modifiers);
                }
            }
        }
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    @Environment(EnvType.CLIENT)
    private class FriendEntry {
        public double x;
        public double y;
        public double width;
        public double height;
        public final Friend friend;
        private boolean editingNote = false;
        private String tempNote = "";

        public FriendEntry(Friend friend) {
            this.friend = friend;
        }

        public String render(class_332 context, int mouseX, int mouseY, float delta, double alpha) {
            float entryRadius = 2.0F;
            float trashRadius = 2.0F;
            boolean hovered = this.isHovered(mouseX, mouseY);
            Color bgColor = hovered ? ClickGUIModule.getColor(new Color(255, 255, 255, 20), alpha) : ClickGUIModule.getColor(new Color(0, 0, 0, 0), alpha);
            Renderer.get()
                .drawRoundedRectStyled(context, (float)this.x, (float)this.y, (float)this.width, (float)this.height, entryRadius, bgColor, GuiShaderStyle.ROW);
            String displayName = this.friend.getDisplayName();
            Color textColor = ClickGUIModule.getColor(ClickGUIModule.getPanelTitleText(), alpha);
            Renderer.get().drawText(context, displayName, (float)(this.x + 5.0), (float)(this.y + 4.0), 10.0F, textColor, false);
            float trashX = (float)(this.x + this.width - 18.0);
            float trashY = (float)(this.y + 2.0);
            float trashSize = 14.0F;
            boolean trashHovered = mouseX >= trashX && mouseX <= trashX + trashSize && mouseY >= trashY && mouseY <= trashY + trashSize;
            Color trashColor = trashHovered
                ? ClickGUIModule.getColor(new Color(255, 80, 80, 180), alpha)
                : ClickGUIModule.getColor(new Color(200, 60, 60, 120), alpha);
            Renderer.get().drawRoundedRectStyled(context, trashX, trashY, trashSize, trashSize, trashRadius, trashColor, GuiShaderStyle.CONTROL);
            Color iconColor = ClickGUIModule.getColor(new Color(255, 255, 255, 255), alpha);
            Renderer.get().drawRect(context, trashX + 2.0F, trashY + 2.0F, 10.0F, 1.5F, iconColor);
            Renderer.get().drawRect(context, trashX + 5.0F, trashY + 1.0F, 4.0F, 1.0F, iconColor);
            Renderer.get().drawRect(context, trashX + 3.0F, trashY + 4.0F, 8.0F, 8.0F, iconColor);
            Renderer.get().drawRect(context, trashX + 5.0F, trashY + 5.0F, 1.0F, 6.0F, new Color(0, 0, 0, 100));
            Renderer.get().drawRect(context, trashX + 8.0F, trashY + 5.0F, 1.0F, 6.0F, new Color(0, 0, 0, 100));
            return hovered ? "Click to remove friend" : null;
        }

        public void mouseClicked(double mouseX, double mouseY, int button) {
            if (this.isHovered(mouseX, mouseY)) {
                float trashX = (float)(this.x + this.width - 18.0);
                float trashY = (float)(this.y + 2.0);
                float trashSize = 14.0F;
                if (mouseX >= trashX && mouseX <= trashX + trashSize && mouseY >= trashY && mouseY <= trashY + trashSize) {
                    FriendManager.getInstance().removeFriend(this.friend.getUuid());
                    FriendlistPanel.this.refreshFriendEntries();
                }
            }
        }

        public void mouseReleased(double mouseX, double mouseY, int button) {
        }

        public void keyPressed(int keyCode, int scanCode, int modifiers) {
        }

        public void charTyped(char chr, int modifiers) {
        }

        public boolean isHovered(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
        }
    }
}
