#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;
in float clipDepth;
out vec4 fragColor;

void main() {
    vec4 base = vertexColor * ColorModulator;
    float depthT = clamp((clipDepth + 1.0) * 0.5, 0.0, 1.0);
    float depthGlow = pow(1.0 - depthT, 1.35);

    vec3 lift = vec3(0.04, 0.07, 0.12) * depthGlow;
    vec3 rgb = base.rgb * (0.92 + 0.18 * depthGlow) + lift;
    float alpha = base.a * (0.84 + 0.16 * depthGlow);

    if (alpha < 0.002) {
        discard;
    }

    fragColor = vec4(rgb, alpha);
}
