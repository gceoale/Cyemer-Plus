package com.slither.cyemer.util;

import com.slither.cyemer.mixin.MinecraftClientAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1268;
import net.minecraft.class_1799;
import net.minecraft.class_1934;
import net.minecraft.class_310;
import net.minecraft.class_3965;
import net.minecraft.class_239.class_240;

@Environment(EnvType.CLIENT)
public class PlaceValidator {
    public static boolean tryPlace(class_310 mc) {
        return tryPlace(mc, "combat.use.place.generic");
    }

    public static boolean tryPlace(class_310 mc, String actionKey) {
        if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1761 == null) {
            return false;
        } else if (mc.field_1755 != null) {
            return false;
        } else if (mc.field_1765 != null && mc.field_1765.method_17783() == class_240.field_1332) {
            class_1799 itemStack = mc.field_1724.method_5998(class_1268.field_5808);
            if (itemStack.method_7960() || !itemStack.method_45435(mc.field_1687.method_45162())) {
                return false;
            } else if (mc.field_1761.method_2920() == class_1934.field_9219) {
                return false;
            } else {
                ((MinecraftClientAccessor)mc).useItem();
                return true;
            }
        } else {
            return false;
        }
    }

    public static boolean canPlace(class_310 mc) {
        if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1761 == null) {
            return false;
        } else if (mc.field_1755 != null) {
            return false;
        } else if (mc.field_1765 == null || mc.field_1765.method_17783() != class_240.field_1332) {
            return false;
        } else if (mc.field_1724.method_6115()) {
            return false;
        } else {
            class_1799 itemStack = mc.field_1724.method_5998(class_1268.field_5808);
            return !itemStack.method_7960() && itemStack.method_45435(mc.field_1687.method_45162());
        }
    }

    public static class_3965 getBlockHitResult(class_310 mc) {
        return mc.field_1765 != null && mc.field_1765.method_17783() == class_240.field_1332 ? (class_3965)mc.field_1765 : null;
    }
}
