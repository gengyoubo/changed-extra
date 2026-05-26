package github.com.gengyoubo.CE.LP.item;

import github.com.gengyoubo.CE.LP.client.renderer.MimicYufengWingsItemRenderer;
import github.com.gengyoubo.CE.LP.init.CELPItem;
import github.com.gengyoubo.CE.fix.SpecialLatexFix.PatreonBenefitsFix;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.ltxprogrammer.changed.entity.TransfurCause;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.init.ChangedTransfurVariants;
import net.ltxprogrammer.changed.item.ExtendedItemProperties;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.ltxprogrammer.changed.util.PatreonBenefits;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class MimicYufengWingsItem extends MimicItem implements ExtendedItemProperties {
    private static final String GRANTED_FLIGHT_TAG = "changede_mimic_yufeng_wings_granted_flight";
    private static final ResourceLocation MIMIC_ID = ResourceLocation.fromNamespaceAndPath("changede", "mimic_yufeng_wings");

    public MimicYufengWingsItem(Properties properties) {
        super(properties);
    }

    public static boolean isMimicYufengWings(ItemStack stack) {
        return stack.is(CELPItem.MIMIC_YUFENG_WINGS.get());
    }

    public static boolean isWornBy(LivingEntity entity) {
        return isMimicYufengWings(entity.getItemBySlot(EquipmentSlot.CHEST));
    }

    public static void updateFlight(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        boolean wearing = isWornBy(player);
        CompoundTag data = player.getPersistentData();
        boolean grantedByWings = data.getBoolean(GRANTED_FLIGHT_TAG);
        Abilities abilities = player.getAbilities();

        if (wearing) {
            if (!abilities.mayfly) {
                abilities.mayfly = true;
                player.onUpdateAbilities();
            }
            if (!grantedByWings) {
                data.putBoolean(GRANTED_FLIGHT_TAG, true);
            }
            return;
        }

        if (!grantedByWings || player.isCreative() || player.isSpectator()) {
            if (grantedByWings) {
                data.remove(GRANTED_FLIGHT_TAG);
            }
            return;
        }

        abilities.mayfly = false;
        abilities.flying = false;
        data.remove(GRANTED_FLIGHT_TAG);
        player.onUpdateAbilities();
    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return ElytraItem.isFlyEnabled(stack);
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (!entity.level().isClientSide) {
            int nextFlightTick = flightTicks + 1;
            if (nextFlightTick % 10 == 0) {
                if (nextFlightTick % 20 == 0) {
                    stack.hurtAndBreak(1, entity, living -> living.broadcastBreakEvent(EquipmentSlot.CHEST));
                }
                entity.gameEvent(GameEvent.ELYTRA_GLIDE);
            }
        }
        return true;
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(Items.PHANTOM_MEMBRANE) || super.isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public boolean allowedInSlot(ItemStack stack, LivingEntity entity, EquipmentSlot slot) {
        return slot == EquipmentSlot.CHEST && hasOwnSpecialWings(entity);
    }

    @Override
    public boolean allowedToWear(ItemStack stack, LivingEntity entity, EquipmentSlot slot) {
        return slot == EquipmentSlot.CHEST && hasOwnSpecialWings(entity);
    }

    private static boolean hasOwnSpecialWings(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return true;
        }

        PatreonBenefitsFix.SpecialForm form = PatreonBenefitsFix.getPlayerSpecialForm(player.getUUID());
        if (form == null) {
            return true;
        }

        return form.modelData().values().stream()
                .map(PatreonBenefits.ModelData::animationData)
                .noneMatch(animationData -> animationData.hasWings() || animationData.hasWingsV2());
    }

    @Override
    public @NotNull SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_ELYTRA;
    }

    @Override
    protected ResourceLocation getMimicId() {
        return MIMIC_ID;
    }

    @Override
    protected void transfur(ServerPlayer player, ItemStack stack) {
        TransfurVariant<?> variant = player.getRandom().nextBoolean()
                ? ChangedTransfurVariants.DARK_LATEX_YUFENG.get()
                : ChangedTransfurVariants.DARK_LATEX_DOUBLE_YUFENG.get();
        ProcessTransfur.setPlayerTransfurVariant(player, variant, TransfurCause.DEFAULT);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private MimicYufengWingsItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new MimicYufengWingsItemRenderer();
                }
                return renderer;
            }
        });
    }
}
