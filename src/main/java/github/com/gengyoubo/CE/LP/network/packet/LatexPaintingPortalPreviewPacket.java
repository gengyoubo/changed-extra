package github.com.gengyoubo.CE.LP.network.packet;

import github.com.gengyoubo.CE.changede;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class LatexPaintingPortalPreviewPacket {
    private static final int MAX_BLOCKS = 32768;

    private final ResourceLocation sourceDimension;
    private final BlockPos portalPos;
    private final List<Entry> entries;

    public LatexPaintingPortalPreviewPacket(ResourceLocation sourceDimension, BlockPos portalPos, List<Entry> entries) {
        this.sourceDimension = sourceDimension;
        this.portalPos = portalPos;
        this.entries = List.copyOf(entries);
    }

    public static void encode(LatexPaintingPortalPreviewPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.sourceDimension);
        buffer.writeBlockPos(packet.portalPos);
        buffer.writeVarInt(packet.entries.size());
        for (Entry entry : packet.entries) {
            buffer.writeByte(entry.dx());
            buffer.writeByte(entry.dy());
            buffer.writeByte(entry.dz());
            buffer.writeVarInt(entry.stateId());
        }
    }

    public static LatexPaintingPortalPreviewPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation sourceDimension = buffer.readResourceLocation();
        BlockPos portalPos = buffer.readBlockPos();
        int encodedSize = buffer.readVarInt();
        int storedSize = Math.min(encodedSize, MAX_BLOCKS);
        List<Entry> entries = new ArrayList<>(storedSize);
        for (int i = 0; i < encodedSize; i++) {
            Entry entry = new Entry(buffer.readByte(), buffer.readByte(), buffer.readByte(), buffer.readVarInt());
            if (i < MAX_BLOCKS) {
                entries.add(entry);
            }
        }
        return new LatexPaintingPortalPreviewPacket(sourceDimension, portalPos, entries);
    }

    public static void handle(LatexPaintingPortalPreviewPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(packet)));
        context.setPacketHandled(true);
    }

    private static void handleClient(LatexPaintingPortalPreviewPacket packet) {
        try {
            changede.LOGGER.warn("Received latex painting portal preview for {} at {}, blocks={}", packet.sourceDimension, packet.portalPos, packet.entries.size());
            Class<?> cache = Class.forName("github.com.gengyoubo.CE.client.LatexPaintingPortalPreviewCache");
            cache.getMethod("update", ResourceLocation.class, BlockPos.class, List.class)
                    .invoke(null, packet.sourceDimension, packet.portalPos, packet.entries);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to update latex painting portal preview cache", exception);
        }
    }

    public record Entry(byte dx, byte dy, byte dz, int stateId) {
    }
}
