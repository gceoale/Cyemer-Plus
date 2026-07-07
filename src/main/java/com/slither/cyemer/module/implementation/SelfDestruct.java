package com.slither.cyemer.module.implementation;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.module.BooleanSetting;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.StringSetting;
import com.slither.cyemer.util.DirectByteOverwriter;
import com.slither.cyemer.util.JarUpdater;
import com.slither.cyemer.util.LogCleaner;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class SelfDestruct extends Module {
    private final BooleanSetting scrambleMemory = new BooleanSetting("Scramble Memory", true);
    private final BooleanSetting replaceJar = new BooleanSetting("Replace Jar File", true);
    private final StringSetting replacementUrl = new StringSetting(
        "Replacement URL", "https://cdn.modrinth.com/data/LQ3K71Q1/versions/td4DfKSI/dynamic-fps-3.9.5%2Bminecraft-1.21.0-fabric.jar"
    );

    public SelfDestruct() {
        super("SelfDestruct", "Wipes the client from memory and replaces the JAR file.", Category.CLIENT);
        this.addSetting(this.scrambleMemory);
        this.addSetting(this.replaceJar);
        this.addSetting(this.replacementUrl);
    }

    private void scrambleClientMemory() {
        Cyemer.selfDestructed = true;
        List<Module> moduleSnapshot;
        synchronized (Cyemer.getInstance().getModuleManager().getModules()) {
            moduleSnapshot = new ArrayList<>(Cyemer.getInstance().getModuleManager().getModules());
        }

        for (Module module : moduleSnapshot) {
            if (module != this) {
                module.setEnabled(false);
                module.setName(" ");
                module.setDescription(" ");
            }
        }

        try {
            synchronized (Cyemer.getInstance().getModuleManager().getModules()) {
                Cyemer.getInstance().getModuleManager().getModules().clear();
            }
        } catch (Exception var7) {
        }

        LogCleaner.clean();

        for (int i = 0; i < 5; i++) {
            System.gc();

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException var5) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void onEnable() {
        if (this.mc.field_1755 != null) {
            this.mc.field_1755.method_25419();
        }

        new Thread(() -> {
            if (this.scrambleMemory.isEnabled()) {
                this.scrambleClientMemory();
            }

            if (this.replaceJar.isEnabled()) {
                File currentJarFile = JarUpdater.getCurrentJarFile();
                if (currentJarFile != null && currentJarFile.exists()) {
                    DirectByteOverwriter.overwriteWithBytes(this.replacementUrl.getValue(), currentJarFile);
                }
            }

            this.setEnabled(false);
        }).start();
    }
}
