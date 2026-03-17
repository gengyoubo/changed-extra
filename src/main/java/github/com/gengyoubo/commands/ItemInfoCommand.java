package github.com.gengyoubo.commands;

import com.mojang.brigadier.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public class ItemInfoCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("iteminfo")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    Player player = source.getPlayerOrException();
                    return executeItemInfo(player);
                })
        );
        
        event.getDispatcher().register(
            Commands.literal("iinfo")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    Player player = source.getPlayerOrException();
                    return executeItemInfo(player);
                })
        );
        
        // 添加调试模式
        event.getDispatcher().register(
            Commands.literal("iteminfo")
                .then(Commands.literal("debug")
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        Player player = source.getPlayerOrException();
                        return executeDebugInfo(player);
                    })
                )
        );
    }

    private static int executeItemInfo(Player player) {
        ItemStack stack = player.getMainHandItem();
        
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c❌ 手中没有物品！"));
            return 0;
        }

        player.sendSystemMessage(Component.literal("§6========================================="));
        player.sendSystemMessage(Component.literal("§e🔍 物品信息分析 §7(按住 F3+H 可查看更多信息)"));
        player.sendSystemMessage(Component.literal("§6========================================="));

        // 1. 物品注册名
        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (registryName == null) {
            player.sendSystemMessage(Component.literal("§c无法获取物品注册名"));
            return 0;
        }
        player.sendSystemMessage(Component.literal("§a📦 注册名: §f" + registryName));

        // 2. 本地化键 - 标准方式
        String translationKey = stack.getDescriptionId();
        player.sendSystemMessage(Component.literal("§a🔑 本地化键: §f" + translationKey));

        // 3. 标准格式的键名（用于对比）
        String standardKey = "item." + registryName.getNamespace() + "." + registryName.getPath();
        if (!translationKey.equals(standardKey)) {
            player.sendSystemMessage(Component.literal("§e   标准格式: §7" + standardKey));
            player.sendSystemMessage(Component.literal("§e   ⚠️ 非标准命名"));
        }

        // 4. 显示名称
        Component displayName = stack.getDisplayName();
        player.sendSystemMessage(Component.literal("§a📝 显示名称: §r" + displayName.getString()));

        // 5. 是否有自定义名称
        if (stack.hasCustomHoverName()) {
            player.sendSystemMessage(Component.literal("§a✏️ 自定义名称: §d" + stack.getHoverName().getString()));
        }

        // 6. 物品类
        player.sendSystemMessage(Component.literal("§a🏷️ 物品类: §7" + stack.getItem().getClass().getName()));

        // 7. 模组ID
        String modId = registryName.getNamespace();
        player.sendSystemMessage(Component.literal("§a🔧 所属模组: §b" + modId));

        // 8. 物品数量
        player.sendSystemMessage(Component.literal("§a🔢 数量: §f" + stack.getCount()));

        // 9. 耐久度
        if (stack.isDamageableItem()) {
            int damage = stack.getDamageValue();
            int maxDamage = stack.getMaxDamage();
            player.sendSystemMessage(Component.literal("§a⚡ 耐久: §f" + (maxDamage - damage) + "/" + maxDamage));
        }

        // 10. NBT 数据
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            player.sendSystemMessage(Component.literal("§a📋 NBT 数据:"));
            player.sendSystemMessage(Component.literal("§7" + formatNBT(tag, 1)));
        } else {
            player.sendSystemMessage(Component.literal("§a📋 NBT 数据: §7无"));
        }

        // 11. 坐标信息
        if (stack.hasTag() && stack.getTag() != null) {
            CompoundTag tag = stack.getTag();
            if (tag.contains("x") && tag.contains("y") && tag.contains("z")) {
                int x = tag.getInt("x");
                int y = tag.getInt("y");
                int z = tag.getInt("z");
                player.sendSystemMessage(Component.literal("§a📍 保存的坐标: §f[" + x + ", " + y + ", " + z + "]"));
                
                BlockPos pos = new BlockPos(x, y, z);
                double distance = Math.sqrt(player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()));
                player.sendSystemMessage(Component.literal("§a📏 当前距离: §f" + Math.round(distance) + " 格"));
            }
        }

        player.sendSystemMessage(Component.literal("§6========================================="));
        
        return Command.SINGLE_SUCCESS;
    }

    private static int executeDebugInfo(Player player) {
        ItemStack stack = player.getMainHandItem();
        
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c❌ 手中没有物品！"));
            return 0;
        }

        player.sendSystemMessage(Component.literal("§6=== 物品调试信息 ==="));
        
        // 基础信息
        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(stack.getItem());
        player.sendSystemMessage(Component.literal("§e注册名: §f" + registryName));
        
        // 获取本地化键的多种方式
        String actualKey = stack.getDescriptionId();
        player.sendSystemMessage(Component.literal("§egetDescriptionId(): §f" + actualKey));
        
        // 尝试通过语言管理器获取翻译
        try {
            // 仅在客户端可用
            if (player.level().isClientSide()) {
                String translation = net.minecraft.client.resources.language.I18n.get(actualKey);
                player.sendSystemMessage(Component.literal("§e当前翻译: §f" + translation));
            } else {
                player.sendSystemMessage(Component.literal("§e当前翻译: §7(需要在客户端查看)"));
            }
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§e当前翻译: §7无法获取"));
        }
        
        // 显示可能的本地化键
        player.sendSystemMessage(Component.literal("§6=== 可能的本地化键 ==="));
        suggestPossibleKeys(player, registryName);
        
        return Command.SINGLE_SUCCESS;
    }

    private static void suggestPossibleKeys(Player player, ResourceLocation registryName) {
        String modId = registryName.getNamespace();
        String path = registryName.getPath();
        
        List<String> possibleKeys = new ArrayList<>();
        
        // 标准变体
        possibleKeys.add("item." + modId + "." + path);
        possibleKeys.add("block." + modId + "." + path);
        
        // 无模组前缀
        possibleKeys.add(path);
        possibleKeys.add("item." + path);
        
        // 常见分类
        possibleKeys.add("tool." + modId + "." + path);
        possibleKeys.add("weapon." + modId + "." + path);
        possibleKeys.add("armor." + modId + "." + path);
        
        // 描述相关
        possibleKeys.add("tooltip." + modId + "." + path);
        possibleKeys.add("desc." + modId + "." + path);
        
        // 消息相关
        possibleKeys.add("message." + modId + "." + path);
        possibleKeys.add("chat." + modId + "." + path);
        
        // 其他变体
        possibleKeys.add(modId + "." + path);
        possibleKeys.add(path + ".name");
        possibleKeys.add(path + ".desc");
        
        for (String key : possibleKeys) {
            player.sendSystemMessage(Component.literal("  §7• §f" + key));
        }
        
        player.sendSystemMessage(Component.literal("§6=== 提示 ==="));
        player.sendSystemMessage(Component.literal("§7使用 §e/iteminfo §7查看基本信息"));
        player.sendSystemMessage(Component.literal("§7按 §eF3+H §7开启高级工具提示"));
    }

    private static String formatNBT(CompoundTag tag, int indent) {
        if (tag == null) return "null";
        
        StringBuilder sb = new StringBuilder();
        String indentStr = "  ".repeat(indent);
        
        for (String key : tag.getAllKeys()) {
            sb.append("\n").append(indentStr).append("§e").append(key).append("§7: ");
            Object value = tag.get(key);
            if (value instanceof CompoundTag) {
                sb.append("{").append(formatNBT((CompoundTag) value, indent + 1)).append("\n").append(indentStr).append("}");
            } else {
                sb.append("§f").append(value);
            }
        }
        
        return sb.toString();
    }
}