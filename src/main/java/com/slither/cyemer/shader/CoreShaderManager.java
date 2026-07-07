package com.slither.cyemer.shader;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.class_5596;
import com.slither.cyemer.util.RenderBackendDetector;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10789;
import net.minecraft.class_10799;
import net.minecraft.class_284;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_5944;

@Environment(EnvType.CLIENT)
public final class CoreShaderManager {
    private static final class_2960 MSDF_TEXT_PIPELINE_ID = class_2960.method_60655("dynamic_fps", "msdf_text");
    private static final class_2960 IMAGE_PIPELINE_ID = class_2960.method_60655("dynamic_fps", "image");
    private static final Set<class_2960> VULKAN_PIPELINES = Set.of(
        MSDF_TEXT_PIPELINE_ID,
        IMAGE_PIPELINE_ID,
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
        class_2960.method_60655("dynamic_fps", "gui_icon"),
        class_2960.method_60655("dynamic_fps", "gui_tooltip"),
        class_2960.method_60655("dynamic_fps", "gui_hud"),
        class_2960.method_60655("dynamic_fps", "gui_glow"),
        class_2960.method_60655("dynamic_fps", "gui_particle"),
        class_2960.method_60655("dynamic_fps", "gui_particle_glow")
    );
    private static CoreShaderManager INSTANCE;
    private final Map<class_2960, RenderPipeline> customPipelines = new HashMap<>();
    private final Map<class_2960, class_5944> shaderPrograms = new HashMap<>();

    private CoreShaderManager() {
    }

