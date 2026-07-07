package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1268;
import net.minecraft.class_1799;
import net.minecraft.class_3489;
import net.minecraft.class_3532;
import net.minecraft.class_4587;
import net.minecraft.class_7833;

@Environment(EnvType.CLIENT)
public class HitAnimations extends Module {
    private static HitAnimations instance;
    private final ModeSetting swingAnimation = new ModeSetting("Swing Animation", "Slice", "Block", "Chop", "Spin", "Pop");
    private final SliderSetting swingSpeed = new SliderSetting("Swing Speed", 1.0, 0.1, 3.0, 1);
    private final BooleanSetting onlySwords = new BooleanSetting("Only Swords", false);
    private final BooleanSetting instantEquip = new BooleanSetting("Instant Equip", true);
    private final BooleanSetting ignoreOffhand = new BooleanSetting("Ignore Offhand", true);

    public HitAnimations() {
        super("HitAnimations", "Custom hit and swing animations.", Category.RENDER);
        this.addSetting(this.swingAnimation);
        this.addSetting(this.swingSpeed);
        this.addSetting(this.instantEquip);
        this.addSetting(this.onlySwords);
        this.addSetting(this.ignoreOffhand);
        instance = this;
    }

    public static HitAnimations getInstance() {
        return instance;
    }

    public double getSwingSpeed() {
        return this.swingSpeed.getValue();
    }

    public boolean isInstantEquipEnabled() {
        return this.instantEquip.isEnabled();
    }

    public boolean onRenderFirstPerson(class_4587 matrices, float swingProgress, class_1799 itemStack, class_1268 hand) {
        if (this.ignoreOffhand.isEnabled() && hand == class_1268.field_5810) {
            return false;
        } else if (this.onlySwords.isEnabled() && !itemStack.method_31573(class_3489.field_42611)) {
            return false;
        } else {
            float sqrtProgress = class_3532.method_15355(swingProgress);
            float sinProgress = class_3532.method_15374(sqrtProgress * (float) Math.PI);
            String var7 = this.swingAnimation.getCurrentMode();
            switch (var7) {
                case "Slice":
                    this.applySliceAnimation(matrices, sinProgress);
                    break;
                case "Block":
                    this.applyBlockAnimation(matrices, sinProgress);
                    break;
                case "Chop":
                    this.applyChopAnimation(matrices, sinProgress);
                    break;
                case "Spin":
                    this.applySpinAnimation(matrices, swingProgress);
                    break;
                case "Pop":
                    this.applyPopAnimation(matrices, sinProgress);
            }

            return true;
        }
    }

    private void applySliceAnimation(class_4587 matrices, float sinProgress) {
        matrices.method_46416(0.27F, 0.2F, -0.16F);
        matrices.method_22907(class_7833.field_40714.rotationDegrees(-46.0F));
        matrices.method_22907(class_7833.field_40716.rotationDegrees(30.46F));
        matrices.method_22907(class_7833.field_40718.rotationDegrees(-46.0F));
        matrices.method_22907(class_7833.field_40714.rotationDegrees(-96.97F * sinProgress));
        matrices.method_22907(class_7833.field_40716.rotationDegrees(30.46F * sinProgress));
        matrices.method_22907(class_7833.field_40718.rotationDegrees(36.83F * sinProgress));
    }

    private void applyBlockAnimation(class_4587 matrices, float sinProgress) {
        matrices.method_22907(class_7833.field_40714.rotationDegrees(-84.23F));
        matrices.method_22907(class_7833.field_40716.rotationDegrees(-4.58F));
        matrices.method_22907(class_7833.field_40718.rotationDegrees(90.99F));
        matrices.method_22907(class_7833.field_40714.rotationDegrees(-23.7F * sinProgress));
        matrices.method_22907(class_7833.field_40716.rotationDegrees(52.76F * sinProgress));
        matrices.method_22907(class_7833.field_40718.rotationDegrees(8.16F * sinProgress));
    }

    private void applyChopAnimation(class_4587 matrices, float sinProgress) {
        matrices.method_46416(0.27F, 0.2F, -0.16F);
        matrices.method_22907(class_7833.field_40714.rotationDegrees(-20.5F));
        matrices.method_22907(class_7833.field_40716.rotationDegrees(-20.5F));
        matrices.method_22907(class_7833.field_40718.rotationDegrees(-11.0F));
        matrices.method_22907(class_7833.field_40714.rotationDegrees(-106.5F * sinProgress));
        matrices.method_22907(class_7833.field_40716.rotationDegrees(-23.7F * sinProgress));
        matrices.method_22907(class_7833.field_40718.rotationDegrees(8.2F * sinProgress));
    }

    private void applyPopAnimation(class_4587 matrices, float sinProgress) {
        matrices.method_46416(0.34F, 0.2F, -0.16F);
        matrices.method_22907(class_7833.field_40714.rotationDegrees(-90.6F));
        matrices.method_22907(class_7833.field_40716.rotationDegrees(4.97F));
        matrices.method_22907(class_7833.field_40718.rotationDegrees(52.76F));
        matrices.method_22907(class_7833.field_40714.rotationDegrees(8.16F * sinProgress));
        matrices.method_22907(class_7833.field_40716.rotationDegrees(30.46F * sinProgress));
        matrices.method_22907(class_7833.field_40718.rotationDegrees(-46.0F * sinProgress));
    }

    private void applySpinAnimation(class_4587 matrices, float progress) {
        matrices.method_46416(0.0F, 0.0F, 0.0F);
        matrices.method_22907(class_7833.field_40716.rotationDegrees(360.0F * progress));
    }
}
