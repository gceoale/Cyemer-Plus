#version 330

#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform ChromaticConfig {
    float Intensity;
};

out vec4 fragColor;

void main() {
    vec2 direction = texCoord - vec2(0.5);
    float dist = length(direction);

    if (dist > 0.0) {
        direction /= dist;
    }

    float pulse = sin(GameTime * 2.0) * 0.5 + 0.5;
    float aberration = Intensity * 0.01 * (1.0 + pulse * 0.3);

    vec2 offsetR = direction * aberration * dist * 1.2;
    vec2 offsetG = direction * aberration * dist * 0.8;
    vec2 offsetB = direction * aberration * dist * 1.0;

    float r = texture(InSampler, texCoord + offsetR).r;
    float g = texture(InSampler, texCoord + offsetG).g;
    float b = texture(InSampler, texCoord + offsetB).b;
    float a = texture(InSampler, texCoord).a;

    fragColor = vec4(r, g, b, a);
}
