package com.slither.cyemer;

import com.slither.cyemer.config.ConfigManager;
import com.slither.cyemer.hud.ClientEventHandler;
import com.slither.cyemer.hud.HUDManager;
import com.slither.cyemer.manager.ModuleManager;
import java.util.Timer;
import java.util.TimerTask;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;

@Environment(EnvType.CLIENT)
public class Cyemer implements ClientModInitializer {
    public static final String MOD_ID = "dynamic_fps";
    public static Cyemer INSTANCE;
    public ModuleManager moduleManager;
    public ConfigManager configManager;
    private HUDManager hudManager;
    public final class_310 mc = class_310.method_1551();
    public static boolean selfDestructed = false;
    private Timer msTimer;

    public void onInitializeClient() {
        INSTANCE = this;
        this.moduleManager = new ModuleManager();
        this.configManager = new ConfigManager();
        this.hudManager = HUDManager.getInstance();
        ClientEventHandler.init();
        this.configManager.load("default");
        this.startMsTimer();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (this.msTimer != null) {
                this.msTimer.cancel();
            }

            this.configManager.save("default");
        }));
    }

    private void startMsTimer() {
        this.msTimer = new Timer("msTimer", true);
        this.msTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Cyemer.this.moduleManager.onMs();
            }
        }, 0L, 1L);
    }

    public static Cyemer getInstance() {
        return INSTANCE;
    }

    public ModuleManager getModuleManager() {
        return this.moduleManager;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public HUDManager getHudManager() {
        return this.hudManager;
    }
}
