package com.slither.cyemer.mixin;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.event.ItemUseListener;
import com.slither.cyemer.hud.HUDManager;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.ModuleAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_3532;
import net.minecraft.class_3965;
import net.minecraft.class_636;
import net.minecraft.class_746;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({class_636.class})
public class ClientPlayerInteractionManagerMixin {
    @Inject(
        method = {"method_2918(Lnet/minecraft/class_1657;Lnet/minecraft/class_1297;)V"},
        at = {@At("HEAD")}
    )
    private void onAttackEntity(class_1657 player, class_1297 target, CallbackInfo ci) {
        if (player != null && target != null) {
            class_243 eyePos = player.method_33571();
            class_238 box = target.method_5829();
            double cx = class_3532.method_15350(eyePos.field_1352, box.field_1323, box.field_1320);
            double cy = class_3532.method_15350(eyePos.field_1351, box.field_1322, box.field_1325);
            double cz = class_3532.method_15350(eyePos.field_1350, box.field_1321, box.field_1324);
            double reach = eyePos.method_1022(new class_243(cx, cy, cz));
            HUDManager.updateLastHitReach(reach);
        }

        Module wTapModule = Cyemer.getInstance().getModuleManager().getModule("WTap");
        if (wTapModule != null && wTapModule.isEnabled()) {
            ModuleAccess.invoke(wTapModule, "onAttack", null);
        }
    }

    @Inject(
        method = {"method_2896(Lnet/minecraft/class_746;Lnet/minecraft/class_1268;Lnet/minecraft/class_3965;)Lnet/minecraft/class_1269;"},
        at = {@At("HEAD")}
    )
    private void onInteractBlock(class_746 player, class_1268 hand, class_3965 hitResult, CallbackInfoReturnable<class_1269> cir) {
        ItemUseListener.onItemUse(hitResult);
    }

    @Environment(EnvType.CLIENT)
    @Mixin({class_636.class})
    public interface ClientPlayerInteractionManagerAccessor {
        @Mutable
        @Accessor("field_3716")
        void setBlockBreakingCooldown(int var1);
    }
}
