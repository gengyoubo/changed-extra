package github.com.gengyoubo.CE.compat.synergy;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/** Inspect method descriptors without resolving client-only GUI parameter classes on a server. */
public final class SafeNativePetMenu {
    private SafeNativePetMenu() {}
    private static final ClassValue<Boolean> CACHE = new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type) {
            for (Class<?> current = type; current != null && current != ChangedEntity.class; current = current.getSuperclass()) {
                boolean[] found = {false};
                try (var input = current.getResourceAsStream("/" + current.getName().replace('.', '/') + ".class")) {
                    if (input == null) continue;
                    new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            if (name.equals("tamedInteract") && Type.getArgumentTypes(descriptor).length == 2) found[0] = true;
                            return null;
                        }
                    }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                } catch (java.io.IOException exception) {
                    github.com.gengyoubo.CE.changede.LOGGER.warn("Cannot inspect native pet menu for {}", current.getName(), exception);
                }
                if (found[0]) return true;
            }
            return false;
        }
    };
    public static boolean hasMenu(ChangedEntity entity) { return CACHE.get(entity.getClass()); }
}
