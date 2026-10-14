package github.com.gengyoubo.CE.compat.entityculling;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/** Links to methods added to entities by either EntityCulling API, without linking its package. */
public final class CullableBridge {
    private static final ClassValue<Access> ACCESS = new ClassValue<>() {
        @Override
        protected Access computeValue(Class<?> type) {
            try {
                var lookup = MethodHandles.publicLookup();
                return new Access(
                        lookup.unreflect(type.getMethod("isForcedVisible"))
                                .asType(MethodType.methodType(boolean.class, Object.class)),
                        lookup.unreflect(type.getMethod("isCulled"))
                                .asType(MethodType.methodType(boolean.class, Object.class)),
                        lookup.unreflect(type.getMethod("setOutOfCamera", boolean.class))
                                .asType(MethodType.methodType(void.class, Object.class, boolean.class))
                );
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Cannot link EntityCulling methods on " + type.getName(), exception);
            }
        }
    };

    private CullableBridge() {}

    public static boolean isForcedVisible(Object entity) {
        try {
            return (boolean) ACCESS.get(entity.getClass()).isForcedVisible.invokeExact(entity);
        } catch (Throwable exception) {
            throw propagate(exception);
        }
    }

    public static boolean isCulled(Object entity) {
        try {
            return (boolean) ACCESS.get(entity.getClass()).isCulled.invokeExact(entity);
        } catch (Throwable exception) {
            throw propagate(exception);
        }
    }

    public static void setOutOfCamera(Object entity, boolean value) {
        try {
            ACCESS.get(entity.getClass()).setOutOfCamera.invokeExact(entity, value);
        } catch (Throwable exception) {
            throw propagate(exception);
        }
    }

    private static RuntimeException propagate(Throwable exception) {
        if (exception instanceof Error error) throw error;
        if (exception instanceof RuntimeException runtime) return runtime;
        return new IllegalStateException("EntityCulling method invocation failed", exception);
    }

    private record Access(MethodHandle isForcedVisible, MethodHandle isCulled, MethodHandle setOutOfCamera) {}
}
