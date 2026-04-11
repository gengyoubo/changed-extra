package github.com.gengyoubo.projectextended.items;

import gg.galaxygaming.projectextended.common.items.PEShield;
import github.com.gengyoubo.projectextended.common.ProjectExtendedLang;
import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.capability.ChargeItemCapabilityWrapper;
import moze_intel.projecte.capability.ItemCapabilityWrapper;
import moze_intel.projecte.capability.ModeChangerItemCapabilityWrapper;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.gameObjs.items.IBarHelper;
import moze_intel.projecte.gameObjs.items.IItemMode;
import moze_intel.projecte.utils.text.ILangEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CEShield extends PEShield implements IItemCharge, IItemMode, IBarHelper {
    public static final byte NORMAL = 0;
    public static final byte REBOUND = 1;
    public static final byte INVULNERABILITY = 2;

    private final int numCharges;
    private final ILangEntry[] modeDesc;

    public CEShield(EnumMatterType matterType, Item.Properties props) {
        super(matterType, props);
        this.numCharges = 5;
        this.modeDesc = new ILangEntry[]{
                ProjectExtendedLang.MODE_SHIELD_1,
                ProjectExtendedLang.MODE_SHIELD_2,
                ProjectExtendedLang.MODE_SHIELD_3
        };
    }

    public boolean isRedMatter() {
        return this.getMatterTier() > 0;
    }

    public boolean isMode(ItemStack stack, byte mode) {
        return this.getMode(stack) == mode;
    }

    public boolean canReflect(ItemStack stack) {
        return this.isMode(stack, REBOUND);
    }

    public float getReflectMultiplier(ItemStack stack) {
        float base = this.isRedMatter() ? 1.0F : 0.5F;
        return base + 0.5F * this.getChargePercent(stack);
    }

    public boolean canNegateDamage(ItemStack stack) {
        return this.isMode(stack, INVULNERABILITY);
    }

    public boolean canNegateAllDamage(ItemStack stack) {
        return this.canNegateDamage(stack) && this.isRedMatter();
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return true;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment.category == EnchantmentCategory.BREAKABLE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public int getNumCharges(@NotNull ItemStack stack) {
        return this.numCharges;
    }

    @Override
    public ILangEntry[] getModeLangEntries() {
        return this.modeDesc;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return this.getScaledBarWidth(stack);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return this.getColorForBar(stack);
    }

    @Override
    public float getWidthForBar(ItemStack stack) {
        return 1.0F - this.getChargePercent(stack);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) {
        return new ItemCapabilityWrapper(stack, new ChargeItemCapabilityWrapper(),
                new ModeChangerItemCapabilityWrapper());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level world, @NotNull List<Component> list, @NotNull TooltipFlag flags) {
        super.appendHoverText(stack, world, list, flags);
        list.add(this.getToolTip(stack));
    }
}
