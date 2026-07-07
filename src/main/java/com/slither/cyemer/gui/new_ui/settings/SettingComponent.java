package com.slither.cyemer.gui.new_ui.settings;

import com.slither.cyemer.module.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public abstract class SettingComponent {
    public final Setting setting;
    public double x;
    public double y;
    public double width;
    public double height;

    public SettingComponent(Setting setting) {
        this.setting = setting;
    }

    public abstract void render(class_332 var1, int var2, int var3, float var4, double var5);

    public abstract void mouseClicked(double var1, double var3, int var5);

    public abstract void mouseReleased(double var1, double var3, int var5);

    protected boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
    }

    public abstract double getComponentHeight();

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
    }

    public void charTyped(char chr, int modifiers) {
    }
}
