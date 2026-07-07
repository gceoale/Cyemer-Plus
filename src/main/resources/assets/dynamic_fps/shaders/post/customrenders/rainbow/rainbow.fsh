#version 330

#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform RainbowConfig {
    vec2 BlurDir;
    float Radius;
    float RadiusMultiplier;
};

out vec4 fragColor;

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec2 oneTexel = 1.0 / InSize;
    vec2 sampleStep = oneTexel * BlurDir;

    vec4 baseColor = texture(InSampler, texCoord);

    float actualRadius = round(Radius * RadiusMultiplier);

    float hue = fract(texCoord.x * 0.5 + texCoord.y * 0.3 + GameTime * 0.2);
    vec3 rainbowColor = hsv2rgb(vec3(hue, 0.5, 1.0));

    vec4 blurred = vec4(0.0);
    float totalWeight = 0.0;

    for (float a = -actualRadius + 0.5; a <= actualRadius; a += 2.0) {
        vec4 sample = texture(InSampler, texCoord + sampleStep * a);
        blurred += sample;
        totalWeight += 1.0;
    }
    blurred += texture(InSampler, texCoord + sampleStep * actualRadius) / 2.0;
    totalWeight += 0.5;

    blurred /= totalWeight;

    float glowStrength = actualRadius / 20.0;
    vec3 finalColor = mix(baseColor.rgb, baseColor.rgb * rainbowColor, glowStrength * 0.5);

    fragColor = vec4(finalColor, baseColor.a);
}
