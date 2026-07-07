package com.slither.cyemer.module.implementation.movement;

import com.slither.cyemer.mixin.KeyBindingAccessor;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_304;
import net.minecraft.class_3675;

@Environment(EnvType.CLIENT)
public class SnapTap extends Module {
    private boolean prevLeft = false;
    private boolean prevRight = false;
    private boolean prevForward = false;
    private boolean prevBack = false;
    private boolean lastStrafe = false;
    private boolean lastAxis = false;

    public SnapTap() {
        super("SnapTap", "like rich person keyboard for people without money", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (this.mc.field_1724 != null && this.mc.field_1755 == null) {
            boolean left = this.pressed(this.mc.field_1690.field_1913);
            boolean right = this.pressed(this.mc.field_1690.field_1849);
            boolean forward = this.pressed(this.mc.field_1690.field_1894);
            boolean back = this.pressed(this.mc.field_1690.field_1881);
            if (left && !this.prevLeft) {
                this.lastStrafe = true;
            }

            if (right && !this.prevRight) {
                this.lastStrafe = false;
            }

            if (forward && !this.prevForward) {
                this.lastAxis = true;
            }

            if (back && !this.prevBack) {
                this.lastAxis = false;
            }

            this.mc.field_1690.field_1913.method_23481(left && right ? this.lastStrafe : left);
            this.mc.field_1690.field_1849.method_23481(left && right ? !this.lastStrafe : right);
            this.mc.field_1690.field_1894.method_23481(forward && back ? this.lastAxis : forward);
            this.mc.field_1690.field_1881.method_23481(forward && back ? !this.lastAxis : back);
            this.prevLeft = left;
            this.prevRight = right;
            this.prevForward = forward;
            this.prevBack = back;
        }
    }

    @Override
    public void onDisable() {
        if (this.mc.field_1690 != null) {
            this.mc.field_1690.field_1913.method_23481(this.pressed(this.mc.field_1690.field_1913));
            this.mc.field_1690.field_1849.method_23481(this.pressed(this.mc.field_1690.field_1849));
            this.mc.field_1690.field_1894.method_23481(this.pressed(this.mc.field_1690.field_1894));
            this.mc.field_1690.field_1881.method_23481(this.pressed(this.mc.field_1690.field_1881));
            this.prevLeft = this.prevRight = this.prevForward = this.prevBack = false;
        }
    }

    private boolean pressed(class_304 key) {
        int code = ((KeyBindingAccessor)key).getBoundKey().method_1444();
        return class_3675.method_15987(this.mc.method_22683(), code);
    }
}
