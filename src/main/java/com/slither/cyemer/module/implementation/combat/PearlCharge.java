package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.mixin.KeyBindingAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.PlaceValidator;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_332;

@Environment(EnvType.CLIENT)
public class PearlCharge extends Module {
    private static final long CLICK_BURST_GAP_MS = 18L;
    private final BooleanSetting swapBack = new BooleanSetting("Swap Back", true);
    private final SliderSetting swapDelay = new SliderSetting("Swap Delay", 76.76, 0.0, 600.0, 2);
    private final BooleanSetting randomization = new BooleanSetting("Randomization", false);
    private final SliderSetting minRandom = new SliderSetting("Min Random", 5.0, 0.0, 300.0, 0);
    private final SliderSetting maxRandom = new SliderSetting("Max Random", 25.0, 0.0, 300.0, 0);
    private final Random random = new Random();
    private PearlCharge.MacroState state = PearlCharge.MacroState.FINISH;
    private long nextActionAt = 0L;
    private int originalMainSlot = -1;
    private int windHotbarSlot = -1;
    private int pearlHotbarSlot = -1;
    private int offhandSwapRetries = 0;

    public PearlCharge() {
        super("PearlCharge", "Offhands wind charge, swaps to pearl, double-right-clicks, then optional swapback.", Category.COMBAT);
        this.addSetting(this.swapBack);
        this.addSetting(this.swapDelay);
        this.addSetting(this.randomization);
        this.addSetting(this.minRandom);
        this.addSetting(this.maxRandom);
    }

    @Override
    public void onEnable() {
        if (this.mc.field_1724 == null || this.mc.field_1687 == null || this.mc.field_1690 == null) {
            this.toggle();
        } else if (this.mc.field_1724.method_6115()) {
            this.toggle();
        } else {
            this.windHotbarSlot = this.findHotbarSlot(class_1802.field_49098);
            this.pearlHotbarSlot = this.findHotbarSlot(class_1802.field_8634);
            if (this.windHotbarSlot != -1 && this.pearlHotbarSlot != -1) {
                this.originalMainSlot = this.mc.field_1724.method_31548().method_67532();
                this.offhandSwapRetries = 0;
                this.state = PearlCharge.MacroState.OFFHAND_WIND;
                this.nextActionAt = System.currentTimeMillis();
            } else {
                this.toggle();
            }
        }
    }

    @Override
    public void onDisable() {
        this.state = PearlCharge.MacroState.FINISH;
        this.nextActionAt = 0L;
        this.originalMainSlot = -1;
        this.windHotbarSlot = -1;
        this.pearlHotbarSlot = -1;
        this.offhandSwapRetries = 0;
    }

    @Override
    public void onTick() {
        this.processMacro();
    }

    @Override
    public void onRender(class_332 context, float tickDelta) {
        if (this.isEnabled()) {
            if (this.state == PearlCharge.MacroState.FIRST_CLICK || this.state == PearlCharge.MacroState.SECOND_CLICK) {
                this.processMacro();
            }
        }
    }

    private void processMacro() {
        if (this.isEnabled()) {
            if (this.mc.field_1724 != null && this.mc.field_1687 != null && this.mc.field_1690 != null) {
                if (System.currentTimeMillis() >= this.nextActionAt) {
                    this.runStateMachine();
                }
            } else {
                this.toggle();
            }
        }
    }

    private void runStateMachine() {
        switch (this.state) {
            case OFFHAND_WIND:
                this.handleOffhandWind();
                break;
            case WAIT_OFFHAND_WIND:
                this.handleWaitOffhandWind();
                break;
            case MAINHAND_PEARL:
                this.handleMainhandPearl();
                break;
            case FIRST_CLICK:
                this.handleFirstClick();
                break;
            case SECOND_CLICK:
                this.handleSecondClick();
                break;
            case SWAPBACK_OFFHAND:
                this.handleSwapbackOffhand();
                break;
            case WAIT_SWAPBACK_OFFHAND:
                this.handleWaitSwapbackOffhand();
                break;
            case SWAPBACK_MAINHAND:
                this.handleSwapbackMainhand();
                break;
            case FINISH:
                this.finishMacro();
        }
    }

