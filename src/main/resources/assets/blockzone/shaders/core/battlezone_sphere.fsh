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
    float filament = smoothstep(0.91 - aa, 0.97 + aa, ridge);
    float facing = abs(dot(normal, normalize(toCamera)));
    float rim = pow(1.0 - facing, 3.0);
    vec3 color = mix(vec3(0.40, 0.82, 0.94), vec3(0.70, 0.86, 1.0), broad);
    color = mix(color, vec3(0.82, 0.96, 1.0), filament * 0.5);
    float alpha = 0.025 + filament * 0.065 + rim * 0.075;
    fragColor = vec4(color, alpha * (outside ? 0.72 : 1.0));
}
