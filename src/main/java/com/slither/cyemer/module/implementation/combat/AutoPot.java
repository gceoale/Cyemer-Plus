package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.ModuleRandomDelay;
import com.slither.cyemer.util.RotationManager;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1842;
import net.minecraft.class_1844;
import net.minecraft.class_1847;
import net.minecraft.class_6880;
import net.minecraft.class_9334;

@Environment(EnvType.CLIENT)
public class AutoPot extends Module {
    private final SliderSetting health = new SliderSetting("Health", 10.0, 1.0, 20.0, 0);
    private final SliderSetting potToHealth = new SliderSetting("Pot To Health", 18.0, 1.0, 20.0, 0);
    private final SliderSetting strength = new SliderSetting("Strength", 10.0, 1.0, 20.0, 1);
    private final ModeSetting rotPattern = new ModeSetting("Pattern", "Sine", "Smooth", "Linear", "Instant");
    private final SliderSetting rotRandom = new SliderSetting("Randomness", 0.1, 0.0, 1.0, 2);
    private final BooleanSetting silentRotation = new BooleanSetting("Silent", true);
    private final BooleanSetting randomization = new BooleanSetting("Randomization", false);
    private final SliderSetting randomMinDelay = new SliderSetting("Random Min (ms)", 0.0, 0.0, 500.0, 0);
    private final SliderSetting randomMaxDelay = new SliderSetting("Random Max (ms)", 25.0, 0.0, 500.0, 0);
    private AutoPot.State currentState = AutoPot.State.IDLE;
    private int potionSlot = -1;
    private int originalSlot = -1;

    public AutoPot() {
        super("AutoPot", "Automatically throws health potions.", Category.COMBAT);
        this.addSetting(this.health);
        this.addSetting(this.potToHealth);
        this.addSetting(this.strength);
        this.addSetting(this.rotPattern);
        this.addSetting(this.rotRandom);
        this.addSetting(this.silentRotation);
        this.addSetting(this.randomization);
        this.addSetting(this.randomMinDelay);
        this.addSetting(this.randomMaxDelay);
    }

    @Override
    public void onEnable() {
        this.reset();
    }

    @Override
    public void onDisable() {
        if (this.mc.field_1690 != null) {
            this.gateUsePress(false);
            this.mc.field_1690.field_1904.method_23481(false);
        }

        if (this.originalSlot != -1 && this.mc.field_1724 != null) {
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
        }

        RotationManager.clearTarget(this);
        this.reset();
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null) {
            switch (this.currentState) {
                case IDLE:
                    if (this.mc.field_1724.method_6032() <= this.health.getPreciseValue()) {
                        this.potionSlot = this.findSplashPotion();
                        if (this.potionSlot != -1) {
                            this.originalSlot = this.mc.field_1724.method_31548().method_67532();
                            RotationManager.setRotationSupplier(this, RotationManager.Priority.HIGH, () -> {
                                float yaw = this.mc.field_1724.method_36454();
                                double rad = Math.toRadians(yaw);
                                double dx = -Math.sin(rad) * 0.1;
                                double dz = Math.cos(rad) * 0.1;
                                return this.mc.field_1724.method_73189().method_1031(dx, -5.0, dz);
                            }, this.strength.getValue(), this.getRotationMode(), 0.0, this.silentRotation.isEnabled(), false);
                            this.currentState = AutoPot.State.ROTATING;
                        }
                    }
                    break;
                case ROTATING:
                    if (RotationManager.getFinalPitch() >= 85.0F) {
                        this.currentState = AutoPot.State.SWAPPING;
                    }
                    break;
                case SWAPPING:
                    this.mc.field_1724.method_31548().method_61496(this.potionSlot);
                    this.currentState = AutoPot.State.PRESSING;
                    break;
                case PRESSING:
                    if (this.gateUsePress(true)) {
                        this.mc.field_1690.field_1904.method_23481(true);
                        this.currentState = AutoPot.State.RELEASING;
                    }
                    break;
                case RELEASING:
                    this.gateUsePress(false);
                    this.mc.field_1690.field_1904.method_23481(false);
                    this.mc.field_1724.method_31548().method_61496(this.originalSlot);
                    RotationManager.clearTarget(this);
                    this.currentState = AutoPot.State.COOLDOWN;
                    break;
                case COOLDOWN:
                    if (this.mc.field_1724.method_6032() > this.potToHealth.getPreciseValue()
                        || this.mc.field_1724.method_6032() <= this.health.getPreciseValue()) {
                        this.reset();
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

    private int findSplashPotion() {
        for (int i = 0; i < 9; i++) {
            class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
            if (stack.method_7909() == class_1802.field_8436) {
                class_1844 potionContents = (class_1844)stack.method_58694(class_9334.field_49651);
                if (potionContents != null) {
                    Optional<class_6880<class_1842>> potionEntryOptional = potionContents.comp_2378();
                    if (potionEntryOptional.isPresent()) {
                        class_6880<class_1842> potionEntry = potionEntryOptional.get();
                        if (potionEntry.method_55838(class_1847.field_8963) || potionEntry.method_55838(class_1847.field_8980)) {
                            return i;
                        }
                    }
                }
            }
        }

        return -1;
    }

    private void reset() {
        this.currentState = AutoPot.State.IDLE;
        this.potionSlot = -1;
        this.originalSlot = -1;
    }

    private long getRandomMinDelay() {
        return (long)this.randomMinDelay.getValue();
    }

    private long getRandomMaxDelay() {
        return (long)this.randomMaxDelay.getValue();
    }

    private boolean gateUsePress(boolean desiredState) {
        return !this.randomization.isEnabled()
            ? desiredState
            : ModuleRandomDelay.gatePress("combat.use.autopot", desiredState, this.getRandomMinDelay(), this.getRandomMaxDelay());
    }

    @Environment(EnvType.CLIENT)
    private static enum State {
        IDLE,
        ROTATING,
        SWAPPING,
        PRESSING,
        RELEASING,
        COOLDOWN;
    }
}
