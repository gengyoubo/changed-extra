package github.com.gengyoubo.CE.init;

import github.com.gengyoubo.CE.items.*;
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
