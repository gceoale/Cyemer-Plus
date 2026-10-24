package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.mixin.PlayerInventoryAccessor;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.AttackValidator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1713;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_3966;

/**
 * Keeps a totem from eating your hit.
 *
 * A totem in the main hand carries no attack damage attribute, so swinging with
 * one buries the hit for about a point of damage. This takes over that swing:
 * the vanilla attack is cancelled, a sword is brought into hand, and the hit is
 * re-issued a tick later so the server resolves damage off the sword's
 * attributes instead. The totem goes straight back afterwards.
 *
 * Deliberately only looks at the main hand. An offhand totem never contributes
 * to a melee hit, so AutoTotem's usual offhand slot is left completely alone.
 */
@Environment(EnvType.CLIENT)
public class TotemHit extends Module {
    /** Hotbar indices; main inventory lives at 9..35 in the same numbering. */
    private static final int HOTBAR_SIZE = 9;
    private static final int INVENTORY_END = 36;
    /** Ticks held before handing the slot back, matching the other swap modules. */
    private static final int RESTORE_TICKS = 3;

    private final BooleanSetting inventorySwap = new BooleanSetting("Inventory Swap", false);
    private final BooleanSetting swapBack = new BooleanSetting("Swap Back", true);

    private boolean isSwapping = false;
    private int swapTicks = 0;
    private int originalSlot = -1;
    private int pulledFromSlot = -1;
    private int pulledIntoHotbar = -1;

    public TotemHit() {
        super("TotemHit", "Swaps a held totem out for a sword so the hit lands with the sword's damage.", Category.COMBAT);
        this.addSetting(this.inventorySwap);
        this.addSetting(this.swapBack);
    }

    @Override
    public void onDisable() {
        this.restore();
    }

    /**
     * Invoked from MinecraftClientMixin ahead of the vanilla attack. Returning
     * true cancels it, because the swap has to reach the server before the hit
     * is worth sending - swapping and attacking in the same tick would still
     * resolve against the totem.
     */
    public boolean handleAttack() {
        if (this.mc.field_1724 == null || this.mc.field_1761 == null || this.isSwapping) {
            return false;
        }
        if (!isTotem(this.mc.field_1724.method_6047())) {
            return false;
        }
        // Nothing under the crosshair means there is no hit to rescue.
        if (!(this.mc.field_1765 instanceof class_3966)) {
            return false;
        }

        int hotbarSword = this.findSword(0, HOTBAR_SIZE);
        if (hotbarSword != -1) {
            this.originalSlot = this.mc.field_1724.method_31548().method_67532();
            this.mc.field_1724.method_31548().method_61496(hotbarSword);
            this.begin();
            return true;
        }

        if (this.inventorySwap.isEnabled()) {
            int packSword = this.findSword(HOTBAR_SIZE, INVENTORY_END);
            if (packSword != -1) {
                this.originalSlot = this.mc.field_1724.method_31548().method_67532();
                // Swapping into the slot we are already holding means the hand
                // changes without the selection moving, and the totem lands in
                // the slot the sword vacated so one more swap undoes it exactly.
                this.pulledFromSlot = packSword;
                this.pulledIntoHotbar = this.originalSlot;
                this.swapSlots(packSword, this.originalSlot);
                this.begin();
                return true;
            }
        }
        return false;
    }

    private void begin() {
        this.isSwapping = true;
        this.swapTicks = 0;
    }

    @Override
    public void onTick() {
        if (!this.isSwapping) {
            return;
        }
        if (this.mc.field_1724 == null) {
            this.reset();
            return;
        }

        this.swapTicks++;
        if (this.swapTicks == 1) {
            if (!AttackValidator.tryAttack(this.mc, "combat.attack.totemhit")) {
                this.restore();
                return;
            }
        }
        if (this.swapTicks >= RESTORE_TICKS) {
            this.restore();
        }
    }

    private void restore() {
        if (this.mc.field_1724 == null) {
            this.reset();
            return;
        }
        if (this.swapBack.isEnabled()) {
            if (this.pulledFromSlot != -1 && this.pulledIntoHotbar != -1) {
                this.swapSlots(this.pulledFromSlot, this.pulledIntoHotbar);
            }
            if (this.originalSlot >= 0 && this.originalSlot < HOTBAR_SIZE) {
                this.mc.field_1724.method_31548().method_61496(this.originalSlot);
            }
        }
        this.reset();
    }

    private void reset() {
        this.isSwapping = false;
        this.swapTicks = 0;
        this.originalSlot = -1;
        this.pulledFromSlot = -1;
        this.pulledIntoHotbar = -1;
    }

    /**
     * SWAP click against the player's own screen handler, which is what lets a
     * slot outside the hotbar be reached without opening the inventory.
     */
    private void swapSlots(int screenSlot, int hotbarIndex) {
        if (this.mc.field_1761 == null || this.mc.field_1724 == null) {
            return;
        }
        int syncId = this.mc.field_1724.field_7512.field_7763;
        this.mc.field_1761.method_2906(syncId, screenSlot, hotbarIndex, class_1713.field_7791, this.mc.field_1724);
    }

    /** First sword found in [from, to) of the player inventory, or -1. */
    private int findSword(int from, int to) {
        for (int i = from; i < to; i++) {
            if (isSword(this.mc.field_1724.method_31548().method_5438(i))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isTotem(class_1799 stack) {
        return stack != null && stack.method_31574(class_1802.field_8288);
    }

    /** No SwordItem class in 1.21.11, so match on the item's registry name. */
    private static boolean isSword(class_1799 stack) {
        if (stack == null || stack.method_7960()) {
            return false;
        }
        return stack.method_7909().toString().toLowerCase().contains("sword");
    }
}
