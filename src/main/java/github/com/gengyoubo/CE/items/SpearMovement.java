package github.com.gengyoubo.CE.items;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Map;
import java.util.WeakHashMap;

/** Samples before entity movement, independently of vanilla's per-tick reset of xo/yo/zo. */
@Mod.EventBusSubscriber(modid = "changede")
public final class SpearMovement {
    private record Sample(Vec3 position, Vec3 velocity, long tick, Object dimension) { }
    private static final Map<Entity, Sample> SAMPLES = new WeakHashMap<>();
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        var player = event.player;
        if (event.phase != TickEvent.Phase.START || player.level().isClientSide
                || !(player.getMainHandItem().getItem() instanceof LatexSpearItem
                || player.getOffhandItem().getItem() instanceof LatexSpearItem)) return;
        sample(player.getRootVehicle());
        for (Entity target : player.level().getEntities(player, player.getBoundingBox().inflate(8))) sample(target.getRootVehicle());
    }
    private static void sample(Entity entity) {
        long tick = entity.level().getGameTime();
        Sample previous = SAMPLES.get(entity);
        if (previous != null && previous.tick == tick && previous.dimension == entity.level()) return;
        Vec3 velocity = previous != null && tick - previous.tick == 1 && previous.dimension == entity.level()
                ? entity.position().subtract(previous.position) : Vec3.ZERO;
        if (velocity.lengthSqr() > 16) velocity = Vec3.ZERO; // Teleports are not a charge.
        SAMPLES.put(entity, new Sample(entity.position(), velocity, tick, entity.level()));
    }
    public static Vec3 motion(Entity entity) {
        Entity vehicle = entity.getRootVehicle();
        Sample sample = SAMPLES.get(vehicle);
        return sample != null && sample.dimension == entity.level()
                && entity.level().getGameTime() - sample.tick <= 1 ? sample.velocity : Vec3.ZERO;
    }
}
