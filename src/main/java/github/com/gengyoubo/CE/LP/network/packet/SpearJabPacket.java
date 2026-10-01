package github.com.gengyoubo.CE.LP.network.packet;

import github.com.gengyoubo.CE.items.LatexSpearItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Only intent crosses the network; the server computes reach, targets and damage. */
public record SpearJabPacket() {
    public static void encode(SpearJabPacket packet, FriendlyByteBuf buffer) { }
    public static SpearJabPacket decode(FriendlyByteBuf buffer) { return new SpearJabPacket(); }
    public static void handle(SpearJabPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null && player.getMainHandItem().getItem() instanceof LatexSpearItem spear) spear.jab(player);
        });
        context.setPacketHandled(true);
    }
}
