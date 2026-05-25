package github.com.gengyoubo.LP.init;

import github.com.gengyoubo.LP.item.MimicYufengWingsItem;
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
    public static final RegistryObject<Item> BASIC_GENERATOR_ITEM;
    public static final RegistryObject<Item> ELECTRIC_FURNACE_ITEM;
    public static final RegistryObject<Item> LATEXCREATIVE_EXTRANALBODY_CRAFT_TABLE_BLOCK_ITEM;
    public static final RegistryObject<Item> MIMIC_YUFENG_WINGS;
    static {
        ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "changede");
        MIMIC_YUFENG_WINGS = ITEMS.register("mimic_yufeng_wings",
                () -> new MimicYufengWingsItem(new Item.Properties().durability(432).rarity(Rarity.RARE)));
        BASIC_WIRE_ITEM = ITEMS.register("basic_wire",
                () -> new BlockItem(CELPBlock.BASIC_WIRE.get(), new Item.Properties()));
        BASIC_GENERATOR_ITEM = ITEMS.register("basic_generator",
                () -> new BlockItem(CELPBlock.BASIC_GENERATOR.get(), new Item.Properties()));
        ELECTRIC_FURNACE_ITEM = ITEMS.register("electric_furnace",
                () -> new BlockItem(CELPBlock.ELECTRIC_FURNACE.get(), new Item.Properties()));
        LATEXCREATIVE_EXTRANALBODY_CRAFT_TABLE_BLOCK_ITEM = ITEMS.register("latexcreative_extranalbody_craft_table_block",
                () -> new BlockItem(CELPBlock.LATEXCREATIVE_EXTRANALBODY_CRAFT_TABLE_BLOCK.get(), new Item.Properties()));
    }
}
