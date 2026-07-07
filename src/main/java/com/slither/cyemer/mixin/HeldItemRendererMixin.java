package com.slither.cyemer.mixin;

import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.implementation.render.HandCham;
import com.slither.cyemer.util.ModuleAccess;
import com.slither.cyemer.util.RenderBackendDetector;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11659;
import net.minecraft.class_1268;
import net.minecraft.class_1306;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
import net.minecraft.class_4587;
import net.minecraft.class_742;
import net.minecraft.class_759;
import net.minecraft.class_811;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({class_759.class})
public abstract class HeldItemRendererMixin {
    private static final ThreadLocal<class_1268> currentHand = new ThreadLocal<>();
    private static final ThreadLocal<class_1799> currentItem = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> handChamPushed = ThreadLocal.withInitial(() -> false);

    @ModifyVariable(
        method = {"method_3228(Lnet/minecraft/class_742;FFLnet/minecraft/class_1268;FLnet/minecraft/class_1799;FLnet/minecraft/class_4587;Lnet/minecraft/class_11659;I)V"},
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 3
    )
    private float modifyEquipProgress(float originalEquipProgress, class_742 player) {
        Module module = ModuleAccess.getModule("HitAnimations");
        boolean instantEquip = ModuleAccess.invokeBoolean(module, "isInstantEquipEnabled", false, null);
        return module != null && module.isEnabled() && instantEquip && !player.method_6115() ? 0.0F : originalEquipProgress;
    }

    @Inject(
        method = {"method_3217(Lnet/minecraft/class_4587;Lnet/minecraft/class_1306;F)V"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void onApplySwingOffset(class_4587 matrices, class_1306 arm, float swingProgress, CallbackInfo ci) {
        Module module = ModuleAccess.getModule("HitAnimations");
        if (module != null && module.isEnabled()) {
            class_1268 hand = currentHand.get();
            class_1799 stack = currentItem.get();
            if (hand == null || stack == null) {
                return;
            }

            boolean applied = ModuleAccess.invokeBoolean(
                module,
                "onRenderFirstPerson",
                false,
                new Class[]{class_4587.class, float.class, class_1799.class, class_1268.class},
                matrices,
                swingProgress,
                stack,
                hand
            );
            if (applied) {
                ci.cancel();
            }
        }
    }

    @Inject(
        method = {"method_3228(Lnet/minecraft/class_742;FFLnet/minecraft/class_1268;FLnet/minecraft/class_1799;FLnet/minecraft/class_4587;Lnet/minecraft/class_11659;I)V"},
        at = {@At("HEAD")}
    )
    private void onRenderFirstPersonItemHead(
        class_742 player,
        float tickDelta,
        float pitch,
        class_1268 hand,
        float swingProgress,
        class_1799 item,
        float equipProgress,
        class_4587 matrices,
        class_11659 queue,
        int light,
        CallbackInfo ci
    ) {
        currentHand.set(hand);
        currentItem.set(item);
        handChamPushed.set(false);
        Module module = ModuleAccess.getModule("ViewModel");
        String mode = ModuleAccess.invokeString(module, "getApplyToMode", "", null);
        if (module != null && module.isEnabled() && "Both".equals(mode) && hand == class_1268.field_5808) {
            matrices.method_22903();
            ModuleAccess.invoke(module, "applyTransformations", new Class[]{class_4587.class}, matrices);
        }

        HandCham handCham = HandCham.INSTANCE;
        if (handCham != null && handCham.shouldApplyFor(hand, item)) {
            matrices.method_22903();
            handCham.applyTransformations(matrices, hand, swingProgress);
            handChamPushed.set(true);
        }
    }

    @Inject(
        method = {"method_3228(Lnet/minecraft/class_742;FFLnet/minecraft/class_1268;FLnet/minecraft/class_1799;FLnet/minecraft/class_4587;Lnet/minecraft/class_11659;I)V"},
        at = {@At("TAIL")}
    )
    private void onRenderFirstPersonItemTail(
        class_742 player,
        float tickDelta,
        float pitch,
        class_1268 hand,
        float swingProgress,
        class_1799 item,
        float equipProgress,
        class_4587 matrices,
        class_11659 queue,
        int light,
        CallbackInfo ci
    ) {
        if (Boolean.TRUE.equals(handChamPushed.get())) {
            matrices.method_22909();
        }

        handChamPushed.remove();
        Module module = ModuleAccess.getModule("ViewModel");
        String mode = ModuleAccess.invokeString(module, "getApplyToMode", "", null);
        if (module != null && module.isEnabled() && "Both".equals(mode) && hand == class_1268.field_5808) {
            matrices.method_22909();
        }

        currentHand.remove();
        currentItem.remove();
    }

    @Inject(
        method = {"method_3233(Lnet/minecraft/class_1309;Lnet/minecraft/class_1799;Lnet/minecraft/class_811;Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;I)V"},
        at = {@At("HEAD")}
    )
    private void onRenderItemHead(class_1309 entity, class_1799 stack, class_811 renderMode, class_4587 matrices, class_11659 queue, int light, CallbackInfo ci) {
        Module module = ModuleAccess.getModule("ViewModel");
        String mode = ModuleAccess.invokeString(module, "getApplyToMode", "", null);
        if (module != null && module.isEnabled() && "Item Only".equals(mode) && currentHand.get() == class_1268.field_5808) {
            matrices.method_22903();
            ModuleAccess.invoke(module, "applyTransformations", new Class[]{class_4587.class}, matrices);
        }
    }

    @Inject(
        method = {"method_3233(Lnet/minecraft/class_1309;Lnet/minecraft/class_1799;Lnet/minecraft/class_811;Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;I)V"},
        at = {@At("TAIL")}
    )
    private void onRenderItemTail(class_1309 entity, class_1799 stack, class_811 renderMode, class_4587 matrices, class_11659 queue, int light, CallbackInfo ci) {
        Module module = ModuleAccess.getModule("ViewModel");
        String mode = ModuleAccess.invokeString(module, "getApplyToMode", "", null);
        if (module != null && module.isEnabled() && "Item Only".equals(mode) && currentHand.get() == class_1268.field_5808) {
            matrices.method_22909();
        }
    }

    @ModifyArg(
        method = {"method_3233(Lnet/minecraft/class_1309;Lnet/minecraft/class_1799;Lnet/minecraft/class_811;Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;I)V"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_10444;method_65604(Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;III)V"
        ),
        index = 4,
        require = 0
    )
    private int modifyHandChamOutlineColor(int originalOutlineColor) {
        if (RenderBackendDetector.isVulkanBackend()) {
            return originalOutlineColor;
        } else {
            class_1268 current = currentHand.get();
            class_1799 held = currentItem.get();
            HandCham handCham = HandCham.INSTANCE;
            return handCham != null && current != null && held != null && handCham.shouldApplyFor(current, held)
                ? handCham.getOutlineColor()
                : originalOutlineColor;
        }
    }
}
