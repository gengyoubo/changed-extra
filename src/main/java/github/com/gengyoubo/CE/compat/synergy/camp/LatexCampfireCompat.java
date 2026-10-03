package github.com.gengyoubo.CE.compat.synergy.camp;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.*;

/** Loaded and registered only when Changed: Synergy is present. */
public final class LatexCampfireCompat {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "changede");
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "changede");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "changede");
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "changede");
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "changede");
    public static final RegistryObject<Block> CAMPFIRE = BLOCKS.register("latex_campfire", LatexCampfireBlock::new);
    public static final RegistryObject<Item> CAMPFIRE_ITEM = ITEMS.register("latex_campfire", () -> new BlockItem(CAMPFIRE.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<LatexCampfireBlockEntity>> CORE = ENTITIES.register("latex_campfire",
            () -> BlockEntityType.Builder.of(LatexCampfireBlockEntity::new, CAMPFIRE.get()).build(null));
    public static final RegistryObject<MenuType<LatexCampfireMenu>> MENU = MENUS.register("latex_campfire", () -> IForgeMenuType.create(LatexCampfireMenu::new));
    // Replace this single entry point when CS provides its own creative tab.
    public static final RegistryObject<CreativeModeTab> SYNERGY_TAB = TABS.register("synergy", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.changede.synergy"))
            .icon(() -> new ItemStack(CAMPFIRE_ITEM.get())).displayItems(LatexCampfireCompat::displaySynergyItems).build());

    private static void displaySynergyItems(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        output.accept(CAMPFIRE_ITEM.get());
    }

    public static void initialize(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus); TABS.register(bus);
        CampNetwork.register();
        MinecraftForge.EVENT_BUS.register(LatexSettlementService.class);
        if (FMLEnvironment.dist == Dist.CLIENT) CampClient.initialize(bus);
    }
}
