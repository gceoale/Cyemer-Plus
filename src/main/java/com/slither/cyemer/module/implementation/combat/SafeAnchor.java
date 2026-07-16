package com.slither.cyemer.module.implementation.combat;

import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1268;
import net.minecraft.class_1792;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_3965;
import net.minecraft.class_4969;

/**
 * Scans nearby charged respawn anchors and drops a glowstone block on
 * the horizontal midpoint between the player and the anchor at the
 * anchor's Y level. The block soaks the anchor blast so the player
 * takes drastically less damage when it detonates.
 */
@Environment(EnvType.CLIENT)
public class SafeAnchor extends Module {
    private final SliderSetting range = new SliderSetting("Range", 6.0, 1.0, 10.0, 1);
    private final SliderSetting placeCooldown = new SliderSetting("Place Cooldown (ms)", 100.0, 0.0, 500.0, 0);

    private long lastPlaceTime = 0L;

    public SafeAnchor() {
        super("SafeAnchor", "Drops glowstone between you and any charged respawn anchor so the blast is absorbed.", Category.COMBAT);
        this.addSetting(this.range);
        this.addSetting(this.placeCooldown);
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 == null || this.mc.field_1687 == null || this.mc.field_1761 == null) return;
        if (System.currentTimeMillis() - this.lastPlaceTime < (long) this.placeCooldown.getValue()) return;

        class_2338 anchorPos = this.findChargedAnchor();
        if (anchorPos == null) return;

        class_2338 shieldPos = this.computeShieldPos(anchorPos);
        if (shieldPos == null) return;
        if (!this.mc.field_1687.method_8320(shieldPos).method_45474()) return;

        int glowstoneSlot = this.findItemInHotbar(class_1802.field_8801);
        if (glowstoneSlot == -1) return;

        class_2338 placeAgainst = this.findAdjacentSolidBlock(shieldPos);
        if (placeAgainst == null) return;

        class_2350 face = class_2350.method_10147(
                shieldPos.method_10263() - placeAgainst.method_10263(),
                shieldPos.method_10264() - placeAgainst.method_10264(),
                shieldPos.method_10260() - placeAgainst.method_10260()
        );
        if (face == null) return;

        int originalSlot = this.mc.field_1724.method_31548().method_67532();
        this.mc.field_1724.method_31548().method_61496(glowstoneSlot);

        class_243 hitPos = new class_243(
                placeAgainst.method_10263() + 0.5 + face.method_10148() * 0.5,
                placeAgainst.method_10264() + 0.5 + face.method_10164() * 0.5,
                placeAgainst.method_10260() + 0.5 + face.method_10165() * 0.5
        );
        class_3965 hitResult = new class_3965(hitPos, face, placeAgainst, false);
        this.mc.field_1761.method_2896(this.mc.field_1724, class_1268.field_5808, hitResult);

        this.mc.field_1724.method_31548().method_61496(originalSlot);
        this.lastPlaceTime = System.currentTimeMillis();
    }

    private class_2338 findChargedAnchor() {
        int r = (int) Math.ceil(this.range.getValue());
        class_2338 origin = this.mc.field_1724.method_24515();
        class_2338 best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    class_2338 pos = origin.method_10069(dx, dy, dz);
                    class_2680 state = this.mc.field_1687.method_8320(pos);
                    if (state.method_27852(class_2246.field_23152)) {
                        int charges = (Integer) state.method_11654(class_4969.field_23153);
                        if (charges > 0) {
                            double d = this.mc.field_1724.method_5649(
                                    pos.method_10263() + 0.5, pos.method_10264() + 0.5, pos.method_10260() + 0.5);
                            if (d < bestDist) {
                                bestDist = d;
                                best = pos;
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    private class_2338 computeShieldPos(class_2338 anchorPos) {
        class_243 playerPos = this.mc.field_1724.method_73189();
        class_243 anchorCenter = anchorPos.method_46558();
        class_243 midpoint = playerPos.method_1019(anchorCenter).method_1021(0.5);
        return new class_2338(
                (int) Math.floor(midpoint.field_1352),
                anchorPos.method_10264(),
                (int) Math.floor(midpoint.field_1350)
        );
    }

    private class_2338 findAdjacentSolidBlock(class_2338 pos) {
        class_2350[] dirs = {
                class_2350.field_11033, class_2350.field_11036,
                class_2350.field_11043, class_2350.field_11035,
                class_2350.field_11039, class_2350.field_11034
        };
        for (class_2350 dir : dirs) {
            class_2338 adj = pos.method_10093(dir);
            if (!this.mc.field_1687.method_8320(adj).method_45474()) {
                return adj;
            }
        }
        return null;
    }

    private int findItemInHotbar(class_1792 item) {
        for (int i = 0; i < 9; i++) {
            if (this.mc.field_1724.method_31548().method_5438(i).method_7909() == item) {
                return i;
            }
        }
        return -1;
    }
}
