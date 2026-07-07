package com.slither.cyemer.manager;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_310;
import net.minecraft.class_3966;

@Environment(EnvType.CLIENT)
public class TargetManager {
    private static class_1309 lockedTarget = null;
    private static boolean wasAttackKeyPressed = false;

    public static void update() {
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 != null && mc.field_1687 != null) {
            if (lockedTarget != null
                && (
                    !lockedTarget.method_5805()
                        || lockedTarget.method_31481()
                        || lockedTarget.method_6032() <= 0.0F
                        || mc.field_1724.method_5739(lockedTarget) > 15.0
                )) {
                lockedTarget = null;
            }

            boolean isAttackKeyPressed = mc.field_1690.field_1886.method_1434();
            boolean manualAttack = isAttackKeyPressed && !wasAttackKeyPressed;
            wasAttackKeyPressed = isAttackKeyPressed;
            if (manualAttack && mc.field_1765 instanceof class_3966 entityHit && entityHit.method_17782() instanceof class_1309 target) {
                lockedTarget = target;
            }
        } else {
            lockedTarget = null;
        }
    }

    public static boolean isLocked(class_1297 target) {
        if (lockedTarget == null) {
            if (target instanceof class_1309 living) {
                lockedTarget = living;
            }

            return true;
        } else {
            return target == lockedTarget;
        }
    }

    public static class_1309 getLockedTarget() {
        return lockedTarget;
    }

    public static void clear() {
        lockedTarget = null;
    }
}
