#version 150
in vec3 Position;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float ZoneTime;
out vec3 spherePosition;
out float flareStrength;

float flareHash(float seed) {
    return fract(sin(seed * 127.1 + 311.7) * 43758.5453);
}
void main() {
    vec3 direction = normalize(Position);
    vec3 displaced = Position;
    flareStrength = 0.0;
    // Four staggered, short-lived lobes on the existing mesh; no particles or extra pass.
    // Position changes only while the envelope is zero, avoiding visible jumps.
    for (int i = 0; i < 4; ++i) {
        float clock = ZoneTime / 13.0 + float(i) * 0.25;
        float epoch = floor(clock);
        float phase = fract(clock);
        float seed = epoch * 19.0 + float(i) * 53.0;
        float y = flareHash(seed + 1.0) * 1.8 - 0.9;
        float angle = flareHash(seed + 2.0) * 6.2831853;
        float radial = sqrt(1.0 - y * y);
        vec3 axis = vec3(radial * cos(angle), y, radial * sin(angle));
        float cap = smoothstep(0.968, 0.997, dot(direction, axis));
        float envelope = sin(3.14159265 * smoothstep(0.12, 0.88, phase));
        envelope *= envelope;
        float strength = cap * envelope;
        vec3 tangent = normalize(cross(axis, vec3(0.0, 1.0, 0.0)));
        displaced += direction * (strength * 0.045)
                   + tangent * (strength * 0.018 * sin(phase * 6.2831853));
        flareStrength = max(flareStrength, strength);
    }
    // Visual protrusions never change the authoritative poison radius.
    spherePosition = displaced;
    gl_Position = ProjMat * ModelViewMat * vec4(displaced, 1.0);
}
