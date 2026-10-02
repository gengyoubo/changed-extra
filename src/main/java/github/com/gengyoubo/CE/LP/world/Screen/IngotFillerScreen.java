package github.com.gengyoubo.CE.LP.world.Screen;

import github.com.gengyoubo.CE.LP.world.Menu.IngotFillerMenu;
import github.com.gengyoubo.CE.util.AmountFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;
import java.util.Locale;

public final class IngotFillerScreen extends AbstractContainerScreen<IngotFillerMenu> {
    public IngotFillerScreen(IngotFillerMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);imageWidth=176;imageHeight=186;inventoryLabelY=90;
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
        if(isHovering(8,80,160,5,mouseX,mouseY))graphics.renderTooltip(font,
                Component.literal(AmountFormat.format(menu.getEnergyStored(),menu.getMaxEnergyStored(),"LP")),mouseX,mouseY);
        if(isHovering(76,28,16,34,mouseX,mouseY)) {
            var amount=Component.literal(AmountFormat.format(menu.getFluidAmount(),menu.getTankCapacity(),"mB"));
            var fluid=menu.getStoredFluid();var rate=Component.literal(menu.getFluidPerSecond()+" mB/s");
            graphics.renderComponentTooltip(font,fluid.isEmpty() ? List.of(amount,rate) : List.of(fluid.getDisplayName(),amount,rate),mouseX,mouseY);
        }
    }
    @Override protected void renderBg(GuiGraphics graphics,float partialTick,int mouseX,int mouseY) {
        MachineGuiStyle.panel(graphics,leftPos,topPos,imageWidth,imageHeight,false,0xFF58AAC5);
        for(var slot:menu.slots)MachineGuiStyle.slot(graphics,leftPos+slot.x,topPos+slot.y);
        MachineGuiStyle.fluid(graphics,menu.getStoredFluid(),menu.getTankCapacity(),leftPos+76,topPos+28,16,34);
        MachineGuiStyle.bar(graphics,leftPos+99,topPos+39,18,5,menu.getProgress(),menu.getProcessTicks(),0xFFE79835);
        MachineGuiStyle.bar(graphics,leftPos+8,topPos+80,160,5,menu.getEnergyStored(),menu.getMaxEnergyStored(),0xFF56A8FF);
    }
    @Override protected void renderLabels(GuiGraphics graphics,int mouseX,int mouseY) {
        graphics.drawString(font,title,8,6,0x303030,false);
        graphics.drawString(font,Component.translatable("gui.changede.ingot_filler.fluid"),72,16,0x303030,false);
        graphics.drawString(font,Component.literal(menu.getFluidPerSecond()+" mB/s"),66,59,0x303030,false);
        String time=AmountFormat.format(String.format(Locale.ROOT,"%.1f",menu.getProgress()/20.0),String.format(Locale.ROOT,"%.1f",menu.getProcessTicks()/20.0));
        graphics.drawString(font,Component.translatable("gui.changede.alloy_furnace.time",time),8,70,0x303030,false);
        graphics.drawString(font,Component.translatable("gui.changede.alloy_furnace.power",menu.getLpPerSecond()),100,70,0x285589,false);
        graphics.drawString(font,playerInventoryTitle,8,inventoryLabelY,0x303030,false);
    }
}
