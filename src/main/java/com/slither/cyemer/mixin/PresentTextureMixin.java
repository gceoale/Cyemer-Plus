package com.slither.cyemer.mixin;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.slither.cyemer.util.streamproof.StreamProofPresenter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_276;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Intercepts the CommandEncoder.presentTexture(view) call inside
 * class_276.method_1237. When StreamProof is on and a cyemer screen is
 * open, the presenter swaps in a backup view (frozen no-cyemer snapshot)
 * for the main window and mirrors the live view to the overlay window.
 */
@Environment(EnvType.CLIENT)
@Mixin(class_276.class)
public class PresentTextureMixin {
    @Redirect(
            method = "method_1237()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/CommandEncoder;presentTexture(Lcom/mojang/blaze3d/textures/GpuTextureView;)V"
            )
    )
    private void cyemerplus$redirectPresent(CommandEncoder encoder, GpuTextureView view) {
        StreamProofPresenter.present(encoder, view);
    }
}
