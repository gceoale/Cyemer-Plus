package com.slither.cyemer.module.implementation.movement;

import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;
import net.minecraft.class_746;

@Environment(EnvType.CLIENT)
public class Fly extends Module {
    private final SliderSetting speed = new SliderSetting("Speed", 1.0, 0.1, 5.0, 1);

    public Fly() {
        super("Fly", "YOU WILL BE BANNED! TESTING ONLY!", Category.MOVEMENT);
        this.addSetting(this.speed);
    }

    @Override
    public void onEnable() {
        if (this.mc != null && this.mc.field_1724 != null) {
            this.mc.field_1724.method_31549().field_7478 = true;
            this.mc.field_1724.method_31549().field_7479 = true;
        }
    }

    @Override
    public void onDisable() {
        if (this.mc != null && this.mc.field_1724 != null) {
            class_746 player = this.mc.field_1724;
            player.method_31549().field_7478 = false;
            player.method_31549().field_7479 = false;
        }
    }

    @Override
    public void onTick() {
        if (this.mc != null && this.mc.field_1724 != null) {
            class_746 player = this.mc.field_1724;
            player.method_31549().field_7478 = true;
            player.method_31549().field_7479 = true;
            double flySpeed = this.speed.getValue() / 10.0;
            double motionY = 0.0;
            if (this.mc.field_1690.field_1903.method_1434()) {
                motionY += flySpeed;
            }

            if (this.mc.field_1690.field_1832.method_1434()) {
                motionY -= flySpeed;
            }

            class_243 forward = class_243.method_1030(0.0F, player.method_36454()).method_1029();
            class_243 right = forward.method_1036(new class_243(0.0, 1.0, 0.0)).method_1029();
            double motionX = 0.0;
            double motionZ = 0.0;
            if (this.mc.field_1690.field_1894.method_1434()) {
                motionX += forward.field_1352 * flySpeed;
                motionZ += forward.field_1350 * flySpeed;
            }

            if (this.mc.field_1690.field_1881.method_1434()) {
                motionX -= forward.field_1352 * flySpeed;
                motionZ -= forward.field_1350 * flySpeed;
            }

            if (this.mc.field_1690.field_1913.method_1434()) {
                motionX -= right.field_1352 * flySpeed;
                motionZ -= right.field_1350 * flySpeed;
            }

            if (this.mc.field_1690.field_1849.method_1434()) {
                motionX += right.field_1352 * flySpeed;
                motionZ += right.field_1350 * flySpeed;
            }

            player.method_18800(motionX, motionY, motionZ);
            player.field_64356 = true;
        }
    }
}
