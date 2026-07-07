#version 330

#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform PixelateConfig {
    float Intensity;
};

out vec4 fragColor;

void main() {

    float pixelSize = max(2.0, Intensity * 2.0);

    float pulse = sin(GameTime * 1.5) * 0.5 + 0.5;
    pixelSize *= (0.7 + pulse * 0.3);

    vec2 pixelCoord = floor(texCoord * OutSize / pixelSize) * pixelSize;
    vec2 pixelUV = pixelCoord / OutSize;

    pixelUV = clamp(pixelUV, vec2(0.0), vec2(1.0));

    fragColor = texture(InSampler, pixelUV);
}
