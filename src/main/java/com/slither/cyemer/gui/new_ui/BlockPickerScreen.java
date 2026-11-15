package com.slither.cyemer.gui.new_ui;

import com.slither.cyemer.module.BlockListSetting;
import com.slither.cyemer.module.implementation.ClickGUIModule;
import com.slither.cyemer.util.ICyemerRenderer;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11905;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_1799;
import net.minecraft.class_2248;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_7923;

/**
 * Searchable list of every block in the game; click a row to add or remove
 * it from a BlockListSetting. Type to search by name or id, Backspace to
 * edit, Ctrl/Cmd+V to paste, Esc to go back to the ClickGUI.
 */
@Environment(EnvType.CLIENT)
public class BlockPickerScreen extends class_437 {
    private static final int ROW_H = 18;
    private static final float FONT = 8.5F;

    private final class_437 parent;
    private final BlockListSetting setting;
    private final List<Entry> all = new ArrayList<>();
    private List<Entry> shown = new ArrayList<>();
    private String query = "";
    private boolean selectedOnly = false;
    private double scroll = 0.0;
    private long openedAt = System.currentTimeMillis();

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int searchY;
    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int selectedBtnX;
    private int clearBtnX;
    private int btnY;
    private static final int BTN_W = 62;
    private static final int BTN_H = 14;

    private record Entry(class_2248 block, String name, String id, String haystack, class_1799 icon) {}

    public BlockPickerScreen(class_437 parent, BlockListSetting setting) {
        super(class_2561.method_43470("Select Blocks"));
        this.parent = parent;
        this.setting = setting;
        for (class_2248 block : class_7923.field_41175) {
            class_2960 id = class_7923.field_41175.method_10221(block);
            String path = id.method_12832();
            if (path.equals("air") || path.equals("cave_air") || path.equals("void_air")) {
                continue;
            }
            String name = block.method_9518().getString();
            class_1799 icon = new class_1799(block);
            this.all.add(new Entry(block, name, id.toString(),
                    (name + " " + id).toLowerCase(Locale.ROOT), icon.method_7960() ? null : icon));
        }
        this.all.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        this.refilter();
    }

    /**
     * Rebuilt only when the search or the filter changes, never on a toggle,
     * so a row does not jump out from under the cursor when clicked.
     * Picked blocks come first when there is no search.
     */
    private void refilter() {
        String q = this.query.toLowerCase(Locale.ROOT).trim();
        List<Entry> picked = new ArrayList<>();
        List<Entry> rest = new ArrayList<>();
        for (Entry e : this.all) {
            boolean on = this.setting.contains(e.block());
            if (this.selectedOnly && !on) {
                continue;
            }
            if (!q.isEmpty() && !e.haystack().contains(q)) {
                continue;
            }
            (on && q.isEmpty() ? picked : rest).add(e);
        }
        picked.addAll(rest);
        this.shown = picked;
        this.scroll = 0.0;
    }

