package github.com.gengyoubo.commands;

import com.mojang.brigadier.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

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
    }
    private static int executeItemInfo(Player player) {
        ItemStack stack = player.getMainHandItem();
        
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c❌ 手中没有物品！"));
            return 0;
        }
        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (registryName == null) {
            player.sendSystemMessage(Component.literal("§c无法获取物品注册名"));
            return 0;
        }
        player.sendSystemMessage(Component.literal("§a📦 注册名: §f" + registryName));
        String translationKey = stack.getDescriptionId();
        player.sendSystemMessage(Component.literal("§a🔑 本地化键: §f" + translationKey));
        Component displayName = stack.getDisplayName();
        player.sendSystemMessage(Component.literal("§a📝 显示名称: §r" + displayName.getString()));
        if (stack.hasCustomHoverName()) {
            player.sendSystemMessage(Component.literal("§a✏️ 自定义名称: §d" + stack.getHoverName().getString()));
        }
        String modId = registryName.getNamespace();
        player.sendSystemMessage(Component.literal("§a🔧 所属模组: §b" + modId));
        return Command.SINGLE_SUCCESS;
    }
}