    private void handleOffhandWind() {
        if (this.mc.field_1724.method_6079().method_7909() == class_1802.field_49098) {
            this.offhandSwapRetries = 0;
            this.transitionTo(PearlCharge.MacroState.MAINHAND_PEARL);
        } else {
            PearlCharge.SwapActionResult toOffhand = this.requestSwapHandsWithHotbarSlot(this.windHotbarSlot, true);
            if (toOffhand == PearlCharge.SwapActionResult.FAILED) {
                this.finishMacro();
            } else if (toOffhand == PearlCharge.SwapActionResult.SENT) {
                this.offhandSwapRetries = 0;
                this.transitionTo(PearlCharge.MacroState.WAIT_OFFHAND_WIND);
            } else {
                this.nextActionAt = System.currentTimeMillis() + this.getActionDelay();
            }
        }
    }

    private void handleWaitOffhandWind() {
        if (this.mc.field_1724.method_6079().method_7909() == class_1802.field_49098) {
            this.offhandSwapRetries = 0;
            this.transitionTo(PearlCharge.MacroState.MAINHAND_PEARL);
        } else if (++this.offhandSwapRetries > 6) {
            this.finishMacro();
        } else {
            PearlCharge.SwapActionResult retryToOffhand = this.requestSwapHandsWithHotbarSlot(this.windHotbarSlot, true);
            if (retryToOffhand == PearlCharge.SwapActionResult.FAILED) {
                this.finishMacro();
            } else {
                this.nextActionAt = System.currentTimeMillis() + this.getActionDelay();
            }
        }
    }

    private void handleMainhandPearl() {
        if (this.findHotbarSlot(class_1802.field_8634) == -1) {
            this.finishMacro();
        } else {
            this.mc.field_1724.method_31548().method_61496(this.pearlHotbarSlot);
            this.transitionTo(PearlCharge.MacroState.FIRST_CLICK);
        }
    }

    private void handleFirstClick() {
        if (this.mc.field_1724.method_6079().method_7909() != class_1802.field_49098) {
            this.finishMacro();
        } else {
            this.mc.field_1724.method_31548().method_61496(this.pearlHotbarSlot);
            this.doRightClick();
            this.transitionToFast(PearlCharge.MacroState.SECOND_CLICK, 18L);
        }
    }

    private void handleSecondClick() {
        if (this.mc.field_1724.method_6079().method_7909() != class_1802.field_49098) {
            this.finishMacro();
        } else {
            this.mc.field_1724.method_31548().method_61496(this.pearlHotbarSlot);
            this.doRightClick();
            if (this.swapBack.isEnabled()) {
                this.transitionTo(PearlCharge.MacroState.SWAPBACK_OFFHAND);
            } else {
                this.transitionTo(PearlCharge.MacroState.FINISH);
            }
        }
    }

    private void handleSwapbackOffhand() {
        PearlCharge.SwapActionResult swapBackResult = this.requestSwapHandsWithHotbarSlot(this.windHotbarSlot, false);
        if (swapBackResult == PearlCharge.SwapActionResult.FAILED) {
            this.finishMacro();
        } else if (swapBackResult == PearlCharge.SwapActionResult.SENT) {
            this.offhandSwapRetries = 0;
            this.transitionTo(PearlCharge.MacroState.WAIT_SWAPBACK_OFFHAND);
        } else {
            this.nextActionAt = System.currentTimeMillis() + this.getActionDelay();
        }
    }

    private void handleWaitSwapbackOffhand() {
        if (this.mc.field_1724.method_6079().method_7909() == class_1802.field_49098 && ++this.offhandSwapRetries <= 4) {
            this.transitionTo(PearlCharge.MacroState.SWAPBACK_OFFHAND);
        } else {
            this.offhandSwapRetries = 0;
            this.transitionTo(PearlCharge.MacroState.SWAPBACK_MAINHAND);
        }
    }

