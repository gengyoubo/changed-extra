package github.com.gengyoubo.CE.diagnostics.mixins;

import github.com.gengyoubo.CE.diagnostics.ChangedCrashReports;
import net.minecraftforge.client.gui.LoadingErrorScreen;
import net.minecraftforge.fml.ModLoadingException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = LoadingErrorScreen.LoadingEntryList.class, remap = false)
public abstract class ForgeChangedLoadingErrorMixin {
    // Match call sites rather than compiler-generated lambda numbers, including row sizing.
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/ModLoadingException;formatToString()Ljava/lang/String;"), require = 0)
    private static String changede$displayDiagnostics(ModLoadingException error) { return changede$message(error); }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/ModLoadingException;getMessage()Ljava/lang/String;"), require = 0)
    private static String changede$sizeDiagnostics(ModLoadingException error) { return changede$message(error); }

    private static String changede$message(ModLoadingException error) {
        String original = error.formatToString();
        try {
            return ChangedCrashReports.ui(error, original);
        } catch (Throwable ignored) { return original; }
    }
}
