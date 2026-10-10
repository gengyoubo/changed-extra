package github.com.gengyoubo.CE.compat.tinkers.smeltery;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer;
import slimeknights.tconstruct.smeltery.item.TankItem;
import slimeknights.tconstruct.smeltery.item.TankItemFluidHandler;

public final class LatexFuelTankItem extends TankItem {
    public LatexFuelTankItem(Block block) { super(block, new Properties().stacksTo(1), false); }
    @Override public FluidTank getTank(ItemStack stack) {
        return super.getTank(stack).setValidator(fluid -> LatexSmelteryRules.isLatex(fluid.getFluid()));
    }
    @Override public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) {
        return new TankItemFluidHandler(this, stack) {
            @Override public boolean isFluidValid(int tank, FluidStack fluid) { return LatexSmelteryRules.isLatex(fluid.getFluid()); }
        };
    }
    @Override public boolean overrideStackedOnOther(ItemStack held, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        ItemStack input = slot.getItem();
        if (input.isEmpty() || input.getCount() != 1 || !mayHaveFluid(input)) return false;
        FluidTank tank = getTank(held);
        var result = FluidTransferHelper.interactWithStack(tank, input, IFluidContainerTransfer.TransferDirection.REVERSE);
        if (result == null) return false;
        if (player.level().isClientSide) player.playSound(result.getSound());
        slot.set(FluidTransferHelper.getOrTransferFilled(player, input, result.stack()));
        setTank(held, tank);
        return true;
    }
}
