package github.com.gengyoubo.CE.compat.tinkers.smeltery;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;

/** Separate molten fluids; ordinary Changed latex keeps its existing temperature and recipes. */
public final class MoltenLatexFluids {
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, "changede");
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, "changede");
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "changede");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "changede");
    public static final Entry DARK = new Entry("molten_dark_latex", 0xFF393345);
    public static final Entry WHITE = new Entry("molten_white_latex", 0xFFFFF4DE);
    private MoltenLatexFluids() {}
    public static void initialize(IEventBus bus) {
        TYPES.register(bus); FLUIDS.register(bus); BLOCKS.register(bus); ITEMS.register(bus);
    }
    public static boolean isMolten(Fluid fluid) {
        return fluid == DARK.source.get() || fluid == DARK.flowing.get() || fluid == WHITE.source.get() || fluid == WHITE.flowing.get();
    }
    public static final class Entry {
        public final RegistryObject<FluidType> type;
        public final RegistryObject<Fluid> source, flowing;
        public final RegistryObject<LiquidBlock> block;
        public final RegistryObject<Item> bucket;
        private Entry(String name, int color) {
            type = TYPES.register(name, () -> new FluidType(FluidType.Properties.create().temperature(2273).density(3000)
                    .viscosity(6000).lightLevel(8).canSwim(false).canDrown(false)) {
                @Override public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override public ResourceLocation getStillTexture() { return ResourceLocation.parse("minecraft:block/lava_still"); }
                        @Override public ResourceLocation getFlowingTexture() { return ResourceLocation.parse("minecraft:block/lava_flow"); }
                        @Override public int getTintColor() { return color; }
                    });
                }
            });
            source = FLUIDS.register(name, () -> new ForgeFlowingFluid.Source(properties()));
            flowing = FLUIDS.register("flowing_" + name, () -> new ForgeFlowingFluid.Flowing(properties()));
            block = BLOCKS.register(name, () -> new LiquidBlock(() -> (net.minecraft.world.level.material.FlowingFluid) source.get(),
                    BlockBehaviour.Properties.copy(Blocks.LAVA).mapColor(MapColor.COLOR_PURPLE)));
            bucket = ITEMS.register(name + "_bucket", () -> new BucketItem(source, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
        }
        private ForgeFlowingFluid.Properties properties() {
            return new ForgeFlowingFluid.Properties(type, source, flowing).block(block).bucket(bucket).tickRate(30);
        }
    }
}
