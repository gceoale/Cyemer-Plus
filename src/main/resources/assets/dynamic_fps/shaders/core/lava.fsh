#version 150

in vec4 vertexColor;
in vec3 worldPos;
in float vTime;

uniform vec4 ColorTint;

out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);

    return mix(
        mix(mix(hash(i + vec3(0,0,0)), hash(i + vec3(1,0,0)), f.x),
            mix(hash(i + vec3(0,1,0)), hash(i + vec3(1,1,0)), f.x), f.y),
        mix(mix(hash(i + vec3(0,0,1)), hash(i + vec3(1,0,1)), f.x),
            mix(hash(i + vec3(0,1,1)), hash(i + vec3(1,1,1)), f.x), f.y),
        f.z
    );
}

float fbm(vec3 p) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;

    for(int i = 0; i < 6; i++) {
        value += amplitude * noise(p * frequency);
        frequency *= 2.0;
        amplitude *= 0.5;
    }

    return value;
}

void main() {
    vec3 p = worldPos * 1.5;

    p.y -= vTime * 0.3;

    float n1 = fbm(p);
    float n2 = fbm(p * 2.0 + vec3(0.0, vTime * 0.5, 0.0));

    float lava = n1 * 0.6 + n2 * 0.4;

    float hotSpot = sin(lava * 8.0 + vTime * 3.0) * 0.5 + 0.5;
    hotSpot = pow(hotSpot, 2.0);

    vec3 darkLava = vec3(0.3, 0.05, 0.0);
    vec3 brightLava = vec3(1.0, 0.6, 0.1);
    vec3 hotLava = vec3(1.0, 0.9, 0.5);

    vec3 lavaColor = mix(darkLava, brightLava, lava);
    lavaColor = mix(lavaColor, hotLava, hotSpot * 0.5);

    lavaColor *= vertexColor.rgb * ColorTint.rgb;

    lavaColor += hotSpot * vec3(0.3, 0.2, 0.0);

    fragColor = vec4(lavaColor, vertexColor.a * ColorTint.a);
}