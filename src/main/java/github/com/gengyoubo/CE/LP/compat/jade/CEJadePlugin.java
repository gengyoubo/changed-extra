package github.com.gengyoubo.CE.LP.compat.jade;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class CEJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(LPEnergyProvider.INSTANCE, BlockEntity.class);
        if (net.minecraftforge.fml.ModList.get().isLoaded("mekanism"))
            github.com.gengyoubo.CE.compat.mekanism.gas.GasJade.registerCommon(registration);
        if (net.minecraftforge.fml.ModList.get().isLoaded("mekanismgenerators"))
            github.com.gengyoubo.CE.compat.mekanism.fission.FissionThermoelectricJade.registerCommon(registration);
        if (net.minecraftforge.fml.ModList.get().isLoaded("create"))
            github.com.gengyoubo.CE.compat.create.burner.LatexBurnerJade.registerCommon(registration);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(LPEnergyProvider.INSTANCE, Block.class);
        if (net.minecraftforge.fml.ModList.get().isLoaded("mekanism"))
            github.com.gengyoubo.CE.compat.mekanism.gas.GasJade.registerClient(registration);
        if (net.minecraftforge.fml.ModList.get().isLoaded("mekanismgenerators"))
            github.com.gengyoubo.CE.compat.mekanism.fission.FissionThermoelectricJade.registerClient(registration);
        if (net.minecraftforge.fml.ModList.get().isLoaded("create"))
            github.com.gengyoubo.CE.compat.create.burner.LatexBurnerJade.registerClient(registration);
    }
}
