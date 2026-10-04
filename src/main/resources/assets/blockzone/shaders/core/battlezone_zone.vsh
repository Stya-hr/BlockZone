#version 150
in vec3 Position;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float ZoneHeightRatio;
out vec3 zonePosition;
out float wallHeight;

void main() {
    zonePosition = vec3(Position.x, Position.y * ZoneHeightRatio, Position.z);
    wallHeight = Position.y * 0.5 + 0.5;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
