package github.com.gengyoubo.CE.compat.create.burner;

import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.*;

/** Only initialized behind the Create mod-presence check in the common entry point. */
public final class LatexBurnerCompat {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "changede");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "changede");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "changede");
    public static final RegistryObject<Block> BLOCK = BLOCKS.register("latex_burner", LatexBurnerBlock::new);
    public static final RegistryObject<Item> FILLED = ITEMS.register("latex_burner", () -> new LatexBurnerItem(true));
    public static final RegistryObject<Item> EMPTY = ITEMS.register("empty_latex_burner", () -> new LatexBurnerItem(false));
    public static final RegistryObject<BlockEntityType<LatexBurnerBlockEntity>> ENTITY = ENTITIES.register("latex_burner",
            () -> BlockEntityType.Builder.of(LatexBurnerBlockEntity::new, BLOCK.get()).build(null));

    private LatexBurnerCompat() {}
    public static void initialize(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus);
        bus.addListener(LatexBurnerCompat::setup);
        bus.addListener(LatexBurnerCompat::creativeContents);
        MinecraftForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> event.addListener(new LatexBurnerProfiles()));
        if (Boolean.getBoolean("changede.verifyLatexBurner"))
            MinecraftForge.EVENT_BUS.addListener(LatexBurnerRegressionChecks::started);
        if (FMLEnvironment.dist == Dist.CLIENT) LatexBurnerClient.initialize(bus);
    }
    private static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().location().equals(ResourceLocation.parse("changede:extra"))) event.accept(EMPTY.get());
    }
    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BoilerHeater.REGISTRY.register(BLOCK.get(), BoilerHeater.BLAZE_BURNER);
            Registry.register(CreateBuiltInRegistries.ARM_INTERACTION_POINT_TYPE, ResourceLocation.parse("changede:latex_burner"),
                    new ArmInteractionPointType() {
                        @Override public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) { return state.is(BLOCK.get()); }
                        @Override public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
                            return new ArmInteractionPoint(this, level, pos, state);
                        }
                    });
        });
    }
}
