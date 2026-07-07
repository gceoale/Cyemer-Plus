package com.slither.cyemer.module.implementation;

import com.slither.cyemer.mixin.HandledScreenAccessor;
import com.slither.cyemer.mixin.HandledScreenInterface;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1713;
import net.minecraft.class_1735;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_332;
import net.minecraft.class_465;
import net.minecraft.class_490;

@Environment(EnvType.CLIENT)
public class HoverTotem extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "Both", "Offhand", "Dhand");
    private final SliderSetting speed = new SliderSetting("Speed (Delay)", 0.0, 0.0, 10.0, 0);
    private final BooleanSetting shutInventory = new BooleanSetting("Shut Inventory", true);
    private final ModeSetting dhandMode = new ModeSetting("Dhand Mode", "Smart", "Manual");
    private final SliderSetting manualSlot = new SliderSetting("Manual Slot", 0.0, 0.0, 8.0, 0);
    private long executionTime = -1L;
    private class_1735 targetSlot = null;
    private HoverTotem.ActionType pendingAction = null;
    private int swapTargetIndex = -1;
    private static final List<class_1792> GARBAGE_ITEMS = Arrays.asList(
        class_1802.field_8831,
        class_1802.field_8270,
        class_1802.field_20391,
        class_1802.field_20412,
        class_1802.field_20394,
        class_1802.field_20401,
        class_1802.field_20407,
        class_1802.field_8328,
        class_1802.field_22000,
        class_1802.field_23843,
        class_1802.field_20399,
        class_1802.field_8110,
        class_1802.field_8858,
        class_1802.field_8317,
        class_1802.field_20384
    );

    public HoverTotem() {
        super("HoverTotem", "Hovering totems equips or moves them", Category.COMBAT);
        this.addSetting(this.mode);
        this.addSetting(this.speed);
        this.addSetting(this.shutInventory);
        this.addSetting(this.dhandMode);
        this.addSetting(this.manualSlot);
    }

    @Override
    public void onDisable() {
        this.resetAction();
    }

    @Override
    public void onRender(class_332 context, float tickDelta) {
        if (this.executionTime != -1L) {
            if (System.currentTimeMillis() >= this.executionTime) {
                this.performAction();
            }
        } else if (!(this.mc.field_1724 != null && this.mc.field_1761 != null && this.mc.field_1755 instanceof class_465<?> screen)) {
            this.resetAction();
        } else if (!(this.mc.field_1755 instanceof class_490)) {
            this.resetAction();
        } else {
            class_1735 hoveredSlot = this.getHoveredSlot(screen);
            if (hoveredSlot != null && hoveredSlot.method_7681() && hoveredSlot.method_7677().method_31574(class_1802.field_8288)) {
                if (!this.isOffhandSlot(hoveredSlot)) {
                    boolean offhandEmpty = this.mc.field_1724.method_6079().method_7960();
                    boolean hotbarFull = this.isHotbarFull();
                    boolean inHotbar = this.isSlotInHotbar(hoveredSlot);
                    String currentMode = this.mode.getCurrentMode();
                    if ((currentMode.equals("Both") || currentMode.equals("Offhand")) && offhandEmpty) {
                        this.scheduleAction(hoveredSlot, HoverTotem.ActionType.OFFHAND_SWAP);
                    } else {
                        if ((currentMode.equals("Both") || currentMode.equals("Dhand")) && !inHotbar) {
                            if (!hotbarFull) {
                                this.scheduleAction(hoveredSlot, HoverTotem.ActionType.HOTBAR_QUICK_MOVE);
                            } else {
                                String dhandModeValue = this.dhandMode.getCurrentMode();
                                if (dhandModeValue.equals("Smart")) {
                                    int garbageIndex = this.getGarbageSlotIndex();
                                    if (garbageIndex != -1) {
                                        this.swapTargetIndex = garbageIndex;
                                        this.scheduleAction(hoveredSlot, HoverTotem.ActionType.HOTBAR_SWAP);
                                    }
                                } else if (dhandModeValue.equals("Manual")) {
                                    int targetIndex = (int)this.manualSlot.getValue();
                                    class_1799 targetStack = this.mc.field_1724.method_31548().method_5438(targetIndex);
                                    if (!targetStack.method_31574(class_1802.field_8288)) {
                                        this.swapTargetIndex = targetIndex;
                                        this.scheduleAction(hoveredSlot, HoverTotem.ActionType.HOTBAR_SWAP);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void scheduleAction(class_1735 slot, HoverTotem.ActionType action) {
        this.targetSlot = slot;
        this.pendingAction = action;
        long delayMs = (long)(this.speed.getValue() * 50.0);
        this.executionTime = System.currentTimeMillis() + delayMs;
    }

    private void performAction() {
        if (this.targetSlot != null && this.pendingAction != null && this.mc.field_1724 != null && this.mc.field_1761 != null) {
            int syncId = this.mc.field_1724.field_7512.field_7763;
            switch (this.pendingAction) {
                case OFFHAND_SWAP:
                    this.mc.field_1761.method_2906(syncId, this.targetSlot.field_7874, 40, class_1713.field_7791, this.mc.field_1724);
                    break;
                case HOTBAR_QUICK_MOVE:
                    this.mc.field_1761.method_2906(syncId, this.targetSlot.field_7874, 0, class_1713.field_7794, this.mc.field_1724);
                    break;
                case HOTBAR_SWAP:
                    if (this.swapTargetIndex != -1) {
                        this.mc.field_1761.method_2906(syncId, this.targetSlot.field_7874, this.swapTargetIndex, class_1713.field_7791, this.mc.field_1724);
                    }
            }

            if (this.shutInventory.isEnabled()) {
                this.mc.field_1724.method_7346();
            }

            this.resetAction();
        } else {
            this.resetAction();
        }
    }

    private void resetAction() {
        this.executionTime = -1L;
        this.targetSlot = null;
        this.pendingAction = null;
        this.swapTargetIndex = -1;
    }

    private class_1735 getHoveredSlot(class_465<?> screen) {
        try {
            HandledScreenInterface acc = (HandledScreenInterface)screen;
            class_1735 s = acc.getFocusedSlot();
            if (s != null) {
                return s;
            }
        } catch (ClassCastException var13) {
        }

        HandledScreenAccessor acc = (HandledScreenAccessor)screen;
        int guiLeft = acc.getX();
        int guiTop = acc.getY();
        double mouseX = this.mc.field_1729.method_1603() * screen.field_22789 / this.mc.method_22683().method_4480();
        double mouseY = this.mc.field_1729.method_1604() * screen.field_22790 / this.mc.method_22683().method_4507();

        for (class_1735 slot : screen.method_17577().field_7761) {
            int slotAbsX = guiLeft + slot.field_7873;
            int slotAbsY = guiTop + slot.field_7872;
            if (mouseX >= slotAbsX && mouseX < slotAbsX + 16 && mouseY >= slotAbsY && mouseY < slotAbsY + 16) {
                return slot;
            }
        }

        return null;
    }

    private boolean isHotbarFull() {
        if (this.mc.field_1724 == null) {
            return true;
        } else {
            for (int i = 0; i < 9; i++) {
                if (this.mc.field_1724.method_31548().method_5438(i).method_7960()) {
                    return false;
                }
            }

            return true;
        }
    }

    private boolean isSlotInHotbar(class_1735 slot) {
        return slot.field_7871 == this.mc.field_1724.method_31548() && slot.method_34266() >= 0 && slot.method_34266() < 9;
    }

    private boolean isOffhandSlot(class_1735 slot) {
        return slot.field_7874 == 45 && (slot.field_7871 != this.mc.field_1724.method_31548() || slot.method_34266() == 40);
    }

    private int getGarbageSlotIndex() {
        if (this.mc.field_1724 == null) {
            return -1;
        } else {
            for (int i = 0; i < 9; i++) {
                class_1799 stack = this.mc.field_1724.method_31548().method_5438(i);
                if (!stack.method_7960() && GARBAGE_ITEMS.contains(stack.method_7909())) {
                    return i;
                }
            }

            return -1;
        }
    }

    @Environment(EnvType.CLIENT)
    private static enum ActionType {
        OFFHAND_SWAP,
        HOTBAR_QUICK_MOVE,
        HOTBAR_SWAP;
    }
}
