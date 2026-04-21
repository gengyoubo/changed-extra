package github.com.gengyoubo.mixins;

import github.com.gengyoubo.fix.PatreonBenefitsFix;
import net.ltxprogrammer.changed.Changed;
import net.ltxprogrammer.changed.entity.TransfurCause;
import net.ltxprogrammer.changed.entity.ai.ImmediateTransfurDecision;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.init.ChangedItems;
import net.ltxprogrammer.changed.init.ChangedSounds;
import net.ltxprogrammer.changed.init.ChangedTransfurVariants;
import net.ltxprogrammer.changed.item.LatexSyringe;
import net.ltxprogrammer.changed.process.Pale;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value= LatexSyringe.class, remap=false)
public class LatexSyringeMixin {
    /**
     * @author gengyoubo
     * @reason Woverite
     */
    @Overwrite
    @NotNull
    public ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity) throws Exception {
        Player player = entity instanceof Player ? (Player)entity : null;
        if (player instanceof ServerPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer)player, stack);
        }

        ChangedSounds.broadcastSound(entity, ChangedSounds.SYRINGE_PRICK, 1.0F, 1.0F);
        if (player != null) {
            CompoundTag tag = stack.getTag();
            TransfurCause cause = player.getUsedItemHand() == InteractionHand.MAIN_HAND == (player.getMainArm() == HumanoidArm.RIGHT) ? TransfurCause.SYRINGE : TransfurCause.SYRINGE_LEFT_HAND;
            if (tag != null && tag.contains("safe") && ProcessTransfur.isPlayerTransfurred(player)) {
                if (tag.getBoolean("safe")) {
                    Pale.tryCure(player);
                }
            } else if (tag != null && tag.contains("form")) {
                ResourceLocation formLocation = ResourceLocation.parse(tag.getString("form"));
                boolean requestedSpecialLatex = formLocation.equals(TransfurVariant.SPECIAL_LATEX);
                if (formLocation.equals(TransfurVariant.SPECIAL_LATEX)) {
                    formLocation = PatreonBenefitsFix.getSpecialFormId(entity.getUUID());
                }
                TransfurVariant<?> variant = PatreonBenefitsFix.resolveVariant(formLocation);

                if (variant == null) {
                    if (requestedSpecialLatex) {
                        Changed.LOGGER.warn("Unable to resolve player special form {}. Falling back to base special latex form.", formLocation);
                        variant = PatreonBenefitsFix.resolveVariant(TransfurVariant.SPECIAL_LATEX);
                    }
                    if (variant == null) {
                        Changed.LOGGER.warn("Unable to resolve transfur variant {}. Falling back to default variant.", formLocation);
                        variant = ChangedTransfurVariants.FALLBACK_VARIANT.get();
                    }
                }
                ProcessTransfur.transfur(entity, ImmediateTransfurDecision.unsafe(variant, cause));
            } else {
                ProcessTransfur.transfur(entity, ImmediateTransfurDecision.unsafe(ChangedTransfurVariants.FALLBACK_VARIANT.get(), cause));
            }

            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            stack = new ItemStack((ItemLike) ChangedItems.SYRINGE.get());
        }

        return stack;
    }
}
