package github.com.gengyoubo.mixins;

import net.ltxprogrammer.changed.entity.TransfurCause;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.ltxprogrammer.changed.network.packet.SyncTransfurPacket$Listing", remap = false)
public interface SyncTransfurPacketListingAccessor {
    @Accessor("form")
    int changede$getForm();

    @Accessor("cause")
    TransfurCause changede$getCause();

    @Accessor("progress")
    float changede$getProgress();

    @Accessor("temporaryFromSuit")
    boolean changede$isTemporaryFromSuit();

    @Accessor("data")
    CompoundTag changede$getData();
}
