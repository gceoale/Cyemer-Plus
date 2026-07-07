package com.slither.cyemer.module.implementation;

import com.slither.cyemer.gui.new_ui.ClickGUI;
import com.slither.cyemer.gui.new_ui.CustomCapeUploadScreen;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.ModeSetting;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.CapeTextureManager;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_437;

@Environment(EnvType.CLIENT)
public class CustomCapeModule extends Module {
    private static final String CUSTOM_MODE = "custom";
    public static CustomCapeModule INSTANCE;
    private final ModeSetting capeName = (ModeSetting)new ModeSetting("Cape", "cyemer", "weedhack", "astolfo", "kitty", "frieren", "snowoman", "custom")
        .onChange(() -> {
            if (this.isCustomMode()) {
                this.openCustomCapeScreenIfAvailable();
            }

            if (this.isEnabled()) {
                this.updateCape();
            }
        });
    private long lastCapeRefreshMillis = 0L;

    public CustomCapeModule() {
        super("CustomCape", "Renders a high-resolution custom cape.", Category.RENDER);
        this.addSetting(this.capeName);
        INSTANCE = this;
        CapeTextureManager.restorePersistedCustomCape();
    }

    public void updateCape() {
        CapeTextureManager.ensureCapeTexture(this.getSelectedCapeName());
    }

    public String getSelectedCapeName() {
        return this.capeName.getCurrentMode();
    }

    public boolean isCustomMode() {
        return "custom".equalsIgnoreCase(this.getSelectedCapeName());
    }

    public class_2960 getPreviewTextureIdentifier() {
        return CapeTextureManager.getPreviewTextureIdentifier(this.getSelectedCapeName());
    }

    public String getCustomCapePath() {
        return CapeTextureManager.getCustomCapeSourcePath();
    }

    public List<CapeTextureManager.CustomCapeEntryView> getCustomCapeLibrary() {
        return CapeTextureManager.getCustomCapeLibrary();
    }

    public String getActiveCustomCapeId() {
        return CapeTextureManager.getActiveCustomCapeId();
    }

    public CapeTextureManager.CustomCapeEntryView getActiveCustomCape() {
        return CapeTextureManager.getActiveCustomCape();
    }

    public boolean selectCustomCape(String entryId) {
        boolean selected = CapeTextureManager.selectCustomCape(entryId);
        if (selected) {
            this.capeName.setCurrentMode("custom");
            this.updateCape();
        }

        return selected;
    }

    public boolean deleteCustomCape(String entryId) {
        boolean deleted = CapeTextureManager.deleteCustomCape(entryId);
        if (deleted) {
            this.updateCape();
        }

        return deleted;
    }

    public CapeTextureManager.ImportResult importCustomCape(Path path, CapeTextureManager.FrameMode frameMode, float frameX, float frameY) {
        String requestedName = path != null && path.getFileName() != null ? path.getFileName().toString() : "Custom Cape";
        CapeTextureManager.ImportResult result = CapeTextureManager.importCustomCape(path, requestedName, frameMode, frameX, frameY);
        if (result.success()) {
            this.capeName.setCurrentMode("custom");
            this.updateCape();
        }

        return result;
    }

    public boolean setCustomCapeFromFile(Path path) {
        boolean loaded = CapeTextureManager.setCustomCapeFromFile(path);
        if (loaded) {
            this.capeName.setCurrentMode("custom");
            this.updateCape();
        }

        return loaded;
    }

    public void openCustomCapeScreenIfAvailable() {
        class_310 client = class_310.method_1551();
        if (client != null) {
            class_437 current = client.field_1755;
            if (current instanceof ClickGUI && !(current instanceof CustomCapeUploadScreen)) {
                client.method_1507(new CustomCapeUploadScreen(current, this));
            }
        }
    }

    @Override
    public void onEnable() {
        this.updateCape();
        this.lastCapeRefreshMillis = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onTick() {
        if (this.isEnabled()) {
            long now = System.currentTimeMillis();
            if (now - this.lastCapeRefreshMillis >= 2000L) {
                CapeTextureManager.ensureCapeTexture(this.getSelectedCapeName());
                this.lastCapeRefreshMillis = now;
            }
        }
    }

    @Override
    public void onRender(class_332 context, float tickDelta) {
    }
}
