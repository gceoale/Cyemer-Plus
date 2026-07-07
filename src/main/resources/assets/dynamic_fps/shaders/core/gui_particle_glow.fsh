#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;

void main() {
    vec4 base = vertexColor * ColorModulator;
    vec2 p = texCoord0 - vec2(0.5);
    float d = length(p) * 2.0;

    float falloff = exp(-3.4 * d * d);
    float edgeSoft = 1.0 - smoothstep(0.85, 1.0, d);
    float alpha = base.a * falloff * edgeSoft;

    if (alpha < 0.0015) {
        discard;
    }

    vec3 rgb = base.rgb * (1.0 + 0.08 * falloff);
    fragColor = vec4(rgb, alpha);
}
