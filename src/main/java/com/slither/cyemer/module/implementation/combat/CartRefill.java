package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1713;
import net.minecraft.class_1802;
import net.minecraft.class_490;

@Environment(EnvType.CLIENT)
public class CartRefill extends Module {
    private final BooleanSetting smartMode = new BooleanSetting("Smart Mode", true);
    private final BooleanSetting autoOpen = new BooleanSetting("Auto Open", true);
    private final BooleanSetting autoClose = new BooleanSetting("Auto Close", true);
    private final SliderSetting minOpenDelay = new SliderSetting("Min Open Delay", 50.0, 0.0, 300.0, 0);
    private final SliderSetting maxOpenDelay = new SliderSetting("Max Open Delay", 150.0, 0.0, 300.0, 0);
    private final SliderSetting minRefillDelay = new SliderSetting("Min Refill Delay", 50.0, 0.0, 300.0, 0);
    private final SliderSetting maxRefillDelay = new SliderSetting("Max Refill Delay", 150.0, 0.0, 300.0, 0);
    private final SliderSetting minCloseDelay = new SliderSetting("Min Close Delay", 50.0, 0.0, 300.0, 0);
    private final SliderSetting maxCloseDelay = new SliderSetting("Max Close Delay", 150.0, 0.0, 300.0, 0);
    private final BooleanSetting slot1 = new BooleanSetting("Refill Slot 1", false);
    private final BooleanSetting slot2 = new BooleanSetting("Refill Slot 2", false);
    private final BooleanSetting slot3 = new BooleanSetting("Refill Slot 3", false);
    private final BooleanSetting slot4 = new BooleanSetting("Refill Slot 4", false);
    private final BooleanSetting slot5 = new BooleanSetting("Refill Slot 5", false);
    private final BooleanSetting slot6 = new BooleanSetting("Refill Slot 6", false);
    private final BooleanSetting slot7 = new BooleanSetting("Refill Slot 7", false);
    private final BooleanSetting slot8 = new BooleanSetting("Refill Slot 8", false);
    private final BooleanSetting slot9 = new BooleanSetting("Refill Slot 9", false);
    private final Random random = new Random();
    private int stage = 0;
    private long lastActionTime = 0L;
    private long currentDelay = 0L;
    private List<Integer> targetHotbarSlots = new ArrayList<>();
    private List<Integer> slotsToRefill = new ArrayList<>();

    public CartRefill() {
        super("Cart Refill", "Automatically refills TNT minecarts into chosen hotbar slots.", Category.COMBAT);
        this.addSetting(this.smartMode);
        this.addSetting(this.autoOpen);
        this.addSetting(this.autoClose);
        this.addSetting(this.minOpenDelay);
        this.addSetting(this.maxOpenDelay);
        this.addSetting(this.minRefillDelay);
        this.addSetting(this.maxRefillDelay);
        this.addSetting(this.minCloseDelay);
        this.addSetting(this.maxCloseDelay);
        this.addSetting(this.slot1);
        this.addSetting(this.slot2);
        this.addSetting(this.slot3);
        this.addSetting(this.slot4);
        this.addSetting(this.slot5);
        this.addSetting(this.slot6);
        this.addSetting(this.slot7);
        this.addSetting(this.slot8);
        this.addSetting(this.slot9);
    }

