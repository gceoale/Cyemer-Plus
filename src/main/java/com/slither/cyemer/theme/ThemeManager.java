package com.slither.cyemer.theme;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ThemeManager {
    private static ThemeManager instance;
    private final List<Theme> themes = new ArrayList<>();
    private Theme currentTheme;

    private ThemeManager() {
        this.themes
            .add(
                new Theme(
                    "Cyemer Fire",
                    new Color(10, 10, 10, 200),
                    new Color(255, 60, 0, 220),
                    Color.CYAN,
                    Color.GRAY,
                    Color.WHITE,
                    new Color(0, 170, 170, 80),
                    new Color(80, 80, 80, 60),
                    new Color(180, 30, 0, 220),
                    new Color(255, 100, 60, 220)
                )
            );
        this.themes
            .add(
                new Theme(
                    "Dark",
                    new Color(21, 21, 21, 200),
                    new Color(31, 31, 31, 220),
                    new Color(0, 170, 170),
                    Color.GRAY,
                    Color.WHITE,
                    new Color(0, 170, 170, 80),
                    new Color(80, 80, 80, 60),
                    new Color(139),
                    Color.CYAN
                )
            );
        this.themes
            .add(
                new Theme(
                    "Light",
                    new Color(90, 90, 90, 200),
                    new Color(200, 200, 200, 220),
                    new Color(0, 128, 255),
                    new Color(80, 80, 80),
                    Color.BLACK,
                    new Color(0, 128, 255, 80),
                    new Color(150, 150, 150, 60),
                    new Color(0, 90, 180, 80),
                    new Color(80, 170, 255, 80)
                )
            );
        this.currentTheme = this.themes.get(this.themes.size() - 1);
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }

        return instance;
    }

    public Theme getCurrentTheme() {
        return this.currentTheme;
    }

    public void setCurrentTheme(String name) {
        for (Theme theme : this.themes) {
            if (theme.name.equalsIgnoreCase(name)) {
                this.currentTheme = theme;
                return;
            }
        }
    }

    public List<Theme> getThemes() {
        return this.themes;
    }
}
