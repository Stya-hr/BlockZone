package dev.stya.blockzone.combat;

import com.ptcrys.fpsmatch.common.event.FPSMapEvent;
import com.ptcrys.fpsmatch.core.damage.DamageSourceCategory;
import com.ptcrys.fpsmatch.core.damage.MinecraftDamageSourceClassifier;
import com.ptcrys.fpsmatch.core.map.DeathContext;
import dev.stya.blockzone.map.battlezone.BattlezoneMap;

/** Server notification after elimination. FPSMatch still owns death and kill accounting.
 * Subscribers must copy data they retain; the framework context is mutable.
 */
public final class BattlezoneDeathEvent extends FPSMapEvent {
    public enum Reason { DIRECT, BLEED_OUT, TEAM_WIPE }
    private final DeathContext context;
    private final Reason reason;
    private final DamageSourceCategory category;

    public BattlezoneDeathEvent(BattlezoneMap map, DeathContext context, Reason reason) {
        super(map);
        this.context = context;
        this.reason = reason;
        this.category = context.isGunKill() ? DamageSourceCategory.BULLET
                : MinecraftDamageSourceClassifier.classify(context.getDamageSource());
    }

    public DeathContext getContext() { return context; }
    public Reason getReason() { return reason; }
    public DamageSourceCategory getCategory() { return category; }
    public boolean isPoisonZone() { return context.getDamageSource().is(CombatDamageTypes.POISON_ZONE); }
}
