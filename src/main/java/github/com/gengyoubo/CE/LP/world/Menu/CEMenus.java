package github.com.gengyoubo.CE.LP.world.Menu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CEMenus {
    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "changede");
    public static final RegistryObject<MenuType<MachineStatusMenu>> MACHINE_STATUS = REGISTRY.register("machine_status", () -> IForgeMenuType.create(MachineStatusMenu::new));
    public static final RegistryObject<MenuType<BasicAlloyFurnaceMenu>> BASIC_ALLOY_FURNACE = REGISTRY.register("basic_alloy_furnace", () -> IForgeMenuType.create(BasicAlloyFurnaceMenu::new));
    public static final RegistryObject<MenuType<BasicGeneratorBlockEntityMenu>> BASIC_GENERATOR_BLOCK_ENTITY = REGISTRY.register("basic_generator_block_entity", () -> IForgeMenuType.create(BasicGeneratorBlockEntityMenu::new));
    public static final RegistryObject<MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE = REGISTRY.register("electric_furnace", () -> IForgeMenuType.create(ElectricFurnaceMenu::new));
    public static final RegistryObject<MenuType<BasicLatexPurifierMenu>> BASIC_LATEX_PURIFIER = REGISTRY.register("basic_latex_purifier", () -> IForgeMenuType.create(BasicLatexPurifierMenu::new));
    public static final RegistryObject<MenuType<LatexCreativeExtranalbodyCraftTableMenu>> LATEX_CREATIVE_EXTRANALBODY_CRAFT_TABLE =
            REGISTRY.register("latexcreative_extranalbody_craft_table", () -> IForgeMenuType.create(LatexCreativeExtranalbodyCraftTableMenu::new));
    public static final RegistryObject<MenuType<SpaceTowerMenu>> SPACE_TOWER =
            REGISTRY.register("space_tower", () -> IForgeMenuType.create(SpaceTowerMenu::new));
    public static final RegistryObject<MenuType<PipeConfigMenu>> PIPE_CONFIG =
            REGISTRY.register("pipe_config", () -> IForgeMenuType.create(PipeConfigMenu::new));

}
