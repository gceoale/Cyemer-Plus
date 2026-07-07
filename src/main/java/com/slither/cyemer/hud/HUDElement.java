package com.slither.cyemer.hud;

import com.slither.cyemer.module.Setting;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public abstract class HUDElement {
    private final String name;
    private double x;
    private double y;
    private double width;
    private double height;
    private boolean enabled = true;
    private boolean dragging = false;
    private double dragOffsetX;
    private double dragOffsetY;

    public HUDElement(String name, double defaultX, double defaultY) {
        this.name = name;
        this.x = defaultX;
        this.y = defaultY;
        this.width = 50.0;
        this.height = 12.0;
    }

    public abstract List<Setting> getSettings();

    public abstract void render(class_332 var1, float var2);

    public void startDragging(double mouseX, double mouseY) {
        this.dragging = true;
        this.dragOffsetX = mouseX - this.x;
        this.dragOffsetY = mouseY - this.y;
    }

    public void stopDragging() {
        this.dragging = false;
    }

    public void mouseDragged(double mouseX, double mouseY) {
        if (this.dragging) {
            this.x = mouseX - this.dragOffsetX;
            this.y = mouseY - this.dragOffsetY;
        }
    }

    public double getWidth() {
        return this.width;
    }

    public double getHeight() {
        return this.height;
    }

    public String getName() {
        return this.name;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public void setHeight(double height) {
        this.height = height;
    }
}
