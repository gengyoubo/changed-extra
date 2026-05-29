package github.com.gengyoubo.CE.init;

import github.com.gengyoubo.CE.items.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CEItem {
    private static final String PROJECT_E_MODID = "projecte";
    private static final String PTOTEM_CLASS = "github.com.gengyoubo.CE.projectextended.PTotemOfUndying";

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

    private static boolean hasProjectE() {
        return ModList.get().isLoaded(PROJECT_E_MODID);
    }

    private static Item resolveProjecteTotem(String fieldName) {
        if (!hasProjectE()) {
            return Items.AIR;
        }

        try {
            Class<?> pTotemClass = Class.forName(PTOTEM_CLASS);
            Object registryObject = pTotemClass.getField(fieldName).get(null);
            Object item = registryObject.getClass().getMethod("get").invoke(registryObject);
            if (item instanceof Item resolvedItem) {
                return resolvedItem;
            }
        } catch (Throwable ignored) {
            // Optional dependency unavailable or class init failed; use safe fallback.
        }

        return Items.TOTEM_OF_UNDYING;
    }

    private static Item getSafeProjecteTotem(String fieldName) {
        Item item = resolveProjecteTotem(fieldName);
        return item == Items.AIR ? Items.TOTEM_OF_UNDYING : item;
    }

    private static void safeAccept(CreativeModeTab.Output output, Item item) {
        if (item != Items.AIR) {
            output.accept(item);
        }
    }
}