    public static CoreShaderManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CoreShaderManager();
        }

        return INSTANCE;
    }

    private static boolean supportsPipeline(class_2960 id) {
        return !RenderBackendDetector.isVulkanBackend() ? true : VULKAN_PIPELINES.contains(id);
    }

    public void registerPositionColorShader(class_2960 id) {
        if (supportsPipeline(id)) {
            RenderPipeline pipeline = RenderPipeline.builder(new Snippet[0])
                .withLocation(class_2960.method_60655(id.method_12836(), "pipeline/" + id.method_12832()))
                .withVertexShader(class_2960.method_60655(id.method_12836(), "core/" + id.method_12832()))
                .withFragmentShader(class_2960.method_60655(id.method_12836(), "core/" + id.method_12832()))
                .withUniform("DynamicTransforms", class_10789.field_60031)
                .withUniform("Projection", class_10789.field_60031)
                .withVertexFormat(class_290.field_1576, class_5596.field_27382)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthWrite(false)
                .build();
            class_10799.method_67887(pipeline);
            this.customPipelines.put(id, pipeline);
        }
    }

    public void registerPositionTextureColorShader(class_2960 id) {
        if (supportsPipeline(id)) {
            RenderPipeline pipeline = RenderPipeline.builder(new Snippet[0])
                .withLocation(class_2960.method_60655(id.method_12836(), "pipeline/" + id.method_12832()))
                .withVertexShader(class_2960.method_60655(id.method_12836(), "core/" + id.method_12832()))
                .withFragmentShader(class_2960.method_60655(id.method_12836(), "core/" + id.method_12832()))
                .withUniform("DynamicTransforms", class_10789.field_60031)
                .withUniform("Projection", class_10789.field_60031)
                .withSampler("Sampler0")
                .withVertexFormat(class_290.field_1575, class_5596.field_27382)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthWrite(false)
                .build();
            class_10799.method_67887(pipeline);
            this.customPipelines.put(id, pipeline);
        }
    }

    public void registerPositionShader(class_2960 id) {
        if (supportsPipeline(id)) {
            RenderPipeline pipeline = RenderPipeline.builder(new Snippet[0])
                .withLocation(class_2960.method_60655(id.method_12836(), "pipeline/" + id.method_12832()))
                .withVertexShader(class_2960.method_60655(id.method_12836(), "core/" + id.method_12832()))
                .withFragmentShader(class_2960.method_60655(id.method_12836(), "core/" + id.method_12832()))
                .withUniform("DynamicTransforms", class_10789.field_60031)
                .withUniform("Projection", class_10789.field_60031)
                .withVertexFormat(class_290.field_1592, class_5596.field_27382)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthWrite(false)
                .build();
            class_10799.method_67887(pipeline);
            this.customPipelines.put(id, pipeline);
        }
    }

    public void registerCurveShader(class_2960 id) {
        if (supportsPipeline(id)) {
            RenderPipeline pipeline = RenderPipeline.builder(new Snippet[0])
                .withLocation(class_2960.method_60655(id.method_12836(), "pipeline/" + id.method_12832()))
                .withVertexShader(class_2960.method_60655(id.method_12836(), "core/" + id.method_12832()))
                .withFragmentShader(class_2960.method_60655(id.method_12836(), "core/" + id.method_12832()))
                .withUniform("DynamicTransforms", class_10789.field_60031)
                .withUniform("Projection", class_10789.field_60031)
                .withVertexFormat(class_290.field_1575, class_5596.field_27382)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthWrite(false)
                .build();
            class_10799.method_67887(pipeline);
            this.customPipelines.put(id, pipeline);
        }
    }

    public void registerMsdfTextShader(class_2960 id) {
        if (supportsPipeline(id)) {
            RenderPipeline pipeline = RenderPipeline.builder(new Snippet[0])
                .withLocation(class_2960.method_60655(id.method_12836(), "pipeline/" + id.method_12832()))
                .withVertexShader(class_2960.method_60655(id.method_12836(), "core/" + id.method_12832()))
                .withFragmentShader(class_2960.method_60655(id.method_12836(), "core/" + id.method_12832()))
                .withUniform("DynamicTransforms", class_10789.field_60031)
                .withUniform("Projection", class_10789.field_60031)
                .withSampler("Sampler0")
                .withVertexFormat(class_290.field_1575, class_5596.field_27382)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthWrite(false)
                .build();
            class_10799.method_67887(pipeline);
            this.customPipelines.put(id, pipeline);
        }
    }

    public void registerImageShader(class_2960 id) {
        if (supportsPipeline(id)) {
            RenderPipeline pipeline = RenderPipeline.builder(new Snippet[0])
                .withLocation(class_2960.method_60655(id.method_12836(), "pipeline/" + id.method_12832()))
                .withVertexShader(class_2960.method_60655(id.method_12836(), "core/" + id.method_12832()))
                .withFragmentShader(class_2960.method_60655(id.method_12836(), "core/" + id.method_12832()))
                .withUniform("DynamicTransforms", class_10789.field_60031)
                .withUniform("Projection", class_10789.field_60031)
                .withSampler("Sampler0")
                .withVertexFormat(class_290.field_1575, class_5596.field_27382)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthWrite(false)
                .build();
            class_10799.method_67887(pipeline);
            this.customPipelines.put(id, pipeline);
        }
    }

    public void registerCurveGradientShader(class_2960 id) {
        if (supportsPipeline(id)) {
            RenderPipeline pipeline = RenderPipeline.builder(new Snippet[0])
                .withLocation(class_2960.method_60655(id.method_12836(), "pipeline/" + id.method_12832()))
                .withVertexShader(class_2960.method_60655(id.method_12836(), "core/" + id.method_12832()))
                .withFragmentShader(class_2960.method_60655(id.method_12836(), "core/" + id.method_12832()))
                .withUniform("DynamicTransforms", class_10789.field_60031)
                .withUniform("Projection", class_10789.field_60031)
                .withVertexFormat(class_290.field_1575, class_5596.field_27382)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthWrite(false)
                .build();
            class_10799.method_67887(pipeline);
            this.customPipelines.put(id, pipeline);
        }
    }

    public void setUniformFloat(class_2960 id, String name, float value) {
        RenderPipeline pipeline = this.customPipelines.get(id);
        if (pipeline != null) {
            RenderSystem.assertOnRenderThread();
        }
    }

    public void setShaderUniforms(class_2960 id, float time, float[] color, float speed) {
        class_5944 program = this.shaderPrograms.get(id);
        if (program != null) {
            RenderSystem.assertOnRenderThread();
            class_284 timeUniform = program.method_34582("Time");
            if (timeUniform != null) {
            }

            class_284 colorUniform = program.method_34582("CustomColor");
            if (colorUniform != null) {
            }

            class_284 speedUniform = program.method_34582("Speed");
            if (speedUniform != null) {
            }
        }
    }

    public RenderPipeline getPipeline(class_2960 id) {
        return !supportsPipeline(id) ? null : this.customPipelines.get(id);
    }

    public class_5944 getShaderProgram(class_2960 id) {
        return this.shaderPrograms.get(id);
    }

    public void clear() {
        this.customPipelines.clear();
        this.shaderPrograms.clear();
    }
}
