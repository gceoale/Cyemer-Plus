package com.slither.cyemer.gui.new_ui;

import com.slither.cyemer.friend.Friend;
import com.slither.cyemer.friend.FriendKeybindManager;
import com.slither.cyemer.friend.FriendManager;
import com.slither.cyemer.gui.new_ui.old.ModuleButton;
import com.slither.cyemer.gui.new_ui.settings.BooleanComponent;
import com.slither.cyemer.gui.new_ui.settings.SettingComponent;
import com.slither.cyemer.gui.new_ui.settings.SliderComponent;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.module.implementation.ClickGUIModule;
import com.slither.cyemer.util.GuiShaderStyle;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;
import org.joml.Vector2d;

@Environment(EnvType.CLIENT)
public class ClickGUIRenderer {
    private final ClickGUI gui;
    private final Map<Integer, Double> hoverAnimations = new HashMap<>();

    public ClickGUIRenderer(ClickGUI gui) {
        this.gui = gui;
    }

    public void renderContainerBackground(class_332 ctx, double x, double y, double w, double h, double alpha) {
        float r = ClickGUILayout.getContainerRadius();
        if (ClickGUIModule.useBlurBackground()) {
            float blur = ClickGUIModule.getBlurStrength();
            if (blur > 0.0F) {
                Renderer.get().drawBlur(ctx, (float)x, (float)y, (float)w, (float)h, blur);
            }
        }

        Renderer.get()
            .drawRoundedRectStyled(
                ctx, (float)x, (float)y, (float)w, (float)h, r, ClickGUIModule.getColor(ClickGUIModule.getPanelBackground(), alpha), GuiShaderStyle.PANEL
            );
        Renderer.get()
            .drawRoundedRectStyled(ctx, (float)x, (float)y, (float)w, (float)h, r, new Color(255, 255, 255, (int)(6.0 * alpha)), GuiShaderStyle.PANEL_GLASS);
        Renderer.get()
            .drawRoundedRectOutline(
                ctx, (float)x, (float)y, (float)w, (float)h, r, 1.0F, ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), alpha * 0.1)
            );
    }

    public void renderSidebar(class_332 ctx, double bx, double by, double sw, double sh, double alpha, Vector2d mouse, double dt) {
        float cr = ClickGUILayout.getContainerRadius();
        float ir = ClickGUILayout.getSidebarIconRadius();
        Renderer.get()
            .drawRoundedRectStyled(
                ctx,
                (float)bx,
                (float)by,
                (float)sw,
                (float)sh,
                cr,
                ClickGUIModule.getColor(ClickGUIModule.getPanelBackground(), alpha * 0.4),
                GuiShaderStyle.PANEL
            );
        if (ClickGUIModule.useSeparator()) {
            Renderer.get()
                .drawRect(
                    ctx,
                    (float)(bx + sw - 1.0),
                    (float)by + 10.0F,
                    1.0F,
                    (float)sh - 20.0F,
                    ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), alpha * 0.12)
                );
        }

        double iconY = by + 14.0;

        for (int i = 0; i < this.gui.categoryTabs.size(); i++) {
            CategoryTab tab = this.gui.categoryTabs.get(i);
            double ix = Math.round(bx + (sw - 36.0) / 2.0);
            double iy = Math.round(iconY);
            boolean selected = i == this.gui.selectedTabIndex && !this.gui.friendsTabSelected && this.gui.settingsSlideProgress < 0.5;
            boolean hov = inBounds(mouse, ix, iy, 36.0, 36.0);
            Color bg = selected
                ? ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientStart(), alpha * 0.6)
                : (
                    hov
                        ? ClickGUIModule.getColor(ClickGUIModule.getModuleButtonHoverBackground(), alpha * 0.55)
                        : ClickGUIModule.getColor(ClickGUIModule.getModuleButtonBackground(), alpha * 0.3)
                );
            Renderer.get().drawRoundedRectStyled(ctx, (float)ix, (float)iy, 36.0F, 36.0F, ir, bg, selected ? GuiShaderStyle.ROW_ENABLED : GuiShaderStyle.ROW);
            if (selected) {
                this.drawSelectionGlow(ctx, ix, iy, ir, alpha);
            }

            this.drawIconOrLabel(ctx, tab.getResolvedIconId(), tab.category.name, ix, iy, alpha);
            iconY += 44.0;
        }

        this.renderFriendsIcon(ctx, bx, iconY, sw, alpha, mouse, ir);
        this.renderProfileCard(ctx, bx, by + sh - 60.0, sw, 60.0, alpha);
    }

    private void renderFriendsIcon(class_332 ctx, double bx, double iconY, double sw, double alpha, Vector2d mouse, float ir) {
        double ix = Math.round(bx + (sw - 36.0) / 2.0);
        double iy = Math.round(iconY);
        boolean selected = this.gui.friendsTabSelected && this.gui.settingsSlideProgress < 0.5;
        boolean hov = inBounds(mouse, ix, iy, 36.0, 36.0);
        Color bg = selected
            ? ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientStart(), alpha * 0.6)
            : (
                hov
                    ? ClickGUIModule.getColor(ClickGUIModule.getModuleButtonHoverBackground(), alpha * 0.55)
                    : ClickGUIModule.getColor(ClickGUIModule.getModuleButtonBackground(), alpha * 0.3)
            );
        Renderer.get().drawRoundedRectStyled(ctx, (float)ix, (float)iy, 36.0F, 36.0F, ir, bg, selected ? GuiShaderStyle.ROW_ENABLED : GuiShaderStyle.ROW);
        if (selected) {
            this.drawSelectionGlow(ctx, ix, iy, ir, alpha);
        }

        this.drawIconOrLabel(ctx, ClickGUIIconCache.getIconId("friend.png"), "FRI", ix, iy, alpha);
    }

    private void drawSelectionGlow(class_332 ctx, double ix, double iy, float ir, double alpha) {
        Renderer.get()
            .drawRoundedRectOutline(
                ctx,
                (float)ix - 1.5F,
                (float)iy - 1.5F,
                39.0F,
                39.0F,
                ir + 1.5F,
                1.2F,
                ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientStart(), alpha * 0.18)
            );
    }

    private void drawIconOrLabel(class_332 ctx, int iconId, String name, double ix, double iy, double alpha) {
        if (iconId != -1) {
            float s = (float)Math.round(17.28);
            float dx = (float)Math.round(ix + (36.0 - s) / 2.0);
            float dy = (float)Math.round(iy + (36.0 - s) / 2.0);
            Renderer.get().drawTexture(ctx, iconId, dx, dy, s, s, 0.0F, 0.0F, 64.0F, 64.0F, 64.0F, 64.0F);
        } else {
            String label = name.substring(0, Math.min(3, name.length()));
            float tw = Renderer.get().getTextWidth(label, 9.0F);
            float th = Renderer.get().getTextHeight(9.0F);
            Renderer.get()
                .drawText(
                    ctx,
                    label,
                    (float)Math.round(ix + (36.0 - tw) / 2.0),
                    (float)Math.round(iy + (36.0 - th) / 2.0),
                    9.0F,
                    new Color(255, 255, 255, (int)(200.0 * alpha)),
                    false
                );
        }
    }

    private void renderProfileCard(class_332 ctx, double x, double y, double w, double h, double alpha) {
        if (ClickGUIModule.useSeparator()) {
            Renderer.get()
                .drawRect(ctx, (float)x + 8.0F, (float)y, (float)w - 16.0F, 1.0F, ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), alpha * 0.1));
        }

        double av = 28.0;
        double ax = x + (w - av) / 2.0;
        double ay = y + 8.0;
        Renderer.get()
            .drawRoundedRectStyled(
                ctx,
                (float)ax,
                (float)ay,
                (float)av,
                (float)av,
                (float)av / 2.0F,
                ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientStart(), alpha * 0.45),
                GuiShaderStyle.ICON
            );
        String letter = profileLetter();
        float lw = Renderer.get().getTextWidth(letter, 12.0F);
        float lh = Renderer.get().getTextHeight(12.0F);
        Renderer.get()
            .drawText(ctx, letter, (float)(ax + (av - lw) / 2.0), (float)(ay + (av - lh) / 2.0), 12.0F, new Color(255, 255, 255, (int)(230.0 * alpha)), false);
        String name = displayName();
        float nw = Renderer.get().getTextWidth(name, 8.0F);
        Renderer.get()
            .drawText(
                ctx,
                name,
                (float)(x + (w - nw) / 2.0),
                (float)(ay + av + 4.0),
                8.0F,
                ClickGUIModule.getColor(ClickGUIModule.getModuleDisabledText(), alpha * 0.85),
                false
            );
    }

    public void renderSearchBox(class_332 ctx, double cx, double sy, double cw, double alpha, Vector2d mouse) {
        float r = ClickGUILayout.getSearchRadius();
        double sx = cx + 14.0;
        double sw = cw - 28.0;
        boolean active = this.gui.searchFocused || inBounds(mouse, sx, sy, sw, 26.0);
        Renderer.get()
            .drawRoundedRectStyled(
                ctx,
                (float)sx,
                (float)sy,
                (float)sw,
                26.0F,
                r,
                active
                    ? ClickGUIModule.getColor(ClickGUIModule.getSearchBoxFocusedBackground(), alpha)
                    : ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBackground(), alpha),
                GuiShaderStyle.SEARCH
            );
        if (active) {
            Renderer.get()
                .drawRoundedRectOutline(
                    ctx, (float)sx, (float)sy, (float)sw, 26.0F, r, 1.0F, ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), alpha * 0.4)
                );
        }

        boolean empty = this.gui.searchQuery.isEmpty();
        float th = Renderer.get().getTextHeight(9.5F);
        Renderer.get()
            .drawText(
                ctx,
                empty ? "Search modules..." : this.gui.searchQuery,
                (float)(sx + 10.0),
                (float)(sy + (26.0 - th) / 2.0),
                9.5F,
                empty
                    ? ClickGUIModule.getColor(ClickGUIModule.getSearchBoxPlaceholder(), alpha)
                    : ClickGUIModule.getColor(ClickGUIModule.getSearchBoxText(), alpha),
                false
            );
    }

    public String renderModuleGrid(class_332 ctx, double cx, double gy, double cw, double alpha, Vector2d mouse, float delta, double dt) {
        if (alpha < 0.01) {
            return null;
        } else {
            List<ModuleButton> buttons = ClickGUILayout.visibleButtons(
                this.gui.selectedTabIndex, this.gui.categoryTabs, this.gui.categoryButtons, this.gui.searchQuery
            );
            double cellW = (cw - 28.0 - 16.0) / 3.0;
            double startX = cx + 14.0;
            double startY = gy + 10.0 - this.gui.moduleScrollOffset;
            String tooltip = null;

            for (int i = 0; i < buttons.size(); i++) {
                ModuleButton btn = buttons.get(i);
                double bx = startX + i % 3 * (cellW + 8.0);
                double by = startY + i / 3 * 42.0;
                btn.x = bx;
                btn.y = by;
                btn.width = cellW;
                btn.height = 34.0;
                btn.isLastButton = false;
                String t = this.renderGridCell(ctx, btn, bx, by, cellW, alpha, mouse, dt, i);
                if (t != null) {
                    tooltip = t;
                }
            }

            return tooltip;
        }
    }

    private String renderGridCell(class_332 ctx, ModuleButton btn, double cx, double cy, double cw, double alpha, Vector2d mouse, double dt, int idx) {
        float r = ClickGUILayout.getCellRadius();
        boolean hov = inBounds(mouse, cx, cy, cw, 34.0);
        boolean hoverEnabled = ClickGUIModule.useHover();
        double target = hoverEnabled && hov ? 1.0 : 0.0;
        double hv = this.hoverAnimations.getOrDefault(idx, 0.0);
        hv += (target - hv) * (1.0 - Math.exp(-(hoverEnabled ? 22.0 : 30.0) * dt));
        if (Math.abs(hv - target) < 0.001) {
            hv = target;
        }

        if (!hoverEnabled && hv <= 0.001) {
            this.hoverAnimations.remove(idx);
        } else {
            this.hoverAnimations.put(idx, hv);
        }

        if (btn.module.isEnabled()) {
            Color s = ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientStart(), alpha);
            Color e = ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientEnd(), alpha);
            if (hv > 0.01) {
                s = ModuleButton.brighten(s, hv * 0.12);
                e = ModuleButton.brighten(e, hv * 0.12);
            }

            if (ClickGUIModule.useCustomHighlight() && !ClickGUIModule.useHighlightGradient()) {
                Renderer.get().drawRoundedRectStyled(ctx, (float)cx, (float)cy, (float)cw, 34.0F, r, s, GuiShaderStyle.ROW_ENABLED);
            } else {
                Renderer.get().drawRoundedRectGradientStyled(ctx, (float)cx, (float)cy, (float)cw, 34.0F, r, s, e, false, GuiShaderStyle.ROW_ENABLED);
            }

            Renderer.get()
                .drawRoundedRectStyled(
                    ctx, (float)cx, (float)cy, (float)cw, 34.0F, r, new Color(255, 255, 255, clampAlpha(alpha * 8.0)), GuiShaderStyle.ROW_ACTIVE
                );
        } else {
            Color bg = ClickGUIModule.getColor(ClickGUIModule.getModuleButtonBackground(), alpha * 0.55);
            if (hv > 0.01) {
                bg = ModuleButton.brighten(bg, hv * 0.1);
            }

            Renderer.get().drawRoundedRectStyled(ctx, (float)cx, (float)cy, (float)cw, 34.0F, r, bg, GuiShaderStyle.ROW);
            Renderer.get()
                .drawRoundedRectStyled(
                    ctx, (float)cx, (float)cy, (float)cw, 34.0F, r, new Color(255, 255, 255, clampAlpha(alpha * 4.0)), GuiShaderStyle.ROW_ACTIVE
                );
        }

        if (hv > 0.01) {
            Renderer.get()
                .drawRoundedRectOutline(
                    ctx, (float)cx, (float)cy, (float)cw, 34.0F, r, 1.0F, ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), alpha * 0.2 * hv)
                );
        }

        float th = Renderer.get().getTextHeight(9.5F);
        Renderer.get()
            .drawText(
                ctx,
                btn.module.getName(),
                (float)cx + 10.0F,
                (float)(cy + (34.0 - th) / 2.0),
                9.5F,
                btn.module.isEnabled()
                    ? ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledText(), alpha)
                    : ClickGUIModule.getColor(ClickGUIModule.getModuleDisabledText(), alpha),
                false
            );
        if (btn.module.getKeyCode() != -1) {
            String key = btn.module.getKeyDisplayName();
            float kw = Renderer.get().getTextWidth(key, 7.5F);
            float kh = Renderer.get().getTextHeight(7.5F);
            float kx = (float)(cx + cw - kw - 8.0);
            float ky = (float)(cy + (34.0 - kh) / 2.0);
            Renderer.get()
                .drawRoundedRectStyled(
                    ctx,
                    kx - 3.0F,
                    ky - 3.0F,
                    kw + 6.0F,
                    kh + 6.0F,
                    4.0F,
                    ClickGUIModule.getColor(ClickGUIModule.getModuleButtonBackground(), alpha * 0.5),
                    GuiShaderStyle.CONTROL
                );
            Renderer.get()
                .drawText(
                    ctx,
                    key,
                    kx,
                    ky,
                    7.5F,
                    btn.module.isEnabled()
                        ? ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledText(), alpha * 0.55)
                        : ClickGUIModule.getColor(ClickGUIModule.getModuleDisabledText(), alpha * 0.4),
                    false
                );
        }

        return hov ? btn.module.getDescription() : null;
    }

    public void renderFriendsTab(class_332 ctx, double cx, double cy, double cw, double alpha, Vector2d mouse) {
        float sr = ClickGUILayout.getSearchRadius();
        float cr = ClickGUILayout.getCellRadius();
        double padX = 14.0;
        double innerW = cw - padX * 2.0;
        double curY = cy + 10.0;
        double searchW = innerW - 120.0 - 8.0;
        double searchX = cx + padX;
        this.renderFriendSearchBox(ctx, searchX, curY, searchW, alpha, mouse, sr);
        this.renderFriendKeybindButton(ctx, searchX + searchW + 8.0, curY, 120.0, alpha, mouse, sr);
        curY += 36.0;
        this.renderAddFriendInput(ctx, cx + padX, curY, innerW, alpha, mouse, sr);
        curY += 38.0;
        if (ClickGUIModule.useSeparator()) {
            Renderer.get()
                .drawRect(ctx, (float)(cx + padX), (float)curY, (float)innerW, 1.0F, ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), alpha * 0.12));
        }

        curY += 8.0;
        double listH = 380.0 - (curY - cy) - 6.0;
        Renderer.get().scissor(ctx, (float)cx, (float)curY, (float)cw, (float)listH);
        this.renderFriendsList(ctx, cx, curY, cw, padX, innerW, alpha, mouse, cr);
        Renderer.get().resetScissor();
    }

    private void renderFriendSearchBox(class_332 ctx, double sx, double sy, double sw, double alpha, Vector2d mouse, float r) {
        boolean active = this.gui.friendSearchFocused || inBounds(mouse, sx, sy, sw, 28.0);
        Renderer.get()
            .drawRoundedRectStyled(
                ctx,
                (float)sx,
                (float)sy,
                (float)sw,
                28.0F,
                r,
                active
                    ? ClickGUIModule.getColor(ClickGUIModule.getSearchBoxFocusedBackground(), alpha)
                    : ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBackground(), alpha),
                GuiShaderStyle.SEARCH
            );
        if (active) {
            Renderer.get()
                .drawRoundedRectOutline(
                    ctx, (float)sx, (float)sy, (float)sw, 28.0F, r, 1.0F, ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), alpha * 0.4)
                );
        }

        boolean empty = this.gui.friendSearchQuery.isEmpty();
        float th = Renderer.get().getTextHeight(9.5F);
        Renderer.get()
            .drawText(
                ctx,
                empty ? "Search friends..." : this.gui.friendSearchQuery,
                (float)(sx + 10.0),
                (float)(sy + (28.0 - th) / 2.0),
                9.5F,
                empty
                    ? ClickGUIModule.getColor(ClickGUIModule.getSearchBoxPlaceholder(), alpha)
                    : ClickGUIModule.getColor(ClickGUIModule.getSearchBoxText(), alpha),
                false
            );
    }

    private void renderFriendKeybindButton(class_332 ctx, double kx, double ky, double kw, double alpha, Vector2d mouse, float r) {
        boolean hov = inBounds(mouse, kx, ky, kw, 28.0);
        Renderer.get()
            .drawRoundedRectStyled(
                ctx,
                (float)kx,
                (float)ky,
                (float)kw,
                28.0F,
                r,
                hov
                    ? ClickGUIModule.getColor(ClickGUIModule.getSearchBoxFocusedBackground(), alpha)
                    : ClickGUIModule.getColor(ClickGUIModule.getModuleButtonBackground(), alpha * 0.55),
                GuiShaderStyle.CONTROL
            );
        if (hov) {
            Renderer.get()
                .drawRoundedRectOutline(
                    ctx, (float)kx, (float)ky, (float)kw, 28.0F, r, 1.0F, ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), alpha * 0.35)
                );
        }

        boolean binding = FriendKeybindManager.getInstance().isBinding();
        String label = binding ? "Press a key..." : "Key: " + FriendKeybindManager.getInstance().getKeyDisplayName();
        float th = Renderer.get().getTextHeight(8.5F);
        Renderer.get()
            .drawText(
                ctx,
                label,
                (float)(kx + 8.0),
                (float)(ky + (28.0 - th) / 2.0),
                8.5F,
                binding
                    ? ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledText(), alpha)
                    : ClickGUIModule.getColor(ClickGUIModule.getModuleDisabledText(), alpha * 0.85),
                false
            );
    }

    private void renderAddFriendInput(class_332 ctx, double ax, double ay, double aw, double alpha, Vector2d mouse, float r) {
        boolean hov = inBounds(mouse, ax, ay, aw, 28.0);
        Renderer.get()
            .drawRoundedRectStyled(
                ctx,
                (float)ax,
                (float)ay,
                (float)aw,
                28.0F,
                r,
                this.gui.addFriendFocused
                    ? ClickGUIModule.getColor(ClickGUIModule.getSearchBoxFocusedBackground(), alpha)
                    : ClickGUIModule.getColor(ClickGUIModule.getModuleButtonBackground(), alpha * 0.45),
                GuiShaderStyle.CONTROL
            );
        if (this.gui.addFriendFocused || hov) {
            Renderer.get()
                .drawRoundedRectOutline(
                    ctx,
                    (float)ax,
                    (float)ay,
                    (float)aw,
                    28.0F,
                    r,
                    1.0F,
                    ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), alpha * (this.gui.addFriendFocused ? 0.5 : 0.2))
                );
        }

        boolean empty = this.gui.addFriendInput.isEmpty();
        float th = Renderer.get().getTextHeight(9.5F);
        Renderer.get()
            .drawText(
                ctx,
                empty ? "+ Add friend..." : this.gui.addFriendInput,
                (float)(ax + 10.0),
                (float)(ay + (28.0 - th) / 2.0),
                9.5F,
                empty
                    ? ClickGUIModule.getColor(ClickGUIModule.getSearchBoxPlaceholder(), alpha * 0.7)
                    : ClickGUIModule.getColor(ClickGUIModule.getSearchBoxText(), alpha),
                false
            );
    }

    private void renderFriendsList(class_332 ctx, double cx, double listY, double cw, double padX, double innerW, double alpha, Vector2d mouse, float r) {
        List<Friend> friends = FriendManager.getInstance().getFriends();
        friends.sort(Comparator.comparing(Friend::getName, String.CASE_INSENSITIVE_ORDER));
        String q = this.gui.friendSearchQuery.toLowerCase();
        long visible = friends.stream().filter(f -> q.isEmpty() || f.getName().toLowerCase().contains(q)).count();
        double ey = listY - this.gui.friendsScrollOffset;

        for (Friend f : friends) {
            if (q.isEmpty() || f.getName().toLowerCase().contains(q)) {
                double ex = cx + padX;
                boolean hov = inBounds(mouse, ex, ey, innerW, 32.0);
                Renderer.get()
                    .drawRoundedRectStyled(
                        ctx,
                        (float)ex,
                        (float)ey,
                        (float)innerW,
                        32.0F,
                        r,
                        hov
                            ? ClickGUIModule.getColor(ClickGUIModule.getModuleButtonHoverBackground(), alpha * 0.45)
                            : ClickGUIModule.getColor(ClickGUIModule.getModuleButtonBackground(), alpha * 0.3),
                        GuiShaderStyle.ROW
                    );
                float nth = Renderer.get().getTextHeight(10.0F);
                Renderer.get()
                    .drawText(
                        ctx,
                        f.getDisplayName(),
                        (float)(ex + 12.0),
                        (float)(ey + (32.0 - nth) / 2.0),
                        10.0F,
                        ClickGUIModule.getColor(ClickGUIModule.getModuleDisabledText(), alpha),
                        false
                    );
                float ts = 18.0F;
                float tx = (float)(ex + innerW - ts - 8.0);
                float ty = (float)(ey + (32.0 - ts) / 2.0);
                Renderer.get()
                    .drawRoundedRectStyled(
                        ctx,
                        tx,
                        ty,
                        ts,
                        ts,
                        4.0F,
                        inBounds(mouse, tx, ty, ts, ts) ? new Color(220, 60, 60, (int)(180.0 * alpha)) : new Color(160, 40, 40, (int)(100.0 * alpha)),
                        GuiShaderStyle.CONTROL
                    );
                float xw = Renderer.get().getTextWidth("x", 9.0F);
                float xh = Renderer.get().getTextHeight(9.0F);
                Renderer.get().drawText(ctx, "x", tx + (ts - xw) / 2.0F, ty + (ts - xh) / 2.0F, 9.0F, new Color(255, 255, 255, (int)(220.0 * alpha)), false);
                ey += 38.0;
            }
        }

        if (visible == 0L) {
            String msg = this.gui.friendSearchQuery.isEmpty() ? "No friends added yet." : "No results.";
            float mw = Renderer.get().getTextWidth(msg, 9.5F);
            Renderer.get()
                .drawText(
                    ctx,
                    msg,
                    (float)(cx + (cw - mw) / 2.0),
                    (float)(listY + 20.0),
                    9.5F,
                    ClickGUIModule.getColor(ClickGUIModule.getModuleDisabledText(), alpha * 0.5),
                    false
                );
        }
    }

    private void renderSettingsHeader(class_332 ctx, float px, float py, float pw, float hh, double alpha) {
        float r = Math.min(Math.max(4.0F, Math.min(ClickGUILayout.getContainerRadius(), hh)), hh * 0.72F);
        Renderer.get()
            .drawRoundedRectStyled(ctx, px, py, pw, hh, r, ClickGUIModule.getColor(ClickGUIModule.getSettingsBackground(), alpha * 0.35), GuiShaderStyle.HEADER);
        Renderer.get().drawRoundedRectStyled(ctx, px, py, pw, hh, r, new Color(255, 255, 255, clampAlpha(alpha * 6.0)), GuiShaderStyle.HEADER_GLASS);
    }

    public void renderSettingsPanel(class_332 ctx, double px, double py, double pw, double ph, double alpha, Vector2d mouse, double dt) {
        if (this.gui.settingsPanelButton != null && !(alpha < 0.01)) {
            ModuleButton btn = this.gui.settingsPanelButton;
            float hh = 29.0F;
            this.renderSettingsHeader(ctx, (float)px, (float)py, (float)pw, hh, alpha);
            this.renderBackButton(ctx, px, py, hh, alpha, mouse, btn);
            if (ClickGUIModule.useSeparator()) {
                Renderer.get()
                    .drawRect(
                        ctx,
                        (float)px + 10.0F,
                        (float)py + hh - 1.0F,
                        (float)pw - 20.0F,
                        1.0F,
                        ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), alpha * 0.12)
                    );
            }

            double areaY = py + 29.0;
            double areaH = ph - 29.0;
            Renderer.get().save();
            Renderer.get().scissor(ctx, (float)px, (float)areaY, (float)pw, (float)areaH);
            double compW = pw - 14.0 - 2.0;
            double compX = px + 7.0 + 1.0;
            double baseY = areaY + 7.0 - this.gui.settingsScrollOffset;
            double[] yPos = this.gui.layoutSettingsComponents(btn, baseY, compW, compX);
            List<SettingComponent> comps = btn.getComponents();

            for (int i = 0; i < comps.size(); i++) {
                SettingComponent comp = comps.get(i);
                double cy = yPos[i];
                if (!(comp instanceof BooleanComponent)) {
                    double labelY = cy - ClickGUILayout.SETTINGS_LABEL_HEIGHT - 5.0;
                    Renderer.get()
                        .drawText(
                            ctx,
                            comp.setting.getName(),
                            (float)compX,
                            (float)labelY,
                            8.5F,
                            ClickGUIModule.getColor(ClickGUIModule.getSettingsText(), alpha * 0.65),
                            false
                        );
                    if (comp instanceof SliderComponent) {
                        String val = ((SliderSetting)comp.setting).getValueAsString();
                        float vw = Renderer.get().getTextWidth(val, 8.5F);
                        Renderer.get()
                            .drawText(
                                ctx,
                                val,
                                (float)(compX + compW - vw),
                                (float)labelY,
                                8.5F,
                                ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledText(), alpha * 0.85),
                                false
                            );
                    }
                }

                comp.x = compX;
                comp.y = cy;
                comp.width = compW;
                comp.height = comp.getComponentHeight();
                Renderer.get().scissor(ctx, (float)px, (float)areaY, (float)pw, (float)areaH);
                comp.render(ctx, (int)mouse.x, (int)mouse.y, (float)dt, alpha);
            }

            Renderer.get().resetScissor();
            Renderer.get().restore();
        }
    }

    private void renderBackButton(class_332 ctx, double px, double py, float hh, double alpha, Vector2d mouse, ModuleButton btn) {
        float bs = 22.0F;
        float bx = (float)px + 10.0F;
        float by = (float)py + (hh - bs) / 2.0F;
        boolean hov = inBounds(mouse, bx, by, bs, bs);
        Renderer.get()
            .drawRoundedRectStyled(
                ctx,
                bx,
                by,
                bs,
                bs,
                ClickGUILayout.getSidebarIconRadius(),
                ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientStart(), alpha * (hov ? 0.55 : 0.35)),
                GuiShaderStyle.CONTROL
            );
        if (btn != null) {
            CategoryTab tab = null;

            for (CategoryTab t : this.gui.categoryTabs) {
                if (t.category == btn.module.getCategory()) {
                    tab = t;
                    break;
                }
            }

            int iconId = tab != null ? tab.getResolvedIconId() : -1;
            if (iconId != -1) {
                float s = Math.round(bs * 0.55F);
                Renderer.get()
                    .drawTexture(
                        ctx,
                        iconId,
                        (float)Math.round(bx + (bs - s) / 2.0),
                        (float)Math.round(by + (bs - s) / 2.0),
                        s,
                        s,
                        0.0F,
                        0.0F,
                        64.0F,
                        64.0F,
                        64.0F,
                        64.0F
                    );
            } else {
                String label = btn.module.getCategory().name.substring(0, Math.min(3, btn.module.getCategory().name.length()));
                float lw = Renderer.get().getTextWidth(label, 8.5F);
                float lh = Renderer.get().getTextHeight(8.5F);
                Renderer.get().drawText(ctx, label, bx + (bs - lw) / 2.0F, by + (bs - lh) / 2.0F, 8.5F, new Color(255, 255, 255, (int)(220.0 * alpha)), false);
            }

            Color titleColor = ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledText(), alpha);
            float tth = Renderer.get().getTextHeight(11.5F);
            Renderer.get().drawText(ctx, btn.module.getName(), bx + bs + 10.0F, (float)py + (hh - tth) / 2.0F, 11.5F, titleColor, false);
            String desc = btn.module.getDescription();
            if (desc != null && !desc.isEmpty()) {
                float tw = Renderer.get().getTextWidth(btn.module.getName(), 11.5F);
                float dth = Renderer.get().getTextHeight(8.5F);
                Renderer.get()
                    .drawText(
                        ctx,
                        desc,
                        bx + bs + 10.0F + tw + 10.0F,
                        (float)py + (hh - dth) / 2.0F,
                        8.5F,
                        ClickGUIModule.getColor(ClickGUIModule.getModuleDisabledText(), alpha * 0.65),
                        false
                    );
            }
        }
    }

    public void renderProfilePanel(class_332 ctx, double px, double py, double pw, double ph, double alpha, Vector2d mouse, double dt) {
        if (this.gui.settingsPanelButton != null && !(alpha < 0.01)) {
            float hh = 29.0F;
            this.renderSettingsHeader(ctx, (float)px, (float)py, (float)pw, hh, alpha);
            float bs = 22.0F;
            float bx = (float)px + 10.0F;
            float by = (float)py + (hh - bs) / 2.0F;
            boolean backHov = inBounds(mouse, bx, by, bs, bs);
            Renderer.get()
                .drawRoundedRectStyled(
                    ctx,
                    bx,
                    by,
                    bs,
                    bs,
                    ClickGUILayout.getSidebarIconRadius(),
                    ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientStart(), alpha * (backHov ? 0.55 : 0.35)),
                    GuiShaderStyle.CONTROL
                );
            float lw = Renderer.get().getTextWidth("<", 11.0F);
            float lh = Renderer.get().getTextHeight(11.0F);
            Renderer.get().drawText(ctx, "<", bx + (bs - lw) / 2.0F, by + (bs - lh) / 2.0F, 11.0F, new Color(255, 255, 255, (int)(220.0 * alpha)), false);
            Color titleColor = ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledText(), alpha);
            Renderer.get()
                .drawText(ctx, "Profile Settings", bx + bs + 10.0F, (float)py + (hh - Renderer.get().getTextHeight(11.5F)) / 2.0F, 11.5F, titleColor, false);
            if (ClickGUIModule.useSeparator()) {
                Renderer.get()
                    .drawRect(
                        ctx,
                        (float)px + 10.0F,
                        (float)py + hh - 1.0F,
                        (float)pw - 20.0F,
                        1.0F,
                        ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), alpha * 0.12)
                    );
            }

            double areaY = py + 29.0;
            double areaH = ph - 29.0;
            double contentW = pw - 14.0 - 2.0;
            double contentX = px + 7.0 + 1.0;
            double curY = areaY + 7.0 - this.gui.settingsScrollOffset;
            Renderer.get().save();
            Renderer.get().scissor(ctx, (float)px, (float)areaY, (float)pw, (float)areaH);
            double av = 56.0;
            double ax = contentX + (contentW - av) / 2.0;
            Renderer.get()
                .drawRoundedRectStyled(
                    ctx,
                    (float)ax,
                    (float)curY,
                    (float)av,
                    (float)av,
                    (float)av / 2.0F,
                    ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientStart(), alpha * 0.5),
                    GuiShaderStyle.ICON
                );
            String letter = profileLetter();
            float aw = Renderer.get().getTextWidth(letter, 20.0F);
            float ah = Renderer.get().getTextHeight(20.0F);
            Renderer.get()
                .drawText(
                    ctx, letter, (float)(ax + (av - aw) / 2.0), (float)(curY + (av - ah) / 2.0), 20.0F, new Color(255, 255, 255, (int)(240.0 * alpha)), false
                );
            curY += av + 10.0 + 18.0;
            List<SettingComponent> comps = this.gui.settingsPanelButton.getComponents();
            SettingComponent nameComp = null;
            SettingComponent iconComp = null;

            for (SettingComponent c : comps) {
                if (c.setting == ClickGUIModule.displayName) {
                    nameComp = c;
                } else if (c.setting == ClickGUIModule.profileIcon) {
                    iconComp = c;
                }
            }

            Color labelColor = ClickGUIModule.getColor(ClickGUIModule.getSettingsText(), alpha * 0.65);
            curY = this.renderProfileField(ctx, px, areaY, pw, areaH, contentX, contentW, curY, "Display Name", nameComp, alpha, mouse, dt, labelColor);
            curY = this.renderProfileField(ctx, px, areaY, pw, areaH, contentX, contentW, curY, "Profile Icon", iconComp, alpha, mouse, dt, labelColor);
            if (ClickGUIModule.useSeparator()) {
                Renderer.get()
                    .drawRect(
                        ctx, (float)contentX, (float)curY, (float)contentW, 1.0F, ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), alpha * 0.1)
                    );
                curY += 8.0;
            }

            Renderer.get().drawText(ctx, "GUI Settings", (float)contentX, (float)curY, 10.0F, titleColor, false);
            curY += 12.0;

            for (SettingComponent comp : comps) {
                if (comp.setting != ClickGUIModule.displayName && comp.setting != ClickGUIModule.profileIcon) {
                    if (!(comp instanceof BooleanComponent)) {
                        Renderer.get().drawText(ctx, comp.setting.getName(), (float)contentX, (float)curY, 8.5F, labelColor, false);
                        curY += ClickGUILayout.SETTINGS_LABEL_HEIGHT + 5.0;
                    }

                    comp.x = contentX;
                    comp.y = curY;
                    comp.width = contentW;
                    comp.height = comp.getComponentHeight();
                    Renderer.get().scissor(ctx, (float)px, (float)areaY, (float)pw, (float)areaH);
                    comp.render(ctx, (int)mouse.x, (int)mouse.y, (float)dt, alpha);
                    curY += comp.getComponentHeight() + ClickGUILayout.SETTINGS_ITEM_SPACING;
                }
            }

            Renderer.get().resetScissor();
            Renderer.get().restore();
        }
    }

    private double renderProfileField(
        class_332 ctx,
        double px,
        double areaY,
        double pw,
        double areaH,
        double contentX,
        double contentW,
        double curY,
        String label,
        SettingComponent comp,
        double alpha,
        Vector2d mouse,
        double dt,
        Color labelColor
    ) {
        Renderer.get().drawText(ctx, label, (float)contentX, (float)curY, 8.5F, labelColor, false);
        curY += 10.0;
        if (comp != null) {
            comp.x = contentX;
            comp.y = curY;
            comp.width = contentW;
            comp.height = comp.getComponentHeight();
            Renderer.get().scissor(ctx, (float)px, (float)areaY, (float)pw, (float)areaH);
            comp.render(ctx, (int)mouse.x, (int)mouse.y, (float)dt, alpha);
            curY += comp.getComponentHeight() + 6.0;
        }

        return curY;
    }

    public void renderTooltip(class_332 ctx, String text, int mx, int my, double zoom) {
        float z = (float)zoom;
        float fs = 9.5F * z;
        float pad = 5.0F * z;
        float tw = Renderer.get().getTextWidth(text, fs) + pad * 2.0F;
        float th = Renderer.get().getTextHeight(fs) + pad * 2.0F;
        float off = 12.0F * z;
        float x = mx + off;
        float y = my + off;
        if (x + tw > this.gui.field_22789) {
            x -= tw + off * 2.0F;
        }

        if (y + th > this.gui.field_22790) {
            y -= th + off * 2.0F;
        }

        float r = Math.max(5.0F * z, Math.min(10.0F * z, ClickGUILayout.getSearchRadius() * z));
        Renderer.get().drawRoundedRectStyled(ctx, x, y, tw, th, r, ClickGUIModule.getTooltipBackground(), GuiShaderStyle.TOOLTIP);
        Renderer.get().drawRoundedRectOutline(ctx, x, y, tw, th, r, z, ClickGUIModule.getTooltipBorder());
        Renderer.get().drawText(ctx, text, x + pad, y + (th - Renderer.get().getTextHeight(fs)) / 2.0F, fs, ClickGUIModule.getTooltipText(), false);
    }

    private static boolean inBounds(Vector2d m, double x, double y, double w, double h) {
        return m.x >= x && m.x <= x + w && m.y >= y && m.y <= y + h;
    }

    private static int clampAlpha(double v) {
        return (int)Math.max(0.0, Math.min(255.0, v));
    }

    private static String profileLetter() {
        String v = ClickGUIModule.profileIcon != null ? ClickGUIModule.profileIcon.getValue() : "C";
        return (v.isEmpty() ? "C" : v).substring(0, 1).toUpperCase();
    }

    private static String displayName() {
        String v = ClickGUIModule.displayName != null ? ClickGUIModule.displayName.getValue() : "User";
        return v.isEmpty() ? "User" : v;
    }
}
