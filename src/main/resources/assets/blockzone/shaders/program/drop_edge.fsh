#version 150
uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec2 pixel = 1.0 / InSize;
    float center = texture(DiffuseSampler, texCoord).a;
    float adjacent = min(min(texture(DiffuseSampler, texCoord + vec2(pixel.x, 0)).a,
                             texture(DiffuseSampler, texCoord - vec2(pixel.x, 0)).a),
                         min(texture(DiffuseSampler, texCoord + vec2(0, pixel.y)).a,
                             texture(DiffuseSampler, texCoord - vec2(0, pixel.y)).a));
    // Inner edge only: never expand a visible fragment onto the wall hiding it.
    fragColor = vec4(1.0, 1.0, 1.0, max(0.0, center - adjacent));
}
