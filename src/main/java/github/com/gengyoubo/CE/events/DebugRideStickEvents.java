package github.com.gengyoubo.CE.events;

import github.com.gengyoubo.CE.init.CEItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public final class DebugRideStickEvents {
    private static final String REMOTE_RIDER_UUID_TAG = "RemoteRiderUuid";
    private static final String REMOTE_RIDER_HANDLED_TICK_TAG = "RemoteRiderHandledTick";

    private DebugRideStickEvents() {
    }

    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        handleInteraction(event.getEntity(), event.getItemStack(), event.getTarget(), event);
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        handleInteraction(event.getEntity(), event.getItemStack(), event.getTarget(), event);
    }

    private static void handleInteraction(Player player, ItemStack stack, Entity target, PlayerInteractEvent event) {
        if (player.level().isClientSide || !(target instanceof LivingEntity livingTarget)) {
            return;
        }

        Item item = stack.getItem();
        if (item == CEItem.RIDING_STICK.get()) {
            makePlayerRideTarget(player, livingTarget);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if (item == CEItem.RIDDEN_STICK.get()) {
            makeTargetRidePlayer(player, livingTarget);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if (item == CEItem.REMOTE_RIDING_STICK.get()) {
            handleRemoteRideStickIfNeeded(player, stack, livingTarget);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private static void handleRemoteRideStickIfNeeded(Player player, ItemStack stack, LivingEntity target) {
        CompoundTag tag = stack.getOrCreateTag();
        long gameTime = player.level().getGameTime();
        if (tag.getLong(REMOTE_RIDER_HANDLED_TICK_TAG) == gameTime) {
            return;
        }

        tag.putLong(REMOTE_RIDER_HANDLED_TICK_TAG, gameTime);
        handleRemoteRideStick(player, stack, target);
    }

    private static void handleRemoteRideStick(Player player, ItemStack stack, LivingEntity target) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.hasUUID(REMOTE_RIDER_UUID_TAG)) {
            selectRemoteRider(player, stack, target);
            return;
        }

        Entity rider = ((ServerLevel) player.level()).getEntity(tag.getUUID(REMOTE_RIDER_UUID_TAG));
        if (!(rider instanceof LivingEntity livingRider) || rider.isRemoved()) {
            selectRemoteRider(player, stack, target);
            return;
        }

        if (livingRider == target) {
            player.displayClientMessage(Component.literal("Selected rider: " + target.getName().getString()), true);
            return;
        }

        makeEntityRideEntity(livingRider, target);
        tag.remove(REMOTE_RIDER_UUID_TAG);
        player.displayClientMessage(
                Component.literal(livingRider.getName().getString() + " is now riding " + target.getName().getString()),
                true
        );
    }

    private static void selectRemoteRider(Player player, ItemStack stack, LivingEntity target) {
        stack.getOrCreateTag().putUUID(REMOTE_RIDER_UUID_TAG, target.getUUID());
        player.displayClientMessage(Component.literal("Selected rider: " + target.getName().getString()), true);
    }

    public static void makePlayerRideTarget(Player player, LivingEntity target) {
        makeEntityRideEntity(player, target);
    }

    public static void makeTargetRidePlayer(Player player, LivingEntity target) {
        makeEntityRideEntity(target, player);
    }

    public static void makeEntityRideEntity(LivingEntity rider, Entity vehicle) {
        rider.stopRiding();
        rider.ejectPassengers();
        vehicle.stopRiding();
        vehicle.ejectPassengers();
        if (rider.startRiding(vehicle, true)) {
            vehicle.positionRider(rider);
            syncPassengers(vehicle);
        }
    }

    private static void syncPassengers(Entity vehicle) {
        if (vehicle.level() instanceof ServerLevel serverLevel) {
            ClientboundSetPassengersPacket packet = new ClientboundSetPassengersPacket(vehicle);
            serverLevel.getServer().getPlayerList().broadcast(
                    null,
                    vehicle.getX(),
                    vehicle.getY(),
                    vehicle.getZ(),
                    64.0D,
                    vehicle.level().dimension(),
                    packet
            );
        }
    }
}
