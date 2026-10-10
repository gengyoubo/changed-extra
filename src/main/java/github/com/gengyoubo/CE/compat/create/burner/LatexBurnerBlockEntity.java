package github.com.gengyoubo.CE.compat.create.burner;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LatexBurnerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final TagKey<Item> ITEM_FUELS = TagKey.create(Registries.ITEM, ResourceLocation.parse("changede:latex_burner_fuels"));
    public static final TagKey<Fluid> FLUID_FUELS = TagKey.create(Registries.FLUID, ResourceLocation.parse("changede:latex_burner_fuels"));
    private CompoundTag creature = new CompoundTag();
    private String creatureType = "", runtimeLatex = "none", customName = "";
    private int itemBurnTicks, fluidBurnTicks;
    private boolean dirtySummary;
    private LatexBurnerRules.Profile clientProfile;
    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot, ItemStack stack) { return !isRemoved() && hasCreature() && profile() != null && stack.is(ITEM_FUELS); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return isRemoved() ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
        }
        @Override protected void onContentsChanged(int slot) { contentsChanged(); }
    };
    private final FluidTank tank = new FluidTank(4000) {
        @Override public boolean isFluidValid(FluidStack stack) {
            return !isRemoved() && hasCreature() && profile() != null && stack.getFluid().is(FLUID_FUELS);
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            return isRemoved() ? FluidStack.EMPTY : super.drain(amount, action);
        }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) {
            return isRemoved() ? FluidStack.EMPTY : super.drain(stack, action);
        }
        @Override protected void onContentsChanged() { contentsChanged(); }
    };
    private LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> items);
    private LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> tank);

    public LatexBurnerBlockEntity(BlockPos pos, BlockState state) { super(LatexBurnerCompat.ENTITY.get(), pos, state); }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}
    private void contentsChanged() { setChanged(); dirtySummary = true; }
    public LatexBurnerRules.Profile profile() {
        return level != null && level.isClientSide && clientProfile != null ? clientProfile : LatexBurnerProfiles.find(creatureType, runtimeLatex);
    }
    public boolean hasCreature() { return !creatureType.isEmpty() && ResourceLocation.tryParse(creatureType) != null; }
    public String creatureType() { return creatureType; }
    public HeatLevel heat() {
        if (profile() == null) return HeatLevel.NONE;
        return switch (LatexBurnerRules.heat(hasCreature(), itemBurnTicks, fluidBurnTicks)) {
            case 1 -> HeatLevel.SMOULDERING;
            case 2 -> HeatLevel.KINDLED;
            case 3 -> HeatLevel.SEETHING;
            default -> HeatLevel.NONE;
        };
    }
    public int itemTicks() { return itemBurnTicks; }
    public int fluidTicks() { return fluidBurnTicks; }
    public IItemHandler items() { return items; }
    public IFluidHandler fluids() { return tank; }

    @Override public void tick() {
        super.tick();
        if (level == null || level.isClientSide || isVirtual()) return;
        LatexBurnerRules.Profile profile = profile();
        if (profile != null) {
            if (itemBurnTicks > 0) { itemBurnTicks--; setChanged(); }
            if (fluidBurnTicks > 0) { fluidBurnTicks--; setChanged(); }
            if (itemBurnTicks == 0 && items.getStackInSlot(0).is(ITEM_FUELS)) {
                items.extractItem(0, 1, false);
                itemBurnTicks = profile.fuelTicks();
                contentsChanged();
            }
            if (fluidBurnTicks == 0 && tank.getFluidAmount() >= LatexBurnerRules.FLUID_PER_FUEL && tank.getFluid().getFluid().is(FLUID_FUELS)) {
                tank.drain(LatexBurnerRules.FLUID_PER_FUEL, IFluidHandler.FluidAction.EXECUTE);
                fluidBurnTicks = profile.fuelTicks();
                contentsChanged();
            }
        }
        updateHeat();
        if (dirtySummary || (hasCreature() && level.getGameTime() % 20 == 0)) { sendData(); dirtySummary = false; }
    }
    public void updateHeat() {
        if (level == null || level.isClientSide || !getBlockState().hasProperty(BlazeBurnerBlock.HEAT_LEVEL)) return;
        HeatLevel next = heat();
        if (getBlockState().getValue(BlazeBurnerBlock.HEAT_LEVEL) == next && getBlockState().getValue(LatexBurnerBlock.OCCUPIED) == hasCreature()) return;
        level.setBlock(worldPosition, getBlockState().setValue(BlazeBurnerBlock.HEAT_LEVEL, next).setValue(LatexBurnerBlock.OCCUPIED, hasCreature()), 3);
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        if (level.getBlockEntity(worldPosition.above()) instanceof BasinBlockEntity basin) basin.notifyChangeOfContents();
        contentsChanged();
    }
    @Override protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putString("CreatureType", creatureType);
        tag.putString("RuntimeLatex", runtimeLatex);
        tag.putString("CreatureName", customName);
        var profile = profile();
        if (clientPacket && profile != null) {
            tag.putString("BurnerLatex", profile.latex()); tag.putInt("SpeedUnits", profile.speedUnits()); tag.putInt("FuelDuration", profile.fuelTicks());
        }
        if (!clientPacket) tag.put("Creature", creature.copy());
        tag.put("ItemFuel", items.serializeNBT());
        tag.put("FluidFuel", tank.writeToNBT(new CompoundTag()));
        tag.putInt("ItemBurnTicks", itemBurnTicks);
        tag.putInt("FluidBurnTicks", fluidBurnTicks);
    }
    @Override protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (!clientPacket) creature = tag.getCompound("Creature").copy();
        creatureType = clientPacket ? tag.getString("CreatureType") : creature.getString("id");
        runtimeLatex = tag.getString("RuntimeLatex");
        customName = clientPacket ? tag.getString("CreatureName") : creature.getString("CustomName");
        clientProfile = null;
        if (clientPacket && tag.contains("SpeedUnits")) {
            try { clientProfile = new LatexBurnerRules.Profile(tag.getString("BurnerLatex"), tag.getInt("SpeedUnits"), tag.getInt("FuelDuration")); }
            catch (IllegalArgumentException ignored) { }
        }
        CompoundTag storedItems = tag.getCompound("ItemFuel").copy();
        storedItems.putInt("Size", 1);
        items.deserializeNBT(storedItems);
        tank.readFromNBT(tag.getCompound("FluidFuel"));
        itemBurnTicks = Math.max(0, Math.min(LatexBurnerRules.MAX_FUEL_TICKS, tag.getInt("ItemBurnTicks")));
        fluidBurnTicks = Math.max(0, Math.min(LatexBurnerRules.MAX_FUEL_TICKS, tag.getInt("FluidBurnTicks")));
    }
    /** Schematics never carry a paid fuel buffer or a captive entity into a new machine. */
    @Override public void writeSafe(CompoundTag tag) { super.writeSafe(tag); }
    public ItemStack carriedStack() {
        ItemStack stack = new ItemStack(hasCreature() ? LatexBurnerCompat.FILLED.get() : LatexBurnerCompat.EMPTY.get());
        if (hasCreature()) stack.getOrCreateTag().put("BlockEntityTag", saveWithoutMetadata());
        return stack;
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); itemCapability.invalidate(); fluidCapability.invalidate(); }
    @Override public void reviveCaps() {
        super.reviveCaps();
        itemCapability = LazyOptional.of(() -> items); fluidCapability = LazyOptional.of(() -> tank);
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (isRemoved()) return LazyOptional.empty();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCapability.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCapability.cast();
        return super.getCapability(cap, side);
    }
    private static Component creatureName(String id, String name) {
        if (!name.isEmpty()) {
            try { Component custom = Component.Serializer.fromJson(name); if (custom != null) return custom; }
            catch (RuntimeException ignored) { }
        }
        ResourceLocation key = ResourceLocation.tryParse(id);
        var type = key == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(key);
        return type == null ? Component.literal(id) : type.getDescription();
    }
    public static List<Component> itemSummary(CompoundTag tag) {
        String id = tag.getCompound("Creature").getString("id");
        var profile = LatexBurnerProfiles.find(id, tag.getString("RuntimeLatex"));
        if (profile == null) return List.of();
        return List.of(Component.translatable("changede.latex_burner.creature", creatureName(id, tag.getCompound("Creature").getString("CustomName"))),
                Component.translatable("changede.latex_burner.speed", String.format(Locale.ROOT, "%.2f", profile.speed())));
    }
    public List<Component> summary() {
        List<Component> lines = new ArrayList<>();
        if (!hasCreature()) { lines.add(Component.translatable("changede.latex_burner.capture_hint")); return lines; }
        lines.add(Component.translatable("changede.latex_burner.creature", creatureName(creatureType, customName)));
        var profile = profile();
        if (profile == null) { lines.add(Component.translatable("changede.latex_burner.unsupported")); return lines; }
        String grade = profile.speedUnits() < 4 ? "young" : profile.speedUnits() < 6 ? "basic" : profile.speedUnits() < 8 ? "advanced" : "elite";
        lines.add(Component.translatable("changede.latex_burner.grade", Component.translatable("changede.latex_burner.grade." + grade)));
        lines.add(Component.translatable("changede.latex_burner.speed", String.format(Locale.ROOT, "%.2f", profile.speed())));
        lines.add(Component.translatable("changede.latex_burner.heat", Component.translatable("changede.latex_burner.heat." + heat().getSerializedName())));
        lines.add(Component.translatable("changede.latex_burner.item_time", (itemBurnTicks + 19) / 20));
        lines.add(Component.translatable("changede.latex_burner.fluid_time", (fluidBurnTicks + 19) / 20));
        if (!items.getStackInSlot(0).isEmpty()) lines.add(Component.translatable("changede.latex_burner.items", items.getStackInSlot(0).getHoverName(), items.getStackInSlot(0).getCount()));
        if (!tank.isEmpty()) lines.add(Component.translatable("changede.latex_burner.fluid", tank.getFluid().getDisplayName(), tank.getFluidAmount()));
        return lines;
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) { tooltip.addAll(summary()); return true; }
}
