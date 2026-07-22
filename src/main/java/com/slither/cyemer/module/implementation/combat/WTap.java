package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.event.EventBus;
import com.slither.cyemer.event.impl.ShieldDrainEvent;
import com.slither.cyemer.mixin.KeyBindingAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_3675;

@Environment(EnvType.CLIENT)
public class WTap extends Module {
    private final SliderSetting wtapDuration = new SliderSetting("WTap Duration (ms)", 50.0, 10.0, 1000.0, 0);
    private final BooleanSetting onlyOnGround = new BooleanSetting("Only On Ground", false);
    private boolean isTapping = false;
    private long tapEndTimestamp = 0L;
    private boolean wasWPressedBeforeTap = false;

    public WTap() {
        super("WTap", "Easily combo players (strafe for an even bigger advantage)", Category.COMBAT);
        this.addSetting(this.wtapDuration);
        this.addSetting(this.onlyOnGround);
    }

    public void onAttack() {
        if (this.isEnabled() && this.mc.field_1724 != null && !this.isTapping) {
            ShieldDrainEvent drainEvent = new ShieldDrainEvent();
            EventBus.post(drainEvent);
            if (!drainEvent.isActive()) {
                if (!this.onlyOnGround.isEnabled() || this.mc.field_1724.method_24828()) {
                    if (this.isUserPressingForward()) {
                        this.isTapping = true;
                        this.wasWPressedBeforeTap = true;
                        this.tapEndTimestamp = System.currentTimeMillis() + (long)this.wtapDuration.getValue();
                        this.mc.field_1690.field_1894.method_23481(false);
                    }
                }
            }
        }
    }

    @Override
    public void onTick() {
        if (this.isTapping) {
            ShieldDrainEvent drainEvent = new ShieldDrainEvent();
            EventBus.post(drainEvent);
            if (!drainEvent.isActive()) {
                long currentTime = System.currentTimeMillis();
                if (currentTime >= this.tapEndTimestamp) {
                    if (this.wasWPressedBeforeTap && this.isUserPressingForward()) {
                        this.mc.field_1690.field_1894.method_23481(true);
                    }

                    this.isTapping = false;
                }
            }
        }
    }

    @Override
    public void onDisable() {
        if (this.isTapping && this.wasWPressedBeforeTap && this.isUserPressingForward()) {
            this.mc.field_1690.field_1894.method_23481(true);
        }

        this.isTapping = false;
    }

    private boolean isUserPressingForward() {
        int code = ((KeyBindingAccessor)this.mc.field_1690.field_1894).getBoundKey().method_1444();
        return class_3675.method_15987(this.mc.method_22683(), code);
    }
}
