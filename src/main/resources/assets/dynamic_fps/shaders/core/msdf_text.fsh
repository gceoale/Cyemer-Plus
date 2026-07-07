#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    float dist = texture(Sampler0, texCoord0).r;

    float fw = fwidth(dist) * 0.75;
    float alpha = smoothstep(0.5 - fw, 0.5 + fw, dist);

    vec4 color = vertexColor * ColorModulator;
    if (color.a * alpha < 0.004) discard;
    fragColor = vec4(color.rgb, color.a * alpha);
}