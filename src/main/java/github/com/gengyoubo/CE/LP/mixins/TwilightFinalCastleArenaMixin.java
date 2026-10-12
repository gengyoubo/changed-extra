package github.com.gengyoubo.CE.LP.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.init.TFBlocks;
import twilightforest.world.components.structures.TFStructureComponentOld;
import twilightforest.world.components.structures.finalcastle.FinalCastleBossGazeboComponent;

@Mixin(value = FinalCastleBossGazeboComponent.class, remap = false)
public abstract class TwilightFinalCastleArenaMixin extends TFStructureComponentOld {
    protected TwilightFinalCastleArenaMixin(StructurePieceType type, CompoundTag tag) { super(type, tag); }
    @Inject(method = "postProcess", at = @At("HEAD"), cancellable = true, remap = true)
    private void changede$finishArena(WorldGenLevel world, StructureManager manager, ChunkGenerator generator,
                                     RandomSource random, BoundingBox bounds, ChunkPos chunk, BlockPos origin, CallbackInfo ci) {
        var wall = TFBlocks.VIOLET_FORCE_FIELD.get().defaultBlockState();
        var floor = TFBlocks.VIOLET_CASTLE_RUNE_BRICK.get().defaultBlockState();
        generateBox(world, bounds, 0, -1, 0, 20, -1, 20, floor, floor, false);
        generateBox(world, bounds, 0, 0, 0, 20, 11, 20, wall, Blocks.AIR.defaultBlockState(), false);
        // Two entrances connect the existing castle roof to the former WIP enclosure.
        generateAirBox(world, bounds, 9, 0, 0, 11, 3, 0);
        generateAirBox(world, bounds, 9, 0, 20, 11, 3, 20);
        placeBlock(world, TFBlocks.FINAL_BOSS_BOSS_SPAWNER.get().defaultBlockState(), 10, 0, 10, bounds);
        ci.cancel();
    }
}
