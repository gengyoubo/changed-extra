package github.com.gengyoubo.CE.LP.item;

import github.com.gengyoubo.CE.LP.client.renderer.MimicSirenTailRenderer;
import github.com.gengyoubo.CE.LP.init.CELPItem;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.TransfurCause;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.init.ChangedTransfurVariants;
import net.ltxprogrammer.changed.item.ExtendedItemProperties;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class MimicSirenTailItem extends MimicItem implements ExtendedItemProperties {
    private static final ResourceLocation MIMIC_ID = ResourceLocation.fromNamespaceAndPath("changede", "mimic_siren_tail");

    public MimicSirenTailItem(Properties properties) {
        super(Type.BOOTS, properties);
    }

    public static boolean isWornBy(LivingEntity entity) {
        if (entity.getItemBySlot(EquipmentSlot.FEET).is(CELPItem.MIMIC_SIREN_TAIL.get())) {
            return true;
        }
        if (entity instanceof ChangedEntity changed && changed.getUnderlyingPlayer() != null) {
            return changed.getUnderlyingPlayer().getItemBySlot(EquipmentSlot.FEET).is(CELPItem.MIMIC_SIREN_TAIL.get());
        }
        return false;
    }

    public static double getSirenSwimSpeed() {
        double speed = DefaultAttributes.getSupplier(ChangedEntities.LATEX_SIREN.get()).getBaseValue(ForgeMod.SWIM_SPEED.get());
        // Changed applies this scaling when transferring a creature's attributes to a player.
        return speed * (1.0D - (speed - 1.0D) / 16.0D);
    }

    public static boolean shouldSwimUnderwater(Player player) {
        return isWornBy(player) && player.isEyeInFluidType(ForgeMod.WATER_TYPE.get())
                && !player.isPassenger() && !player.isSleeping() && !player.isFallFlying()
                && !player.getAbilities().flying && !player.isSpectator();
    }

    @Override
    public boolean allowedInSlot(ItemStack stack, LivingEntity entity, EquipmentSlot slot) {
        return slot == EquipmentSlot.FEET;
    }

    @Override
    public boolean allowedToWear(ItemStack stack, LivingEntity entity, EquipmentSlot slot) {
        return slot == EquipmentSlot.FEET;
    }

    @Override
    protected ResourceLocation getMimicId() {
        return MIMIC_ID;
    }

    @Override
    protected void transfur(ServerPlayer player, ItemStack stack) {
        ProcessTransfur.setPlayerTransfurVariant(player, ChangedTransfurVariants.LATEX_SIREN.get(), TransfurCause.DEFAULT);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "changed:textures/latex_siren.png";
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.changede.mimic_siren_tail.desc"));
        tooltip.add(Component.translatable("item.changede.mimic_siren_tail.risk"));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private MimicSirenTailRenderer renderer;

            @Override
            public @NotNull Model getGenericArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                return MimicSirenTailRenderer.getArmorModel(entity, original);
            }

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new MimicSirenTailRenderer();
                }
                return renderer;
            }
        });
    }
}
