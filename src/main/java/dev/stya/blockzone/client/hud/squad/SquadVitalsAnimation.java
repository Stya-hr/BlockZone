package dev.stya.blockzone.client.hud.squad;

import dev.stya.blockzone.squad.SquadMemberStatus;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Animates every roster member, including members on another page, by stable UUID. */
final class SquadVitalsAnimation {
    private final Map<UUID, State> states = new HashMap<>();
    private String scope = "";
    private double lastFrame = Double.NEGATIVE_INFINITY;
    void update(List<SquadMemberStatus> members, String newScope, double now) {
        if (!scope.equals(newScope) || now - lastFrame > 1) states.clear();
        scope = newScope;
        lastFrame = now;
        var present = new HashSet<UUID>();
        for (var member : members) {
            present.add(member.id());
            var state = states.get(member.id());
            int mode = (member.online() ? 1 : 0) | (member.living() ? 2 : 0) | (member.downed() ? 4 : 0);
            if (state == null || state.mode != mode || state.armor.length != member.plateSlots()) {
                state = new State(member, mode, now);
                states.put(member.id(), state);
            }
            if (member.online() && member.living()
                    && (member.health() < state.lastHealth || member.armor() < state.lastArmor)) state.hitAt = now;
            state.lastHealth = member.health();
            state.lastArmor = member.armor();
            state.health.update(member.healthFraction(), now);
            for (int i = 0; i < state.armor.length; i++) state.armor[i].update(member.armorFraction(i), now);
        }
        states.keySet().retainAll(present);
    }
    State get(UUID id) { return states.get(id); }
    static final class State {
        final int mode;
        float lastHealth, lastArmor;
        double hitAt = Double.NEGATIVE_INFINITY;
        final VitalBarMotion health;
        final VitalBarMotion[] armor;
        State(SquadMemberStatus member, int mode, double now) {
            this.mode = mode;
            lastHealth = member.health();
            lastArmor = member.armor();
            health = new VitalBarMotion(member.healthFraction(), now);
            armor = new VitalBarMotion[member.plateSlots()];
            for (int i = 0; i < armor.length; i++) armor[i] = new VitalBarMotion(member.armorFraction(i), now);
        }
    }
}
