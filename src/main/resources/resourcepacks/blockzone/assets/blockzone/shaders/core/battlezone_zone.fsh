#version 150
uniform float ZoneTime;
uniform int ZoneShape;
uniform float ZoneYOffset;
uniform float ZoneDaylight;
uniform vec3 CameraLocalPosition;
in vec3 zonePosition;
in float wallHeight;

out vec4 fragColor;

// Smooth 3D value noise, continuous across the vertical shell.
float hash(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}
float noise(vec3 p) {
    vec3 cell = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(cell), hash(cell + vec3(1, 0, 0)), f.x),
                   mix(hash(cell + vec3(0, 1, 0)), hash(cell + vec3(1, 1, 0)), f.x), f.y),
               mix(mix(hash(cell + vec3(0, 0, 1)), hash(cell + vec3(1, 0, 1)), f.x),
                   mix(hash(cell + vec3(0, 1, 1)), hash(cell + vec3(1, 1, 1)), f.x), f.y), f.z);
}
void main() {
    vec3 normal;
    vec3 toCamera = CameraLocalPosition - zonePosition;
    bool outside;
    if (ZoneShape == 0) {
        normal = normalize(vec3(zonePosition.x, 0.0, zonePosition.z));
        outside = dot(CameraLocalPosition.xz, CameraLocalPosition.xz) > 1.0;
    } else {
        normal = abs(zonePosition.x) >= abs(zonePosition.z)
            ? vec3(sign(zonePosition.x), 0, 0) : vec3(0, 0, sign(zonePosition.z));
        outside = max(abs(CameraLocalPosition.x), abs(CameraLocalPosition.z)) > 1.0;
    }
    // Outside observers see only the near shell, avoiding a second translucent layer.
    if (outside && dot(normal, toCamera) <= 0.0) discard;

    float time = ZoneTime * 0.045;
    float twist = normal.y * 2.2 + time;
    float c = cos(twist), s = sin(twist);
    vec3 samplePosition = zonePosition;
    samplePosition.y += ZoneYOffset;
    vec3 flow = vec3(c * samplePosition.x - s * samplePosition.z, samplePosition.y,
                     s * samplePosition.x + c * samplePosition.z);
    vec3 drift = vec3(time * 0.8, -time * 0.5, time * 0.6);
    // Two noise samples, as before: broad smoke warped into soft swirling billows.
    float broad = noise(flow * 4.0 + drift);
    float detail = noise(flow * 9.0 + vec3(broad * 2.4, -broad * 1.6, broad) + drift);
    float smoke = smoothstep(0.22, 0.78, broad * 0.60 + detail * 0.40);
    float wisps = smoothstep(0.35, 0.80, detail) * (1.0 - smoothstep(0.55, 0.90, broad));
    float facing = abs(dot(normal, normalize(toCamera)));
    float rim = pow(1.0 - facing, 2.0);
    // Invert ambient brightness: dark violet by day, luminous lavender by night.
    float daylight = smoothstep(0.0, 1.0, ZoneDaylight);
    vec3 darkBody = mix(vec3(0.075, 0.035, 0.20), vec3(0.16, 0.08, 0.34), smoke);
    vec3 lightBody = mix(vec3(0.48, 0.36, 0.76), vec3(0.68, 0.56, 0.94), smoke);
    vec3 color = mix(lightBody, darkBody, daylight);
    color = mix(color, mix(vec3(0.76, 0.65, 1.0), vec3(0.13, 0.055, 0.32), daylight), wisps * 0.55);
    color = mix(color, mix(vec3(0.84, 0.76, 1.0), vec3(0.20, 0.10, 0.43), daylight), rim);
    // A stronger daytime body actually darkens the background through alpha blending.
    float alpha = mix(0.07, 0.14, daylight) + smoke * 0.13 + wisps * 0.08 + rim * 0.24;
    // Fade the upper third to avoid a hard horizontal edge against the sky.
    float topFade = 1.0 - smoothstep(2.0 / 3.0, 1.0, wallHeight);
    fragColor = vec4(color, alpha * topFade * (outside ? 0.90 : 1.0));
}
