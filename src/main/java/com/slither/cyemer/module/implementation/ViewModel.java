package com.slither.cyemer.module.implementation;

import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.SliderSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_4587;

@Environment(EnvType.CLIENT)
public class ViewModel extends Module {
    private static ViewModel instance;
    private final ModeSetting applyTo = new ModeSetting("Apply To", "Item Only", "Both");
    private final SliderSetting positionX = new SliderSetting("Position X", 0.0, -2.0, 2.0, 2);
    private final SliderSetting positionY = new SliderSetting("Position Y", 0.0, -2.0, 2.0, 2);
    private final SliderSetting positionZ = new SliderSetting("Position Z", 0.0, -2.0, 2.0, 2);
    private final SliderSetting scaleX = new SliderSetting("Scale X", 1.0, 0.1, 3.0, 2);
    private final SliderSetting scaleY = new SliderSetting("Scale Y", 1.0, 0.1, 3.0, 2);
    private final SliderSetting scaleZ = new SliderSetting("Scale Z", 1.0, 0.1, 3.0, 2);

    public ViewModel() {
        super("ViewModel", "Changes the size and position of your held item.", Category.RENDER);
        this.addSetting(this.applyTo);
        this.addSetting(this.positionX);
        this.addSetting(this.positionY);
        this.addSetting(this.positionZ);
        this.addSetting(this.scaleX);
        this.addSetting(this.scaleY);
        this.addSetting(this.scaleZ);
        instance = this;
    }

    public static ViewModel getInstance() {
        return instance;
    }

    public String getApplyToMode() {
        return this.applyTo.getCurrentMode();
    }

    public void applyTransformations(class_4587 matrices) {
        float posX = (float)this.positionX.getValue();
        float posY = (float)this.positionY.getValue();
        float posZ = (float)this.positionZ.getValue();
        float scaX = (float)this.scaleX.getValue();
        float scaY = (float)this.scaleY.getValue();
        float scaZ = (float)this.scaleZ.getValue();
        matrices.method_46416(posX, posY, posZ);
        matrices.method_22905(scaX, scaY, scaZ);
    }
}
