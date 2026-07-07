package com.slither.cyemer.module;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_3532;

@Environment(EnvType.CLIENT)
public class SliderSetting extends Setting {
    private double value;
    private final double min;
    private final double max;
    private final int decimalPlaces;

    public SliderSetting(String name, double defaultValue, double min, double max, int decimalPlaces) {
        super(name);
        this.min = min;
        this.max = max;
        this.decimalPlaces = decimalPlaces;
        this.setValue(defaultValue);
    }

    public double getValue() {
        return this.round(this.value);
    }

    public double getPreciseValue() {
        return this.value;
    }

    public void setValue(double value) {
        this.value = class_3532.method_15350(value, this.min, this.max);
    }

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }

    public String getValueAsString() {
        return String.format("%." + this.decimalPlaces + "f", this.getValue());
    }

    private double round(double value) {
        return this.decimalPlaces <= 0 ? Math.round(value) : new BigDecimal(value).setScale(this.decimalPlaces, RoundingMode.HALF_UP).doubleValue();
    }
}
