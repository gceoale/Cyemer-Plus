#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;

out vec4 vertexColor;
out float clipDepth;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    vec4 clipPos = ProjMat * viewPos;
    gl_Position = clipPos;
    vertexColor = Color;
    clipDepth = clamp(clipPos.z / max(abs(clipPos.w), 1e-5), -1.0, 1.0);
}
