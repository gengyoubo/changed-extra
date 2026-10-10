package github.com.gengyoubo.CE.compat.create.burner;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.ltxprogrammer.changed.init.ChangedItems;
import net.ltxprogrammer.changed.init.ChangedFluids;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public final class LatexBurnerPonder {
    private LatexBurnerPonder() {}
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(LatexBurnerCompat.EMPTY.getId(), LatexBurnerCompat.FILLED.getId())
                .addStoryBoard("latex_burner/intro", LatexBurnerPonder::intro);
    }
    private static void intro(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("latex_burner", "Latex Creature Burner");
        scene.configureBasePlate(0, 0, 5); scene.showBasePlate();
        BlockPos pos = util.grid().at(2, 1, 2);
        scene.world().setBlock(pos, LatexBurnerCompat.BLOCK.get().defaultBlockState(), false);
        scene.world().showSection(util.select().position(pos), Direction.DOWN);
        text(scene, util, pos, "capture");
        scene.world().modifyBlockEntity(pos, LatexBurnerBlockEntity.class, burner -> {
            CompoundTag tag = new CompoundTag(), creature = new CompoundTag();
            creature.putString("id", "changed:dark_latex_wolf_male");
            tag.put("Creature", creature); tag.putString("RuntimeLatex", "dark");
            tag.putInt("ItemBurnTicks", 2400); burner.load(tag);
        });
        scene.world().setBlock(pos, LatexBurnerCompat.BLOCK.get().defaultBlockState().setValue(LatexBurnerBlock.OCCUPIED, true)
                .setValue(com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL,
                        com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.KINDLED), false);
        scene.world().setBlock(pos.above(), net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("create:basin")).defaultBlockState(), false);
        scene.world().setBlock(pos.above(3), net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("create:mechanical_mixer")).defaultBlockState(), false);
        scene.world().showSection(util.select().fromTo(pos.above(), pos.above(3)), Direction.DOWN);
        scene.world().setKineticSpeed(util.select().position(pos.above(3)), 64);
        text(scene, util, pos, "single");
        scene.world().modifyBlockEntity(pos, LatexBurnerBlockEntity.class, burner -> {
            CompoundTag tag = burner.saveWithoutMetadata(); tag.putInt("FluidBurnTicks", 2400); burner.load(tag);
        });
        scene.world().modifyBlock(pos, state -> state.setValue(com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL,
                com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.SEETHING), false);
        text(scene, util, pos, "both"); text(scene, util, pos, "speed");
        scene.world().modifyBlockEntity(pos, LatexBurnerBlockEntity.class, burner -> {
            CompoundTag tag = burner.saveWithoutMetadata(); tag.putInt("ItemBurnTicks", 0); tag.putInt("FluidBurnTicks", 0); burner.load(tag);
        });
        scene.world().modifyBlock(pos, state -> state.setValue(com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL,
                com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel.SMOULDERING), false);
        text(scene, util, pos, "empty");
    }
    private static void text(CreateSceneBuilder scene, SceneBuildingUtil util, BlockPos pos, String key) {
        scene.overlay().showText(100).text(I18n.get("changede.ponder.latex_burner." + key)).pointAt(util.vector().topOf(pos)).placeNearTarget();
        scene.idle(110);
    }
}
