package com.slither.cyemer.theme;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Theme {
    public String name;
    public Color panelBg;
    public Color headerBg;
    public Color moduleEnabledText;
    public Color moduleDisabledText;
    public Color textColor;
    public Color moduleEnabledBg;
    public Color moduleDisabledBg;
    public Color grad1;
    public Color grad2;

    public Theme(
        String name,
        Color panelBg,
        Color headerBg,
        Color moduleEnabledText,
        Color moduleDisabledText,
        Color textColor,
        Color moduleEnabledBg,
        Color moduleDisabledBg,
        Color grad1,
        Color grad2
    ) {
        this.name = name;
        this.panelBg = panelBg;
        this.headerBg = headerBg;
        this.moduleEnabledText = moduleEnabledText;
        this.moduleDisabledText = moduleDisabledText;
        this.textColor = textColor;
        this.moduleEnabledBg = moduleEnabledBg;
        this.moduleDisabledBg = moduleDisabledBg;
        this.grad1 = grad1;
        this.grad2 = grad2;
    }

    public Color getPanelBackgroundColor() {
        return this.panelBg;
    }

    public Color getAccentColor() {
        return this.grad1;
    }

    public Color getHeaderTextColor() {
        return this.textColor;
    }

    public String getName() {
        return this.name;
    }

    public Color getHeaderBackgroundColor() {
        return this.headerBg;
    }

    public Color getModuleEnabledTextColor() {
        return this.moduleEnabledText;
    }

    public Color getModuleDisabledTextColor() {
        return this.moduleDisabledText;
    }

    public Color getModuleEnabledBackgroundColor() {
        return this.moduleEnabledBg;
    }

    public Color getModuleDisabledBackgroundColor() {
        return this.moduleDisabledBg;
    }

    public Color getSecondaryAccentColor() {
        return this.grad2;
    }
}
