package com.slither.cyemer.util.render.types;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.class_5596;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_10799;
import net.minecraft.class_12247;
import net.minecraft.class_1921;
import net.minecraft.class_290;
import net.minecraft.class_2960;

@Environment(EnvType.CLIENT)
public final class RenderTypes {
    private static final class_2960 POSITION_COLOR_SHADER = class_2960.method_60655("minecraft", "core/position_color");
    private static final RenderPipeline POSITION_COLOR_TRIANGLES_SEE_THROUGH = class_10799.method_67887(
        RenderPipeline.builder(new Snippet[]{class_10799.field_60125, class_10799.field_60126})
            .withLocation(class_2960.method_60655("dynamic_fps", "cyemer_position_color_triangles_see_through"))
            .withVertexShader(POSITION_COLOR_SHADER)
            .withFragmentShader(POSITION_COLOR_SHADER)
            .withVertexFormat(class_290.field_1576, class_5596.field_27379)
            .withCull(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build()
    );
    private static final RenderPipeline POSITION_COLOR_TRIANGLES_DEPTH = class_10799.method_67887(
        RenderPipeline.builder(new Snippet[]{class_10799.field_60125, class_10799.field_60126})
            .withLocation(class_2960.method_60655("dynamic_fps", "cyemer_position_color_triangles_depth"))
            .withVertexShader(POSITION_COLOR_SHADER)
            .withFragmentShader(POSITION_COLOR_SHADER)
            .withVertexFormat(class_290.field_1576, class_5596.field_27379)
            .withCull(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .build()
    );
    private static final RenderPipeline POSITION_COLOR_LINES_SEE_THROUGH = class_10799.method_67887(
        RenderPipeline.builder(new Snippet[]{class_10799.field_60125, class_10799.field_60126})
            .withLocation(class_2960.method_60655("dynamic_fps", "cyemer_position_color_lines_see_through"))
            .withVertexShader(POSITION_COLOR_SHADER)
            .withFragmentShader(POSITION_COLOR_SHADER)
            .withVertexFormat(class_290.field_1576, class_5596.field_27377)
            .withCull(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build()
    );
    private static final RenderPipeline POSITION_COLOR_LINES_DEPTH = class_10799.method_67887(
        RenderPipeline.builder(new Snippet[]{class_10799.field_60125, class_10799.field_60126})
            .withLocation(class_2960.method_60655("dynamic_fps", "cyemer_position_color_lines_depth"))
            .withVertexShader(POSITION_COLOR_SHADER)
            .withFragmentShader(POSITION_COLOR_SHADER)
            .withVertexFormat(class_290.field_1576, class_5596.field_27377)
            .withCull(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .build()
    );
    public static final class_1921 TRIANGLES = class_1921.method_75940(
        "cyemer_triangles", class_12247.method_75927(POSITION_COLOR_TRIANGLES_DEPTH).method_75929(1536).method_75937().method_75938()
    );
    public static final class_1921 TRIANGLES_NO_DEPTH = class_1921.method_75940(
        "cyemer_triangles_no_depth", class_12247.method_75927(POSITION_COLOR_TRIANGLES_SEE_THROUGH).method_75929(1536).method_75937().method_75938()
    );
    public static final class_1921 LINES = class_1921.method_75940(
        "cyemer_lines", class_12247.method_75927(POSITION_COLOR_LINES_DEPTH).method_75929(1536).method_75937().method_75938()
    );
    public static final class_1921 LINES_NO_DEPTH = class_1921.method_75940(
        "cyemer_lines_no_depth", class_12247.method_75927(POSITION_COLOR_LINES_SEE_THROUGH).method_75929(1536).method_75937().method_75938()
    );

    private RenderTypes() {
    }

    public static class_1921 getLinesWithWidth(double width) {
        return LINES;
    }

    public static void setupNoDepth() {
    }

    public static void cleanupNoDepth() {
    }
}
