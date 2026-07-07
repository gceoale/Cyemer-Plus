package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.PlaceValidator;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1792;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_3965;
import net.minecraft.class_4969;

@Environment(EnvType.CLIENT)
public class AnchorMacro extends Module {
    private final BooleanSetting useTotem = new BooleanSetting("Use Totem", true);
    private final SliderSetting fillDelay = new SliderSetting("Fill Delay (ms)", 50.0, 0.0, 500.0, 0);
    private final SliderSetting explodeDelay = new SliderSetting("Explode Delay (ms)", 50.0, 0.0, 500.0, 0);
    private final BooleanSetting onlyOwnAnchors = new BooleanSetting("Only Own Anchors", true);
    private class_2338 trackedAnchor = null;
    private long lastAction = 0L;
    private int explodeClicks = 0;
    private int lookTicks = 0;
    private int originalSlot = -1;
    private final Set<class_2338> ownedAnchors = new HashSet<>();

    public AnchorMacro() {
        super("AnchorMacro", "Automatically fills and explodes anchors.", Category.COMBAT);
        this.addSetting(this.useTotem);
        this.addSetting(this.fillDelay);
        this.addSetting(this.explodeDelay);
        this.addSetting(this.onlyOwnAnchors);
    }

    @Override
    public void onEnable() {
        this.reset();
    }

    @Override
    public void onDisable() {
        this.restoreSlot();
        this.reset();
        this.ownedAnchors.clear();
    }

    public void onItemUse(class_3965 hit) {
        if (this.onlyOwnAnchors.isEnabled()) {
            if (this.mc.field_1724 != null) {
                if (this.mc.field_1724.method_6047().method_7909() == class_1802.field_23141) {
                    class_2338 clicked = hit.method_17777();
                    class_2680 state = this.mc.field_1687.method_8320(clicked);
                    class_2338 placement = state.method_45474() ? clicked : clicked.method_10093(hit.method_17780());
                    this.ownedAnchors.add(placement);
                }
            }
        }
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null) {
            if (!this.mc.field_1724.method_6115()) {
                class_3965 hit = PlaceValidator.getBlockHitResult(this.mc);
                class_2338 target = this.getAnchorAt(hit);
                if (!this.isSamePos(target)) {
                    this.restoreSlot();
                    this.reset();
                    if (target == null) {
                        return;
                    }

                    this.trackedAnchor = target;
                    this.originalSlot = this.mc.field_1724.method_31548().method_67532();
                }

                if (!this.isLookingAt(hit)) {
                    this.lookTicks = 0;
                } else {
                    this.lookTicks++;
                    if (this.lookTicks >= 2) {
                        int charges = (Integer)this.mc.field_1687.method_8320(this.trackedAnchor).method_11654(class_4969.field_23153);
                        if (charges == 0) {
                            if (!(this.elapsed() < this.fillDelay.getValue())) {
                                int gsSlot = this.findInHotbar(class_1802.field_8801);
                                if (gsSlot != -1) {
                                    this.mc.field_1724.method_31548().method_61496(gsSlot);
                                    if (PlaceValidator.canPlace(this.mc)) {
                                        PlaceValidator.tryPlace(this.mc);
                                        this.lastAction = System.currentTimeMillis();
                                    }
                                }
                            }
                        } else if (!(this.elapsed() < this.explodeDelay.getValue())) {
                            int slot = this.useTotem.isEnabled() ? this.findInHotbar(class_1802.field_8288) : this.originalSlot;
                            if (slot == -1) {
                                slot = this.originalSlot;
                            }

                            if (slot != -1) {
                                this.mc.field_1724.method_31548().method_61496(slot);
                            }

                            if (PlaceValidator.tryPlace(this.mc)) {
                                this.explodeClicks++;
                                this.lastAction = System.currentTimeMillis();
                            }

                            if (this.explodeClicks >= 1) {
                                this.ownedAnchors.remove(this.trackedAnchor);
                                this.restoreSlot();
                                this.reset();
                            }
                        }
                    }
                }
            }
        }
    }

    private class_2338 getAnchorAt(class_3965 hit) {
        if (hit == null) {
            return null;
        } else {
            class_2338 pos = hit.method_17777();
            if (!this.mc.field_1687.method_8320(pos).method_27852(class_2246.field_23152)) {
                return null;
            } else {
                return this.onlyOwnAnchors.isEnabled() && !this.ownedAnchors.contains(pos) ? null : pos;
            }
        }
    }

    private boolean isSamePos(class_2338 pos) {
        if (this.trackedAnchor == null && pos == null) {
            return true;
        } else {
            return this.trackedAnchor != null && pos != null ? this.trackedAnchor.equals(pos) : false;
        }
    }

    private boolean isLookingAt(class_3965 hit) {
        return hit != null && this.trackedAnchor != null ? hit.method_17777().equals(this.trackedAnchor) : false;
    }

    private int findInHotbar(class_1792 item) {
        for (int i = 0; i < 9; i++) {
            if (this.mc.field_1724.method_31548().method_5438(i).method_7909() == item) {
                return i;
            }
        }

        return -1;
    }

    private long elapsed() {
        return System.currentTimeMillis() - this.lastAction;
    }

    private void restoreSlot() {
        if (this.mc.field_1724 != null && this.originalSlot != -1) {
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
            this.originalSlot = -1;
        }
    }

    private void reset() {
        this.trackedAnchor = null;
        this.lastAction = 0L;
        this.explodeClicks = 0;
        this.lookTicks = 0;
    }
}
