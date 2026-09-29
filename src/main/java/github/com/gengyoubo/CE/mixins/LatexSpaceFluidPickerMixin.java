package github.com.gengyoubo.CE.mixins;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class LatexSpaceFluidPickerMixin {
    private static final ResourceKey<NoiseGeneratorSettings> LATEX_SPACE_SETTINGS = ResourceKey.create(
            Registries.NOISE_SETTINGS, ResourceLocation.fromNamespaceAndPath("changede", "latex_space"));
    private static final Set<ResourceKey<Biome>> DARK_BIOMES = Set.of(
            biome("dark_latex_plains"), biome("dark_latex_forest"), biome("dark_latex_sea"));
    private static final ResourceLocation DARK_FLUID = ResourceLocation.fromNamespaceAndPath("changed", "dark_latex_fluid");
    private static final ResourceLocation WHITE_FLUID = ResourceLocation.fromNamespaceAndPath("changed", "white_latex_fluid");

    @Shadow @Final private Holder<NoiseGeneratorSettings> settings;

    @Inject(method = "createNoiseChunk", at = @At("HEAD"), cancellable = true)
    private void changede$useBiomeFluid(ChunkAccess chunk, StructureManager structures, Blender blender,
                                        RandomState randomState, CallbackInfoReturnable<NoiseChunk> result) {
        if (!settings.is(LATEX_SPACE_SETTINGS)) {
            return;
        }

        int seaLevel = settings.value().seaLevel();
        Aquifer.FluidStatus lava = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        Aquifer.FluidStatus dark = new Aquifer.FluidStatus(seaLevel, fluidState(DARK_FLUID, settings.value().defaultFluid()));
        Aquifer.FluidStatus white = new Aquifer.FluidStatus(seaLevel, fluidState(WHITE_FLUID, Blocks.WATER.defaultBlockState()));
        BiomeSource biomes = ((ChunkGenerator) (Object) this).getBiomeSource();
        Long2ObjectOpenHashMap<Aquifer.FluidStatus> biomeFluids = new Long2ObjectOpenHashMap<>();
        Aquifer.FluidPicker picker = (x, y, z) -> {
            if (y < Math.min(-54, seaLevel)) {
                return lava;
            }
            int quartX = QuartPos.fromBlock(x);
            int quartY = QuartPos.fromBlock(y);
            int quartZ = QuartPos.fromBlock(z);
            long biomePos = BlockPos.asLong(quartX, quartY, quartZ);
            Aquifer.FluidStatus cached = biomeFluids.get(biomePos);
            if (cached != null) {
                return cached;
            }
            Holder<Biome> biome = biomes.getNoiseBiome(
                    quartX, quartY, quartZ, randomState.sampler());
            Aquifer.FluidStatus selected = biome.is(DARK_BIOMES::contains) ? dark : white;
            biomeFluids.put(biomePos, selected);
            return selected;
        };

        result.setReturnValue(NoiseChunk.forChunk(chunk, randomState,
                Beardifier.forStructuresInChunk(structures, chunk.getPos()), settings.value(), picker, blender));
    }

    private static ResourceKey<Biome> biome(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("changede", name));
    }

    private static BlockState fluidState(ResourceLocation id, BlockState fallback) {
        net.minecraft.world.level.block.Block block = ForgeRegistries.BLOCKS.getValue(id);
        return block == null ? fallback : block.defaultBlockState();
    }
}
