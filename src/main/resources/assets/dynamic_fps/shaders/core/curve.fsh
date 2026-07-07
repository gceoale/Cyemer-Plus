#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;
in vec2 texCoord0;
flat in float packedRadiusPx;
out vec4 fragColor;

float resolveRadius(float fallbackPx) {
    return packedRadiusPx > 0.0 ? packedRadiusPx : fallbackPx;
}

void main() {
    vec2 uv = texCoord0;

    vec2 dUVdx = dFdx(uv);
    vec2 dUVdy = dFdy(uv);
    float W = 1.0 / length(dUVdx);
    float H = 1.0 / length(dUVdy);

    float radius = resolveRadius(20.0);
    radius = min(radius, min(W, H) * 0.5);

    vec2 p = (uv - 0.5) * vec2(W, H);
    vec2 halfSize = vec2(W, H) * 0.5 - vec2(radius);

    vec2 q = abs(p) - halfSize;
    float dist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;

    float fw = max(0.7, fwidth(dist) * 1.3);
    float alpha = 1.0 - smoothstep(-fw, fw, dist);

    vec4 color = vertexColor * ColorModulator;
    if (color.a * alpha < 0.01) discard;
    fragColor = vec4(color.rgb, color.a * alpha);
}
