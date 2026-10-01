package github.com.gengyoubo.CE.diagnostics;

import org.spongepowered.asm.mixin.extensibility.*;
import org.spongepowered.asm.mixin.injection.selectors.ISelectorContext;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.throwables.MixinApplyError;
import org.spongepowered.asm.mixin.transformer.throwables.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Function;

public class ChangedMixinDiagnosticsTest {
    private static final String TARGET = "net.ltxprogrammer.changed.block.entity.PurifierBlockEntity";
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    @SuppressWarnings("unchecked") private static <T> T proxy(Class<T> type, Function<String, Object> values) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (p, m, a) -> values.apply(m.getName()));
    }
    private static IMixinInfo info(String target) {
        IMixinConfig config = proxy(IMixinConfig.class, name -> name.equals("getName") ? "mixins/changede-lp.mixins.json" : null);
        return proxy(IMixinInfo.class, name -> switch (name) {
            case "getConfig" -> config;
            case "getClassName" -> "github.com.gengyoubo.CE.LP.mixins.AddonMekanismEnergyMixin";
            case "getName" -> "AddonMekanismEnergyMixin";
            case "getTargetClasses" -> List.of(target, "net.foxyas.changedaddon.block.entity.UnifuserBlockEntity");
            default -> null;
        });
    }
    private static InvalidInjectionException failure(String target, String message) {
        IMixinInfo mixin = info(target);
        IMixinContext context = proxy(IMixinContext.class, name -> switch (name) {
            case "getMixin" -> mixin;
            case "getTargetClassRef" -> target.replace('.', '/');
            default -> null;
        });
        ISelectorContext selector = proxy(ISelectorContext.class, name -> name.equals("getMixin") ? context : null);
        return new InvalidInjectionException(selector, message);
    }
    public static void main(String[] args) {
        var ce = new ChangedMixinDiagnostics.Source("changede", "Changed Extra");
        var injection = failure(TARGET, "@Inject changede$exposeStrictEnergy could not find target getCapability");
        Throwable error = new MixinTransformerError(new MixinApplyError(injection));
        String message = injection.getMessage();
        StackTraceElement[] stack = injection.getStackTrace().clone();
        var result = ChangedMixinDiagnostics.inspect(error, "changed", (config, mixin) -> Optional.of(ce)).orElseThrow();
        check(result.target().equals(TARGET));
        check(result.config().equals("mixins/changede-lp.mixins.json"));
        check(result.mixin().endsWith("AddonMekanismEnergyMixin"));
        check(result.source().equals(ce));
        check(result.rootType().equals(InvalidInjectionException.class.getName()));
        check(result.rootMessage().equals(message));
        check(result.report().contains("Affected Mod: Changed (changed)"));
        check(result.distinction().contains("belongs to Changed Extra (changede)"));
        check(result.ui(true).contains("受影响模组"));
        check(result.ui(false).contains("Affected mod"));
        check(error.getCause().getCause() == injection && injection.getMessage().equals(message));
        check(Arrays.equals(injection.getStackTrace(), stack));
        check(ChangedMixinDiagnostics.inspect(error, "changed", (c,m) -> Optional.empty()).orElseThrow().source() == null);
        check(ChangedMixinDiagnostics.inspect(error, "changed", (c,m) -> { throw new LinkageError(); }).orElseThrow().source() == null);
        check(ChangedMixinDiagnostics.inspect(error, "changed", (c,m) -> Optional.of(new ChangedMixinDiagnostics.Source("changed", "Changed")))
                .orElseThrow().distinction().contains("owns this failing Mixin"));
        check(ChangedMixinDiagnostics.inspect(error, "changede", (c,m) -> Optional.of(ce)).isEmpty());
        check(ChangedMixinDiagnostics.inspect(new RuntimeException("MixinApplyError changede"), "changed", (c,m) -> Optional.of(ce)).isEmpty());
        check(ChangedMixinDiagnostics.inspect(failure("other.mod.Target", "Invalid injection"), "changed", (c,m) -> Optional.of(ce)).isEmpty());
        check(ChangedMixinDiagnostics.inspect(null, "changed", (c,m) -> Optional.of(ce)).isEmpty());
        var handler = new ChangedMixinErrorHandler();
        for (var action : IMixinErrorHandler.ErrorAction.values()) {
            check(handler.onApplyError(TARGET, injection, info(TARGET), action) == action);
            check(handler.onPrepareError(null, injection, info(TARGET), action) == action);
        }
        var cycleA = new RuntimeException("cycle A");
        var cycleB = new RuntimeException("cycle B");
        cycleA.initCause(cycleB);
        cycleB.initCause(cycleA);
        var cycle = new InvalidMixinException(info(TARGET), "outer", cycleA);
        ChangedMixinDiagnostics.rememberTarget(cycle, TARGET);
        check(ChangedMixinDiagnostics.inspect(cycle, "changed", (c,m) -> Optional.of(ce)).isPresent());
        Throwable suppressed = new RuntimeException("outer");
        suppressed.addSuppressed(injection);
        check(ChangedMixinDiagnostics.inspect(suppressed, "changed", (c,m) -> Optional.of(ce)).isPresent());
        var nested = failure(TARGET, "message claiming another owner is not attribution evidence");
        nested.initCause(new IllegalArgumentException("Bad descriptor"));
        check(ChangedMixinDiagnostics.inspect(nested, "changed", (c,m) -> Optional.of(ce)).orElseThrow().rootType()
                .equals(IllegalArgumentException.class.getName()));
        System.out.println("Changed Mixin diagnostics passed: structured ownership, exact target, root cause, unchanged exceptions/actions, unknown sources and bounded cyclic chains.");
    }
}
