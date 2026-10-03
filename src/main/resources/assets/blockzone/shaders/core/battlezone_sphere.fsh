#version 150
uniform float ZoneTime;
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
    // Blue-violet separates the translucent shell from daylight sky and pale poisoned terrain.
    vec3 color = mix(vec3(0.22, 0.12, 0.52), vec3(0.40, 0.28, 0.72), broad);
    color = mix(color, vec3(0.25, 0.12, 0.58), filament * 0.75);
    color = mix(color, vec3(0.32, 0.20, 0.68), rim);
    // Concentrate opacity on filaments and the silhouette, keeping the shell transparent.
    float alpha = 0.04 + filament * 0.22 + rim * 0.30;
    fragColor = vec4(color, alpha * (outside ? 0.90 : 1.0));
}
