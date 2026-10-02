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

bool isOnDomeCap(vec3 positionFromCameraView) {
    vec3 worldRelative = viewToWorldDirection(positionFromCameraView - DomeCenter);
    return worldRelative.y >= 0.0;
}

bool intersectDome(vec3 rayDirectionView, out float hitDistance, out vec3 hitPositionView) {
    // The dome currently uses equal horizontal and vertical radii, so its
    // intersection is a sphere in either view or world space.
    float radius = max(DomeRadius, 0.001);
    vec3 origin = -DomeCenter / radius;
    vec3 direction = rayDirectionView / radius;
    float a = dot(direction, direction);
    float b = dot(origin, direction);
    float c = dot(origin, origin) - 1.0;
    float discriminant = b * b - a * c;
    if (discriminant < 0.0 || a < 1.0e-8) {
        return false;
    }

    float root = sqrt(discriminant);
    float nearDistance = (-b - root) / a;
    float farDistance = (-b + root) / a;
    hitDistance = nearDistance;
    hitPositionView = rayDirectionView * hitDistance;
    if (hitDistance <= 0.0 || !isOnDomeCap(hitPositionView)) {
        hitDistance = farDistance;
        hitPositionView = rayDirectionView * hitDistance;
    }
    if (hitDistance <= 0.0 || !isOnDomeCap(hitPositionView)) {
        return false;
    }

    return true;
}

vec3 applyDomeSurface(vec3 sceneColor, vec3 rayDirectionView, vec3 hitPositionView) {
    vec3 radii = vec3(max(DomeRadius, 0.001), max(DomeHeight, 0.001), max(DomeRadius, 0.001));
    vec3 worldRelative = viewToWorldDirection(hitPositionView - DomeCenter);
    vec3 surface = worldRelative / radii;
    vec3 normalWorld = normalize(worldRelative / (radii * radii));
    vec3 rayDirectionWorld = normalize(viewToWorldDirection(rayDirectionView));
    // Keep the vortex field attached to the synchronized circle center. Its
    // coordinates are world aligned and do not rotate with the camera or time.
    float azimuth = atan(surface.z, surface.x) + surface.y * 2.6;
    float orbit = sin(azimuth * 5.0 + (1.0 - surface.y) * 17.0
            + sin(azimuth * 2.0) * 1.4);
    float crossFlow = sin(surface.x * 9.0 - surface.z * 6.0
            + sin(surface.y * 10.0) * 1.2);
    float flow = 0.5 + 0.5 * (orbit * 0.68 + crossFlow * 0.32);
    float filament = smoothstep(0.48, 0.88, flow);
    float rim = pow(1.0 - abs(dot(normalWorld, rayDirectionWorld)), 2.5);

    vec3 deepColor = mix(vec3(0.34, 0.40, 0.58), vec3(0.48, 0.56, 0.70), flow * 0.60);
    vec3 hotColor = mix(vec3(0.22, 0.82, 0.96), vec3(0.92, 0.48, 0.82), filament);
    float equatorialGlow = 1.0 - smoothstep(0.02, 0.42, surface.y);
    vec3 surfaceColor = mix(deepColor, hotColor, filament * 0.66)
            + rim * vec3(0.34, 0.42, 0.56)
            + equatorialGlow * filament * vec3(0.12, 0.16, 0.23);
    float opacity = clamp(0.09 + flow * 0.07 + filament * 0.08 + rim * 0.18, 0.0, 0.40);
    return mix(sceneColor, surfaceColor, opacity);
}

void main() {
    vec4 sceneColor = texture(DiffuseSampler, texCoord);
    float depth = texture(DepthSampler, texCoord).r;
    bool isSky = !(depth < 1.0 - 1.0e-7);

    vec2 clipXY = texCoord * 2.0 - 1.0;
    vec4 viewRay = InverseProjection * vec4(clipXY, -1.0, 1.0);
    if (abs(viewRay.w) < 1.0e-7 || any(isnan(viewRay)) || any(isinf(viewRay))) {
        fragColor = sceneColor;
        return;
    }
    viewRay /= viewRay.w;
    vec3 rayDirectionView = normalize(viewRay.xyz);
    if (any(isnan(rayDirectionView)) || any(isinf(rayDirectionView))) {
        fragColor = sceneColor;
        return;
    }

    float sceneDistance = 1.0e30;
    vec3 scenePosition = vec3(0.0);
    bool validScenePosition = false;
    if (!isSky) {
        vec4 clipPosition = vec4(clipXY, depth * 2.0 - 1.0, 1.0);
        vec4 viewPosition = InverseProjection * clipPosition;
        if (abs(viewPosition.w) >= 1.0e-7 && !any(isnan(viewPosition)) && !any(isinf(viewPosition))) {
            viewPosition /= viewPosition.w;
            scenePosition = viewPosition.xyz;
            validScenePosition = !any(isnan(scenePosition)) && !any(isinf(scenePosition));
            if (validScenePosition) {
                sceneDistance = length(scenePosition);
                vec3 relative = viewToWorldDirection(scenePosition - DomeCenter);
                float radialDistanceSquared = dot(relative.xz, relative.xz) / max(DomeRadius * DomeRadius, 1.0e-6);
                float roofHeight = DomeHeight * sqrt(max(0.0, 1.0 - radialDistanceSquared));
                bool insideDome = radialDistanceSquared <= 1.0
                        && relative.y >= 0.0 && relative.y <= roofHeight;
                if (!insideDome) {
                    float luminance = dot(sceneColor.rgb, vec3(0.2126, 0.7152, 0.0722));
                    vec3 desaturated = mix(sceneColor.rgb, vec3(luminance), 0.92);
                    sceneColor.rgb = mix(desaturated, vec3(OUTSIDE_GRAY_LEVEL), OUTSIDE_GRAY_MIX);
                }
            }
        }
    }

    float domeDistance;
    vec3 domeHitView;
    if (intersectDome(rayDirectionView, domeDistance, domeHitView)
            && (isSky || !validScenePosition || domeDistance < sceneDistance - 0.03)) {
        sceneColor.rgb = applyDomeSurface(sceneColor.rgb, rayDirectionView, domeHitView);
    }

    fragColor = sceneColor;
}
