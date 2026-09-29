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
    private static final String SOCIAL_PROFILE = "net.parkabird.changedsynergy.ai.CreatureSocialProfile";
    private static final Object API_FAILURE = new Object();
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

    /** True only when Synergy reports its maximum per-player familiarity score. */
    public static boolean hasMaximumFamiliarity(ChangedEntity creature, ServerPlayer player) {
        if (!ModList.get().isLoaded(SYNERGY_MOD_ID) || creature == null || player == null
                || player.level().isClientSide()) {
            return false;
        }
        Object familiarity = invokeSynergy(
                PERSONALITY,
                "familiarity",
                new Class<?>[]{ChangedEntity.class, ServerPlayer.class},
                creature,
                player
        );
        return familiarity instanceof Number score && score.intValue() >= 60;
    }

    /** Adds 100 familiarity, establishes the relationship, and consumes one enchanted orange. */
    public static OfferResult offerEnchantedGoldenOrange(ChangedEntity creature, ServerPlayer player) {
        if (!ModList.get().isLoaded(SYNERGY_MOD_ID)) {
            return OfferResult.API_UNAVAILABLE;
        }
        if (creature == null || player == null || player.level().isClientSide()) {
            return OfferResult.INVALID_CONTEXT;
        }

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            return OfferResult.NO_ITEM;
        }

        Object relationshipAllowed = invokeSynergy(
                SOCIAL_PROFILE,
                "allowsPersonalRelationship",
                new Class<?>[]{ChangedEntity.class},
                creature
        );
        if (relationshipAllowed == API_FAILURE) {
            return OfferResult.API_UNAVAILABLE;
        }
        if (!Boolean.TRUE.equals(relationshipAllowed)) {
            return OfferResult.NOT_APPLICABLE;
        }

        Object familiarity = invokeSynergy(
                PERSONALITY,
                "adjustFamiliarity",
                new Class<?>[]{ChangedEntity.class, ServerPlayer.class, int.class},
                creature,
                player,
                100
        );
        if (familiarity == API_FAILURE) {
            return OfferResult.API_UNAVAILABLE;
        }
        if (!(familiarity instanceof Number score) || score.intValue() <= 0) {
            return OfferResult.NOT_APPLICABLE;
        }

        Object newlyEstablished = invokeSynergy(
                PERSONALITY,
                "establishRelationship",
                new Class<?>[]{ChangedEntity.class, ServerPlayer.class},
                creature,
                player
        );
        Object established = invokeSynergy(
                PERSONALITY,
                "hasEstablishedRelationship",
                new Class<?>[]{ChangedEntity.class, ServerPlayer.class},
                creature,
                player
        );
        if (newlyEstablished == API_FAILURE || established == API_FAILURE) {
            return OfferResult.API_UNAVAILABLE;
        }
        if (!Boolean.TRUE.equals(established)) {
            return OfferResult.NOT_APPLICABLE;
        }

        applyEnchantedGoldenOrangeBenefits(creature);
        if (held.getCount() <= 1) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        } else {
            held.shrink(1);
        }
        return Boolean.TRUE.equals(newlyEstablished) ? OfferResult.ESTABLISHED : OfferResult.EXISTING;
    }

    private static void applyEnchantedGoldenOrangeBenefits(ChangedEntity creature) {
        Item goldenOrange = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("changed_addon", "golden_orange"));
        if (goldenOrange == null || goldenOrange == net.minecraft.world.item.Items.AIR) {
            goldenOrange = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("changed_additions", "golden_orange"));
        }
        if (goldenOrange != null && goldenOrange != net.minecraft.world.item.Items.AIR) {
            invokeSynergy(
                    FAVOR_SERVICE,
                    "applyGoldenOrangeBenefits",
                    new Class<?>[]{ChangedEntity.class, ItemStack.class},
                    creature,
                    new ItemStack(goldenOrange)
            );
        }
    }

    /** Offers a held gift through Synergy's relationship gift API. */
    public static OfferResult offerHeldRelationshipGift(ChangedEntity creature, ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return invokeOffer("offerHeldRelationshipGift", creature, player);
        }

        ItemStack original = player.getMainHandItem().copy();
        OfferResult result = invokeOffer("offerHeldRelationshipGift", creature, player);
        if (result.isAccepted() && original.isDamageableItem()) {
            original.hurtAndBreak(1, player, entity -> entity.broadcastBreakEvent(InteractionHand.MAIN_HAND));
            player.setItemInHand(InteractionHand.MAIN_HAND, original);
        }
        return result;
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

    private static Object invokeSynergy(String className, String methodName, Class<?>[] parameterTypes, Object... arguments) {
        if (!ModList.get().isLoaded(SYNERGY_MOD_ID)) {
            return API_FAILURE;
        }

        try {
            ClassLoader loader = ChangedSynergyFeedApi.class.getClassLoader();
            Class<?> apiClass = Class.forName(className, true, loader);
            Method method = apiClass.getMethod(methodName, parameterTypes);
            return method.invoke(null, arguments);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                 | InvocationTargetException | LinkageError exception) {
            logApiFailure(methodName, exception);
            return API_FAILURE;
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
