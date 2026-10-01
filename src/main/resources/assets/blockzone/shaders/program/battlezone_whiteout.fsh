#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;
out vec4 fragColor;

uniform vec2 CircleCenter;
uniform float CircleRadius;
uniform vec4 MapBounds;
uniform mat4 InverseProjection;
uniform mat4 InverseViewRotation;

void main() {
    vec4 sceneColor = texture(DiffuseSampler, texCoord);
    float depth = texture(DepthSampler, texCoord).r;
    if (depth >= 0.999999) {
        fragColor = sceneColor;
        return;
    }

    vec4 clipPosition = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewPosition = InverseProjection * clipPosition;
    viewPosition /= viewPosition.w;
    vec2 worldXZ = (InverseViewRotation * vec4(viewPosition.xyz, 0.0)).xz;

    float distanceFromCenter = distance(worldXZ, CircleCenter);
    bool onCircleBoundary = abs(distanceFromCenter - CircleRadius) <= 0.65;
    bool withinMapZ = worldXZ.y >= MapBounds.y - 0.15 && worldXZ.y <= MapBounds.w + 0.15;
    bool onMapBoundary = withinMapZ && (abs(worldXZ.x - MapBounds.x) <= 0.15
            || abs(worldXZ.x - MapBounds.z) <= 0.15);
    bool withinMapX = worldXZ.x >= MapBounds.x - 0.15 && worldXZ.x <= MapBounds.z + 0.15;
    onMapBoundary = onMapBoundary || (withinMapX && (abs(worldXZ.y - MapBounds.y) <= 0.15
            || abs(worldXZ.y - MapBounds.w) <= 0.15));

    if (distanceFromCenter > CircleRadius && !onCircleBoundary && !onMapBoundary) {
        fragColor = vec4(1.0, 1.0, 1.0, sceneColor.a);
    } else {
        fragColor = sceneColor;
    }
}
