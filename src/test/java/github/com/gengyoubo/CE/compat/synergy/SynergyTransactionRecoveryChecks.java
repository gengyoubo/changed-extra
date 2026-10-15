package github.com.gengyoubo.CE.compat.synergy;

import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

public final class SynergyTransactionRecoveryChecks {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static CompoundTag capture() {
        var tag = new CompoundTag();
        tag.putString("Mode", "ABSORPTION");
        tag.putBoolean("CapturePending", true);
        tag.putUUID("SourceUuid", UUID.fromString("00000000-0000-0000-0000-000000000001"));
        tag.putString("SourceType", "changed:dark_latex_wolf_male");
        return tag;
    }

    public static void main(String[] args) {
        var root = new CompoundTag();
        root.putString("Unrelated", "preserved");
        root.put(SynergyTransactionRecoveryGuard.TRANSACTION, capture());
        var guard = new SynergyTransactionRecoveryGuard();
        for (int tick = 0; tick < SynergyTransactionRecoveryGuard.STABLE_TICKS - 1; tick++) {
            check(!guard.observe(root, true, tick), "Capture must remain until stable timeout");
        }
        check(guard.observe(root, true, 19), "Stable stranded capture must be retired");
        check(!root.contains(SynergyTransactionRecoveryGuard.TRANSACTION)
                && root.getString("Unrelated").equals("preserved"), "Recovery must preserve unrelated data");

        for (String flag : new String[]{"ResultPlayerForm", "FormRestorePending", "ExternalReleasePending",
                "AbsorptionReleasing", "ReleaseWhenSafe"}) {
            var transaction = capture();
            transaction.putBoolean(flag, true);
            check(!SynergyTransactionRecoveryGuard.isUnfinishedCapture(transaction), "Active recovery flag: " + flag);
        }
        var incomplete = capture();
        incomplete.remove("SourceUuid");
        check(!SynergyTransactionRecoveryGuard.isUnfinishedCapture(incomplete), "Missing source identity must not be retired");

        root.put(SynergyTransactionRecoveryGuard.TRANSACTION, capture());
        for (int tick = 0; tick < 19; tick++) check(!guard.observe(root, true, tick), "Before reset");
        check(!guard.observe(root, false, 19), "Active player must reset observation");
        for (int tick = 20; tick < 39; tick++) check(!guard.observe(root, true, tick), "Idle timeout must restart");
        root.getCompound(SynergyTransactionRecoveryGuard.TRANSACTION).putString("Progress", "changed");
        check(!guard.observe(root, true, 39), "Transaction changes must reset observation");
        check(!guard.observe(root, true, 100), "Tick gaps must reset observation");
        for (int tick = 101; tick < 119; tick++) check(!guard.observe(root, true, tick), "After tick gap");
        check(guard.observe(root, true, 119), "Stable timeout after tick gap");
        System.out.println("All Synergy transaction recovery checks passed");
    }
}
