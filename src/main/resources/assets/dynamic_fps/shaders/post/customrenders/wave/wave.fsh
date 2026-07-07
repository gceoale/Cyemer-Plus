#version 330

#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform WaveConfig {
    float Intensity;
};

out vec4 fragColor;

void main() {
    float frequency = 10.0;
    float amplitude = Intensity * 0.005;

    float waveX = sin(texCoord.y * frequency + GameTime * 3.0) * amplitude;

    float waveY = sin(texCoord.x * frequency + GameTime * 2.5) * amplitude;

    vec2 center = vec2(0.5);
    float dist = length(texCoord - center);
    float ripple = sin(dist * 20.0 - GameTime * 4.0) * amplitude * 0.5;

    vec2 distortion = vec2(waveX + ripple, waveY + ripple);
    vec2 distortedCoord = texCoord + distortion;

    fragColor = texture(InSampler, distortedCoord);
}
