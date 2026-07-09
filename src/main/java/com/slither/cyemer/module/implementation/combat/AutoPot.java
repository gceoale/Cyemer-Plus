package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.mixin.MinecraftClientAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.RotationManager;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1842;
import net.minecraft.class_1844;
import net.minecraft.class_1847;
import net.minecraft.class_243;
import net.minecraft.class_6880;
import net.minecraft.class_9334;

@Environment(EnvType.CLIENT)
public class AutoPot extends Module {
    private static final float PITCH_THRESHOLD = 85.0F;
    private static final float HEAL_MARGIN = 0.5F;

    private final SliderSetting threshold = new SliderSetting("Threshold", 0.625, 0.0, 1.0, 3);
    private final SliderSetting rotationSpeed = new SliderSetting("Rotation Speed", 20.0, 1.0, 20.0, 1);
    private final SliderSetting throwGap = new SliderSetting("Throw Gap (ms)", 50.0, 0.0, 500.0, 0);
    private final SliderSetting cooldownMax = new SliderSetting("Cooldown Max (ms)", 1500.0, 100.0, 3000.0, 0);
    private final BooleanSetting silent = new BooleanSetting("Silent", true);

    private State state = State.IDLE;
    private int originalSlot = -1;
    private int throwsRemaining = 0;
    private long lastThrowTime = 0L;
    private long cooldownStartedAt = 0L;
    private float hpAtLastThrow = 0.0F;

    public AutoPot() {
        super("AutoPot", "FPS lookdown, throws two instant-health splash pots when hp drops below the threshold.", Category.COMBAT);
        this.addSetting(this.threshold);
        this.addSetting(this.rotationSpeed);
        this.addSetting(this.throwGap);
        this.addSetting(this.cooldownMax);
        this.addSetting(this.silent);
    }

    @Override
    public void onDisable() {
        this.cleanup();
        this.state = State.IDLE;
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 == null || this.mc.field_1687 == null) {
            return;
        }

        float maxHp = this.mc.field_1724.method_6063();
        float hp = this.mc.field_1724.method_6032();
        float fraction = maxHp <= 0.0F ? 1.0F : hp / maxHp;
        long now = System.currentTimeMillis();

        switch (this.state) {
            case IDLE:
                if (fraction > this.threshold.getValue()) {
                    return;
                }
                if (this.findHealthPotSlot() == -1) {
                    return;
                }
                this.throwsRemaining = 2;
                this.originalSlot = this.mc.field_1724.method_31548().method_67532();
                this.startLookDown();
                this.lastThrowTime = 0L;
                this.state = State.ACTIVE;
                break;

            case ACTIVE:
                int slot = this.throwsRemaining > 0 ? this.findHealthPotSlot() : -1;
                boolean gapElapsed = now - this.lastThrowTime >= (long) this.throwGap.getValue();
                boolean pitchReady = RotationManager.getFinalPitch() >= PITCH_THRESHOLD;

                if (slot != -1 && gapElapsed && pitchReady) {
                    this.hpAtLastThrow = hp;
                    this.mc.field_1724.method_31548().method_61496(slot);
                    ((MinecraftClientAccessor) this.mc).useItem();
                    this.throwsRemaining--;
                    this.lastThrowTime = now;
                    break;
                }

                if (this.throwsRemaining == 0 || slot == -1) {
                    this.finishSequence(now);
                }
                break;

            case COOLDOWN:
                float healed = hp - this.hpAtLastThrow;
                boolean recovered = healed >= HEAL_MARGIN;
                boolean timedOut = now - this.cooldownStartedAt >= (long) this.cooldownMax.getValue();
                if (recovered || timedOut) {
                    this.state = State.IDLE;
                }
                break;
        }
    }

    private void finishSequence(long now) {
        if (this.originalSlot != -1) {
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
            this.originalSlot = -1;
        }
        RotationManager.clearTarget(this);
        this.cooldownStartedAt = now;
        this.state = State.COOLDOWN;
    }

    private void startLookDown() {
        RotationManager.setRotationSupplier(
                this,
                RotationManager.Priority.HIGH,
                () -> {
                    float yaw = this.mc.field_1724.method_36454();
                    double rad = Math.toRadians(yaw);
                    double dx = -Math.sin(rad) * 0.05;
                    double dz = Math.cos(rad) * 0.05;
                    class_243 eye = this.mc.field_1724.method_33571();
                    return eye.method_1031(dx, -20.0, dz);
                },
                this.rotationSpeed.getValue(),
                RotationManager.RotationMode.FPS,
                0.0,
                this.silent.isEnabled(),
                false);
    }

    private void cleanup() {
        if (this.originalSlot != -1 && this.mc.field_1724 != null) {
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
        }
        RotationManager.clearTarget(this);
        this.originalSlot = -1;
        this.throwsRemaining = 0;
        this.lastThrowTime = 0L;
    }

    private int findHealthPotSlot() {
        for (int i = 0; i < 9; i++) {
            class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
            if (this.isHealthPot(stack)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isHealthPot(class_1799 stack) {
        if (stack == null || stack.method_7960() || stack.method_7909() != class_1802.field_8436) {
            return false;
        }
        class_1844 contents = stack.method_58694(class_9334.field_49651);
        if (contents == null) {
            return false;
        }
        Optional<class_6880<class_1842>> entry = contents.comp_2378();
        if (!entry.isPresent()) {
            return false;
        }
        class_6880<class_1842> potion = entry.get();
        return potion.method_55838(class_1847.field_8963) || potion.method_55838(class_1847.field_8980);
    }

    @Environment(EnvType.CLIENT)
    private static enum State {
        IDLE,
        ACTIVE,
        COOLDOWN;
    }
}
