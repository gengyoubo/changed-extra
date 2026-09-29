package github.com.gengyoubo.CE.LP.BlockEntity.WireBlockEntity;

public enum PipeConnectionMode {
    BIDIRECTIONAL,
    INPUT,
    OUTPUT,
    DISABLED;

    public PipeConnectionMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public boolean canSource() {
        return this == BIDIRECTIONAL || this == INPUT;
    }

    public boolean canSink() {
        return this == BIDIRECTIONAL || this == OUTPUT;
    }
}
