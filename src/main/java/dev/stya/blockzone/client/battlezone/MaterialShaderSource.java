package dev.stya.blockzone.client.battlezone;

/** Adds a world position varying to vanilla world materials, preserving their existing shading. */
public final class MaterialShaderSource {
    private MaterialShaderSource() {}

    public static String transform(boolean vertex, String name, String source) {
        String path = name.startsWith("minecraft:") ? name.substring(10) : name;
        if (path.contains(":")) return source;
        boolean world = path.matches("rendertype_(solid|cutout|cutout_mipped|translucent|translucent_moving_block|tripwire|eyes|entity_.*|armor_cutout_no_cull)")
                || path.equals("particle");
        if (!world || !source.matches("(?s).*void\\s+main\\s*\\(\\s*\\)\\s*\\{.*")) return source;
        String declarations = vertex
                ? "\nuniform mat4 BlockzoneClipToWorld;\nuniform float BlockzoneMaterialsActive;\nout vec3 BlockzoneWorldPosition;\n"
                : "\nin vec3 BlockzoneWorldPosition;\nuniform float BlockzoneMaterialsActive;\nuniform vec3 BlockzoneZoneCenter;\nuniform float BlockzoneZoneRadius;\n";
        // Keep #version, #extension and resource-pack imports in their original order.
        String modified = source.replaceFirst("void\\s+main\\s*\\(\\s*\\)",
                declarations + "void blockzoneOriginalMain()");
        return modified + (vertex ? """

                void main() {
                    blockzoneOriginalMain();
                    BlockzoneWorldPosition = vec3(0.0);
                    if (BlockzoneMaterialsActive > 0.5) {
                        vec4 world = BlockzoneClipToWorld * gl_Position;
                        BlockzoneWorldPosition = world.xyz / world.w;
                    }
                }
                """ : """

                void main() {
                    blockzoneOriginalMain();
                    vec3 delta = BlockzoneWorldPosition - BlockzoneZoneCenter;
                    if (BlockzoneMaterialsActive > 0.5 && (BlockzoneZoneRadius <= 0.0
                        || dot(delta, delta) > BlockzoneZoneRadius * BlockzoneZoneRadius)) {
                        float luminance = clamp(dot(fragColor.rgb, vec3(0.2126, 0.7152, 0.0722)), 0.0, 1.0);
                        // Lift shadows without flattening texture and lighting contrast.
                        float grey = 0.16 + 0.70 * sqrt(luminance);
                        fragColor.rgb = vec3(grey);
                    }
                }
                """);
    }
}
