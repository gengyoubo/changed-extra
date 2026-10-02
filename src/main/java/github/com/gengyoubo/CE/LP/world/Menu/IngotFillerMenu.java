package github.com.gengyoubo.CE.LP.world.Menu;

import github.com.gengyoubo.CE.LP.BlockEntity.MachineBlockEntity.IngotFillerBlockEntity;
import github.com.gengyoubo.CE.LP.init.CELPBlock;
import github.com.gengyoubo.CE.LP.recipe.IngotFillingDiagnostics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

@SuppressWarnings("deprecation")
public final class IngotFillerMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT=18;
    private final Level level;
    private final BlockPos pos;
    private final ContainerData data;
    private final String recipeErrors;
    public IngotFillerMenu(int id,Inventory inventory,FriendlyByteBuf buffer) { this(id,inventory,buffer.readBlockPos(),new SimpleContainerData(DATA_COUNT),buffer.readUtf()); }
    public IngotFillerMenu(int id,Inventory inventory,BlockPos pos) { this(id,inventory,pos,createData(inventory.player.level(),pos),IngotFillingDiagnostics.getDisplay()); }
    private IngotFillerMenu(int id,Inventory inventory,BlockPos pos,ContainerData data,String recipeErrors) {
        super(CEMenus.INGOT_FILLER.get(),id);level=inventory.player.level();this.pos=pos;this.data=data;
        this.recipeErrors=recipeErrors;
        IItemHandler items=level.getBlockEntity(pos) instanceof IngotFillerBlockEntity machine ? machine.getItemHandler() : new ItemStackHandler(2);
        addSlot(new SlotItemHandler(items,0,35,35) {
            @Override public boolean mayPlace(ItemStack stack) { return IngotFillerBlockEntity.isValidInput(level,stack) && super.mayPlace(stack); }
        });
        addSlot(new SlotItemHandler(items,1,125,35) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+(row+1)*9,8+col*18,102+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,160));
        checkContainerDataCount(data,DATA_COUNT);addDataSlots(data);
    }
    private static ContainerData createData(Level level,BlockPos pos) {
        return new ContainerData() {
            @Override public int get(int index) {
                if(!(level.getBlockEntity(pos) instanceof IngotFillerBlockEntity machine))return 0;
                int value=switch(index/2) {
                    case 0->machine.getEnergyStored();case 1->machine.getMaxEnergyStored();
                    case 2->machine.getProgress();case 3->machine.getProcessTicks();case 4->machine.getLpPerSecond();
                    case 5->machine.getStoredFluid().getAmount();case 6->IngotFillerBlockEntity.TANK_CAPACITY;
                    case 7->BuiltInRegistries.FLUID.getId(machine.getStoredFluid().getFluid());case 8->machine.getFluidPerSecond();default->0;
                };
                return index%2==0 ? value&0xFFFF : value>>>16;
            }
            @Override public void set(int index,int value) { }
            @Override public int getCount() { return DATA_COUNT; }
        };
    }
    private int value(int index) { return (data.get(index*2)&0xFFFF)|((data.get(index*2+1)&0xFFFF)<<16); }
    public int getEnergyStored() { return value(0); }
    public int getMaxEnergyStored() { return value(1); }
    public int getProgress() { return value(2); }
    public int getProcessTicks() { return value(3); }
    public int getLpPerSecond() { return value(4); }
    public int getFluidAmount() { return value(5); }
    public int getTankCapacity() { return value(6); }
    public int getFluidPerSecond() { return value(8); }
    public String getRecipeErrors() { return recipeErrors; }
    public FluidStack getStoredFluid() {
        var fluid=BuiltInRegistries.FLUID.byId(value(7));return fluid==null || getFluidAmount()<=0 ? FluidStack.EMPTY : new FluidStack(fluid,getFluidAmount());
    }
    @Override public boolean stillValid(Player player) { return stillValid(ContainerLevelAccess.create(level,pos),player,CELPBlock.INGOT_FILLER.get()); }
    @Override public boolean clickMenuButton(Player player,int button) {
        if(button!=0 || level.isClientSide || !stillValid(player) || !(level.getBlockEntity(pos) instanceof IngotFillerBlockEntity machine))return false;
        machine.clearStoredFluid();broadcastChanges();return true;
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0 || index>=slots.size() || !slots.get(index).hasItem())return ItemStack.EMPTY;
        var slot=slots.get(index);var stack=slot.getItem();var original=stack.copy();
        if(index<2) { if(!moveItemStackTo(stack,2,slots.size(),true))return ItemStack.EMPTY; }
        else if(!IngotFillerBlockEntity.isValidInput(level,stack) || !moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;
        if(stack.getCount()==original.getCount())return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return original;
    }
}
