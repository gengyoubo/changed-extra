package github.com.gengyoubo.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.Random;
import java.util.UUID;

public final class Perseverance {
    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 10;
    private static final String LEVEL_TAG = "changede_perseverance_level";
    private static final long SEED_SALT = 0x4D696D69634C6174L;

    private Perseverance() {
    }

    public static int getLevel(Player player) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(LEVEL_TAG)) {
            return Mth.clamp(data.getInt(LEVEL_TAG), MIN_LEVEL, MAX_LEVEL);
        }
        return getFixedLevel(player.getUUID());
    }

    public static int getFixedLevel(UUID uuid) {
        Random random = new Random(uuid.getMostSignificantBits() ^ Long.rotateLeft(uuid.getLeastSignificantBits(), 21) ^ SEED_SALT);
        int level = MIN_LEVEL;
        while (level < MAX_LEVEL && random.nextDouble() < 0.42D) {
            level++;
        }
        return level;
    }

    public static double getKeepFormChance(Player player) {
        return Mth.clamp(getLevel(player) * 0.1D, 0.0D, 1.0D);
    }

    public static double getMimicTransfurChance(Player player) {
        return Math.max(0.0D, 0.5D - Math.min(getLevel(player), 5) * 0.1D);
    }

    public static boolean rollKeepForm(Player player) {
        return player.getRandom().nextDouble() < getKeepFormChance(player);
    }

    public static boolean rollMimicTransfur(Player player) {
        return player.getRandom().nextDouble() < getMimicTransfurChance(player);
    }
}
