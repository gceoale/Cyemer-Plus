package com.slither.cyemer.module.implementation;

import com.slither.cyemer.mixin.SimpleOptionAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_3532;
import net.minecraft.class_7172;

@Environment(EnvType.CLIENT)
public class FullBright extends Module {
    private final BooleanSetting fade = new BooleanSetting("Fade", false);
    private double originalGamma = 1.0;

    public FullBright() {
        super("FullBright", "Sets brightness to a very high number", Category.RENDER);
        this.addSetting(this.fade);
    }

    @Override
    public void onEnable() {
        if (this.mc.field_1690 != null) {
            this.originalGamma = (Double)this.mc.field_1690.method_42473().method_41753();
        }
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onTick() {
        if (this.mc.field_1690 != null) {
            double targetGamma = this.isEnabled() ? 16.0 : this.originalGamma;
            double currentGamma = (Double)this.mc.field_1690.method_42473().method_41753();
            if (!(Math.abs(currentGamma - targetGamma) < 0.01)) {
                if (this.fade.isEnabled()) {
                    this.forceSetGamma(class_3532.method_16436(0.2, currentGamma, targetGamma));
                } else {
                    this.forceSetGamma(targetGamma);
                }
            }
        }
    }

    private void forceSetGamma(double gamma) {
        class_7172<Double> gammaOption = this.mc.field_1690.method_42473();
        ((SimpleOptionAccessor)(Object)gammaOption).forceSetValue(gamma);
    }
}
