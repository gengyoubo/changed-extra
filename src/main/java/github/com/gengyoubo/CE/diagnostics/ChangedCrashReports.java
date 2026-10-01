package github.com.gengyoubo.CE.diagnostics;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import github.com.gengyoubo.CE.diagnostics.mixins.CrashReportDetailsAccessor;
import net.minecraftforge.fml.LoadingFailedException;
import net.minecraftforge.fml.ModLoadingException;
import net.minecraftforge.fml.loading.LoadingModList;
import java.nio.file.Files;
import java.util.*;

/** Works before CE's mod constructor; ownership requires config AND class in the same mod file. */
public final class ChangedCrashReports {
    private ChangedCrashReports() { }
    private static Optional<ChangedMixinDiagnostics.Source> owner(String config, String mixinClass) {
        if (config.contains("..") || config.startsWith("/") || config.contains(":")) return Optional.empty();
        var loading = LoadingModList.get();
        if (loading == null) return Optional.empty();
        List<ChangedMixinDiagnostics.Source> matches = new ArrayList<>();
        for (var file : loading.getModFiles()) {
            if (!Files.isRegularFile(file.getFile().findResource(config.split("/")))
                    || !Files.isRegularFile(file.getFile().findResource((mixinClass.replace('.', '/') + ".class").split("/")))) continue;
            for (var mod : file.getMods()) matches.add(new ChangedMixinDiagnostics.Source(mod.getModId(), mod.getDisplayName()));
        }
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    public static Optional<ChangedMixinDiagnostics.Diagnostic> inspect(ModLoadingException error) {
        try {
            String affected = error.getModInfo() == null ? "" : error.getModInfo().getModId();
            return ChangedMixinDiagnostics.inspect(error, affected, ChangedCrashReports::owner);
        } catch (Throwable ignored) { return Optional.empty(); }
    }

    public static void append(CrashReport report, LoadingFailedException failure) {
        try {
            Set<String> added = new HashSet<>();
            for (var error : failure.getErrors()) inspect(error).ifPresent(diagnostic -> {
                if (added.add(diagnostic.report())) {
                    // addCategory also changes the report's Head/stack tracking; append data only.
                    var category = new CrashReportCategory("Changed Mixin Diagnostics");
                    category.setDetail("Affected Mod", "Changed (changed)");
                    category.setDetail("Failure occurred while transforming", diagnostic.target());
                    category.setDetail("Failing Mixin", diagnostic.mixin());
                    category.setDetail("Mixin Config", diagnostic.config());
                    category.setDetail("Likely Source Mod", diagnostic.owner());
                    category.setDetail("Root Cause", diagnostic.rootType() + ": " + diagnostic.rootMessage());
                    category.setDetail("Attribution", diagnostic.distinction());
                    ((CrashReportDetailsAccessor) report).changede$getDiagnosticCategories().add(category);
                }
            });
        } catch (Throwable ignored) { /* The original Forge report must still be saved. */ }
    }
    public static String ui(ModLoadingException error, String original) {
        try { return inspect(error).map(d -> original + d.ui()).orElse(original); }
        catch (Throwable ignored) { return original; }
    }
}
