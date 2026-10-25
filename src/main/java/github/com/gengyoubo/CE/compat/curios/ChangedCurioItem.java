package github.com.gengyoubo.CE.compat.curios;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.UUID;
import net.ltxprogrammer.changed.data.AccessorySlotContext;
import net.ltxprogrammer.changed.item.AccessoryItem;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Curios owns equipment changes, ticking and attributes; Changed keeps its item behavior. */
final class ChangedCurioItem implements ICurioItem {
    private AccessorySlotContext<?> context(SlotContext context, ItemStack stack) {
        return ChangedCuriosSlots.typeFor(context.identifier(), stack)
                .map(type -> new AccessorySlotContext<>(context.entity(), type, stack)).orElse(null);
    }

    @Override public boolean canEquip(SlotContext context, ItemStack stack) {
        return ChangedCuriosSlots.canEquip(context, stack);
    }

    @Override public boolean canEquipFromUse(SlotContext context, ItemStack stack) { return true; }

    @Override public void curioTick(SlotContext context, ItemStack stack) {
        var changed = context(context, stack);
        if (changed != null && !context.cosmetic()) ((AccessoryItem) stack.getItem()).accessoryTick(changed);
    }

    @Override public void onEquip(SlotContext context, ItemStack previous, ItemStack stack) {
        var changed = context(context, stack);
        if (changed != null && !context.cosmetic() && previous.getItem() != stack.getItem()) {
            ((AccessoryItem) stack.getItem()).accessoryEquipped(changed);
        }
    }

    @Override public void onUnequip(SlotContext context, ItemStack next, ItemStack stack) {
        var changed = context(context, stack);
        if (changed != null && !context.cosmetic() && next.getItem() != stack.getItem()) {
            ((AccessoryItem) stack.getItem()).accessoryRemoved(changed);
        }
    }

    @Override public void curioBreak(SlotContext context, ItemStack stack) {
        var changed = context(context, stack);
        if (changed != null) ((AccessoryItem) stack.getItem()).accessoryBreak(changed);
    }

    @Override public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext context, UUID uuid, ItemStack stack) {
        var changed = context(context, stack);
        return changed == null || context.cosmetic() ? ImmutableMultimap.of()
                : stack.getAttributeModifiers(changed.slotType().getEquivalentSlot());
    }
}
