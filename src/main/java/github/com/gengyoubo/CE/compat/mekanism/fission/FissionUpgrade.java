package github.com.gengyoubo.CE.compat.mekanism.fission;

public enum FissionUpgrade {
    COOLING("advanced_fission_cooling"),
    PROCESSOR_X("advanced_uranium_processor_x"),
    PROCESSOR_Y("advanced_uranium_processor_y"),
    SCRAM("fission_emergency_scram"),
    ME_PORT("fission_me_interface"),
    THERMOELECTRIC("fission_thermoelectric_module");

    public final String id;
    FissionUpgrade(String id) { this.id = id; }
}
