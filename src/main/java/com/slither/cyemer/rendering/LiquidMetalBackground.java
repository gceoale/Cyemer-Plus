package com.slither.cyemer.rendering;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public class LiquidMetalBackground {
    public void render(class_332 context, int screenWidth, int screenHeight, float delta) {
        context.method_25294(0, 0, screenWidth, screenHeight, -16777216);
    }
}
