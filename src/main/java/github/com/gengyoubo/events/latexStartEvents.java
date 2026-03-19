package github.com.gengyoubo.events;

import github.com.gengyoubo.CERegister;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.init.ChangedRegistry;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class latexStartEvents {
    public static final List<TransfurVariant<?>> FORM_VARIANTS = new ArrayList<>();
    public static boolean isLatexStart(Level level) {
        return level.getGameRules().getBoolean(CERegister.LATEX_START);
    }
    public static void setup(final FMLCommonSetupEvent event) {
        System.out.println("start program!");
        event.enqueueWork(() -> {
            var registry = ChangedRegistry.TRANSFUR_VARIANT.get();

            FORM_VARIANTS.clear();

            for (TransfurVariant<?> variant : registry.getValues()) {
                var id = variant.getFormId();

                if (id == null) continue;

                String path = id.getPath();

                // ✔ 只要 form_
                if (!path.startsWith("form_")) continue;

                // ❌ 黑名单过滤
                if (BLACKLIST.contains(path)) {
                    System.out.println("黑名单跳过: " + id);
                    continue;
                }

                FORM_VARIANTS.add(variant);
                System.out.println("加入: " + id);
            }

            System.out.println("最终数量: " + FORM_VARIANTS.size());
        });
    }
    public static TransfurVariant<?> getRandomForm(Random random) {
        if (FORM_VARIANTS.isEmpty()) return null;
        return FORM_VARIANTS.get(random.nextInt(FORM_VARIANTS.size()));
    }
    //黑名单
    public static final Set<String> BLACKLIST = Set.of(
            "form_special"
    );
}

