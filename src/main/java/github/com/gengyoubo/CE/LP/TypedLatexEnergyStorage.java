package github.com.gengyoubo.CE.LP;

/** Bounded energy storage that keeps WLP and DLP separate from LP. */
public final class TypedLatexEnergyStorage {
    private final LatexEnergyType type;
    private final int capacity;
    private int energy;

    public TypedLatexEnergyStorage(LatexEnergyType type, int capacity) {
        this.type = type;
        this.capacity = capacity;
    }

    public LatexEnergyType getType() { return type; }
    public int getEnergyStored() { return energy; }
    public int getCapacity() { return capacity; }
    public int receive(int amount) {
        int accepted = Math.min(Math.max(amount, 0), capacity - energy);
        energy += accepted;
        return accepted;
    }
    public int extract(int amount) {
        int extracted = Math.min(Math.max(amount, 0), energy);
        energy -= extracted;
        return extracted;
    }
    public void setEnergy(int amount) { energy = Math.min(Math.max(amount, 0), capacity); }
}
