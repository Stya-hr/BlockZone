#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;
out vec4 fragColor;

uniform vec3 DomeCenter;
uniform float DomeRadius;
uniform float DomeHeight;
uniform float GameTime;
uniform mat4 InverseProjection;
uniform mat4 InverseViewRotation;

const float OUTSIDE_GRAY_LEVEL = 0.78;
const float OUTSIDE_GRAY_MIX = 0.62;

bool intersectDome(vec3 rayDirection, out float hitDistance, out vec3 hitPosition) {
    vec3 radii = vec3(max(DomeRadius, 0.001), max(DomeHeight, 0.001), max(DomeRadius, 0.001));
    vec3 origin = -DomeCenter / radii;
    vec3 direction = rayDirection / radii;
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
    if (hitDistance <= 0.0 || rayDirection.y * hitDistance < DomeCenter.y) {
        hitDistance = farDistance;
    }
    if (hitDistance <= 0.0 || rayDirection.y * hitDistance < DomeCenter.y) {
        return false;
    }

    hitPosition = rayDirection * hitDistance;
    return true;
}

vec3 applyDomeSurface(vec3 sceneColor, vec3 rayDirection, vec3 hitPosition) {
    vec3 radii = vec3(max(DomeRadius, 0.001), max(DomeHeight, 0.001), max(DomeRadius, 0.001));
    vec3 surface = (hitPosition - DomeCenter) / radii;
    vec3 normal = normalize(vec3(surface.x / radii.x, surface.y / radii.y, surface.z / radii.z));
    float time = mod(GameTime, 4096.0);
    // Wrapped azimuth waves create a slowly orbiting vortex; their integer
    // frequencies keep the texture continuous at the longitude seam.
    float azimuth = atan(surface.z, surface.x) + surface.y * 2.6 - time * 0.16;
    float orbit = sin(azimuth * 5.0 + (1.0 - surface.y) * 17.0 - time * 1.25
            + sin(azimuth * 2.0 - time * 0.42) * 1.4);
    float crossFlow = sin(surface.x * 9.0 - surface.z * 6.0 + time * 0.58
            + sin(surface.y * 10.0 - time * 0.31) * 1.2);
    float flow = 0.5 + 0.5 * (orbit * 0.68 + crossFlow * 0.32);
    float filament = smoothstep(0.48, 0.88, flow);
    float rim = pow(1.0 - abs(dot(normal, rayDirection)), 2.5);

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
    vec3 rayDirection = normalize((InverseViewRotation * vec4(viewRay.xyz, 0.0)).xyz);
    if (any(isnan(rayDirection)) || any(isinf(rayDirection))) {
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
            scenePosition = (InverseViewRotation * vec4(viewPosition.xyz, 0.0)).xyz;
            validScenePosition = !any(isnan(scenePosition)) && !any(isinf(scenePosition));
            if (validScenePosition) {
                sceneDistance = length(scenePosition);
                vec3 relative = scenePosition - DomeCenter;
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
    vec3 domeHit;
    if (intersectDome(rayDirection, domeDistance, domeHit)
            && (isSky || !validScenePosition || domeDistance < sceneDistance - 0.03)) {
        sceneColor.rgb = applyDomeSurface(sceneColor.rgb, rayDirection, domeHit);
    }

    fragColor = sceneColor;
}
