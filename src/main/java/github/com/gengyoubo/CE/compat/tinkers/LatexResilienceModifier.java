package github.com.gengyoubo.CE.compat.tinkers;

import github.com.gengyoubo.CE.skill.SkillCombat;
import net.ltxprogrammer.changed.init.ChangedLatexTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.DigDurabilityEnchantment;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.hook.behavior.ToolDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

@SuppressWarnings("deprecation")
public final class LatexResilienceModifier extends Modifier implements ToolStatsModifierHook, ToolDamageModifierHook {
    @Override protected void registerHooks(ModuleHookMap.Builder builder) { builder.addHook(this, ModifierHooks.TOOL_STATS, ModifierHooks.TOOL_DAMAGE); }
    @Override public void addToolStats(IToolContext tool, ModifierEntry modifier, ModifierStatsBuilder builder) {
        // Rebuilt from material stats each time, so levels add 10% without compounding.
        // A multiplier also preserves the advertised bonus on handles with negative durability.
        ToolStats.DURABILITY.multiply(builder, 1 + .1 * LatexTinkersCompat.level(modifier.getLevel()));
    }
    @Override public int onDamageTool(IToolStackView tool, ModifierEntry modifier, int amount, @Nullable LivingEntity holder) {
        return onDamageTool(tool, modifier, amount, holder, null);
    }
    @Override public int beforeDamageTool(IToolStackView tool, ModifierEntry modifier, int amount, @Nullable LivingEntity holder,
                                          @Nullable ItemStack stack, ModifierId cause) {
        if (ModifierManager.isInTag(cause, TinkerTags.Modifiers.BYPASS_REINFORCED)) return amount;
        return onDamageTool(tool, modifier, amount, holder, stack);
    }
    @Override public int onDamageTool(IToolStackView tool, ModifierEntry modifier, int amount, @Nullable LivingEntity holder, @Nullable ItemStack stack) {
        int level = LatexTinkersCompat.level(modifier.getLevel());
        if (amount <= 0 || level == 0 || !(holder instanceof Player) || SkillCombat.latexType(holder) != ChangedLatexTypes.DARK_LATEX.get()) return amount;
        ItemStack actual = stack == null ? new ItemStack(tool.getItem()) : stack;
        int remaining = 0;
        for (int point = 0; point < amount; point++)
            if (!DigDurabilityEnchantment.shouldIgnoreDurabilityDrop(actual, level, holder.getRandom())) remaining++;
        return remaining;
    }
}
