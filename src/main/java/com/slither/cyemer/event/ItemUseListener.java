package com.slither.cyemer.event;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.module.Module;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_239;
import net.minecraft.class_3965;

@Environment(EnvType.CLIENT)
public class ItemUseListener {
    public static void onItemUse(class_239 hitResult) {
        if (hitResult instanceof class_3965 blockHitResult) {
            try {
                Module autoAnchor = Cyemer.getInstance().getModuleManager().getModule("AutoAnchor");
                if (autoAnchor != null && autoAnchor.isEnabled()) {
                    autoAnchor.getClass().getMethod("onItemUse", class_3965.class).invoke(autoAnchor, blockHitResult);
                }

                Module autoDrain = Cyemer.getInstance().getModuleManager().getModule("AutoDrain");
                if (autoDrain != null && autoDrain.isEnabled()) {
                    autoDrain.getClass().getMethod("onItemUse", class_3965.class).invoke(autoDrain, blockHitResult);
                }

                Module anchorMacro = Cyemer.getInstance().getModuleManager().getModule("AnchorMacro");
                if (anchorMacro != null && anchorMacro.isEnabled()) {
                    anchorMacro.getClass().getMethod("onItemUse", class_3965.class).invoke(anchorMacro, blockHitResult);
                }
            } catch (Exception var5) {
            }
        }
    }
}
