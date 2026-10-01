package github.com.gengyoubo.CE.diagnostics.mixins;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

@Mixin(CrashReport.class)
public interface CrashReportDetailsAccessor {
    @Accessor("details")
    List<CrashReportCategory> changede$getDiagnosticCategories();
}
