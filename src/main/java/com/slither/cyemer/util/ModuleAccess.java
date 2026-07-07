package com.slither.cyemer.util;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.module.Module;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ModuleAccess {
    private ModuleAccess() {
    }

    public static Module getModule(String moduleName) {
        if (moduleName != null && !moduleName.isBlank()) {
            try {
                Cyemer cyemer = Cyemer.getInstance();
                return cyemer != null && cyemer.getModuleManager() != null ? cyemer.getModuleManager().getModule(moduleName) : null;
            } catch (Throwable var2) {
                return null;
            }
        } else {
            return null;
        }
    }

    public static boolean isEnabled(String moduleName) {
        Module module = getModule(moduleName);
        return module != null && module.isEnabled();
    }

    public static Object readField(Module module, String fieldName) {
        if (module != null && fieldName != null && !fieldName.isBlank()) {
            try {
                Field field;
                try {
                    field = module.getClass().getField(fieldName);
                } catch (NoSuchFieldException var4) {
                    field = module.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                }

                return field.get(module);
            } catch (Throwable var5) {
                return null;
            }
        } else {
            return null;
        }
    }

    public static boolean readBooleanField(Module module, String fieldName, boolean fallback) {
        return readField(module, fieldName) instanceof Boolean b ? b : fallback;
    }

    public static Object invoke(Module module, String methodName, Class<?>[] parameterTypes, Object... args) {
        return module != null && methodName != null && !methodName.isBlank() ? invokeOn(module, methodName, parameterTypes, args) : null;
    }

    public static Object invokeOn(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        if (target != null && methodName != null && !methodName.isBlank()) {
            try {
                Method method = resolveMethod(target.getClass(), methodName, parameterTypes);
                return method == null ? null : method.invoke(target, args);
            } catch (Throwable var5) {
                return null;
            }
        } else {
            return null;
        }
    }

    public static boolean invokeBoolean(Module module, String methodName, boolean fallback, Class<?>[] parameterTypes, Object... args) {
        return invoke(module, methodName, parameterTypes, args) instanceof Boolean b ? b : fallback;
    }

    public static double invokeDouble(Module module, String methodName, double fallback, Class<?>[] parameterTypes, Object... args) {
        return invoke(module, methodName, parameterTypes, args) instanceof Number number ? number.doubleValue() : fallback;
    }

    public static String invokeString(Module module, String methodName, String fallback, Class<?>[] parameterTypes, Object... args) {
        return invoke(module, methodName, parameterTypes, args) instanceof String str ? str : fallback;
    }

    public static boolean invokeBooleanOn(Object target, String methodName, boolean fallback, Class<?>[] parameterTypes, Object... args) {
        return invokeOn(target, methodName, parameterTypes, args) instanceof Boolean b ? b : fallback;
    }

    public static Object invokeStatic(Module module, String className, String methodName, Class<?>[] parameterTypes, Object... args) {
        if (className != null && !className.isBlank() && methodName != null && !methodName.isBlank()) {
            try {
                Class<?> clazz = loadClass(module, className);
                if (clazz == null) {
                    return null;
                } else {
                    Method method = resolveMethod(clazz, methodName, parameterTypes);
                    return method == null ? null : method.invoke(null, args);
                }
            } catch (Throwable var7) {
                return null;
            }
        } else {
            return null;
        }
    }

    public static boolean invokeStaticBoolean(Module module, String className, String methodName, boolean fallback, Class<?>[] parameterTypes, Object... args) {
        return invokeStatic(module, className, methodName, parameterTypes, args) instanceof Boolean b ? b : fallback;
    }

    public static Class<?> loadClass(Module module, String className) {
        if (className != null && !className.isBlank()) {
            ClassLoader moduleLoader = module != null ? module.getClass().getClassLoader() : null;
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            ClassLoader fallbackLoader = ModuleAccess.class.getClassLoader();
            Class<?> loaded = tryLoadClass(className, moduleLoader);
            if (loaded != null) {
                return loaded;
            } else {
                loaded = tryLoadClass(className, contextLoader);
                return loaded != null ? loaded : tryLoadClass(className, fallbackLoader);
            }
        } else {
            return null;
        }
    }

    private static Class<?> tryLoadClass(String className, ClassLoader loader) {
        if (loader == null) {
            return null;
        } else {
            try {
                return Class.forName(className, false, loader);
            } catch (Throwable var3) {
                return null;
            }
        }
    }

    private static Method resolveMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        Class<?>[] safeParams = parameterTypes == null ? new Class[0] : parameterTypes;

        try {
            return clazz.getMethod(methodName, safeParams);
        } catch (NoSuchMethodException var7) {
            try {
                Method method = clazz.getDeclaredMethod(methodName, safeParams);
                method.setAccessible(true);
                return method;
            } catch (Throwable var6) {
                return null;
            }
        }
    }
}
