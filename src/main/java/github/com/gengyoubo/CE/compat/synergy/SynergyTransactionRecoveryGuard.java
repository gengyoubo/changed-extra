package github.com.gengyoubo.CE.compat.synergy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Observes a stranded capture without writing recovery metadata into the save. */
final class SynergyTransactionRecoveryGuard {
    static final String TRANSACTION = "ChangedSynergyTransfurNegotiation";
    static final int STABLE_TICKS = 20;

    private CompoundTag parent;
    private CompoundTag transaction;
    private CompoundTag snapshot;
    private long lastTick;
    private int observations;

    static boolean isUnfinishedCapture(CompoundTag data) {
        return "ABSORPTION".equals(data.getString("Mode"))
                && data.getBoolean("CapturePending")
                && data.hasUUID("SourceUuid")
                && data.contains("SourceType", Tag.TAG_STRING)
                && !data.getString("SourceType").isEmpty()
                && !data.contains("ResultPlayerForm")
                && !data.getBoolean("FormRestorePending")
                && !data.getBoolean("ExternalReleasePending")
                && !data.getBoolean("AbsorptionReleasing")
                && !data.getBoolean("ReleaseWhenSafe");
    }

    /** Removes only this exact compound after consecutive, unchanged idle observations. */
    boolean observe(CompoundTag persisted, boolean idleHuman, long tick) {
        if (!idleHuman || !persisted.contains(TRANSACTION, Tag.TAG_COMPOUND)) {
            reset();
            return false;
        }
        CompoundTag current = persisted.getCompound(TRANSACTION);
        if (!isUnfinishedCapture(current)) {
            reset();
            return false;
        }
        if (parent != persisted || transaction != current || !current.equals(snapshot)
                || tick != lastTick + 1) {
            parent = persisted;
            transaction = current;
            snapshot = current.copy();
            observations = 0;
        }
        lastTick = tick;
        if (++observations < STABLE_TICKS) return false;

        persisted.remove(TRANSACTION);
        reset();
        return true;
    }

    private void reset() {
        parent = null;
        transaction = null;
        snapshot = null;
        observations = 0;
    }
}
