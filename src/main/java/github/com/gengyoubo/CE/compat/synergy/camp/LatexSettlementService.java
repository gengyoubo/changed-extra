package github.com.gengyoubo.CE.compat.synergy.camp;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.network.NetworkHooks;
import net.parkabird.changedsynergy.ai.*;

import java.util.*;

import static github.com.gengyoubo.CE.compat.synergy.camp.LatexSettlementData.*;

public final class LatexSettlementService {
    static final String MEMBER = "changede_camp_resident", VISITOR = "changede_camp_visitor", RAIDER = "changede_camp_raider";
    static final String FROZEN = "changede_camp_expedition_flags";
    private LatexSettlementService() {}
    static long now(ServerLevel level) { return level.getServer().overworld().getGameTime(); }
    static ServerLevel level(net.minecraft.server.MinecraftServer server, Settlement camp) {
        ResourceLocation id = ResourceLocation.tryParse(camp.dimension);
        return id == null ? null : server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id));
    }
    static Settlement resolve(ServerLevel level, LatexCampfireBlockEntity core) {
        if (core.settlementId == null) return null;
        Settlement camp = LatexSettlementData.get(level.getServer()).settlements.get(core.settlementId);
        return camp != null && camp.active && camp.dimension.equals(level.dimension().location().toString())
                && camp.core.equals(core.getBlockPos()) ? camp : null;
    }
    static Settlement member(ChangedEntity mob) {
        if (!(mob.level() instanceof ServerLevel level) || !mob.getPersistentData().hasUUID(MEMBER)) return null;
        Settlement camp = LatexSettlementData.get(level.getServer()).settlements.get(mob.getPersistentData().getUUID(MEMBER));
        return camp != null && camp.residents.containsKey(mob.getUUID()) ? camp : null;
    }
    static void claim(ServerLevel level, LatexCampfireBlockEntity core, ServerPlayer owner) {
        LatexSettlementData data = LatexSettlementData.get(level.getServer());
        Settlement camp = data.settlements.values().stream().filter(c -> !c.active && c.owner.equals(owner.getUUID()))
                .reduce((first, last) -> last).orElse(null);
        if (camp == null) {
            camp = new Settlement(UUID.randomUUID(), owner.getUUID(), level.dimension().location().toString(), core.getBlockPos());
            data.settlements.put(camp.id, camp);
        } else {
            camp.dimension = level.dimension().location().toString(); camp.core = core.getBlockPos().immutable(); camp.active = true;
            camp.storages.clear();
        }
        camp.nextRaid = now(level) + 12000; camp.nextVisitor = now(level) + 2400;
        core.bind(camp); data.setDirty();
    }
    static void open(ServerPlayer player, LatexCampfireBlockEntity core) {
        ServerLevel level = player.serverLevel();
        if (resolve(level, core) == null) return;
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, p) -> new LatexCampfireMenu(id, inventory,
                core.getBlockPos(), core.settlementId), Component.translatable("block.changede.latex_campfire")),
                buf -> { buf.writeBlockPos(core.getBlockPos()); buf.writeUUID(core.settlementId); });
    }
    static void deactivate(ServerLevel level, LatexCampfireBlockEntity core) {
        Settlement camp = resolve(level, core);
        if (camp == null) return;
        camp.active = false; core.active = false;
        for (Resident resident : camp.residents.values()) {
            cancelLegacy(level, camp, resident);
            ChangedEntity mob = anywhere(level.getServer(), resident.id);
            if (mob != null) { thaw(mob); cancelWork(mob, camp); }
        }
        endRaid(level, camp, false);
        removeVisitor(level, camp);
        LatexSettlementData.get(level.getServer()).setDirty();
    }
    static ChangedEntity entity(ServerLevel level, UUID id) {
        return level.getEntity(id) instanceof ChangedEntity mob && mob.isAlive() ? mob : null;
    }
    static ChangedEntity anywhere(net.minecraft.server.MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) { ChangedEntity found = entity(level, id); if (found != null) return found; }
        return null;
    }
    static boolean eligible(ChangedEntity mob, Settlement camp, ServerPlayer player) {
        return mob.isAlive() && !mob.isNoAi() && !mob.isPassenger() && !mob.isVehicle()
                && !mob.getPersistentData().hasUUID(MEMBER) && !mob.getPersistentData().hasUUID(VISITOR)
                && !mob.getPersistentData().hasUUID(RAIDER) && mob.getPersistentData().getString("changede_maid_work_task").isEmpty()
                && PlayerOutpostData.get(player.server).byMember(mob.getUUID()).isEmpty()
                && PlayerOutpostService.authorized(mob, player)
                && mob.blockPosition().distSqr(camp.core) <= 32 * 32;
    }
    static List<ChangedEntity> candidates(ServerLevel level, Settlement camp, ServerPlayer player) {
        if (!camp.owner.equals(player.getUUID())) return List.of();
        return level.getEntitiesOfClass(ChangedEntity.class, new AABB(camp.core).inflate(32), mob -> eligible(mob, camp, player));
    }
    static void enroll(ServerLevel level, Settlement camp, ChangedEntity mob, ServerPlayer owner) {
        if (camp.residents.size() >= 16 || !eligible(mob, camp, owner)) return;
        Resident resident = new Resident(mob.getUUID(), mob.getName().getString());
        camp.residents.put(resident.id, resident);
        if (camp.residents.size() == 1) camp.faction = HunterFaction.of(mob).id();
        CreatureCommunityData.detach(mob);
        LatexSocialMemory.setFollowingOwner(mob, false);
        CreaturePersonality.setSocialFollowing(mob, owner, false);
        mob.getPersistentData().putUUID(MEMBER, camp.id); mob.setPersistenceRequired(); mob.setTarget(null);
        ensureGoal(mob); updateResident(mob, resident, now(level));
        if (level.getBlockEntity(camp.core) instanceof LatexCampfireBlockEntity core) core.bind(camp);
    }
    static void ensureGoal(ChangedEntity mob) {
        if (mob.goalSelector.getAvailableGoals().stream().noneMatch(g -> g.getGoal() instanceof LatexCampGoal))
            mob.goalSelector.addGoal(-1, new LatexCampGoal(mob));
    }
    static void thaw(ChangedEntity mob) {
        if (!mob.getPersistentData().contains(FROZEN, Tag.TAG_COMPOUND)) return;
        CompoundTag flags = mob.getPersistentData().getCompound(FROZEN);
        mob.setNoAi(flags.getBoolean("NoAI")); mob.setInvisible(flags.getBoolean("Invisible"));
        mob.setInvulnerable(flags.getBoolean("Invulnerable")); mob.setNoGravity(flags.getBoolean("NoGravity"));
        mob.getPersistentData().remove(FROZEN);
        CampNetwork.visibility(mob, false);
    }
    static void cancelLegacy(ServerLevel level, Settlement camp, Resident resident) {
        CompoundTag old = resident.legacyExpedition;
        if (old == null) return;
        resident.legacyExpedition = null;
        for (Tag entry : old.getList("Cargo", Tag.TAG_COMPOUND)) {
            ItemStack stack = ItemStack.of((CompoundTag) entry);
            if (!stack.isEmpty()) camp.pending.add(stack);
        }
        LatexSettlementData.get(level.getServer()).setDirty();
    }
    static void cancelWork(ChangedEntity mob, Settlement camp) {
        CampResidentWork.cancel(mob);
        camp.pending.addAll(CampWorkBuffer.takeCargo(mob));
        Resident resident = camp.residents.get(mob.getUUID());
        if (resident != null) { resident.workState = WorkState.IDLE; resident.workTarget = null; }
    }
    static void updateResident(ChangedEntity mob, Resident resident, long time) {
        resident.name = mob.getName().getString(); resident.health = mob.getHealth(); resident.maxHealth = mob.getMaxHealth();
        resident.displayName = Component.Serializer.toJson(mob.getDisplayName());
        resident.dimension = mob.level().dimension().location().toString();
        resident.lastSeen = time; resident.pos = mob.blockPosition();
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.overworld().getGameTime() % 20 != 0) return;
        LatexSettlementData data = LatexSettlementData.get(server);
        for (Settlement camp : data.settlements.values()) {
            ServerLevel level = level(server, camp);
            if (level == null) continue;
            long time = now(level);
            boolean loaded = camp.active && level.hasChunkAt(camp.core);
            if (loaded && (!(level.getBlockEntity(camp.core) instanceof LatexCampfireBlockEntity core)
                    || !camp.id.equals(core.settlementId))) {
                camp.active = false; loaded = false; endRaid(level, camp, false); removeVisitor(level, camp);
            }
            for (Resident resident : camp.residents.values()) {
                cancelLegacy(level, camp, resident);
                ChangedEntity mob = anywhere(server, resident.id);
                if (mob != null) {
                    thaw(mob); updateResident(mob, resident, time); ensureGoal(mob);
                    if (!camp.active) cancelWork(mob, camp);
                }
            }
            if (loaded) {
                if (!camp.pending.isEmpty()) CampWarehouse.deposit(level, camp);
                raidTick(level, camp, time);
                visitorTick(level, camp, time);
            }
        }
        if (!data.settlements.isEmpty()) data.setDirty();
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void livingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ChangedEntity mob) || !(mob.level() instanceof ServerLevel)) return;
        thaw(mob); // Recovery only: resident work never freezes or hides the entity.
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void join(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ChangedEntity mob) || !(event.getLevel() instanceof ServerLevel level)) return;
        LatexSettlementData data = LatexSettlementData.get(level.getServer());
        Settlement camp = member(mob);
        if (camp != null) {
            ensureGoal(mob);
            Resident resident = camp.residents.get(mob.getUUID());
            cancelLegacy(level, camp, resident); thaw(mob);
        } else { mob.getPersistentData().remove(MEMBER); thaw(mob); }
        for (String key : List.of(VISITOR, RAIDER)) if (mob.getPersistentData().hasUUID(key)) {
            Settlement owner = data.settlements.get(mob.getPersistentData().getUUID(key));
            boolean valid = owner != null && owner.active && (key.equals(VISITOR)
                    ? owner.visitor != null && owner.visitor.id.equals(mob.getUUID()) && now(level) < owner.visitor.leaveAt
                    : owner.raid == Raid.ACTIVE && owner.raiders.contains(mob.getUUID()));
            if (!valid) { event.setCanceled(true); return; }
            ensureGoal(mob);
        }
    }
    @SubscribeEvent public static void track(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof ChangedEntity mob)
            CampNetwork.visibility(player, mob, mob.getPersistentData().contains(FROZEN));
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void death(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ChangedEntity mob) || !(mob.level() instanceof ServerLevel level)) return;
        LatexSettlementData data = LatexSettlementData.get(level.getServer());
        Settlement camp = member(mob);
        if (camp != null) {
            Resident resident = camp.residents.remove(mob.getUUID());
            cancelLegacy(level, camp, resident);
        }
        for (Settlement owner : data.settlements.values()) {
            owner.raiders.remove(mob.getUUID());
            if (owner.visitor != null && owner.visitor.id.equals(mob.getUUID())) owner.visitor = null;
        }
        CampResidentWork.cancel(mob, null);
        for (ItemStack stack : CampWorkBuffer.takeCargo(mob)) mob.spawnAtLocation(stack);
        mob.getPersistentData().remove(MEMBER); data.setDirty();
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof ChangedEntity mob) || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (mob.getPersistentData().contains(FROZEN)) { event.setCanceled(true); event.setCancellationResult(InteractionResult.FAIL); return; }
        if (!mob.getPersistentData().hasUUID(VISITOR)) return;
        Settlement camp = LatexSettlementData.get(player.server).settlements.get(mob.getPersistentData().getUUID(VISITOR));
        if (camp == null || !camp.active || camp.visitor == null || !camp.visitor.id.equals(mob.getUUID())
                || mob.distanceToSqr(player) > 64) return;
        if (player.serverLevel().getBlockEntity(camp.core) instanceof LatexCampfireBlockEntity core) open(player, core);
        event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
    }

    static boolean playersNearby(ServerLevel level, Settlement camp) {
        return level.players().stream().anyMatch(player -> player.blockPosition().distSqr(camp.core) < 64 * 64 && !player.isSpectator());
    }
    static ChangedEntity spawn(ServerLevel level, Settlement camp, boolean dark, int radius, String marker) {
        var type = dark ? ChangedEntities.DARK_LATEX_WOLF_MALE.get() : ChangedEntities.PURE_WHITE_LATEX_WOLF.get();
        for (int tries = 0; tries < 16; tries++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            int x = camp.core.getX() + (int) (Math.cos(angle) * radius), z = camp.core.getZ() + (int) (Math.sin(angle) * radius);
            BlockPos column = new BlockPos(x, camp.core.getY(), z);
            if (!level.hasChunkAt(column)) continue;
            BlockPos pos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            if (Math.abs(pos.getY() - camp.core.getY()) > 12 || !level.getWorldBorder().isWithinBounds(pos)) continue;
            ChangedEntity mob = type.create(level);
            if (mob == null) return null;
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360, 0);
            if (!level.noCollision(mob) || level.containsAnyLiquid(mob.getBoundingBox())) continue;
            mob.getPersistentData().putUUID(marker, camp.id); mob.setPersistenceRequired(); ensureGoal(mob);
            // Register identity before EntityJoinLevelEvent validates it.
            if (marker.equals(RAIDER)) camp.raiders.add(mob.getUUID());
            else camp.visitor = new Visitor(mob.getUUID(), now(level) + 6000, offers(camp));
            if (level.addFreshEntity(mob)) return mob;
            camp.raiders.remove(mob.getUUID()); if (marker.equals(VISITOR)) camp.visitor = null;
        }
        return null;
    }
    static void raidTick(ServerLevel level, Settlement camp, long time) {
        if (camp.raid == Raid.PREPARING && time >= camp.raidEnds) {
            if (!playersNearby(level, camp)) return;
            camp.raid = Raid.ACTIVE; camp.raidEnds = time + 12000; camp.wave = 0; camp.nextRaid = time;
        }
        if (camp.raid == Raid.ACTIVE) {
            if (camp.raiders.isEmpty() && camp.wave >= 2) { endRaid(level, camp, true); return; }
            if (time >= camp.raidEnds) { endRaid(level, camp, false); return; }
            if (camp.raiders.isEmpty() && camp.wave < 2 && time >= camp.nextRaid && playersNearby(level, camp)) {
                camp.nextRaid = time + 200;
                int count = Math.min(6, 2 + camp.residents.size() / 4);
                for (int i = 0; i < count; i++) spawn(level, camp, !camp.faction.equals("dark"), 26, RAIDER);
                if (!camp.raiders.isEmpty()) camp.wave++;
            }
            if (camp.raiders.isEmpty() && camp.wave >= 2) endRaid(level, camp, true);
            else if (time >= camp.raidEnds) endRaid(level, camp, false);
            return;
        }
        if (camp.raid == Raid.COOLDOWN && time >= camp.nextRaid) camp.raid = Raid.IDLE;
        if (camp.raid != Raid.IDLE || time < camp.nextRaid) return;
        camp.nextRaid = time + 12000;
        HunterFaction faction = HunterFaction.fromId(camp.faction);
        HunterFaction opposite = faction == HunterFaction.WHITE ? HunterFaction.DARK : faction == HunterFaction.DARK ? HunterFaction.WHITE : null;
        int hostile = opposite == null ? 0 : level.getEntitiesOfClass(ChangedEntity.class, new AABB(camp.core).inflate(48),
                mob -> mob.isAlive() && !camp.residents.containsKey(mob.getUUID()) && !mob.getPersistentData().hasUUID(VISITOR)
                        && HunterFaction.of(mob) == opposite).size();
        boolean border = opposite != null && net.parkabird.changedsynergy.dialogue.LatexTerritory.isInOrNearFactionTerritory(level, camp.core, opposite, 32);
        camp.threat = Math.min(100, camp.threat + camp.residents.size() * 1.5F + camp.prosperity * 0.2F + Math.min(12, hostile * 2) + (border ? 8 : 0));
        if (!camp.residents.isEmpty() && playersNearby(level, camp) && level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
                && level.getDifficulty() != Difficulty.PEACEFUL && level.random.nextFloat() < camp.threat / 150) {
            camp.raid = Raid.PREPARING; camp.raidEnds = time + 2400;
            for (ServerPlayer player : level.players()) if (player.blockPosition().distSqr(camp.core) < 64 * 64)
                player.displayClientMessage(Component.translatable("camp.changede.raid_warning"), false);
        }
    }
    static void endRaid(ServerLevel level, Settlement camp, boolean victory) {
        if (victory) { camp.prosperity += 3; camp.reputation += 2; camp.pending.add(new ItemStack(Items.EMERALD, 2)); }
        else if (camp.raid == Raid.ACTIVE) camp.prosperity = Math.max(0, camp.prosperity - 3);
        for (UUID id : camp.raiders) { ChangedEntity mob = anywhere(level.getServer(), id); if (mob != null) mob.discard(); }
        camp.raiders.clear(); camp.raid = Raid.COOLDOWN; camp.nextRaid = now(level) + 24000; camp.threat *= 0.5F;
    }
    static MerchantOffers offers(Settlement camp) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemStack(Items.EMERALD), new ItemStack(Items.SWEET_BERRIES, 8), 8, 0, 0));
        offers.add(new MerchantOffer(new ItemStack(Items.COOKED_BEEF, 8), new ItemStack(Items.EMERALD), 6, 0, 0));
        offers.add(new MerchantOffer(new ItemStack(Items.EMERALD, 3), new ItemStack(camp.faction.equals("dark") ? Items.IRON_AXE : Items.GOLDEN_APPLE), 4, 0, 0));
        if (camp.prosperity >= 10) offers.add(new MerchantOffer(new ItemStack(Items.EMERALD, 6), new ItemStack(Items.DIAMOND), 2, 0, 0));
        return offers;
    }
    static void visitorTick(ServerLevel level, Settlement camp, long time) {
        if (camp.visitor != null && time >= camp.visitor.leaveAt) removeVisitor(level, camp);
        if (camp.visitor == null && time >= camp.nextVisitor && camp.raid != Raid.ACTIVE && camp.raid != Raid.PREPARING && playersNearby(level, camp)) {
            camp.nextVisitor = time + 12000 + level.random.nextInt(12000);
            spawn(level, camp, camp.faction.equals("dark"), 10, VISITOR);
        }
    }
    static void removeVisitor(ServerLevel level, Settlement camp) {
        if (camp.visitor != null) { ChangedEntity mob = anywhere(level.getServer(), camp.visitor.id); if (mob != null) mob.discard(); }
        camp.visitor = null;
    }

    static CompoundTag snapshot(ServerPlayer player, Settlement camp) {
        ServerLevel level = player.serverLevel(); long time = now(level);
        CompoundTag tag = new CompoundTag(); tag.putUUID("Id", camp.id); tag.putBoolean("Owner", camp.owner.equals(player.getUUID()));
        tag.putBoolean("CanManage", camp.owner.equals(player.getUUID())
                && player.distanceToSqr(camp.core.getX() + 0.5, camp.core.getY() + 0.5, camp.core.getZ() + 0.5) <= 64);
        tag.putString("Faction", camp.faction); tag.putInt("Prosperity", camp.prosperity); tag.putInt("Reputation", camp.reputation);
        tag.putFloat("Threat", camp.threat); tag.putString("Raid", camp.raid.name());
        tag.putLong("RaidRemaining", Math.max(0, (camp.raid == Raid.PREPARING || camp.raid == Raid.ACTIVE ? camp.raidEnds : camp.nextRaid) - time));
        tag.putInt("Raiders", camp.raiders.size()); tag.putInt("Wave", camp.wave);
        ListTag residents = new ListTag();
        for (Resident r : camp.residents.values()) {
            CompoundTag view = r.save(); ChangedEntity mob = anywhere(player.server, r.id);
            view.putBoolean("Loaded", mob != null); view.putBoolean("InCamp", mob != null && mob.level() == level && mob.blockPosition().distSqr(camp.core) <= 32 * 32);
            view.putBoolean("OtherDimension", !r.dimension.isEmpty() && !r.dimension.equals(camp.dimension));
            view.putString("WorkState", r.workState.name());
            if (r.workTarget != null) view.putLong("WorkTarget", r.workTarget.asLong());
            view.remove("Expedition"); residents.add(view);
        }
        tag.put("Residents", residents);
        ListTag nearby = new ListTag();
        candidates(level, camp, player).stream().limit(32).forEach(mob -> { CompoundTag entry = new CompoundTag(); entry.putUUID("Id", mob.getUUID()); entry.putString("Name", mob.getName().getString()); entry.putString("DisplayName", Component.Serializer.toJson(mob.getDisplayName())); nearby.add(entry); });
        tag.put("Candidates", nearby);
        ListTag links = new ListTag();
        for (BlockPos pos : camp.storages) { CompoundTag entry = new CompoundTag(); entry.putLong("Pos", pos.asLong()); entry.putBoolean("Loaded", CampWarehouse.inventory(level, pos) != null); links.add(entry); }
        tag.put("Storages", links); tag.put("Stock", CampWarehouse.summary(level, camp));
        ListTag available = new ListTag();
        if (camp.owner.equals(player.getUUID())) for (BlockPos pos : CampWarehouse.nearby(level, camp)) {
            CompoundTag entry = new CompoundTag(); entry.putLong("Pos", pos.asLong()); available.add(entry);
        }
        tag.put("NearbyStorages", available);
        tag.putInt("Pending", camp.pending.stream().mapToInt(ItemStack::getCount).sum());
        tag.putInt("Food", CampWarehouse.inventories(level, camp).stream().mapToInt(handler -> {
            int count = 0; for (int i = 0; i < handler.getSlots(); i++) if (handler.getStackInSlot(i).isEdible()) count += handler.getStackInSlot(i).getCount(); return count;
        }).sum());
        if (camp.visitor != null) { tag.put("Visitor", camp.visitor.save()); tag.putLong("VisitorRemaining", Math.max(0, camp.visitor.leaveAt - time)); }
        return tag;
    }
    static void action(ServerPlayer player, Settlement camp, String action, UUID target, int index) {
        ServerLevel level = player.serverLevel();
        if (action.equals("trade")) { trade(player, camp, index); return; }
        if (!camp.owner.equals(player.getUUID()) || player.distanceToSqr(camp.core.getX() + 0.5, camp.core.getY() + 0.5, camp.core.getZ() + 0.5) > 64) return;
        Resident resident = target == null ? null : camp.residents.get(target);
        switch (action) {
            case "invite" -> { ChangedEntity mob = target == null ? null : entity(level, target); if (mob != null) enroll(level, camp, mob, player); }
            case "role" -> {
                if (resident != null && index >= 0 && index < CampRole.values().length) {
                    ChangedEntity mob = anywhere(player.server, resident.id);
                    if (mob != null) cancelWork(mob, camp);
                    resident.role = CampRole.values()[index]; resident.workEnabled = true;
                }
            }
            case "pause", "resume" -> {
                if (resident != null) {
                    resident.workEnabled = action.equals("resume");
                    ChangedEntity mob = anywhere(player.server, resident.id);
                    if (!resident.workEnabled && mob != null) cancelWork(mob, camp);
                    resident.workState = resident.workEnabled ? WorkState.IDLE : WorkState.PAUSED;
                }
            }
            case "release" -> {
                if (resident != null) {
                    cancelLegacy(level, camp, resident);
                    ChangedEntity mob = anywhere(player.server, resident.id);
                    if (mob != null) { thaw(mob); cancelWork(mob, camp); mob.getPersistentData().remove(MEMBER); mob.getNavigation().stop(); }
                    camp.residents.remove(resident.id);
                }
            }
            case "link" -> {
                if (target != null && target.getMostSignificantBits() == 0) {
                    BlockPos requested = BlockPos.of(target.getLeastSignificantBits());
                    if (!level.hasChunkAt(requested) || camp.core.distSqr(requested) > 32 * 32) return;
                    BlockPos pos = CampWarehouse.canonical(level, requested);
                    boolean claimed = LatexSettlementData.get(player.server).settlements.values().stream().filter(other -> other != camp && other.active && other.dimension.equals(camp.dimension))
                            .anyMatch(other -> other.storages.stream().anyMatch(p -> CampWarehouse.canonical(level, p).equals(pos)));
                    if (!claimed && camp.storages.size() < 4 && !camp.storages.contains(pos) && camp.core.distSqr(pos) <= 32 * 32 && CampWarehouse.inventory(level, pos) != null) camp.storages.add(pos);
                }
            }
            case "unlink" -> { if (index >= 0 && index < camp.storages.size()) camp.storages.remove(index); }
            case "collect" -> {
                for (Iterator<ItemStack> it = camp.pending.iterator(); it.hasNext();) { ItemStack stack = it.next(); player.getInventory().add(stack); if (stack.isEmpty()) it.remove(); }
                player.getInventory().setChanged();
            }
            case "faction" -> { if (camp.residents.isEmpty() && index >= 0 && index < HunterFaction.values().length) {
                camp.faction = HunterFaction.values()[index].id();
                if (level.getBlockEntity(camp.core) instanceof LatexCampfireBlockEntity core) core.bind(camp);
            } }
            default -> {}
        }
        LatexSettlementData.get(player.server).setDirty();
    }
    static void trade(ServerPlayer player, Settlement camp, int index) {
        if (camp.visitor == null || index < 0 || index >= camp.visitor.offers.size() || now(player.serverLevel()) >= camp.visitor.leaveAt) return;
        ChangedEntity mob = entity(player.serverLevel(), camp.visitor.id);
        if (mob == null || mob.blockPosition().distSqr(camp.core) > 32 * 32 || player.distanceToSqr(mob) > 64) return;
        MerchantOffer offer = camp.visitor.offers.get(index);
        if (offer.isOutOfStock()) return;
        // Simulate both costs against one temporary inventory, then mutate the real slots once.
        List<ItemStack> simulated = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) simulated.add(player.getInventory().getItem(i).copy());
        for (ItemStack cost : List.of(offer.getCostA(), offer.getCostB())) {
            int needed = cost.getCount();
            for (ItemStack stack : simulated) if (!cost.isEmpty() && ItemStack.isSameItemSameTags(stack, cost)) {
                int take = Math.min(needed, stack.getCount()); stack.shrink(take); needed -= take;
            }
            if (needed > 0) return;
        }
        for (int i = 0; i < simulated.size(); i++) {
            ItemStack actual = player.getInventory().getItem(i);
            actual.shrink(actual.getCount() - simulated.get(i).getCount());
        }
        ItemStack result = offer.getResult().copy(); player.getInventory().add(result); if (!result.isEmpty()) player.drop(result, false);
        offer.increaseUses(); camp.reputation++; player.getInventory().setChanged(); LatexSettlementData.get(player.server).setDirty();
    }
    public static void transfer(ChangedEntity previous, ChangedEntity replacement, ServerLevel level) {
        LatexSettlementData data = LatexSettlementData.get(level.getServer());
        for (String key : List.of(MEMBER, VISITOR, RAIDER, FROZEN)) if (previous.getPersistentData().contains(key))
            replacement.getPersistentData().put(key, previous.getPersistentData().get(key).copy());
        for (Settlement camp : data.settlements.values()) {
            Resident resident = camp.residents.remove(previous.getUUID());
            if (resident != null) { resident.id = replacement.getUUID(); camp.residents.put(resident.id, resident); ensureGoal(replacement); }
            if (camp.raiders.remove(previous.getUUID())) camp.raiders.add(replacement.getUUID());
            if (camp.visitor != null && camp.visitor.id.equals(previous.getUUID())) camp.visitor.id = replacement.getUUID();
        }
        if (replacement.getPersistentData().hasUUID(MEMBER) || replacement.getPersistentData().hasUUID(VISITOR) || replacement.getPersistentData().hasUUID(RAIDER)) {
            CreatureCommunityData.detach(replacement); ensureGoal(replacement);
            thaw(replacement);
        }
        CampWorkBuffer.transfer(previous, replacement);
        data.setDirty();
    }
    public static boolean isAreaClaimed(ServerLevel level, BlockPos pos, double radius) {
        String dimension = level.dimension().location().toString();
        return LatexSettlementData.get(level.getServer()).settlements.values().stream()
                .anyMatch(camp -> camp.active && camp.dimension.equals(dimension)
                        && Math.pow(camp.core.getX() - (double) pos.getX(), 2) + Math.pow(camp.core.getZ() - (double) pos.getZ(), 2) < radius * radius);
    }
    public static boolean prepareMaidWork(ChangedEntity mob, ServerPlayer player) {
        Settlement camp = member(mob);
        if (camp == null) { mob.getPersistentData().remove(MEMBER); return true; }
        if (camp.active || !camp.owner.equals(player.getUUID())) return false;
        Resident resident = camp.residents.get(mob.getUUID());
        cancelLegacy(player.serverLevel(), camp, resident); cancelWork(mob, camp);
        camp.residents.remove(mob.getUUID()); mob.getPersistentData().remove(MEMBER);
        LatexSettlementData.get(player.server).setDirty();
        return true;
    }
}
