package github.com.gengyoubo.CE.LP.mixins;

import github.com.gengyoubo.CE.LP.energy.WorkbenchEnergyHolder;
import github.com.gengyoubo.CE.LP.energy.WorkbenchMekanismEnergyHandler;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.capabilities.Capabilities;
import net.ltxprogrammer.changed.block.entity.PurifierBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Purifier inherits these capability methods, so there is no local injection target. */
@Mixin(value = PurifierBlockEntity.class, remap = false)
public abstract class PurifierMekanismEnergyMixin extends BaseContainerBlockEntity {
    @Unique private LazyOptional<IStrictEnergyHandler> changede$strictEnergy = changede$newStrictEnergy();

    protected PurifierMekanismEnergyMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Unique
    private LazyOptional<IStrictEnergyHandler> changede$newStrictEnergy() {
        return LazyOptional.of(() -> new WorkbenchMekanismEnergyHandler((WorkbenchEnergyHolder) (Object) this));
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == Capabilities.STRICT_ENERGY) return changede$strictEnergy.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        changede$strictEnergy.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        changede$strictEnergy = changede$newStrictEnergy();
    }
}
