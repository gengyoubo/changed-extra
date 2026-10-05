package github.com.gengyoubo.CE.LP.network.packet;

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
    private static final int MAX_BLOCKS = 4096;

    private final ResourceLocation sourceDimension;
    private final BlockPos portalPos;
    private final int skyColor;
    private final List<Entry> entries;

    public LatexPaintingPortalPreviewPacket(ResourceLocation sourceDimension, BlockPos portalPos, List<Entry> entries) {
        this(sourceDimension, portalPos, 0xD4DCE5, entries);
    }

    public LatexPaintingPortalPreviewPacket(ResourceLocation sourceDimension, BlockPos portalPos, int skyColor, List<Entry> entries) {
        this.sourceDimension = sourceDimension;
        this.portalPos = portalPos;
        this.skyColor = skyColor;
        this.entries = List.copyOf(entries);
    }

    public static void encode(LatexPaintingPortalPreviewPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.sourceDimension);
        buffer.writeBlockPos(packet.portalPos);
        buffer.writeVarInt(packet.skyColor);
        buffer.writeVarInt(packet.entries.size());
        for (Entry entry : packet.entries) {
            buffer.writeShort(entry.dx());
            buffer.writeShort(entry.dy());
            buffer.writeShort(entry.dz());
            buffer.writeVarInt(entry.stateId());
        }
    }

    public static LatexPaintingPortalPreviewPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation sourceDimension = buffer.readResourceLocation();
        BlockPos portalPos = buffer.readBlockPos();
        int skyColor = buffer.readVarInt();
        int encodedSize = buffer.readVarInt();
        if (encodedSize < 0 || encodedSize > MAX_BLOCKS) {
            throw new IllegalArgumentException("Invalid portal preview size: " + encodedSize);
        }
        List<Entry> entries = new ArrayList<>(encodedSize);
        for (int i = 0; i < encodedSize; i++) {
            Entry entry = new Entry(buffer.readShort(), buffer.readShort(), buffer.readShort(), buffer.readVarInt());
            entries.add(entry);
        }
        return new LatexPaintingPortalPreviewPacket(sourceDimension, portalPos, skyColor, entries);
    }

    public static void handle(LatexPaintingPortalPreviewPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(packet)));
        context.setPacketHandled(true);
    }

    private static void handleClient(LatexPaintingPortalPreviewPacket packet) {
        try {
            Class<?> cache = Class.forName("github.com.gengyoubo.CE.client.LatexPaintingPortalPreviewCache");
            cache.getMethod("update", ResourceLocation.class, BlockPos.class, int.class, List.class)
                    .invoke(null, packet.sourceDimension, packet.portalPos, packet.skyColor, packet.entries);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to update latex painting portal preview cache", exception);
        }
    }

    public record Entry(int dx, int dy, int dz, int stateId) {
    }
}
