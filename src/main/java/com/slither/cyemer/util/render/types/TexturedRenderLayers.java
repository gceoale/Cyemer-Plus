package com.slither.cyemer.util.render.types;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.class_5596;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10799;
import net.minecraft.class_12247;
import net.minecraft.class_1921;
import net.minecraft.class_290;
import net.minecraft.class_2960;

@Environment(EnvType.CLIENT)
public final class TexturedRenderLayers {
    private static final class_2960 POSITION_TEX_COLOR_SHADER = class_2960.method_60655("minecraft", "core/position_tex_color");
    private static final RenderPipeline TEX_COLOR_TRIANGLES_NO_DEPTH_ALPHA = class_10799.method_67887(
        RenderPipeline.builder(new Snippet[]{class_10799.field_60125, class_10799.field_60126})
            .withLocation(class_2960.method_60655("dynamic_fps", "cyemer_tex_color_triangles_nd_alpha"))
            .withVertexShader(POSITION_TEX_COLOR_SHADER)
            .withFragmentShader(POSITION_TEX_COLOR_SHADER)
            .withVertexFormat(class_290.field_1575, class_5596.field_27379)
            .withCull(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build()
    );
    private static final RenderPipeline TEX_COLOR_TRIANGLES_NO_DEPTH_ADDITIVE = class_10799.method_67887(
        RenderPipeline.builder(new Snippet[]{class_10799.field_60125, class_10799.field_60126})
            .withLocation(class_2960.method_60655("dynamic_fps", "cyemer_tex_color_triangles_nd_add"))
            .withVertexShader(POSITION_TEX_COLOR_SHADER)
            .withFragmentShader(POSITION_TEX_COLOR_SHADER)
            .withVertexFormat(class_290.field_1575, class_5596.field_27379)
            .withCull(false)
            .withBlend(BlendFunction.LIGHTNING)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build()
    );
    private static final RenderPipeline TEX_COLOR_TRIANGLES_DEPTH_ALPHA = class_10799.method_67887(
        RenderPipeline.builder(new Snippet[]{class_10799.field_60125, class_10799.field_60126})
            .withLocation(class_2960.method_60655("dynamic_fps", "cyemer_tex_color_triangles_depth_alpha"))
            .withVertexShader(POSITION_TEX_COLOR_SHADER)
            .withFragmentShader(POSITION_TEX_COLOR_SHADER)
            .withVertexFormat(class_290.field_1575, class_5596.field_27379)
            .withCull(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .build()
    );
    private static final RenderPipeline TEX_COLOR_TRIANGLES_DEPTH_ADDITIVE = class_10799.method_67887(
        RenderPipeline.builder(new Snippet[]{class_10799.field_60125, class_10799.field_60126})
            .withLocation(class_2960.method_60655("dynamic_fps", "cyemer_tex_color_triangles_depth_add"))
            .withVertexShader(POSITION_TEX_COLOR_SHADER)
            .withFragmentShader(POSITION_TEX_COLOR_SHADER)
            .withVertexFormat(class_290.field_1575, class_5596.field_27379)
            .withCull(false)
            .withBlend(BlendFunction.LIGHTNING)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .build()
    );
    private static final Map<class_2960, class_1921> LAYER_ND_ALPHA = new ConcurrentHashMap<>();
    private static final Map<class_2960, class_1921> LAYER_ND_ADD = new ConcurrentHashMap<>();
    private static final Map<class_2960, class_1921> LAYER_DEPTH_ALPHA = new ConcurrentHashMap<>();
    private static final Map<class_2960, class_1921> LAYER_DEPTH_ADD = new ConcurrentHashMap<>();

    private TexturedRenderLayers() {
    }

    public static class_1921 getTexturedLayer(class_2960 texture, boolean additive) {
        Map<class_2960, class_1921> cache = additive ? LAYER_ND_ADD : LAYER_ND_ALPHA;
        RenderPipeline pipeline = additive ? TEX_COLOR_TRIANGLES_NO_DEPTH_ADDITIVE : TEX_COLOR_TRIANGLES_NO_DEPTH_ALPHA;
        return cache.computeIfAbsent(texture, id -> createLayer("cyemer_textured_effect_" + (additive ? "add" : "alpha"), pipeline, id));
    }

    public static class_1921 getTexturedLayerNoDepth(class_2960 texture, boolean additive) {
        Map<class_2960, class_1921> cache = additive ? LAYER_DEPTH_ADD : LAYER_DEPTH_ALPHA;
        RenderPipeline pipeline = additive ? TEX_COLOR_TRIANGLES_DEPTH_ADDITIVE : TEX_COLOR_TRIANGLES_DEPTH_ALPHA;
        return cache.computeIfAbsent(texture, id -> createLayer("cyemer_textured_depth_" + (additive ? "add" : "alpha"), pipeline, id));
    }

    public static class_1921 getTexturedGlowLayer(class_2960 texture) {
        return getTexturedLayer(texture, true);
    }

    private static class_1921 createLayer(String name, RenderPipeline pipeline, class_2960 texture) {
        return class_1921.method_75940(
            name, class_12247.method_75927(pipeline).method_75934("Sampler0", texture).method_75929(1536).method_75937().method_75938()
        );
    }
}
