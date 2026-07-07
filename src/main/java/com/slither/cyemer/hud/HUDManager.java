package com.slither.cyemer.hud;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public class HUDManager {
    private static HUDManager instance;
    private final List<HUDElement> elements = new ArrayList<>();
    private static double lastHitReach = 0.0;

    private HUDManager() {
        this.elements.add(new FPSHudElement("FPS", 10.0, 10.0));
        this.elements.add(new CoordinatesHudElement("Coordinates", 10.0, 35.0));
        this.elements.add(new TargetHudElement("TargetHUD", 10.0, 60.0));
        this.elements.add(new ReachHudElement("Last Reach", 10.0, 90.0));
        this.elements.add(new EffectHudElement("EffectHUD", 10.0, 120.0));
    }

    public static double getLastHitReach() {
        return lastHitReach;
    }

    public static void updateLastHitReach(double reach) {
        lastHitReach = reach;
    }

    public void initialize() {
        TotemPopManager.getInstance();
    }

    public void render(class_332 context, float delta) {
        for (HUDElement element : this.elements) {
            if (element.isEnabled()) {
                element.render(context, delta);
            }
        }
    }

    public List<HUDElement> getElements() {
        return this.elements;
    }

    public HUDElement getElement(String name) {
        for (HUDElement element : this.elements) {
            if (element.getName().equalsIgnoreCase(name)) {
                return element;
            }
        }

        return null;
    }

    public static HUDManager getInstance() {
        if (instance == null) {
            instance = new HUDManager();
        }

        return instance;
    }
}
