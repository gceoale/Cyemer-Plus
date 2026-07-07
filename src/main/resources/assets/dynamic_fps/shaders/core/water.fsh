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

    for(int i = 0; i < 4; i++) {
        value += amplitude * noise(p * frequency);
        frequency *= 2.0;
        amplitude *= 0.5;
    }

    return value;
}

void main() {
    vec3 p = worldPos * 3.0;

    p.xy += vec2(vTime * 0.2, -vTime * 0.4);

    float waves = fbm(p);
    float ripples = fbm(p * 3.0 + vec3(vTime * 0.5));

    float water = waves * 0.7 + ripples * 0.3;

    float caustics = sin(water * 10.0 + vTime * 2.0) * 0.5 + 0.5;
    caustics = pow(caustics, 3.0);

    vec3 deepWater = vec3(0.0, 0.2, 0.4);
    vec3 shallowWater = vec3(0.1, 0.5, 0.7);

    vec3 waterColor = mix(deepWater, shallowWater, water);

    waterColor *= vertexColor.rgb * ColorTint.rgb;

    waterColor += caustics * vec3(0.3, 0.4, 0.5) * 0.5;

    fragColor = vec4(waterColor, vertexColor.a * ColorTint.a);
}