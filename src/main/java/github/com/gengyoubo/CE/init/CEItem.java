package github.com.gengyoubo.CE.init;

import github.com.gengyoubo.CE.items.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public class CEItem {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "changede");
    public static final RegistryObject<Item> INACTIVE_DARK_LATEX =
            ITEMS.register("inactive_dark_latex", InactiveDarkLatex::new);
    public static final RegistryObject<Item> INACTIVE_WHITE_LATEX =
            ITEMS.register("inactive_white_latex", InactiveWhiteLatex::new);
    public static final RegistryObject<Item> LATEX_GRAY =
            ITEMS.register("latex_gray", LatexGray::new);
    public static final RegistryObject<Item> LATEX_INGOT =
            ITEMS.register("latex_ingot", LatexIngot::new);
    public static final RegistryObject<Item> UNBAKED_LATEX_INGOT =
            ITEMS.register("unbaked_latex_ingot", UnbakedLatexIngot::new);
    public static final RegistryObject<Item> DARK_LATEX_LOG =
            ITEMS.register("dark_latex_log", () -> new BlockItem(CEBlock.DARK_LATEX_LOG.get(), new Item.Properties()));
    public static final RegistryObject<Item> DARK_LATEX_PLANKS =
            ITEMS.register("dark_latex_planks", () -> new BlockItem(CEBlock.DARK_LATEX_PLANKS.get(), new Item.Properties()));
    public static final RegistryObject<Item> DARK_LATEX_LEAVES =
            ITEMS.register("dark_latex_leaves", () -> new BlockItem(CEBlock.DARK_LATEX_LEAVES.get(), new Item.Properties()));
    public static final RegistryObject<Item> DARK_LATEX_STONE =
            ITEMS.register("dark_latex_stone", () -> new BlockItem(CEBlock.DARK_LATEX_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> DARK_LATEX_COBBLESTONE =
            ITEMS.register("dark_latex_cobblestone", () -> new BlockItem(CEBlock.DARK_LATEX_COBBLESTONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> WHITE_LATEX_LOG =
            ITEMS.register("white_latex_log", () -> new BlockItem(CEBlock.WHITE_LATEX_LOG.get(), new Item.Properties()));
    public static final RegistryObject<Item> WHITE_LATEX_PLANKS =
            ITEMS.register("white_latex_planks", () -> new BlockItem(CEBlock.WHITE_LATEX_PLANKS.get(), new Item.Properties()));
    public static final RegistryObject<Item> WHITE_LATEX_LEAVES =
            ITEMS.register("white_latex_leaves", () -> new BlockItem(CEBlock.WHITE_LATEX_LEAVES.get(), new Item.Properties()));
    public static final RegistryObject<Item> WHITE_LATEX_STONE =
            ITEMS.register("white_latex_stone", () -> new BlockItem(CEBlock.WHITE_LATEX_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> WHITE_LATEX_COBBLESTONE =
            ITEMS.register("white_latex_cobblestone", () -> new BlockItem(CEBlock.WHITE_LATEX_COBBLESTONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> LATEX_PAINTING_PORTAL =
            ITEMS.register("latex_painting_portal", () -> new LatexPaintingPortalItem(new Item.Properties()));
    public static final RegistryObject<Item> PEACH =
            ITEMS.register("peach", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                    .nutrition(4)
                    .saturationMod(0.3F)
                    .build())));
    public static final RegistryObject<Item> ENCHANTED_GOLDEN_ORANGE =
            ITEMS.register("enchanted_golden_orange", EnchantedGoldenOrange::new);
    public static final RegistryObject<Item> CHAIN_INGOT =
            ITEMS.register("chain_ingot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PLATE =
            ITEMS.register("plate", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PLATE_HELMET =
            ITEMS.register("plate_helmet", () -> new ArmorItem(CEArmorMaterials.PLATE, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> PLATE_CHESTPLATE =
            ITEMS.register("plate_chestplate", () -> new ArmorItem(CEArmorMaterials.PLATE, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> PLATE_LEGGINGS =
            ITEMS.register("plate_leggings", () -> new ArmorItem(CEArmorMaterials.PLATE, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> PLATE_BOOTS =
            ITEMS.register("plate_boots", () -> new ArmorItem(CEArmorMaterials.PLATE, ArmorItem.Type.BOOTS, new Item.Properties()));
    public static final RegistryObject<Item> BLACK_LATEX_COFFEE_POWDER = simpleItem("black_latex_coffee_powder");
    public static final RegistryObject<Item> WHITE_LATEX_MILK = simpleItem("white_latex_milk");
    public static final RegistryObject<Item> HOT_LATEX_COFFEE_E_HALF = simpleItem("hot_latex_coffee_e_half");
    public static final RegistryObject<Item> HOT_LATEX_COFFEE_E = latexDrinkItem("hot_latex_coffee_e", false);
    public static final RegistryObject<Item> HOT_LATEX_MACCHIATO_HALF = simpleItem("hot_latex_macchiato_half");
    public static final RegistryObject<Item> HOT_LATEX_MACCHIATO = latexDrinkItem("hot_latex_macchiato", false);
    public static final RegistryObject<Item> HOT_LATEX_COFFEE_A_HALF = simpleItem("hot_latex_coffee_a_half");
    public static final RegistryObject<Item> HOT_LATEX_COFFEE_A = latexDrinkItem("hot_latex_coffee_a", false);
    public static final RegistryObject<Item> HOT_LATEX_WHITE_COFFEE_HALF = simpleItem("hot_latex_white_coffee_half");
    public static final RegistryObject<Item> HOT_LATEX_WHITE_COFFEE = latexDrinkItem("hot_latex_white_coffee", false);
    public static final RegistryObject<Item> HOT_LATEX_LATTE_HALF = simpleItem("hot_latex_latte_half");
    public static final RegistryObject<Item> HOT_LATEX_LATTE = latexDrinkItem("hot_latex_latte", false);
    public static final RegistryObject<Item> HOT_LATEX_CON_PANNA_HALF = simpleItem("hot_latex_con_panna_half");
    public static final RegistryObject<Item> HOT_LATEX_CON_PANNA = latexDrinkItem("hot_latex_con_panna", false);
    public static final RegistryObject<Item> HOT_LATEX_HALF_LATTE_HALF = simpleItem("hot_latex_half_latte_half");
    public static final RegistryObject<Item> HOT_LATEX_HALF_LATTE = latexDrinkItem("hot_latex_half_latte", false);
    public static final RegistryObject<Item> HOT_LATEX_CAPPUCCINO_HALF = simpleItem("hot_latex_cappuccino_half");
    public static final RegistryObject<Item> HOT_LATEX_CAPPUCCINO = latexDrinkItem("hot_latex_cappuccino", false);
    public static final RegistryObject<Item> HOT_LATEX_MOCHA_HALF = simpleItem("hot_latex_mocha_half");
    public static final RegistryObject<Item> HOT_LATEX_MOCHA = latexDrinkItem("hot_latex_mocha", false);
    public static final RegistryObject<Item> HOT_LATEX_SUGAR_MACCHIATO_HALF = simpleItem("hot_latex_sugar_macchiato_half");
    public static final RegistryObject<Item> HOT_LATEX_SUGAR_MACCHIATO = latexDrinkItem("hot_latex_sugar_macchiato", false);
    public static final RegistryObject<Item> HOT_LATEX_VIENNA_COFFEE_HALF = simpleItem("hot_latex_vienna_coffee_half");
    public static final RegistryObject<Item> HOT_LATEX_VIENNA_COFFEE = latexDrinkItem("hot_latex_vienna_coffee", false);
    public static final RegistryObject<Item> ICE_LATEX_COFFEE_E_HALF = simpleItem("ice_latex_coffee_e_half");
    public static final RegistryObject<Item> ICE_LATEX_COFFEE_E = latexDrinkItem("ice_latex_coffee_e", true);
    public static final RegistryObject<Item> ICE_LATEX_MACCHIATO_HALF = simpleItem("ice_latex_macchiato_half");
    public static final RegistryObject<Item> ICE_LATEX_MACCHIATO = latexDrinkItem("ice_latex_macchiato", true);
    public static final RegistryObject<Item> ICE_LATEX_COFFEE_A_HALF = simpleItem("ice_latex_coffee_a_half");
    public static final RegistryObject<Item> ICE_LATEX_COFFEE_A = latexDrinkItem("ice_latex_coffee_a", true);
    public static final RegistryObject<Item> ICE_LATEX_WHITE_COFFEE_HALF = simpleItem("ice_latex_white_coffee_half");
    public static final RegistryObject<Item> ICE_LATEX_WHITE_COFFEE = latexDrinkItem("ice_latex_white_coffee", true);
    public static final RegistryObject<Item> ICE_LATEX_LATTE_HALF = simpleItem("ice_latex_latte_half");
    public static final RegistryObject<Item> ICE_LATEX_LATTE = latexDrinkItem("ice_latex_latte", true);
    public static final RegistryObject<Item> ICE_LATEX_CON_PANNA_HALF = simpleItem("ice_latex_con_panna_half");
    public static final RegistryObject<Item> ICE_LATEX_CON_PANNA = latexDrinkItem("ice_latex_con_panna", true);
    public static final RegistryObject<Item> ICE_LATEX_HALF_LATTE_HALF = simpleItem("ice_latex_half_latte_half");
    public static final RegistryObject<Item> ICE_LATEX_HALF_LATTE = latexDrinkItem("ice_latex_half_latte", true);
    public static final RegistryObject<Item> ICE_LATEX_CAPPUCCINO_HALF = simpleItem("ice_latex_cappuccino_half");
    public static final RegistryObject<Item> ICE_LATEX_CAPPUCCINO = latexDrinkItem("ice_latex_cappuccino", true);
    public static final RegistryObject<Item> ICE_LATEX_MOCHA_HALF = simpleItem("ice_latex_mocha_half");
    public static final RegistryObject<Item> ICE_LATEX_MOCHA = latexDrinkItem("ice_latex_mocha", true);
    public static final RegistryObject<Item> ICE_LATEX_SUGAR_MACCHIATO_HALF = simpleItem("ice_latex_sugar_macchiato_half");
    public static final RegistryObject<Item> ICE_LATEX_SUGAR_MACCHIATO = latexDrinkItem("ice_latex_sugar_macchiato", true);
    public static final RegistryObject<Item> ICE_LATEX_VIENNA_COFFEE_HALF = simpleItem("ice_latex_vienna_coffee_half");
    public static final RegistryObject<Item> ICE_LATEX_VIENNA_COFFEE = latexDrinkItem("ice_latex_vienna_coffee", true);
    public static final List<RegistryObject<Item>> LATEX_DRINKS = List.of(
            BLACK_LATEX_COFFEE_POWDER, WHITE_LATEX_MILK,
            HOT_LATEX_COFFEE_E_HALF, HOT_LATEX_COFFEE_E,
            HOT_LATEX_MACCHIATO_HALF, HOT_LATEX_MACCHIATO,
            HOT_LATEX_COFFEE_A_HALF, HOT_LATEX_COFFEE_A,
            HOT_LATEX_WHITE_COFFEE_HALF, HOT_LATEX_WHITE_COFFEE,
            HOT_LATEX_LATTE_HALF, HOT_LATEX_LATTE,
            HOT_LATEX_CON_PANNA_HALF, HOT_LATEX_CON_PANNA,
            HOT_LATEX_HALF_LATTE_HALF, HOT_LATEX_HALF_LATTE,
            HOT_LATEX_CAPPUCCINO_HALF, HOT_LATEX_CAPPUCCINO,
            HOT_LATEX_MOCHA_HALF, HOT_LATEX_MOCHA,
            HOT_LATEX_SUGAR_MACCHIATO_HALF, HOT_LATEX_SUGAR_MACCHIATO,
            HOT_LATEX_VIENNA_COFFEE_HALF, HOT_LATEX_VIENNA_COFFEE,
            ICE_LATEX_COFFEE_E_HALF, ICE_LATEX_COFFEE_E,
            ICE_LATEX_MACCHIATO_HALF, ICE_LATEX_MACCHIATO,
            ICE_LATEX_COFFEE_A_HALF, ICE_LATEX_COFFEE_A,
            ICE_LATEX_WHITE_COFFEE_HALF, ICE_LATEX_WHITE_COFFEE,
            ICE_LATEX_LATTE_HALF, ICE_LATEX_LATTE,
            ICE_LATEX_CON_PANNA_HALF, ICE_LATEX_CON_PANNA,
            ICE_LATEX_HALF_LATTE_HALF, ICE_LATEX_HALF_LATTE,
            ICE_LATEX_CAPPUCCINO_HALF, ICE_LATEX_CAPPUCCINO,
            ICE_LATEX_MOCHA_HALF, ICE_LATEX_MOCHA,
            ICE_LATEX_SUGAR_MACCHIATO_HALF, ICE_LATEX_SUGAR_MACCHIATO,
            ICE_LATEX_VIENNA_COFFEE_HALF, ICE_LATEX_VIENNA_COFFEE
    );

    private static RegistryObject<Item> simpleItem(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Item> latexDrinkItem(String name, boolean iced) {
        return ITEMS.register(name, () -> new LatexDrinkItem(iced));
    }
}
