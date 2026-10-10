package github.com.gengyoubo.CE.LP.world.Menu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CEMenus {
    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "changede");
    public static final RegistryObject<MenuType<github.com.gengyoubo.CE.skill.LatexSkillResearchMenu>> LATEX_SKILL_RESEARCH =
            REGISTRY.register("latex_skill_research",()->IForgeMenuType.create(github.com.gengyoubo.CE.skill.LatexSkillResearchMenu::new));
    public static final RegistryObject<MenuType<MachineStatusMenu>> MACHINE_STATUS = REGISTRY.register("machine_status", () -> IForgeMenuType.create(MachineStatusMenu::new));
    public static final RegistryObject<MenuType<BasicAlloyFurnaceMenu>> BASIC_ALLOY_FURNACE = REGISTRY.register("basic_alloy_furnace", () -> IForgeMenuType.create(BasicAlloyFurnaceMenu::new));
    public static final RegistryObject<MenuType<IngotFillerMenu>> INGOT_FILLER = REGISTRY.register("ingot_filler",()->IForgeMenuType.create(IngotFillerMenu::new));
    public static final RegistryObject<MenuType<BasicGeneratorBlockEntityMenu>> BASIC_GENERATOR_BLOCK_ENTITY = REGISTRY.register("basic_generator_block_entity", () -> IForgeMenuType.create(BasicGeneratorBlockEntityMenu::new));
    public static final RegistryObject<MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE = REGISTRY.register("electric_furnace", () -> IForgeMenuType.create(ElectricFurnaceMenu::new));
    public static final RegistryObject<MenuType<BasicLatexPurifierMenu>> BASIC_LATEX_PURIFIER = REGISTRY.register("basic_latex_purifier", () -> IForgeMenuType.create(BasicLatexPurifierMenu::new));
    public static final RegistryObject<MenuType<LatexCreativeExtranalbodyCraftTableMenu>> LATEX_CREATIVE_EXTRANALBODY_CRAFT_TABLE =
            REGISTRY.register("latexcreative_extranalbody_craft_table", () -> IForgeMenuType.create(LatexCreativeExtranalbodyCraftTableMenu::new));
    public static final RegistryObject<MenuType<SpaceTowerMenu>> SPACE_TOWER =
            REGISTRY.register("space_tower", () -> IForgeMenuType.create(SpaceTowerMenu::new));
    public static final RegistryObject<MenuType<DimensionSpaceTowerMenu>> DIMENSION_SPACE_TOWER =
            REGISTRY.register("dimension_space_tower", () -> IForgeMenuType.create(DimensionSpaceTowerMenu::new));
    public static final RegistryObject<MenuType<FluidDimensionSpaceTowerMenu>> FLUID_DIMENSION_SPACE_TOWER =
            REGISTRY.register("fluid_dimension_space_tower", () -> IForgeMenuType.create(FluidDimensionSpaceTowerMenu::new));
    public static final RegistryObject<MenuType<PipeConfigMenu>> PIPE_CONFIG =
            REGISTRY.register("pipe_config", () -> IForgeMenuType.create(PipeConfigMenu::new));

}
