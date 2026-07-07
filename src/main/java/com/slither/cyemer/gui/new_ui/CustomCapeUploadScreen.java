package com.slither.cyemer.gui.new_ui;

import com.slither.cyemer.module.implementation.ClickGUIModule;
import com.slither.cyemer.module.implementation.CustomCapeModule;
import com.slither.cyemer.util.CapeTextureManager;
import com.slither.cyemer.util.GuiShaderStyle;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10799;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_768;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

@Environment(EnvType.CLIENT)
public class CustomCapeUploadScreen extends class_437 {
    private static final int ENTRY_HEIGHT = 22;
    private static final int ENTRY_SPACING = 3;
    private final class_437 parent;
    private final CustomCapeModule module;
    private class_768 panelBounds;
    private class_768 previewBounds;
    private class_768 libraryBounds;
    private class_768 libraryListBounds;
    private class_768 uploadButtonBounds;
    private class_768 useButtonBounds;
    private class_768 deleteButtonBounds;
    private class_768 backButtonBounds;
    private String statusMessage = "Upload a PNG to auto-convert it into a cape.";
    private int statusColor = -5786156;
    private final List<CapeTextureManager.CustomCapeEntryView> savedCapes = new ArrayList<>();
    private String selectedEntryId;
    private double listScroll;
    private double listMaxScroll;
    private long lastEntryClick;

    public CustomCapeUploadScreen(class_437 parent, CustomCapeModule module) {
        super(class_2561.method_43470("Custom Cape Upload"));
        this.parent = parent;
        this.module = module;
    }

    protected void method_25426() {
        int panelWidth = Math.min(500, this.field_22789 - 36);
        int panelHeight = Math.min(350, this.field_22790 - 24);
        int panelX = (this.field_22789 - panelWidth) / 2;
        int panelY = (this.field_22790 - panelHeight) / 2;
        this.panelBounds = new class_768(panelX, panelY, panelWidth, panelHeight);
        int padding = 14;
        int contentTop = panelY + 42;
        int leftWidth = Math.min(200, panelWidth / 2 - 24);
        int leftX = panelX + padding;
        int previewHeight = 120;
        this.previewBounds = new class_768(leftX, contentTop, leftWidth, previewHeight);
        int rightX = this.previewBounds.method_3321() + this.previewBounds.method_3319() + 12;
        int rightWidth = panelX + panelWidth - padding - rightX;
        int listContainerHeight = 208;
        this.libraryBounds = new class_768(rightX, contentTop, rightWidth, listContainerHeight);
        this.libraryListBounds = new class_768(
            this.libraryBounds.method_3321() + 4,
            this.libraryBounds.method_3322() + 24,
            this.libraryBounds.method_3319() - 8,
            this.libraryBounds.method_3320() - 28
        );
        int buttonY = this.previewBounds.method_3322() + this.previewBounds.method_3320() + 10;
        this.uploadButtonBounds = new class_768(leftX, buttonY, leftWidth, 24);
        int actionY = this.libraryBounds.method_3322() + this.libraryBounds.method_3320() + 10;
        int actionWidth = (rightWidth - 8) / 2;
        this.useButtonBounds = new class_768(rightX, actionY, actionWidth, 24);
        this.deleteButtonBounds = new class_768(rightX + actionWidth + 8, actionY, actionWidth, 24);
        this.backButtonBounds = new class_768(panelX + padding, panelY + panelHeight - 40, panelWidth - padding * 2, 24);
        this.refreshSavedCapes();
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        if (Renderer.get().beginFrame(this.field_22789, this.field_22790, this.field_22787.method_22683().method_4495())) {
            try {
                Renderer.get().drawRect(context, 0.0F, 0.0F, this.field_22789, this.field_22790, new Color(0, 0, 0, 134));
                this.drawPanel(context);
                this.drawTitleText(context);
                this.drawPreviewCardText(context);
                this.drawSavedList(context, mouseX, mouseY);
                this.drawButtons(context, mouseX, mouseY);
                String statusLine = this.trimToWidth(this.statusMessage, this.panelBounds.method_3319() - 28, 8.8F);
                Renderer.get()
                    .drawText(
                        context,
                        statusLine,
                        this.panelBounds.method_3321() + 14,
                        this.panelBounds.method_3322() + 31,
                        8.8F,
                        new Color(this.statusColor, true),
                        false
                    );
            } finally {
                Renderer.get().endFrame();
            }

            this.renderPreviewTexture(context);
        }
    }

