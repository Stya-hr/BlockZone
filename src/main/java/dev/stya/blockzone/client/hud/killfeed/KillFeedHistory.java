package dev.stya.blockzone.client.hud.killfeed;

import dev.stya.blockzone.client.hud.notice.AnnouncementLifetime;
import dev.stya.blockzone.net.battlezone.KillFeedS2CPacket;
import java.util.ArrayDeque;
import java.util.List;

/** Newest first; bounded independently of whether the HUD is currently visible. */
public final class KillFeedHistory {
    public record Entry(KillFeedS2CPacket notice, AnnouncementLifetime lifetime) { }
    private final ArrayDeque<Entry> entries = new ArrayDeque<>();
    public void add(KillFeedS2CPacket notice, long now) {
        entries.removeIf(e -> now >= e.lifetime().startedAt() + e.lifetime().durationMillis());
        entries.addFirst(new Entry(notice, new AnnouncementLifetime(now, 6000)));
        while (entries.size() > 5) entries.removeLast();
    }
    public List<Entry> visible(long now) {
        entries.removeIf(e -> now >= e.lifetime().startedAt() + e.lifetime().durationMillis());
        return List.copyOf(entries);
    }
    public void clear() { entries.clear(); }
}
