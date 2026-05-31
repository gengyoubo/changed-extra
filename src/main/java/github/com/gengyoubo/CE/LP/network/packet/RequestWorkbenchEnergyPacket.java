package github.com.gengyoubo.CE.LP.network.packet;

import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergyHolder;
import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergyRules;
import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergyStorage;
import github.com.gengyoubo.CE.LP.network.CENetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class RequestWorkbenchEnergyPacket {
    private final BlockPos pos;

    public RequestWorkbenchEnergyPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(RequestWorkbenchEnergyPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
    }

    public static RequestWorkbenchEnergyPacket decode(FriendlyByteBuf buffer) {
        return new RequestWorkbenchEnergyPacket(buffer.readBlockPos());
    }

    public static void handle(RequestWorkbenchEnergyPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.blockPosition().closerThan(packet.pos, 64.0D)) {
                return;
            }

            BlockEntity blockEntity = player.level().getBlockEntity(packet.pos);
            EnergySnapshot snapshot = snapshot(blockEntity);
            if (snapshot == null) {
                snapshot = fallbackSnapshot(player.level().getBlockState(packet.pos).getBlock());
            }
            if (snapshot == null) {
                return;
            }

            CENetwork.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new WorkbenchEnergyPacket(player.level().dimension().location(), packet.pos, snapshot.stored(), snapshot.max())
            );
        });
        context.setPacketHandled(true);
    }

    private static EnergySnapshot snapshot(BlockEntity blockEntity) {
        if (blockEntity instanceof ILatexEnergyHandler handler) {
            return new EnergySnapshot(handler.getEnergyStored(), handler.getMaxEnergyStored());
        }
        if (blockEntity instanceof WorkbenchEnergyHolder holder) {
            WorkbenchEnergyStorage storage = holder.changede$getWorkbenchEnergy();
            return new EnergySnapshot(storage.getEnergyStored(), storage.getMaxEnergyStored());
        }
        return null;
    }

    private static EnergySnapshot fallbackSnapshot(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id == null) {
            return null;
        }

        String blockId = id.toString();
        return switch (blockId) {
            case "changed:infuser", "changed:purifier", "changed_addon:unifuser", "changed_addon:catalyzer" ->
                    new EnergySnapshot(0, WorkbenchEnergyRules.NORMAL_CAPACITY);
            case "changed_addon:advanced_unifuser", "changed_addon:advanced_catalyzer" ->
                    new EnergySnapshot(0, WorkbenchEnergyRules.ADVANCED_CAPACITY);
            default -> null;
        };
    }

    private record EnergySnapshot(int stored, int max) {
    }
}
