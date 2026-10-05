package dev.stya.blockzone.data.persistence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ptcrys.fpsmatch.core.capability.CapabilityMap;
import com.ptcrys.fpsmatch.core.map.BaseMap;
import com.ptcrys.fpsmatch.core.team.TeamData;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

/** Team definitions and capability configuration, without any match/player state. */
public final class BattlezoneTeamConfiguration {
    public record Entry(String name, int playerLimit, CapabilityMap.Wrapper capabilities) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(Entry::name),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("playerLimit").forGetter(Entry::playerLimit),
                CapabilityMap.Wrapper.CODEC.fieldOf("capabilities").forGetter(Entry::capabilities)).apply(i, Entry::new));
        public Entry {
            if (name == null || name.isBlank() || playerLimit < 1) throw new IllegalArgumentException("Invalid team definition");
            capabilities = new CapabilityMap.Wrapper(java.util.Map.copyOf(capabilities.data()));
        }
    }
    private BattlezoneTeamConfiguration() { }
    public static List<Entry> capture(BaseMap map) {
        return map.getMapTeams().getTeamsWithSpectator().stream()
                .map(team -> new Entry(team.getName(), Math.max(1, team.getPlayerLimit()), team.getCapabilityMap().getData())).toList();
    }
    public static void validate(List<Entry> entries) {
        var names = new HashSet<String>();
        for (var entry : entries) if (!names.add(entry.name())) throw new IllegalArgumentException("Duplicate team: " + entry.name());
    }
    public static void restore(BaseMap map, List<Entry> entries) {
        validate(entries);
        if (!entries.isEmpty()) {
            var savedNames = entries.stream().map(Entry::name).collect(java.util.stream.Collectors.toSet());
            for (var team : List.copyOf(map.getMapTeams().getNormalTeams())) {
                if (team.isEmpty() && team.getName().startsWith("squad_") && !savedNames.contains(team.getName())) {
                    map.getMapTeams().delTeam(team.getPlayerTeam());
                }
            }
        }
        var data = new LinkedHashMap<String, CapabilityMap.Wrapper>();
        for (var entry : entries) {
            if (map.getMapTeams().getTeamByName(entry.name()).isEmpty()) map.addTeam(new TeamData(entry.name(), entry.playerLimit()));
            data.put(entry.name(), entry.capabilities());
        }
        map.getMapTeams().writeData(data);
    }
}
