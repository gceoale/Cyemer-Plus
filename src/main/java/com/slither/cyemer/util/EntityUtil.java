package com.slither.cyemer.util;

import java.util.Comparator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1657;
import net.minecraft.class_310;

@Environment(EnvType.CLIENT)
public class EntityUtil {
    public static final EntityUtil INSTANCE = new EntityUtil();
    private static final class_310 mc = class_310.method_1551();

    public class_1657 getPlayer() {
        return mc.field_1687 != null && mc.field_1724 != null
            ? mc.field_1687
                .method_18456()
                .stream()
                .filter(player -> player != mc.field_1724)
                .min(Comparator.comparing(player -> mc.field_1724.method_5739(player)))
                .orElse(null)
            : null;
    }
}
