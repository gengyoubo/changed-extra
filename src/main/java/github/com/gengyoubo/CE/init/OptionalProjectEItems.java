package github.com.gengyoubo.CE.init;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;

final class OptionalProjectEItems {
    private static final String PROJECT_E_MODID = "projecte";
    private static final String PTOTEM_CLASS = "github.com.gengyoubo.CE.projectextended.PTotemOfUndying";

    private OptionalProjectEItems() {
    }

    static Item resolveTotem(String fieldName) {
        if (!ModList.get().isLoaded(PROJECT_E_MODID)) {
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
}
