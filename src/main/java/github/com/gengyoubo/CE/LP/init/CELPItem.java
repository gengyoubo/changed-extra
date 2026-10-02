package github.com.gengyoubo.CE.LP.init;

import github.com.gengyoubo.CE.LP.item.MimicYufengWingsItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CELPItem {
    public static final DeferredRegister<Item> ITEMS;
    //Item

    //BlockItem
    public static final RegistryObject<Item> BASIC_WIRE_ITEM;
    public static final RegistryObject<Item> WLP_PIPE_ITEM;
    public static final RegistryObject<Item> DLP_PIPE_ITEM;
    public static final RegistryObject<Item> BASIC_ITEM_PIPE_ITEM;
    public static final RegistryObject<Item> BASIC_FLUID_PIPE_ITEM;
    public static final RegistryObject<Item> BASIC_PUMP_ITEM;
    public static final RegistryObject<Item> BASIC_LATEX_FLUID_GENERATOR_ITEM;
    public static final RegistryObject<Item> PIPE_WRENCH;
    public static final RegistryObject<Item> BASIC_GENERATOR_ITEM;
    public static final RegistryObject<Item> BASIC_CRYSTAL_GENERATOR_ITEM;
    public static final RegistryObject<Item> ELECTRIC_FURNACE_ITEM;
    public static final RegistryObject<Item> BASIC_ALLOY_FURNACE_ITEM;
    public static final RegistryObject<Item> BASIC_LATEX_PURIFIER_ITEM;
    public static final RegistryObject<Item> LATEXCREATIVE_EXTRANALBODY_CRAFT_TABLE_BLOCK_ITEM;
    public static final RegistryObject<Item> SPACE_TOWER_ITEM;
    public static final RegistryObject<Item> MIMIC_YUFENG_WINGS;
    public static final RegistryObject<Item> WHITE_LATEX_POWER_CONVERTER_ITEM;
    public static final RegistryObject<Item> DARK_LATEX_POWER_CONVERTER_ITEM;
    public static final RegistryObject<Item> ORANGE_PRODUCER_ITEM;
    static {
        ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "changede");
        MIMIC_YUFENG_WINGS = ITEMS.register("mimic_yufeng_wings",
                () -> new MimicYufengWingsItem(new Item.Properties().durability(432).rarity(Rarity.RARE)));
        PIPE_WRENCH = ITEMS.register("pipe_wrench", () -> new Item(new Item.Properties()));
        BASIC_WIRE_ITEM = ITEMS.register("basic_wire",
                () -> new BlockItem(CELPBlock.BASIC_WIRE.get(), new Item.Properties()));
        WLP_PIPE_ITEM = ITEMS.register("wlp_pipe",()->new BlockItem(CELPBlock.WLP_PIPE.get(),new Item.Properties()));
        DLP_PIPE_ITEM = ITEMS.register("dlp_pipe",()->new BlockItem(CELPBlock.DLP_PIPE.get(),new Item.Properties()));
        BASIC_ITEM_PIPE_ITEM = ITEMS.register("basic_item_pipe",
                () -> new BlockItem(CELPBlock.BASIC_ITEM_PIPE.get(), new Item.Properties()));
        BASIC_FLUID_PIPE_ITEM = ITEMS.register("basic_fluid_pipe",
                () -> new BlockItem(CELPBlock.BASIC_FLUID_PIPE.get(), new Item.Properties()));
        BASIC_PUMP_ITEM = ITEMS.register("basic_pump",
                () -> new BlockItem(CELPBlock.BASIC_PUMP.get(), new Item.Properties()));
        BASIC_LATEX_FLUID_GENERATOR_ITEM = ITEMS.register("basic_latex_fluid_generator",
                () -> new BlockItem(CELPBlock.BASIC_LATEX_FLUID_GENERATOR.get(), new Item.Properties()));
        BASIC_GENERATOR_ITEM = ITEMS.register("basic_generator",
                () -> new BlockItem(CELPBlock.BASIC_GENERATOR.get(), new Item.Properties()));
        BASIC_CRYSTAL_GENERATOR_ITEM = ITEMS.register("basic_crystal_generator",
                () -> new BlockItem(CELPBlock.BASIC_CRYSTAL_GENERATOR.get(), new Item.Properties()));
        ELECTRIC_FURNACE_ITEM = ITEMS.register("electric_furnace",
                () -> new BlockItem(CELPBlock.ELECTRIC_FURNACE.get(), new Item.Properties()));
        BASIC_ALLOY_FURNACE_ITEM = ITEMS.register("basic_alloy_furnace",
                () -> new BlockItem(CELPBlock.BASIC_ALLOY_FURNACE.get(), new Item.Properties()));
        BASIC_LATEX_PURIFIER_ITEM = ITEMS.register("basic_latex_purifier",
                () -> new BlockItem(CELPBlock.BASIC_LATEX_PURIFIER.get(), new Item.Properties()));
        LATEXCREATIVE_EXTRANALBODY_CRAFT_TABLE_BLOCK_ITEM = ITEMS.register("latexcreative_extranalbody_craft_table_block",
                () -> new BlockItem(CELPBlock.LATEXCREATIVE_EXTRANALBODY_CRAFT_TABLE_BLOCK.get(), new Item.Properties()));
        SPACE_TOWER_ITEM = ITEMS.register("space_tower",
                () -> new BlockItem(CELPBlock.SPACE_TOWER.get(), new Item.Properties().rarity(Rarity.RARE)));
        WHITE_LATEX_POWER_CONVERTER_ITEM = ITEMS.register("white_latex_power_converter",
                () -> new BlockItem(CELPBlock.WHITE_LATEX_POWER_CONVERTER.get(), new Item.Properties()));
        DARK_LATEX_POWER_CONVERTER_ITEM = ITEMS.register("dark_latex_power_converter",
                () -> new BlockItem(CELPBlock.DARK_LATEX_POWER_CONVERTER.get(), new Item.Properties()));
        ORANGE_PRODUCER_ITEM = ITEMS.register("orange_producer",
                () -> new BlockItem(CELPBlock.ORANGE_PRODUCER.get(), new Item.Properties()));
    }
}
