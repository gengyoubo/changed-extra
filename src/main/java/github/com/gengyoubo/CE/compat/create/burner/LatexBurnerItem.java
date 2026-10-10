package github.com.gengyoubo.CE.compat.create.burner;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class LatexBurnerItem extends BlockItem {
    private final boolean filled;
    public LatexBurnerItem(boolean filled) {
        super(LatexBurnerCompat.BLOCK.get(), filled ? new Item.Properties().stacksTo(1) : new Item.Properties());
        this.filled = filled;
    }
    @Override public void registerBlocks(Map<net.minecraft.world.level.block.Block, Item> map, Item item) {
        if (filled) super.registerBlocks(map, item);
    }
    @Override public String getDescriptionId() { return filled ? "block.changede.latex_burner" : "item.changede.empty_latex_burner"; }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return filled ? InteractionResult.PASS : LatexBurnerCapture.capture(stack, player, target, hand);
    }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        if (!filled) { lines.add(Component.translatable("changede.latex_burner.capture_hint")); return; }
        var data = stack.getTagElement("BlockEntityTag");
        if (data != null) LatexBurnerBlockEntity.itemSummary(data).forEach(lines::add);
    }
}
