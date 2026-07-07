package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.rendering.font.MSDFFontRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_2960;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_11719.class_11721;

@Environment(EnvType.CLIENT)
public class CustomFont extends Module {
    private static final class_2960 SANS = class_2960.method_60655("dynamic_fps", "sans");
    private static final class_2960 SEMIBOLD = class_2960.method_60655("dynamic_fps", "semibold");
    private static final class_2960 CYEMER = class_2960.method_60655("dynamic_fps", "cyemer");
    public static CustomFont INSTANCE;
    private final ModeSetting font = new ModeSetting("Font", "Minecraft", "Sans", "SemiBold", "Cyemer");
    private final BooleanSetting msdfRenderer = new BooleanSetting("MSDF Renderer", false);
    private final BooleanSetting globalFont = new BooleanSetting("Global Font", false);
    private final SliderSetting fontScale = new SliderSetting("Font Scale", 1.0, 0.5, 2.0, 2);
    private final BooleanSetting shadows = new BooleanSetting("Shadows", true);

    public CustomFont() {
        super("CustomFont", "Custom GUI font selection.", Category.CLIENT);
        INSTANCE = this;
        this.addSetting(this.font);
        this.addSetting(this.msdfRenderer);
        this.addSetting(this.globalFont);
        this.addSetting(this.fontScale);
        this.addSetting(this.shadows);
    }

    public static class_2960 getSelectedFontId() {
        if (INSTANCE != null && INSTANCE.isEnabled()) {
            String mode = INSTANCE.font.getCurrentMode();
            if ("Sans".equals(mode)) {
                return SANS;
            } else if ("SemiBold".equals(mode)) {
                return SEMIBOLD;
            } else {
                return "Cyemer".equals(mode) ? CYEMER : null;
            }
        } else {
            return null;
        }
    }

    public static boolean isGlobalFontEnabled() {
        return isGlobalSettingsEnabled() && INSTANCE.globalFont.isEnabled() && getSelectedFontId() != null;
    }

    public static boolean isMsdfEnabled() {
        return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.msdfRenderer.isEnabled() && getSelectedFontId() != null;
    }

    public static boolean isGlobalSettingsEnabled() {
        return INSTANCE != null && INSTANCE.isEnabled();
    }

    public static float getClientFontScale() {
        return INSTANCE != null && INSTANCE.isEnabled() ? (float)INSTANCE.fontScale.getValue() : 1.0F;
    }

    public static float getGlobalFontScale() {
        return !isGlobalSettingsEnabled() ? 1.0F : getClientFontScale();
    }

    public static boolean useClientShadow(boolean requestedShadow) {
        return !requestedShadow ? false : INSTANCE == null || !INSTANCE.isEnabled() || INSTANCE.shadows.isEnabled();
    }

    public static boolean useGlobalShadow(boolean requestedShadow) {
        if (!requestedShadow) {
            return false;
        } else {
            return !isGlobalSettingsEnabled() ? requestedShadow : false;
        }
    }

    public static float getFontScale() {
        return getClientFontScale();
    }

    public static boolean useShadow(boolean requestedShadow) {
        return useClientShadow(requestedShadow);
    }

    public static void drawText(class_332 context, class_327 textRenderer, String text, int x, int y, int color, boolean shadow) {
        class_2960 fontId = getSelectedFontId();
        boolean useShadow = useClientShadow(shadow);
        if (fontId == null) {
            context.method_51433(textRenderer, text, x, y, color, useShadow);
        } else if (!isMsdfEnabled() || !MSDFFontRenderer.drawText(context, fontId, text, x, y, 1.0F, color, useShadow)) {
            context.method_51439(
                textRenderer, class_2561.method_43470(text).method_10862(class_2583.field_24360.method_27704(new class_11721(fontId))), x, y, color, useShadow
            );
        }
    }

    public static void drawText(class_332 context, class_327 textRenderer, String text, float x, float y, int color, boolean shadow) {
        drawText(context, textRenderer, text, Math.round(x), Math.round(y), color, shadow);
    }

    public static void drawGlobalText(class_332 context, class_327 textRenderer, String text, int x, int y, int color, boolean shadow) {
        class_2960 fontId = getSelectedFontId();
        boolean useShadow = useGlobalShadow(shadow);
        if (fontId == null) {
            context.method_51433(textRenderer, text, x, y, color, useShadow);
        } else if (!isMsdfEnabled() || !MSDFFontRenderer.drawText(context, fontId, text, x, y, getGlobalFontScale(), color, useShadow)) {
            context.method_51439(
                textRenderer, class_2561.method_43470(text).method_10862(class_2583.field_24360.method_27704(new class_11721(fontId))), x, y, color, useShadow
            );
        }
    }

    public static int getTextWidth(class_327 textRenderer, String text) {
        class_2960 fontId = getSelectedFontId();
        if (fontId != null && isMsdfEnabled()) {
            return Math.round(MSDFFontRenderer.getTextWidth(fontId, text));
        } else {
            return fontId == null
                ? textRenderer.method_1727(text)
                : textRenderer.method_27525(class_2561.method_43470(text).method_10862(class_2583.field_24360.method_27704(new class_11721(fontId))));
        }
    }

    public static int getGlobalTextWidth(class_327 textRenderer, String text) {
        class_2960 fontId = getSelectedFontId();
        if (fontId != null && isMsdfEnabled()) {
            return Math.round(MSDFFontRenderer.getTextWidth(fontId, text) * getGlobalFontScale());
        } else {
            return fontId == null
                ? textRenderer.method_1727(text)
                : textRenderer.method_27525(class_2561.method_43470(text).method_10862(class_2583.field_24360.method_27704(new class_11721(fontId))));
        }
    }

    public static float getTextHeight(class_327 textRenderer) {
        class_2960 fontId = getSelectedFontId();
        return fontId != null && isMsdfEnabled() ? MSDFFontRenderer.getTextHeight(fontId) : 9.0F;
    }

    public static float getGlobalTextHeight(class_327 textRenderer) {
        class_2960 fontId = getSelectedFontId();
        return fontId != null && isMsdfEnabled() ? MSDFFontRenderer.getTextHeight(fontId) * getGlobalFontScale() : 9.0F;
    }
}
