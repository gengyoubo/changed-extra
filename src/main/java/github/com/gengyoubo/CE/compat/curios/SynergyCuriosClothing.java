package github.com.gengyoubo.CE.compat.curios;

import java.nio.file.Files;
import net.minecraftforge.fml.loading.LoadingModList;

/**
 * Changed: Synergy 1.1.5+ stores Changed accessories in its own player Curios slots
 * ({@code changed_clothing_*}). Installing our slots as well would show every body part twice,
 * so the Curios accessory compat stays off whenever that integration is present.
 */
public final class SynergyCuriosClothing {
    private static final String MARKER = "net/parkabird/changedsynergy/compat/curios/NativeClothingInventory.class";
    private static Boolean present;

    public static boolean present() {
        if (present == null) present = detect();
        return present;
    }

    private static boolean detect() {
        try {
            var file = LoadingModList.get().getModFileById("changed_synergy");
            return file != null && Files.exists(file.getFile().findResource(MARKER));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private SynergyCuriosClothing() {}
}
