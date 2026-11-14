package com.slither.cyemer.mixin;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.module.implementation.donutsmp.FarmFinder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_638;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({class_638.class})
public class ClientWorldMixin {
    /**
     * ClientWorld.handleBlockUpdate. Both the single-block and the chunk-delta
     * packet handlers funnel every server block change through it, after their
     * main-thread handoff, so this sees each change exactly once and on the
     * client thread. At HEAD the old state is still in the world, which is what
     * lets FarmFinder tell a real state change apart from a placement.
     *
     * require = 0 so that if a future mapping moves this method the hook simply
     * does not apply, instead of taking the whole game down at startup.
     */
    @Inject(
        method = {"method_41928(Lnet/minecraft/class_2338;Lnet/minecraft/class_2680;I)V"},
        at = {@At("HEAD")},
        require = 0
    )
    private void onHandleBlockUpdate(class_2338 pos, class_2680 state, int flags, CallbackInfo ci) {
        if (Cyemer.selfDestructed) {
            return;
        }
        FarmFinder finder = FarmFinder.getInstance();
        if (finder != null && finder.isEnabled()) {
            class_638 world = (class_638)(Object)this;
            finder.onBlockUpdate(pos, world.method_8320(pos), state);
        }
    }
}