    @Override
    public void onEnable() {
        if (this.mc.field_1724 == null) {
            this.toggle();
        } else {
            this.updateTargetSlots();
            this.stage = 0;
            this.lastActionTime = 0L;
            this.currentDelay = 0L;
            this.slotsToRefill.clear();
            if (!this.smartMode.isEnabled()) {
                if (!this.hasCartsInMainInventory()) {
                    this.toggle();
                    return;
                }

                this.populateSlotsToRefill();
                if (this.slotsToRefill.isEmpty()) {
                    this.toggle();
                    return;
                }

                this.startRefillSequence();
            }
        }
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1761 != null) {
            this.updateTargetSlots();
            long currentTime = System.currentTimeMillis();
            if (currentTime - this.lastActionTime >= this.currentDelay) {
                switch (this.stage) {
                    case 0:
                        if (this.smartMode.isEnabled() && this.hasCartsInMainInventory()) {
                            this.populateSlotsToRefill();
                            if (!this.slotsToRefill.isEmpty()) {
                                this.startRefillSequence();
                            }
                        }
                        break;
                    case 1:
                        if (this.autoOpen.isEnabled() && !(this.mc.field_1755 instanceof class_490)) {
                            this.mc.method_1507(new class_490(this.mc.field_1724));
                        }

                        this.stage = 2;
                        this.lastActionTime = System.currentTimeMillis();
                        this.currentDelay = this.getDelay(this.minOpenDelay.getValue(), this.maxOpenDelay.getValue());
                        break;
                    case 2:
                        if (this.slotsToRefill.isEmpty()) {
                            this.stage = 3;
                            this.lastActionTime = System.currentTimeMillis();
                            this.currentDelay = 0L;
                        } else {
                            int hotbarSlotIndex = this.slotsToRefill.remove(0);
                            int inventoryCartSlot = this.findCartInMainInventory();
                            if (inventoryCartSlot != -1) {
                                this.mc
                                    .field_1761
                                    .method_2906(
                                        this.mc.field_1724.field_7498.field_7763, inventoryCartSlot, hotbarSlotIndex, class_1713.field_7791, this.mc.field_1724
                                    );
                                this.lastActionTime = System.currentTimeMillis();
                                this.currentDelay = this.getDelay(this.minRefillDelay.getValue(), this.maxRefillDelay.getValue());
                            } else {
                                this.slotsToRefill.clear();
                            }
                        }
                        break;
                    case 3:
                        if (this.autoClose.isEnabled() && this.mc.field_1755 instanceof class_490) {
                            this.mc.field_1724.method_7346();
                        }

                        this.stage = 4;
                        this.lastActionTime = System.currentTimeMillis();
                        this.currentDelay = this.getDelay(this.minCloseDelay.getValue(), this.maxCloseDelay.getValue());
                        break;
                    case 4:
                        if (this.smartMode.isEnabled()) {
                            this.stage = 0;
                        } else {
                            this.toggle();
                        }
                }
            }
        } else {
            if (this.isEnabled()) {
                this.toggle();
            }
        }
    }

    private void startRefillSequence() {
        this.stage = 1;
        this.lastActionTime = System.currentTimeMillis();
        this.currentDelay = 0L;
    }

    private void updateTargetSlots() {
        this.targetHotbarSlots.clear();
        this.addSlotIfValid(this.slot1, 0);
        this.addSlotIfValid(this.slot2, 1);
        this.addSlotIfValid(this.slot3, 2);
        this.addSlotIfValid(this.slot4, 3);
        this.addSlotIfValid(this.slot5, 4);
        this.addSlotIfValid(this.slot6, 5);
        this.addSlotIfValid(this.slot7, 6);
        this.addSlotIfValid(this.slot8, 7);
        this.addSlotIfValid(this.slot9, 8);
    }

    private void addSlotIfValid(BooleanSetting setting, int slotIndex) {
        if (setting.isEnabled()) {
            this.targetHotbarSlots.add(slotIndex);
        }
    }

    private void populateSlotsToRefill() {
        this.slotsToRefill.clear();

        for (int slot : this.targetHotbarSlots) {
            if (this.mc.field_1724.method_31548().method_5438(slot).method_7909() != class_1802.field_8069) {
                this.slotsToRefill.add(slot);
            }
        }
    }

    private boolean hasCartsInMainInventory() {
        return this.findCartInMainInventory() != -1;
    }

    private int findCartInMainInventory() {
        for (int i = 9; i < 36; i++) {
            if (this.mc.field_1724.method_31548().method_5438(i).method_7909() == class_1802.field_8069) {
                return i;
            }
        }

        return -1;
    }

    private long getDelay(double min, double max) {
        return min >= max ? (long)min : (long)(min + this.random.nextDouble() * (max - min));
    }
}
