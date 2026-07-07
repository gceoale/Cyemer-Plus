package com.slither.cyemer.module.implementation;

import com.slither.cyemer.mixin.HandledScreenAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.SystemInputSimulator;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1713;
import net.minecraft.class_1735;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_332;
import net.minecraft.class_418;
import net.minecraft.class_465;
import net.minecraft.class_490;

@Environment(EnvType.CLIENT)
public class AutoTotem extends Module {
    private static final int OFFHAND_CRAFTING_SLOT_ID = 45;
    private static final int OFFHAND_INVENTORY_INDEX = 40;
    private static final int OPEN_DELAY_MIN = 0;
    private static final int OPEN_DELAY_RANGE = 50;
    private static final int SCAN_DELAY_MIN = 0;
    private static final int SCAN_DELAY_RANGE = 50;
    private static final int CLOSE_DELAY_MIN = 100;
    private static final int CLOSE_DELAY_RANGE = 100;
    private static final int CURSOR_BUSY_DELAY = 200;
    private static final int IDLE_COOLDOWN = 500;
    private static final int POST_CLICK_SCAN_DELAY_MIN = 60;
    private static final int POST_CLICK_SCAN_DELAY_RANGE = 60;
    private static final long DELAY_MIN_CLAMP = 1L;
    private static final long DELAY_MAX_CLAMP = 1500L;
    private final SliderSetting fastDelay = new SliderSetting("Fast Delay (ms)", 67.0, 0.0, 300.0, 0);
    private final SliderSetting slowDelay = new SliderSetting("Fumble Delay (ms)", 325.0, 0.0, 800.0, 0);
    private final SliderSetting fumbleChance = new SliderSetting("Fumble Chance %", 55.0, 0.0, 100.0, 0);
    private final BooleanSetting autoOpen = new BooleanSetting("Auto Open Inv", true);
    private final BooleanSetting shutInventory = new BooleanSetting("Auto Close", true);
    private final BooleanSetting clickSim = new BooleanSetting("ClickSim", false);
    private final BooleanSetting simCursor = new BooleanSetting("SimCursor", false);
    private final SliderSetting cursorSpeed = new SliderSetting("Cursor Speed", 18.0, 1.0, 100.0, 0);
    private final ModeSetting cursorMode = new ModeSetting("Cursor Mode", "Smooth", "Heuristic", "Instant");
    private AutoTotem.State currentState = AutoTotem.State.IDLE;
    private long actionTimer = -1L;
    private class_1735 targetSlot = null;
    private boolean openedByBot = false;
    private final Random random = new Random();

    public AutoTotem() {
        super("AutoTotem", "Automatically equips totems to offhand with humanized timing", Category.COMBAT);
        this.addSetting(this.fastDelay);
        this.addSetting(this.slowDelay);
        this.addSetting(this.fumbleChance);
        this.addSetting(this.autoOpen);
        this.addSetting(this.shutInventory);
        this.addSetting(this.clickSim);
        this.addSetting(this.simCursor);
        this.addSetting(this.cursorSpeed);
        this.addSetting(this.cursorMode);
    }

    @Override
    public void onDisable() {
        this.resetState();
    }

    @Override
    public void onRender(class_332 context, float tickDelta) {
        this.runCycle();
    }

    @Override
    public void onTick() {
        this.runCycle();
    }

    private void runCycle() {
        if (this.mc.field_1724 != null && this.mc.field_1687 != null && this.mc.field_1761 != null) {
            if (!this.mc.field_1724.method_29504() && !(this.mc.field_1724.method_6032() <= 0.0F) && !(this.mc.field_1755 instanceof class_418)) {
                if (this.mc.field_1755 == null) {
                    this.handlePlayingLogic();
                } else if (this.mc.field_1755 instanceof class_465) {
                    this.handleInventoryLogic((class_465<?>)this.mc.field_1755);
                } else {
                    this.resetState();
                }
            } else {
                this.resetState();
            }
        } else {
            this.resetState();
        }
    }

