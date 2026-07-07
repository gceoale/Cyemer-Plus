#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;
in vec2 texCoord0;
flat in float packedRadiusPx;
out vec4 fragColor;

float resolveRadius(float fallbackPx) {
    return packedRadiusPx > 0.0 ? packedRadiusPx : fallbackPx;
}

float roundedMask(vec2 uv, float radiusPx, float softness) {
    vec2 dUVdx = dFdx(uv);
    vec2 dUVdy = dFdy(uv);
    float widthPx = 1.0 / max(length(dUVdx), 1e-5);
    float heightPx = 1.0 / max(length(dUVdy), 1e-5);
    vec2 sizePx = vec2(widthPx, heightPx);

    float radius = min(radiusPx, min(sizePx.x, sizePx.y) * 0.5);
    vec2 p = (uv - 0.5) * sizePx;
    vec2 halfSize = sizePx * 0.5 - vec2(radius);
    vec2 q = abs(p) - halfSize;
    float dist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;

    float aa = max(0.9, fwidth(dist) * (softness + 0.3));
    return 1.0 - smoothstep(-aa, aa, dist);
}

void main() {
    vec4 base = vertexColor * ColorModulator;
    float cornerRadius = resolveRadius(8.0);
    float mask = roundedMask(texCoord0, cornerRadius, 2.0);

    float midGlow = exp(-pow((texCoord0.y - 0.5) * 2.2, 2.0));
    float sideFade = 1.0 - abs(texCoord0.x - 0.5) * 1.1;
    sideFade = clamp(sideFade, 0.0, 1.0);

    float topSheen = pow(1.0 - texCoord0.y, 1.6) * 0.08;

    vec3 sheen = vec3(0.10, 0.18, 0.28) * (0.14 + 0.38 * midGlow * sideFade);
    vec3 rgb = base.rgb + sheen + vec3(topSheen);

    float alpha = base.a * mask;
    if (alpha < 0.003) {
        discard;
    }

    fragColor = vec4(rgb, alpha);
}
