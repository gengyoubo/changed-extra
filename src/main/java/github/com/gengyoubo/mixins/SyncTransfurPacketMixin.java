package github.com.gengyoubo.mixins;

import github.com.gengyoubo.fix.SpecialLatex.PatreonBenefitsFix;
import github.com.gengyoubo.fix.SpecialLatex.SpecialLatex;
import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.entity.TransfurContext;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.init.ChangedRegistry;
import net.ltxprogrammer.changed.init.ChangedTransfurVariants;
import net.ltxprogrammer.changed.network.packet.SyncTransfurPacket;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(value = SyncTransfurPacket.class, remap = false)
public abstract class SyncTransfurPacketMixin {
    @Shadow @Final private Map<Integer, ?> changedForms;

    /**
     * @author gengyoubo
     * @reason Support dynamic special/form_uuid variants that are not in TRANSFUR_VARIANT registry IDs.
     */
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void changede$handle(
            NetworkEvent.Context context,
            CompletableFuture<net.minecraft.world.level.Level> levelFuture,
            Executor sidedExecutor,
            CallbackInfoReturnable<CompletableFuture<Void>> cir
    ) {
        if (context.getDirection().getReceptionSide().isClient()) {
            context.setPacketHandled(true);
            cir.setReturnValue(levelFuture.thenAccept(level -> this.changedForms.forEach((entityId, rawListing) -> {
                Entity entity = level.getEntity(entityId);
                if (!(entity instanceof Player player)) {
                    Changed.LOGGER.warn("Failed to find player specified in SyncTransfurPacket, {}", entityId);
                    return;
                }

                SyncTransfurPacketListingAccessor listing = (SyncTransfurPacketListingAccessor) rawListing;
                int formId = listing.changede$getForm();
                TransfurVariant<?> variant = ChangedRegistry.TRANSFUR_VARIANT.getValue(formId);
                boolean usedSpecialFallback = false;
                var data = listing.changede$getData();
                boolean hasData = data != null && !data.isEmpty();
                boolean looksLikeActiveTransfur =
                        listing.changede$getProgress() > 0.0001f ||
                        listing.changede$isTemporaryFromSuit() ||
                        hasData;

                // Dynamic special variants may be synced as NO_FORM (-1), but NO_FORM is also used by untf.
                // Only use special fallback when packet clearly represents an active transfur state.
                if (variant == null && looksLikeActiveTransfur) {
                    variant = PatreonBenefitsFix.getPlayerSpecialVariant(player.getUUID());
                    usedSpecialFallback = variant != null;
                }
                if (variant == null) {
                    variant = PatreonBenefitsFix.resolveVariant(TransfurVariant.SPECIAL_LATEX);
                }
                if (variant == null) {
                    variant = ChangedTransfurVariants.FALLBACK_VARIANT.get();
                }
                Changed.LOGGER.debug(
                        "SyncTransfurPacket client entityId={} player={} formId={} progress={} hasData={} temp={} variant={} fallback={}",
                        entityId,
                        player.getScoreboardName(),
                        formId,
                        listing.changede$getProgress(),
                        hasData,
                        listing.changede$isTemporaryFromSuit(),
                        variant == null ? "null" : variant.getFormId(),
                        usedSpecialFallback
                );

                var instance = ProcessTransfur.setPlayerTransfurVariant(
                        player,
                        variant,
                        TransfurContext.hazard(listing.changede$getCause()),
                        listing.changede$getProgress(),
                        listing.changede$isTemporaryFromSuit()
                );
                if (instance != null) {
                    instance.load(listing.changede$getData());
                    if (instance.getChangedEntity() instanceof SpecialLatex specialLatex) {
                        specialLatex.setSpecialForm(player.getUUID());
                    }
                } else {
                    Changed.LOGGER.debug("SyncTransfurPacket failed to set transfur variant for player={}", player.getScoreboardName());
                }
            })));
            return;
        }

        // Server side: let original SyncTransfurPacket#handle run.
        // We only override client resolution to support dynamic special/form_uuid variants.
    }
}
