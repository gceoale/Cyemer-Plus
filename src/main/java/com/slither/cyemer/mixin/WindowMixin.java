package com.slither.cyemer.mixin;

import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1041;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({class_1041.class})
public class WindowMixin {
    @Inject(
        method = {"<init>(Lnet/minecraft/class_3678;Lnet/minecraft/class_323;Lnet/minecraft/class_543;Ljava/lang/String;Ljava/lang/String;)V"},
        at = {@At(
            value = "INVOKE",
            target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J",
            shift = Shift.BEFORE
        )},
        require = 0
    )
    private void cyemer$maybeHideWindow(CallbackInfo ci) {
        if (shouldHideWindow()) {
            GLFW.glfwWindowHint(131076, 0);
        }
    }

    private static boolean shouldHideWindow() {
        if (Boolean.getBoolean("cyemer.hide_window")) {
            return true;
        } else {
            try {
                return Files.exists(Path.of(".cyemer-hide-window"));
            } catch (Exception var1) {
                return false;
            }
        }
    }
}
