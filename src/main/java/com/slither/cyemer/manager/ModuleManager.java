package com.slither.cyemer.manager;

import com.slither.cyemer.module.Module;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_4587;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ModuleManager {
    public static final String REGISTRY = "com.slither.cyemer.manager.ModuleRegistry";
    private volatile List<Module> modules;
    private final class_310 mc = class_310.method_1551();

    public ModuleManager() {
        this.modules = createModules(REGISTRY, ModuleManager.class.getClassLoader(), type -> null);
    }

    /**
     * The module list, built by ModuleRegistry. It is looked up by name on
     * purpose: a direct reference would tie every module to the core and
     * stop HotReload from swapping them in a running game.
     */
    @SuppressWarnings("unchecked")
    public static List<Module> createModules(String registryClass, ClassLoader loader, Function<Class<?>, Module> existing) {
        try {
            Class<?> registry = Class.forName(registryClass, true, loader);
            return new ArrayList<>((List<Module>) registry.getMethod("create", Function.class).invoke(null, existing));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("could not build the module list from " + registryClass, e);
        }
    }

    /** Swaps in a new module list. Readers keep whichever list they already have, so nothing iterating breaks. */
    public void replaceModules(List<Module> fresh) {
        this.modules = new ArrayList<>(fresh);
    }

    public void onTick() {
        TargetManager.update();

        for (Module module : new ArrayList<>(this.modules)) {
            if (module.isEnabled() || module.getName().equals("FullBright") || module.getName().equals("Booster")) {
                module.onTick();
            }
        }
    }

    public void onTickPost() {
        for (Module module : new ArrayList<>(this.modules)) {
            if (module.isEnabled() || module.getName().equals("FullBright") || module.getName().equals("Booster")) {
                module.onTickPost();
            }
        }
    }

    public void onRender(class_332 context, float tickDelta) {
        for (Module module : this.getModules()) {
            if (module.isEnabled()) {
                try {
                    module.onRender(context, tickDelta);
                } catch (Throwable var8) {
                    try {
                        module.setEnabled(false);
                    } catch (Throwable var7) {
                    }
                }
            }
        }
    }

    public void onMs() {
        for (Module module : this.modules) {
            if (module.isEnabled()) {
                module.onMs();
            }
        }
    }

    public void onWorldRender(class_4587 matrices, float tickDelta) {
        for (Module module : this.getModules()) {
            if (module.isEnabled()) {
                try {
                    class_4587 isolatedMatrices = new class_4587();
                    isolatedMatrices.method_34425(matrices.method_23760().method_23761());
                    module.onWorldRender(isolatedMatrices, tickDelta);
                } catch (Throwable var8) {
                    try {
                        module.setEnabled(false);
                    } catch (Throwable var7) {
                    }
                }
            }
        }
    }

    public List<Module> getModules() {
        return this.modules;
    }

    @Nullable
    public Module getModule(String name) {
        for (Module module : this.modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }

        return null;
    }
}
