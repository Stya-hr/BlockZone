package dev.stya.blockzone.map.battlezone;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.UUID;

/** A match-only modifier; never overwrites the player's base health or other mods' modifiers. */
public final class CombatHealth {
    private static final UUID MODIFIER = UUID.fromString("bf296ad0-2094-48bd-a52d-920278df9dd9");
    private CombatHealth() { }

    public static void initialize(ServerPlayer player) {
        var attribute = player.getAttribute(Attributes.MAX_HEALTH);
        initialize(attribute);
        player.setHealth(player.getMaxHealth());
    }

    static void initialize(net.minecraft.world.entity.ai.attributes.AttributeInstance attribute) {
        if (attribute != null) {
            attribute.removeModifier(MODIFIER);
            double original = attribute.getValue();
            if (original > 0) attribute.addTransientModifier(new AttributeModifier(MODIFIER,
                    "Blockzone match health", CombatRecovery.MAX_HEALTH / original - 1,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    public static void remove(ServerPlayer player) {
        var attribute = player.getAttribute(Attributes.MAX_HEALTH);
        remove(attribute);
    }

    static void remove(net.minecraft.world.entity.ai.attributes.AttributeInstance attribute) {
        if (attribute != null) attribute.removeModifier(MODIFIER);
    }
}
