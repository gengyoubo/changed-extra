package github.com.gengyoubo.CE.compat.twilight;

import net.foxyas.changedaddon.entity.bosses.Experiment009BossEntity;
import net.foxyas.changedaddon.init.ChangedAddonEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayerFactory;
import twilightforest.TFConfig;
import twilightforest.block.entity.spawner.BossSpawnerBlockEntity;
import twilightforest.block.entity.spawner.FinalBossSpawnerBlockEntity;
import twilightforest.init.TFBlocks;
import twilightforest.init.TFStructures;
import twilightforest.util.LandmarkUtil;
import twilightforest.world.components.structures.finalcastle.FinalCastleBossGazeboComponent;
import twilightforest.world.components.structures.finalcastle.FinalCastleMainComponent;
import twilightforest.world.components.structures.start.TFStructureStart;

import java.util.List;
import java.util.function.BiConsumer;

/** Real arena generation, native spawner lifecycle, conquest and reward checks. */
public final class TwilightFinalBossChecks {
    private TwilightFinalBossChecks() {}
    public static void prepare(MinecraftServer server) {
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("twilightforest:twilight_forest")));
        for (int x = -2; x <= 1; x++) for (int z = -2; z <= 1; z++) { level.setChunkForced(x, z, true); level.getChunk(x, z); }
    }
    public static void run(MinecraftServer server, BiConsumer<Boolean, String> check) {
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("twilightforest:twilight_forest")));
        check.accept(level != null, "Twilight dimension loads with optional compatibility");
        var main = new FinalCastleMainComponent(0, 0, 0, 0);
        var arena = new FinalCastleBossGazeboComponent(1, main, 0, 0, 0);
        var bounds = arena.getBoundingBox();
        for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++)
            for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) { level.setChunkForced(x, z, true); level.getChunk(x, z); }
        level.getChunkSource().tick(() -> true, false);
        for (String name : new String[]{"iridium", "painite", "dark_morphic", "white_morphic"}) {
            BlockPos orePos = new BlockPos(6, 16, 6);
            for (BlockPos pos : BlockPos.betweenClosed(orePos.offset(-6, -6, -6), orePos.offset(6, 6, 6)))
                level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            var feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .get(ResourceLocation.parse("changede:twilight_" + name + "_ore"));
            boolean placed = false;
            for (int i = 0; i < 20; i++) placed |= feature.place(level, level.getChunkSource().getGenerator(), level.random, orePos);
            check.accept(placed, "Native ore generation places " + name + " in Twilight stone");
            check.accept(!feature.place(server.overworld(), server.overworld().getChunkSource().getGenerator(), level.random, orePos),
                    "Twilight ore feature rejects the Overworld: " + name);
        }
        level.getEntitiesOfClass(Experiment009BossEntity.class, new AABB(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX() + 1, bounds.maxY() + 1, bounds.maxZ() + 1),
                entity -> entity.getPersistentData().getBoolean(TwilightFinalBossCompat.MARKER)).forEach(Entity::discard);
        arena.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random,
                BoundingBox.infinite(), new ChunkPos(BlockPos.ZERO), BlockPos.ZERO);
        BlockPos home = new BlockPos((bounds.minX() + bounds.maxX()) / 2, bounds.minY(), (bounds.minZ() + bounds.maxZ()) / 2);
        check.accept(level.getBlockState(home).is(TFBlocks.FINAL_BOSS_BOSS_SPAWNER.get()), "Actual final-castle structure generates the final boss spawner");
        check.accept(level.getBlockState(home.below()).is(TFBlocks.VIOLET_CASTLE_RUNE_BRICK.get()), "Generated arena has a solid floor");
        check.accept(level.getBlockState(new BlockPos(home.getX(), home.getY() + 1, bounds.minZ())).isAir()
                && level.getBlockState(new BlockPos(home.getX(), home.getY() + 1, bounds.maxZ())).isAir(), "Both WIP force-field entrances are opened");
        var structure = LandmarkUtil.structureForKey(level, TFStructures.FINAL_CASTLE);
        var start = new TFStructureStart(structure, new ChunkPos(home), 0, new PiecesContainer(List.of(arena)));
        var center = twilightforest.util.LegacyLandmarkPlacements.getNearestCenterXZ(home.getX() >> 4, home.getZ() >> 4);
        var startChunk = level.getChunkAt(center);
        startChunk.setStartForStructure(structure, start);
        startChunk.addReferenceForStructure(structure, startChunk.getPos().toLong());
        check.accept(LandmarkUtil.locateNearestLandmarkStart(level, TFStructures.FINAL_CASTLE, home).orElse(null) == start,
                "Conquest fixture resolves through native landmark references");
        var player = FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.fromString("c7ae3aab-1d38-4e67-971d-003800000009"), "CEIssue38Twilight"));
        player.setPos(home.getX() + 1, home.getY(), home.getZ()); player.tickCount = 40;
        level.setBlockAndUpdate(home, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(home.getX(), home.getY(), bounds.minZ()), TFBlocks.VIOLET_FORCE_FIELD.get().defaultBlockState());
        var text = new net.minecraft.world.entity.decoration.ArmorStand(level, home.getX() + .5, home.getY() + 2.3, home.getZ() + .5);
        text.setCustomName(net.minecraft.network.chat.Component.literal("Final Castle WIP.")); level.addFreshEntity(text);
        TwilightCastleUpgrade.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END, player));
        check.accept(level.getBlockState(home).is(TFBlocks.FINAL_BOSS_BOSS_SPAWNER.get()) && text.isRemoved(),
                "Existing untouched WIP arena upgrades on player approach and removes its placeholder (home=" + home
                        + ", player=" + player.blockPosition() + ", level=" + player.level().dimension().location()
                        + ", state=" + level.getBlockState(home) + ", textRemoved=" + text.isRemoved()
                        + ", visible=" + level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class, new AABB(home).inflate(3)).size() + ")");
        var state = level.getBlockState(home);
        var spawner = new FinalBossSpawnerBlockEntity(home, state) { @Override public boolean anyPlayerInRange() { return true; } };
        spawner.setLevel(level);
        server.setDifficulty(Difficulty.PEACEFUL, true);
        BossSpawnerBlockEntity.tick(level, home, state, spawner);
        check.accept(level.getBlockState(home).is(TFBlocks.FINAL_BOSS_BOSS_SPAWNER.get()), "Peaceful mode retains final boss spawner");
        server.setDifficulty(Difficulty.NORMAL, true);
        BossSpawnerBlockEntity.tick(level, home, state, spawner);
        level.tick(() -> true);
        var bosses = level.getEntitiesOfClass(Experiment009BossEntity.class, new AABB(home).inflate(8),
                boss -> boss.getPersistentData().getBoolean(TwilightFinalBossCompat.MARKER));
        check.accept(bosses.size() == 1 && level.getBlockState(home).isAir(), "Native final spawner creates exactly one Experiment 009 and consumes itself");
        var boss = bosses.get(0);
        boss.setNoAi(true);
        check.accept(boss.getMaxHealth() == 2800 && boss.getHealth() == 2800, "Final encounter has exactly 1400 hearts");
        var ordinary = ChangedAddonEntities.EXPERIMENT_009_BOSS.get().create(level);
        check.accept(ordinary.getMaxHealth() == 425, "Ordinary Experiment 009 boss retains Addon's health");
        boss.setHealth(2600);
        var saved = new CompoundTag(); boss.save(saved);
        var loaded = ChangedAddonEntities.EXPERIMENT_009_BOSS.get().create(level); loaded.load(saved);
        check.accept(loaded.getHealth() == 2600 && loaded.getMaxHealth() == 2800 && loaded.getPersistentData().getLong(TwilightFinalBossCompat.HOME) == home.asLong(),
                "Encounter health and origin survive entity NBT reload without healing");
        boolean config = TFConfig.COMMON_CONFIG.bossDropChests.get();
        try {
            TFConfig.COMMON_CONFIG.bossDropChests.set(true);
            stages(level, boss, home, player, check);
            boss.setLastHurtByPlayer(player); boss.setHealth(0); boss.die(level.damageSources().playerAttack(player));
            check.accept(level.getBlockEntity(home.below()) == null, "Chest waits until the native death animation ends");
            check.accept(boss.getPersistentData().contains("changede_final_rewards"), "Extra loot is buffered in entity NBT at death");
            check.accept(start.isConquered(), "Final castle is marked conquered through Twilight's API");
            for (int tick = 0; tick < 200 && !boss.isRemoved(); tick++) boss.tick();
            check.accept(boss.isRemoved() && boss.getRemovalReason() == Entity.RemovalReason.KILLED, "Native death animation removes the boss as KILLED");
            check.accept(level.getBlockState(home.below()).is(TFBlocks.DARK_CHEST.get()) && level.getBlockEntity(home.below()) instanceof Container,
                    "After removal, dark-wood reward chest appears at home.below()");
            var chest = (Container) level.getBlockEntity(home.below());
            int before = count(chest); boolean twilight = false, changed = false;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(chest.getItem(i).getItem());
                twilight |= id != null && id.getNamespace().equals("twilightforest");
                changed |= id != null && (id.getNamespace().equals("changed") || id.getNamespace().equals("changed_addon"));
            }
            check.accept(before > 0 && twilight && changed, "Final chest includes both Twilight and Changed-series rewards");
            TwilightFinalBossRewards.deposit(level, boss);
            check.accept(count(chest) == before, "Repeated removal/deposit cannot duplicate rewards");
            check.accept(level.getEntitiesOfClass(ItemEntity.class, new AABB(home).inflate(20)).stream().anyMatch(item -> {
                var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item.getItem().getItem());
                return id != null && (id.getNamespace().equals("changed") || id.getNamespace().equals("changed_addon"));
            }), "Experiment 009 retains its native Changed-series ground drops");
            for (boolean enabled : new boolean[]{false, true}) {
                BlockPos extraHome = home.offset(enabled ? 40 : 32, 0, 0); level.getChunkAt(extraHome);
                level.setBlockAndUpdate(extraHome.below(), Blocks.CHEST.defaultBlockState());
                var occupied = (Container) level.getBlockEntity(extraHome.below()); occupied.setItem(0, new ItemStack(Items.DIAMOND, 7));
                var extra = ChangedAddonEntities.EXPERIMENT_009_BOSS.get().create(level);
                extra.getPersistentData().putBoolean(TwilightFinalBossCompat.MARKER, true);
                extra.getPersistentData().putLong(TwilightFinalBossCompat.HOME, extraHome.asLong());
                TFConfig.COMMON_CONFIG.bossDropChests.set(enabled);
                TwilightFinalBossRewards.prepare(new net.minecraftforge.event.entity.living.LivingDeathEvent(extra, level.damageSources().genericKill()));
                TwilightFinalBossRewards.deposit(level, extra);
                check.accept(occupied.getItem(0).getCount() == 7, "Reward placement preserves an occupied chest");
                if (!enabled) check.accept(!level.getEntitiesOfClass(ItemEntity.class, new AABB(extraHome).inflate(3)).isEmpty(),
                        "bossDropChests=false drops extra loot on the ground");
            }
        } finally { TFConfig.COMMON_CONFIG.bossDropChests.set(config); }
    }
    private static void stages(ServerLevel level, Experiment009BossEntity boss, BlockPos home,
                               net.minecraft.server.level.ServerPlayer player, BiConsumer<Boolean, String> check) {
        int[] counts = {1, 1, 7, 2, 1, 1};
        String[][] types = {{"naga"}, {"lich"}, {"minoshroom", "knight_phantom"}, {"alpha_yeti", "snow_queen"}, {"hydra"}, {"ur_ghast"}};
        for (int waveIndex = 0; waveIndex < 6; waveIndex++) {
            check.accept(TwilightFinalEncounter.stage(boss) == waveIndex && boss.getPhase() == TwilightFinalEncounter.phase(boss),
                    "Addon phase follows the 2/2/3 stage distribution: stage " + (waveIndex + 1));
            boss.setHealth(-99999); // A single huge hit must not skip any segment or the guardian fight.
            float expected = 2800 - (waveIndex + 1) * 400;
            check.accept(boss.getHealth() == expected && TwilightFinalEncounter.locked(boss), "Oversized hit stops exactly at the 200-heart checkpoint");
            var wave = boss.getPersistentData().getList(TwilightFinalEncounter.WAVE, net.minecraft.nbt.Tag.TAG_COMPOUND);
            check.accept(wave.size() == counts[waveIndex], "Correct native guardian group size for wave " + (waveIndex + 1));
            for (String type : types[waveIndex]) {
                boolean matched = false;
                for (int i = 0; i < wave.size(); i++) matched |= wave.getCompound(i).getString("type").equals("twilightforest:" + type);
                check.accept(matched, "Guardian group includes " + type);
            }
            boss.setHealth(1); boss.heal(100);
            check.accept(boss.getHealth() == expected, "Damage and healing cannot move the locked checkpoint");
            boss.die(level.damageSources().genericKill());
            check.accept(!boss.getPersistentData().contains("changede_final_rewards"), "Locked stage cannot award final rewards");
            var saved = new CompoundTag(); boss.save(saved);
            var reload = ChangedAddonEntities.EXPERIMENT_009_BOSS.get().create(level); reload.load(saved);
            check.accept(reload.getPersistentData().getList(TwilightFinalEncounter.WAVE, net.minecraft.nbt.Tag.TAG_COMPOUND).size() == wave.size()
                    && reload.getHealth() == expected, "Locked wave and checkpoint survive NBT reload");
            for (int i = 0; i < wave.size(); i++) {
                var entry = wave.getCompound(i);
                var guardian = (net.minecraft.world.entity.Mob) level.getEntity(entry.getUUID("id"));
                check.accept(guardian != null && guardian.getPersistentData().getBoolean(TwilightFinalEncounter.GUARDIAN), "Summoned native boss exists and is scoped to the encounter");
                guardian.setLastHurtByPlayer(player); guardian.setHealth(0); guardian.die(level.damageSources().playerAttack(player));
                guardian.remove(Entity.RemovalReason.KILLED);
                TwilightFinalEncounter.tick(boss);
                check.accept(i == wave.size() - 1 ? !TwilightFinalEncounter.locked(boss) : TwilightFinalEncounter.locked(boss),
                        "009 unlocks only after every guardian in the group dies");
                check.accept(level.getBlockEntity(home.below()) == null, "Guardian death skips Twilight's native reward chest");
            }
            check.accept(TwilightFinalEncounter.stage(boss) == waveIndex + 1 && boss.getHealth() == expected,
                    "Next 200-heart segment resumes without healing");
        }
        check.accept(boss.getPhase() == Experiment009BossEntity.Exp9Phase.PHASE3, "Final three segments use Addon's third phase and its skill multipliers");
    }
    private static int count(Container container) {
        int total = 0;
        for (int i = 0; i < container.getContainerSize(); i++) total += container.getItem(i).getCount();
        return total;
    }
}
