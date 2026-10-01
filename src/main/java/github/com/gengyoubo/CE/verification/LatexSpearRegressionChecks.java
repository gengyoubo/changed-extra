package github.com.gengyoubo.CE.verification;

import com.mojang.authlib.GameProfile;
import github.com.gengyoubo.CE.init.CEItem;
import github.com.gengyoubo.CE.items.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import java.util.UUID;

/** Opt-in isolated server check; never runs in normal games. */
@Mod.EventBusSubscriber(modid = "changede")
public final class LatexSpearRegressionChecks {
    private static void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
        LogManager.getLogger("changede-spear-check").info("PASS: {}", message);
    }
    @SubscribeEvent public static void verify(ServerStartedEvent event) {
        if (!Boolean.getBoolean("changede.verifySpear")) return;
        ServerLevel level = event.getServer().overworld();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "SpearTest"));
        LatexSpearItem spear = (LatexSpearItem) CEItem.LATEX_SPEAR.get();
        player.setPos(0, 260, 0);
        player.setYRot(0);
        player.setXRot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(spear));
        spear.getAttributeModifiers(EquipmentSlot.MAINHAND, player.getMainHandItem()).forEach((attribute, modifier) -> {
            var instance = player.getAttribute(attribute);
            if (instance != null) instance.addTransientModifier(modifier);
        });
        try {
            Zombie target = target(level, 3.5);
            float health = target.getHealth();
            spear.jab(player);
            check(Math.abs(health - target.getHealth() - 3.5F) < 0.001, "Far jab deals 3.5 damage");
            health = target.getHealth();
            spear.jab(player);
            check(health == target.getHealth(), "Post-attack cooldown blocks repeated damage");
            target.discard();
            player.getCooldowns().removeCooldown(spear);
            target = target(level, 2.1);
            health = target.getHealth();
            spear.jab(player);
            check(target.getHealth() < health, "Target overlapping the minimum-range segment can be hit");
            target.discard();
            player.getCooldowns().removeCooldown(spear);
            target = target(level, 3.5);
            long originalTime = level.getGameTime();
            var timeData = (net.minecraft.world.level.storage.ServerLevelData) level.getLevelData();
            try {
                timeData.setGameTime(originalTime + 1);
                SpearMovement.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player));
                timeData.setGameTime(originalTime + 2);
                player.setPos(0, 260, 0.3);
                SpearMovement.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, player));
                check(SpearMovement.motion(player).z > 0.29, "Server movement sample records forward motion");
                spear.use(level, player, InteractionHand.MAIN_HAND);
                health = target.getHealth();
                spear.onUseTick(level, player, player.getMainHandItem(), SpearCombatRules.USE_DURATION - SpearCombatRules.WARMUP);
                check(target.getHealth() < health, "Charge deals damage using measured movement");
            } finally { timeData.setGameTime(originalTime); player.stopUsingItem(); target.discard(); }
            for (InteractionHand hand : InteractionHand.values()) {
                for (double distance : new double[]{3.25, 3.75, 4.4}) {
                    chargeThroughPlayerTicks(level, spear, hand, distance, false);
                    chargeThroughPlayerTicks(level, spear, hand, distance, true);
                }
            }
            LogManager.getLogger("changede-spear-check").info("ALL SPEAR SERVER CHECKS PASSED");
        } finally { event.getServer().halt(false); }
    }
    private static void chargeThroughPlayerTicks(ServerLevel level, LatexSpearItem spear,
                                                  InteractionHand hand, double distance, boolean startNearTarget) {
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "ChargeRangeTest"));
        player.setPos(0, 260, 0);
        player.setNoGravity(true);
        player.setYRot(0);
        player.setXRot(0);
        player.setSprinting(true);
        player.setItemInHand(hand, new ItemStack(spear));
        double step = 0.28; // Approximately normal forward sprint speed, in blocks/tick.
        int ticks = SpearCombatRules.WARMUP + 1;
        Zombie target = target(level, distance + (startNearTarget ? 0 : step * ticks));
        float health = target.getHealth();
        long originalTime = level.getGameTime();
        var timeData = (net.minecraft.world.level.storage.ServerLevelData) level.getLevelData();
        try {
            spear.use(level, player, hand);
            for (int tick = 0; tick <= ticks; tick++) {
                timeData.setGameTime(originalTime + tick + 1);
                player.setPos(0, 260, step * tick);
                player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                // Exercises Forge PlayerTickEvent -> real LivingEntity use tick, rather than
                // manually calling onUseTick with a prepared movement sample.
                player.doTick();
            }
            if (startNearTarget) {
                check(target.getHealth() == health, "Starting charge at " + distance + " blocks enters minimum-range blind spot during warmup using " + hand);
            } else {
                check(target.getHealth() < health, "Sprint charge through player ticks hits " + distance + " blocks using " + hand);
            }
        } finally {
            timeData.setGameTime(originalTime);
            player.stopUsingItem();
            target.discard();
        }
    }
    private static Zombie target(ServerLevel level, double z) {
        Zombie zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) throw new AssertionError("Zombie creation failed");
        zombie.setNoAi(true);
        zombie.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);
        zombie.setPos(0, 260, z);
        level.addFreshEntity(zombie);
        return zombie;
    }
}
