package github.com.gengyoubo.CE.LP.world.Screen;

import github.com.gengyoubo.CE.LP.world.Menu.IngotFillerMenu;
import github.com.gengyoubo.CE.util.AmountFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;
import java.util.Locale;

public final class IngotFillerScreen extends AbstractContainerScreen<IngotFillerMenu> {
    private static final ResourceLocation TEXTURE=MachineGuiStyle.texture("ingot_filler");
    public IngotFillerScreen(IngotFillerMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);imageWidth=176;imageHeight=166;inventoryLabelY=74;
    }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.changede.ingot_filler.clear"),
                button->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0))
                .bounds(leftPos+132,topPos+5,36,16)
                .tooltip(Tooltip.create(Component.translatable("gui.changede.ingot_filler.clear_hint"))).build());
        if(!menu.getRecipeErrors().isEmpty())addRenderableWidget(Button.builder(Component.literal("!"),button->{})
                .bounds(leftPos+114,topPos+5,16,16)
                .tooltip(Tooltip.create(Component.translatable("gui.changede.ingot_filler.recipe_errors",menu.getRecipeErrors()))).build());
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        renderBackground(graphics);super.render(graphics,mouseX,mouseY,partialTick);renderTooltip(graphics,mouseX,mouseY);
        if(isHovering(8,66,160,4,mouseX,mouseY))graphics.renderComponentTooltip(font,List.of(
                MachineGuiStyle.amount(menu.getEnergyStored(),menu.getMaxEnergyStored(),"LP"),
                Component.translatable("gui.changede.alloy_furnace.power",menu.getLpPerSecond())),mouseX,mouseY);
        if(isHovering(76,22,16,32,mouseX,mouseY)) {
            var amount=MachineGuiStyle.amount(menu.getFluidAmount(),menu.getTankCapacity(),"mB");
            var fluid=menu.getStoredFluid();var rate=Component.literal(menu.getFluidPerSecond()+" mB/s");
            graphics.renderComponentTooltip(font,fluid.isEmpty() ? List.of(amount,rate) : List.of(fluid.getDisplayName(),amount,rate),mouseX,mouseY);
        }
        if(isHovering(99,39,18,5,mouseX,mouseY)) {
            String time=AmountFormat.format(String.format(Locale.ROOT,"%.1f",menu.getProgress()/20.0),String.format(Locale.ROOT,"%.1f",menu.getProcessTicks()/20.0));
            graphics.renderTooltip(font,Component.translatable("gui.changede.alloy_furnace.time",time),mouseX,mouseY);
        }
    }
    @Override protected void renderBg(GuiGraphics graphics,float partialTick,int mouseX,int mouseY) {
        MachineGuiStyle.panel(graphics,TEXTURE,leftPos,topPos,imageWidth,imageHeight);
        MachineGuiStyle.fluid(graphics,menu.getStoredFluid(),menu.getTankCapacity(),leftPos+76,topPos+22,16,32);
        MachineGuiStyle.bar(graphics,leftPos+99,topPos+39,18,5,menu.getProgress(),menu.getProcessTicks(),MachineGuiStyle.PROGRESS);
        MachineGuiStyle.bar(graphics,leftPos+8,topPos+66,160,4,menu.getEnergyStored(),menu.getMaxEnergyStored(),MachineGuiStyle.LP);
    }
    @Override protected void renderLabels(GuiGraphics graphics,int mouseX,int mouseY) {
        graphics.drawString(font,title,8,6,MachineGuiStyle.TITLE,false);
        graphics.drawString(font,playerInventoryTitle,8,inventoryLabelY,MachineGuiStyle.LABEL,false);
    }
}
