#version 150
uniform sampler2D Sampler0;
uniform vec4 UvRect;
uniform vec4 MapBounds;
uniform vec4 SafeZone;
uniform float ViewSpan;
in vec2 texCoord;
out vec4 fragColor;
// The shared HUD pass already draws the backing, including before terrain is ready.
const vec4 BACKGROUND = vec4(0.0);
void main() {
    vec2 uv = (texCoord - UvRect.xy) / UvRect.zw;
    vec2 world = (vec2(uv.x, 1.0 - uv.y) - 0.5) * ViewSpan;
    if (any(lessThan(world, MapBounds.xy)) || any(greaterThanEqual(world, MapBounds.zw))) {
        fragColor = BACKGROUND;
        return;
    }
    vec4 terrain = texture(Sampler0, texCoord);
    if (terrain.a < 0.01) {
        fragColor = BACKGROUND;
        return;
    }
    vec3 rgb = clamp(terrain.rgb / max(terrain.a, 0.01), 0.0, 1.0);
    float light = dot(rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 color = mix(vec3(0.12, 0.17, 0.19), vec3(0.72, 0.75, 0.69), smoothstep(0.12, 0.9, light));
    if (rgb.g > rgb.r * 1.12 && rgb.g > rgb.b * 1.10) color = mix(vec3(0.17, 0.24, 0.22), vec3(0.32, 0.39, 0.31), light);
    if (rgb.b > rgb.r * 1.25 && rgb.b > rgb.g * 1.1) color = vec3(0.12, 0.25, 0.31);
    vec2 delta = abs(world - SafeZone.xy);
    float distanceToZone = (SafeZone.w > 0.5 ? max(delta.x, delta.y) : length(delta)) - SafeZone.z;
    float pixel = max(length(dFdx(world)), length(dFdy(world)));
    if (distanceToZone > 0.0) color = mix(color, vec3(0.53, 0.17, 0.33), 0.48);
    float line = 1.0 - smoothstep(pixel * 0.65, pixel * 1.65, abs(distanceToZone));
    color = mix(color, vec3(0.86, 0.95, 0.91), line);
    fragColor = vec4(color * 0.85, 0.85);
}
