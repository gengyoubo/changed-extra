package github.com.gengyoubo.CE.compat.mekanism.gas;

import github.com.gengyoubo.CE.LP.ILatexMultiEnergyHandler;
import github.com.gengyoubo.CE.LP.LatexEnergyType;
import github.com.gengyoubo.CE.LP.TypedLatexEnergyStorage;
import github.com.gengyoubo.CE.LP.energy.DualEnergyProcess;
import java.util.Comparator;
import java.util.Map;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.RelativeSide;
import mekanism.api.chemical.ChemicalTankBuilder;
import mekanism.api.chemical.attribute.ChemicalAttributeValidator;
import mekanism.api.chemical.gas.*;
import mekanism.common.capabilities.holder.chemical.ChemicalTankHelper;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.interfaces.ISideConfiguration;
import mekanism.common.tile.interfaces.ISustainedData;
import mekanism.common.util.ChemicalUtil;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public final class ReverserBlockEntity extends TileEntityMekanism implements ILatexMultiEnergyHandler, ISustainedData, ISideConfiguration {
    public static final int TANK_CAPACITY = 10_000, ENERGY_CAPACITY = 100_000;
    public enum Status { IDLE, RUNNING, INPUT_LOW, WHITE_LOW, DARK_LOW, OUTPUT_FULL, REDSTONE, DRAINING }
    private final TypedLatexEnergyStorage white = new TypedLatexEnergyStorage(LatexEnergyType.WLP, ENERGY_CAPACITY);
    private final TypedLatexEnergyStorage dark = new TypedLatexEnergyStorage(LatexEnergyType.DLP, ENERGY_CAPACITY);
    public IGasTank inputTank, outputTank;
    private int progress, whiteRemainder, darkRemainder, processTicks, whiteRate, darkRate;
    private String runningSignature = "";
    private long lastProcessTick = Long.MIN_VALUE;
    private Status status = Status.IDLE;
    private boolean draining;
    private final TileComponentConfig configComponent;
    private final TileComponentEjector ejectorComponent;

    public ReverserBlockEntity(BlockPos pos, BlockState state) {
        super(GasCompat.REVERSER, pos, state);
        // Mekanism side config for the gas ports. Defaults match the block's LEDs: in at the back, out at the front;
        // the sides carry WLP/DLP (outside Mekanism's config) and start with no gas port.
        configComponent = new TileComponentConfig(this, TransmissionType.GAS);
        ConfigInfo gas = configComponent.setupIOConfig(TransmissionType.GAS, inputTank, outputTank, RelativeSide.FRONT);
        gas.setDataType(DataType.NONE, RelativeSide.LEFT, RelativeSide.RIGHT, RelativeSide.TOP, RelativeSide.BOTTOM);
        gas.setEjecting(true);
        ejectorComponent = new TileComponentEjector(this).setOutputData(configComponent, TransmissionType.GAS);
    }
    @Override public TileComponentConfig getConfig() { return configComponent; }
    @Override public TileComponentEjector getEjector() { return ejectorComponent; }
    @Override public IChemicalTankHolder<Gas, GasStack, IGasTank> getInitialGasTanks(IContentsListener listener) {
        // Built before the config exists, so the holder reads it lazily.
        var sided = ChemicalTankHelper.forSideGasWithConfig(this::getDirection, this::getConfig);
        inputTank = ChemicalTankBuilder.GAS.create(TANK_CAPACITY,
                (gas, automation) -> automation != AutomationType.EXTERNAL || draining,
                (gas, automation) -> automation == AutomationType.INTERNAL || !draining,
                this::acceptsGas, ChemicalAttributeValidator.ALWAYS_ALLOW, listener);
        outputTank = ChemicalTankBuilder.GAS.output(TANK_CAPACITY, listener);
        sided.addTank(inputTank); sided.addTank(outputTank);
        return sided.build();
    }
    public boolean acceptsGas(Gas gas) {
        return level != null && level.getRecipeManager().getAllRecipesFor(GasCompat.REVERSING_TYPE.get())
                .stream().anyMatch(recipe -> recipe.getInput().testType(gas));
    }
    public Status status() { return status; }
    public int progress() { return progress; }
    public int processTicks() { return processTicks; }
    public int whiteRate() { return whiteRate; }
    public int darkRate() { return darkRate; }
    public boolean isDraining() { return draining; }
    public void toggleDrain() { draining = !draining; markForSave(); }
    private void reset() {
        if (!runningSignature.isEmpty() || progress != 0 || whiteRemainder != 0 || darkRemainder != 0) markForSave();
        runningSignature = ""; progress = whiteRemainder = darkRemainder = processTicks = whiteRate = darkRate = 0;
    }
    private void pause(Status reason) { status = reason; setActive(false); }
    @Override protected void onUpdateServer() {
        super.onUpdateServer();
        // Mekanism machines drive their own ejector; the base tile only ticks frequency, upgrade and chunk components.
        ejectorComponent.tickServer();
        process();
    }
    /** Public for the opt-in dedicated-server regression fixture; normal ticking is provided by Mek. */
    public void process() {
        if (level == null || level.isClientSide || isRemoved() || lastProcessTick == level.getGameTime()) return;
        lastProcessTick = level.getGameTime();
        // Output leaves through the ejector on whichever sides the config marks as output.
        if (draining) {
            pause(Status.DRAINING);
            ChemicalUtil.emit(configComponent.getConfig(TransmissionType.GAS).getSidesForData(DataType.INPUT), inputTank, this, 1000);
            return;
        }
        ReversingRecipe recipe = level.getRecipeManager().getAllRecipesFor(GasCompat.REVERSING_TYPE.get()).stream()
                .filter(candidate -> candidate.getInput().testType(inputTank.getStack()))
                .min(Comparator.comparing(candidate -> candidate.getId().toString())).orElse(null);
        if (recipe == null) { reset(); pause(Status.IDLE); return; }
        if (!recipe.signature().equals(runningSignature)) { reset(); runningSignature = recipe.signature(); markForSave(); }
        processTicks = recipe.ticks(); whiteRate = recipe.whiteRate(); darkRate = recipe.darkRate();
        if (!MekanismUtils.canFunction(this)) { pause(Status.REDSTONE); return; }
        if (!recipe.test(inputTank.getStack())) { pause(Status.INPUT_LOW); return; }
        GasStack result = recipe.getOutput(inputTank.getStack());
        if (!outputTank.insert(result, Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) { pause(Status.OUTPUT_FULL); return; }
        var step = DualEnergyProcess.step(whiteRate, darkRate, whiteRemainder, darkRemainder, progress + 1 >= processTicks);
        if (white.getEnergyStored() < step.white()) { pause(Status.WHITE_LOW); return; }
        if (dark.getEnergyStored() < step.dark()) { pause(Status.DARK_LOW); return; }
        white.extract(step.white()); dark.extract(step.dark());
        whiteRemainder = step.whiteRemainder(); darkRemainder = step.darkRemainder();
        if (++progress >= processTicks) {
            inputTank.shrinkStack(recipe.getInput().getNeededAmount(inputTank.getStack()), Action.EXECUTE);
            outputTank.insert(result, Action.EXECUTE, AutomationType.INTERNAL);
            progress = 0; whiteRemainder = darkRemainder = 0;
        }
        status = Status.RUNNING; setActive(true); markForSave();
    }
    /** Everything the GUI shows beyond the tanks, which Mekanism's container already syncs. */
    @Override public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableInt.create(white::getEnergyStored, white::setEnergy));
        container.track(SyncableInt.create(dark::getEnergyStored, dark::setEnergy));
        container.track(SyncableInt.create(() -> progress, value -> progress = value));
        container.track(SyncableInt.create(() -> processTicks, value -> processTicks = value));
        container.track(SyncableInt.create(() -> whiteRate, value -> whiteRate = value));
        container.track(SyncableInt.create(() -> darkRate, value -> darkRate = value));
        container.track(SyncableInt.create(() -> status.ordinal(), value -> status = Status.values()[value]));
        container.track(SyncableInt.create(() -> draining ? 1 : 0, value -> draining = value != 0));
    }
    @Override public boolean supportsTypedEnergy(LatexEnergyType type, Direction side) {
        if (type == LatexEnergyType.LP) return false;
        return side == null || side == (type == LatexEnergyType.WLP ? RelativeSide.LEFT : RelativeSide.RIGHT).getDirection(getDirection());
    }
    private TypedLatexEnergyStorage storage(LatexEnergyType type) { return type == LatexEnergyType.WLP ? white : dark; }
    @Override public int receiveTypedEnergy(LatexEnergyType type, int amount, Direction side) {
        if (!supportsTypedEnergy(type, side) || isRemoved()) return 0;
        int received = storage(type).receive(amount); if (received > 0) markForSave(); return received;
    }
    @Override public int extractTypedEnergy(LatexEnergyType type, int amount, Direction side) { return 0; }
    @Override public int getTypedEnergyStored(LatexEnergyType type) { return type == LatexEnergyType.LP ? 0 : storage(type).getEnergyStored(); }
    @Override public int getTypedEnergyCapacity(LatexEnergyType type) { return type == LatexEnergyType.LP ? 0 : ENERGY_CAPACITY; }
    @Override public void writeSustainedData(CompoundTag data) {
        CompoundTag power = new CompoundTag(); power.putInt("WLP", white.getEnergyStored()); power.putInt("DLP", dark.getEnergyStored());
        data.put("CEDualPower", power);
    }
    @Override public void readSustainedData(CompoundTag data) {
        CompoundTag power = data.getCompound("CEDualPower"); white.setEnergy(power.getInt("WLP")); dark.setEnergy(power.getInt("DLP"));
    }
    @Override public Map<String, String> getTileDataRemap() { return Map.of("CEDualPower", "CEDualPower"); }
    @Override public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("CERunningRecipe", runningSignature); tag.putInt("CEProgress", progress);
        tag.putInt("CEWhiteRemainder", whiteRemainder); tag.putInt("CEDarkRemainder", darkRemainder);
        tag.putLong("CELastProcessTick", lastProcessTick); tag.putBoolean("CEDraining", draining);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); runningSignature = tag.getString("CERunningRecipe"); progress = Math.max(0, tag.getInt("CEProgress"));
        whiteRemainder = Math.floorMod(tag.getInt("CEWhiteRemainder"), 20); darkRemainder = Math.floorMod(tag.getInt("CEDarkRemainder"), 20);
        lastProcessTick = tag.contains("CELastProcessTick") ? tag.getLong("CELastProcessTick") : Long.MIN_VALUE; draining = tag.getBoolean("CEDraining");
    }
}
