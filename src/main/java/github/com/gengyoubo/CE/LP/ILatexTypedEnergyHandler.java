package github.com.gengyoubo.CE.LP;

/** Interface for energy stores whose type must match exactly during transfer. */
public interface ILatexTypedEnergyHandler {
    LatexEnergyType getEnergyType();
    int receiveTypedEnergy(LatexEnergyType type, int amount);
    int extractTypedEnergy(LatexEnergyType type, int amount);
    int getTypedEnergyStored();
    int getTypedEnergyCapacity();
}
