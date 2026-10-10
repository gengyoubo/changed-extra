package github.com.gengyoubo.CE.compat.tinkers;

import github.com.gengyoubo.CE.skill.SkillCombat;
import net.ltxprogrammer.changed.init.ChangedLatexTypes;
import net.minecraft.nbt.LongTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

@SuppressWarnings("deprecation")
public final class LatexPlasticityModifier extends Modifier implements InventoryTickModifierHook {
    private static final ResourceLocation LAST_REPAIR = ResourceLocation.parse("changede:last_plasticity_repair");
    @Override protected void registerHooks(ModuleHookMap.Builder builder) { builder.addHook(this, ModifierHooks.INVENTORY_TICK); }
    @Override public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level, LivingEntity holder,
                                          int slot, boolean selected, boolean correctSlot, ItemStack stack) {
        if (level.isClientSide || !(holder instanceof Player) || tool.getDamage() <= 0 || level.getGameTime() % 60 != 0) return;
        var data = tool.getPersistentData();
        long now = level.getGameTime();
        if (data.contains(LAST_REPAIR) && data.get(LAST_REPAIR, (tag, key) -> tag.getLong(key)) == now) return;
        int amount = LatexTinkersCompat.level(modifier.getLevel());
        if (SkillCombat.latexType(holder) == ChangedLatexTypes.WHITE_LATEX.get()) amount *= 2;
        if (amount > 0) {
            data.put(LAST_REPAIR, LongTag.valueOf(now));
            ToolDamageUtil.repair(tool, amount);
        }
    }
}
