package github.com.gengyoubo.projectextended.items;

import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.PETags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.TierSortingRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public enum EnumMatterTypeExtend implements StringRepresentable, Tier {
    TRUE("true_matter", 5.0F, 14.0F, 12.0F, 4,PETags.Blocks.NEEDS_DARK_MATTER_TOOL, Tiers.NETHERITE, (ResourceLocation)null, MapColor.COLOR_BLACK);
    private final String name;
    private final float attackDamage;
    private final float efficiency;
    private final float chargeModifier;
    private final int harvestLevel;
    private final TagKey<Block> neededTag;
    private final MapColor mapColor;
    private EnumMatterTypeExtend(String name, float attackDamage, float efficiency, float chargeModifier, int harvestLevel, @Nullable TagKey<Block> neededTag, Tier previous, ResourceLocation next, MapColor mapColor) {
        this.name = name;
        this.attackDamage = attackDamage;
        this.efficiency = efficiency;
        this.chargeModifier = chargeModifier;
        this.harvestLevel = harvestLevel;
        this.neededTag = neededTag;
        this.mapColor = mapColor;
        TierSortingRegistry.registerTier(this, PECore.rl(name), List.of(previous), next == null ? Collections.emptyList() : List.of(next));
    }
    public @NotNull String getSerializedName() {
        return this.name;
    }

    public String toString() {
        return this.getSerializedName();
    }

    public int getUses() {
        return 0;
    }

    public float getChargeModifier() {
        return this.chargeModifier;
    }

    public float getSpeed() {
        return this.efficiency;
    }

    public float getAttackDamageBonus() {
        return this.attackDamage;
    }

    @SuppressWarnings("deprecation")
    public int getLevel() {
        return this.harvestLevel;
    }

    public int getEnchantmentValue() {
        return 0;
    }

    public @NotNull Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }

    public MapColor getMapColor() {
        return this.mapColor;
    }

    public int getMatterTier() {
        return this.ordinal();
    }

    public @NotNull TagKey<Block> getTag() {
        return this.neededTag;
    }
}
