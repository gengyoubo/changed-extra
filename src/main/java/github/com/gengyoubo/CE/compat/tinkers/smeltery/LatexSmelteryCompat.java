package github.com.gengyoubo.CE.compat.tinkers.smeltery;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.resource.PathPackResources;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import slimeknights.tconstruct.smeltery.block.component.SearedBlock;
import slimeknights.tconstruct.smeltery.block.entity.component.SmelteryComponentBlockEntity;

public final class LatexSmelteryCompat {
    public static final int CAPACITY = 8000;
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "changede");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "changede");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "changede");
    public static final RegistryObject<Block> CORE = BLOCKS.register("latex_combustion_core", () -> new SearedBlock(properties(), true) {
        @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CoreBlockEntity(pos, state);
        }
    });
    public static final RegistryObject<LatexFuelTankBlock> TANK = BLOCKS.register("latex_fuel_tank", LatexFuelTankBlock::new);
    public static final RegistryObject<Item> CORE_ITEM = ITEMS.register("latex_combustion_core", () -> new BlockItem(CORE.get(), new Item.Properties()));
    public static final RegistryObject<Item> TANK_ITEM = ITEMS.register("latex_fuel_tank", () -> new LatexFuelTankItem(TANK.get()));
    public static final RegistryObject<BlockEntityType<CoreBlockEntity>> CORE_ENTITY = ENTITIES.register("latex_combustion_core",
            () -> BlockEntityType.Builder.of(CoreBlockEntity::new, CORE.get()).build(null));
    public static final RegistryObject<BlockEntityType<LatexFuelTankBlockEntity>> TANK_ENTITY = ENTITIES.register("latex_fuel_tank",
            () -> BlockEntityType.Builder.of(LatexFuelTankBlockEntity::new, TANK.get()).build(null));
    private LatexSmelteryCompat() {}
    public static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.copy(Blocks.BRICKS).strength(3, 20).requiresCorrectToolForDrops();
    }
    public static void initialize(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus);
        bus.addListener((AddPackFindersEvent event) -> {
            if (event.getPackType() != PackType.SERVER_DATA) return;
            var path = ModList.get().getModFileById("changede").getFile().findResource("resourcepacks", "latex_smeltery");
            var pack = Pack.readMetaAndCreate("changede:latex_smeltery", Component.literal("Changed Extra Latex Smeltery"), true,
                    id -> new PathPackResources(id, true, path), PackType.SERVER_DATA, Pack.Position.BOTTOM, PackSource.BUILT_IN);
            if (pack != null) event.addRepositorySource(output -> output.accept(pack));
        });
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().location().equals(ResourceLocation.parse("changede:extra"))) {
                event.accept(CORE_ITEM.get()); event.accept(TANK_ITEM.get());
            }
        });
        if (FMLEnvironment.dist == Dist.CLIENT) LatexSmelteryClient.initialize(bus);
    }
    public static final class CoreBlockEntity extends SmelteryComponentBlockEntity {
        public CoreBlockEntity(BlockPos pos, BlockState state) { super(CORE_ENTITY.get(), pos, state); }
    }
}
