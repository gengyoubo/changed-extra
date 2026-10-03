package github.com.gengyoubo.CE.compat.synergy.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/** Authoritative camp state. No entity snapshots and no copies of warehouse inventories. */
public final class LatexSettlementData extends SavedData {
    final Map<UUID, Settlement> settlements = new LinkedHashMap<>();

    public static LatexSettlementData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(LatexSettlementData::load,
                LatexSettlementData::new, "changede_latex_settlements");
    }

    static LatexSettlementData load(CompoundTag root) {
        LatexSettlementData data = new LatexSettlementData();
        for (Tag entry : root.getList("Settlements", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) entry;
            if (!tag.hasUUID("Id") || !tag.hasUUID("Owner")) continue;
            Settlement camp = new Settlement(tag.getUUID("Id"), tag.getUUID("Owner"),
                    tag.getString("Dimension"), BlockPos.of(tag.getLong("Core")));
            camp.active = tag.getBoolean("Active");
            camp.faction = tag.getString("Faction");
            camp.prosperity = Math.max(0, tag.getInt("Prosperity"));
            camp.reputation = tag.getInt("Reputation");
            camp.threat = Math.max(0, Math.min(100, tag.getFloat("Threat")));
            camp.nextRaid = tag.getLong("NextRaid");
            camp.nextVisitor = tag.getLong("NextVisitor");
            camp.raid = enumValue(Raid.class, tag.getString("Raid"), Raid.IDLE);
            camp.raidEnds = tag.getLong("RaidEnds");
            camp.wave = tag.getInt("Wave");
            for (Tag value : tag.getList("Residents", Tag.TAG_COMPOUND)) {
                CompoundTag r = (CompoundTag) value;
                if (!r.hasUUID("Id")) continue;
                Resident resident = new Resident(r.getUUID("Id"), r.getString("Name"));
                resident.displayName = r.getString("DisplayName");
                resident.dimension = r.getString("Dimension");
                resident.workEnabled = !r.contains("WorkEnabled") || r.getBoolean("WorkEnabled");
                resident.role = enumValue(CampRole.class, r.getString("Role"),
                        r.getString("Role").equals("SUPPLY") ? CampRole.COOK : CampRole.NONE);
                resident.health = r.getFloat("Health");
                resident.maxHealth = r.getFloat("MaxHealth");
                resident.lastSeen = r.getLong("LastSeen");
                resident.pos = BlockPos.of(r.getLong("Pos"));
                if (r.contains("Expedition", Tag.TAG_COMPOUND)) resident.legacyExpedition = r.getCompound("Expedition").copy();
                camp.residents.put(resident.id, resident);
            }
            for (Tag value : tag.getList("Storages", Tag.TAG_COMPOUND))
                camp.storages.add(BlockPos.of(((CompoundTag) value).getLong("Pos")));
            for (Tag value : tag.getList("Pending", Tag.TAG_COMPOUND)) {
                ItemStack stack = ItemStack.of((CompoundTag) value);
                if (!stack.isEmpty()) camp.pending.add(stack);
            }
            for (Tag value : tag.getList("Raiders", Tag.TAG_COMPOUND)) {
                CompoundTag raider = (CompoundTag) value;
                if (raider.hasUUID("Id")) camp.raiders.add(raider.getUUID("Id"));
            }
            if (tag.contains("Visitor", Tag.TAG_COMPOUND)) {
                CompoundTag v = tag.getCompound("Visitor");
                if (v.hasUUID("Id")) camp.visitor = new Visitor(v.getUUID("Id"), v.getLong("LeaveAt"),
                        new MerchantOffers(v.getCompound("Offers")));
            }
            data.settlements.put(camp.id, camp);
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag root) {
        root.putInt("Version", 2);
        ListTag list = new ListTag();
        for (Settlement camp : settlements.values()) list.add(camp.save());
        root.put("Settlements", list);
        return root;
    }

    static <T extends Enum<T>> T enumValue(Class<T> type, String value, T fallback) {
        try { return Enum.valueOf(type, value); } catch (IllegalArgumentException ignored) { return fallback; }
    }

    public enum CampRole { NONE, FISHER, FARMER, GUARD, COOK }
    public enum WorkState {
        IDLE, SEARCHING, WALKING, FISHING, HARVESTING, FETCHING, COOKING, DELIVERING, PATROLLING, FIGHTING,
        WAITING_ROD, WAITING_WATER, WAITING_FIELD, WAITING_SEEDS, WAITING_MATERIALS, WAITING_STATION,
        WAITING_STORAGE, WAITING_STOCK, PAUSED, RETURNING
    }
    public enum Raid { IDLE, PREPARING, ACTIVE, COOLDOWN }
    public static final class Resident {
        UUID id;
        String name;
        String displayName = "";
        String dimension = "";
        CampRole role = CampRole.NONE;
        float health, maxHealth;
        long lastSeen;
        BlockPos pos = BlockPos.ZERO;
        CompoundTag legacyExpedition;
        boolean workEnabled = true;
        WorkState workState = WorkState.IDLE;
        BlockPos workTarget;
        Resident(UUID id, String name) { this.id = id; this.name = name; }
        CompoundTag save() {
            CompoundTag tag = new CompoundTag(); tag.putUUID("Id", id); tag.putString("Name", name);
            tag.putString("DisplayName", displayName);
            tag.putString("Dimension", dimension);
            tag.putBoolean("WorkEnabled", workEnabled); tag.putString("Role", role.name()); tag.putFloat("Health", health); tag.putFloat("MaxHealth", maxHealth);
            tag.putLong("LastSeen", lastSeen); tag.putLong("Pos", pos.asLong());
            if (legacyExpedition != null) tag.put("Expedition", legacyExpedition.copy());
            return tag;
        }
    }

    public static final class Visitor {
        UUID id;
        long leaveAt;
        MerchantOffers offers;
        Visitor(UUID id, long leaveAt, MerchantOffers offers) { this.id = id; this.leaveAt = leaveAt; this.offers = offers; }
        CompoundTag save() {
            CompoundTag tag = new CompoundTag(); tag.putUUID("Id", id); tag.putLong("LeaveAt", leaveAt);
            tag.put("Offers", offers.createTag()); return tag;
        }
    }

    public static final class Settlement {
        final UUID id, owner;
        String dimension, faction = "white";
        BlockPos core;
        boolean active = true;
        int prosperity, reputation, wave;
        float threat;
        long nextRaid, raidEnds, nextVisitor;
        Raid raid = Raid.IDLE;
        final Map<UUID, Resident> residents = new LinkedHashMap<>();
        final List<BlockPos> storages = new ArrayList<>();
        final List<ItemStack> pending = new ArrayList<>();
        final Set<UUID> raiders = new LinkedHashSet<>();
        Visitor visitor;
        Settlement(UUID id, UUID owner, String dimension, BlockPos core) {
            this.id = id; this.owner = owner; this.dimension = dimension; this.core = core.immutable();
        }
        CompoundTag save() {
            CompoundTag tag = new CompoundTag(); tag.putUUID("Id", id); tag.putUUID("Owner", owner);
            tag.putString("Dimension", dimension); tag.putLong("Core", core.asLong()); tag.putBoolean("Active", active);
            tag.putString("Faction", faction); tag.putInt("Prosperity", prosperity); tag.putInt("Reputation", reputation);
            tag.putFloat("Threat", threat); tag.putLong("NextRaid", nextRaid); tag.putString("Raid", raid.name());
            tag.putLong("RaidEnds", raidEnds); tag.putInt("Wave", wave); tag.putLong("NextVisitor", nextVisitor);
            ListTag members = new ListTag(); residents.values().forEach(r -> members.add(r.save())); tag.put("Residents", members);
            ListTag storageList = new ListTag(); storages.forEach(pos -> { CompoundTag p = new CompoundTag(); p.putLong("Pos", pos.asLong()); storageList.add(p); });
            tag.put("Storages", storageList);
            ListTag items = new ListTag(); pending.forEach(stack -> items.add(stack.save(new CompoundTag()))); tag.put("Pending", items);
            ListTag enemies = new ListTag(); raiders.forEach(id -> { CompoundTag r = new CompoundTag(); r.putUUID("Id", id); enemies.add(r); }); tag.put("Raiders", enemies);
            if (visitor != null) tag.put("Visitor", visitor.save());
            return tag;
        }
    }
}
