package github.com.gengyoubo.CE.compat.twilight;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Guardian deaths remain authoritative even while their owner 009 is unloaded. */
public final class TwilightGuardianDeaths extends SavedData {
    private final Set<UUID> dead = new HashSet<>(), lost = new HashSet<>();
    private static TwilightGuardianDeaths load(CompoundTag tag) {
        var result = new TwilightGuardianDeaths();
        for (String key : new String[]{"dead", "lost"}) {
            var list = tag.getList(key, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) (key.equals("dead") ? result.dead : result.lost).add(list.getCompound(i).getUUID("id"));
        }
        return result;
    }
    public static TwilightGuardianDeaths get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TwilightGuardianDeaths::load, TwilightGuardianDeaths::new, "changede_twilight_guardian_deaths");
    }
    @Override public CompoundTag save(CompoundTag tag) {
        for (String key : new String[]{"dead", "lost"}) {
            ListTag list = new ListTag();
            for (UUID id : key.equals("dead") ? dead : lost) { var entry = new CompoundTag(); entry.putUUID("id", id); list.add(entry); }
            tag.put(key, list);
        }
        return tag;
    }
    public boolean dead(UUID id) { return dead.contains(id); }
    public boolean lost(UUID id) { return lost.contains(id); }
    public void markDead(UUID id) { dead.add(id); lost.remove(id); setDirty(); }
    public void markLost(UUID id) { if (!dead.contains(id)) { lost.add(id); setDirty(); } }
    public void forget(UUID id) { if (dead.remove(id) | lost.remove(id)) setDirty(); }
}
