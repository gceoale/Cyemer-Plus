package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import com.slither.cyemer.util.RotationManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1268;
import net.minecraft.class_1747;
import net.minecraft.class_1799;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_3965;

/**
 * Places a block under you to walk on, the way a player would rather than the
 * way a packet would.
 *
 * Every placement here is one the server would accept from a human: it clicks a
 * real exposed face of a block that already exists, from inside normal block
 * reach, after actually turning to look at that face. Nothing is placed through
 * geometry, at an angle the camera never held, or against a face that is not
 * facing us.
 *
 * The strongest guarantee is Require Crosshair, on by default: the placement is
 * withheld until the client's own raycast independently lands on the same block
 * and the same face we intend to click. That makes the sent interaction a
 * description of what the player is genuinely looking at, so a wall between us
 * and the target stops the placement rather than being ignored.
 *
 * The cost of being legitimate is that the camera has to move. A scaffold that
 * does not turn to face its target is the blatant kind, so Rotate defaults on.
 */
@Environment(EnvType.CLIENT)
public class Scaffold extends Module {
    /** Vanilla block interaction reach, squared. */
    private static final double REACH_SQUARED = 20.25;
    /** Order tried: straight down reads as the most natural scaffold motion. */
    private static final class_2350[] HORIZONTALS = {
            class_2350.field_11043, class_2350.field_11035,
            class_2350.field_11039, class_2350.field_11034
    };

    private final SliderSetting placeDelay = new SliderSetting("Place Delay (ms)", 120.0, 0.0, 500.0, 0);
    private final BooleanSetting rotate = new BooleanSetting("Rotate", true);
    private final SliderSetting aimSpeed = new SliderSetting("Aim Speed", 22.0, 1.0, 35.0, 1);
    private final SliderSetting aimTolerance = new SliderSetting("Aim Tolerance", 3.0, 0.5, 15.0, 1);
    private final BooleanSetting requireCrosshair = new BooleanSetting("Require Crosshair", true);
    private final BooleanSetting silentAim = new BooleanSetting("Silent Aim", false);
    private final BooleanSetting onlyWhenFalling = new BooleanSetting("Only When Falling", false);
    private final BooleanSetting swapBack = new BooleanSetting("Swap Back", true);

    private long lastPlaceAt = 0L;
    private int originalSlot = -1;
    private class_243 pendingAim = null;

    public Scaffold() {
        super("Scaffold", "Places blocks beneath you to walk on, using placements a player could actually make.", Category.PLAYER);
        this.addSetting(this.placeDelay);
        this.addSetting(this.rotate);
        this.addSetting(this.aimSpeed);
        this.addSetting(this.aimTolerance);
        this.addSetting(this.requireCrosshair);
        this.addSetting(this.silentAim);
        this.addSetting(this.onlyWhenFalling);
        this.addSetting(this.swapBack);
    }

    @Override
    public void onDisable() {
        this.release();
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 == null || this.mc.field_1687 == null || this.mc.field_1761 == null) {
            this.release();
            return;
        }
        if (this.mc.field_1755 != null) {
            this.release();
            return;
        }

        class_2338 support = this.mc.field_1724.method_24515().method_10074();
        // Already standing on something, so there is nothing a player would be
        // placing here and no reason to be turning the camera.
        if (!this.mc.field_1687.method_8320(support).method_45474()) {
            this.release();
            return;
        }
        if (this.onlyWhenFalling.isEnabled() && this.mc.field_1724.method_24828()) {
            this.release();
            return;
        }

        Placement placement = this.findPlacement(support);
        if (placement == null) {
            this.release();
            return;
        }

        this.pendingAim = placement.aim();
        if (this.rotate.isEnabled()) {
            RotationManager.setRotationSupplier(
                    this,
                    RotationManager.Priority.NORMAL,
                    () -> this.pendingAim,
                    this.aimSpeed.getValue(),
                    RotationManager.RotationMode.LINEAR,
                    0.0,
                    this.silentAim.isEnabled(),
                    false
            );
            if (!RotationManager.isControlledBy(this)) {
                return;
            }
            if (!RotationManager.isRotationComplete((float) this.aimTolerance.getValue())) {
                return;
            }
        }

