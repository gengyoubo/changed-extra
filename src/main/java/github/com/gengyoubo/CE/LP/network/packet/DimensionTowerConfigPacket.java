package github.com.gengyoubo.CE.LP.network.packet;

import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.DimensionSpaceTowerBlockEntity;
import github.com.gengyoubo.CE.LP.energy.DimensionTowerChannels;
import github.com.gengyoubo.CE.LP.world.Menu.DimensionSpaceTowerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;
@SuppressWarnings("deprecation")

public record DimensionTowerConfigPacket(BlockPos pos, int action, int value) {
    public static final int SET_CHANNEL = 0;
    public static final int TOGGLE_ENABLED = 1;
    public static final int CYCLE_TYPE = 2;
    public static final int CYCLE_MODE = 3;
    public static final int CYCLE_REDSTONE = 4;

    public static void encode(DimensionTowerConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeVarInt(packet.action);
        buffer.writeVarInt(packet.value);
    }
    public static DimensionTowerConfigPacket decode(FriendlyByteBuf buffer) {
        return new DimensionTowerConfigPacket(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt());
    }
    public static void handle(DimensionTowerConfigPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.containerMenu instanceof DimensionSpaceTowerMenu menu)
                    || !menu.getBlockPos().equals(packet.pos) || !player.level().hasChunkAt(packet.pos)
                    || !menu.stillValid(player) || !player.level().mayInteract(player, packet.pos)) return;
            if (!(player.level().getBlockEntity(packet.pos) instanceof DimensionSpaceTowerBlockEntity tower)) return;
            switch (packet.action) {
                case SET_CHANNEL -> {
                    if (packet.value >= 0 && packet.value <= DimensionTowerChannels.MAX_CHANNEL) tower.setChannel(packet.value);
                }
                case TOGGLE_ENABLED -> tower.toggleEnabled();
                case CYCLE_TYPE -> tower.cycleEnergyType();
                case CYCLE_MODE -> tower.cycleMode();
                case CYCLE_REDSTONE -> tower.cycleRedstoneMode();
                default -> { }
            }
        });
        context.setPacketHandled(true);
    }
}