    private void handlePlayingLogic() {
        if (this.currentState == AutoTotem.State.IDLE) {
            this.openedByBot = false;
        }

        boolean needsRefill = !this.mc.field_1724.method_6079().method_31574(class_1802.field_8288);
        if (this.autoOpen.isEnabled() && needsRefill && this.hasTotemInInventory()) {
            switch (this.currentState) {
                case IDLE:
                case COOLDOWN:
                case CLOSING:
                    this.scheduleNextState(AutoTotem.State.PRE_OPENING, this.getBimodalDelay());
                    break;
                case PRE_OPENING:
                    if (System.currentTimeMillis() >= this.actionTimer) {
                        if (this.mc.field_1755 == null) {
                            this.openedByBot = true;
                            this.mc.method_1507(new class_490(this.mc.field_1724));
                            this.scheduleNextState(AutoTotem.State.OPENING_WAIT, 0 + this.random.nextInt(50));
                        } else {
                            this.resetState();
                        }
                    }
                    break;
                case OPENING_WAIT:
                case SCANNING:
                case SCHEDULED:
                case EXECUTING:
                default:
                    this.resetState();
            }
        } else {
            this.currentState = AutoTotem.State.IDLE;
        }
    }

    private void handleInventoryLogic(class_465<?> screen) {
        if (!screen.method_17577().method_34255().method_7960()) {
            this.currentState = AutoTotem.State.IDLE;
            this.actionTimer = System.currentTimeMillis() + 200L;
        } else {
            switch (this.currentState) {
                case IDLE:
                case PRE_OPENING:
                case OPENING_WAIT:
                    if (!this.isPlayerInventory(screen)) {
                        this.resetState();
                        return;
                    }

                    this.scheduleNextState(AutoTotem.State.SCANNING, 0 + this.random.nextInt(50));
                    break;
                case SCANNING:
                    if (System.currentTimeMillis() < this.actionTimer) {
                        return;
                    }

                    if (!this.isPlayerInventory(screen)) {
                        this.resetState();
                        return;
                    }

                    if (!this.mc.field_1724.method_6079().method_31574(class_1802.field_8288)) {
                        class_1735 totem = this.findTotem(screen);
                        if (totem != null) {
                            this.targetSlot = totem;
                            this.scheduleNextState(AutoTotem.State.SCHEDULED, this.getBimodalDelay());
                            return;
                        }
                    }

                    if (this.openedByBot && this.shutInventory.isEnabled()) {
                        this.scheduleNextState(AutoTotem.State.CLOSING, 100 + this.random.nextInt(100));
                    } else {
                        this.currentState = AutoTotem.State.IDLE;
                        this.actionTimer = System.currentTimeMillis() + 500L;
                    }
                    break;
                case SCHEDULED:
                    if (System.currentTimeMillis() >= this.actionTimer) {
                        this.currentState = AutoTotem.State.EXECUTING;
                    }
                    break;
                case EXECUTING:
                    if (this.performClick(screen)) {
                        this.scheduleNextState(AutoTotem.State.SCANNING, 60 + this.random.nextInt(60));
                    }
                    break;
                case COOLDOWN:
                    this.scheduleNextState(AutoTotem.State.SCANNING, 10L);
                    break;
                case CLOSING:
                    if (System.currentTimeMillis() >= this.actionTimer) {
                        if (this.openedByBot) {
                            this.mc.field_1724.method_7346();
                        }

                        this.resetState();
                    }
            }
        }
    }

    private boolean isPlayerInventory(class_465<?> screen) {
        return screen instanceof class_490;
    }

    private boolean performClick(class_465<?> screen) {
        if (this.targetSlot == null || this.mc.field_1761 == null) {
            return true;
        } else if (this.simCursor.isEnabled() && !this.moveCursorToSlot(screen, this.targetSlot)) {
            return false;
        } else {
            int syncId = this.mc.field_1724.field_7512.field_7763;
            if (this.clickSim.isEnabled()) {
                if (!this.simCursor.isEnabled()) {
                    this.mc.field_1761.method_2906(syncId, this.targetSlot.field_7874, 40, class_1713.field_7791, this.mc.field_1724);
                } else {
                    SystemInputSimulator.tapKey(this.mc.field_1690.field_1831);
                }
            } else {
                this.mc.field_1761.method_2906(syncId, this.targetSlot.field_7874, 40, class_1713.field_7791, this.mc.field_1724);
            }

            return true;
        }
    }

