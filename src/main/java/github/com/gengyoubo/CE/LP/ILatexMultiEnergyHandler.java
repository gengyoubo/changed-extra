package github.com.gengyoubo.CE.LP;

import net.minecraft.core.Direction;

/** A machine port can expose independent typed buffers without joining their networks. */
public interface ILatexMultiEnergyHandler {
    boolean supportsTypedEnergy(LatexEnergyType type, Direction side);
    int receiveTypedEnergy(LatexEnergyType type, int amount, Direction side);
    int extractTypedEnergy(LatexEnergyType type, int amount, Direction side);
    int getTypedEnergyStored(LatexEnergyType type);
    int getTypedEnergyCapacity(LatexEnergyType type);
}
