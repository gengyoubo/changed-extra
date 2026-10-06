package github.com.gengyoubo.CE.LP.energy;

import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.ILatexTypedEnergyHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;

/** Copies only energy state into BlockEntityTag, which vanilla restores when placing a BlockItem. */
public final class MachineEnergyPersistence {
    private static final Set<String> NAMESPACES = Set.of("changede", "changed", "changed_addon");
    public static final List<String> ENERGY_KEYS = List.of("Energy", "PipeEnergy", "TypedEnergy", "LP", "WLP", "DLP",
            "LpEnergy", "JouleBuffer", "CeStoredLp", "CeInputAccumulator", "CeOutputDebt", WorkbenchEnergyRules.NBT_KEY);
    private static final List<String> CAPACITY_KEYS = List.of("CeRpm", "CeSu");

    private MachineEnergyPersistence() { }

    public static CompoundTag capture(BlockEntity blockEntity) {
        CompoundTag energy = new CompoundTag();
        if (blockEntity == null || !(blockEntity instanceof ILatexEnergyHandler
                || blockEntity instanceof ILatexTypedEnergyHandler || blockEntity instanceof WorkbenchEnergyHolder)) return energy;
        var id = ForgeRegistries.BLOCKS.getKey(blockEntity.getBlockState().getBlock());
        if (id == null || !NAMESPACES.contains(id.getNamespace())) return energy;
        CompoundTag saved = blockEntity.saveWithoutMetadata();
        for (String key : ENERGY_KEYS) {
            if (saved.contains(key, Tag.TAG_ANY_NUMERIC)) energy.put(key, saved.get(key).copy());
        }
        if (!energy.isEmpty()) {
            // CE storage capacity depends on these settings; retaining them avoids clamping away stored energy.
            for (String key : CAPACITY_KEYS) {
                if (saved.contains(key, Tag.TAG_ANY_NUMERIC)) energy.put(key, saved.get(key).copy());
            }
        }
        return energy;
    }

    public static void attach(ItemStack stack, CompoundTag energy) {
        if (stack.isEmpty() || energy.isEmpty() || !(stack.getItem() instanceof BlockItem)) return;
        CompoundTag existing = BlockItem.getBlockEntityData(stack);
        CompoundTag data = existing == null ? new CompoundTag() : existing.copy();
        data.merge(energy);
        stack.getOrCreateTag().put("BlockEntityTag", data);
    }
}
