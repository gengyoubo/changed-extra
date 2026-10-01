package github.com.gengyoubo.CE.diagnostics;

import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.throwables.MixinApplyError;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.mixin.transformer.throwables.MixinTransformerError;
import java.util.*;

/** No game state, class loading, exception rewriting or exception-message attribution. */
public final class ChangedMixinDiagnostics {
    public record Source(String id, String name) {
        public String display() { return name + " (" + id + ")"; }
    }
    @FunctionalInterface public interface OwnerLookup { Optional<Source> find(String config, String mixinClass); }
    public record Diagnostic(String target, String mixin, String config, Source source, String rootType, String rootMessage) {
        public String owner() { return source == null ? "Unknown (ownership could not be established)" : source.display(); }
        public String distinction() {
            if (source == null) return "Changed is the affected/loading mod. The failing transformer's owner is unknown.";
            if (source.id().equals("changed")) return "Changed is the affected/loading mod and owns this failing Mixin.";
            return "Changed is the affected/loading mod. The failing Mixin belongs to " + source.display() + ".";
        }
        public String report() {
            return "Affected Mod: Changed (changed)\nFailure occurred while transforming: " + target
                    + "\nFailing Mixin: " + mixin + "\nMixin Config: " + config + "\nLikely Source Mod: " + owner()
                    + "\nRoot Cause: " + rootType + ": " + rootMessage + "\nAttribution: " + distinction();
        }
        public String ui(boolean chinese) {
            String root = rootType.substring(rootType.lastIndexOf('.') + 1) + ": " + rootMessage.replace('\n', ' ').replace('\r', ' ');
            if (root.length() > 700) root = root.substring(0, 700) + "…";
            return chinese ? "\n\nChanged Mixin 诊断（补充信息）\n受影响模组：Changed (changed)\n失败 Mixin 来源：" + owner()
                    + "\nMixin：" + mixin + "\n目标类：" + target + "\n根因：" + root
                    + "\n" + (source == null ? "来源未能确认，请同时查看原始崩溃报告。" : source.id().equals("changed")
                    ? "此次失败的 Mixin 属于 Changed。" : "Changed 是加载失败的模组；失败的 Mixin 属于 " + source.display() + "。")
                    : "\n\nChanged Mixin Diagnostics (additional information)\nAffected mod: Changed (changed)\nFailing Mixin source: " + owner()
                    + "\nMixin: " + mixin + "\nTarget class: " + target + "\nRoot cause: " + root + "\n" + distinction();
        }
    }

    private static final Map<Throwable, String> TARGETS = Collections.synchronizedMap(new WeakHashMap<>());
    private ChangedMixinDiagnostics() { }
    public static void rememberTarget(Throwable error, String target) {
        try {
            synchronized (TARGETS) {
                if (TARGETS.size() >= 128) TARGETS.clear();
                TARGETS.put(error, target.replace('/', '.'));
            }
        } catch (Throwable ignored) { }
    }

    public static Optional<Diagnostic> inspect(Throwable error, String affectedMod, OwnerLookup owners) {
        try { return inspectSafely(error, affectedMod, owners); }
        catch (Throwable ignored) { return Optional.empty(); }
    }

    private static Optional<Diagnostic> inspectSafely(Throwable error, String affectedMod, OwnerLookup owners) {
        List<Throwable> chain = graph(error);
        if (chain.stream().noneMatch(t -> t instanceof InvalidMixinException || t instanceof MixinApplyError || t instanceof MixinTransformerError))
            return Optional.empty();
        InvalidMixinException failure = chain.stream().filter(InvalidInjectionException.class::isInstance)
                .map(InvalidMixinException.class::cast).findFirst().orElseGet(() -> chain.stream().filter(InvalidMixinException.class::isInstance)
                        .map(InvalidMixinException.class::cast).findFirst().orElse(null));
        IMixinInfo info = failure == null ? null : failure.getMixin();
        String target = chain.stream().map(TARGETS::get).filter(Objects::nonNull).findFirst().orElse("");
        if (target.isEmpty() && failure instanceof InvalidInjectionException injection && injection.getContext() != null
                && injection.getContext().getMixin() != null) target = injection.getContext().getMixin().getTargetClassRef().replace('/', '.');
        List<String> candidates = info == null ? List.of() : info.getTargetClasses().stream().map(t -> t.replace('/', '.')).toList();
        if (target.isEmpty() && candidates.size() == 1) target = candidates.get(0);
        boolean touchesChanged = target.startsWith("net.ltxprogrammer.changed.")
                || candidates.stream().anyMatch(t -> t.startsWith("net.ltxprogrammer.changed."))
                || chain.stream().flatMap(t -> Arrays.stream(t.getStackTrace())).anyMatch(s -> s.getClassName().startsWith("net.ltxprogrammer.changed."));
        if (!affectedMod.equals("changed") || !touchesChanged) return Optional.empty();
        String config = info == null ? "Unknown" : info.getConfig().getName();
        String mixin = info == null ? "Unknown" : info.getClassName();
        Source source = null;
        if (info != null) try { source = owners.find(config, mixin).orElse(null); } catch (Throwable ignored) { }
        Throwable root = deepest(failure == null ? error : failure);
        if (target.isEmpty()) target = "Unknown (declared targets: " + String.join(", ", candidates) + ")";
        return Optional.of(new Diagnostic(target, mixin, config, source, root.getClass().getName(),
                Objects.toString(root.getMessage(), "No exception message")));
    }

    private static List<Throwable> graph(Throwable error) {
        List<Throwable> result = new ArrayList<>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<Throwable> pending = new ArrayDeque<>();
        if (error != null) pending.add(error);
        while (!pending.isEmpty() && seen.size() < 256) {
            Throwable current = pending.removeFirst();
            if (!seen.add(current)) continue;
            result.add(current);
            if (current.getCause() != null) pending.addFirst(current.getCause());
            for (Throwable suppressed : current.getSuppressed()) if (pending.size() < 256) pending.addLast(suppressed);
        }
        return result;
    }
    private static Throwable deepest(Throwable error) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = error;
        seen.add(current);
        while (seen.size() < 256 && current.getCause() != null && seen.add(current.getCause())) current = current.getCause();
        return current;
    }
}
