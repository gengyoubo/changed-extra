package github.com.gengyoubo.CE.mixins;

import net.ltxprogrammer.changed.entity.TransfurCause;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.ltxprogrammer.changed.network.packet.SyncTransfurPacket$Listing", remap = false)
public interface SyncTransfurPacketListingAccessor {
    @Accessor("form")
    int changed_extra$getForm();

    @Accessor("cause")
    TransfurCause changed_extra$getCause();

    @Accessor("progress")
    float changed_extra$getProgress();

    @Accessor("temporaryFromSuit")
    boolean changed_extra$isTemporaryFromSuit();

    @Accessor("data")
    CompoundTag changed_extra$getData();
}
