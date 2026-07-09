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
    private final SliderSetting healThreshold = new SliderSetting("Heal Threshold", 0.625, 0.0, 1.0, 3);
    private final SliderSetting panicThreshold = new SliderSetting("Panic Threshold", 0.25, 0.0, 1.0, 3);
    private final SliderSetting rotationSpeed = new SliderSetting("Rotation Speed", 15.0, 1.0, 20.0, 1);
    private final SliderSetting throwGap = new SliderSetting("Throw Gap (ms)", 100.0, 0.0, 500.0, 0);
    private final SliderSetting cooldown = new SliderSetting("Cooldown (ms)", 400.0, 0.0, 2000.0, 0);
    private final BooleanSetting silent = new BooleanSetting("Silent", true);

    private State state = State.IDLE;
    private int potionSlot = -1;
    private int originalSlot = -1;
    private int throwsRemaining = 0;
    private long stateEnteredAt = 0L;

    public AutoPot() {
        super("AutoPot", "Look down and throw instant-health splash pots when low. Double-pots below panic threshold.", Category.COMBAT);
        this.addSetting(this.healThreshold);
        this.addSetting(this.panicThreshold);
        this.addSetting(this.rotationSpeed);
        this.addSetting(this.throwGap);
        this.addSetting(this.cooldown);
        this.addSetting(this.silent);
    }

    @Override
    public void onDisable() {
        this.cleanup();
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 == null || this.mc.field_1687 == null) {
            return;
        }

        float maxHp = this.mc.field_1724.method_6063();
        float hp = this.mc.field_1724.method_6032();
        float fraction = maxHp <= 0.0F ? 1.0F : hp / maxHp;

        switch (this.state) {
            case IDLE:
                if (fraction <= this.panicThreshold.getValue()) {
                    this.throwsRemaining = 2;
                } else if (fraction <= this.healThreshold.getValue()) {
                    this.throwsRemaining = 1;
                } else {
                    return;
                }

                this.potionSlot = this.findHealthPotSlot();
                if (this.potionSlot == -1) {
                    this.throwsRemaining = 0;
                    return;
                }

                this.originalSlot = this.mc.field_1724.method_31548().method_67532();
                this.startLookDown();
                this.changeState(State.AIMING);
                break;

            case AIMING:
                if (RotationManager.getFinalPitch() >= 85.0F) {
                    this.changeState(State.SWAPPING);
                }
                break;

            case SWAPPING:
                this.mc.field_1724.method_31548().method_61496(this.potionSlot);
                this.changeState(State.THROWING);
                break;

            case THROWING:
                ((MinecraftClientAccessor) this.mc).useItem();
                this.throwsRemaining--;
                this.changeState(State.THROW_GAP);
                break;

            case THROW_GAP:
                if (System.currentTimeMillis() - this.stateEnteredAt < (long) this.throwGap.getValue()) {
                    return;
                }
                if (this.throwsRemaining > 0) {
                    this.potionSlot = this.findHealthPotSlot();
                    if (this.potionSlot != -1) {
                        this.changeState(State.SWAPPING);
                        return;
                    }
                }
                this.changeState(State.CLEANUP);
                break;

            case CLEANUP:
                if (this.originalSlot != -1) {
                    this.mc.field_1724.method_31548().method_61496(this.originalSlot);
                }
                RotationManager.clearTarget(this);
                this.originalSlot = -1;
                this.potionSlot = -1;
                this.changeState(State.COOLDOWN);
                break;

            case COOLDOWN:
                if (System.currentTimeMillis() - this.stateEnteredAt >= (long) this.cooldown.getValue()) {
                    this.state = State.IDLE;
                }
                break;
        }
    }

    private void startLookDown() {
        RotationManager.setRotationSupplier(
                this,
                RotationManager.Priority.HIGH,
                () -> {
                    // Preserve current yaw and aim ~straight down. Small horizontal
                    // offset keeps the yaw math stable; large -Y drives pitch to ~89.
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

    private void changeState(State next) {
        this.state = next;
        this.stateEnteredAt = System.currentTimeMillis();
    }

    private void cleanup() {
        if (this.originalSlot != -1 && this.mc.field_1724 != null) {
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
        }
        RotationManager.clearTarget(this);
        this.state = State.IDLE;
        this.potionSlot = -1;
        this.originalSlot = -1;
        this.throwsRemaining = 0;
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
        AIMING,
        SWAPPING,
        THROWING,
        THROW_GAP,
        CLEANUP,
        COOLDOWN;
    }
}
