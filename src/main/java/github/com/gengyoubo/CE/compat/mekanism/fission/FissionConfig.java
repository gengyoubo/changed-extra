package github.com.gengyoubo.CE.compat.mekanism.fission;

import net.minecraftforge.common.ForgeConfigSpec;

public final class FissionConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue COOLING_EFFICIENCY;
    public static final ForgeConfigSpec.DoubleValue FUEL_EFFICIENCY;
    public static final ForgeConfigSpec.DoubleValue OUTPUT_BONUS;
    public static final ForgeConfigSpec.DoubleValue RETURN_CHANCE;
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
        SPEC = builder.build();
    }
    private FissionConfig() {}
}
