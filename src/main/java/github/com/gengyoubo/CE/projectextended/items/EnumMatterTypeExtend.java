package github.com.gengyoubo.CE.projectextended.items;

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

import java.util.Collections;
import java.util.List;

public enum EnumMatterTypeExtend implements StringRepresentable, Tier {
    TRUE();
    private final String name;
    private final float attackDamage;
    private final float efficiency;
    private final float chargeModifier;
    private final int harvestLevel;
    private final TagKey<Block> neededTag;
    private final MapColor mapColor;
    EnumMatterTypeExtend() {
        this.name = "true_matter";
        this.attackDamage = (float) 5.0;
        this.efficiency = (float) 14.0;
        this.chargeModifier = (float) 12.0;
        this.harvestLevel = 4;
        this.neededTag = PETags.Blocks.NEEDS_DARK_MATTER_TOOL;
        this.mapColor = MapColor.COLOR_BLACK;
        TierSortingRegistry.registerTier(this, PECore.rl("true_matter"), List.of(Tiers.NETHERITE), Collections.emptyList());
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
