#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;

float circleMask(vec2 uv, float softness) {
    vec2 p = uv - vec2(0.5);
    float d = length(p);
    float aa = max(0.001, fwidth(d) * softness);
    return 1.0 - smoothstep(0.5 - aa, 0.5 + aa, d);
}

void main() {
    vec4 base = vertexColor * ColorModulator;
    float mask = circleMask(texCoord0, 1.6);

    float radial = 1.0 - length(texCoord0 - vec2(0.5)) * 1.6;
    radial = clamp(radial, 0.0, 1.0);

    vec3 rgb = base.rgb * (0.96 + radial * 0.10);
    float alpha = base.a * mask;

    if (alpha < 0.0025) {
        discard;
    }

    fragColor = vec4(rgb, alpha);
}
