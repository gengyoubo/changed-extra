package github.com.gengyoubo.projectextended.items;

import moze_intel.projecte.gameObjs.EnumMatterType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import org.jetbrains.annotations.NotNull;

public class CETotemOfUndying extends Item {
    public static final int DARK_MATTER_COOLDOWN = 20 * 60 * 10;
    public static final int RED_MATTER_COOLDOWN = 20 * 60;
    public static final int TRUE_MATTER_COOLDOWN = 0;

    private final Tier matterType;
    private final int cooldownTicks;

    public CETotemOfUndying(EnumMatterType matterType, Properties props) {
        this(matterType, getCooldownFor(matterType), props.rarity(getRarityFor(matterType)));
    }

    public CETotemOfUndying(EnumMatterTypeExtend matterType, Properties props) {
        this(matterType, TRUE_MATTER_COOLDOWN, props.rarity(Rarity.EPIC));
    }

    private CETotemOfUndying(Tier matterType, int cooldownTicks, Properties props) {
        super(props);
        this.matterType = matterType;
        this.cooldownTicks = cooldownTicks;
    }

    public Tier getMatterType() {
        return this.matterType;
    }

    public int getCooldownTicks() {
        return this.cooldownTicks;
    }

    public boolean hasCooldown() {
        return this.cooldownTicks > 0;
    }

    public boolean isTrueMatterTotem() {
        return this.cooldownTicks == TRUE_MATTER_COOLDOWN;
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        if (this.isTrueMatterTotem()) {
            long time = System.currentTimeMillis() / 50L;
            float hue = (time % 10L) / 10.0F;
            int color = Mth.hsvToRgb(hue, 1.0F, 1.0F);
            return Component.translatable(this.getDescriptionId(stack))
                    .setStyle(Style.EMPTY.withColor(color));
        }
        return super.getName(stack);
    }

    private static int getCooldownFor(EnumMatterType matterType) {
        return matterType == EnumMatterType.RED_MATTER ? RED_MATTER_COOLDOWN : DARK_MATTER_COOLDOWN;
    }

    private static Rarity getRarityFor(EnumMatterType matterType) {
        return matterType == EnumMatterType.RED_MATTER ? Rarity.EPIC : Rarity.RARE;
    }
}
