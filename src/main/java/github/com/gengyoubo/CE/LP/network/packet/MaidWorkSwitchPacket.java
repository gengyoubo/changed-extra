package github.com.gengyoubo.CE.LP.network.packet;

import github.com.gengyoubo.CE.changede;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

/** Requests a validated switch between the Synergy inventory and the work page. */
public record MaidWorkSwitchPacket(int creatureId, boolean openWork) {
    public static void encode(MaidWorkSwitchPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.creatureId);
        buffer.writeBoolean(packet.openWork);
    }

    public static MaidWorkSwitchPacket decode(FriendlyByteBuf buffer) {
        return new MaidWorkSwitchPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(MaidWorkSwitchPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !ModList.get().isLoaded("changed_synergy")
                    || !ModList.get().isLoaded("touhou_little_maid")) return;
            try {
                Class<?> compat = Class.forName("github.com.gengyoubo.CE.compat.maid.LatexMaidCompat");
                Method switchMenu = compat.getMethod("switchMenu", ServerPlayer.class, int.class, boolean.class);
                switchMenu.invoke(null, player, packet.creatureId, packet.openWork);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                     | InvocationTargetException | LinkageError exception) {
                changede.LOGGER.warn("Could not switch latex maid work menu", exception);
            }
        });
        context.setPacketHandled(true);
    }
}
