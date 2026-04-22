package github.com.gengyoubo.commands;

import com.mojang.brigadier.Command;
import github.com.gengyoubo.fix.PatreonBenefitsFix;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber
public class CheckSpecialFormCommand {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("CheckSpecialForm")
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.argument("uuid", UuidArgument.uuid())
                                .executes(context -> run(
                                        context.getSource(),
                                        UuidArgument.getUuid(context, "uuid")
                                )))
        );
    }

    private static int run(CommandSourceStack source, UUID uuid) {
        boolean exists = PatreonBenefitsFix.getPlayerSpecialForm(uuid) != null;
        source.sendSuccess(() -> Component.literal(Boolean.toString(exists)), false);
        return Command.SINGLE_SUCCESS;
    }
}
