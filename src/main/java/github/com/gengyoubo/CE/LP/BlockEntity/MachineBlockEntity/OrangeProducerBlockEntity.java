package github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity;

import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OrangeProducerBlockEntity extends BlockEntity implements ILatexEnergyHandler {
    public static final int LP_CAPACITY = 10_000;
    private static final int LP_PER_SECOND = 200;
    private static final int TICKS_PER_ORANGE = 200;
    private static final ResourceLocation ORANGE_ID = ResourceLocation.parse("changed:orange");

    private int lp;
    private int progress;
    private final ItemStackHandler output = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
    };
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> output);

    public OrangeProducerBlockEntity(BlockPos pos, BlockState state) {
        super(CELPBlockEntity.ORANGE_PRODUCER.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide || lp < LP_PER_SECOND / 20 || !canOutputOrange()) return;
        lp -= LP_PER_SECOND / 20;
        progress++;
        boolean produced = false;
        if (progress >= TICKS_PER_ORANGE) {
            progress = 0;
            Item orange = ForgeRegistries.ITEMS.getValue(ORANGE_ID);
            if (orange != null) {
                ItemStack stored = output.getStackInSlot(0);
                if (stored.isEmpty()) output.setStackInSlot(0, new ItemStack(orange));
                else stored.grow(1);
                produced = true;
            }
        }
        setChanged();
        if (produced) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private boolean canOutputOrange() {
        Item orange = ForgeRegistries.ITEMS.getValue(ORANGE_ID);
        if (orange == null) return false;
        ItemStack stack = output.getStackInSlot(0);
        return stack.isEmpty() || (stack.is(orange) && stack.getCount() < Math.min(64, stack.getMaxStackSize()));
    }

    public int getOrangeCount() { return output.getStackInSlot(0).getCount(); }
    public int getProgress() { return progress; }

    @Override public int receiveEnergy(int amount, Direction from) {
        int received = Math.min(Math.max(amount, 0), LP_CAPACITY - lp);
        lp += received;
        if (received > 0) setChanged();
        return received;
    }
    @Override public int extractEnergy(int amount, Direction from) { return 0; }
    @Override public int getEnergyStored() { return lp; }
    @Override public int getMaxEnergyStored() { return LP_CAPACITY; }

    @Override protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("LP", lp);
        tag.putInt("Progress", progress);
        tag.put("Output", output.serializeNBT());
    }
    @Override public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        lp = Math.min(Math.max(tag.getInt("LP"), 0), LP_CAPACITY);
        progress = Math.min(Math.max(tag.getInt("Progress"), 0), TICKS_PER_ORANGE - 1);
        output.deserializeNBT(tag.getCompound("Output"));
    }
    @Override public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
        itemCapability = LazyOptional.of(() -> output);
    }
    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        return super.getCapability(capability, side);
    }
}
