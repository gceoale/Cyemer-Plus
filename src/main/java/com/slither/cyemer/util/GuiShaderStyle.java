package com.slither.cyemer.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2960;

@Environment(EnvType.CLIENT)
public enum GuiShaderStyle {
    DEFAULT(class_2960.method_60655("dynamic_fps", "curve")),
    PANEL(class_2960.method_60655("dynamic_fps", "gui_panel")),
    PANEL_GLASS(class_2960.method_60655("dynamic_fps", "gui_panel_glass")),
    HEADER(class_2960.method_60655("dynamic_fps", "gui_header")),
    HEADER_GLASS(class_2960.method_60655("dynamic_fps", "gui_header_glass")),
    ROW(class_2960.method_60655("dynamic_fps", "gui_row")),
    ROW_ACTIVE(class_2960.method_60655("dynamic_fps", "gui_row_active")),
    ROW_ENABLED(class_2960.method_60655("dynamic_fps", "gui_row_enabled")),
    ROW_LAST(class_2960.method_60655("dynamic_fps", "gui_row_last")),
    SEARCH(class_2960.method_60655("dynamic_fps", "gui_search")),
    CONTROL(class_2960.method_60655("dynamic_fps", "gui_control")),
    ICON(class_2960.method_60655("dynamic_fps", "gui_icon")),
    TOOLTIP(class_2960.method_60655("dynamic_fps", "gui_tooltip")),
    HUD(class_2960.method_60655("dynamic_fps", "gui_hud")),
    GLOW(class_2960.method_60655("dynamic_fps", "gui_glow")),
    PARTICLE(class_2960.method_60655("dynamic_fps", "gui_particle")),
    PARTICLE_GLOW(class_2960.method_60655("dynamic_fps", "gui_particle_glow"));

    private final class_2960 pipelineId;

    private GuiShaderStyle(class_2960 pipelineId) {
        this.pipelineId = pipelineId;
    }

    public class_2960 pipelineId() {
        return this.pipelineId;
    }
}
