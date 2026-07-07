#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;

vec4 sampleTent9(vec2 uv, vec2 texel) {
    vec4 c = texture(Sampler0, uv) * 4.0;
    c += texture(Sampler0, uv + vec2(texel.x, 0.0)) * 2.0;
    c += texture(Sampler0, uv - vec2(texel.x, 0.0)) * 2.0;
    c += texture(Sampler0, uv + vec2(0.0, texel.y)) * 2.0;
    c += texture(Sampler0, uv - vec2(0.0, texel.y)) * 2.0;
    c += texture(Sampler0, uv + vec2(texel.x, texel.y));
    c += texture(Sampler0, uv + vec2(texel.x, -texel.y));
    c += texture(Sampler0, uv + vec2(-texel.x, texel.y));
    c += texture(Sampler0, uv + vec2(-texel.x, -texel.y));
    return c * (1.0 / 16.0);
}

void main() {
    vec2 dx = dFdx(texCoord0);
    vec2 dy = dFdy(texCoord0);
    vec2 texel = vec2(max(abs(dx.x), abs(dy.x)), max(abs(dx.y), abs(dy.y)));
    texel = clamp(texel, vec2(1.0 / 2048.0), vec2(1.0 / 48.0));

    vec4 center = texture(Sampler0, texCoord0);
    vec4 filtered = sampleTent9(texCoord0, texel);
    vec4 texColor = mix(filtered, center, 0.66);

    float alphaSample = max(center.a, filtered.a);
    float aa = max(fwidth(alphaSample) * 1.45, 0.0008);
    float alpha = alphaSample;
    alpha = smoothstep(0.01 - aa, 0.01 + aa, alpha);

    float luma = dot(texColor.rgb, vec3(0.2126, 0.7152, 0.0722));
    float darkInk = smoothstep(0.24, 0.03, luma);
    vec3 liftedDark = mix(texColor.rgb * 1.15, vec3(0.86), 0.58);
    vec3 iconRgb = mix(texColor.rgb, liftedDark, darkInk * 0.85);

    vec4 color = vec4(iconRgb, alpha) * vertexColor * ColorModulator;
    if (color.a < 0.0025) {
        discard;
    }

    fragColor = color;
}
