package github.com.gengyoubo.CE.compat.mekanism.fission;

import java.util.EnumMap;
import mekanism.common.registration.impl.BlockDeferredRegister;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registration.impl.TileEntityTypeDeferredRegister;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.generators.common.tile.fission.TileEntityFissionReactorCasing;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.resource.PathPackResources;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Loaded only when Mekanism Generators is present. AE2 classes are isolated in FissionMeInterface. */
@SuppressWarnings("removal")
public final class FissionCompat {
    private static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister("changede");
    private static final TileEntityTypeDeferredRegister ENTITIES = new TileEntityTypeDeferredRegister("changede");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "changede");
    public static final RegistryObject<Item> HEAT_EXCHANGE_CORE = ITEMS.register("fission_heat_exchange_core", () -> new Item(new Item.Properties()));
    public static final EnumMap<FissionUpgrade, BlockRegistryObject<FissionUpgradeBlock, BlockItem>> UPGRADES = new EnumMap<>(FissionUpgrade.class);
    public static final EnumMap<FissionUpgrade, TileEntityTypeRegistryObject<TileEntityFissionReactorCasing>> TILES = new EnumMap<>(FissionUpgrade.class);

    public static void initialize(IEventBus bus) {
        for (FissionUpgrade upgrade : FissionUpgrade.values()) {
            if (upgrade == FissionUpgrade.ME_PORT && !ModList.get().isLoaded("ae2")) continue;
            var block = BLOCKS.register(upgrade.id, () -> upgrade == FissionUpgrade.THERMOELECTRIC ? new FissionThermoelectricBlock() : new FissionUpgradeBlock(upgrade));
            UPGRADES.put(upgrade, block);
            var tile = ENTITIES.<TileEntityFissionReactorCasing>register(block, (pos, state) -> upgrade == FissionUpgrade.ME_PORT
                    ? FissionAe2Factory.create(block, pos, state)
                    : upgrade == FissionUpgrade.THERMOELECTRIC ? new FissionThermoelectricBlockEntity(block, pos, state)
                    : new TileEntityFissionReactorCasing(block, pos, state), TileEntityMekanism::tickServer, TileEntityMekanism::tickClient);
            TILES.put(upgrade, tile);
        }
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        ITEMS.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, FissionConfig.SPEC, "changede-fission-server.toml");
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().location().equals(ResourceLocation.parse("changede:extra"))) {
                UPGRADES.values().forEach(block -> event.accept(block.asItem()));
                event.accept(HEAT_EXCHANGE_CORE.get());
            }
        });
        bus.addListener((AddPackFindersEvent event) -> {
            if (event.getPackType() != PackType.SERVER_DATA) return;
            for (String name : new String[]{"fission_upgrades", "fission_me"}) {
                if (name.equals("fission_me") && !ModList.get().isLoaded("ae2")) continue;
                var path = ModList.get().getModFileById("changede").getFile().findResource("resourcepacks", name);
                var pack = Pack.readMetaAndCreate("changede:" + name, Component.literal("Changed Extra " + name), true,
                        id -> new PathPackResources(id, true, path), PackType.SERVER_DATA, Pack.Position.BOTTOM, PackSource.BUILT_IN);
                if (pack != null) event.addRepositorySource(output -> output.accept(pack));
            }
        });
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT)
            FissionClient.initialize(bus);
        if (Boolean.getBoolean("changede.verifyFission")) {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(FissionRegressionChecks::started);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(FissionRegressionChecks::tick);
        }
    }
    private FissionCompat() {}
}
