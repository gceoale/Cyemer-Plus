package com.slither.cyemer.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum AimTarget {
    HEAD,
    CHEST,
    LEGS,
    MANUAL;
}