    private long getBimodalDelay() {
        boolean isFumble = this.random.nextInt(100) < this.fumbleChance.getValue();
        long delay;
        if (isFumble) {
            long mean = (long)this.slowDelay.getValue();
            delay = (long)(mean + this.random.nextGaussian() * 50.0);
        } else {
            long mean = (long)this.fastDelay.getValue();
            delay = (long)(mean + this.random.nextGaussian() * 20.0);
        }

        return Math.max(1L, Math.min(delay, 1500L));
    }

    private void scheduleNextState(AutoTotem.State state, long delayMs) {
        this.currentState = state;
        this.actionTimer = System.currentTimeMillis() + delayMs;
    }

    private void resetState() {
        this.currentState = AutoTotem.State.IDLE;
        this.actionTimer = -1L;
        this.targetSlot = null;
        this.openedByBot = false;
    }

    private boolean hasTotemInInventory() {
        for (class_1799 stack : this.mc.field_1724.method_31548().method_67533()) {
            if (stack.method_31574(class_1802.field_8288)) {
                return true;
            }
        }

        return false;
    }

    private class_1735 findTotem(class_465<?> screen) {
        if (this.simCursor.isEnabled()) {
            double cursorX = SystemInputSimulator.getScaledCursorX(screen.field_22789);
            double cursorY = SystemInputSimulator.getScaledCursorY(screen.field_22790);
            return this.findNearestTotem(screen, cursorX, cursorY);
        } else {
            return this.findFirstTotem(screen);
        }
    }

    private class_1735 findFirstTotem(class_465<?> screen) {
        for (class_1735 slot : screen.method_17577().field_7761) {
            if (slot.method_7681()
                && slot.method_7677().method_31574(class_1802.field_8288)
                && slot.field_7874 != 45
                && (slot.field_7871 != this.mc.field_1724.method_31548() || slot.method_34266() != 40)) {
                return slot;
            }
        }

        return null;
    }

    private class_1735 findNearestTotem(class_465<?> screen, double cursorX, double cursorY) {
        HandledScreenAccessor accessor = (HandledScreenAccessor)screen;
        int guiX = accessor.getX();
        int guiY = accessor.getY();
        class_1735 nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (class_1735 slot : screen.method_17577().field_7761) {
            if (slot.method_7681()
                && slot.method_7677().method_31574(class_1802.field_8288)
                && slot.field_7874 != 45
                && (slot.field_7871 != this.mc.field_1724.method_31548() || slot.method_34266() != 40)) {
                double slotCenterX = guiX + slot.field_7873 + 8.0;
                double slotCenterY = guiY + slot.field_7872 + 8.0;
                double dx = slotCenterX - cursorX;
                double dy = slotCenterY - cursorY;
                double distanceSq = dx * dx + dy * dy;
                if (distanceSq < nearestDistance) {
                    nearestDistance = distanceSq;
                    nearest = slot;
                }
            }
        }

        return nearest;
    }

    private boolean moveCursorToSlot(class_465<?> screen, class_1735 slot) {
        HandledScreenAccessor accessor = (HandledScreenAccessor)screen;
        double targetX = accessor.getX() + slot.field_7873 + 8.0;
        double targetY = accessor.getY() + slot.field_7872 + 8.0;
        return SystemInputSimulator.moveCursorToScaled(
            screen.field_22789, screen.field_22790, targetX, targetY, this.cursorSpeed.getValue(), this.cursorMode.getCurrentMode()
        );
    }

    @Environment(EnvType.CLIENT)
    private static enum State {
        IDLE,
        PRE_OPENING,
        OPENING_WAIT,
        SCANNING,
        SCHEDULED,
        EXECUTING,
        COOLDOWN,
        CLOSING;
    }
}
