package github.com.gengyoubo.CE.compat.synergy.camp;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;

final class CampNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.parse("changede:latex_camp"),
            () -> "1", "1"::equals, "1"::equals);
    record Action(int menu, String action, UUID target, int index) {}
    record Snapshot(int menu, CompoundTag view) {}
    record Visibility(int entity, UUID id, boolean hidden) {}
    static void register() {
        CHANNEL.registerMessage(0, Action.class,
                (p, buf) -> { buf.writeVarInt(p.menu); buf.writeUtf(p.action, 32); buf.writeBoolean(p.target != null); if (p.target != null) buf.writeUUID(p.target); buf.writeVarInt(p.index); },
                buf -> new Action(buf.readVarInt(), buf.readUtf(32), buf.readBoolean() ? buf.readUUID() : null, buf.readVarInt()),
                (p, context) -> {
                    var ctx = context.get(); ctx.enqueueWork(() -> {
                        ServerPlayer player = ctx.getSender();
                        if (player != null && player.containerMenu instanceof LatexCampfireMenu menu && menu.containerId == p.menu)
                            menu.action(player, p.action, p.target, p.index);
                    }); ctx.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, Snapshot.class,
                (p, buf) -> { buf.writeVarInt(p.menu); buf.writeNbt(p.view); },
                buf -> new Snapshot(buf.readVarInt(), buf.readNbt()),
                (p, context) -> {
                    var ctx = context.get(); ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CampClient.snapshot(p)));
                    ctx.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2, Visibility.class,
                (p, buf) -> { buf.writeVarInt(p.entity); buf.writeUUID(p.id); buf.writeBoolean(p.hidden); },
                buf -> new Visibility(buf.readVarInt(), buf.readUUID(), buf.readBoolean()),
                (p, context) -> {
                    var ctx = context.get(); ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CampClient.visibility(p)));
                    ctx.setPacketHandled(true);
                }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    static void sync(ServerPlayer player, int menu, CompoundTag view) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Snapshot(menu, view)); }
    static void action(int menu, String action, UUID target, int index) { CHANNEL.sendToServer(new Action(menu, action, target, index)); }
    static void visibility(net.ltxprogrammer.changed.entity.ChangedEntity mob, boolean hidden) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> mob), new Visibility(mob.getId(), mob.getUUID(), hidden));
    }
    static void visibility(ServerPlayer player, net.ltxprogrammer.changed.entity.ChangedEntity mob, boolean hidden) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Visibility(mob.getId(), mob.getUUID(), hidden));
    }
}
