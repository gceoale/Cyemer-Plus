#version 150

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float GameTime;
uniform float Speed;

out vec4 vertexColor;
out vec3 worldPos;
out float vTime;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexColor = Color;
    worldPos = Position;
    vTime = GameTime * Speed;
}