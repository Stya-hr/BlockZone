package dev.stya.blockzone.util.editor;

import dev.stya.blockzone.util.battlezone.PoisonPath;
import java.util.ArrayList;
import java.util.List;

/** Local draft; every operation preserves a nonempty sequence and circle selection. */
public final class PoisonEditorDraft {
    private final ArrayList<PoisonPath> paths;
    private int sequence, circle;
    public PoisonEditorDraft(List<PoisonPath> source) {
        if (source.isEmpty() || source.stream().anyMatch(p -> p.circles().isEmpty()))
            throw new IllegalArgumentException("Keep at least one circle in every sequence");
        paths = new ArrayList<>(source);
    }
    public List<PoisonPath> paths() { return List.copyOf(paths); }
    public int sequence() { return sequence; }
    public int circle() { return circle; }
    public PoisonPath path() { return paths.get(sequence); }
    public PoisonPath.Circle selected() { return path().circles().get(circle); }
    public void selectSequence(int index) { sequence = Math.floorMod(index, paths.size()); circle = 0; }
    public void selectCircle(int index) { circle = Math.floorMod(index, path().circles().size()); }
    public void replace(PoisonPath.Circle value) {
        var circles = new ArrayList<>(path().circles()); circles.set(circle, value);
        paths.set(sequence, new PoisonPath(circles, path().shape()));
    }
    public void setShape(dev.stya.blockzone.util.battlezone.ZoneShape shape) {
        paths.set(sequence, new PoisonPath(path().circles(), shape));
    }
    public void addCircle() {
        if (path().circles().size() >= 128) throw new IllegalArgumentException("Editor limit: 128 circles per sequence");
        var circles = new ArrayList<>(path().circles()); circles.add(circle + 1, selected());
        paths.set(sequence, new PoisonPath(circles, path().shape())); circle++;
    }
    public boolean removeCircle() {
        if (path().circles().size() == 1) return false;
        var circles = new ArrayList<>(path().circles()); circles.remove(circle);
        paths.set(sequence, new PoisonPath(circles, path().shape())); circle = Math.min(circle, circles.size() - 1); return true;
    }
    public void duplicateSequence() { if (paths.size() >= 128) throw new IllegalArgumentException("Editor limit: 128 sequences"); paths.add(sequence + 1, path()); sequence++; }
    public boolean removeSequence() {
        if (paths.size() == 1) return false;
        paths.remove(sequence); sequence = Math.min(sequence, paths.size() - 1); circle = 0; return true;
    }
    public void moveCircle(int delta) {
        int target = circle + delta;
        if (target < 0 || target >= path().circles().size()) return;
        var circles = new ArrayList<>(path().circles());
        java.util.Collections.swap(circles, circle, target);
        paths.set(sequence, new PoisonPath(circles, path().shape())); circle = target;
    }
}
