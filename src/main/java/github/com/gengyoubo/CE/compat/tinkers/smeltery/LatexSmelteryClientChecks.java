package github.com.gengyoubo.CE.compat.tinkers.smeltery;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;

import java.nio.file.Files;
import java.nio.file.Path;

/** Opt-in resource/mixin smoke check; exits at the title screen without loading a save. */
public final class LatexSmelteryClientChecks {
    private static boolean checked;
    private LatexSmelteryClientChecks() {}
    public static void tick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (checked || event.phase != TickEvent.Phase.END || !(minecraft.screen instanceof TitleScreen)) return;
        checked = true;
        try {
            Class.forName("slimeknights.tconstruct.smeltery.client.screen.module.GuiFuelModule");
            for (String name : new String[]{"latex_combustion_core", "latex_fuel_tank"}) {
                var model = minecraft.getModelManager().getModel(new ModelResourceLocation(ResourceLocation.parse("changede:" + name), "inventory"));
                if (model == minecraft.getModelManager().getMissingModel()) throw new AssertionError("Missing item model: " + name);
                if (Component.translatable("block.changede." + name).getString().equals("block.changede." + name)) throw new AssertionError("Missing translation: " + name);
            }
            if (minecraft.getBlockEntityRenderDispatcher().getRenderer(new LatexFuelTankBlockEntity(BlockPos.ZERO, LatexSmelteryCompat.TANK.get().defaultBlockState())) == null)
                throw new AssertionError("Tank fluid renderer not registered");
            Files.writeString(Path.of("latex-smeltery-client-checks.txt"), "PASS: native fuel GUI mixin transforms; both item models bake; translations and tank fluid renderer register\n");
        } catch (Throwable error) {
            github.com.gengyoubo.CE.changede.LOGGER.error("LATEX SMELTERY CLIENT CHECKS FAILED", error);
            try { Files.writeString(Path.of("latex-smeltery-client-checks.txt"), "FAIL: " + error + "\n"); } catch (Exception ignored) {}
        } finally { minecraft.stop(); }
    }
}
