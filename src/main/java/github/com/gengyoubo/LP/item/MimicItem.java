package github.com.gengyoubo.LP.item;

import github.com.gengyoubo.changede;
import github.com.gengyoubo.player.Perseverance;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public abstract class MimicItem extends ArmorItem {
    private static final String ROOT_TAG = "changede_mimic_items";
    private static final String WILL_TRANSFUR_TAG = "will_transfur";

    protected MimicItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    @SuppressWarnings("removal")
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        super.onArmorTick(stack, level, player);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (serverPlayer.isCreative() || serverPlayer.isSpectator()) {
            resetDeathState(serverPlayer);
            return;
        }
        if (ProcessTransfur.isPlayerTransfurred(serverPlayer)) {
            return;
        }

        CompoundTag state = getMimicState(serverPlayer, getMimicId());
        if (!state.contains(WILL_TRANSFUR_TAG)) {
            state.putBoolean(WILL_TRANSFUR_TAG, Perseverance.rollMimicTransfur(serverPlayer));
        }
        if (!state.getBoolean(WILL_TRANSFUR_TAG)) {
            return;
        }

        transfur(serverPlayer, stack);
    }

    public static void resetDeathState(Player player) {
        player.getPersistentData().remove(ROOT_TAG);
    }

    private static CompoundTag getMimicState(Player player, ResourceLocation mimicId) {
        CompoundTag data = player.getPersistentData();
        CompoundTag root = data.getCompound(ROOT_TAG);
        String key = mimicId.toString();
        CompoundTag state = root.getCompound(key);
        root.put(key, state);
        data.put(ROOT_TAG, root);
        return state;
    }

    protected abstract ResourceLocation getMimicId();

    protected abstract void transfur(ServerPlayer player, ItemStack stack);

    protected void warnMissingVariant(ServerPlayer player, ResourceLocation mimicId) {
        changede.LOGGER.warn("Mimic item {} could not find a transfur variant for player {}", mimicId, player.getGameProfile().getName());
    }
}
