package github.com.gengyoubo.CE.compat.twilight;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import twilightforest.init.TFBlocks;
import twilightforest.init.TFStructures;
import twilightforest.util.LandmarkUtil;
import twilightforest.world.components.structures.finalcastle.FinalCastleBossGazeboComponent;

import java.util.HashSet;
import java.util.Set;

/** Upgrade only untouched, positively identified WIP arenas in existing saves. */
public final class TwilightCastleUpgrade extends SavedData {
    private final Set<Long> upgraded = new HashSet<>();
    private static final Set<String> WIP_TEXT = Set.of("Final Castle WIP.", "Join our Discord server to",
            "follow development of the mod:", "discord.experiment115.com");
    private static TwilightCastleUpgrade load(CompoundTag tag) {
        var data = new TwilightCastleUpgrade();
        for (long pos : tag.getLongArray("arenas")) data.upgraded.add(pos);
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("arenas", upgraded.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.tickCount % 40 != 0
                || !(event.player.level() instanceof ServerLevel level)
                || !level.dimension().location().equals(net.minecraft.resources.ResourceLocation.parse("twilightforest:twilight_forest"))) return;
        var nearest = twilightforest.util.LegacyLandmarkPlacements.getNearestCenterXZ(event.player.getBlockX() >> 4, event.player.getBlockZ() >> 4);
        if (!level.hasChunk(nearest.getX() >> 4, nearest.getZ() >> 4)) return;
        var start = LandmarkUtil.locateNearestLandmarkStart(level, TFStructures.FINAL_CASTLE, event.player.blockPosition());
        if (start.isEmpty() || start.get() instanceof twilightforest.world.components.structures.start.TFStructureStart castle && castle.isConquered()) return;
        var data = level.getDataStorage().computeIfAbsent(TwilightCastleUpgrade::load, TwilightCastleUpgrade::new, "changede_final_castle_upgrades");
        for (var piece : start.get().getPieces()) {
            if (!(piece instanceof FinalCastleBossGazeboComponent)) continue;
            var bounds = piece.getBoundingBox();
            BlockPos center = new BlockPos((bounds.minX() + bounds.maxX()) / 2, bounds.minY(), (bounds.minZ() + bounds.maxZ()) / 2);
            if (data.upgraded.contains(center.asLong()) || event.player.distanceToSqr(center.getX() + .5, center.getY(), center.getZ() + .5) > 32 * 32) continue;
            if (!level.hasChunkAt(center.offset(-22, 0, -22)) || !level.hasChunkAt(center.offset(22, 0, 22))) continue;
            var texts = level.getEntitiesOfClass(ArmorStand.class, new AABB(center).inflate(3),
                    stand -> stand.hasCustomName() && WIP_TEXT.contains(stand.getCustomName().getString()));
            if (texts.stream().noneMatch(stand -> stand.getCustomName().getString().equals("Final Castle WIP."))) continue;
            // No edits to rebuilt/player-owned arenas: require the original force-field enclosure.
            if (!level.getBlockState(new BlockPos(center.getX(), center.getY(), bounds.minZ())).is(TFBlocks.VIOLET_FORCE_FIELD.get())
                    || !level.getBlockState(center).isAir()) continue;
            // Replace the old WIP enclosure with the same 45x45x25 arena used by newly generated castles.
            for (BlockPos pos : BlockPos.betweenClosed(bounds.minX(), center.getY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()))
                if (level.getBlockState(pos).is(TFBlocks.VIOLET_FORCE_FIELD.get())) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            for (int x = -22; x <= 22; x++) for (int z = -22; z <= 22; z++) {
                placeArenaBlock(level, center.offset(x, -1, z), TFBlocks.VIOLET_CASTLE_RUNE_BRICK.get().defaultBlockState());
                for (int y = 0; y < 25; y++) {
                    if (Math.abs(x) != 22 && Math.abs(z) != 22 && y != 24) continue;
                    if (Math.abs(x) <= 1 && Math.abs(z) == 22 && y <= 3) continue;
                    placeArenaBlock(level, center.offset(x, y, z), TFBlocks.VIOLET_FORCE_FIELD.get().defaultBlockState());
                }
            }
            level.setBlockAndUpdate(center, TFBlocks.FINAL_BOSS_BOSS_SPAWNER.get().defaultBlockState());
            texts.forEach(ArmorStand::discard);
            data.upgraded.add(center.asLong()); data.setDirty();
        }
    }
    private static void placeArenaBlock(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        var existing = level.getBlockState(pos);
        if (level.getBlockEntity(pos) == null && (existing.isAir() || existing.canBeReplaced()
                || existing.is(TFBlocks.VIOLET_CASTLE_RUNE_BRICK.get()) || existing.is(TFBlocks.VIOLET_FORCE_FIELD.get())))
            level.setBlockAndUpdate(pos, state);
    }
}
