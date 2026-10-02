#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;
out vec4 fragColor;

uniform vec3 DomeCenter;
uniform float DomeRadius;
uniform float DomeHeight;
uniform mat4 InverseProjection;
uniform mat4 WorldFromView;

const float OUTSIDE_GRAY_LEVEL = 0.78;
const float OUTSIDE_GRAY_MIX = 0.62;

vec3 viewToWorldDirection(vec3 viewDirection) {
    return (WorldFromView * vec4(viewDirection, 0.0)).xyz;
}

void main() {
    vec4 sceneColor = texture(DiffuseSampler, texCoord);
    float depth = texture(DepthSampler, texCoord).r;
    if (depth >= 1.0 - 1.0e-7) {
        fragColor = sceneColor;
        return;
    }

    vec2 clipXY = texCoord * 2.0 - 1.0;
    vec4 clipPosition = vec4(clipXY, depth * 2.0 - 1.0, 1.0);
    vec4 viewPosition = InverseProjection * clipPosition;
    if (abs(viewPosition.w) < 1.0e-7 || any(isnan(viewPosition)) || any(isinf(viewPosition))) {
        fragColor = sceneColor;
        return;
    }
    viewPosition /= viewPosition.w;

    // Both the depth-reconstructed position and DomeCenter are view relative.
    // Convert their difference to world axes before classifying it against the zone.
    vec3 relative = viewToWorldDirection(viewPosition.xyz - DomeCenter);
    float radialDistanceSquared = dot(relative.xz, relative.xz) / max(DomeRadius * DomeRadius, 1.0e-6);
    float roofHeight = DomeHeight * sqrt(max(0.0, 1.0 - radialDistanceSquared));
    bool insideDome = radialDistanceSquared <= 1.0
            && relative.y >= 0.0 && relative.y <= roofHeight;
    if (!insideDome) {
        float luminance = dot(sceneColor.rgb, vec3(0.2126, 0.7152, 0.0722));
        vec3 desaturated = mix(sceneColor.rgb, vec3(luminance), 0.92);
        sceneColor.rgb = mix(desaturated, vec3(OUTSIDE_GRAY_LEVEL), OUTSIDE_GRAY_MIX);
    }

    fragColor = sceneColor;
}