    protected void method_25426() {
        this.panelW = Math.min(340, this.field_22789 - 40);
        this.panelH = Math.min(320, this.field_22790 - 40);
        this.panelX = (this.field_22789 - this.panelW) / 2;
        this.panelY = (this.field_22790 - this.panelH) / 2;
        this.btnY = this.panelY + 8;
        this.clearBtnX = this.panelX + this.panelW - 10 - BTN_W;
        this.selectedBtnX = this.clearBtnX - 6 - BTN_W;
        this.searchY = this.panelY + 30;
        this.listX = this.panelX + 10;
        this.listY = this.searchY + 22;
        this.listW = this.panelW - 20;
        this.listH = this.panelY + this.panelH - 10 - this.listY;
        this.clampScroll();
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        ICyemerRenderer r = Renderer.get();
        r.beginFrame(this.field_22789, this.field_22790, this.field_22787.method_22683().method_4495());
        boolean shadows = ClickGUIModule.useShadows();
        Color accent = ClickGUIModule.getModuleEnabledGradientStart();
        Color muted = new Color(150, 150, 150);

        r.drawRect(context, 0.0F, 0.0F, this.field_22789, this.field_22790, new Color(0, 0, 0, 110));
        r.drawRoundedRect(context, this.panelX, this.panelY, this.panelW, this.panelH, 8.0F, new Color(22, 22, 26, 245));
        r.drawRoundedRectOutline(context, this.panelX, this.panelY, this.panelW, this.panelH, 8.0F, 1.0F, new Color(65, 65, 75));

        r.drawText(context, "Blocks", this.panelX + 12, this.panelY + 8, 11.0F, Color.WHITE, shadows);
        int count = this.setting.getBlocks().size();
        r.drawText(context, count + " selected", this.panelX + 12, this.panelY + 20, 7.5F, count > 0 ? accent : muted, shadows);

        this.drawButton(r, context, this.selectedBtnX, this.selectedOnly ? "Selected" : "All", this.selectedOnly, mouseX, mouseY, accent);
        this.drawButton(r, context, this.clearBtnX, "Clear", false, mouseX, mouseY, accent);

        // Search box; always focused, so typing anywhere searches.
        r.drawRoundedRect(context, this.listX, this.searchY, this.listW, 16, 5.0F, new Color(38, 38, 46));
        String text = this.query.isEmpty() ? "Search blocks..." : this.query;
        if ((System.currentTimeMillis() - this.openedAt) % 1000L < 500L && !this.query.isEmpty()) {
            text = text + "|";
        }
        float textY = this.searchY + (16 - r.getTextHeight(FONT)) / 2.0F;
        r.drawText(context, text, this.listX + 6, textY, FONT, this.query.isEmpty() ? muted : Color.WHITE, false);
        String hits = this.shown.size() + "";
        r.drawText(context, hits, this.listX + this.listW - 6 - r.getTextWidth(hits, 7.5F), textY, 7.5F, muted, false);

        // Rows, clipped to the list.
        r.scissor(context, this.listX, this.listY, this.listW, this.listH);
        int first = Math.max(0, (int) (this.scroll / ROW_H));
        for (int i = first; i < this.shown.size(); i++) {
            int rowY = (int) (this.listY + i * ROW_H - this.scroll);
            if (rowY > this.listY + this.listH) {
                break;
            }
            Entry e = this.shown.get(i);
            boolean on = this.setting.contains(e.block());
            boolean hover = mouseX >= this.listX && mouseX < this.listX + this.listW - 6 && mouseY >= rowY && mouseY < rowY + ROW_H
                    && mouseY >= this.listY && mouseY < this.listY + this.listH;
            if (hover || on) {
                r.drawRoundedRect(context, this.listX, rowY, this.listW - 6, ROW_H - 1, 4.0F,
                        on ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), hover ? 70 : 45) : new Color(255, 255, 255, 18));
            }
            // Checkbox
            float cbX = this.listX + 5;
            float cbY = rowY + (ROW_H - 10) / 2.0F;
            if (on) {
                r.drawRoundedRect(context, cbX, cbY, 10, 10, 3.0F, accent);
                r.drawRoundedRect(context, cbX + 3, cbY + 3, 4, 4, 1.5F, Color.WHITE);
            } else {
                r.drawRoundedRectOutline(context, cbX, cbY, 10, 10, 3.0F, 1.0F, new Color(120, 120, 130));
            }
            if (e.icon() != null) {
                context.method_51427(e.icon(), this.listX + 20, rowY + 1);
            }
            float nameY = rowY + (ROW_H - r.getTextHeight(FONT)) / 2.0F;
            r.drawText(context, e.name(), this.listX + 40, nameY, FONT, Color.WHITE, false);
            float nameW = r.getTextWidth(e.name(), FONT);
            r.drawText(context, e.id(), this.listX + 46 + nameW, nameY + 0.5F, 7.0F, muted, false);
        }
        r.resetScissor();

        if (this.shown.isEmpty()) {
            r.drawCenteredText(context, this.selectedOnly ? "Nothing selected yet" : "No blocks match",
                    this.listX + this.listW / 2.0F, this.listY + 20, FONT, muted, false);
        }

        // Scrollbar
        double content = this.shown.size() * (double) ROW_H;
        if (content > this.listH) {
            float barH = Math.max(18.0F, (float) (this.listH * this.listH / content));
            float barY = this.listY + (float) ((this.listH - barH) * (this.scroll / (content - this.listH)));
            r.drawRoundedRect(context, this.listX + this.listW - 3, barY, 3, barH, 1.5F, new Color(255, 255, 255, 70));
        }
        r.endFrame();
    }

    private void drawButton(ICyemerRenderer r, class_332 context, int x, String label, boolean active, int mouseX, int mouseY, Color accent) {
        boolean hover = mouseX >= x && mouseX < x + BTN_W && mouseY >= this.btnY && mouseY < this.btnY + BTN_H;
        Color bg = active ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), hover ? 200 : 160)
                : new Color(hover ? 60 : 44, hover ? 60 : 44, hover ? 72 : 54);
        r.drawRoundedRect(context, x, this.btnY, BTN_W, BTN_H, 4.0F, bg);
        r.drawCenteredText(context, label, x + BTN_W / 2.0F, this.btnY + (BTN_H - r.getTextHeight(7.5F)) / 2.0F, 7.5F, Color.WHITE, false);
    }

    public boolean method_25402(class_11909 click, boolean doubleClick) {
        double mx = click.comp_4798();
        double my = click.comp_4799();
        if (click.method_74245() != 0) {
            return super.method_25402(click, doubleClick);
        }
        if (my >= this.btnY && my < this.btnY + BTN_H) {
            if (mx >= this.selectedBtnX && mx < this.selectedBtnX + BTN_W) {
                this.selectedOnly = !this.selectedOnly;
                this.refilter();
                return true;
            }
            if (mx >= this.clearBtnX && mx < this.clearBtnX + BTN_W) {
                this.setting.clear();
                this.refilter();
                return true;
            }
        }
        if (mx >= this.listX && mx < this.listX + this.listW - 6 && my >= this.listY && my < this.listY + this.listH) {
            int index = (int) ((my - this.listY + this.scroll) / ROW_H);
            if (index >= 0 && index < this.shown.size()) {
                this.setting.toggle(this.shown.get(index).block());
                return true;
            }
        }
        return super.method_25402(click, doubleClick);
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.scroll -= verticalAmount * ROW_H * 2.0;
        this.clampScroll();
        return true;
    }

    private void clampScroll() {
        double max = Math.max(0.0, this.shown.size() * (double) ROW_H - this.listH);
        this.scroll = Math.max(0.0, Math.min(max, this.scroll));
    }

    public boolean method_25400(class_11905 input) {
        String typed = input.method_74226();
        if (typed != null && !typed.isEmpty() && typed.chars().allMatch(c -> c >= ' ')) {
            this.query += typed;
            this.refilter();
            return true;
        }
        return super.method_25400(input);
    }

    public boolean method_25404(class_11908 keyInput) {
        int key = keyInput.comp_4795();
        boolean ctrl = (keyInput.comp_4797() & 10) != 0; // control or super (cmd on mac)
        if (key == 256) {
            this.method_25419();
            return true;
        }
        if (key == 259 && !this.query.isEmpty()) {
            this.query = ctrl ? "" : this.query.substring(0, this.query.length() - 1);
            this.refilter();
            return true;
        }
        if (key == 86 && ctrl) {
            String clip = this.field_22787.field_1774.method_1460();
            if (clip != null) {
                this.query += clip.replaceAll("[\\r\\n]", "");
                this.refilter();
            }
            return true;
        }
        return super.method_25404(keyInput);
    }

    public void method_25419() {
        this.field_22787.method_1507(this.parent);
    }
}
