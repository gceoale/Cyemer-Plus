package com.slither.cyemer.module.implementation.targeteffect;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_4587;
import net.minecraft.class_4588;

@Environment(EnvType.CLIENT)
public abstract class BaseEffect {
    public abstract void render(class_4587 var1, class_4588 var2, class_4588 var3, EffectRenderContext var4);

    public void renderTextured(class_4587 matrices, class_4588 mainBuffer, class_4588 glowBuffer, EffectRenderContext ctx) {
        this.render(matrices, mainBuffer, glowBuffer, ctx);
    }
}
