package github.com.gengyoubo.CE.events;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

public class LatexSpaceSpawnEvents {
    private static final float DAYTIME_SPAWN_RATE_MULTIPLIER = 0.5F;

    private static final ResourceKey<Level> LATEX_SPACE = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("changede", "latex_space")
    );

    private static final Set<ResourceKey<Biome>> LAND_BIOMES = Set.of(
            biome("dark_latex_plains"),
            biome("dark_latex_forest"),
            biome("white_latex_plains"),
            biome("white_latex_forest")
    );

    private static final Set<ResourceLocation> DAYTIME_LATEX_SPAWNS = Set.of(
            changed("dark_latex_wolf_male"),
            changed("dark_latex_wolf_female"),
            changed("dark_latex_wolf_pup"),
            changed("dark_latex_yufeng"),
            changed("dark_latex_double_yufeng"),
            changed("white_latex_wolf_male"),
            changed("white_latex_wolf_female"),
            changed("pure_white_latex_wolf"),
            changed("pure_white_latex_wolf_pup"),
            changed("white_latex_knight"),
            changed("white_latex_centaur"),
            changed("milk_pudding")
    );

    public static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getDefaultResult()) {
            return;
        }

        if (!event.getLevel().getLevel().isDay()) {
            return;
        }

        if (event.getSpawnType() != MobSpawnType.NATURAL && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION) {
            return;
        }

        if (event.getLevel().getLevel().dimension() != LATEX_SPACE) {
            return;
        }

        if (!event.getLevel().getBiome(event.getPos()).is(LAND_BIOMES::contains)) {
            return;
        }

        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntityType());
        if (entityId == null || !DAYTIME_LATEX_SPAWNS.contains(entityId)) {
            return;
        }

        if (!hasSafeLatexLandSpawnSurface(event)) {
            return;
        }

        if (event.getRandom().nextFloat() >= DAYTIME_SPAWN_RATE_MULTIPLIER) {
            return;
        }

        event.setResult(Event.Result.ALLOW);
    }

    private static boolean hasSafeLatexLandSpawnSurface(MobSpawnEvent.SpawnPlacementCheck event) {
        BlockPos pos = event.getPos();
        BlockState below = event.getLevel().getBlockState(pos.below());
        BlockState feet = event.getLevel().getBlockState(pos);
        BlockState head = event.getLevel().getBlockState(pos.above());

        if (below.isAir() || below.is(BlockTags.LEAVES) || below.getFluidState().isSource()) {
            return false;
        }

        if (!below.isFaceSturdy(event.getLevel(), pos.below(), net.minecraft.core.Direction.UP)) {
            return false;
        }

        return feet.getFluidState().isEmpty() && head.getFluidState().isEmpty();
    }

    private static ResourceKey<Biome> biome(String path) {
        return ResourceKey.create(
                net.minecraft.core.registries.Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath("changede", path)
        );
    }

    private static ResourceLocation changed(String path) {
        return ResourceLocation.fromNamespaceAndPath("changed", path);
    }
}
