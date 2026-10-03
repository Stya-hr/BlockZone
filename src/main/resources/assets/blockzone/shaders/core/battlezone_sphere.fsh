#version 150
uniform float ZoneTime;
uniform float ZoneDaylight;
uniform vec3 CameraLocalPosition;
in vec3 spherePosition;
out vec4 fragColor;

// Smooth 3D value noise: continuous across the sphere's longitude seam and poles.
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
    vec3 normal = normalize(spherePosition);
    vec3 toCamera = CameraLocalPosition - spherePosition;
    bool outside = dot(CameraLocalPosition, CameraLocalPosition) > 1.0;
    // Outside observers see only the near shell, avoiding a second translucent layer.
    if (outside && dot(normal, toCamera) <= 0.0) discard;

    float time = ZoneTime * 0.045;
    float twist = normal.y * 2.2 + time;
    float c = cos(twist), s = sin(twist);
    vec3 flow = vec3(c * normal.x - s * normal.z, normal.y,
                     s * normal.x + c * normal.z);
    vec3 drift = vec3(time * 0.8, -time * 0.5, time * 0.6);
    float broad = noise(flow * 5.0 + drift);
    float detail = noise(flow * 19.0 + vec3(broad * 1.7) + drift);
    float ridge = 1.0 - abs(detail * 2.0 - 1.0);
    float aa = max(fwidth(ridge), 0.015);
    float filament = smoothstep(0.86 - aa, 0.95 + aa, ridge);
    float facing = abs(dot(normal, normalize(toCamera)));
    float rim = pow(1.0 - facing, 2.0);
    // Invert ambient brightness: dark violet by day, luminous lavender by night.
    float daylight = smoothstep(0.0, 1.0, ZoneDaylight);
    vec3 darkBody = mix(vec3(0.075, 0.035, 0.20), vec3(0.16, 0.08, 0.34), broad);
    vec3 lightBody = mix(vec3(0.48, 0.36, 0.76), vec3(0.68, 0.56, 0.94), broad);
    vec3 color = mix(lightBody, darkBody, daylight);
    color = mix(color, mix(vec3(0.76, 0.65, 1.0), vec3(0.13, 0.055, 0.32), daylight), filament * 0.75);
    color = mix(color, mix(vec3(0.84, 0.76, 1.0), vec3(0.20, 0.10, 0.43), daylight), rim);
    // A stronger daytime body actually darkens the background through alpha blending.
    float alpha = mix(0.07, 0.14, daylight) + filament * 0.22 + rim * 0.30;
    fragColor = vec4(color, alpha * (outside ? 0.90 : 1.0));
}
