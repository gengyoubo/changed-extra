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
    @Inject(method = "<init>(ILtwilightforest/world/components/structures/TFStructureComponentOld;III)V", at = @At("RETURN"))
    private void changede$expandArena(int index, TFStructureComponentOld keep, int x, int y, int z, CallbackInfo ci) {
        var box = keep.getBoundingBox();
        this.boundingBox = new BoundingBox(box.minX() + 2, box.maxY() + 2, box.minZ() + 2, box.maxX() - 2, box.maxY() + 26, box.maxZ() - 2);
    }
    @Inject(method = "postProcess", at = @At("HEAD"), cancellable = true, remap = true)
    private void changede$finishArena(WorldGenLevel world, StructureManager manager, ChunkGenerator generator,
                                     RandomSource random, BoundingBox bounds, ChunkPos chunk, BlockPos origin, CallbackInfo ci) {
        var wall = TFBlocks.VIOLET_FORCE_FIELD.get().defaultBlockState();
        var floor = TFBlocks.VIOLET_CASTLE_RUNE_BRICK.get().defaultBlockState();
        generateBox(world, bounds, 0, -1, 0, 44, -1, 44, floor, floor, false);
        generateBox(world, bounds, 0, 0, 0, 44, 24, 44, wall, Blocks.AIR.defaultBlockState(), false);
        // Two entrances connect the existing castle roof to the former WIP enclosure.
        generateAirBox(world, bounds, 21, 0, 0, 23, 3, 0);
        generateAirBox(world, bounds, 21, 0, 44, 23, 3, 44);
        placeBlock(world, TFBlocks.FINAL_BOSS_BOSS_SPAWNER.get().defaultBlockState(), 22, 0, 22, bounds);
        ci.cancel();
    }
}
