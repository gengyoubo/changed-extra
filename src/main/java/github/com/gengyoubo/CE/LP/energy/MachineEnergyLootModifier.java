package github.com.gengyoubo.CE.LP.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Enriches the existing block drop; never creates an additional drop or copies machine inventories. */
public final class MachineEnergyLootModifier extends LootModifier {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "changede");
    public static final RegistryObject<Codec<MachineEnergyLootModifier>> CODEC = SERIALIZERS.register(
            "preserve_machine_energy", () -> RecordCodecBuilder.create(instance -> codecStart(instance).apply(instance, MachineEnergyLootModifier::new)));

    public MachineEnergyLootModifier(LootItemCondition[] conditions) { super(conditions); }

    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        var blockEntity = context.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        if (blockEntity == null) return loot;
        var energy = MachineEnergyPersistence.capture(blockEntity);
        if (energy.isEmpty()) return loot;
        var item = blockEntity.getBlockState().getBlock().asItem();
        for (ItemStack stack : loot) {
            if (stack.is(item) && stack.getCount() == 1) {
                MachineEnergyPersistence.attach(stack, energy);
                break; // Only one original block item can carry the owner's stored energy.
            }
        }
        return loot;
    }

    @Override public Codec<? extends IGlobalLootModifier> codec() { return CODEC.get(); }
}
