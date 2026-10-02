package github.com.gengyoubo.CE.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import java.util.List;

/** The material chain and research consumption are explained directly in item tooltips. */
public final class MorphicCrystalItem extends Item {
    private final String kind;
    public MorphicCrystalItem(String kind) { super(new Properties());this.kind=kind; }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flag) {
        super.appendHoverText(stack,level,tooltip,flag);
        tooltip.add(Component.translatable("tooltip.changede.morphic_crystal."+kind).withStyle(ChatFormatting.GRAY));
    }
    public static BlockItem ore(Block block) {
        return new BlockItem(block,new Properties()) {
            @Override public void appendHoverText(ItemStack stack,Level level,List<Component> tooltip,TooltipFlag flag) {
                super.appendHoverText(stack,level,tooltip,flag);
                tooltip.add(Component.translatable("tooltip.changede.morphic_crystal.ore").withStyle(ChatFormatting.GRAY));
            }
        };
    }
}
