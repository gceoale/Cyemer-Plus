#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec4 vertexColor;
out vec2 texCoord0;
flat out float packedRadiusPx;

const float RADIUS_PACK_STRIDE = 2.0;
const float RADIUS_PACK_SCALE = 4.0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    float packedRadius = floor(UV0.x / RADIUS_PACK_STRIDE + 1e-4);
    texCoord0 = vec2(UV0.x - packedRadius * RADIUS_PACK_STRIDE, UV0.y);
    packedRadiusPx = packedRadius / RADIUS_PACK_SCALE;
    vertexColor = Color;
}
