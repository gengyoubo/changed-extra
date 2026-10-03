package github.com.gengyoubo.CE.compat.synergy.camp;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.parkabird.changedsynergy.ai.CreatureSettlementService;

import java.util.*;

final class CampWorkSites {
    static final TagKey<Block> FARMLAND = TagKey.create(Registries.BLOCK, ResourceLocation.parse("changede:camp_farmland"));
    static final TagKey<Block> CRAFTING = TagKey.create(Registries.BLOCK, ResourceLocation.parse("changede:camp_crafting_workstations"));
    private record Key(String dimension, BlockPos pos) {}
    private record Claim(UUID worker, long until) {}
    private record Sites(ServerLevel level, long scanned, List<BlockPos> crops, List<BlockPos> kitchens,
                         List<CreatureSettlementService.FishingSite> fishing) {}
    private static final Map<UUID, Sites> CACHE = new HashMap<>();
    private static final Map<Key, Claim> CLAIMS = new HashMap<>();
    static void clear() { CACHE.clear(); CLAIMS.clear(); }
    static boolean inside(LatexSettlementData.Settlement camp, BlockPos pos) { return camp.core.distSqr(pos) <= 32 * 32; }
    static Sites sites(ServerLevel level, LatexSettlementData.Settlement camp) {
        long time = level.getGameTime(); Sites cache = CACHE.get(camp.id);
        if (cache != null && cache.level == level && time >= cache.scanned && time - cache.scanned < 300) return cache;
        List<BlockPos> crops = new ArrayList<>(), kitchens = new ArrayList<>();
        List<CreatureSettlementService.FishingSite> fishing = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(camp.core.offset(-32, -6, -32), camp.core.offset(32, 6, 32))) {
            if (!inside(camp, pos) || !level.hasChunkAt(pos)) continue;
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock && level.getBlockState(pos.below()).is(FARMLAND)) crops.add(pos.immutable());
            if (state.is(CRAFTING) || state.is(Blocks.FURNACE) || state.is(Blocks.SMOKER)) kitchens.add(pos.immutable());
            if (level.getFluidState(pos).isEmpty() && level.getFluidState(pos.above()).isEmpty()
                    && state.getCollisionShape(level, pos).isEmpty()
                    && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                    && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                for (Direction direction : Direction.Plane.HORIZONTAL) for (int drop = 0; drop <= 1; drop++) {
                    BlockPos water = pos.relative(direction).below(drop);
                    if (inside(camp, water) && level.hasChunkAt(water) && level.getBlockState(water).is(Blocks.WATER)
                            && level.getFluidState(water.above()).isEmpty()) {
                        fishing.add(new CreatureSettlementService.FishingSite(pos.immutable(), water.immutable())); break;
                    }
                }
            }
        }
        cache = new Sites(level, time, crops, kitchens, fishing); CACHE.put(camp.id, cache); return cache;
    }
    static List<BlockPos> crops(ServerLevel level, LatexSettlementData.Settlement camp) { return sites(level, camp).crops; }
    static List<BlockPos> kitchens(ServerLevel level, LatexSettlementData.Settlement camp) { return sites(level, camp).kitchens; }
    static CreatureSettlementService.FishingSite fishing(ChangedEntity mob, LatexSettlementData.Settlement camp) {
        ServerLevel level = (ServerLevel) mob.level();
        var preferred = CreatureSettlementService.findCompanionFishingSite(mob, 32, 6)
                .filter(site -> validFishing(level, camp, mob, site)).orElse(null);
        if (preferred != null) return preferred;
        // CS returns only its nearest site. Search other shore positions when that one is claimed/outside the camp.
        for (var site : sites(level, camp).fishing.stream()
                .sorted(Comparator.comparingDouble(candidate -> candidate.stand().distSqr(mob.blockPosition()))).toList()) {
            if (!validFishing(level, camp, mob, site)) continue;
            Vec3 destination = CreatureSettlementService.fishingApproach(mob, site);
            if (!level.noCollision(mob, mob.getBoundingBox().move(destination.subtract(mob.position())))) continue;
            var path = mob.getNavigation().createPath(BlockPos.containing(destination), 0);
            if (mob.distanceToSqr(destination) <= 4 || path != null && path.canReach()) return site;
        }
        return null;
    }
    private static boolean validFishing(ServerLevel level, LatexSettlementData.Settlement camp, ChangedEntity mob,
                                        CreatureSettlementService.FishingSite site) {
        return inside(camp, site.water()) && inside(camp, site.stand()) && level.hasChunkAt(site.water())
                && level.hasChunkAt(site.stand()) && level.getBlockState(site.water()).is(Blocks.WATER)
                && level.getFluidState(site.water().above()).isEmpty() && available(level, site.water(), mob.getUUID())
                && level.getFluidState(site.stand()).isEmpty() && level.getFluidState(site.stand().above()).isEmpty()
                && level.getBlockState(site.stand()).getCollisionShape(level, site.stand()).isEmpty()
                && level.getBlockState(site.stand().above()).getCollisionShape(level, site.stand().above()).isEmpty()
                && level.getBlockState(site.stand().below()).isFaceSturdy(level, site.stand().below(), Direction.UP);
    }
    static boolean claim(ServerLevel level, BlockPos pos, UUID worker) {
        Key key = new Key(level.dimension().location().toString(), pos.immutable()); Claim old = CLAIMS.get(key);
        if (old != null && old.until > level.getGameTime() && !old.worker.equals(worker)) return false;
        CLAIMS.put(key, new Claim(worker, level.getGameTime() + 1200)); return true;
    }
    static boolean available(ServerLevel level, BlockPos pos, UUID worker) {
        Claim claim = CLAIMS.get(new Key(level.dimension().location().toString(), pos));
        return claim == null || claim.until <= level.getGameTime() || claim.worker.equals(worker);
    }
    static void release(UUID worker) { CLAIMS.values().removeIf(claim -> claim.worker.equals(worker)); }
    static Vec3 approach(ChangedEntity mob, BlockPos target) {
        ServerLevel level = (ServerLevel) mob.level();
        for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) for (int y : new int[]{0, -1, 1}) {
            BlockPos stand = target.relative(direction).offset(0, y, 0);
            if (!level.hasChunkAt(stand) || !level.getBlockState(stand).getCollisionShape(level, stand).isEmpty()
                    || level.getBlockState(stand.below()).getCollisionShape(level, stand.below()).isEmpty()) continue;
            Vec3 dest = Vec3.atBottomCenterOf(stand);
            if (!level.noCollision(mob, mob.getBoundingBox().move(dest.subtract(mob.position())))) continue;
            if (mob.distanceToSqr(dest) <= 2.25) return dest;
            var path = mob.getNavigation().createPath(stand, 0);
            if (path != null && path.canReach()) return dest;
        }
        return null;
    }
}
