package github.com.gengyoubo.LP.world.Menu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CEMenus {
    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "changede");
    public static final RegistryObject<MenuType<BasicGeneratorBlockEntityMenu>> BASIC_GENERATOR_BLOCK_ENTITY = REGISTRY.register("basic_generator_block_entity", () -> IForgeMenuType.create(BasicGeneratorBlockEntityMenu::new));
    public static final RegistryObject<MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE = REGISTRY.register("electric_furnace", () -> IForgeMenuType.create(ElectricFurnaceMenu::new));

}
