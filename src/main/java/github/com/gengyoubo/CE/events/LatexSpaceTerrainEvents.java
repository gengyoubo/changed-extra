package github.com.gengyoubo.CE.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LatexSpaceTerrainEvents {
    private static final ThreadLocal<Boolean> REPLACING_LATEX_SPACE_FLUID = ThreadLocal.withInitial(() -> false);

    private static final ResourceKey<Level> LATEX_SPACE = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("changede", "latex_space")
    );
    private static final Set<Long> PROCESSED_CHUNKS = ConcurrentHashMap.newKeySet();

    private static final Set<ResourceKey<Biome>> DARK_BIOMES = Set.of(
            biome("dark_latex_plains"),
            biome("dark_latex_forest"),
            biome("dark_latex_sea")
    );
    private static final Set<ResourceKey<Biome>> WHITE_BIOMES = Set.of(
            biome("white_latex_plains"),
            biome("white_latex_forest"),
            biome("white_latex_sea")
    );

    private static final Set<Block> LATEX_SURFACE_REPLACEABLE = Set.of(
            Blocks.GRASS_BLOCK,
            Blocks.DIRT,
            Blocks.COARSE_DIRT,
            Blocks.ROOTED_DIRT,
            Blocks.PODZOL,
            Blocks.MYCELIUM,
            Blocks.MUD,
            Blocks.CLAY,
            Blocks.SAND,
            Blocks.RED_SAND,
            Blocks.GRAVEL
    );
    private static final Set<Block> LATEX_STONE_REPLACEABLE = Set.of(
            Blocks.STONE,
            Blocks.DEEPSLATE,
            Blocks.GRANITE,
            Blocks.DIORITE,
            Blocks.ANDESITE,
            Blocks.TUFF,
            Blocks.CALCITE
    );
    private static final Set<Block> LATEX_COBBLE_REPLACEABLE = Set.of(
            Blocks.COBBLESTONE,
            Blocks.MOSSY_COBBLESTONE,
            Blocks.COBBLED_DEEPSLATE
    );

    private static final ResourceLocation DARK_LATEX_BLOCK = ResourceLocation.fromNamespaceAndPath("changed", "dark_latex_block");
    private static final ResourceLocation WHITE_LATEX_BLOCK = ResourceLocation.fromNamespaceAndPath("changed", "white_latex_block");
    private static final ResourceLocation DARK_LATEX_FLUID_BLOCK = ResourceLocation.fromNamespaceAndPath("changed", "dark_latex_fluid");
    private static final ResourceLocation WHITE_LATEX_FLUID_BLOCK = ResourceLocation.fromNamespaceAndPath("changed", "white_latex_fluid");
    private static final ResourceLocation DARK_LATEX_STONE = ResourceLocation.fromNamespaceAndPath("changede", "dark_latex_stone");
    private static final ResourceLocation WHITE_LATEX_STONE = ResourceLocation.fromNamespaceAndPath("changede", "white_latex_stone");
    private static final ResourceLocation DARK_LATEX_COBBLESTONE = ResourceLocation.fromNamespaceAndPath("changede", "dark_latex_cobblestone");
    private static final ResourceLocation WHITE_LATEX_COBBLESTONE = ResourceLocation.fromNamespaceAndPath("changede", "white_latex_cobblestone");

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(LATEX_SPACE)) {
            return;
        }

        ChunkAccess chunk = event.getChunk();
        if (chunk.getStatus().isOrAfter(ChunkStatus.FULL) && PROCESSED_CHUNKS.add(chunk.getPos().toLong())) {
            replaceLatexSpaceTerrain(level, chunk);
        }
    }

    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(LATEX_SPACE)) {
            PROCESSED_CHUNKS.clear();
        }
    }

    public static boolean shouldSkipLatexSpaceFluidOnPlace() {
        return Boolean.TRUE.equals(REPLACING_LATEX_SPACE_FLUID.get());
    }

    private static void replaceLatexSpaceTerrain(ServerLevel level, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();
        boolean changed = false;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int x = chunkPos.getMinBlockX() + localX;
                int z = chunkPos.getMinBlockZ() + localZ;
                int maxY = Math.max(minY, chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, localX, localZ));
                LatexPalette palette = paletteFor(level, pos.set(x, Math.max(minY, maxY - 1), z));

                if (palette == null) {
                    continue;
                }

                for (int y = minY; y <= maxY; y++) {
                    pos.set(x, y, z);
                    BlockState current = chunk.getBlockState(pos);
                    BlockState replacement = palette.replacementFor(current);
                    if (replacement == null || current.equals(replacement)) {
                        continue;
                    }

                    if (current.is(Blocks.WATER)) {
                        setLatexSpaceFluidBlock(chunk, pos, replacement);
                    } else {
                        chunk.setBlockState(pos, replacement, false);
                    }
                    changed = true;
                }
            }
        }

        if (changed) {
            chunk.setUnsaved(true);
        }
    }

    private static void setLatexSpaceFluidBlock(ChunkAccess chunk, BlockPos pos, BlockState fluid) {
        boolean wasReplacing = shouldSkipLatexSpaceFluidOnPlace();
        REPLACING_LATEX_SPACE_FLUID.set(true);
        try {
            chunk.setBlockState(pos, fluid, false);
        } finally {
            REPLACING_LATEX_SPACE_FLUID.set(wasReplacing);
        }
    }

    private static LatexPalette paletteFor(ServerLevel level, BlockPos pos) {
        if (level.getBiome(pos).is(DARK_BIOMES::contains)) {
            return LatexPalette.DARK;
        }
        if (level.getBiome(pos).is(WHITE_BIOMES::contains)) {
            return LatexPalette.WHITE;
        }
        return null;
    }

    private static ResourceKey<Biome> biome(String path) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("changede", path));
    }

    private static BlockState blockState(ResourceLocation id, Block fallback) {
        Block block = ForgeRegistries.BLOCKS.getValue(id);
        return (block == null ? fallback : block).defaultBlockState();
    }

    private enum LatexPalette {
        DARK(
                DARK_LATEX_BLOCK,
                Blocks.BLACK_CONCRETE,
                DARK_LATEX_STONE,
                Blocks.DEEPSLATE,
                DARK_LATEX_COBBLESTONE,
                Blocks.COBBLED_DEEPSLATE,
                DARK_LATEX_FLUID_BLOCK
        ),
        WHITE(
                WHITE_LATEX_BLOCK,
                Blocks.WHITE_CONCRETE,
                WHITE_LATEX_STONE,
                Blocks.STONE,
                WHITE_LATEX_COBBLESTONE,
                Blocks.COBBLESTONE,
                WHITE_LATEX_FLUID_BLOCK
        );

        private final ResourceLocation surfaceId;
        private final Block surfaceFallback;
        private final ResourceLocation stoneId;
        private final Block stoneFallback;
        private final ResourceLocation cobblestoneId;
        private final Block cobblestoneFallback;
        private final ResourceLocation fluidId;
        private BlockState surface;
        private BlockState stone;
        private BlockState cobblestone;
        private BlockState fluid;

        LatexPalette(ResourceLocation surfaceId, Block surfaceFallback,
                     ResourceLocation stoneId, Block stoneFallback,
                     ResourceLocation cobblestoneId, Block cobblestoneFallback,
                     ResourceLocation fluidId) {
            this.surfaceId = surfaceId;
            this.surfaceFallback = surfaceFallback;
            this.stoneId = stoneId;
            this.stoneFallback = stoneFallback;
            this.cobblestoneId = cobblestoneId;
            this.cobblestoneFallback = cobblestoneFallback;
            this.fluidId = fluidId;
        }

        private BlockState replacementFor(BlockState current) {
            resolveStates();
            Block block = current.getBlock();
            if (LATEX_SURFACE_REPLACEABLE.contains(block)) {
                return surface;
            }
            if (LATEX_STONE_REPLACEABLE.contains(block)) {
                return stone;
            }
            if (LATEX_COBBLE_REPLACEABLE.contains(block)) {
                return cobblestone;
            }
            if (current.is(Blocks.WATER)) {
                return fluid;
            }
            return null;
        }

        private void resolveStates() {
            if (surface == null) {
                surface = blockState(surfaceId, surfaceFallback);
                stone = blockState(stoneId, stoneFallback);
                cobblestone = blockState(cobblestoneId, cobblestoneFallback);
                fluid = blockState(fluidId, Blocks.WATER);
            }
        }
    }
}
