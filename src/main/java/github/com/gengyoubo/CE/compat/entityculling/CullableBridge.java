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

    /** NMSCullingHelper moved to the util package in newer EntityCulling; look it up in either place. */
    private static final String[] HELPERS = {
            "dev.tr7zw.entityculling.util.NMSCullingHelper",
            "dev.tr7zw.entityculling.NMSCullingHelper"
    };
    private static final MethodHandle IGNORES_CULLING = helper("ignoresCulling",
            MethodType.methodType(boolean.class, net.minecraft.world.entity.Entity.class));
    private static final MethodHandle GET_RENDER_OFFSET = helper("getRenderOffset",
            MethodType.methodType(net.minecraft.world.phys.Vec3.class, net.minecraft.client.renderer.entity.EntityRenderer.class,
                    net.minecraft.world.entity.Entity.class, float.class));

    private static MethodHandle helper(String name, MethodType type) {
        for (String className : HELPERS) {
            try {
                Class<?> helper = Class.forName(className, false, CullableBridge.class.getClassLoader());
                return MethodHandles.publicLookup().findStatic(helper, name, type);
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Try the next location.
            }
        }
        return null;
    }

    private CullableBridge() {}

    /** Replaces Changed's link to {@code NMSCullingHelper.ignoresCulling}. */
    public static boolean ignoresCulling(net.minecraft.world.entity.Entity entity) {
        if (IGNORES_CULLING == null) return false;
        try {
            return (boolean) IGNORES_CULLING.invokeExact(entity);
        } catch (Throwable exception) {
            throw propagate(exception);
        }
    }

    /** Replaces Changed's link to {@code NMSCullingHelper.getRenderOffset}; vanilla offset if the helper is gone. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static net.minecraft.world.phys.Vec3 getRenderOffset(net.minecraft.client.renderer.entity.EntityRenderer renderer,
                                                               net.minecraft.world.entity.Entity entity, float partialTick) {
        if (GET_RENDER_OFFSET == null) return renderer.getRenderOffset(entity, partialTick);
        try {
            return (net.minecraft.world.phys.Vec3) GET_RENDER_OFFSET.invokeExact(renderer, entity, partialTick);
        } catch (Throwable exception) {
            throw propagate(exception);
        }
    }

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
