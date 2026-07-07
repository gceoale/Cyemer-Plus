package com.slither.cyemer.mixin;

import com.slither.cyemer.module.Module;
import com.slither.cyemer.shader.PostShaderManager;
import com.slither.cyemer.util.ModuleAccess;
import com.slither.cyemer.util.RenderBackendDetector;
import com.slither.cyemer.util.RotationManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_4184;
import net.minecraft.class_5912;
import net.minecraft.class_757;
import net.minecraft.class_7833;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({class_757.class})
public class GameRendererMixin {
    @Redirect(
        method = {"method_3188(Lnet/minecraft/class_9779;)V"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_4184;method_23767()Lorg/joml/Quaternionf;"
        )
    )
    private Quaternionf redirectCameraRotation(class_4184 instance) {
        if (RenderBackendDetector.isVulkanBackend()) {
            return instance.method_23767();
        } else if (RotationManager.isSilentRotationActive() && !instance.method_19333()) {
            float visualYaw = RotationManager.getVisualYaw() + 180.0F;
            float visualPitch = RotationManager.getVisualPitch();
            if (Float.isFinite(visualYaw) && Float.isFinite(visualPitch)) {
                Quaternionf customRotation = new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F);
                customRotation.mul(class_7833.field_40716.rotationDegrees(-visualYaw));
                customRotation.mul(class_7833.field_40713.rotationDegrees(visualPitch));
                return Float.isFinite(customRotation.x)
                        && Float.isFinite(customRotation.y)
                        && Float.isFinite(customRotation.z)
                        && Float.isFinite(customRotation.w)
                    ? customRotation
                    : instance.method_23767();
            } else {
                return instance.method_23767();
            }
        } else {
            return instance.method_23767();
        }
    }

    @Inject(
        method = {"method_3172(FZLorg/joml/Matrix4f;)V"},
        at = {@At("HEAD")},
        require = 0
    )
    private void onRenderHandHead(CallbackInfo ci) {
        Module handCham = ModuleAccess.getModule("HandCham");
        if (handCham != null && handCham.isEnabled()) {
            ModuleAccess.invoke(handCham, "prepareRender", null);
        }
    }

    @Inject(
        method = {"method_3172(FZLorg/joml/Matrix4f;)V"},
        at = {@At("RETURN")},
        require = 0
    )
    private void onRenderHandReturn(CallbackInfo ci) {
        Module handCham = ModuleAccess.getModule("HandCham");
        if (handCham != null && handCham.isEnabled()) {
            ModuleAccess.invoke(handCham, "drawRender", null);
        }
    }

    @Inject(
        method = {"method_34521(Lnet/minecraft/class_5912;)V"},
        at = {@At("HEAD")}
    )
    private void onPreloadProgramsStart(class_5912 factory, CallbackInfo ci) {
        try {
            PostShaderManager.getInstance().clear();
        } catch (Exception var4) {
        }
    }

    @Inject(
        method = {"method_34521(Lnet/minecraft/class_5912;)V"},
        at = {@At("TAIL")}
    )
    private void onPreloadProgramsEnd(class_5912 factory, CallbackInfo ci) {
        try {
            if (RenderBackendDetector.isVulkanBackend()) {
                return;
            }

            PostShaderManager manager = PostShaderManager.getInstance();
            manager.registerShader(PostShaderManager.Effects.BLUR);
        } catch (Exception var4) {
        }
    }
}
