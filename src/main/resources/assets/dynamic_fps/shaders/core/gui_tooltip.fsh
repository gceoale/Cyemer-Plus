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

    float aa = max(0.7, fwidth(dist) * softness);
    return 1.0 - smoothstep(-aa, aa, dist);
}

float innerShadow(vec2 uv, float radiusPx, float shadowWidth) {
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

    return smoothstep(-shadowWidth, 0.0, dist);
}

void main() {
    vec4 base = vertexColor * ColorModulator;
    float cornerRadius = resolveRadius(4.0);
    float mask = roundedMask(texCoord0, cornerRadius, 1.8);

    float shadow = innerShadow(texCoord0, cornerRadius, 2.0);
    float topLight = smoothstep(0.0, 1.0, 1.0 - texCoord0.y);
    vec3 rgb = base.rgb * (0.94 + 0.10 * topLight);
    rgb -= vec3(0.012, 0.016, 0.02) * shadow;

    float alpha = base.a * mask;
    if (alpha < 0.003) {
        discard;
    }

    fragColor = vec4(rgb, alpha);
}
