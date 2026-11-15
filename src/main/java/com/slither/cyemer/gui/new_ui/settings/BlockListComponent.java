package com.slither.cyemer.gui.new_ui.settings;

import com.slither.cyemer.gui.new_ui.BlockPickerScreen;
import com.slither.cyemer.module.BlockListSetting;
import com.slither.cyemer.module.implementation.ClickGUIModule;
import com.slither.cyemer.util.GuiShaderStyle;
import com.slither.cyemer.util.Renderer;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2248;
import net.minecraft.class_310;
import net.minecraft.class_332;

/** A button showing what is picked; clicking it opens the block picker. */
@Environment(EnvType.CLIENT)
public class BlockListComponent extends SettingComponent {
    private final BlockListSetting blockSetting;
    private double hoverAnimation = 0.0;

    public BlockListComponent(BlockListSetting setting) {
        super(setting);
        this.blockSetting = setting;
    }

    @Override
    public void render(class_332 context, int mouseX, int mouseY, float delta, double alpha) {
        double dt = Math.max(0.001, Math.min((double) delta, 0.05));
        double target = this.isHovered(mouseX, mouseY) ? 1.0 : 0.0;
        this.hoverAnimation += (target - this.hoverAnimation) * (1.0 - Math.exp(-20.0 * dt));

        float fontSize = 8.5F;
        Color bg = ClickGUIModule.getColor(ClickGUIModule.getSearchBoxBackground(), alpha * (0.5 + this.hoverAnimation * 0.3));
        Renderer.get().drawRoundedRectStyled(context, (float) this.x, (float) this.y, (float) this.width, (float) this.height, 6.0F, bg, GuiShaderStyle.SEARCH);
        if (this.hoverAnimation > 0.01) {
            Color border = ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientStart(), alpha * this.hoverAnimation);
            Renderer.get().drawRoundedRectOutline(context, (float) this.x, (float) this.y, (float) this.width, (float) this.height, 6.0F, 1.0F, border);
        }

        String edit = "Edit >";
        float editWidth = Renderer.get().getTextWidth(edit, fontSize);
        double padding = 3.0;
        String summary = this.summary();
        double room = this.width - padding * 3.0 - editWidth;
        if (Renderer.get().getTextWidth(summary, fontSize) > room) {
            while (!summary.isEmpty() && Renderer.get().getTextWidth(summary + "...", fontSize) > room) {
                summary = summary.substring(0, summary.length() - 1);
            }
            summary = summary + "...";
        }

        float textY = (float) (this.y + (this.height - Renderer.get().getTextHeight(fontSize)) / 2.0);
        Color text = this.blockSetting.getBlocks().isEmpty()
                ? ClickGUIModule.getColor(ClickGUIModule.getModuleDisabledText(), alpha * 0.6)
                : ClickGUIModule.getColor(ClickGUIModule.getSearchBoxText(), alpha);
        Renderer.get().drawText(context, summary, (float) (this.x + padding), textY, fontSize, text, false);
        Color accent = ClickGUIModule.getColor(ClickGUIModule.getModuleEnabledGradientStart(), alpha * (0.7 + 0.3 * this.hoverAnimation));
        Renderer.get().drawText(context, edit, (float) (this.x + this.width - padding - editWidth), textY, fontSize, accent, false);
    }

    /** "Respawn Anchor", "Respawn Anchor, Ancient Debris", or "7 blocks". */
    private String summary() {
        List<String> names = new ArrayList<>();
        for (class_2248 block : this.blockSetting.getBlocks()) {
            names.add(block.method_9518().getString());
        }
        if (names.isEmpty()) {
            return "None selected";
        }
        if (names.size() > 2) {
            return names.size() + " blocks";
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return String.join(", ", names);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.isHovered(mouseX, mouseY)) {
            class_310 mc = class_310.method_1551();
            mc.method_1507(new BlockPickerScreen(mc.field_1755, this.blockSetting));
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    @Override
    public double getComponentHeight() {
        return 14.0;
    }
}
