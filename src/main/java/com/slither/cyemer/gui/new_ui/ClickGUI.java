package com.slither.cyemer.gui.new_ui;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.friend.Friend;
import com.slither.cyemer.friend.FriendKeybindManager;
import com.slither.cyemer.friend.FriendManager;
import com.slither.cyemer.friend.UUIDFetcher;
import com.slither.cyemer.gui.new_ui.old.ModuleButton;
import com.slither.cyemer.gui.new_ui.settings.BooleanComponent;
import com.slither.cyemer.gui.new_ui.settings.SettingComponent;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.implementation.ClickGUIModule;
import com.slither.cyemer.util.ModuleAccess;
import com.slither.cyemer.util.Renderer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11905;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_3532;
import net.minecraft.class_437;
import org.joml.Vector2d;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class ClickGUI extends class_437 {
    final List<CategoryTab> categoryTabs = new ArrayList<>();
    final Map<Category, List<ModuleButton>> categoryButtons = new HashMap<>();
    int selectedTabIndex = 0;
    boolean friendsTabSelected = false;
    String friendSearchQuery = "";
    boolean friendSearchFocused = false;
    String addFriendInput = "";
    boolean addFriendFocused = false;
    double friendsScrollOffset = 0.0;
    double targetFriendsScroll = 0.0;
    public static double cameraX = 0.0;
    public static double cameraY = 0.0;
    public static double zoom = 1.0;
    double openAnimationProgress = 0.0;
    private final double animationDuration = 0.18;
    private double originalBackgroundBlur = -1.0;
    String searchQuery = "";
    boolean searchFocused = false;
    ModuleButton settingsPanelButton = null;
    boolean profileSettingsMode = false;
    double settingsSlideProgress = 0.0;
    double settingsScrollOffset = 0.0;
    double targetSettingsScroll = 0.0;
    double moduleScrollOffset = 0.0;
    double targetModuleScroll = 0.0;
    private long lastFrameTime = System.nanoTime();
    private final ClickGUIRenderer renderer = new ClickGUIRenderer(this);

    public ClickGUI() {
        super(class_2561.method_43470("Click GUI"));
        List<Category> categories = new ArrayList<>(List.of(Category.values()));
        categories.sort(Comparator.comparing(Enum::ordinal));

        for (Category category : categories) {
            CategoryTab tab = new CategoryTab(category);
            this.categoryTabs.add(tab);
            List<ModuleButton> buttons = new ArrayList<>();
            Cyemer.getInstance()
                .getModuleManager()
                .getModules()
                .stream()
                .filter(m -> m.getCategory() == category)
                .forEach(m -> buttons.add(new ModuleButton(m)));
            this.categoryButtons.put(category, buttons);
        }
    }

    protected void method_25426() {
        super.method_25426();
        this.openAnimationProgress = 0.0;
        cameraX = 0.0;
        cameraY = 0.0;
        this.lastFrameTime = System.nanoTime();
        Renderer.get().init();
        ClickGUIIconCache.retryFailedIcons();
    }

    private double getDeltaSeconds() {
        long now = System.nanoTime();
        double dt = (now - this.lastFrameTime) / 1.0E9;
        this.lastFrameTime = now;
        return Math.max(0.004166666666666667, Math.min(0.05, dt));
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        double dt = this.getDeltaSeconds();
        if (this.openAnimationProgress < 1.0) {
            this.openAnimationProgress = Math.min(this.openAnimationProgress + dt / 0.18, 1.0);
        }

        double slideTarget = this.settingsPanelButton != null ? 1.0 : 0.0;
        this.settingsSlideProgress = this.settingsSlideProgress + (slideTarget - this.settingsSlideProgress) * (1.0 - Math.exp(-32.0 * dt));
        if (Math.abs(this.settingsSlideProgress - slideTarget) < 0.002) {
            this.settingsSlideProgress = slideTarget;
        }

        this.moduleScrollOffset = this.moduleScrollOffset + (this.targetModuleScroll - this.moduleScrollOffset) * (1.0 - Math.exp(-18.0 * dt));
        if (Math.abs(this.moduleScrollOffset - this.targetModuleScroll) < 0.5) {
            this.moduleScrollOffset = this.targetModuleScroll;
        }

        this.settingsScrollOffset = this.settingsScrollOffset + (this.targetSettingsScroll - this.settingsScrollOffset) * (1.0 - Math.exp(-18.0 * dt));
        if (Math.abs(this.settingsScrollOffset - this.targetSettingsScroll) < 0.5) {
            this.settingsScrollOffset = this.targetSettingsScroll;
        }

        this.friendsScrollOffset = this.friendsScrollOffset + (this.targetFriendsScroll - this.friendsScrollOffset) * (1.0 - Math.exp(-18.0 * dt));
        if (Math.abs(this.friendsScrollOffset - this.targetFriendsScroll) < 0.5) {
            this.friendsScrollOffset = this.targetFriendsScroll;
        }

        double guiScale = this.field_22787.method_22683().method_4495();
        double autoZoom = Math.min(this.field_22789 * 0.92 / 620.0, this.field_22790 * 0.92 / 380.0);
        double finalZoom = zoom * autoZoom;
        float pixelRatio = (float)(guiScale * ClickGUIModule.getUiResolutionScale());
        if (Renderer.get().beginFrame(this.field_22789, this.field_22790, pixelRatio)) {
            try {
                double easedOpen = ClickGUILayout.backOut(this.openAnimationProgress);
                double alpha = class_3532.method_15350(this.openAnimationProgress * 2.5, 0.0, 1.0);
                if (alpha < 0.01) {
                    Renderer.get().endFrame();
                    return;
                }

                float originX = this.field_22789 / 2.0F;
                float originY = this.field_22790 / 2.0F;
                double boxX = -310.0;
                double boxY = -190.0;
                Renderer.get().save();
                Renderer.get().translate(originX, originY);
                Renderer.get().scale((float)finalZoom, (float)finalZoom);
                float scaleF = (float)(0.88 + 0.12 * easedOpen);
                Renderer.get().scale(scaleF, scaleF);
                Vector2d mouse = ClickGUILayout.transformMouse(mouseX, mouseY, finalZoom, scaleF, originX, originY);
                this.renderer.renderContainerBackground(context, boxX, boxY, 620.0, 380.0, alpha);
                this.renderer.renderSidebar(context, boxX, boxY, 62.0, 380.0, alpha, mouse, dt);
                double contentX = boxX + 62.0;
                Renderer.get().save();
                Renderer.get().scissor(context, (float)contentX, (float)boxY, 558.0F, 380.0F);
                String tooltip = null;
                double slideOffset = this.settingsSlideProgress * 558.0;
                if (this.settingsSlideProgress < 0.99) {
                    Renderer.get().save();
                    Renderer.get().translate((float)(-slideOffset), 0.0F);
                    if (this.friendsTabSelected) {
                        Renderer.get().scissor(context, (float)contentX, (float)boxY, 558.0F, 380.0F);
                        this.renderer.renderFriendsTab(context, contentX, boxY, 558.0, alpha * (1.0 - this.settingsSlideProgress), mouse);
                    } else {
                        double gridY = boxY + 8.0;
                        if (ClickGUIModule.useSearch()) {
                            this.renderer.renderSearchBox(context, contentX, gridY, 558.0, alpha * (1.0 - this.settingsSlideProgress), mouse);
                            gridY += 36.0;
                        }

                        double gridClipHeight = Math.max(0.0, 380.0 - (gridY - boxY));
                        Renderer.get().scissor(context, (float)contentX, (float)gridY, 558.0F, (float)gridClipHeight);
                        tooltip = this.renderer.renderModuleGrid(context, contentX, gridY, 558.0, alpha * (1.0 - this.settingsSlideProgress), mouse, delta, dt);
                    }

                    Renderer.get().scissor(context, (float)contentX, (float)boxY, 558.0F, 380.0F);
                    Renderer.get().restore();
                }

                if (this.settingsSlideProgress > 0.01 && this.settingsPanelButton != null) {
                    Renderer.get().save();
                    Renderer.get().translate((float)(558.0 - slideOffset), 0.0F);
                    if (this.profileSettingsMode) {
                        this.renderer.renderProfilePanel(context, contentX, boxY, 558.0, 380.0, alpha * this.settingsSlideProgress, mouse, dt);
                    } else {
                        this.renderer.renderSettingsPanel(context, contentX, boxY, 558.0, 380.0, alpha * this.settingsSlideProgress, mouse, dt);
                    }

                    Renderer.get().restore();
                }

                Renderer.get().resetScissor();
                Renderer.get().restore();
                Renderer.get().restore();
                if (tooltip != null && !tooltip.isEmpty() && this.settingsSlideProgress < 0.3) {
                    this.renderer.renderTooltip(context, tooltip, mouseX, mouseY, finalZoom);
                }

                Renderer.get().endFrame();
            } catch (Exception var37) {
                Renderer.forceVanillaRenderer();
            }
        }
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.isControlDown()) {
            double scrollAmount = verticalAmount != 0.0 ? verticalAmount : horizontalAmount;
            zoom = Math.max(0.5, Math.min(zoom * (1.0 + scrollAmount * 0.05), 2.0));
            return true;
        } else if (this.isInSettingsPanel() && this.settingsPanelButton != null) {
            double totalH = this.profileSettingsMode
                ? this.computeProfileContentHeight(this.settingsPanelButton) + 14.0
                : this.computeSettingsContentHeight(this.settingsPanelButton) + 14.0;
            double maxScroll = Math.max(0.0, totalH - 351.0);
            this.targetSettingsScroll = class_3532.method_15350(this.targetSettingsScroll - verticalAmount * 14.0, 0.0, maxScroll);
            return true;
        } else if (this.friendsTabSelected) {
            List<Friend> friends = FriendManager.getInstance().getFriends();
            double totalH = friends.size() * 38.0;
            double headerH = 92.0;
            double maxScroll = Math.max(0.0, totalH - (380.0 - headerH));
            this.targetFriendsScroll = class_3532.method_15350(this.targetFriendsScroll - verticalAmount * 14.0, 0.0, maxScroll);
            return true;
        } else {
            if (!this.isInSettingsPanel()) {
                List<ModuleButton> btns = ClickGUILayout.visibleButtons(this.selectedTabIndex, this.categoryTabs, this.categoryButtons, this.searchQuery);
                int rows = Math.max(1, (int)Math.ceil(btns.size() / 3.0));
                double gridH = rows * 42.0 + 20.0;
                double searchH = ClickGUIModule.useSearch() ? 36.0 : 0.0;
                double maxScroll = Math.max(0.0, gridH - (372.0 - searchH));
                this.targetModuleScroll = class_3532.method_15350(this.targetModuleScroll - verticalAmount * 14.0, 0.0, maxScroll);
            }

            return true;
        }
    }

    public boolean method_25402(class_11909 click, boolean doubleClick) {
        double autoZoom = Math.min(this.field_22789 * 0.92 / 620.0, this.field_22790 * 0.92 / 380.0);
        double finalZoom = zoom * autoZoom;
        float scaleF = (float)(0.88 + 0.12 * ClickGUILayout.backOut(this.openAnimationProgress));
        Vector2d m = ClickGUILayout.transformMouse(click.comp_4798(), click.comp_4799(), finalZoom, scaleF, this.field_22789 / 2.0F, this.field_22790 / 2.0F);
        double boxX = -310.0;
        double boxY = -190.0;
        int button = click.method_74245();
        if (this.handleSidebarClick(m, boxX, boxY, button)) {
            return super.method_25402(click, doubleClick);
        } else {
            double contentX = boxX + 62.0;
            if (this.isInSettingsPanel()) {
                if (this.handleSettingsPanelClick(m, contentX, boxY, button)) {
                    return super.method_25402(click, doubleClick);
                }
            } else if (this.friendsTabSelected) {
                this.handleFriendsTabClick(m, contentX, boxY, button);
            } else {
                double gridY = boxY + 8.0;
                if (ClickGUIModule.useSearch()) {
                    if (this.handleSearchClick(m, contentX, gridY)) {
                        return super.method_25402(click, doubleClick);
                    }

                    gridY += 36.0;
                }

                this.searchFocused = false;
                this.handleModuleGridClick(m, contentX, boxY, button);
            }

            return super.method_25402(click, doubleClick);
        }
    }

    public boolean method_25406(class_11909 click) {
        if (this.isInSettingsPanel() && this.settingsPanelButton != null) {
            for (SettingComponent comp : this.settingsPanelButton.getComponents()) {
                comp.mouseReleased(click.comp_4798(), click.comp_4799(), click.method_74245());
            }
        }

        return super.method_25406(click);
    }

    public boolean method_25404(class_11908 keyInput) {
        int keyCode = keyInput.comp_4795();
        if (this.isInSettingsPanel()) {
            if (keyCode == 256) {
                boolean wasBinding = Cyemer.getInstance().getModuleManager().getModules().stream().anyMatch(m -> m.isBinding());
                if (this.settingsPanelButton != null) {
                    for (SettingComponent comp : this.settingsPanelButton.getComponents()) {
                        comp.keyPressed(keyCode, keyInput.comp_4796(), keyInput.comp_4797());
                    }
                }

                if (!wasBinding) {
                    this.settingsPanelButton = null;
                    this.profileSettingsMode = false;
                    this.targetSettingsScroll = 0.0;
                    this.settingsScrollOffset = 0.0;
                }

                return true;
            } else {
                if (this.settingsPanelButton != null) {
                    for (SettingComponent comp : this.settingsPanelButton.getComponents()) {
                        comp.keyPressed(keyCode, keyInput.comp_4796(), keyInput.comp_4797());
                    }
                }

                return true;
            }
        } else if (!this.friendsTabSelected) {
            if (this.searchFocused && ClickGUIModule.useSearch()) {
                if (keyCode == 259 && !this.searchQuery.isEmpty()) {
                    this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
                    return true;
                }

                if (keyCode == 256) {
                    this.searchFocused = false;
                    return true;
                }
            }

            return super.method_25404(keyInput);
        } else if (FriendKeybindManager.getInstance().isBinding()) {
            FriendKeybindManager.getInstance().setKeyCode(keyCode == 256 ? -1 : keyCode);
            FriendKeybindManager.getInstance().setBinding(false);
            return true;
        } else if (keyCode == 256) {
            this.addFriendFocused = false;
            this.addFriendInput = "";
            this.friendSearchFocused = false;
            return super.method_25404(keyInput);
        } else {
            if (this.addFriendFocused) {
                if (keyCode == 259 && !this.addFriendInput.isEmpty()) {
                    this.addFriendInput = this.addFriendInput.substring(0, this.addFriendInput.length() - 1);
                    return true;
                }

                if (keyCode == 257 || keyCode == 335) {
                    this.submitAddFriend();
                    return true;
                }
            }

            if (this.friendSearchFocused && keyCode == 259 && !this.friendSearchQuery.isEmpty()) {
                this.friendSearchQuery = this.friendSearchQuery.substring(0, this.friendSearchQuery.length() - 1);
                return true;
            } else {
                return true;
            }
        }
    }

    public boolean method_25400(class_11905 input) {
        if (input.method_74227() && Character.isBmpCodePoint(input.comp_4793())) {
            char chr = (char)input.comp_4793();
            if (this.isInSettingsPanel() && this.settingsPanelButton != null) {
                for (SettingComponent comp : this.settingsPanelButton.getComponents()) {
                    comp.charTyped(chr, 0);
                }

                return true;
            }

            if (this.friendsTabSelected) {
                if (this.addFriendFocused && chr >= ' ' && chr <= '~') {
                    this.addFriendInput = this.addFriendInput + chr;
                    return true;
                }

                if (this.friendSearchFocused && chr >= ' ' && chr <= '~') {
                    this.friendSearchQuery = this.friendSearchQuery + chr;
                    return true;
                }

                return true;
            }

            if (this.searchFocused && ClickGUIModule.useSearch()) {
                if (chr >= ' ' && chr <= '~') {
                    this.searchQuery = this.searchQuery + chr;
                }

                return true;
            }
        }

        return super.method_25400(input);
    }

    public void method_25419() {
        if (this.field_22787 != null && this.originalBackgroundBlur != -1.0) {
            this.field_22787.field_1690.method_57702().method_41748((int)this.originalBackgroundBlur);
        }

        super.method_25419();
    }

    public boolean method_25421() {
        return false;
    }

    public List<SettingComponent> getSettingComponentsForButton(ModuleButton button) {
        return button.getComponents();
    }

    double[] layoutSettingsComponents(ModuleButton btn, double startY, double compW, double compX) {
        List<SettingComponent> comps = btn.getComponents();
        double[] yPos = new double[comps.size()];
        double curY = startY;

        for (int i = 0; i < comps.size(); i++) {
            SettingComponent comp = comps.get(i);
            if (!(comp instanceof BooleanComponent)) {
                curY += ClickGUILayout.SETTINGS_LABEL_HEIGHT + 5.0;
            }

            yPos[i] = curY;
            curY += comp.getComponentHeight() + ClickGUILayout.SETTINGS_ITEM_SPACING;
        }

        return yPos;
    }

    private void submitAddFriend() {
        if (!this.addFriendInput.isEmpty()) {
            String username = this.addFriendInput;
            this.addFriendInput = "";
            UUIDFetcher.fetchUUID(username).thenAccept(uuid -> {
                Module notifications = Cyemer.getInstance().getModuleManager().getModule("Notifications");
                if (uuid != null) {
                    FriendManager.getInstance().addFriend(username, uuid);
                    if (notifications != null) {
                        ModuleAccess.invoke(notifications, "success", new Class[]{String.class, String.class}, "Friends", "Added " + username + "!");
                    }
                } else if (notifications != null) {
                    ModuleAccess.invoke(notifications, "error", new Class[]{String.class, String.class}, "Friends", "Player '" + username + "' not found!");
                }
            });
        }
    }

    private void handleFriendsTabClick(Vector2d mouse, double contentX, double boxY, int button) {
        double padX = 14.0;
        double innerW = 558.0 - padX * 2.0;
        double curY = boxY + 10.0;
        double searchW = innerW - 120.0 - 8.0;
        double searchX = contentX + padX;
        if (mouse.x >= searchX && mouse.x <= searchX + searchW && mouse.y >= curY && mouse.y <= curY + 28.0) {
            this.friendSearchFocused = true;
            this.addFriendFocused = false;
        } else {
            double keybindX = searchX + searchW + 8.0;
            if (mouse.x >= keybindX && mouse.x <= keybindX + 120.0 && mouse.y >= curY && mouse.y <= curY + 28.0) {
                if (button == 0) {
                    FriendKeybindManager.getInstance().setBinding(true);
                    FriendKeybindManager.getInstance().setBindStartTime(System.currentTimeMillis());
                } else if (button == 1) {
                    FriendKeybindManager.getInstance().setKeyCode(-1);
                    FriendKeybindManager.getInstance().setBinding(false);
                }

                this.friendSearchFocused = false;
                this.addFriendFocused = false;
            } else {
                curY += 36.0;
                double addX = contentX + padX;
                if (mouse.x >= addX && mouse.x <= addX + innerW && mouse.y >= curY && mouse.y <= curY + 28.0) {
                    this.addFriendFocused = true;
                    this.friendSearchFocused = false;
                } else {
                    this.friendSearchFocused = false;
                    this.addFriendFocused = false;
                    curY += 46.0;
                    List<Friend> friends = FriendManager.getInstance().getFriends();
                    friends.sort(Comparator.comparing(Friend::getName, String.CASE_INSENSITIVE_ORDER));
                    double entryY = curY - this.friendsScrollOffset;

                    for (Friend friend : friends) {
                        if (this.friendSearchQuery.isEmpty() || friend.getName().toLowerCase().contains(this.friendSearchQuery.toLowerCase())) {
                            double entryX = contentX + padX;
                            if (mouse.x >= entryX && mouse.x <= entryX + innerW && mouse.y >= entryY && mouse.y <= entryY + 32.0) {
                                float trashSize = 18.0F;
                                float trashX = (float)(entryX + innerW - trashSize - 8.0);
                                float trashY = (float)(entryY + (32.0 - trashSize) / 2.0);
                                if (mouse.x >= trashX && mouse.x <= trashX + trashSize && mouse.y >= trashY && mouse.y <= trashY + trashSize) {
                                    FriendManager.getInstance().removeFriend(friend.getUuid());
                                }

                                return;
                            }

                            entryY += 38.0;
                        }
                    }
                }
            }
        }
    }

    private boolean handleSidebarClick(Vector2d mouse, double boxX, double boxY, int button) {
        double iconY = boxY + 14.0;

        for (int i = 0; i < this.categoryTabs.size(); i++) {
            double iconX = boxX + 13.0;
            if (mouse.x >= iconX && mouse.x <= iconX + 36.0 && mouse.y >= iconY && mouse.y <= iconY + 36.0 && button == 0) {
                this.selectedTabIndex = i;
                this.friendsTabSelected = false;
                this.searchQuery = "";
                this.targetModuleScroll = 0.0;
                this.moduleScrollOffset = 0.0;
                this.settingsPanelButton = null;
                this.profileSettingsMode = false;
                this.targetSettingsScroll = 0.0;
                this.settingsScrollOffset = 0.0;
                return true;
            }

            iconY += 44.0;
        }

        double friendsIconX = boxX + 13.0;
        if (mouse.x >= friendsIconX && mouse.x <= friendsIconX + 36.0 && mouse.y >= iconY && mouse.y <= iconY + 36.0 && button == 0) {
            this.friendsTabSelected = true;
            this.friendSearchQuery = "";
            this.addFriendInput = "";
            this.friendSearchFocused = false;
            this.addFriendFocused = false;
            this.targetFriendsScroll = 0.0;
            this.friendsScrollOffset = 0.0;
            this.settingsPanelButton = null;
            this.profileSettingsMode = false;
            this.targetSettingsScroll = 0.0;
            this.settingsScrollOffset = 0.0;
            return true;
        } else {
            double profileY = boxY + 380.0 - 60.0;
            if (!(mouse.x >= boxX) || !(mouse.x <= boxX + 62.0) || !(mouse.y >= profileY) || !(mouse.y <= boxY + 380.0)) {
                return false;
            } else if (button == 1) {
                Module clickGuiMod = Cyemer.getInstance()
                    .getModuleManager()
                    .getModules()
                    .stream()
                    .filter(m -> m instanceof ClickGUIModule)
                    .findFirst()
                    .orElse(null);
                if (clickGuiMod != null) {
                    this.settingsPanelButton = new ModuleButton(clickGuiMod);
                    this.profileSettingsMode = true;
                    this.targetSettingsScroll = 0.0;
                    this.settingsScrollOffset = 0.0;
                }

                return true;
            } else {
                return button == 0;
            }
        }
    }

    private boolean handleSettingsPanelClick(Vector2d mouse, double contentX, double boxY, int button) {
        float bs = 22.0F;
        float bx = (float)contentX + 10.0F;
        float by = (float)boxY + (29.0F - bs) / 2.0F;
        if (mouse.x >= bx && mouse.x <= bx + bs && mouse.y >= by && mouse.y <= by + bs && button == 0) {
            this.settingsPanelButton = null;
            this.profileSettingsMode = false;
            this.targetSettingsScroll = 0.0;
            this.settingsScrollOffset = 0.0;
            return true;
        } else {
            if (this.settingsPanelButton != null) {
                double areaY = boxY + 29.0;
                if (mouse.x >= contentX && mouse.x <= contentX + 558.0 && mouse.y >= areaY && mouse.y <= boxY + 380.0) {
                    if (this.profileSettingsMode) {
                        for (SettingComponent comp : this.settingsPanelButton.getComponents()) {
                            if (mouse.x >= comp.x && mouse.x <= comp.x + comp.width && mouse.y >= comp.y && mouse.y <= comp.y + comp.height) {
                                comp.mouseClicked(mouse.x, mouse.y, button);
                                return true;
                            }
                        }

                        return true;
                    }

                    double compW = 544.0;
                    double compX = contentX + 7.0;
                    double baseY = areaY + 7.0 - this.settingsScrollOffset;
                    double[] yPos = this.layoutSettingsComponents(this.settingsPanelButton, baseY, compW, compX);
                    List<SettingComponent> comps = this.settingsPanelButton.getComponents();

                    for (int i = 0; i < comps.size(); i++) {
                        SettingComponent compx = comps.get(i);
                        double cy = yPos[i];
                        if (mouse.y >= cy && mouse.y <= cy + compx.getComponentHeight()) {
                            compx.mouseClicked(mouse.x, mouse.y, button);
                            return true;
                        }
                    }

                    return true;
                }
            }

            return false;
        }
    }

    private boolean handleSearchClick(Vector2d mouse, double contentX, double searchY) {
        double searchX = contentX + 14.0;
        double searchW = 530.0;
        if (mouse.x >= searchX && mouse.x <= searchX + searchW && mouse.y >= searchY && mouse.y <= searchY + 26.0) {
            this.searchFocused = true;
            return true;
        } else {
            return false;
        }
    }

    private void handleModuleGridClick(Vector2d mouse, double contentX, double boxY, int button) {
        List<ModuleButton> visible = ClickGUILayout.visibleButtons(this.selectedTabIndex, this.categoryTabs, this.categoryButtons, this.searchQuery);
        double cellW = 171.33333333333334;
        double startX = contentX + 14.0;
        double searchH = ClickGUIModule.useSearch() ? 36.0 : 0.0;
        double startY = boxY + 8.0 + searchH + 10.0 - this.moduleScrollOffset;

        for (int i = 0; i < visible.size(); i++) {
            ModuleButton btn = visible.get(i);
            double cx = startX + i % 3 * (cellW + 8.0);
            double cy = startY + i / 3 * 42.0;
            if (mouse.x >= cx && mouse.x <= cx + cellW && mouse.y >= cy && mouse.y <= cy + 34.0) {
                if (button == 0) {
                    btn.module.forceToggle();
                } else if (button == 1 && !btn.getComponents().isEmpty()) {
                    this.settingsPanelButton = btn;
                    this.targetSettingsScroll = 0.0;
                    this.settingsScrollOffset = 0.0;
                }

                return;
            }
        }
    }

    private double computeSettingsContentHeight(ModuleButton btn) {
        double h = 0.0;

        for (SettingComponent comp : btn.getComponents()) {
            if (!(comp instanceof BooleanComponent)) {
                h += ClickGUILayout.SETTINGS_LABEL_HEIGHT + 5.0;
            }

            h += comp.getComponentHeight() + ClickGUILayout.SETTINGS_ITEM_SPACING;
        }

        return h;
    }

    private double computeProfileContentHeight(ModuleButton btn) {
        List<SettingComponent> comps = btn.getComponents();
        SettingComponent displayNameComp = null;
        SettingComponent profileIconComp = null;

        for (SettingComponent comp : comps) {
            if (comp.setting == ClickGUIModule.displayName) {
                displayNameComp = comp;
            } else if (comp.setting == ClickGUIModule.profileIcon) {
                profileIconComp = comp;
            }
        }

        double h = 94.0;
        if (displayNameComp != null) {
            h += displayNameComp.getComponentHeight() + 6.0;
        }

        h += 10.0;
        if (profileIconComp != null) {
            h += profileIconComp.getComponentHeight() + 6.0;
        }

        if (ClickGUIModule.useSeparator()) {
            h += 8.0;
        }

        h += 12.0;

        for (SettingComponent compx : comps) {
            if (compx.setting != ClickGUIModule.displayName && compx.setting != ClickGUIModule.profileIcon) {
                if (!(compx instanceof BooleanComponent)) {
                    h += ClickGUILayout.SETTINGS_LABEL_HEIGHT + 5.0;
                }

                h += compx.getComponentHeight() + ClickGUILayout.SETTINGS_ITEM_SPACING;
            }
        }

        return h;
    }

    private boolean isInSettingsPanel() {
        return this.settingsPanelButton != null && this.settingsSlideProgress > 0.5;
    }

    private boolean isControlDown() {
        if (this.field_22787 == null) {
            return false;
        } else {
            long h = this.field_22787.method_22683().method_4490();
            return GLFW.glfwGetKey(h, 341) == 1 || GLFW.glfwGetKey(h, 345) == 1;
        }
    }
}
