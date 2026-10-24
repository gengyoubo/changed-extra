package github.com.gengyoubo.CE.LP.world.Screen;

import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.LP.network.packet.CycleGeneratorRedstoneModePacket;
import github.com.gengyoubo.CE.LP.world.Menu.BasicGeneratorBlockEntityMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.Locale;

public class BasicGeneratorBlockEntityScreen extends AbstractContainerScreen<BasicGeneratorBlockEntityMenu> {
    private static final ResourceLocation TEXTURE = MachineGuiStyle.texture("basic_generator");
    private Button redstoneModeButton;

    public BasicGeneratorBlockEntityScreen(BasicGeneratorBlockEntityMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 230;
        imageHeight = 180;
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineGuiStyle.panel(graphics, TEXTURE, leftPos, topPos, imageWidth, imageHeight);
        MachineGuiStyle.bar(graphics, leftPos + 35, topPos + 72, 160, 5, menu.getEnergyStored(), menu.getMaxEnergyStored(), MachineGuiStyle.LP);
        if (minecraft != null && minecraft.level != null) {
            ItemStack icon = new ItemStack(minecraft.level.getBlockState(menu.getBlockPos()).getBlock());
            graphics.renderItem(icon, leftPos + 116, topPos + 39);
        }
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 10, 6, MachineGuiStyle.TITLE, false);
        graphics.drawString(font, Component.translatable("gui.changede.machine.fuel"), 70, 26, MachineGuiStyle.LABEL, false);
        graphics.drawString(font, playerInventoryTitle, 35, 80, MachineGuiStyle.LABEL, false);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(35, 72, 160, 5, mouseX, mouseY))
            graphics.renderTooltip(font, MachineGuiStyle.amount(menu.getEnergyStored(), menu.getMaxEnergyStored(), "LP"), mouseX, mouseY);
    }

    @Override protected void init() {
        super.init();
        redstoneModeButton = addRenderableWidget(Button.builder(Component.empty(),
                button -> CENetwork.sendToServer(new CycleGeneratorRedstoneModePacket(menu.getBlockPos())))
                .bounds(leftPos + 150, topPos + 24, 70, 20).build());
        updateRedstoneModeButton();
    }

    @Override protected void containerTick() { super.containerTick(); updateRedstoneModeButton(); }

    private void updateRedstoneModeButton() {
        if (redstoneModeButton != null) redstoneModeButton.setMessage(Component.translatable(
                "gui.changede.redstone." + menu.getRedstoneMode().name().toLowerCase(Locale.ROOT)));
    }
}
