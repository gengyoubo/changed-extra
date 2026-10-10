package github.com.gengyoubo.CE.compat.create.burner;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/** Registered only when both Jade and Create are available. */
public enum LatexBurnerJade implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;
    public static void registerCommon(IWailaCommonRegistration registration) { registration.registerBlockDataProvider(INSTANCE, LatexBurnerBlockEntity.class); }
    public static void registerClient(IWailaClientRegistration registration) { registration.registerBlockComponent(INSTANCE, LatexBurnerBlock.class); }
    @Override public void appendServerData(net.minecraft.nbt.CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof LatexBurnerBlockEntity burner)) return;
        ListTag lines = new ListTag();
        burner.summary().forEach(line -> lines.add(StringTag.valueOf(Component.Serializer.toJson(line))));
        data.put("LatexBurnerSummary", lines);
    }
    @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        for (var line : accessor.getServerData().getList("LatexBurnerSummary", net.minecraft.nbt.Tag.TAG_STRING)) {
            Component component = Component.Serializer.fromJson(line.getAsString());
            if (component != null) tooltip.add(component);
        }
    }
    @Override public ResourceLocation getUid() { return ResourceLocation.parse("changede:latex_burner"); }
}
