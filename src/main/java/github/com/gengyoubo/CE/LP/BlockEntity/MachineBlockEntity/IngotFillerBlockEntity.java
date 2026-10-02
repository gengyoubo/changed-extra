package github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity;

import github.com.gengyoubo.CE.LP.BlockEntity.BaseEnergyBlockEntity;
import github.com.gengyoubo.CE.LP.init.CELPBlockEntity;
import github.com.gengyoubo.CE.LP.recipe.CELPRecipes;
import github.com.gengyoubo.CE.LP.recipe.IngotFillingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public final class IngotFillerBlockEntity extends BaseEnergyBlockEntity {
    public static final int CAPACITY=100000,TANK_CAPACITY=10000;
    private int progress,costRemainder,fluidRemainder,processTicks,lpPerSecond,fluidAmount;
    private ResourceLocation runningRecipe;
    private ItemStack runningInput=ItemStack.EMPTY;
    private final ItemStackHandler items=new ItemStackHandler(2) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
        @Override public boolean isItemValid(int slot,ItemStack stack) { return slot==0 && isValidInput(level,stack); }
    };
    private final IItemHandler automation=new IItemHandler() {
        @Override public int getSlots() { return 2; }
        @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot,ItemStack stack) { return items.isItemValid(slot,stack); }
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) {
            return isItemValid(slot,stack) ? items.insertItem(slot,stack,simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate) {
            return slot==1 ? items.extractItem(slot,amount,simulate) : ItemStack.EMPTY;
        }
    };
    private final FluidTank tank=new FluidTank(TANK_CAPACITY) {
        @Override public boolean isFluidValid(FluidStack stack) {
            return level!=null && level.getRecipeManager().getAllRecipesFor(CELPRecipes.INGOT_FILLING_TYPE).stream().anyMatch(r->r.acceptsFluid(stack));
        }
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private final IFluidHandler inputTank=new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int index) { return tank.getFluidInTank(index); }
        @Override public int getTankCapacity(int index) { return TANK_CAPACITY; }
        @Override public boolean isFluidValid(int index,FluidStack fluid) { return tank.isFluidValid(fluid); }
        @Override public int fill(FluidStack fluid,FluidAction action) { return tank.fill(fluid,action); }
        @Override public FluidStack drain(FluidStack fluid,FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int amount,FluidAction action) { return FluidStack.EMPTY; }
    };
    private LazyOptional<IItemHandler> itemCap=LazyOptional.of(()->automation);
    private LazyOptional<IFluidHandler> fluidCap=LazyOptional.of(()->inputTank);
    public IngotFillerBlockEntity(BlockPos pos,BlockState state) { super(CELPBlockEntity.INGOT_FILLER.get(),pos,state,CAPACITY); }
    public static boolean isValidInput(Level level,ItemStack input) {
        return level!=null && level.getRecipeManager().getAllRecipesFor(CELPRecipes.INGOT_FILLING_TYPE).stream().anyMatch(r->r.acceptsInput(input));
    }
    public ItemStackHandler getItemHandler() { return items; }
    public FluidStack getStoredFluid() { return tank.getFluid().copy(); }
    public int getProgress() { return progress; }
    public int getProcessTicks() { return processTicks; }
    public int getLpPerSecond() { return lpPerSecond; }
    public int getFluidPerSecond() { return processTicks>0 ? (int)((long)fluidAmount*20/processTicks) : 0; }
    public void clearStoredFluid() {
        if(level==null || level.isClientSide)return;
        tank.setFluid(FluidStack.EMPTY);reset();setChanged();
    }
    private void reset() {
        if(runningRecipe!=null || progress!=0)setChanged();
        runningRecipe=null;runningInput=ItemStack.EMPTY;progress=costRemainder=fluidRemainder=processTicks=lpPerSecond=fluidAmount=0;
    }
    @Override public void tick() {
        if(level==null || level.isClientSide)return;
        // An empty tank pauses the current recipe instead of discarding paid progress.
        IngotFillingRecipe recipe=runningRecipe==null ? null : level.getRecipeManager().byKey(runningRecipe)
                .filter(r->r instanceof IngotFillingRecipe).map(r->(IngotFillingRecipe)r)
                .filter(r->r.acceptsInput(items.getStackInSlot(0)) && (tank.isEmpty() || r.acceptsFluid(tank.getFluid()))).orElse(null);
        if(recipe==null)recipe=level.getRecipeManager().getAllRecipesFor(CELPRecipes.INGOT_FILLING_TYPE).stream()
                .filter(r->r.acceptsInput(items.getStackInSlot(0)) && r.acceptsFluid(tank.getFluid())).findFirst().orElse(null);
        if(recipe==null) { reset();return; }
        if(!recipe.getId().equals(runningRecipe) || !ItemStack.isSameItemSameTags(runningInput,items.getStackInSlot(0))
                || recipe.getProcessTicks()!=processTicks || recipe.getLpPerSecond()!=lpPerSecond || recipe.getFluidAmount()!=fluidAmount) {
            reset();runningRecipe=recipe.getId();runningInput=items.getStackInSlot(0).copyWithCount(1);
            processTicks=recipe.getProcessTicks();lpPerSecond=recipe.getLpPerSecond();fluidAmount=recipe.getFluidAmount();setChanged();
        }
        var result=recipe.getResultItem(level.registryAccess());var output=items.getStackInSlot(1);
        int limit=Math.min(items.getSlotLimit(1),result.getMaxStackSize());
        if((!output.isEmpty() && !ItemStack.isSameItemSameTags(output,result))
                || output.getCount()+result.getCount()>limit)return;
        long accumulated=(long)costRemainder+lpPerSecond;
        int cost=(int)(accumulated/20),remainder=(int)(accumulated%20);
        if(progress+1>=processTicks && remainder>0) { cost++;remainder=0; }
        long fluidAccumulated=(long)fluidRemainder+recipe.getFluidPerSecond();
        int fluidCost=(int)(fluidAccumulated/20),nextFluidRemainder=(int)(fluidAccumulated%20);
        if(tank.isEmpty() || tank.getFluidAmount()<fluidCost || getEnergyStored()<cost)return;
        extractEnergy(cost,null);tank.drain(fluidCost,IFluidHandler.FluidAction.EXECUTE);
        costRemainder=remainder;fluidRemainder=nextFluidRemainder;progress++;
        if(progress>=processTicks) {
            items.extractItem(0,1,false);
            items.setStackInSlot(1,output.isEmpty() ? result.copy() : output.copyWithCount(output.getCount()+result.getCount()));
            progress=costRemainder=fluidRemainder=0;
        }
        setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);tag.put("Inventory",items.serializeNBT());tag.put("FillingTank",tank.writeToNBT(new CompoundTag()));
        tag.putInt("Progress",progress);tag.putInt("CostRemainder",costRemainder);tag.putInt("ProcessTicks",processTicks);
        tag.putInt("FluidRemainder",fluidRemainder);
        tag.putInt("LpPerSecond",lpPerSecond);tag.putInt("FluidAmount",fluidAmount);
        if(runningRecipe!=null)tag.putString("RunningRecipe",runningRecipe.toString());
        tag.put("RunningInput",runningInput.save(new CompoundTag()));
    }
    @Override public void load(CompoundTag tag) {
        energy.extractEnergy(Integer.MAX_VALUE,null);super.load(tag);
        items.deserializeNBT(tag.getCompound("Inventory"));tank.readFromNBT(tag.getCompound("FillingTank"));
        progress=Math.max(0,tag.getInt("Progress"));costRemainder=Math.floorMod(tag.getInt("CostRemainder"),20);
        fluidRemainder=Math.floorMod(tag.getInt("FluidRemainder"),20);
        processTicks=Math.max(0,tag.getInt("ProcessTicks"));lpPerSecond=Math.max(0,tag.getInt("LpPerSecond"));fluidAmount=Math.max(0,tag.getInt("FluidAmount"));
        runningRecipe=ResourceLocation.tryParse(tag.getString("RunningRecipe"));runningInput=ItemStack.of(tag.getCompound("RunningInput"));
    }
    @Override public void invalidateCaps() { super.invalidateCaps();itemCap.invalidate();fluidCap.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps();itemCap=LazyOptional.of(()->automation);fluidCap=LazyOptional.of(()->inputTank); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability,Direction side) {
        if(capability==ForgeCapabilities.ITEM_HANDLER)return itemCap.cast();
        if(capability==ForgeCapabilities.FLUID_HANDLER)return fluidCap.cast();
        return super.getCapability(capability,side);
    }
}