        // Let the client's own raycast agree before sending anything. If it
        // resolves somewhere else - a wall in between, or the camera not there
        // yet - then a player in this position could not make this click.
        if (this.requireCrosshair.isEnabled() && !this.crosshairAgrees(placement)) {
            return;
        }

        if (System.currentTimeMillis() - this.lastPlaceAt < this.placeDelay.getValue()) {
            return;
        }

        int slot = this.findBlockSlot();
        if (slot == -1) {
            return;
        }

        int previous = this.mc.field_1724.method_31548().method_67532();
        if (slot != previous) {
            this.originalSlot = previous;
            this.mc.field_1724.method_31548().method_61496(slot);
        }

        this.mc.field_1761.method_2896(
                this.mc.field_1724,
                class_1268.field_5808,
                new class_3965(placement.aim(), placement.face(), placement.against(), false)
        );
        this.lastPlaceAt = System.currentTimeMillis();

        if (this.swapBack.isEnabled() && this.originalSlot != -1) {
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
            this.originalSlot = -1;
        }
    }

    private boolean crosshairAgrees(Placement placement) {
        return this.mc.field_1765 instanceof class_3965 hit
                && hit.method_17777().equals(placement.against())
                && hit.method_17780() == placement.face();
    }

    /**
     * Looks for an existing block next to the gap whose face toward the gap we
     * could legitimately click. Straight down first, then sideways, which is
     * the order the motion reads naturally in - dropping off a ledge leaves the
     * ledge itself as the sideways candidate.
     */
    private Placement findPlacement(class_2338 gap) {
        Placement best = null;
        double bestDistance = Double.MAX_VALUE;

        Placement below = this.consider(gap, class_2350.field_11033);
        if (below != null) {
            return below;
        }

        for (class_2350 dir : HORIZONTALS) {
            Placement candidate = this.consider(gap, dir);
            if (candidate == null) {
                continue;
            }
            double d = this.mc.field_1724.method_33571().method_1025(candidate.aim());
            if (d < bestDistance) {
                bestDistance = d;
                best = candidate;
            }
        }
        return best;
    }

    /**
     * The neighbour of the gap in {@code dir}, if it is solid enough to place
     * against and close enough to reach. The clicked face is the one pointing
     * back at the gap, which is by definition exposed, since the gap is empty.
     */
    private Placement consider(class_2338 gap, class_2350 dir) {
        class_2338 against = gap.method_10093(dir);
        if (this.mc.field_1687.method_8320(against).method_45474()) {
            return null;
        }
        class_2350 face = dir.method_10153();
        class_243 aim = new class_243(
                against.method_10263() + 0.5 + face.method_10148() * 0.5,
                against.method_10264() + 0.5 + face.method_10164() * 0.5,
                against.method_10260() + 0.5 + face.method_10165() * 0.5
        );
        if (this.mc.field_1724.method_5707(aim) > REACH_SQUARED) {
            return null;
        }
        return new Placement(against, face, aim);
    }

    /** Prefers whatever is already in hand, so the common case needs no swap. */
    private int findBlockSlot() {
        if (isBlock(this.mc.field_1724.method_6047())) {
            return this.mc.field_1724.method_31548().method_67532();
        }
        for (int i = 0; i < 9; i++) {
            if (isBlock(this.mc.field_1724.method_31548().method_5438(i))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isBlock(class_1799 stack) {
        return stack != null && !stack.method_7960() && stack.method_7909() instanceof class_1747;
    }

    private void release() {
        if (this.swapBack.isEnabled() && this.originalSlot != -1 && this.mc.field_1724 != null) {
            this.mc.field_1724.method_31548().method_61496(this.originalSlot);
        }
        this.originalSlot = -1;
        this.pendingAim = null;
        RotationManager.stop(this);
    }

    private record Placement(class_2338 against, class_2350 face, class_243 aim) {}
}
