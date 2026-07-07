package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.mixin.KeyBindingAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.RotationManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_4587;

@Environment(EnvType.CLIENT)
public class AutoWindCharge extends Module {
    private final SliderSetting strength = new SliderSetting("Rot Speed", 12.0, 1.0, 20.0, 1);
    private final ModeSetting rotPattern = new ModeSetting("Pattern", "Sine", "Smooth", "Instant");
    private final SliderSetting rotRandom = new SliderSetting("Randomness", 0.05, 0.0, 1.0, 2);
    private final BooleanSetting silentRotations = new BooleanSetting("Silent", true);
    private final SliderSetting preparationDelay = new SliderSetting("Prep Delay", 0.0, 0.0, 1000.0, 10);
    private final BooleanSetting autoJump = new BooleanSetting("Auto Jump", true);
    private final SliderSetting jumpDelay = new SliderSetting("Jump Delay", 150.0, 0.0, 500.0, 10);
    private final BooleanSetting autoCrouch = new BooleanSetting("Auto Crouch", true);
    private final SliderSetting crouchDelay = new SliderSetting("Crouch Delay", 150.0, 0.0, 500.0, 10);
    private AutoWindCharge.State currentState = AutoWindCharge.State.SEARCHING;
    private int chargeSlot = -1;
    private int originalSlot = -1;
    private long throwTime = 0L;
    private long enableTime = 0L;

    public AutoWindCharge() {
        super("AutoWindCharge", "Performs mace wind charge jumps.", Category.COMBAT);
        this.addSetting(this.strength);
        this.addSetting(this.rotPattern);
        this.addSetting(this.rotRandom);
        this.addSetting(this.silentRotations);
        this.addSetting(this.preparationDelay);
        this.addSetting(this.autoJump);
        this.addSetting(this.jumpDelay);
        this.addSetting(this.autoCrouch);
        this.addSetting(this.crouchDelay);
    }

    @Override
    public void onEnable() {
        this.currentState = AutoWindCharge.State.SEARCHING;
        this.chargeSlot = -1;
        this.originalSlot = -1;
        this.throwTime = 0L;
        this.enableTime = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        if (this.mc.field_1690 != null) {
            this.mc.field_1690.field_1904.method_23481(false);
            this.mc.field_1690.field_1903.method_23481(false);
            this.mc.field_1690.field_1832.method_23481(false);
        }

        if (this.originalSlot != -1 && this.mc.field_1724 != null) {
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
        }

        RotationManager.clearTarget(this);
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            switch (this.currentState) {
                case SEARCHING:
                    this.chargeSlot = this.findWindCharge();
                    if (this.chargeSlot == -1) {
                        this.toggle();
                        return;
                    }

                    this.originalSlot = this.mc.field_1724.method_31548().method_67532();
                    RotationManager.setRotationSupplier(this, RotationManager.Priority.HIGHEST, () -> {
                        float yaw = RotationManager.getVisualYaw();
                        double rad = Math.toRadians(yaw);
                        double dx = -Math.sin(rad) * 0.1;
                        double dz = Math.cos(rad) * 0.1;
                        return this.mc.field_1724.method_73189().method_1031(dx, -5.0, dz);
                    }, this.strength.getValue(), this.getRotationMode(), 0.0, this.silentRotations.isEnabled(), false);
                    this.currentState = AutoWindCharge.State.ROTATING;
                    break;
                case CLEANUP:
                    this.mc.field_1724.method_31548().method_61496(this.originalSlot);
                    RotationManager.clearTarget(this);
                    this.mc.field_1690.field_1903.method_23481(false);
                    this.mc.field_1690.field_1832.method_23481(false);
                    this.currentState = AutoWindCharge.State.FINISHING;
                    break;
                case FINISHING:
                    this.toggle();
            }
        } else {
            this.toggle();
        }
    }

    @Override
    public void onWorldRender(class_4587 matrices, float tickDelta) {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            switch (this.currentState) {
                case ROTATING:
                    boolean isLookingDown = RotationManager.getFinalPitch() >= 88.0F;
                    boolean isDelayPassed = System.currentTimeMillis() - this.enableTime >= this.preparationDelay.getValue();
                    if (isLookingDown && isDelayPassed) {
                        this.currentState = AutoWindCharge.State.SWAPPING;
                    }
                    break;
                case SWAPPING:
                    this.mc.field_1724.method_31548().method_61496(this.chargeSlot);
                    this.currentState = AutoWindCharge.State.THROWING;
                    break;
                case THROWING:
                    KeyBindingAccessor useKey = (KeyBindingAccessor)this.mc.field_1690.field_1904;
                    useKey.setTimesPressed(useKey.getTimesPressed() + 1);
                    this.throwTime = System.currentTimeMillis();
                    this.currentState = AutoWindCharge.State.WAITING_FOR_ACTION;
                    break;
                case WAITING_FOR_ACTION:
                    this.currentState = AutoWindCharge.State.EXECUTING_ACTION;
                    break;
                case EXECUTING_ACTION:
                    long elapsed = System.currentTimeMillis() - this.throwTime;
                    if (this.autoJump.isEnabled() && elapsed >= this.jumpDelay.getValue() && this.mc.field_1724.method_24828()) {
                        this.mc.field_1690.field_1903.method_23481(true);
                    }

                    if (this.autoCrouch.isEnabled() && elapsed >= this.crouchDelay.getValue()) {
                        this.mc.field_1690.field_1832.method_23481(true);
                    }

                    double maxDelay = 0.0;
                    if (this.autoJump.isEnabled()) {
                        maxDelay = Math.max(maxDelay, this.jumpDelay.getValue());
                    }

                    if (this.autoCrouch.isEnabled()) {
                        maxDelay = Math.max(maxDelay, this.crouchDelay.getValue());
                    }

                    if (elapsed > maxDelay + 50.0) {
                        this.currentState = AutoWindCharge.State.CLEANUP;
                    }
            }
        }
    }

    private RotationManager.RotationMode getRotationMode() {
        try {
            return RotationManager.RotationMode.valueOf(this.rotPattern.getCurrentMode().toUpperCase());
        } catch (Exception var2) {
            return RotationManager.RotationMode.SINE;
        }
    }

    private int findWindCharge() {
        for (int i = 0; i < 9; i++) {
            class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
            if (stack.method_7909() == class_1802.field_49098) {
                return i;
            }
        }

        return -1;
    }

    @Environment(EnvType.CLIENT)
    private static enum State {
        SEARCHING,
        ROTATING,
        SWAPPING,
        THROWING,
        WAITING_FOR_ACTION,
        EXECUTING_ACTION,
        CLEANUP,
        FINISHING;
    }
}
