package com.slither.cyemer.util;

import com.mojang.authlib.GameProfile;
import java.lang.reflect.Method;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class GameProfileCompat {
    private static final Method ID_METHOD = findAccessor(GameProfile.class, "getId", "id");
    private static final Method NAME_METHOD = findAccessor(GameProfile.class, "getName", "name");

    private GameProfileCompat() {
    }

    private static Method findAccessor(Class<?> clazz, String legacyName, String modernName) {
        try {
            Method m = clazz.getMethod(legacyName);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException var5) {
            try {
                Method mx = clazz.getMethod(modernName);
                mx.setAccessible(true);
                return mx;
            } catch (NoSuchMethodException var4) {
                return null;
            }
        }
    }

    public static UUID getId(GameProfile profile) {
        if (profile != null && ID_METHOD != null) {
            try {
                return ID_METHOD.invoke(profile) instanceof UUID uuid ? uuid : null;
            } catch (ReflectiveOperationException var3) {
                return null;
            }
        } else {
            return null;
        }
    }

    public static String getName(GameProfile profile) {
        if (profile != null && NAME_METHOD != null) {
            try {
                return NAME_METHOD.invoke(profile) instanceof String s ? s : null;
            } catch (ReflectiveOperationException var3) {
                return null;
            }
        } else {
            return null;
        }
    }
}