    private void drawPanel(class_332 context) {
        float panelRadius = this.uiRadius(Math.max(7.0F, ClickGUIModule.getGuiCornerRadiusScaled(0.62)));
        Color panelBg = ClickGUIModule.getColor(ClickGUIModule.getPanelBackground(), 0.95);
        Color panelGlass = new Color(255, 255, 255, 10);
        Renderer.get()
            .drawRoundedRectStyled(
                context,
                this.panelBounds.method_3321(),
                this.panelBounds.method_3322(),
                this.panelBounds.method_3319(),
                this.panelBounds.method_3320(),
                panelRadius,
                panelBg,
                GuiShaderStyle.PANEL
            );
        Renderer.get()
            .drawRoundedRectStyled(
                context,
                this.panelBounds.method_3321(),
                this.panelBounds.method_3322(),
                this.panelBounds.method_3319(),
                this.panelBounds.method_3320(),
                panelRadius,
                panelGlass,
                GuiShaderStyle.PANEL_GLASS
            );
        float headerHeight = 32.0F;
        Renderer.get().scissor(context, this.panelBounds.method_3321(), this.panelBounds.method_3322(), this.panelBounds.method_3319(), headerHeight);
        Renderer.get()
            .drawRoundedRectStyled(
                context,
                this.panelBounds.method_3321(),
                this.panelBounds.method_3322(),
                this.panelBounds.method_3319(),
                headerHeight + panelRadius,
                panelRadius,
                ClickGUIModule.getColor(ClickGUIModule.getHeaderBackground(), 0.98),
                GuiShaderStyle.HEADER
            );
        Renderer.get()
            .drawRoundedRectStyled(
                context,
                this.panelBounds.method_3321(),
                this.panelBounds.method_3322(),
                this.panelBounds.method_3319(),
                headerHeight + panelRadius,
                panelRadius,
                new Color(255, 255, 255, 12),
                GuiShaderStyle.HEADER_GLASS
            );
        Renderer.get().resetScissor();
        Color cardBg = ClickGUIModule.getColor(ClickGUIModule.getSettingsBackground(), 0.86);
        float cardRadius = this.uiRadius(8.0F);
        Renderer.get()
            .drawRoundedRectStyled(
                context,
                this.previewBounds.method_3321(),
                this.previewBounds.method_3322(),
                this.previewBounds.method_3319(),
                this.previewBounds.method_3320(),
                cardRadius,
                cardBg,
                GuiShaderStyle.CONTROL
            );
        Renderer.get()
            .drawRoundedRectOutline(
                context,
                this.previewBounds.method_3321(),
                this.previewBounds.method_3322(),
                this.previewBounds.method_3319(),
                this.previewBounds.method_3320(),
                cardRadius,
                1.0F,
                ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), 0.45)
            );
        Renderer.get()
            .drawRoundedRectStyled(
                context,
                this.libraryBounds.method_3321(),
                this.libraryBounds.method_3322(),
                this.libraryBounds.method_3319(),
                this.libraryBounds.method_3320(),
                cardRadius,
                cardBg,
                GuiShaderStyle.CONTROL
            );
        Renderer.get()
            .drawRoundedRectOutline(
                context,
                this.libraryBounds.method_3321(),
                this.libraryBounds.method_3322(),
                this.libraryBounds.method_3319(),
                this.libraryBounds.method_3320(),
                cardRadius,
                1.0F,
                ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBorder(), 0.45)
            );
    }

    private void drawTitleText(class_332 context) {
        Renderer.get()
            .drawText(
                context,
                "Custom Cape Library",
                this.panelBounds.method_3321() + 14,
                this.panelBounds.method_3322() + 11,
                13.0F,
                ClickGUIModule.getModuleEnabledText(),
                false
            );
    }

    private void drawPreviewCardText(class_332 context) {
        Renderer.get()
            .drawText(
                context,
                "Preview",
                this.previewBounds.method_3321() + 6,
                this.previewBounds.method_3322() - 11,
                9.0F,
                ClickGUIModule.getModuleDisabledText(),
                false
            );
    }

    private void drawSavedList(class_332 context, int mouseX, int mouseY) {
        Renderer.get()
            .drawText(
                context,
                "Saved capes",
                this.libraryBounds.method_3321() + 7,
                this.libraryBounds.method_3322() + 7,
                9.0F,
                ClickGUIModule.getModuleDisabledText(),
                false
            );
        Renderer.get()
            .scissor(
                context,
                this.libraryListBounds.method_3321(),
                this.libraryListBounds.method_3322(),
                this.libraryListBounds.method_3319(),
                this.libraryListBounds.method_3320()
            );
        int rowY = this.libraryListBounds.method_3322() + 2 - (int)Math.round(this.listScroll);

        for (CapeTextureManager.CustomCapeEntryView entry : this.savedCapes) {
            class_768 rowBounds = new class_768(this.libraryListBounds.method_3321() + 1, rowY, this.libraryListBounds.method_3319() - 2, 22);
            if (rowBounds.method_3322() + rowBounds.method_3320() >= this.libraryListBounds.method_3322()
                && rowBounds.method_3322() <= this.libraryListBounds.method_3322() + this.libraryListBounds.method_3320()) {
                boolean selected = Objects.equals(entry.id(), this.selectedEntryId);
                boolean hovered = rowBounds.method_3318(mouseX, mouseY);
                if (selected) {
                    Color start = ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientStart(), hovered ? 0.95 : 0.86);
                    Color end = ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientEnd(), hovered ? 0.95 : 0.86);
                    float rowRadius = this.uiRadius(5.5F);
                    Renderer.get()
                        .drawRoundedRectGradientStyled(
                            context,
                            rowBounds.method_3321(),
                            rowBounds.method_3322(),
                            rowBounds.method_3319(),
                            rowBounds.method_3320(),
                            rowRadius,
                            start,
                            end,
                            false,
                            GuiShaderStyle.ROW_ACTIVE
                        );
                } else if (hovered) {
                    float rowRadius = this.uiRadius(5.5F);
                    Renderer.get()
                        .drawRoundedRectStyled(
                            context,
                            rowBounds.method_3321(),
                            rowBounds.method_3322(),
                            rowBounds.method_3319(),
                            rowBounds.method_3320(),
                            rowRadius,
                            ClickGUIModule.getColor(ClickGUIModule.getSearchBoxFocusedBackground(), 0.72),
                            GuiShaderStyle.ROW
                        );
                }

                String displayName = this.trimToWidth(entry.name(), rowBounds.method_3319() - 66, 9.2F);
                Renderer.get()
                    .drawText(
                        context,
                        displayName,
                        rowBounds.method_3321() + 6,
                        rowBounds.method_3322() + 6,
                        9.2F,
                        selected ? ClickGUIModule.getModuleEnabledText() : ClickGUIModule.getSettingsText(),
                        false
                    );
                if (Objects.equals(entry.id(), this.module.getActiveCustomCapeId())) {
                    String activeTag = "active";
                    float activeWidth = Renderer.get().getTextWidth(activeTag, 8.0F);
                    Renderer.get()
                        .drawText(
                            context,
                            activeTag,
                            rowBounds.method_3321() + rowBounds.method_3319() - activeWidth - 6.0F,
                            rowBounds.method_3322() + 6,
                            8.0F,
                            ClickGUIModule.getModuleEnabledText(),
                            false
                        );
                }
            }

            rowY += 25;
        }

        Renderer.get().resetScissor();
        if (this.savedCapes.isEmpty()) {
            Renderer.get()
                .drawText(
                    context,
                    "No custom capes yet",
                    this.libraryListBounds.method_3321() + 8,
                    this.libraryListBounds.method_3322() + 6,
                    8.6F,
                    ClickGUIModule.getSearchBoxPlaceholder(),
                    false
                );
        }
    }

    private void drawButtons(class_332 context, int mouseX, int mouseY) {
        this.drawButton(context, this.uploadButtonBounds, "Upload PNG", mouseX, mouseY, true, true);
        boolean hasSelection = this.getSelectedEntry() != null;
        this.drawButton(context, this.useButtonBounds, "Use Selected", mouseX, mouseY, true, hasSelection);
        this.drawButton(context, this.deleteButtonBounds, "Delete", mouseX, mouseY, false, hasSelection);
        this.drawButton(context, this.backButtonBounds, "Back", mouseX, mouseY, false, true);
    }

    private void drawButton(class_332 context, class_768 bounds, String label, int mouseX, int mouseY, boolean primary, boolean enabled) {
        boolean hovered = enabled && bounds.method_3318(mouseX, mouseY);
        Color start;
        Color end;
        if (!enabled) {
            start = ClickGUIModule.getColor(ClickGUIModule.getSettingsBackground(), 0.45);
            end = ClickGUIModule.getColor(ClickGUIModule.getPanelBackground(), 0.45);
        } else if (primary) {
            start = ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientStart(), hovered ? 1.0 : 0.9);
            end = ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientEnd(), hovered ? 1.0 : 0.9);
        } else {
            start = ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBackground(), hovered ? 0.95 : 0.84);
            end = ClickGUIModule.getColor(ClickGUIModule.getPanelBackground(), hovered ? 0.95 : 0.84);
        }

        Renderer.get()
            .drawRoundedRectGradientStyled(
                context,
                bounds.method_3321(),
                bounds.method_3322(),
                bounds.method_3319(),
                bounds.method_3320(),
                this.uiRadius(6.0F),
                start,
                end,
                false,
                GuiShaderStyle.ROW_ACTIVE
            );
        float font = 9.5F;
        float textW = Renderer.get().getTextWidth(label, font);
        float textH = Renderer.get().getTextHeight(font);
        float tx = bounds.method_3321() + (bounds.method_3319() - textW) * 0.5F;
        float ty = bounds.method_3322() + (bounds.method_3320() - textH) * 0.5F;
        Color textColor = enabled ? ClickGUIModule.getModuleEnabledText() : ClickGUIModule.getModuleDisabledText();
        Renderer.get().drawText(context, label, tx, ty, font, textColor, false);
    }

    private void renderPreviewTexture(class_332 context) {
        class_2960 texture = this.module.getPreviewTextureIdentifier();
        if (texture != null) {
            int texWidth = this.module.isCustomMode() && CapeTextureManager.hasCustomCapeTexture() ? Math.max(1, CapeTextureManager.getCustomCapeWidth()) : 64;
            int texHeight = this.module.isCustomMode() && CapeTextureManager.hasCustomCapeTexture()
                ? Math.max(1, CapeTextureManager.getCustomCapeHeight())
                : 32;
            float padding = 8.0F;
            float areaX = this.previewBounds.method_3321() + padding;
            float areaY = this.previewBounds.method_3322() + padding;
            float areaW = this.previewBounds.method_3319() - padding * 2.0F;
            float areaH = this.previewBounds.method_3320() - padding * 2.0F;
            float capeAspect = (float)texWidth / texHeight;
            float areaAspect = areaW / areaH;
            float drawW = areaW;
            float drawH = areaH;
            if (capeAspect > areaAspect) {
                drawH = areaW / capeAspect;
            } else {
                drawW = areaH * capeAspect;
            }

            int drawX = Math.round(areaX + (areaW - drawW) * 0.5F);
            int drawY = Math.round(areaY + (areaH - drawH) * 0.5F);
            int drawWidth = Math.max(1, Math.round(drawW));
            int drawHeight = Math.max(1, Math.round(drawH));
            context.method_44379(
                this.previewBounds.method_3321(),
                this.previewBounds.method_3322(),
                this.previewBounds.method_3321() + this.previewBounds.method_3319(),
                this.previewBounds.method_3322() + this.previewBounds.method_3320()
            );
            context.method_25302(class_10799.field_56883, texture, drawX, drawY, 0.0F, 0.0F, drawWidth, drawHeight, texWidth, texHeight, texWidth, texHeight);
            context.method_44380();
        }
    }

    public boolean method_25402(class_11909 click, boolean doubleClick) {
        if (click.method_74245() != 0) {
            return super.method_25402(click, doubleClick);
        } else {
            int mouseX = (int)click.comp_4798();
            int mouseY = (int)click.comp_4799();
            if (this.uploadButtonBounds.method_3318(mouseX, mouseY)) {
                this.onUploadClicked();
                return true;
            } else if (this.useButtonBounds.method_3318(mouseX, mouseY)) {
                this.onUseSelected();
                return true;
            } else if (this.deleteButtonBounds.method_3318(mouseX, mouseY)) {
                this.onDeleteSelected();
                return true;
            } else if (this.backButtonBounds.method_3318(mouseX, mouseY)) {
                this.method_25419();
                return true;
            } else {
                if (this.libraryListBounds.method_3318(mouseX, mouseY)) {
                    String hit = this.findEntryAt(mouseX, mouseY);
                    if (hit != null) {
                        this.selectedEntryId = hit;
                        long now = System.currentTimeMillis();
                        if (doubleClick || now - this.lastEntryClick <= 260L) {
                            this.onUseSelected();
                        }

                        this.lastEntryClick = now;
                        return true;
                    }
                }

                return super.method_25402(click, doubleClick);
            }
        }
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.libraryListBounds != null && this.libraryListBounds.method_3318((int)mouseX, (int)mouseY)) {
            this.listScroll -= verticalAmount * 18.0;
            if (this.listScroll < 0.0) {
                this.listScroll = 0.0;
            }

            if (this.listScroll > this.listMaxScroll) {
                this.listScroll = this.listMaxScroll;
            }

            return true;
        } else {
            return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
    }

    public boolean method_25404(class_11908 input) {
        if (input.comp_4795() == 256) {
            this.method_25419();
            return true;
        } else {
            return super.method_25404(input);
        }
    }

    private void onUploadClicked() {
        String selected = this.openCapeFileDialog();
        if (selected != null && !selected.isBlank()) {
            Path sourcePath;
            try {
                sourcePath = Paths.get(selected).toAbsolutePath().normalize();
            } catch (Exception var4) {
                this.setStatus("Invalid file path.", -39322);
                return;
            }

            CapeTextureManager.ImportResult result = this.module.importCustomCape(sourcePath, CapeTextureManager.FrameMode.FIT, 0.5F, 0.5F);
            if (!result.success()) {
                this.setStatus(result.message(), -39322);
            } else {
                if (result.entry() != null) {
                    this.selectedEntryId = result.entry().id();
                }

                this.refreshSavedCapes();
                this.setStatus("Imported and converted: " + sourcePath.getFileName(), -9967198);
            }
        } else {
            this.setStatus("Upload cancelled.", -5855578);
        }
    }

    private void onUseSelected() {
        CapeTextureManager.CustomCapeEntryView selected = this.getSelectedEntry();
        if (selected == null) {
            this.setStatus("Select a saved cape first.", -11654);
        } else {
            if (this.module.selectCustomCape(selected.id())) {
                this.refreshSavedCapes();
                this.setStatus("Active cape set to: " + selected.name(), -9967198);
            } else {
                this.setStatus("Failed to activate selected cape.", -39322);
            }
        }
    }

    private void onDeleteSelected() {
        CapeTextureManager.CustomCapeEntryView selected = this.getSelectedEntry();
        if (selected == null) {
            this.setStatus("Select a saved cape first.", -11654);
        } else {
            if (this.module.deleteCustomCape(selected.id())) {
                this.setStatus("Deleted: " + selected.name(), -6834701);
                this.refreshSavedCapes();
            } else {
                this.setStatus("Could not delete selected cape.", -39322);
            }
        }
    }

    private String findEntryAt(int mouseX, int mouseY) {
        int rowY = this.libraryListBounds.method_3322() + 2 - (int)Math.round(this.listScroll);

        for (CapeTextureManager.CustomCapeEntryView entry : this.savedCapes) {
            class_768 rowBounds = new class_768(this.libraryListBounds.method_3321() + 1, rowY, this.libraryListBounds.method_3319() - 2, 22);
            if (rowBounds.method_3318(mouseX, mouseY)) {
                return entry.id();
            }

            rowY += 25;
        }

        return null;
    }

    private CapeTextureManager.CustomCapeEntryView getSelectedEntry() {
        for (CapeTextureManager.CustomCapeEntryView entry : this.savedCapes) {
            if (Objects.equals(entry.id(), this.selectedEntryId)) {
                return entry;
            }
        }

        return null;
    }

    private void refreshSavedCapes() {
        this.savedCapes.clear();
        this.savedCapes.addAll(this.module.getCustomCapeLibrary());
        String activeId = this.module.getActiveCustomCapeId();
        if (this.selectedEntryId == null || this.savedCapes.stream().noneMatch(entryx -> Objects.equals(entryx.id(), this.selectedEntryId))) {
            this.selectedEntryId = activeId;
            if (this.selectedEntryId == null && !this.savedCapes.isEmpty()) {
                this.selectedEntryId = this.savedCapes.get(0).id();
            }
        }

        if (activeId != null) {
            for (CapeTextureManager.CustomCapeEntryView entry : this.savedCapes) {
                if (Objects.equals(entry.id(), activeId)) {
                    break;
                }
            }
        }

        int totalHeight = this.savedCapes.size() * 25;
        this.listMaxScroll = Math.max(0.0, totalHeight - this.libraryListBounds.method_3320() + 4.0);
        if (this.listScroll > this.listMaxScroll) {
            this.listScroll = this.listMaxScroll;
        }
    }

    private String openCapeFileDialog() {
        String defaultPath = this.module.getCustomCapePath();
        if (defaultPath == null || defaultPath.isBlank()) {
            defaultPath = System.getProperty("user.home", ".");
        }

        try {
            MemoryStack stack = MemoryStack.stackPush();

            String var4;
            try {
                PointerBuffer filters = stack.mallocPointer(1);
                filters.put(stack.UTF8("*.png"));
                filters.flip();
                var4 = TinyFileDialogs.tinyfd_openFileDialog("Select custom cape PNG", defaultPath, filters, "PNG image (*.png)", false);
            } catch (Throwable var6) {
                if (stack != null) {
                    try {
                        stack.close();
                    } catch (Throwable var5) {
                        var6.addSuppressed(var5);
                    }
                }

                throw var6;
            }

            if (stack != null) {
                stack.close();
            }

            return var4;
        } catch (Exception var7) {
            return null;
        }
    }

    private void setStatus(String message, int argbColor) {
        this.statusMessage = message == null ? "" : message;
        this.statusColor = argbColor;
    }

    private String trimToWidth(String text, float maxWidth, float fontSize) {
        if (text != null && !text.isEmpty()) {
            if (Renderer.get().getTextWidth(text, fontSize) <= maxWidth) {
                return text;
            } else {
                String suffix = "...";
                String current = text;

                while (!current.isEmpty() && Renderer.get().getTextWidth(current + suffix, fontSize) > maxWidth) {
                    current = current.substring(0, current.length() - 1);
                }

                return current + suffix;
            }
        } else {
            return "";
        }
    }

    private float uiRadius(float normalRadius) {
        return ClickGUIModule.useSquareGui() ? 0.0F : normalRadius;
    }

    public void method_25419() {
        if (this.field_22787 != null) {
            this.field_22787.method_1507(this.parent);
        }
    }

    public boolean method_25421() {
        return false;
    }
}
