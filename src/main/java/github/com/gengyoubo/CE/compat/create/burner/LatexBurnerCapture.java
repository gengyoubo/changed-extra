package github.com.gengyoubo.CE.compat.create.burner;

import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class LatexBurnerCapture {
    private LatexBurnerCapture() {}
    public static boolean protectedCreature(LivingEntity entity) {
        if (entity instanceof TamableAnimal animal && (animal.isTame() || animal.getOwnerUUID() != null)) return true;
        if (entity.isPassenger() || entity.isVehicle()) return true;
        CompoundTag data = entity.getPersistentData();
        if (data.hasUUID("changede_camp_resident") || data.hasUUID("changede_camp_visitor")
                || data.hasUUID("changede_camp_raider") || data.hasUUID("changede_maid_work_owner")
                || !data.getString("changede_maid_work_task").isEmpty()) return true;
        CompoundTag social = data.getCompound("ChangedSynergySocial");
        if (social.hasUUID("PetOwner") || !social.getCompound("BondedPlayers").isEmpty()) return true;
        CompoundTag memories = data.getCompound("ChangedSynergyPersonality").getCompound("PlayerMemories");
        for (String id : memories.getAllKeys()) if (memories.getCompound(id).getBoolean("Relationship")) return true;
        return false;
    }
    public static InteractionResult capture(ItemStack held, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof ChangedEntity creature) || !target.isAlive() || target.isRemoved()
                || !player.mayBuild() || !player.level().mayInteract(player, target.blockPosition())) return InteractionResult.PASS;
        if (protectedCreature(target)) {
            if (!player.level().isClientSide) player.displayClientMessage(Component.translatable("changede.latex_burner.protected"), true);
            return InteractionResult.FAIL;
        }
        if (LatexBurnerProfiles.find(creature) == null) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        CompoundTag snapshot = new CompoundTag();
        if (!target.save(snapshot)) return InteractionResult.FAIL;
        CompoundTag machine = new CompoundTag();
        machine.put("Creature", snapshot);
        machine.putString("RuntimeLatex", LatexBurnerProfiles.runtimeLatex(creature));
        ItemStack filled = new ItemStack(LatexBurnerCompat.FILLED.get());
        filled.getOrCreateTag().put("BlockEntityTag", machine);
        // Deliver one complete snapshot, then retire the only world entity. No death loot is fired.
        if (!player.isCreative() && held.getCount() == 1) {
            // Player.interactOn clears this hand if its original stack becomes empty after the
            // item callback. Replace the stack without emptying that reference, or it erases
            // the newly captured burner after we return.
            player.setItemInHand(hand, filled);
        } else {
            if (!player.isCreative()) held.shrink(1);
            player.getInventory().placeItemBackInInventory(filled);
        }
        target.discard();
        player.level().playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.BUCKET_FILL,
                net.minecraft.sounds.SoundSource.BLOCKS, .8f, .7f);
        return InteractionResult.SUCCESS;
    }
}
