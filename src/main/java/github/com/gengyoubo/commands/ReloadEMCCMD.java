package github.com.gengyoubo.commands;

import com.mojang.brigadier.context.CommandContext;
import moze_intel.projecte.config.CustomEMCParser;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.network.PacketHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;

public class ReloadEMCCMD {

    public ReloadEMCCMD() {
        MinecraftForge.EVENT_BUS.addListener(this::register);
    }
    private void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("reloadEMC")
                        .requires(source -> source.hasPermission(4))
                        .executes(this::run)
        );
    }

    public int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Player player = source.getPlayer();
        assert player != null;
        var server = context.getSource().getServer();
        player.sendSystemMessage(Component.translatable("pe.command.reload.started"));
        CustomEMCParser.init();
        EMCMappingHandler.map(server.getServerResources().managers(), server.registryAccess(), server.getResourceManager());
        player.sendSystemMessage(Component.translatable("pe.command.reload.success"));
        // 发送更新包
        PacketHandler.sendFragmentedEmcPacketToAll();
        return 1;
    }
}