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
        var start = LandmarkUtil.locateNearestLandmarkStart(level, TFStructures.FINAL_CASTLE, event.player.blockPosition());
        if (start.isEmpty()) return;
        var data = level.getDataStorage().computeIfAbsent(TwilightCastleUpgrade::load, TwilightCastleUpgrade::new, "changede_final_castle_upgrades");
        for (var piece : start.get().getPieces()) {
            if (!(piece instanceof FinalCastleBossGazeboComponent)) continue;
            var bounds = piece.getBoundingBox();
            BlockPos center = new BlockPos((bounds.minX() + bounds.maxX()) / 2, bounds.minY(), (bounds.minZ() + bounds.maxZ()) / 2);
            if (data.upgraded.contains(center.asLong()) || event.player.distanceToSqr(center.getX() + .5, center.getY(), center.getZ() + .5) > 32 * 32) continue;
            if (!level.hasChunkAt(new BlockPos(bounds.minX(), center.getY(), bounds.minZ()))
                    || !level.hasChunkAt(new BlockPos(bounds.maxX(), center.getY(), bounds.maxZ()))) continue;
            var texts = level.getEntitiesOfClass(ArmorStand.class, new AABB(center).inflate(3),
                    stand -> stand.hasCustomName() && WIP_TEXT.contains(stand.getCustomName().getString()));
            if (texts.stream().noneMatch(stand -> stand.getCustomName().getString().equals("Final Castle WIP."))) continue;
            // No edits to rebuilt/player-owned arenas: require the original force-field enclosure.
            if (!level.getBlockState(new BlockPos(center.getX(), center.getY(), bounds.minZ())).is(TFBlocks.VIOLET_FORCE_FIELD.get())
                    || !level.getBlockState(center).isAir()) continue;
            for (int z : new int[]{bounds.minZ(), bounds.maxZ()}) {
                for (BlockPos pos : BlockPos.betweenClosed(center.getX() - 1, center.getY(), z, center.getX() + 1, center.getY() + 3, z)) {
                    if (level.getBlockState(pos).is(TFBlocks.VIOLET_FORCE_FIELD.get())) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
            }
            level.setBlockAndUpdate(center, TFBlocks.FINAL_BOSS_BOSS_SPAWNER.get().defaultBlockState());
            texts.forEach(ArmorStand::discard);
            data.upgraded.add(center.asLong()); data.setDirty();
        }
    }
}
