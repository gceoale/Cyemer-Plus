package com.slither.cyemer.gui.new_ui;

import com.slither.cyemer.gui.new_ui.old.ModuleButton;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.implementation.ClickGUIModule;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector2d;

@Environment(EnvType.CLIENT)
public final class ClickGUILayout {
    public static final double TOTAL_WIDTH = 620.0;
    public static final double SIDEBAR_WIDTH = 62.0;
    public static final double CONTENT_WIDTH = 558.0;
    public static final double CONTAINER_HEIGHT = 380.0;
    public static final double SIDEBAR_ICON_SIZE = 36.0;
    public static final double SIDEBAR_ICON_SPACING = 8.0;
    public static final double SIDEBAR_PADDING_TOP = 14.0;
    public static final double SIDEBAR_PROFILE_HEIGHT = 60.0;
    public static final double SEARCH_HEIGHT = 26.0;
    public static final double SEARCH_BOTTOM_GAP = 10.0;
    public static final double MODULE_CELL_HEIGHT = 34.0;
    public static final double CELL_PADDING = 8.0;
    public static final int GRID_COLUMNS = 3;
    public static final double GRID_PADDING_X = 14.0;
    public static final double GRID_PADDING_Y = 10.0;
    public static final double FRIENDS_ROW_HEIGHT = 32.0;
    public static final double FRIENDS_ROW_SPACING = 6.0;
    public static final double FRIENDS_PADDING_X = 14.0;
    public static final double FRIENDS_PADDING_TOP = 10.0;
    public static final double FRIENDS_INPUT_HEIGHT = 28.0;
    public static final double FRIENDS_KEYBIND_WIDTH = 120.0;
    public static final float CONTAINER_RADIUS = 18.0F;
    public static final float SIDEBAR_ICON_RADIUS = 12.0F;
    public static final float CELL_RADIUS = 12.0F;
    public static final float SEARCH_RADIUS = 10.0F;
    public static final double SETTINGS_DENSITY = 0.5;
    public static final double SETTINGS_HEADER_HEIGHT = 29.0;
    public static final double SETTINGS_PADDING = 7.0;
    public static final double SETTINGS_ITEM_SPACING = Math.max(4.0, 3.5);
    public static final double SETTINGS_LABEL_HEIGHT = Math.max(6.0, 6.0);
    public static final double SETTINGS_LABEL_GAP = 5.0;
    public static final float SETTINGS_BACK_SIZE = 22.0F;
    public static final double PROFILE_AFTER_AVATAR_GAP = 10.0;
    public static final double PROFILE_STATUS_ROW_HEIGHT = 18.0;
    public static final double PROFILE_LABEL_GAP = 10.0;
    public static final double PROFILE_AFTER_INPUT_GAP = 6.0;
    public static final double PROFILE_SEPARATOR_GAP = 8.0;
    public static final double PROFILE_HEADING_GAP = 12.0;
    public static final double MIN_ZOOM = 0.5;
    public static final double MAX_ZOOM = 2.0;
    public static final double ZOOM_SENSITIVITY = 0.05;

    private ClickGUILayout() {
    }

    public static float getContainerRadius() {
        return Math.max(3.0F, Math.min(18.0F, ClickGUIModule.getGuiCornerRadiusScaled(0.9F)));
    }

    public static float getSidebarIconRadius() {
        return Math.max(4.0F, Math.min(12.0F, getContainerRadius() * 0.72F));
    }

    public static float getCellRadius() {
        return Math.max(3.0F, Math.min(12.0F, ClickGUIModule.getGuiCornerRadiusScaled(0.75)));
    }

    public static float getSearchRadius() {
        return Math.max(3.0F, Math.min(10.0F, ClickGUIModule.getGuiCornerRadiusScaled(0.65F)));
    }

    public static double backOut(double t) {
        double s = 1.70158;
        return --t * t * (2.70158 * t + 1.70158) + 1.0;
    }

    public static Vector2d transformMouse(double mx, double my, double fz, double as, float ox, float oy) {
        double cx = (mx - ox) / as / fz;
        double cy = (my - oy) / as / fz;
        return new Vector2d(cx, cy);
    }

    public static List<ModuleButton> visibleButtons(int tabIndex, List<CategoryTab> tabs, Map<Category, List<ModuleButton>> buttons, String query) {
        if (tabIndex >= 0 && tabIndex < tabs.size()) {
            Category cat = tabs.get(tabIndex).category;
            List<ModuleButton> all = buttons.getOrDefault(cat, new ArrayList<>());
            if (query.isEmpty()) {
                return all;
            } else {
                String q = query.toLowerCase();
                List<ModuleButton> filtered = new ArrayList<>();

                for (ModuleButton b : all) {
                    if (b.module.getName().toLowerCase().contains(q)) {
                        filtered.add(b);
                    }
                }

                return filtered;
            }
        } else {
            return new ArrayList<>();
        }
    }
}
