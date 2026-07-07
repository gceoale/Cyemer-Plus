package com.slither.cyemer.mixin;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.hud.TotemPopManager;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.ModuleAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_2663;
import net.minecraft.class_2664;
import net.minecraft.class_2743;
import net.minecraft.class_310;
import net.minecraft.class_634;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({class_634.class})
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(
        method = {"method_11148(Lnet/minecraft/class_2663;)V"},
        at = {@At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_2663;method_11469(Lnet/minecraft/class_1937;)Lnet/minecraft/class_1297;"
        )}
    )
    private void onEntityStatus(class_2663 packet, CallbackInfo ci) {
        class_310 client = class_310.method_1551();
        if (client.field_1687 != null) {
            class_1297 entity = packet.method_11469(client.field_1687);
            if (packet.method_11470() == 35 && entity instanceof class_1657) {
                TotemPopManager.getInstance().onTotemPop((class_1657)entity);
            }

            Module autoShieldBreak = ModuleAccess.getModule("AutoShieldBreak");
            if (autoShieldBreak != null && autoShieldBreak.isEnabled() && packet.method_11470() == 30 && entity instanceof class_1657 player) {
                ModuleAccess.invoke(autoShieldBreak, "breakConfirmed", new Class[]{class_1657.class}, player);
            }
        }
    }

    @Inject(
        method = {"method_11124(Lnet/minecraft/class_2664;)V"},
        at = {@At("HEAD")}
    )
    private void onExplosion(class_2664 packet, CallbackInfo ci) {
        Module fakelag = ModuleAccess.getModule("Fakelag");
        if (fakelag != null && fakelag.isEnabled()) {
            ModuleAccess.invoke(fakelag, "sendQueuedPackets", null);
        }
    }

    @Inject(
        method = {"method_11132(Lnet/minecraft/class_2743;)V"},
        at = {@At("HEAD")}
    )
    private void onEntityVelocityUpdate(class_2743 packet, CallbackInfo ci) {
        if (class_310.method_1551().field_1724 != null && class_310.method_1551().field_1687 != null) {
            if (packet.method_11818() == class_310.method_1551().field_1724.method_5628()) {
                Module autoJumpReset = Cyemer.INSTANCE.getModuleManager().getModule("AutoJumpReset");
                if (autoJumpReset != null && autoJumpReset.isEnabled()) {
                    ModuleAccess.invoke(autoJumpReset, "onVelocityPacket", null);
                }
            }
        }
    }
}
