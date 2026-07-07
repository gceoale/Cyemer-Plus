package com.slither.cyemer.module.implementation;

import com.slither.cyemer.gui.new_ui.ClickGUI;
import com.slither.cyemer.gui.new_ui.old.LegacyClickGUI;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ColorSetting;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.module.StringSetting;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ClickGUIModule extends Module {
    public static ModeSetting theme;
    public static BooleanSetting gradientMenus;
    public static BooleanSetting customHighlight;
    public static BooleanSetting highlightGradient;
    public static ColorSetting highlightColor1;
    public static ColorSetting highlightColor2;
    public static BooleanSetting blurBackground;
    public static SliderSetting blurStrength;
    public static BooleanSetting separator;
    public static BooleanSetting hover;
    public static BooleanSetting search;
    public static BooleanSetting oldGui;
    public static ColorSetting customBackgroundColor;
    public static SliderSetting backgroundOpacity;
    public static SliderSetting globalCurvature;
    public static SliderSetting shaderQuality;
    public static BooleanSetting customHeaderColour;
    public static ColorSetting customTextColor;
    public static ColorSetting customAccentColor;
    public static StringSetting displayName;
    public static StringSetting profileIcon;
    private static Map<String, ClickGUIModule.ThemeColors> themes = new HashMap<>();
    private static volatile boolean themesInitialized = false;
    private static ClickGUIModule.ThemeColors cachedTheme = null;
    private static String cachedThemeName = null;
    private static final int[] colorCacheKeys = new int[256];
    private static final Color[] colorCacheVals = new Color[256];

    public ClickGUIModule() {
        super("ClickGUI", "Opens the ClickGUI interface", Category.CLIENT);
        ensureThemesInitialized();
        theme = new ModeSetting(
            "Theme",
            "Tenacity",
            "Dark Purple",
            "Cyberpunk",
            "Ocean Blue",
            "Forest Green",
            "Sunset Orange",
            "Cherry Blossom",
            "Midnight",
            "Lava",
            "Glass",
            "Custom"
        );
        gradientMenus = new BooleanSetting("Gradient Menus", false);
        customHighlight = new BooleanSetting("Custom Highlight", false);
        highlightGradient = new BooleanSetting("Highlight Gradient", true);
        highlightColor1 = new ColorSetting("Highlight Color 1", new Color(76, 132, 255, 245));
        highlightColor2 = new ColorSetting("Highlight Color 2", new Color(126, 210, 255, 245));
        blurBackground = new BooleanSetting("Blur BG", false);
        blurStrength = new SliderSetting("Blur Strength", 12.0, 0.0, 30.0, 1);
        globalCurvature = new SliderSetting("Global Curvature", 10.0, 0.0, 28.0, 1);
        shaderQuality = new SliderSetting("Shader Quality", 0.3, 0.0, 1.0, 2);
        separator = new BooleanSetting("Separator", true);
        hover = new BooleanSetting("Hover", true);
        search = new BooleanSetting("Search", true);
        oldGui = new BooleanSetting("Old GUI", false);
        customBackgroundColor = new ColorSetting("Custom Background", new Color(24, 24, 30, 220));
        backgroundOpacity = new SliderSetting("Background Opacity", 100.0, 0.0, 100.0, 0);
        customHeaderColour = new BooleanSetting("Custom Header Colour", false);
        customTextColor = new ColorSetting("Custom Text", new Color(238, 238, 238));
        customAccentColor = new ColorSetting("Custom Accent", new Color(255, 120, 185));
        this.addSetting(theme);
        this.addSetting(gradientMenus);
        this.addSetting(customHighlight);
        this.addSetting(highlightGradient);
        this.addSetting(highlightColor1);
        this.addSetting(highlightColor2);
        this.addSetting(blurBackground);
        this.addSetting(blurStrength);
        this.addSetting(globalCurvature);
        this.addSetting(shaderQuality);
        this.addSetting(separator);
        this.addSetting(hover);
        this.addSetting(search);
        this.addSetting(oldGui);
        this.addSetting(customBackgroundColor);
        this.addSetting(backgroundOpacity);
        this.addSetting(customHeaderColour);
        this.addSetting(customTextColor);
        this.addSetting(customAccentColor);
        displayName = new StringSetting("Display Name", "User");
        profileIcon = new StringSetting("Profile Icon", "C");
        this.addSetting(displayName);
        this.addSetting(profileIcon);
    }

    private static void ensureThemesInitialized() {
        if (!themesInitialized) {
            synchronized (ClickGUIModule.class) {
                if (!themesInitialized) {
                    initializeThemes();
                    themesInitialized = true;
                }
            }
        }
    }

    private static void initializeThemes() {
        themes.put(
            "Tenacity",
            new ClickGUIModule.ThemeColors(
                new Color(25, 25, 25, 220),
                new Color(255, 180, 255, 200),
                new Color(215, 138, 255, 255),
                new Color(215, 138, 255, 255),
                new Color(30, 30, 30, 255),
                new Color(35, 35, 35, 255),
                new Color(215, 138, 255, 150),
                new Color(255, 255, 255, 255),
                new Color(100, 100, 100, 255),
                new Color(40, 45, 55, 140),
                new Color(255, 255, 255, 12),
                new Color(255, 255, 255, 255),
                new Color(170, 170, 170, 255),
                new Color(0, 255, 128, 255),
                new Color(255, 50, 50, 255),
                new Color(25, 25, 25, 180),
                new Color(255, 255, 255, 200),
                new Color(20, 20, 20, 230),
                new Color(215, 138, 255, 180),
                new Color(255, 255, 255, 255),
                new Color(255, 105, 180, 255),
                new Color(135, 206, 250, 255)
            )
        );
        themes.put(
            "Dark Purple",
            new ClickGUIModule.ThemeColors(
                new Color(20, 15, 30, 220),
                new Color(138, 43, 226, 200),
                new Color(186, 85, 211, 255),
                new Color(147, 112, 219, 255),
                new Color(25, 20, 35, 255),
                new Color(30, 25, 40, 255),
                new Color(138, 43, 226, 150),
                new Color(240, 230, 255, 255),
                new Color(120, 100, 140, 255),
                new Color(30, 25, 40, 140),
                new Color(255, 255, 255, 12),
                new Color(240, 230, 255, 255),
                new Color(160, 160, 180, 255),
                new Color(138, 43, 226, 255),
                new Color(220, 20, 60, 255),
                new Color(20, 15, 30, 180),
                new Color(180, 170, 200, 255),
                new Color(15, 10, 25, 230),
                new Color(138, 43, 226, 180),
                new Color(240, 230, 255, 255),
                new Color(138, 43, 226, 255),
                new Color(186, 85, 211, 255)
            )
        );
        themes.put(
            "Cyberpunk",
            new ClickGUIModule.ThemeColors(
                new Color(10, 10, 15, 220),
                new Color(0, 255, 255, 200),
                new Color(255, 0, 255, 255),
                new Color(0, 255, 255, 255),
                new Color(15, 15, 20, 255),
                new Color(20, 20, 30, 255),
                new Color(0, 255, 255, 150),
                new Color(0, 255, 255, 255),
                new Color(100, 100, 120, 255),
                new Color(20, 20, 30, 140),
                new Color(255, 255, 255, 12),
                new Color(0, 255, 255, 255),
                new Color(150, 150, 170, 255),
                new Color(0, 255, 255, 255),
                new Color(255, 0, 100, 255),
                new Color(10, 10, 15, 180),
                new Color(200, 200, 220, 255),
                new Color(5, 5, 10, 230),
                new Color(255, 0, 255, 180),
                new Color(0, 255, 255, 255),
                new Color(255, 0, 255, 255),
                new Color(0, 255, 255, 255)
            )
        );
        themes.put(
            "Ocean Blue",
            new ClickGUIModule.ThemeColors(
                new Color(15, 25, 35, 220),
                new Color(64, 156, 255, 200),
                new Color(100, 180, 255, 255),
                new Color(64, 156, 255, 255),
                new Color(20, 30, 40, 255),
                new Color(25, 35, 45, 255),
                new Color(64, 156, 255, 150),
                new Color(220, 240, 255, 255),
                new Color(100, 130, 160, 255),
                new Color(25, 35, 45, 140),
                new Color(255, 255, 255, 12),
                new Color(220, 240, 255, 255),
                new Color(160, 180, 200, 255),
                new Color(0, 200, 255, 255),
                new Color(255, 100, 100, 255),
                new Color(15, 25, 35, 180),
                new Color(180, 200, 220, 255),
                new Color(10, 20, 30, 230),
                new Color(64, 156, 255, 180),
                new Color(220, 240, 255, 255),
                new Color(30, 144, 255, 255),
                new Color(135, 206, 250, 255)
            )
        );
        themes.put(
            "Forest Green",
            new ClickGUIModule.ThemeColors(
                new Color(15, 25, 15, 220),
                new Color(50, 205, 50, 200),
                new Color(124, 252, 0, 255),
                new Color(50, 205, 50, 255),
                new Color(20, 30, 20, 255),
                new Color(25, 35, 25, 255),
                new Color(50, 205, 50, 150),
                new Color(240, 255, 240, 255),
                new Color(100, 130, 100, 255),
                new Color(25, 35, 25, 140),
                new Color(255, 255, 255, 12),
                new Color(240, 255, 240, 255),
                new Color(170, 180, 170, 255),
                new Color(50, 205, 50, 255),
                new Color(255, 69, 0, 255),
                new Color(15, 25, 15, 180),
                new Color(190, 200, 190, 255),
                new Color(10, 20, 10, 230),
                new Color(50, 205, 50, 180),
                new Color(240, 255, 240, 255),
                new Color(34, 139, 34, 255),
                new Color(124, 252, 0, 255)
            )
        );
        themes.put(
            "Sunset Orange",
            new ClickGUIModule.ThemeColors(
                new Color(30, 20, 15, 220),
                new Color(255, 140, 0, 200),
                new Color(255, 165, 0, 255),
                new Color(255, 140, 0, 255),
                new Color(35, 25, 20, 255),
                new Color(40, 30, 25, 255),
                new Color(255, 140, 0, 150),
                new Color(255, 245, 230, 255),
                new Color(140, 110, 90, 255),
                new Color(40, 30, 25, 140),
                new Color(255, 255, 255, 12),
                new Color(255, 245, 230, 255),
                new Color(180, 170, 160, 255),
                new Color(255, 215, 0, 255),
                new Color(220, 20, 60, 255),
                new Color(30, 20, 15, 180),
                new Color(200, 190, 180, 255),
                new Color(25, 15, 10, 230),
                new Color(255, 140, 0, 180),
                new Color(255, 245, 230, 255),
                new Color(255, 69, 0, 255),
                new Color(255, 215, 0, 255)
            )
        );
        themes.put(
            "Cherry Blossom",
            new ClickGUIModule.ThemeColors(
                new Color(30, 20, 25, 220),
                new Color(255, 182, 193, 200),
                new Color(255, 192, 203, 255),
                new Color(255, 182, 193, 255),
                new Color(35, 25, 30, 255),
                new Color(40, 30, 35, 255),
                new Color(255, 182, 193, 150),
                new Color(255, 240, 245, 255),
                new Color(140, 110, 120, 255),
                new Color(40, 30, 35, 140),
                new Color(255, 255, 255, 12),
                new Color(255, 240, 245, 255),
                new Color(180, 170, 175, 255),
                new Color(255, 192, 203, 255),
                new Color(199, 21, 133, 255),
                new Color(30, 20, 25, 180),
                new Color(200, 190, 195, 255),
                new Color(25, 15, 20, 230),
                new Color(255, 182, 193, 180),
                new Color(255, 240, 245, 255),
                new Color(255, 105, 180, 255),
                new Color(255, 192, 203, 255)
            )
        );
        themes.put(
            "Midnight",
            new ClickGUIModule.ThemeColors(
                new Color(10, 10, 20, 220),
                new Color(70, 130, 180, 200),
                new Color(135, 206, 250, 255),
                new Color(70, 130, 180, 255),
                new Color(15, 15, 25, 255),
                new Color(20, 20, 30, 255),
                new Color(70, 130, 180, 150),
                new Color(230, 240, 255, 255),
                new Color(80, 90, 110, 255),
                new Color(20, 20, 30, 140),
                new Color(255, 255, 255, 12),
                new Color(230, 240, 255, 255),
                new Color(150, 160, 180, 255),
                new Color(100, 200, 255, 255),
                new Color(220, 20, 60, 255),
                new Color(10, 10, 20, 180),
                new Color(170, 180, 200, 255),
                new Color(5, 5, 15, 230),
                new Color(70, 130, 180, 180),
                new Color(230, 240, 255, 255),
                new Color(25, 25, 112, 255),
                new Color(135, 206, 250, 255)
            )
        );
        themes.put(
            "Lava",
            new ClickGUIModule.ThemeColors(
                new Color(30, 10, 5, 220),
                new Color(255, 69, 0, 200),
                new Color(255, 99, 71, 255),
                new Color(255, 69, 0, 255),
                new Color(35, 15, 10, 255),
                new Color(40, 20, 15, 255),
                new Color(255, 69, 0, 150),
                new Color(255, 230, 200, 255),
                new Color(130, 90, 80, 255),
                new Color(40, 20, 15, 140),
                new Color(255, 255, 255, 12),
                new Color(255, 230, 200, 255),
                new Color(170, 150, 140, 255),
                new Color(255, 140, 0, 255),
                new Color(139, 0, 0, 255),
                new Color(30, 10, 5, 180),
                new Color(190, 170, 160, 255),
                new Color(25, 5, 0, 230),
                new Color(255, 69, 0, 180),
                new Color(255, 230, 200, 255),
                new Color(178, 34, 34, 255),
                new Color(255, 140, 0, 255)
            )
        );
        themes.put(
            "Glass",
            new ClickGUIModule.ThemeColors(
                new Color(20, 22, 30, 120),
                new Color(180, 200, 255, 140),
                new Color(220, 230, 255, 255),
                new Color(160, 180, 220, 200),
                new Color(30, 35, 50, 100),
                new Color(40, 45, 65, 130),
                new Color(140, 170, 255, 90),
                new Color(230, 235, 255, 255),
                new Color(150, 160, 190, 180),
                new Color(25, 28, 40, 90),
                new Color(50, 55, 75, 110),
                new Color(245, 248, 255, 255),
                new Color(170, 180, 210, 200),
                new Color(120, 160, 255, 200),
                new Color(200, 100, 100, 180),
                new Color(25, 28, 40, 130),
                new Color(220, 225, 240, 240),
                new Color(15, 18, 28, 180),
                new Color(120, 150, 220, 120),
                new Color(230, 235, 255, 255),
                new Color(100, 140, 240, 220),
                new Color(160, 190, 255, 220)
            )
        );
    }

    @Override
    public void onEnable() {
        if (this.mc.field_1755 == null) {
            if (useOldGui()) {
                this.mc.method_1507(new LegacyClickGUI());
            } else {
                this.mc.method_1507(new ClickGUI());
            }
        }

        this.setEnabled(false);
    }

    private static ClickGUIModule.ThemeColors getCurrentTheme() {
        ensureThemesInitialized();
        String currentName = theme != null ? theme.getCurrentMode() : "Tenacity";
        if (cachedTheme != null && currentName.equals(cachedThemeName)) {
            return cachedTheme;
        } else {
            ClickGUIModule.ThemeColors result;
            if ("Custom".equals(currentName)) {
                result = buildCustomTheme();
            } else {
                result = themes.getOrDefault(currentName, themes.get("Tenacity"));
            }

            if (result == null) {
                result = buildEmergencyTheme();
            }

            cachedTheme = result;
            cachedThemeName = currentName;
            return result;
        }
    }

    private static ClickGUIModule.ThemeColors buildEmergencyTheme() {
        return new ClickGUIModule.ThemeColors(
            new Color(25, 25, 25, 220),
            new Color(255, 180, 255, 200),
            new Color(215, 138, 255, 255),
            new Color(215, 138, 255, 255),
            new Color(30, 30, 30, 255),
            new Color(35, 35, 35, 255),
            new Color(215, 138, 255, 150),
            new Color(255, 255, 255, 255),
            new Color(100, 100, 100, 255),
            new Color(40, 45, 55, 140),
            new Color(255, 255, 255, 12),
            new Color(255, 255, 255, 255),
            new Color(170, 170, 170, 255),
            new Color(0, 255, 128, 255),
            new Color(255, 50, 50, 255),
            new Color(25, 25, 25, 180),
            new Color(255, 255, 255, 200),
            new Color(20, 20, 20, 230),
            new Color(215, 138, 255, 180),
            new Color(255, 255, 255, 255),
            new Color(255, 105, 180, 255),
            new Color(135, 206, 250, 255)
        );
    }

    private static ClickGUIModule.ThemeColors buildCustomTheme() {
        Color baseSource = customBackgroundColor != null ? customBackgroundColor.getValue() : new Color(24, 24, 30, 220);
        Color base = withAlpha(baseSource, 220);
        Color text = customTextColor != null ? withAlpha(customTextColor.getValue(), 255) : new Color(238, 238, 238, 255);
        Color accent = customAccentColor != null ? withAlpha(customAccentColor.getValue(), 255) : new Color(255, 120, 185, 255);
        Color panelBg = withAlpha(base, 220);
        Color panelGlow = withAlpha(accent, 180);
        Color panelParticles = withAlpha(accent, 210);
        Color searchBg = withAlpha(darken(base, 0.14), 170);
        Color searchFocusedBg = withAlpha(darken(base, 0.06), 208);
        Color searchBorder = withAlpha(accent, 165);
        Color moduleBg = withAlpha(darken(base, 0.18), 150);
        Color moduleHoverBg = withAlpha(darken(base, 0.1), 188);
        Color moduleDisabledText = withAlpha(blend(text, base, 0.35), 230);
        Color moduleDisabledDot = withAlpha(blend(text, base, 0.55), 200);
        Color settingsBg = withAlpha(darken(base, 0.12), 160);
        Color tooltipBg = withAlpha(darken(base, 0.2), 235);
        Color tooltipBorder = withAlpha(accent, 170);
        Color placeholder = withAlpha(blend(text, base, 0.5), 210);
        Color gradStart = brighten(accent, 0.12);
        Color gradEnd = darken(accent, 0.2);
        return new ClickGUIModule.ThemeColors(
            panelBg,
            panelGlow,
            text,
            panelParticles,
            searchBg,
            searchFocusedBg,
            searchBorder,
            text,
            placeholder,
            moduleBg,
            moduleHoverBg,
            text,
            moduleDisabledText,
            accent,
            moduleDisabledDot,
            settingsBg,
            text,
            tooltipBg,
            tooltipBorder,
            text,
            gradStart,
            gradEnd
        );
    }

    public static Color getPanelBackground() {
        return applyBackgroundOpacity(getCurrentTheme().panelBackground);
    }

    public static Color getHeaderBackground() {
        Color panelBg = getPanelBackground();
        return customHeaderColour != null && customHeaderColour.isEnabled() ? withAlpha(darken(panelBg, 0.06), panelBg.getAlpha()) : panelBg;
    }

    public static boolean useCustomHeaderColour() {
        return customHeaderColour != null && customHeaderColour.isEnabled();
    }

    public static Color getPanelTitleGlow() {
        return applyBackgroundOpacity(getCurrentTheme().panelTitleGlow);
    }

    public static Color getPanelTitleText() {
        return getCurrentTheme().panelTitleText;
    }

    public static Color getPanelParticles() {
        return applyBackgroundOpacity(getCurrentTheme().panelParticles);
    }

    public static Color getSearchBoxBackground() {
        return applyBackgroundOpacity(getCurrentTheme().searchBoxBackground);
    }

    public static Color getSearchBoxFocusedBackground() {
        return applyBackgroundOpacity(getCurrentTheme().searchBoxFocusedBackground);
    }

    public static Color getSearchBoxBorder() {
        return applyBackgroundOpacity(getCurrentTheme().searchBoxBorder);
    }

    public static Color getSearchBoxText() {
        return getCurrentTheme().searchBoxText;
    }

    public static Color getSearchBoxPlaceholder() {
        return getCurrentTheme().searchBoxPlaceholder;
    }

    public static Color getModuleButtonBackground() {
        return applyBackgroundOpacity(getCurrentTheme().moduleButtonBackground);
    }

    public static Color getModuleButtonHoverBackground() {
        return applyBackgroundOpacity(getCurrentTheme().moduleButtonHoverBackground);
    }

    public static Color getModuleEnabledText() {
        return getCurrentTheme().moduleEnabledText;
    }

    public static Color getModuleDisabledText() {
        return getCurrentTheme().moduleDisabledText;
    }

    public static Color getModuleEnabledDot() {
        return getCurrentTheme().moduleEnabledDot;
    }

    public static Color getModuleDisabledDot() {
        return getCurrentTheme().moduleDisabledDot;
    }

    public static Color getSettingsBackground() {
        return applyBackgroundOpacity(getCurrentTheme().settingsBackground);
    }

    public static Color getSettingsText() {
        return getCurrentTheme().settingsText;
    }

    public static Color getTooltipBackground() {
        return applyBackgroundOpacity(getCurrentTheme().tooltipBackground);
    }

    public static Color getTooltipBorder() {
        return applyBackgroundOpacity(getCurrentTheme().tooltipBorder);
    }

    public static Color getTooltipText() {
        return getCurrentTheme().tooltipText;
    }

    public static Color getModuleEnabledGradientStart() {
        return useCustomHighlight() && highlightColor1 != null ? highlightColor1.getValue() : getCurrentTheme().moduleEnabledGradientStart;
    }

    public static Color getModuleEnabledGradientEnd() {
        if (useCustomHighlight()) {
            if (useHighlightGradient() && highlightColor2 != null) {
                return highlightColor2.getValue();
            } else {
                return highlightColor1 != null ? highlightColor1.getValue() : getCurrentTheme().moduleEnabledGradientStart;
            }
        } else {
            return getCurrentTheme().moduleEnabledGradientEnd;
        }
    }

    public static float getCornerRadius() {
        return globalCurvature == null ? 10.0F : (float)Math.max(0.0, Math.min(28.0, globalCurvature.getValue()));
    }

    public static float getCornerRadiusScaled(double factor) {
        return (float)Math.max(0.0, getCornerRadius() * factor);
    }

    public static float getGuiCornerRadius() {
        return Math.max(0.0F, Math.min(22.0F, getCornerRadius()));
    }

    public static float getGuiCornerRadiusScaled(double factor) {
        return (float)Math.max(0.0, getGuiCornerRadius() * factor);
    }

    public static String getIconColorMode() {
        return "White";
    }

    public static boolean useBlackIcons() {
        return false;
    }

    public static boolean useGradientMenus() {
        return gradientMenus != null && gradientMenus.isEnabled();
    }

    public static boolean useCustomHighlight() {
        return customHighlight != null && customHighlight.isEnabled();
    }

    public static boolean useHighlightGradient() {
        return highlightGradient == null || highlightGradient.isEnabled();
    }

    public static boolean useBlurBackground() {
        return blurBackground != null && blurBackground.isEnabled();
    }

    public static float getBlurStrength() {
        return blurStrength == null ? 12.0F : (float)Math.max(0.0, blurStrength.getValue());
    }

    public static boolean useSeparator() {
        return separator == null || separator.isEnabled();
    }

    public static boolean useHover() {
        return hover == null || hover.isEnabled();
    }

    public static boolean useSearch() {
        return search == null || search.isEnabled();
    }

    public static boolean useSquareGui() {
        return false;
    }

    public static boolean useOldGui() {
        return oldGui != null && oldGui.isEnabled();
    }

    public static boolean useShadows() {
        return true;
    }

    public static float getGradientQualityFactor() {
        return shaderQuality == null ? 0.3F : (float)Math.max(0.1, Math.min(0.75, shaderQuality.getValue()));
    }

    public static float getUiResolutionScale() {
        return 1.0F;
    }

    private static Color applyBackgroundOpacity(Color color) {
        if (color == null) {
            color = new Color(25, 25, 25, 220);
        }

        if (backgroundOpacity == null) {
            return color;
        } else {
            double percent = Math.max(0.0, Math.min(100.0, backgroundOpacity.getValue()));
            int alpha = (int)Math.round(color.getAlpha() * (percent / 100.0));
            return withAlpha(color, alpha);
        }
    }

    public static Color getColor(Color color, double alpha) {
        if (color == null) {
            color = new Color(255, 255, 255, 255);
        }

        int computedAlpha = Math.max(0, Math.min(255, (int)Math.round(color.getAlpha() * alpha)));
        int key = color.getRGB() & 16777215 | computedAlpha << 24;
        int slot = key & 0xFF;
        if (colorCacheKeys[slot] == key && colorCacheVals[slot] != null) {
            return colorCacheVals[slot];
        } else {
            Color result = new Color(color.getRed(), color.getGreen(), color.getBlue(), computedAlpha);
            colorCacheKeys[slot] = key;
            colorCacheVals[slot] = result;
            return result;
        }
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private static Color blend(Color a, Color b, double t) {
        double clamped = Math.max(0.0, Math.min(1.0, t));
        int r = (int)Math.round(a.getRed() + (b.getRed() - a.getRed()) * clamped);
        int g = (int)Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * clamped);
        int bl = (int)Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * clamped);
        int al = (int)Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * clamped);
        return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, bl)), Math.max(0, Math.min(255, al)));
    }

    private static Color darken(Color color, double amount) {
        double clamped = Math.max(0.0, Math.min(1.0, amount));
        int r = (int)Math.round(color.getRed() * (1.0 - clamped));
        int g = (int)Math.round(color.getGreen() * (1.0 - clamped));
        int b = (int)Math.round(color.getBlue() * (1.0 - clamped));
        return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)), color.getAlpha());
    }

    private static Color brighten(Color color, double amount) {
        double clamped = Math.max(0.0, Math.min(1.0, amount));
        int r = (int)Math.round(color.getRed() + (255 - color.getRed()) * clamped);
        int g = (int)Math.round(color.getGreen() + (255 - color.getGreen()) * clamped);
        int b = (int)Math.round(color.getBlue() + (255 - color.getBlue()) * clamped);
        return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, g)), Math.max(0, Math.min(255, b)), color.getAlpha());
    }

    @Environment(EnvType.CLIENT)
    private static class ThemeColors {
        public final Color panelBackground;
        public final Color panelTitleGlow;
        public final Color panelTitleText;
        public final Color panelParticles;
        public final Color searchBoxBackground;
        public final Color searchBoxFocusedBackground;
        public final Color searchBoxBorder;
        public final Color searchBoxText;
        public final Color searchBoxPlaceholder;
        public final Color moduleButtonBackground;
        public final Color moduleButtonHoverBackground;
        public final Color moduleEnabledText;
        public final Color moduleDisabledText;
        public final Color moduleEnabledDot;
        public final Color moduleDisabledDot;
        public final Color settingsBackground;
        public final Color settingsText;
        public final Color tooltipBackground;
        public final Color tooltipBorder;
        public final Color tooltipText;
        public final Color moduleEnabledGradientStart;
        public final Color moduleEnabledGradientEnd;

        public ThemeColors(
            Color panelBackground,
            Color panelTitleGlow,
            Color panelTitleText,
            Color panelParticles,
            Color searchBoxBackground,
            Color searchBoxFocusedBackground,
            Color searchBoxBorder,
            Color searchBoxText,
            Color searchBoxPlaceholder,
            Color moduleButtonBackground,
            Color moduleButtonHoverBackground,
            Color moduleEnabledText,
            Color moduleDisabledText,
            Color moduleEnabledDot,
            Color moduleDisabledDot,
            Color settingsBackground,
            Color settingsText,
            Color tooltipBackground,
            Color tooltipBorder,
            Color tooltipText,
            Color moduleEnabledGradientStart,
            Color moduleEnabledGradientEnd
        ) {
            this.panelBackground = panelBackground;
            this.panelTitleGlow = panelTitleGlow;
            this.panelTitleText = panelTitleText;
            this.panelParticles = panelParticles;
            this.searchBoxBackground = searchBoxBackground;
            this.searchBoxFocusedBackground = searchBoxFocusedBackground;
            this.searchBoxBorder = searchBoxBorder;
            this.searchBoxText = searchBoxText;
            this.searchBoxPlaceholder = searchBoxPlaceholder;
            this.moduleButtonBackground = moduleButtonBackground;
            this.moduleButtonHoverBackground = moduleButtonHoverBackground;
            this.moduleEnabledText = moduleEnabledText;
            this.moduleDisabledText = moduleDisabledText;
            this.moduleEnabledDot = moduleEnabledDot;
            this.moduleDisabledDot = moduleDisabledDot;
            this.settingsBackground = settingsBackground;
            this.settingsText = settingsText;
            this.tooltipBackground = tooltipBackground;
            this.tooltipBorder = tooltipBorder;
            this.tooltipText = tooltipText;
            this.moduleEnabledGradientStart = moduleEnabledGradientStart;
            this.moduleEnabledGradientEnd = moduleEnabledGradientEnd;
        }
    }
}
