package github.com.gengyoubo.CE.compat.mekanism.gas;

import java.util.LinkedHashMap;
import java.util.Map;
import mekanism.api.chemical.gas.Gas;
import mekanism.common.block.attribute.AttributeStateFacing;
import mekanism.common.block.attribute.Attributes;
import mekanism.common.block.transmitter.BlockPressurizedTube;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.item.block.transmitter.ItemBlockPressurizedTube;
import mekanism.common.registration.impl.*;
import mekanism.common.tier.TubeTier;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.transmitter.TileEntityPressurizedTube;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.*;
import net.minecraftforge.resource.PathPackResources;

/** Optional Mek main-mod integration; independent of Generators and AE2. */
public final class GasCompat {
    private static final GasDeferredRegister GASES = new GasDeferredRegister("changede");
    private static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister("changede");
    private static final TileEntityTypeDeferredRegister TILES = new TileEntityTypeDeferredRegister("changede");
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "changede");
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, "changede");
    private static final ContainerTypeDeferredRegister CONTAINERS = new ContainerTypeDeferredRegister("changede");
    public static final Map<String, GasRegistryObject<Gas>> BASIC_GASES = new LinkedHashMap<>();
    static {
        BASIC_GASES.put("white_latex_gas", GASES.register("white_latex_gas", 0xF2F1E9));
        BASIC_GASES.put("dark_latex_gas", GASES.register("dark_latex_gas", 0x29232F));
        BASIC_GASES.put("skunk_gas", GASES.register("skunk_gas", 0xA3CA6B));
        BASIC_GASES.put("tiger_gas", GASES.register("tiger_gas", 0xF3AE50));
        BASIC_GASES.put("wolf_gas", GASES.register("wolf_gas", 0x859BCC));
    }
    public static final BlockTypeTile<ReverserBlockEntity> REVERSER_TYPE = BlockTypeTile.BlockTileBuilder
            .<ReverserBlockEntity>createBlock(() -> GasCompat.REVERSER_TILE, () -> "tooltip.changede.reverser")
            .withGui(() -> GasCompat.REVERSER_CONTAINER)
            .with(new AttributeStateFacing(), Attributes.ACTIVE, Attributes.REDSTONE, Attributes.SECURITY).build();
    public static final BlockRegistryObject<ReverserBlock, BlockItem> REVERSER = BLOCKS.register("reverser", ReverserBlock::new);
    public static final TileEntityTypeRegistryObject<ReverserBlockEntity> REVERSER_TILE = TILES.register(REVERSER, ReverserBlockEntity::new, TileEntityMekanism::tickServer, TileEntityMekanism::tickClient);
    public static final BlockRegistryObject<GasPipeBlock, ItemBlockPressurizedTube> GAS_PIPE = BLOCKS.register("basic_gas_pipe", GasPipeBlock::new, ItemBlockPressurizedTube::new);
    public static final TileEntityTypeRegistryObject<TileEntityPressurizedTube> GAS_PIPE_TILE = TILES.<TileEntityPressurizedTube>builder(GAS_PIPE,
            (pos, state) -> new TileEntityPressurizedTube(GAS_PIPE, pos, state))
            .serverTicker(mekanism.common.tile.transmitter.TileEntityTransmitter::tickServer).build();
    public static final RegistryObject<RecipeType<ReversingRecipe>> REVERSING_TYPE = RECIPE_TYPES.register("reversing", () -> RecipeType.simple(ResourceLocation.parse("changede:reversing")));
    public static final RegistryObject<RecipeSerializer<ReversingRecipe>> REVERSING_SERIALIZER = SERIALIZERS.register("reversing", ReversingRecipe.Serializer::new);
    public static final ContainerTypeRegistryObject<ReverserContainer> REVERSER_CONTAINER = CONTAINERS.register(REVERSER, ReverserBlockEntity.class, ReverserContainer::new);

    public static void initialize(IEventBus bus) {
        GASES.register(bus); BLOCKS.register(bus); TILES.register(bus); SERIALIZERS.register(bus); RECIPE_TYPES.register(bus); CONTAINERS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().location().equals(ResourceLocation.parse("changede:extra"))) { event.accept(REVERSER.asItem()); event.accept(GAS_PIPE.asItem()); }
        });
        bus.addListener((AddPackFindersEvent event) -> {
            if (event.getPackType() != PackType.SERVER_DATA) return;
            var path = ModList.get().getModFileById("changede").getFile().findResource("resourcepacks", "gas_technology");
            var pack = Pack.readMetaAndCreate("changede:gas_technology", Component.literal("Changed Extra gas technology"), true,
                    id -> new PathPackResources(id, true, path), PackType.SERVER_DATA, Pack.Position.BOTTOM, PackSource.BUILT_IN);
            if (pack != null) event.addRepositorySource(output -> output.accept(pack));
        });
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) GasClient.initialize(bus);
        if (Boolean.getBoolean("changede.verifyGasTechnology")) {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(GasTechnologyChecks::started);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(GasTechnologyChecks::tick);
        }
    }
    public static final class GasPipeBlock extends BlockPressurizedTube {
        public GasPipeBlock() { super(TubeTier.BASIC); }
        @Override public TileEntityTypeRegistryObject<TileEntityPressurizedTube> getTileType() { return GAS_PIPE_TILE; }
    }
    private GasCompat() {}
}
