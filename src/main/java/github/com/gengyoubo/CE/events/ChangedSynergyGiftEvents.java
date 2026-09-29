package github.com.gengyoubo.CE.events;

import github.com.gengyoubo.CE.compat.synergy.ChangedSynergyFeedApi;
import github.com.gengyoubo.CE.init.CEItem;
import github.com.gengyoubo.CE.items.LatexDrinkItem;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public final class ChangedSynergyGiftEvents {
    private ChangedSynergyGiftEvents() {
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof ChangedEntity creature)
                || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack held = event.getItemStack();
        boolean enchantedOrange = held.is(CEItem.ENCHANTED_GOLDEN_ORANGE.get());
        boolean latexDrink = held.getItem() instanceof LatexDrinkItem;
        if (!enchantedOrange && !latexDrink) {
            return;
        }

        ChangedSynergyFeedApi.OfferResult result = ChangedSynergyFeedApi.OfferResult.NOT_APPLICABLE;
        if (enchantedOrange) {
            result = ChangedSynergyFeedApi.offerEnchantedGoldenOrange(creature, player);
        } else if (latexDrink) {
            ChangedSynergyFeedApi.RelationshipTier tier = ChangedSynergyFeedApi.relationshipTier(creature, player);
            if (tier == ChangedSynergyFeedApi.RelationshipTier.FAMILIAR
                    || tier == ChangedSynergyFeedApi.RelationshipTier.CLOSE) {
                result = ChangedSynergyFeedApi.offerHeldRelationshipGift(creature, player);
            }
        }

        if (result != ChangedSynergyFeedApi.OfferResult.API_UNAVAILABLE
                && result != ChangedSynergyFeedApi.OfferResult.INVALID_CONTEXT
                && result != ChangedSynergyFeedApi.OfferResult.NOT_APPLICABLE
                && result != ChangedSynergyFeedApi.OfferResult.NO_ITEM
                && result != ChangedSynergyFeedApi.OfferResult.UNKNOWN) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}
