package github.com.gengyoubo.CE.LP;

import net.minecraft.core.Direction;

/** Interface for energy stores whose type must match exactly during transfer. */
public interface ILatexTypedEnergyHandler {
    LatexEnergyType getEnergyType();
    int receiveTypedEnergy(LatexEnergyType type, int amount);
    int extractTypedEnergy(LatexEnergyType type, int amount);
    default int receiveTypedEnergy(LatexEnergyType type,int amount,Direction from) { return receiveTypedEnergy(type,amount); }
    default int extractTypedEnergy(LatexEnergyType type,int amount,Direction from) { return extractTypedEnergy(type,amount); }
    int getTypedEnergyStored();
    int getTypedEnergyCapacity();
}
