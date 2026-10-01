#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;
out vec4 fragColor;

uniform vec2 CircleCenter;
uniform float CircleRadius;
uniform mat4 InverseProjection;
uniform mat4 InverseViewRotation;

const float OUTSIDE_DESATURATION = 0.92;
const float OUTSIDE_WHITE_LEVEL = 0.96;
const float OUTSIDE_WHITE_MIX = 0.82;

void main() {
    vec4 sceneColor = texture(DiffuseSampler, texCoord);
    float depth = texture(DepthSampler, texCoord).r;
    // A cleared depth value belongs to the sky (including stars). Never unproject
    // it: the inverse projection has no finite world position at the far plane.
    if (!(depth < 1.0 - 1.0e-7)) {
        fragColor = sceneColor;
        return;
    }

    vec4 clipPosition = vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewPosition = InverseProjection * clipPosition;
    if (abs(viewPosition.w) < 1.0e-7 || any(isnan(viewPosition)) || any(isinf(viewPosition))) {
        fragColor = sceneColor;
        return;
    }
    viewPosition /= viewPosition.w;
    vec3 cameraRelativeWorld = (InverseViewRotation * vec4(viewPosition.xyz, 0.0)).xyz;
    if (any(isnan(cameraRelativeWorld)) || any(isinf(cameraRelativeWorld))) {
        fragColor = sceneColor;
        return;
    }

    float distanceFromCenter = distance(cameraRelativeWorld.xz, CircleCenter);
    if (distanceFromCenter >= CircleRadius) {
        float luminance = dot(sceneColor.rgb, vec3(0.2126, 0.7152, 0.0722));
        vec3 desaturatedColor = mix(sceneColor.rgb, vec3(luminance), OUTSIDE_DESATURATION);
        vec3 whiteModelColor = mix(desaturatedColor, vec3(OUTSIDE_WHITE_LEVEL), OUTSIDE_WHITE_MIX);
        fragColor = vec4(whiteModelColor, sceneColor.a);
    } else {
        fragColor = sceneColor;
    }

}
