package github.com.gengyoubo.CE.compat.synergy;

import github.com.gengyoubo.CE.changede;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.network.PacketDistributor;
import net.parkabird.changedsynergy.ai.InvoluntaryTransfurNegotiation;
import net.parkabird.changedsynergy.ai.TakeoverService;
import net.parkabird.changedsynergy.network.AbsorptionNegotiationStatePacket;
import net.parkabird.changedsynergy.network.ChangedSynergyNetwork;

import java.util.IdentityHashMap;
import java.util.Map;

/** Retires demonstrably stranded CS captures; Changed remains authoritative. */
public final class ChangedSynergyTransactionRecovery {
    private static final Map<ServerPlayer, SynergyTransactionRecoveryGuard> OBSERVATIONS = new IdentityHashMap<>();

    private ChangedSynergyTransactionRecovery() {}

    public static void initialize() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false,
                TickEvent.PlayerTickEvent.class, ChangedSynergyTransactionRecovery::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false,
                PlayerEvent.PlayerLoggedOutEvent.class, event -> OBSERVATIONS.remove(event.getEntity()));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false,
                PlayerEvent.Clone.class, event -> {
                    OBSERVATIONS.remove(event.getOriginal());
                    OBSERVATIONS.remove(event.getEntity());
                });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false,
                PlayerEvent.PlayerChangedDimensionEvent.class, event -> OBSERVATIONS.remove(event.getEntity()));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false,
                ServerStoppedEvent.class, event -> OBSERVATIONS.clear());
    }

    private static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            OBSERVATIONS.remove(player);
            return;
        }
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(SynergyTransactionRecoveryGuard.TRANSACTION, Tag.TAG_COMPOUND)
                || !SynergyTransactionRecoveryGuard.isUnfinishedCapture(
                        persisted.getCompound(SynergyTransactionRecoveryGuard.TRANSACTION))
                || !isIdleHuman(player, persisted)) {
            OBSERVATIONS.remove(player);
            return;
        }

        var guard = OBSERVATIONS.computeIfAbsent(player, ignored -> new SynergyTransactionRecoveryGuard());
        if (guard.observe(persisted, true, player.server.getTickCount())) {
            OBSERVATIONS.remove(player);
            // Clear the corresponding client UI flag without invoking CS form restoration,
            // source respawning, or its broader abandon/relationship cleanup paths.
            ChangedSynergyNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new AbsorptionNegotiationStatePacket(false));
            changede.LOGGER.info("Removed stranded Changed: Synergy absorption transaction for {}",
                    player.getGameProfile().getName());
        }
    }

    private static boolean isIdleHuman(ServerPlayer player, CompoundTag persisted) {
        if (!player.isAlive() || player.isRemoved()
                || ProcessTransfur.getPlayerTransfurVariant(player) != null
                || ProcessTransfur.getPlayerTransfurProgress(player) != 0.0F
                || TakeoverService.active(player)
                || persisted.contains("SynergyTakeover")) return false;

        // Use the read-only holder query: isReleaseParticipant may repair source UUIDs.
        for (var level : player.server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof ChangedEntity source
                        && InvoluntaryTransfurNegotiation.isReleaseHoldActive(source, player)) return false;
            }
        }
        return true;
    }
}
