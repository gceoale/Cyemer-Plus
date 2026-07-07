package com.slither.cyemer.util;

import com.slither.cyemer.event.EventBus;
import com.slither.cyemer.event.impl.AttackEvent;
import com.slither.cyemer.mixin.MinecraftClientAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1268;
import net.minecraft.class_1297;
import net.minecraft.class_1799;
import net.minecraft.class_1934;
import net.minecraft.class_310;
import net.minecraft.class_3966;
import net.minecraft.class_239.class_240;

@Environment(EnvType.CLIENT)
public class AttackValidator {
    public static boolean tryAttack(class_310 mc) {
        return tryAttack(mc, "combat.attack.generic");
    }

    public static boolean tryAttack(class_310 mc, String actionKey) {
        if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1761 == null) {
            return false;
        } else if (mc.field_1771 > 0) {
            return false;
        } else if (mc.field_1765 == null) {
            return false;
        } else {
            class_1799 itemStack = mc.field_1724.method_5998(class_1268.field_5808);
            if (!itemStack.method_7960() && !itemStack.method_45435(mc.field_1687.method_45162())) {
                return false;
            } else if (mc.field_1761.method_2920() == class_1934.field_9219) {
                return false;
            } else {
                if (mc.field_1765.method_17783() == class_240.field_1331) {
                    class_3966 entityHit = (class_3966)mc.field_1765;
                    class_1297 target = entityHit.method_17782();
                    if (!target.method_5805()) {
                        return false;
                    }
                }

                AttackEvent event = new AttackEvent();
                EventBus.post(event);
                ((MinecraftClientAccessor)mc).attack();
                return true;
            }
        }
    }

    public static boolean canAttack(class_310 mc) {
        if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1761 == null) {
            return false;
        } else if (mc.field_1771 > 0) {
            return false;
        } else if (mc.field_1765 == null) {
            return false;
        } else if (mc.field_1724.method_6115()) {
            return false;
        } else {
            class_1799 itemStack = mc.field_1724.method_5998(class_1268.field_5808);
            if (!itemStack.method_7960() && !itemStack.method_45435(mc.field_1687.method_45162())) {
                return false;
            } else if (mc.field_1761.method_2920() == class_1934.field_9219) {
                return false;
            } else {
                if (mc.field_1765.method_17783() == class_240.field_1331) {
                    class_3966 entityHit = (class_3966)mc.field_1765;
                    class_1297 target = entityHit.method_17782();
                    if (!target.method_5805()) {
                        return false;
                    }
                }

                return true;
            }
        }
    }
}
