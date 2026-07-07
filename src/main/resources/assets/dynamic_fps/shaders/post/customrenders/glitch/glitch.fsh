#version 330

#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform GlitchConfig {
    float Intensity;
};

out vec4 fragColor;

float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

void main() {
    vec2 uv = texCoord;

    float strength = Intensity * 0.02;

    float glitchTime = floor(GameTime * 10.0);
    float glitchRandom = random(vec2(glitchTime, 0.0));

    if (glitchRandom > 0.7) {
        float scanline = floor(uv.y * 100.0);
        float scanlineRandom = random(vec2(scanline, glitchTime));

        if (scanlineRandom > 0.8) {
            uv.x += (scanlineRandom - 0.9) * strength * 2.0;
        }

        if (scanlineRandom > 0.9) {
            vec4 base = texture(InSampler, uv);
            float offset = strength * 0.5;

            fragColor = vec4(
                texture(InSampler, uv + vec2(offset, 0.0)).r,
                base.g,
                texture(InSampler, uv - vec2(offset, 0.0)).b,
                base.a
            );
            return;
        }

        float blockY = floor(uv.y * 20.0);
        float blockRandom = random(vec2(blockY, glitchTime * 0.1));

        if (blockRandom > 0.85) {
            uv.x += (blockRandom - 0.925) * strength * 4.0;
        }
    }

    vec4 color = texture(InSampler, uv);

    if (glitchRandom > 0.95) {
        color.rgb = 1.0 - color.rgb;
    }

    fragColor = color;
}
