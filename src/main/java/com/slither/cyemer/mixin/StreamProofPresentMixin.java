package com.slither.cyemer.mixin;

import com.slither.cyemer.util.streamproof.FramePresenter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires from inside MinecraftClient.render(boolean) right before it calls
 * window.updateDisplay(). At that moment the backbuffer holds the fully-
 * rendered frame (including any cyemer content). FramePresenter uses that
 * to feed the overlay window and to overwrite the backbuffer with the
 * saved "no cyemer" snapshot so OBS captures the safe version.
 */
@Environment(EnvType.CLIENT)
@Mixin(class_310.class)
public class StreamProofPresentMixin {
    @Inject(
            method = "method_1523(Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/class_1041;method_15998(Lnet/minecraft/class_10219;)V"
            )
    )
    private void cyemerplus$beforeUpdateDisplay(boolean tick, CallbackInfo ci) {
        FramePresenter.beforeDisplayUpdate();
    }
}
