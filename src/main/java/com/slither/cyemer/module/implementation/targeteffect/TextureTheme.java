package com.slither.cyemer.module.implementation.targeteffect;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2960;

@Environment(EnvType.CLIENT)
public enum TextureTheme {
    SOLID("Solid", null, false, null),
    LIQUID_METAL("Liquid Metal", null, false, "liquid_metal"),
    LAVA("Lava", null, false, "lava"),
    WATER("Water", null, false, "water");

    private final String displayName;
    private final class_2960 textureId;
    private final boolean animated;
    private final String shaderName;

    private TextureTheme(String displayName, class_2960 textureId, boolean animated, String shaderName) {
        this.displayName = displayName;
        this.textureId = textureId;
        this.animated = animated;
        this.shaderName = shaderName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public class_2960 getTextureId() {
        return this.textureId;
    }

    public class_2960 getTexture() {
        return this.textureId;
    }

    @Deprecated
    public boolean isAnimated() {
        return this.animated;
    }

    public String getShaderName() {
        return this.shaderName;
    }

    public boolean usesShader() {
        return this.shaderName != null;
    }

    public boolean hasTexture() {
        return this.usesShader() || this.textureId != null;
    }

    @Deprecated
    public float[] getAnimatedUVs(long time, double speedMultiplier) {
        return new float[]{0.0F, 0.0F};
    }

    public Color applyThemeTint(Color baseColor) {
        switch (this) {
            case LIQUID_METAL: {
                float[] hsb = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null);
                hsb[1] *= 0.8F;
                hsb[2] *= 1.1F;
                return Color.getHSBColor(hsb[0], hsb[1], Math.min(1.0F, hsb[2]));
            }
            case LAVA: {
                float[] hsb = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null);
                hsb[1] = Math.min(1.0F, hsb[1] * 1.2F);
                hsb[2] *= 1.3F;
                return Color.getHSBColor(hsb[0], hsb[1], Math.min(1.0F, hsb[2]));
            }
            case WATER: {
                float[] hsb = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null);
                hsb[0] = (hsb[0] + 0.55F) % 1.0F;
                hsb[1] *= 0.9F;
                return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
            }
            default:
                return baseColor;
        }
    }

    public static TextureTheme fromString(String name) {
        for (TextureTheme theme : values()) {
            if (theme.displayName.equalsIgnoreCase(name)) {
                return theme;
            }
        }

        return SOLID;
    }

    public static String[] getDisplayNames() {
        TextureTheme[] themes = values();
        String[] names = new String[themes.length];

        for (int i = 0; i < themes.length; i++) {
            names[i] = themes[i].displayName;
        }

        return names;
    }
}
