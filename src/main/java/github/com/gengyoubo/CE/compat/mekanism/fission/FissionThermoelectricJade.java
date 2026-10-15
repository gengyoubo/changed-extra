package github.com.gengyoubo.CE.compat.mekanism.fission;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/** Loaded through CE's Jade plugin only when Mekanism Generators is present. */
public enum FissionThermoelectricJade implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;
    public static void registerCommon(IWailaCommonRegistration registration) { registration.registerBlockDataProvider(INSTANCE, FissionThermoelectricBlockEntity.class); }
    public static void registerClient(IWailaClientRegistration registration) { registration.registerBlockComponent(INSTANCE, FissionThermoelectricBlock.class); }
    @Override public void appendServerData(net.minecraft.nbt.CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof FissionThermoelectricBlockEntity module)) return;
        ListTag lines = new ListTag();
        module.summary().forEach(line -> lines.add(StringTag.valueOf(Component.Serializer.toJson(line))));
        data.put("ThermoelectricSummary", lines);
    }
    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        for (var line : accessor.getServerData().getList("ThermoelectricSummary", net.minecraft.nbt.Tag.TAG_STRING)) {
            Component component = Component.Serializer.fromJson(line.getAsString());
            if (component != null) tooltip.add(component);
        }
    }
    @Override public ResourceLocation getUid() { return ResourceLocation.parse("changede:fission_thermoelectric"); }
}
