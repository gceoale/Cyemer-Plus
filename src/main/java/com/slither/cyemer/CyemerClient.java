package com.slither.cyemer;

import com.slither.cyemer.gui.new_ui.ClickGUIIconCache;
import com.slither.cyemer.hud.HUDRenderer;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.rendering.font.MSDFFontRenderer;
import com.slither.cyemer.shader.CoreShaderManager;
import com.slither.cyemer.shader.PostShaderManager;
import com.slither.cyemer.util.LogCleaner;
import com.slither.cyemer.util.RenderBackendDetector;
import com.slither.cyemer.util.RotationManager;
import com.slither.cyemer.util.streamproof.OverlayCoordinator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.EndMain;
import net.minecraft.class_2960;
import net.minecraft.class_310;

@Environment(EnvType.CLIENT)
public class CyemerClient implements ClientModInitializer {
    private static final class_2960[] GUI_CURVE_SHADERS = new class_2960[]{
        class_2960.method_60655("dynamic_fps", "gui_panel"),
        class_2960.method_60655("dynamic_fps", "gui_panel_glass"),
        class_2960.method_60655("dynamic_fps", "gui_header"),
        class_2960.method_60655("dynamic_fps", "gui_header_glass"),
        class_2960.method_60655("dynamic_fps", "gui_row"),
        class_2960.method_60655("dynamic_fps", "gui_row_active"),
        class_2960.method_60655("dynamic_fps", "gui_row_enabled"),
        class_2960.method_60655("dynamic_fps", "gui_row_last"),
        class_2960.method_60655("dynamic_fps", "gui_search"),
        class_2960.method_60655("dynamic_fps", "gui_control"),
        class_2960.method_60655("dynamic_fps", "gui_tooltip"),
        class_2960.method_60655("dynamic_fps", "gui_hud"),
        class_2960.method_60655("dynamic_fps", "gui_glow"),
        class_2960.method_60655("dynamic_fps", "gui_particle"),
        class_2960.method_60655("dynamic_fps", "gui_particle_glow")
    };

    public void onInitializeClient() {
        this.registerCoreShaders();
        this.registerFonts();
        this.registerPostShaders();
        ClickGUIIconCache.preloadAllIcons();
        WorldRenderEvents.END_MAIN.register((EndMain)context -> {
            float tickDelta = class_310.method_1551().method_61966().method_60637(true);
            RotationManager.update(tickDelta);
            Cyemer.getInstance().getModuleManager().onWorldRender(context.matrices(), tickDelta);
            this.renderPostEffects();
        });
        HudRenderCallback hudRenderer = new HUDRenderer();
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            OverlayCoordinator.beginRedirect();
            try {
                hudRenderer.onHudRender(drawContext, tickDelta);
            } finally {
                OverlayCoordinator.endRedirect();
            }
        });
        HudRenderCallback fakelagCallback = (drawContext, tickDelta) -> {
            Module mod = Cyemer.getInstance().getModuleManager().getModule("Fakelag");
            if (mod != null && mod.isEnabled()) {
                try {
                    mod.getClass().getMethod("onHudRender", drawContext.getClass(), float.class).invoke(mod, drawContext, tickDelta);
                } catch (Exception var4) {
                }
            }
        };
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            OverlayCoordinator.beginRedirect();
            try {
                fakelagCallback.onHudRender(drawContext, tickDelta);
            } finally {
                OverlayCoordinator.endRedirect();
            }
        });
        LogCleaner.clean();
    }

    private void registerCoreShaders() {
        CoreShaderManager manager = CoreShaderManager.getInstance();
        manager.registerMsdfTextShader(class_2960.method_60655("dynamic_fps", "msdf_text"));
        manager.registerImageShader(class_2960.method_60655("dynamic_fps", "image"));
        manager.registerImageShader(class_2960.method_60655("dynamic_fps", "gui_icon"));
        this.registerGuiCurveShaders(manager);
        if (!RenderBackendDetector.isVulkanBackend()) {
            manager.registerCurveShader(class_2960.method_60655("dynamic_fps", "liquid_metal"));
            manager.registerPositionColorShader(class_2960.method_60655("dynamic_fps", "lava"));
            manager.registerPositionColorShader(class_2960.method_60655("dynamic_fps", "water"));
            manager.registerCurveShader(class_2960.method_60655("dynamic_fps", "curve"));
        }
    }

    private void registerGuiCurveShaders(CoreShaderManager manager) {
        for (class_2960 id : GUI_CURVE_SHADERS) {
            manager.registerCurveShader(id);
        }
    }

    private void registerFonts() {
        MSDFFontRenderer.preloadFont(class_2960.method_60655("dynamic_fps", "sans"));
        MSDFFontRenderer.preloadFont(class_2960.method_60655("dynamic_fps", "semibold"));
        MSDFFontRenderer.preloadFont(class_2960.method_60655("dynamic_fps", "cyemer"));
    }

    private void registerPostShaders() {
        PostShaderManager manager = PostShaderManager.getInstance();
    }

    private void renderPostEffects() {
        class_310 client = class_310.method_1551();
        PostShaderManager manager = PostShaderManager.getInstance();
        if (RenderBackendDetector.isVulkanBackend()) {
            manager.clearEffect();
        } else {
            manager.render(client.method_1522());
        }
    }
}
