package github.com.gengyoubo.CE.diagnostics;

import org.spongepowered.asm.mixin.extensibility.*;

/** Observe the exact target supplied by Mixin; never change ERROR/WARN/NONE decisions. */
public final class ChangedMixinErrorHandler implements IMixinErrorHandler {
    @Override public ErrorAction onPrepareError(IMixinConfig config, Throwable error, IMixinInfo mixin, ErrorAction action) { return action; }
    @Override public ErrorAction onApplyError(String targetClassName, Throwable error, IMixinInfo mixin, ErrorAction action) {
        ChangedMixinDiagnostics.rememberTarget(error, targetClassName);
        return action;
    }
}
