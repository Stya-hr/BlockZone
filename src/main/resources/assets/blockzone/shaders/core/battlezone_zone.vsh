#version 150
in vec3 Position;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float ZoneHeightRatio;
out vec3 zonePosition;

void main() {
    zonePosition = vec3(Position.x, Position.y * ZoneHeightRatio, Position.z);
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
