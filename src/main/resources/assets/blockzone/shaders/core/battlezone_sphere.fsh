#version 150
uniform float ZoneTime;
in vec3 spherePosition;
out vec4 fragColor;
void main() {
    vec3 normal = normalize(spherePosition);
    float latitude = asin(clamp(normal.y, -1.0, 1.0));
    float angle = atan(normal.z, normal.x);
    float flow = angle * 6.0 + latitude * 14.0 - ZoneTime * 1.2;
    float warp = sin(latitude * 9.0 + ZoneTime * 0.6);
    float bands = 0.5 + 0.5 * sin(flow + warp * 2.0);
    vec3 color = mix(vec3(0.35, 0.85, 1.0), vec3(1.0, 0.65, 0.95), bands);
    float poleFade = smoothstep(0.0, 0.18, length(normal.xz));
    fragColor = vec4(color, mix(0.12, 0.36, bands) * poleFade);
}
