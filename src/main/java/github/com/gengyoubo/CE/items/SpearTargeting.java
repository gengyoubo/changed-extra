package github.com.gengyoubo.CE.items;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Clip the allowed attack segment, not the full eye ray's first intersection with the target. */
public final class SpearTargeting {
    public static boolean intersects(AABB bounds, Vec3 start, Vec3 end) {
        AABB box = bounds.inflate(SpearCombatRules.HITBOX_MARGIN);
        return box.contains(start) || box.clip(start, end).isPresent();
    }
    private SpearTargeting() { }
}
