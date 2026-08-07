package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.AttackValidator;
import com.slither.cyemer.util.RotationManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_243;
import net.minecraft.class_3966;
import net.minecraft.class_9236;

/**
 * Deflects wind charges thrown at you by other players. Scans for
 * AbstractWindChargeEntity within Range, filters out any charges owned by
 * you or your own vehicle, aims at the closest one via RotationManager, and
 * once the crosshair intersects the charge's hitbox it triggers a vanilla
 * attack (which knocks the charge back toward its sender). No custom
 * packets - just aim + click through the normal attack path.
 */
@Environment(EnvType.CLIENT)
public class WindChargeHitter extends Module {
    private final SliderSetting range = new SliderSetting("Range", 3.5, 2.0, 4.5, 1);
    private final SliderSetting aimSpeed = new SliderSetting("Aim Speed", 22.0, 1.0, 35.0, 1);
    private final BooleanSetting silent = new BooleanSetting("Silent Aim", false);
    private final BooleanSetting visibleOnly = new BooleanSetting("Visible Only", true);
    private final BooleanSetting predictMotion = new BooleanSetting("Predict Motion", true);

    private class_9236 targetCharge = null;

    public WindChargeHitter() {
        super("WindChargeHitter", "Deflects incoming wind charges thrown by other players.", Category.COMBAT);
        this.addSetting(this.range);
        this.addSetting(this.aimSpeed);
        this.addSetting(this.silent);
        this.addSetting(this.visibleOnly);
        this.addSetting(this.predictMotion);
    }

    @Override
    public void onDisable() {
        RotationManager.stop(this);
        this.targetCharge = null;
    }

    @Override
    public void onTick() {
        if (!this.isEnabled() || this.mc.field_1724 == null || this.mc.field_1687 == null) {
            RotationManager.stop(this);
            this.targetCharge = null;
            return;
        }

        this.targetCharge = this.findTargetCharge();
        if (this.targetCharge == null) {
            RotationManager.stop(this);
            return;
        }

        RotationManager.setRotationSupplier(
            this,
            RotationManager.Priority.HIGH,
            this::computeAimPoint,
            this.aimSpeed.getValue(),
            RotationManager.RotationMode.LINEAR,
            0.0,
            this.silent.isEnabled(),
            false
        );

        if (this.mc.field_1765 instanceof class_3966 ehr && ehr.method_17782() == this.targetCharge) {
            AttackValidator.tryAttack(this.mc, "combat.attack.windchargehitter");
        }
    }

    private class_243 computeAimPoint() {
        if (this.targetCharge == null || !this.targetCharge.method_5805()) return null;
        class_243 center = this.targetCharge.method_5829().method_1005();
        if (!this.predictMotion.isEnabled()) return center;
        // Wind charges move ~1.5 blocks/tick. At sub-3-block reach, half a tick
        // of lead lines the crosshair up with where the charge will be when the
        // rotation packet actually arrives at server. More than that overshoots.
        class_243 vel = this.targetCharge.method_18798();
        return center.method_1031(vel.field_1352 * 0.5, vel.field_1351 * 0.5, vel.field_1350 * 0.5);
    }

    private class_9236 findTargetCharge() {
        class_9236 best = null;
        double bestDist = this.range.getValue();
        double rangeVal = this.range.getValue();

        for (class_1297 entity : this.mc.field_1687.method_18112()) {
            if (!(entity instanceof class_9236 charge)) continue;
            if (!charge.method_5805()) continue;
            if (this.isOwnedByPlayer(charge)) continue;
            double dist = this.mc.field_1724.method_5739(charge);
            if (dist > rangeVal) continue;
            if (this.visibleOnly.isEnabled() && !this.mc.field_1724.method_6057(charge)) continue;
            if (dist < bestDist) {
                bestDist = dist;
                best = charge;
            }
        }
        return best;
    }

    private boolean isOwnedByPlayer(class_9236 charge) {
        class_1297 owner = charge.method_24921();
        if (owner == null) return false;
        if (this.mc.field_1724 == null) return false;
        if (owner == this.mc.field_1724) return true;
        return owner.method_5667().equals(this.mc.field_1724.method_5667());
    }
}
