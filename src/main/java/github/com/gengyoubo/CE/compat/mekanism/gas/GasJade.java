package github.com.gengyoubo.CE.compat.mekanism.gas;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

public enum GasJade implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;
    public static void registerCommon(IWailaCommonRegistration registration) { registration.registerBlockDataProvider(INSTANCE, ReverserBlockEntity.class); }
    public static void registerClient(IWailaClientRegistration registration) { registration.registerBlockComponent(INSTANCE, ReverserBlock.class); }
    @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof ReverserBlockEntity tile) {
            data.putString("CEReverserState", tile.status().name().toLowerCase(java.util.Locale.ROOT));
            data.putInt("CEReverserProgress", tile.progress()); data.putInt("CEReverserTicks", tile.processTicks());
        }
    }
    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var data = accessor.getServerData();
        if (data.contains("CEReverserState")) {
            tooltip.add(Component.translatable("state.changede.reverser." + data.getString("CEReverserState")));
            tooltip.add(Component.translatable("gui.changede.reverser.progress", data.getInt("CEReverserProgress") / 20.0, data.getInt("CEReverserTicks") / 20.0));
        }
    }
    @Override public ResourceLocation getUid() { return ResourceLocation.parse("changede:reverser"); }
}
