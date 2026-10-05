package dev.stya.blockzone.combat;

import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatHealthTest {
    private AttributeInstance health(double base) {
        return new AttributeInstance(new RangedAttribute("test.health", base, 1, 1024), value -> { });
    }

    @Test void matchHealthDoesNotChangeBaseAndRestoresOnExit() {
        var health = health(20);
        CombatHealth.initialize(health, 100);
        assertEquals(100, health.getValue(), 0.0001);
        assertEquals(20, health.getBaseValue());
        CombatHealth.remove(health);
        assertEquals(20, health.getValue());
    }

    @Test void customMatchHealthRestoresOriginalValue() {
        var health = health(20);
        CombatHealth.initialize(health, 200);
        assertEquals(200, health.getValue(), 0.0001);
        CombatHealth.remove(health);
        assertEquals(20, health.getValue());
    }

    @Test void repeatedInitializationDoesNotStack() {
        var health = health(40);
        CombatHealth.initialize(health, 100);
        CombatHealth.initialize(health, 100);
        assertEquals(100, health.getValue(), 0.0001);
        CombatHealth.remove(health);
        assertEquals(40, health.getValue());
    }

    @Test void preservesOtherModsMultiplicativeAndAdditiveModifiers() {
        var health = health(20);
        var bonus = new AttributeModifier(UUID.randomUUID(), "other mod", 10, AttributeModifier.Operation.ADDITION);
        var multiplier = new AttributeModifier(UUID.randomUUID(), "other scale", .5, AttributeModifier.Operation.MULTIPLY_TOTAL);
        health.addTransientModifier(bonus);
        health.addTransientModifier(multiplier);
        assertEquals(45, health.getValue());
        CombatHealth.initialize(health, 100);
        assertEquals(100, health.getValue(), 0.0001);
        CombatHealth.remove(health);
        assertEquals(45, health.getValue());
        assertEquals(bonus, health.getModifier(bonus.getId()));
        assertEquals(multiplier, health.getModifier(multiplier.getId()));
    }
}
