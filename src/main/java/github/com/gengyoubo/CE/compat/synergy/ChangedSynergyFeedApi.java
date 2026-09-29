package github.com.gengyoubo.CE.compat.synergy;

import github.com.gengyoubo.CE.changede;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/** Optional adapter for handing a held-food offer to Changed: Synergy. */
public final class ChangedSynergyFeedApi {
    private static final String SYNERGY_MOD_ID = "changed_synergy";
    private static final String FAVOR_SERVICE = "net.parkabird.changedsynergy.ai.RelationshipFavorService";
    private static final String PERSONALITY = "net.parkabird.changedsynergy.ai.CreaturePersonality";
    private static final AtomicBoolean API_FAILURE_LOGGED = new AtomicBoolean();

    private ChangedSynergyFeedApi() {
    }

    /**
     * Offers the player's main-hand item to a creature through Synergy's feeding API.
     * Call this before another interaction handler consumes the item.
     */
    public static OfferResult offerHeldFood(ChangedEntity creature, ServerPlayer player) {
        return invokeOffer("offerHeldFood", creature, player);
    }

    /**
     * Checks the relationship tier reported by Synergy without linking against its optional classes.
     */
    public static RelationshipTier relationshipTier(ChangedEntity creature, ServerPlayer player) {
        if (!ModList.get().isLoaded(SYNERGY_MOD_ID)) {
            return RelationshipTier.API_UNAVAILABLE;
        }
        if (creature == null || player == null || player.level().isClientSide()) {
            return RelationshipTier.INVALID_CONTEXT;
        }

        try {
            ClassLoader loader = ChangedSynergyFeedApi.class.getClassLoader();
            Class<?> personality = Class.forName(PERSONALITY, true, loader);
            Method relationshipTier = personality.getMethod("relationshipTier", ChangedEntity.class, ServerPlayer.class);
            Object result = relationshipTier.invoke(null, creature, player);
            if (result instanceof Enum<?> tier) {
                try {
                    return RelationshipTier.valueOf(tier.name());
                } catch (IllegalArgumentException ignored) {
                    return RelationshipTier.UNKNOWN;
                }
            }
            return RelationshipTier.UNKNOWN;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                 | InvocationTargetException | LinkageError exception) {
            logApiFailure("relationship-tier API", exception);
            return RelationshipTier.API_UNAVAILABLE;
        }
    }

    /**
     * Offers this mod's enchanted orange as Synergy's golden orange, while preserving
     * the original stack and consuming one of it only when Synergy accepts the gift.
     */
    public static OfferResult offerEnchantedGoldenOrange(ChangedEntity creature, ServerPlayer player) {
        if (!ModList.get().isLoaded(SYNERGY_MOD_ID)) {
            return OfferResult.API_UNAVAILABLE;
        }
        if (creature == null || player == null || player.level().isClientSide()) {
            return OfferResult.INVALID_CONTEXT;
        }

        Item aliasItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("changed_addon", "golden_orange"));
        if (aliasItem == null || aliasItem == net.minecraft.world.item.Items.AIR) {
            aliasItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("changed_additions", "golden_orange"));
        }
        if (aliasItem == null || aliasItem == net.minecraft.world.item.Items.AIR) {
            return OfferResult.API_UNAVAILABLE;
        }

        ItemStack original = player.getMainHandItem().copy();
        int originalCount = original.getCount();
        if (original.isEmpty()) {
            return OfferResult.NO_ITEM;
        }

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(aliasItem, originalCount));
        try {
            return invokeOffer("offerHeldFood", creature, player);
        } finally {
            ItemStack aliasAfterOffer = player.getMainHandItem();
            int remaining = aliasAfterOffer.is(aliasItem) ? aliasAfterOffer.getCount() : 0;
            int consumed = Math.max(0, originalCount - remaining);
            if (consumed == 0) {
                player.setItemInHand(InteractionHand.MAIN_HAND, original);
            } else if (consumed >= original.getCount()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            } else {
                original.shrink(consumed);
                player.setItemInHand(InteractionHand.MAIN_HAND, original);
            }
        }
    }

    /** Offers a held gift through Synergy's relationship gift API. */
    public static OfferResult offerHeldRelationshipGift(ChangedEntity creature, ServerPlayer player) {
        return invokeOffer("offerHeldRelationshipGift", creature, player);
    }

    private static OfferResult invokeOffer(String methodName, ChangedEntity creature, ServerPlayer player) {
        if (!ModList.get().isLoaded(SYNERGY_MOD_ID)) {
            return OfferResult.API_UNAVAILABLE;
        }
        if (creature == null || player == null || player.level().isClientSide()) {
            return OfferResult.INVALID_CONTEXT;
        }

        try {
            ClassLoader loader = ChangedSynergyFeedApi.class.getClassLoader();
            Class<?> service = Class.forName(FAVOR_SERVICE, true, loader);
            Method offer = service.getMethod(methodName, ChangedEntity.class, ServerPlayer.class);
            Object result = offer.invoke(null, creature, player);
            if (result instanceof Enum<?> synergyResult) {
                try {
                    return OfferResult.valueOf(synergyResult.name());
                } catch (IllegalArgumentException ignored) {
                    return OfferResult.UNKNOWN;
                }
            }
            return OfferResult.UNKNOWN;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                 | InvocationTargetException | LinkageError exception) {
            logApiFailure(methodName, exception);
            return OfferResult.API_UNAVAILABLE;
        }
    }

    private static void logApiFailure(String api, Throwable exception) {
        if (API_FAILURE_LOGGED.compareAndSet(false, true)) {
            changede.LOGGER.warn("Could not call Changed: Synergy's {}", api, exception);
        }
    }

    public enum RelationshipTier {
        API_UNAVAILABLE,
        INVALID_CONTEXT,
        STRANGER,
        NEW,
        FAMILIAR,
        CLOSE,
        STRAINED,
        UNKNOWN
    }

    public enum OfferResult {
        API_UNAVAILABLE,
        INVALID_CONTEXT,
        NOT_APPLICABLE,
        NO_ITEM,
        UNSUITABLE,
        REJECTED,
        CAT_ORANGE_REFUSED,
        BUILDING,
        ESTABLISHED,
        EXISTING,
        DIET_BUILDING,
        DIET_ESTABLISHED,
        DIET_EXISTING,
        UNKNOWN;

        public boolean isAccepted() {
            return switch (this) {
                case BUILDING, ESTABLISHED, EXISTING, DIET_BUILDING, DIET_ESTABLISHED, DIET_EXISTING -> true;
                default -> false;
            };
        }

        public boolean isDedicatedDiet() {
            return this == DIET_BUILDING || this == DIET_ESTABLISHED || this == DIET_EXISTING;
        }
    }
}
