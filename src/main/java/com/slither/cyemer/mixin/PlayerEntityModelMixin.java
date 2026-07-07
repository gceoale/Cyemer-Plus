package com.slither.cyemer.mixin;

import com.slither.cyemer.util.RotationManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10055;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_572;
import net.minecraft.class_591;
import net.minecraft.class_630;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({class_591.class})
public class PlayerEntityModelMixin extends class_572<class_10055> {
    public PlayerEntityModelMixin(class_630 root) {
        super(root);
    }

    @Inject(
        method = {"method_62110(Lnet/minecraft/class_10055;)V"},
        at = {@At("TAIL")}
    )
    private void onSetAngles(class_10055 state, CallbackInfo ci) {
        if (RotationManager.isSilentRotationActive() && !class_310.method_1551().field_1690.method_31044().method_31034()) {
            boolean isLocalPlayer = class_310.method_1551().field_1724 != null && state.field_53520.equals(class_310.method_1551().field_1724.method_52814());
            if (isLocalPlayer) {
                float bodyYaw = state.field_53446;
                float yawOffset = class_3532.method_15393(RotationManager.getFinalYaw() - bodyYaw);
                this.field_3398.field_3675 = (float)Math.toRadians(yawOffset);
                this.field_3398.field_3654 = (float)Math.toRadians(RotationManager.getFinalPitch());
            }
        }
    }
}
