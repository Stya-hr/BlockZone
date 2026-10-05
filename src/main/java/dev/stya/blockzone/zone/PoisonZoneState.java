package dev.stya.blockzone.zone;

import dev.stya.blockzone.util.battlezone.PoisonPath;
import dev.stya.blockzone.util.battlezone.ZoneGeometry;
import dev.stya.blockzone.util.battlezone.ZoneShape;
import java.util.List;

/** Per-match zone progression, independent of players, world ticks and packet transport. */
public final class PoisonZoneState {
    private PoisonPath path;
    private List<ZoneGeometry> sequence = List.of();
    private List<PoisonPath.Circle> phases = List.of();
    private double baseDamage;
    private int phaseIndex;
    private int phaseTicks;
    private float radius;
    private double centerX, centerZ;
    private float startRadius, targetRadius;
    private double startX, startZ, targetX, targetZ;
    private boolean stageInitialized;

    public void initialize(PoisonPath path, List<ZoneGeometry> sequence, double baseDamage) {
        this.path = path;
        this.sequence = List.copyOf(sequence);
        this.phases = path.circles().stream().skip(1).toList();
        this.baseDamage = baseDamage;
        restart();
    }

    public void restart() { reset(sequence.get(0)); }

    public void reset(ZoneGeometry initial) {
        phaseIndex = 0;
        phaseTicks = 0;
        radius = (float) initial.radius();
        centerX = initial.centerX();
        centerZ = initial.centerZ();
        stageInitialized = false;
    }

    public void tick() {
        if (phaseIndex >= phases.size()) return;
        var stage = phases.get(phaseIndex);
        if (!stageInitialized) {
            var target = sequence.get(phaseIndex + 1);
            startRadius = radius;
            startX = centerX;
            startZ = centerZ;
            targetRadius = (float) target.radius();
            targetX = target.centerX();
            targetZ = target.centerZ();
            stageInitialized = true;
        }
        int waitTicks = Math.max(0, stage.waitSeconds()) * 20;
        int shrinkTicks = Math.max(0, stage.shrinkSeconds()) * 20;
        phaseTicks++;
        if (phaseTicks <= waitTicks) {
            radius = startRadius;
            centerX = startX;
            centerZ = startZ;
        } else if (shrinkTicks == 0 || phaseTicks >= waitTicks + shrinkTicks) {
            radius = targetRadius;
            centerX = targetX;
            centerZ = targetZ;
            phaseIndex++;
            phaseTicks = 0;
            stageInitialized = false;
        } else {
            float progress = (float) (phaseTicks - waitTicks) / shrinkTicks;
            radius = startRadius + (targetRadius - startRadius) * progress;
            centerX = startX + (targetX - startX) * progress;
            centerZ = startZ + (targetZ - startZ) * progress;
        }
    }

    public ZoneGeometry current(double centerY) { return new ZoneGeometry(centerX, centerY, centerZ, radius, shape()); }
    public ZoneShape shape() { return path == null ? ZoneShape.CYLINDER : path.shape(); }
    public List<ZoneGeometry> sequence() { return sequence; }
    public double damage() { return path.damageAt(phaseIndex, baseDamage); }
}
