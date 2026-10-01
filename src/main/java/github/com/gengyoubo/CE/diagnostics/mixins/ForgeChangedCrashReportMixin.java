package github.com.gengyoubo.CE.diagnostics.mixins;

import github.com.gengyoubo.CE.diagnostics.ChangedCrashReports;
import net.minecraft.CrashReport;
import net.minecraftforge.fml.LoadingFailedException;
import net.minecraftforge.logging.CrashReportExtender;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.io.File;

@Mixin(value = CrashReportExtender.class, remap = false)
public abstract class ForgeChangedCrashReportMixin {
    @ModifyVariable(method = "dumpModLoadingCrashReport", at = @At("STORE"), ordinal = 0, require = 0)
    private static CrashReport changede$appendDiagnostics(CrashReport report, Logger logger, LoadingFailedException error, File directory) {
        ChangedCrashReports.append(report, error);
        return report;
    }
}