    private void handleSwapbackMainhand() {
        if (this.originalMainSlot >= 0 && this.originalMainSlot < 9) {
            this.mc.field_1724.method_31548().method_61496(this.originalMainSlot);
        }

        this.transitionTo(PearlCharge.MacroState.FINISH);
    }

    private void finishMacro() {
        this.toggle();
    }

    private void transitionTo(PearlCharge.MacroState next) {
        this.state = next;
        this.nextActionAt = System.currentTimeMillis() + this.getActionDelay();
    }

    private void transitionToFast(PearlCharge.MacroState next, long delayMs) {
        this.state = next;
        this.nextActionAt = System.currentTimeMillis() + Math.max(0L, delayMs);
    }

    private PearlCharge.SwapActionResult requestSwapHandsWithHotbarSlot(int hotbarSlot, boolean requireItemInSlot) {
        if (this.mc.field_1724 == null || this.mc.field_1690 == null) {
            return PearlCharge.SwapActionResult.FAILED;
        } else if (hotbarSlot >= 0 && hotbarSlot <= 8) {
            if (this.mc.field_1724.method_31548().method_67532() != hotbarSlot) {
                this.mc.field_1724.method_31548().method_61496(hotbarSlot);
                return PearlCharge.SwapActionResult.WAITING_SLOT_SYNC;
            } else {
                class_1799 stack = this.mc.field_1724.method_31548().method_5438(hotbarSlot);
                if (requireItemInSlot && stack.method_7960()) {
                    return PearlCharge.SwapActionResult.FAILED;
                } else {
                    KeyBindingAccessor swapKey = (KeyBindingAccessor)this.mc.field_1690.field_1831;
                    swapKey.setTimesPressed(swapKey.getTimesPressed() + 1);
                    return PearlCharge.SwapActionResult.SENT;
                }
            }
        } else {
            return PearlCharge.SwapActionResult.FAILED;
        }
    }

    private void doRightClick() {
        if (!PlaceValidator.tryPlace(this.mc, "combat.use.pearlcharge")) {
            if (this.mc.field_1690 != null) {
                KeyBindingAccessor useKey = (KeyBindingAccessor)this.mc.field_1690.field_1904;
                this.mc.field_1690.field_1904.method_23481(true);
                useKey.setTimesPressed(useKey.getTimesPressed() + 1);
                this.mc.field_1690.field_1904.method_23481(false);
            }
        }
    }

    private int findHotbarSlot(class_1792 item) {
        if (this.mc.field_1724 == null) {
            return -1;
        } else {
            for (int i = 0; i < 9; i++) {
                class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
                if (!stack.method_7960() && stack.method_7909() == item) {
                    return i;
                }
            }

            return -1;
        }
    }

    private long getActionDelay() {
        long delay = Math.round(this.swapDelay.getValue());
        if (!this.randomization.isEnabled()) {
            return delay;
        } else {
            double min = Math.min(this.minRandom.getValue(), this.maxRandom.getValue());
            double max = Math.max(this.minRandom.getValue(), this.maxRandom.getValue());
            return max <= min ? delay + (long)min : delay + (long)(min + this.random.nextDouble() * (max - min));
        }
    }

    @Environment(EnvType.CLIENT)
    private static enum MacroState {
        OFFHAND_WIND,
        WAIT_OFFHAND_WIND,
        MAINHAND_PEARL,
        FIRST_CLICK,
        SECOND_CLICK,
        SWAPBACK_OFFHAND,
        WAIT_SWAPBACK_OFFHAND,
        SWAPBACK_MAINHAND,
        FINISH;
    }

    @Environment(EnvType.CLIENT)
    private static enum SwapActionResult {
        FAILED,
        WAITING_SLOT_SYNC,
        SENT;
    }
}
