package github.com.gengyoubo.CE.LP.client;

import github.com.gengyoubo.CE.LP.ILatexEnergyHandler;
import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.LP.network.packet.RequestWorkbenchEnergyPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public final class WorkbenchEnergyOverlayEvents {
    private WorkbenchEnergyOverlayEvents() {
    }

    public static void onRenderScreenPost(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return;
        }

        renderEnergyOverlay(containerScreen, event.getGuiGraphics());
    }

    public static void renderEnergyOverlay(AbstractContainerScreen<?> containerScreen, GuiGraphics guiGraphics) {
        AbstractContainerMenu menu = containerScreen.getMenu();
        if (!isSupportedMenu(menu)) {
            return;
        }

        BlockPos pos = getMenuBlockPos(menu);
        Minecraft minecraft = Minecraft.getInstance();
        if (pos == null || minecraft.level == null) {
            return;
        }

        ResourceLocation dimension = minecraft.level.dimension().location();
        long gameTime = minecraft.level.getGameTime();
        if (WorkbenchEnergyClientCache.shouldRequest(dimension, pos, gameTime)) {
            CENetwork.sendToServer(new RequestWorkbenchEnergyPacket(pos));
        }

        String text = getEnergyText(minecraft, dimension, pos).orElse(null);
        if (text == null) {
            text = "LP: loading...";
        }
        int x = Math.max(4, containerScreen.width / 2 - 86);
        int y = Math.max(4, containerScreen.height / 2 - 92);
        guiGraphics.drawString(minecraft.font, text, x, y, 0x55FFFF, true);
    }

    private static boolean isSupportedMenu(AbstractContainerMenu menu) {
        String name = menu.getClass().getName();
        return name.equals("net.ltxprogrammer.changed.world.inventory.InfuserMenu")
                || name.equals("net.ltxprogrammer.changed.world.inventory.PurifierMenu")
                || name.equals("net.foxyas.changedaddon.menu.UnifuserGuiMenu")
                || name.equals("net.foxyas.changedaddon.menu.CatalyzerGuiMenu");
    }

    private static BlockPos getMenuBlockPos(AbstractContainerMenu menu) {
        try {
            Method method = menu.getClass().getMethod("getBlockPos");
            Object value = method.invoke(menu);
            if (value instanceof BlockPos pos) {
                return pos;
            }
        } catch (ReflectiveOperationException ignored) {
            // Changed menus expose x/y/z fields instead.
        }

        try {
            return new BlockPos(getIntField(menu, "x"), getIntField(menu, "y"), getIntField(menu, "z"));
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Optional<String> getEnergyText(Minecraft minecraft, ResourceLocation dimension, BlockPos pos) {
        Optional<WorkbenchEnergyClientCache.Entry> cached = WorkbenchEnergyClientCache.get(dimension, pos);
        if (cached.isPresent()) {
            WorkbenchEnergyClientCache.Entry entry = cached.get();
            return Optional.of("LP: " + entry.stored() + " / " + entry.max());
        }

        BlockEntity blockEntity = minecraft.level == null ? null : minecraft.level.getBlockEntity(pos);
        if (blockEntity == null) {
            return Optional.empty();
        }

        if (blockEntity instanceof ILatexEnergyHandler latexEnergyHandler) {
            return Optional.of("LP: " + latexEnergyHandler.getEnergyStored() + " / " + latexEnergyHandler.getMaxEnergyStored());
        }

        Optional<IEnergyStorage> energy = blockEntity.getCapability(ForgeCapabilities.ENERGY).resolve();
        return energy.map(storage -> "LP: " + storage.getEnergyStored() + " / " + storage.getMaxEnergyStored());
    }

    private static int getIntField(Object owner, String name) throws ReflectiveOperationException {
        Field field = owner.getClass().getField(name);
        return field.getInt(owner);
    }
}
