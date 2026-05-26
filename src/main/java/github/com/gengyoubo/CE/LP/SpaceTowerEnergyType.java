package github.com.gengyoubo.CE.LP;

public enum SpaceTowerEnergyType {
    LP(100.0D),
    EU(10.0D),
    AE(5.0D),
    RF(2.5D),
    J(1.0D),
    CE(100.0D);

    private final double joulesPerUnit;

    SpaceTowerEnergyType(double joulesPerUnit) {
        this.joulesPerUnit = joulesPerUnit;
    }

    public double joulesPerUnit() {
        return joulesPerUnit;
    }

    public static SpaceTowerEnergyType byOrdinal(int ordinal) {
        SpaceTowerEnergyType[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return LP;
        }
        return values[ordinal];
    }
}
