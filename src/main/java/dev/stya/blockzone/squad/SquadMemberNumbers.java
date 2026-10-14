package dev.stya.blockzone.squad;

import java.util.*;

/** Keep a member's number/color until they actually leave, including offline members. */
public final class SquadMemberNumbers {
    private final Map<UUID, Integer> numbers = new HashMap<>();
    public void update(Collection<UUID> roster) {
        numbers.keySet().retainAll(roster);
        for (var id : roster.stream().sorted(Comparator.comparing(UUID::toString)).toList()) {
            if (numbers.containsKey(id)) continue;
            int next = 1;
            while (numbers.containsValue(next)) next++;
            numbers.put(id, next);
        }
    }
    public int number(UUID id) { return numbers.getOrDefault(id, 0); }
}
