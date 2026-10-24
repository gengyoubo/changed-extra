package github.com.gengyoubo.CE.events;

import github.com.gengyoubo.CE.LP.item.MimicSirenTailItem;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingBreatheEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "changede")
public final class MimicSirenTailEvents {
    private static final UUID SWIM_MODIFIER = UUID.fromString("c01d7435-9b60-40f7-9d23-a43c503868dc");
    private static final String GRAVITY_TAG = "changede_mimic_siren_tail_no_gravity";

    private MimicSirenTailEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        boolean wearing = player.isAlive() && MimicSirenTailItem.isWornBy(player);
        if (!player.level().isClientSide) {
            AttributeInstance swim = player.getAttribute(ForgeMod.SWIM_SPEED.get());
            if (swim != null) {
                // Supply only the missing speed, so an existing aquatic form is not boosted twice.
                double bonus = wearing ? Math.max(0.0D, MimicSirenTailItem.getSirenSwimSpeed() - swim.getBaseValue()) : 0.0D;
                AttributeModifier previous = swim.getModifier(SWIM_MODIFIER);
                if (previous != null && Double.compare(previous.getAmount(), bonus) != 0) {
                    swim.removeModifier(SWIM_MODIFIER);
                    previous = null;
                }
                if (bonus > 0.0D && previous == null) {
                    swim.addTransientModifier(new AttributeModifier(SWIM_MODIFIER, "Mimic siren swimming", bonus, AttributeModifier.Operation.ADDITION));
                }
            }
        }

        boolean swimming = wearing && MimicSirenTailItem.shouldSwimUnderwater(player);
        boolean ownsGravity = player.getPersistentData().getBoolean(GRAVITY_TAG);
        if (swimming && !player.isNoGravity()) {
            player.setNoGravity(true);
            player.getPersistentData().putBoolean(GRAVITY_TAG, true);
        } else if (!swimming && ownsGravity) {
            player.setNoGravity(false);
            player.getPersistentData().remove(GRAVITY_TAG);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreathe(LivingBreatheEvent event) {
        if (MimicSirenTailItem.isWornBy(event.getEntity()) && event.getEntity().isEyeInFluidType(ForgeMod.WATER_TYPE.get())) {
            event.setCanBreathe(true);
            event.setCanRefillAir(true);
        }
    }
}
