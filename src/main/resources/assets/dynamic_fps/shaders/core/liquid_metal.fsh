#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i),                    hash(i + vec2(1.0, 0.0)), u.x),
        mix(hash(i + vec2(0.0, 1.0)),   hash(i + vec2(1.0, 1.0)), u.x),
        u.y
    );
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 rot = mat2(0.8, 0.6, -0.6, 0.8);
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p = rot * p + vec2(1.7, 9.2);
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 screen = texCoord0;
    vec2 uv = screen * 2.5;

    float tR = floor(vertexColor.r * 255.0 + 0.5);
    float tG = floor(vertexColor.g * 255.0 + 0.5);
    float t = (tR * 256.0 + tG) * 0.004;

    vec2 toff = vec2(mod(t * 0.13, 10.0), mod(t * 0.07, 10.0));

    vec2 q = vec2(fbm(uv + toff),               fbm(uv + toff + vec2(5.2, 1.3)));
    vec2 r = vec2(fbm(uv + toff + 4.0 * q),     fbm(uv + toff + 4.0 * q + vec2(8.3, 2.8)));
    float f = fbm(uv + toff + 3.0 * r);

    float sy = screen.y;

    float band1 = exp(-pow((sy - 0.38 + r.x * 0.18) / 0.20, 2.0));
    float band2 = exp(-pow((sy - 0.62 + q.y * 0.14) / 0.25, 2.0));
    float band3 = exp(-pow((sy - 0.20 + r.y * 0.10) / 0.12, 2.0));

    vec3 void_base   = vec3(0.015, 0.008, 0.040);
    vec3 deep_violet = vec3(0.080, 0.020, 0.200);
    vec3 aurora_blue = vec3(0.080, 0.280, 0.750);
    vec3 aurora_teal = vec3(0.030, 0.500, 0.600);
    vec3 aurora_mag  = vec3(0.550, 0.060, 0.700);
    vec3 core_glow   = vec3(0.800, 0.650, 1.000);

    vec3 col = mix(void_base, deep_violet, smoothstep(0.0, 0.5, f));
    col = mix(col, aurora_blue, band1 * smoothstep(0.25, 0.75, f) * 1.0);
    col = mix(col, aurora_teal, band2 * smoothstep(0.35, 0.85, f) * 0.85);
    col = mix(col, aurora_mag,  band3 * smoothstep(0.45, 0.90, f) * 0.70);

    float edge = pow(max(0.0, fbm(uv + toff * 0.7 + r) - 0.52), 2.5);
    col += core_glow * edge * (band1 * 0.9 + band2 * 0.7) * 1.2;
    col += deep_violet * smoothstep(0.4, 0.9, f) * 0.3;

    float grain = hash(screen * 500.0 + fract(q * 17.3)) * 0.018;
    col += grain;

    vec2 centered = screen - 0.5;
    float vig = 1.0 - smoothstep(0.30, 1.10, length(centered * vec2(1.6, 1.8)));
    col *= 0.45 + 0.55 * vig;

    col = pow(clamp(col, 0.0, 1.0), vec3(0.85));

    fragColor = vec4(col, 1.0) * ColorModulator;
}