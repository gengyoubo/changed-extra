package github.com.gengyoubo.CE.events;

import net.ltxprogrammer.changed.entity.variant.TransfurVariantInstance;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = "changede")
public final class LatexSpearAdvancements {
    private static final ResourceLocation AERIAL_ASSASSIN =
            ResourceLocation.fromNamespaceAndPath("changede", "aerial_assassin");
    private static final ResourceLocation EXCUSE_ME =
            ResourceLocation.fromNamespaceAndPath("changede", "excuse_me");
    private static final Map<ServerPlayer, Charge> CHARGES = new WeakHashMap<>();

    private record Charge(TransfurVariantInstance<?> form, ItemStack stack,
                          ResourceKey<Level> dimension, Set<UUID> victims) {
        boolean isActive(ServerPlayer player) {
            return player.isAlive() && !player.isSpectator() && player.isUsingItem()
                    && player.getUseItem() == stack && !stack.isEmpty()
                    && player.level().dimension().equals(dimension)
                    && flyingForm(player) == form;
        }
    }

    private LatexSpearAdvancements() { }

    private static TransfurVariantInstance<?> flyingForm(Player player) {
        var form = ProcessTransfur.getPlayerTransfurVariant(player);
        // Test the form's capability, not creative flight or equipment granted to a human.
        return form != null && (form.canElytraGlide() || form.canCreativeFly()) ? form : null;
    }

    public static void beginCharge(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        CHARGES.remove(serverPlayer);
        var form = flyingForm(player);
        if (form != null) {
            CHARGES.put(serverPlayer, new Charge(form, stack, player.level().dimension(), new HashSet<>()));
        }
    }

    public static void endCharge(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) CHARGES.remove(player);
    }

    public static void onHit(Player attacker, LivingEntity target, ItemStack stack, boolean wasAtFullHealth) {
        if (!(attacker instanceof ServerPlayer player) || !(target instanceof Mob)
                || !target.isDeadOrDying() || flyingForm(player) == null) return;
        if (wasAtFullHealth) AdvancementChainEvents.awardAdvancement(player, AERIAL_ASSASSIN);

        Charge charge = CHARGES.get(player);
        if (charge == null) return;
        if (!charge.isActive(player)) {
            CHARGES.remove(player);
            return;
        }
        if (charge.stack() == stack && charge.victims().add(target.getUUID()) && charge.victims().size() == 10) {
            AdvancementChainEvents.awardAdvancement(player, EXCUSE_ME);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        Charge charge = CHARGES.get(player);
        if (charge != null && !charge.isActive(player)) CHARGES.remove(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        endCharge(event.getEntity());
    }
}
