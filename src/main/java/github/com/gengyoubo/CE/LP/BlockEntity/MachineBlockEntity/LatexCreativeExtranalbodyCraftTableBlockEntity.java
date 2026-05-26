package github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity;

import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import github.com.gengyoubo.CE.LP.recipe.CELPRecipes;
import github.com.gengyoubo.CE.LP.recipe.LatexCreativeExtranalbodyCraftingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class LatexCreativeExtranalbodyCraftTableBlockEntity extends MachineBlockEntity {
    private static final int CAPACITY = 30_000;
    private static final int DEFAULT_ENERGY_COST = 30;
    private static final int DEFAULT_PROCESS_TIME = 160;

    private final ItemStackHandler itemHandler = new ItemStackHandler(10) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> itemHandler);

    public LatexCreativeExtranalbodyCraftTableBlockEntity(BlockPos pos, BlockState state) {
        super(CELPBlockEntity.LATEX_CREATIVE_EXTRANALBODY_CRAFT_TABLE_BLOCK_ENTITY.get(), pos, state, CAPACITY);
    }

    @Override
    protected int getEnergyCost() {
        return getCurrentRecipe().map(LatexCreativeExtranalbodyCraftingRecipe::getEnergyCost).orElse(DEFAULT_ENERGY_COST);
    }

    @Override
    protected int getMaxProgress() {
        return getCurrentRecipe().map(LatexCreativeExtranalbodyCraftingRecipe::getProcessTime).orElse(DEFAULT_PROCESS_TIME);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerCap.invalidate();
        itemHandlerCap = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandlerCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected boolean canProcess() {
        Optional<LatexCreativeExtranalbodyCraftingRecipe> recipe = getCurrentRecipe();
        if (recipe.isEmpty()) {
            return false;
        }

        assert level != null;
        ItemStack result = recipe.get().getResultItem(level.registryAccess());
        ItemStack output = itemHandler.getStackInSlot(9);

        if (output.isEmpty()) {
            return true;
        }

        if (!ItemStack.isSameItemSameTags(output, result)) {
            return false;
        }

        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    @Override
    protected void processItem() {
        Optional<LatexCreativeExtranalbodyCraftingRecipe> recipe = getCurrentRecipe();
        if (recipe.isEmpty()) {
            return;
        }

        assert level != null;
        ItemStack result = recipe.get().getResultItem(level.registryAccess()).copy();
        NonNullList<Ingredient> ingredients = recipe.get().getIngredientsGrid();
        int width = recipe.get().getRecipeWidth();
        int height = recipe.get().getRecipeHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int recipeIndex = x + y * width;
                if (ingredients.get(recipeIndex) != Ingredient.EMPTY) {
                    int slot = x + y * 3;
                    itemHandler.extractItem(slot, 1, false);
                }
            }
        }

        ItemStack output = itemHandler.getStackInSlot(9);
        if (output.isEmpty()) {
            itemHandler.setStackInSlot(9, result);
        } else {
            output.grow(result.getCount());
        }

        setChanged();
    }

    private Optional<LatexCreativeExtranalbodyCraftingRecipe> getCurrentRecipe() {
        if (level == null) {
            return Optional.empty();
        }

        SimpleContainer inventory = new SimpleContainer(9);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        return level.getRecipeManager()
                .getAllRecipesFor(CELPRecipes.LATEX_CREATIVE_EXTRANALBODY_CRAFTING_TYPE)
                .stream()
                .filter(recipe -> recipe.getId().getPath().startsWith("lectb/"))
                .filter(recipe -> recipe.matches(inventory, level))
                .findFirst();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putInt("Progress", progress);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        progress = tag.getInt("Progress");
    }
}
