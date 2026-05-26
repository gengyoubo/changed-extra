package github.com.gengyoubo.CE.LP.network.packet;

import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.SpaceTowerBlockEntity;
import github.com.gengyoubo.CE.LP.SpaceTowerEnergyType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SpaceTowerConfigPacket {
    public static final int ACTION_TOGGLE_MODE = 0;
    public static final int ACTION_ADJUST_RPM = 1;
    public static final int ACTION_ADJUST_SU = 2;
    public static final int ACTION_SET_RPM = 3;
    public static final int ACTION_SET_SU = 4;

    private final BlockPos pos;
    private final int action;
    private final int typeOrdinal;
    private final int delta;

    public SpaceTowerConfigPacket(BlockPos pos, int action, int typeOrdinal, int delta) {
        this.pos = pos;
        this.action = action;
        this.typeOrdinal = typeOrdinal;
        this.delta = delta;
    }

    public static SpaceTowerConfigPacket toggleMode(BlockPos pos, SpaceTowerEnergyType type) {
        return new SpaceTowerConfigPacket(pos, ACTION_TOGGLE_MODE, type.ordinal(), 0);
    }

    public static SpaceTowerConfigPacket adjustRpm(BlockPos pos, int delta) {
        return new SpaceTowerConfigPacket(pos, ACTION_ADJUST_RPM, 0, delta);
    }

    public static SpaceTowerConfigPacket adjustSu(BlockPos pos, int delta) {
        return new SpaceTowerConfigPacket(pos, ACTION_ADJUST_SU, 0, delta);
    }

    public static SpaceTowerConfigPacket setRpm(BlockPos pos, int rpm) {
        return new SpaceTowerConfigPacket(pos, ACTION_SET_RPM, 0, rpm);
    }

    public static SpaceTowerConfigPacket setSu(BlockPos pos, int su) {
        return new SpaceTowerConfigPacket(pos, ACTION_SET_SU, 0, su);
    }

    public static void encode(SpaceTowerConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeVarInt(packet.action);
        buffer.writeVarInt(packet.typeOrdinal);
        buffer.writeVarInt(packet.delta);
    }

    public static SpaceTowerConfigPacket decode(FriendlyByteBuf buffer) {
        return new SpaceTowerConfigPacket(
                buffer.readBlockPos(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt()
        );
    }

    public static void handle(SpaceTowerConfigPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.blockPosition().closerThan(packet.pos, 16.0D)) {
                return;
            }

            BlockEntity blockEntity = player.level().getBlockEntity(packet.pos);
            if (!(blockEntity instanceof SpaceTowerBlockEntity tower)) {
                return;
            }

            switch (packet.action) {
                case ACTION_TOGGLE_MODE -> tower.toggleMode(SpaceTowerEnergyType.byOrdinal(packet.typeOrdinal));
                case ACTION_ADJUST_RPM -> tower.adjustCeRpm(packet.delta);
                case ACTION_ADJUST_SU -> tower.adjustCeSu(packet.delta);
                case ACTION_SET_RPM -> tower.setCeRpm(packet.delta);
                case ACTION_SET_SU -> tower.setCeSu(packet.delta);
                default -> {
                }
            }
        });
        context.setPacketHandled(true);
    }
}
