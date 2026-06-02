package github.com.gengyoubo.CE.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LatexDrinkItem extends Item {
    private static final int MAX_USES = 12;
    private static final int HOT_SPEED_DURATION = 20 * 60;
    private static final int ICE_EFFECT_DURATION = 20 * 30;

    private final boolean iced;

    public LatexDrinkItem(boolean iced) {
        super(new Item.Properties().durability(MAX_USES));
        this.iced = iced;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity livingEntity) {
        if (!level.isClientSide) {
            if (iced) {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ICE_EFFECT_DURATION, 1));
                livingEntity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ICE_EFFECT_DURATION, 0));
            } else {
                livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, HOT_SPEED_DURATION, 0));
            }
        }

        if (!(livingEntity instanceof Player player) || !player.getAbilities().instabuild) {
            stack.hurtAndBreak(1, livingEntity, entity -> entity.broadcastBreakEvent(livingEntity.getUsedItemHand()));
        }

        return stack;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        int remainingUses = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
        tooltip.add(Component.translatable("item.bakeries.tips.repeat_eat_item.drink")
                .append(remainingUses + "/" + stack.getMaxDamage()));
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
        return 32;
    }
}
