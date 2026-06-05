package github.com.gengyoubo.CE.items;

import github.com.gengyoubo.CE.init.CEItem;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public enum CEArmorMaterials implements ArmorMaterial, StringRepresentable {
    PLATE(
            new int[]{1, 4, 5, 1},
            () -> Ingredient.of(CEItem.PLATE.get())
    );

    private final String name;
    private final int durabilityMultiplier;
    private final int[] slotProtections;
    private final int enchantmentValue;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repairIngredient;

    CEArmorMaterials(
            int[] slotProtections,
            Supplier<Ingredient> repairIngredient
    ) {
        this.name = "iron";
        this.durabilityMultiplier = 11;
        this.slotProtections = slotProtections;
        this.enchantmentValue = 8;
        this.equipSound = SoundEvents.ARMOR_EQUIP_IRON;
        this.toughness = (float) 0.0;
        this.knockbackResistance = (float) 0.0;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        int baseDurability = switch (type) {
            case HELMET -> 11;
            case CHESTPLATE -> 16;
            case LEGGINGS -> 15;
            case BOOTS -> 13;
        };
        return baseDurability * durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> slotProtections[0];
            case CHESTPLATE -> slotProtections[1];
            case LEGGINGS -> slotProtections[2];
            case BOOTS -> slotProtections[3];
        };
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public @NotNull SoundEvent getEquipSound() {
        return equipSound;
    }

    @Override
    public @NotNull Ingredient getRepairIngredient() {
        return repairIngredient.get();
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return knockbackResistance;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
