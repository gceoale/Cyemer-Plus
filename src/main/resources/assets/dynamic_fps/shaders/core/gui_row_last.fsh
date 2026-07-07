#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;
in vec2 texCoord0;
flat in float packedRadiusPx;
out vec4 fragColor;

float resolveRadius(float fallbackPx) {
    return packedRadiusPx > 0.0 ? packedRadiusPx : fallbackPx;
}

float sdRoundedRect(vec2 p, vec2 halfSize, vec4 radii) {
    vec4 r = radii;
    r.xy = (p.x >= 0.0) ? r.xy : r.zw;
    float cornerRadius = (p.y >= 0.0) ? r.x : r.y;

    vec2 q = abs(p) - halfSize + vec2(cornerRadius);
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - cornerRadius;
}

float bottomRoundedMask(vec2 uv, float radiusPx, float softness) {
    vec2 dUVdx = dFdx(uv);
    vec2 dUVdy = dFdy(uv);
    float widthPx = 1.0 / max(length(dUVdx), 1e-5);
    float heightPx = 1.0 / max(length(dUVdy), 1e-5);
    vec2 sizePx = vec2(widthPx, heightPx);

    float maxBottomRadius = min(sizePx.x * 0.5, sizePx.y * 0.55);
    float radius = min(radiusPx, maxBottomRadius);
    vec2 p = vec2((uv.x - 0.5) * sizePx.x, (0.5 - uv.y) * sizePx.y);
    vec2 halfSize = sizePx * 0.5;

    vec4 cornerRadii = vec4(0.0, radius, 0.0, radius);
    float dist = sdRoundedRect(p, halfSize, cornerRadii);

    float aa = max(0.9, fwidth(dist) * (softness + 0.35));
    return 1.0 - smoothstep(-aa, aa, dist);
}

float innerShadow(vec2 uv, float radiusPx, float shadowWidth) {
    vec2 dUVdx = dFdx(uv);
    vec2 dUVdy = dFdy(uv);
    float widthPx = 1.0 / max(length(dUVdx), 1e-5);
    float heightPx = 1.0 / max(length(dUVdy), 1e-5);
    vec2 sizePx = vec2(widthPx, heightPx);

    float maxBottomRadius = min(sizePx.x * 0.5, sizePx.y * 0.55);
    float radius = min(radiusPx, maxBottomRadius);
    vec2 p = vec2((uv.x - 0.5) * sizePx.x, (0.5 - uv.y) * sizePx.y);
    vec2 halfSize = sizePx * 0.5;
    vec4 cornerRadii = vec4(0.0, radius, 0.0, radius);
    float dist = sdRoundedRect(p, halfSize, cornerRadii);

    return smoothstep(-shadowWidth, 0.0, dist);
}

void main() {
    vec4 base = vertexColor * ColorModulator;
    float cornerRadius = resolveRadius(12.0);
    float mask = bottomRoundedMask(texCoord0, cornerRadius, 2.2);

    float shadow = innerShadow(texCoord0, cornerRadius, 4.0);
    float bottomSheen = smoothstep(0.4, 1.0, texCoord0.y);
    float centerFade = 1.0 - abs(texCoord0.x - 0.5) * 1.2;
    centerFade = clamp(centerFade, 0.0, 1.0);

    vec3 edgeTint = vec3(0.06, 0.14, 0.24) * (0.12 + 0.24 * bottomSheen * centerFade);
    vec3 rgb = base.rgb + edgeTint;
    rgb -= vec3(0.012, 0.016, 0.02) * shadow;

    float alpha = base.a * mask;
    if (alpha < 0.003) {
        discard;
    }

    fragColor = vec4(rgb, alpha);
}
