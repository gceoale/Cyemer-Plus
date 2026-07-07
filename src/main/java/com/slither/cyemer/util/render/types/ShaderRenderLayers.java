package com.slither.cyemer.util.render.types;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.class_5596;
import com.slither.cyemer.util.RenderBackendDetector;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10799;
import net.minecraft.class_12247;
import net.minecraft.class_12249;
import net.minecraft.class_1921;
import net.minecraft.class_290;
import net.minecraft.class_2960;

@Environment(EnvType.CLIENT)
public final class ShaderRenderLayers {
    private static final class_2960 POSITION_COLOR_SHADER = class_2960.method_60655("minecraft", "core/position_color");
    private static final class_2960 ESP_SURFACE_SHADER = class_2960.method_60655("dynamic_fps", "core/esp_surface");
    private static final RenderPipeline POSITION_COLOR_TRIANGLES_GLOW = class_10799.method_67887(
        RenderPipeline.builder(new Snippet[]{class_10799.field_60125, class_10799.field_60126})
            .withLocation(class_2960.method_60655("dynamic_fps", "cyemer_position_color_triangles_glow"))
            .withVertexShader(POSITION_COLOR_SHADER)
            .withFragmentShader(POSITION_COLOR_SHADER)
            .withVertexFormat(class_290.field_1576, class_5596.field_27379)
            .withCull(false)
            .withBlend(BlendFunction.LIGHTNING)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build()
    );
    private static final class_1921 GLOW_LAYER_NO_DEPTH = class_1921.method_75940(
        "cyemer_shader_glow_nd", class_12247.method_75927(POSITION_COLOR_TRIANGLES_GLOW).method_75929(256).method_75937().method_75938()
    );
    private static final RenderPipeline ESP_TRIANGLES_DEPTH = class_10799.method_67887(
        RenderPipeline.builder(new Snippet[]{class_10799.field_60125, class_10799.field_60126})
            .withLocation(class_2960.method_60655("dynamic_fps", "cyemer_esp_triangles_depth"))
            .withVertexShader(ESP_SURFACE_SHADER)
            .withFragmentShader(ESP_SURFACE_SHADER)
            .withVertexFormat(class_290.field_1576, class_5596.field_27379)
            .withCull(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .build()
    );
    private static final RenderPipeline ESP_TRIANGLES_NO_DEPTH = class_10799.method_67887(
        RenderPipeline.builder(new Snippet[]{class_10799.field_60125, class_10799.field_60126})
            .withLocation(class_2960.method_60655("dynamic_fps", "cyemer_esp_triangles_no_depth"))
            .withVertexShader(ESP_SURFACE_SHADER)
            .withFragmentShader(ESP_SURFACE_SHADER)
            .withVertexFormat(class_290.field_1576, class_5596.field_27379)
            .withCull(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build()
    );
    private static final class_1921 ESP_LAYER_DEPTH = class_1921.method_75940(
        "cyemer_esp_depth", class_12247.method_75927(ESP_TRIANGLES_DEPTH).method_75929(1536).method_75937().method_75938()
    );
    private static final class_1921 ESP_LAYER_NO_DEPTH = class_1921.method_75940(
        "cyemer_esp_no_depth", class_12247.method_75927(ESP_TRIANGLES_NO_DEPTH).method_75929(1536).method_75937().method_75938()
    );

    private ShaderRenderLayers() {
    }

    public static class_1921 getShaderLayerNoDepth(String shaderName, boolean isGlow) {
        return isGlow ? GLOW_LAYER_NO_DEPTH : RenderTypes.TRIANGLES_NO_DEPTH;
    }

    public static class_1921 getShaderLayer(String shaderName, boolean isGlow) {
        return isGlow ? GLOW_LAYER_NO_DEPTH : RenderTypes.TRIANGLES;
    }

    public static class_1921 getShaderEntityLayer(String shaderName, class_2960 texture, boolean isGlow) {
        return isGlow ? class_12249.method_76002(texture) : class_12249.method_76000(texture);
    }

    public static class_1921 getEspLayer(boolean seeThrough) {
        if (RenderBackendDetector.isVulkanBackend()) {
            return seeThrough ? RenderTypes.TRIANGLES_NO_DEPTH : RenderTypes.TRIANGLES;
        } else {
            return seeThrough ? ESP_LAYER_NO_DEPTH : ESP_LAYER_DEPTH;
        }
    }
}
