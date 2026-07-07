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

    float aa = max(0.8, fwidth(dist) * softness);
    return 1.0 - smoothstep(-aa, aa, dist);
}

void main() {
    vec4 base = vertexColor * ColorModulator;
    float cornerRadius = resolveRadius(18.0);
    float mask = roundedMask(texCoord0, cornerRadius, 2.3);

    float sheen = smoothstep(1.0, 0.0, texCoord0.y);
    float edgeDist = min(min(texCoord0.x, 1.0 - texCoord0.x), min(texCoord0.y, 1.0 - texCoord0.y));
    float edgeHighlight = pow(1.0 - clamp(edgeDist * 5.0, 0.0, 1.0), 2.5) * 0.06;

    vec3 rgb = base.rgb * (1.0 + 0.12 * sheen);
    rgb += vec3(edgeHighlight);

    float alpha = base.a * mask * (0.80 + 0.20 * sheen);
    if (alpha < 0.003) {
        discard;
    }

    fragColor = vec4(rgb, alpha);
}
