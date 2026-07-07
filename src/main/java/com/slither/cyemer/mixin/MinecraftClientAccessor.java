package com.slither.cyemer.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin({class_310.class})
public interface MinecraftClientAccessor {
    @Accessor("field_1752")
    int getItemUseCooldown();

    @Mutable
    @Accessor("field_1752")
    void setItemUseCooldown(int var1);

    @Invoker("method_1536")
    boolean attack();

    @Invoker("method_1583")
    void useItem();
}
