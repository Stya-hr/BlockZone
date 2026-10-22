package dev.stya.blockzone.combat;

import com.ptcrys.fpsmatch.common.event.FPSMapEvent;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;
import net.minecraft.server.level.ServerPlayer;
import javax.annotation.Nullable;

/** Non-cancellable notification after an eliminated participant has successfully re-entered the match. */
public final class BattlezoneReviveEvent extends FPSMapEvent {
    private final ServerPlayer source;
    private final ServerPlayer player;
    public BattlezoneReviveEvent(BattlezoneMap map, @Nullable ServerPlayer source, ServerPlayer player) {
        super(map);
        this.source = source;
        this.player = player;
    }
    @Nullable public ServerPlayer getSource() { return source; }
    public ServerPlayer getPlayer() { return player; }
}
