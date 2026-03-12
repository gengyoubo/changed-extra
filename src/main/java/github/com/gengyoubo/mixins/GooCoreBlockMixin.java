package github.com.gengyoubo.mixins;

import net.foxyas.changedaddon.block.GooCoreBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(GooCoreBlock.class)
public class GooCoreBlockMixin {

    /**
     * @author gengyoubo
     * @reason 替换硬编码的 Tooltip 为翻译键
     */
    @Overwrite
    public void appendHoverText(ItemStack itemstack, @Nullable BlockGetter world, 
                                List<Component> list, TooltipFlag flag) {
        // 调用父类方法（可选，如果父类有 tooltip 需要保留）
        // super.appendHoverText(itemstack, world, list, flag);
        
        // 直接添加翻译后的文本
        list.add(Component.translatable("tooltip.goo_core_block.desc"));
    }
}