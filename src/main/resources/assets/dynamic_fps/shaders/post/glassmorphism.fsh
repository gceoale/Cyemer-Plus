#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform GlassConfig {
    float BlurRadius;
    float Brightness;
    float Alpha;
};

out vec4 fragColor;

void main() {
    vec4 color = vec4(0.0);
    float totalWeight = 0.0;

    int samples = int(BlurRadius);
    vec2 sampleStep = 1.0 / InSize;

    for (int x = -samples; x <= samples; x++) {
        for (int y = -samples; y <= samples; y++) {
            vec2 offset = vec2(float(x), float(y)) * sampleStep;
            float distance = length(vec2(x, y));
            float weight = exp(-distance * distance / (2.0 * BlurRadius * BlurRadius));

            color += texture(InSampler, texCoord + offset) * weight;
            totalWeight += weight;
        }
    }

    color /= totalWeight;


    color.rgb *= Brightness;

    color.rgb += vec3(0.02, 0.03, 0.05);

    vec4 original = texture(InSampler, texCoord);
    color = mix(original, color, Alpha);

    fragColor = color;
}
