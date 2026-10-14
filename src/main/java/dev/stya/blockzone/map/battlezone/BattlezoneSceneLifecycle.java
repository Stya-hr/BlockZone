package dev.stya.blockzone.map.battlezone;

import dev.stya.blockzone.data.snapshot.SceneSnapshot;
import dev.stya.blockzone.editor.loot.LootCrateEdit;
import java.util.List;
import java.util.function.Consumer;

/** Owns snapshot validity and deferred saves; the map owns match phase transitions. */
final class BattlezoneSceneLifecycle {
    private final SceneSnapshot snapshot;
    private boolean valid;
    private boolean savePending;

    BattlezoneSceneLifecycle(BattlezoneMap map) {
        snapshot = new SceneSnapshot(map);
        valid = snapshot.load();
        if (!valid && !snapshot.exists()) snapshot.beginSave();
    }

    void tick() {
        snapshot.tick();
        if (savePending && !snapshot.isBusy()) {
            savePending = false;
            beginSave();
        }
        valid = snapshot.hasValidSnapshot();
    }

    void onAreaChanged() {
        valid = false;
        if (snapshot.isBusy()) savePending = true;
        else snapshot.beginSave();
    }

    boolean beginSave() {
        boolean started = snapshot.beginSave();
        if (started) valid = false;
        return started;
    }

    boolean hasValidSnapshot() { return valid; }
    boolean isBusy() { return snapshot.isBusy(); }
    boolean lastOperationSucceeded() { return snapshot.lastOperationSucceeded(); }
    boolean beginRestore() { return snapshot.beginRestore(); }

    boolean saveLootCrateEdits(List<LootCrateEdit> changes, Consumer<Boolean> result) {
        return snapshot.saveLootCrateEdits(changes, result);
    }
}
