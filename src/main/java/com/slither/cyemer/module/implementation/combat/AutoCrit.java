package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.event.EventBus;
import com.slither.cyemer.event.EventTarget;
import com.slither.cyemer.event.impl.TriggerBotReadyEvent;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1309;
import net.minecraft.class_3966;

@Environment(EnvType.CLIENT)
public class AutoCrit extends Module {
    private final BooleanSetting triggerbotSync = new BooleanSetting("Triggerbot Sync", true);
    private boolean simulatedJump = false;

    public AutoCrit() {
        super("AutoCrit", "Automatically jumps before an attack to inflict crits.", Category.COMBAT);
        this.addSetting(this.triggerbotSync);
    }

    @Override
    public void onEnable() {
        EventBus.register(this);
        this.simulatedJump = false;
    }

    @Override
    public void onDisable() {
        EventBus.unregister(this);
        this.releaseJump();
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            if (this.simulatedJump && !this.mc.field_1724.method_24828()) {
                this.releaseJump();
            }

            if (!this.triggerbotSync.isEnabled()) {
                if (this.mc.field_1724.method_24828() && !this.mc.field_1724.method_6115() && !this.mc.field_1690.field_1903.method_1434()) {
                    if (this.mc.field_1724.method_7261(0.5F) >= 1.0F
                        && this.mc.field_1765 instanceof class_3966 entityHit
                        && entityHit.method_17782() instanceof class_1309 target
                        && target.method_5805()
                        && target.method_6032() > 0.0F) {
                        this.doJump();
                    }
                }
            }
        }
    }

    @EventTarget
    public void onTriggerBotReady(TriggerBotReadyEvent event) {
        if (this.isEnabled() && this.triggerbotSync.isEnabled() && this.mc.field_1724 != null) {
            if (this.mc.field_1724.method_24828() && !this.mc.field_1724.method_6115() && !this.mc.field_1690.field_1903.method_1434()) {
                this.doJump();
                event.setCancelled(true);
            }
        }
    }

    private void doJump() {
        if (!this.mc.field_1690.field_1903.method_1434()) {
            this.mc.field_1690.field_1903.method_23481(true);
            this.simulatedJump = true;
        }
    }

    private void releaseJump() {
        if (this.simulatedJump) {
            this.mc.field_1690.field_1903.method_23481(false);
            this.simulatedJump = false;
        }
    }
}
