package com.slither.cyemer.mixin;

import com.slither.cyemer.util.streamproof.OverlayCoordinator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_332;
import net.minecraft.class_437;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Redirects cyemer's own screens (ClickGUI, HudEditor, ConfigScreen, etc.)
 * to draw into the stream-proof overlay framebuffer instead of the main
 * one, so OBS doesn't see them.
 *
 * Non-cyemer screens (chat, vanilla inventory, other mods' screens) are
 * left alone - they still render to the main framebuffer and remain
 * visible to screen capture.
 */
@Environment(EnvType.CLIENT)
@Mixin(class_437.class)
public class CyemerScreenMixin {
    @Inject(method = "method_25394(Lnet/minecraft/class_332;IIF)V", at = @At("HEAD"))
    private void cyemerplus$beginRedirect(class_332 context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.cyemerplus$shouldRedirect()) {
            OverlayCoordinator.beginRedirect();
        }
    }

    @Inject(method = "method_25394(Lnet/minecraft/class_332;IIF)V", at = @At("RETURN"))
    private void cyemerplus$endRedirect(class_332 context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.cyemerplus$shouldRedirect()) {
            OverlayCoordinator.endRedirect();
        }
    }

    private boolean cyemerplus$shouldRedirect() {
        Class<?> cls = ((Object) this).getClass();
        return cls.getName().startsWith("com.slither.cyemer.");
    }
}
