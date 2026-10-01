package github.com.gengyoubo.CE.events;

import github.com.gengyoubo.CE.init.CEWoodFamilies;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = "changede")
public final class CEWoodEvents {
    @SubscribeEvent public static void strip(BlockEvent.BlockToolModificationEvent event) {
        if (event.getToolAction() != ToolActions.AXE_STRIP) return;
        for (var family : CEWoodFamilies.ALL) {
            String target = event.getState().is(family.log.get()) ? "stripped_log"
                    : event.getState().is(family.blocks.get("wood").get()) ? "stripped_wood" : null;
            if (target != null) event.setFinalState(family.blocks.get(target).get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, event.getState().getValue(RotatedPillarBlock.AXIS)));
        }
    }
    @Mod.EventBusSubscriber(modid = "changede", bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent public static void setup(FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                var fire = (github.com.gengyoubo.CE.mixins.FireBlockAccessor) Blocks.FIRE;
                for (var family : CEWoodFamilies.ALL) family.blocks.forEach((name, block) -> {
                    if (!name.contains("sign") && !name.equals("button") && !name.equals("pressure_plate"))
                        fire.changede$setFlammable(block.get(), 5,
                                name.contains("wood") || name.contains("log") ? 5 : 20);
                });
            });
        }
    }
}
