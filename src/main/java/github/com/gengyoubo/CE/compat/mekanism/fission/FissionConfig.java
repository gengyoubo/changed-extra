package github.com.gengyoubo.CE.compat.mekanism.fission;

import net.minecraftforge.common.ForgeConfigSpec;

public final class FissionConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue COOLING_EFFICIENCY;
    public static final ForgeConfigSpec.DoubleValue FUEL_EFFICIENCY;
    public static final ForgeConfigSpec.DoubleValue OUTPUT_BONUS;
    public static final ForgeConfigSpec.DoubleValue RETURN_CHANCE;
    public static final ForgeConfigSpec.DoubleValue ME_IDLE_POWER;
    public static final ForgeConfigSpec.DoubleValue THERMO_MAX_HEAT;
    public static final ForgeConfigSpec.DoubleValue THERMO_PASSIVE;
    public static final ForgeConfigSpec.DoubleValue THERMO_EFFICIENCY;
    public static final ForgeConfigSpec.DoubleValue THERMO_CAPACITY;
    public static final ForgeConfigSpec.IntValue THERMO_OUTPUT;
    static {
        var builder = new ForgeConfigSpec.Builder();
        COOLING_EFFICIENCY = builder.comment("Added cooling efficiency per block. N blocks consume 1/(1+N*bonus) water for unchanged steam and cooling; water rounds up.")
                .defineInRange("coolingEfficiencyPerBlock", 0.01, 0.0, 1.0);
        FUEL_EFFICIENCY = builder.comment("Processor X fuel efficiency per block. N blocks consume 1/(1+N*bonus) fuel at the configured burn rate.")
                .defineInRange("fuelEfficiencyPerBlock", 0.01, 0.0, 1.0);
        OUTPUT_BONUS = builder.comment("Processor X added heat and nuclear waste per block. Multiplier=1+N*bonus. Extra heat requires extra cooling.")
                .defineInRange("outputBonusPerBlock", 0.01, 0.0, 1.0);
        RETURN_CHANCE = builder.comment("Processor Y added chance per block to refund this burning tick's consumed fuel. Total chance caps at 100%; heat and waste are retained.")
                .defineInRange("fuelReturnChancePerBlock", 0.01, 0.0, 1.0);
        ME_IDLE_POWER = builder.comment("Fixed power draw per connected fission ME interface, in AE/t before AE2's power multiplier. Water extraction has no additional per-volume energy cost.")
                .defineInRange("meInterfaceIdlePower", 16.0, 0.0, 1_000_000.0);
        builder.push("thermoelectric");
        THERMO_MAX_HEAT = builder.comment("Maximum heat removed by each intact module, J/t. Starts at 450 K, reaches full load at 1100 K; fuses at 1800 K.")
                .defineInRange("maxHeatPerTick", 100_000.0, 0.0, 1.0E9);
        THERMO_PASSIVE = builder.comment("Fraction of requested cooling retained by the independent radiator when energy storage is full.")
                .defineInRange("passiveCoolingFraction", 0.25, 0.0, 1.0);
        THERMO_EFFICIENCY = builder.comment("Electrical efficiency coefficient, multiplied by max(0, 1 - ambientKelvin/reactorKelvin). Remaining heat is rejected to the environment.")
                .defineInRange("efficiency", 0.25, 0.0, 1.0);
        THERMO_CAPACITY = builder.comment("Per-module electricity storage in Joules, independent of FE conversion settings. Default corresponds to 1,000,000 FE.")
                .defineInRange("energyCapacityJoules", 2_500_000.0, 1.0, 1.0E12);
        THERMO_OUTPUT = builder.comment("Total FE-equivalent electricity output per module per tick, shared by all faces and energy APIs.")
                .defineInRange("maxOutputFEPerTick", 16_000, 1, 100_000_000);
        builder.pop();
        SPEC = builder.build();
    }
    private FissionConfig() {}
}
