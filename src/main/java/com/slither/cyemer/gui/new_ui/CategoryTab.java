package com.slither.cyemer.gui.new_ui;

import com.slither.cyemer.module.Category;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1011;

@Environment(EnvType.CLIENT)
public class CategoryTab {
    public final Category category;
    private final String iconFileName;

    public CategoryTab(Category category) {
        this.category = category;
        this.iconFileName = this.resolveFileName();
    }

    private String resolveFileName() {
        return switch (this.category) {
            case CLIENT -> "client.png";
            case MISC -> "misc.png";
            case RENDER -> "render.png";
            case COMBAT -> "combat.png";
            case MOVEMENT -> "movement.png";
            case PLAYER -> "player.png";
            default -> this.category.name().toLowerCase() + ".png";
        };
    }

    public int getResolvedIconId() {
        return this.iconFileName == null ? -1 : ClickGUIIconCache.globalIconIds.getOrDefault(this.iconFileName, -1);
    }

    public static class_1011 scaleImageStatic(class_1011 original, int targetSize) {
        class_1011 scaled = new class_1011(targetSize, targetSize, false);

        for (int py = 0; py < targetSize; py++) {
            for (int px = 0; px < targetSize; px++) {
                float srcX = (px + 0.5F) / targetSize * original.method_4307() - 0.5F;
                float srcY = (py + 0.5F) / targetSize * original.method_4323() - 0.5F;
                int x0 = Math.max(0, (int)Math.floor(srcX));
                int y0 = Math.max(0, (int)Math.floor(srcY));
                int x1 = Math.min(original.method_4307() - 1, x0 + 1);
                int y1 = Math.min(original.method_4323() - 1, y0 + 1);
                float fx = srcX - (float)Math.floor(srcX);
                float fy = srcY - (float)Math.floor(srcY);
                int c00 = original.method_61940(x0, y0);
                int c10 = original.method_61940(x1, y0);
                int c01 = original.method_61940(x0, y1);
                int c11 = original.method_61940(x1, y1);
                int a = (int)lerp(lerp(argbA(c00), argbA(c10), fx), lerp(argbA(c01), argbA(c11), fx), fy);
                int r = (int)lerp(lerp(argbR(c00), argbR(c10), fx), lerp(argbR(c01), argbR(c11), fx), fy);
                int g = (int)lerp(lerp(argbG(c00), argbG(c10), fx), lerp(argbG(c01), argbG(c11), fx), fy);
                int b = (int)lerp(lerp(argbB(c00), argbB(c10), fx), lerp(argbB(c01), argbB(c11), fx), fy);
                scaled.method_61941(px, py, a << 24 | r << 16 | g << 8 | b);
            }
        }

        return scaled;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static int argbA(int argb) {
        return argb >> 24 & 0xFF;
    }

    private static int argbR(int argb) {
        return argb >> 16 & 0xFF;
    }

    private static int argbG(int argb) {
        return argb >> 8 & 0xFF;
    }

    private static int argbB(int argb) {
        return argb & 0xFF;
    }
}
