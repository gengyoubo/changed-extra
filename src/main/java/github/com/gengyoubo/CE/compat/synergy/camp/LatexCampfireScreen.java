package github.com.gengyoubo.CE.compat.synergy.camp;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.UUID;

public final class LatexCampfireScreen extends AbstractContainerScreen<LatexCampfireMenu> {
    private static final String[] TABS = {"overview", "residents", "tasks", "defense", "visitors", "warehouse"};
    private int tab, page, selectedTask;
    private boolean nearby;
    private UUID selected;
    private float uiScale = 1;
    public LatexCampfireScreen(LatexCampfireMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 400; imageHeight = 266;
    }
    private static Component tr(String key, Object... arguments) { return Component.translatable("camp.changede." + key, arguments); }
    @Override protected void init() {
        super.init();
        uiScale = Math.min(1, Math.min(width / 416.0F, height / 282.0F));
        leftPos = ((int) (width / uiScale) - imageWidth) / 2;
        topPos = ((int) (height / uiScale) - imageHeight) / 2;
        refresh();
    }
    @Override public boolean mouseClicked(double x, double y, int button) { return super.mouseClicked(x / uiScale, y / uiScale, button); }
    @Override public boolean mouseReleased(double x, double y, int button) { return super.mouseReleased(x / uiScale, y / uiScale, button); }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) { return super.mouseDragged(x / uiScale, y / uiScale, button, dx / uiScale, dy / uiScale); }
    @Override public boolean mouseScrolled(double x, double y, double delta) { return super.mouseScrolled(x / uiScale, y / uiScale, delta); }
    private CompoundTag view() { return menu.view; }
    private boolean owner() { return view().getBoolean("CanManage"); }
    private Button button(int x, int y, int width, Component label, Runnable action, boolean enabled) {
        Button button = Button.builder(label, b -> action.run()).bounds(leftPos + x, topPos + y, width, 20).build();
        button.active = enabled; addRenderableWidget(button); return button;
    }
    private void send(String action, UUID target, int index) { CampNetwork.action(menu.containerId, action, target, index); }
    private ListTag residents() { return view().getList("Residents", Tag.TAG_COMPOUND); }
    private CompoundTag selectedResident() {
        for (Tag value : residents()) if (((CompoundTag) value).hasUUID("Id") && ((CompoundTag) value).getUUID("Id").equals(selected)) return (CompoundTag) value;
        return new CompoundTag();
    }
    private void choose(int tab) { this.tab = tab; page = 0; refresh(); }
    void refresh() {
        clearWidgets();
        for (int i = 0; i < TABS.length; i++) {
            int index = i;
            button(8 + i * 64, 27, 62, tr(TABS[i]), () -> choose(index), tab != i);
        }
        if (tab == 0) {
            String[] factions = {"white", "dark", "aquatic", "light"};
            for (int i = 0; i < factions.length; i++) {
                int index = i;
                button(8 + i * 95, 218, 92, Component.translatable("faction.changed_synergy." + factions[i]),
                        () -> send("faction", null, index), owner() && residents().isEmpty());
            }
        } else if (tab == 1) {
            button(8, 52, 110, tr(nearby ? "show_residents" : "show_nearby"), () -> { nearby = !nearby; page = 0; refresh(); }, owner());
            ListTag list = nearby ? view().getList("Candidates", Tag.TAG_COMPOUND) : residents();
            clampPage(list.size(), 6);
            for (int row = 0; row < 6; row++) {
                int index = page * 6 + row;
                if (index >= list.size()) break;
                CompoundTag entry = list.getCompound(index); UUID id = entry.getUUID("Id");
                button(326, 79 + row * 22, 64, tr(nearby ? "invite" : "select"), () -> {
                    if (nearby) send("invite", id, 0); else { selected = id; refresh(); }
                }, owner() && (!nearby || residents().size() < 16));
            }
            CompoundTag r = selectedResident(); boolean ready = owner() && !r.isEmpty() && !r.contains("Task");
            button(8, 217, 106, tr("cycle_role"), () -> {
                int role = LatexSettlementData.enumValue(LatexSettlementData.Role.class, r.getString("Role"), LatexSettlementData.Role.RESIDENT).ordinal();
                send("role", selected, (role + 1) % 3);
            }, ready);
            button(118, 217, 88, tr("release"), () -> send("release", selected, 0), ready);
            paging(list.size(), 6);
        } else if (tab == 2 || tab == 3) {
            clampPage(residents().size(), 5);
            for (int row = 0; row < 5; row++) {
                int index = page * 5 + row;
                if (index >= residents().size()) break;
                CompoundTag r = residents().getCompound(index); UUID id = r.getUUID("Id");
                button(326, 92 + row * 22, 64, tr("select"), () -> { selected = id; refresh(); }, owner());
            }
            CompoundTag r = selectedResident(); boolean ready = owner() && !r.isEmpty() && !r.contains("Task");
            if (tab == 2) {
                LatexSettlementData.Task task = LatexSettlementData.Task.values()[selectedTask];
                button(8, 213, 130, tr("task." + task.name().toLowerCase(java.util.Locale.ROOT)), () -> {
                    selectedTask = (selectedTask + 1) % LatexSettlementData.Task.values().length; refresh();
                }, owner()).setTooltip(Tooltip.create(tr("task_info", task.duration / 1200, (int) (task.risk * 100), tr("reward." + task.name().toLowerCase(java.util.Locale.ROOT)))));
                button(142, 213, 80, tr("dispatch"), () -> send("dispatch", selected, selectedTask), ready && r.getBoolean("Loaded")
                        && r.getBoolean("InCamp") && r.getFloat("Health") >= r.getFloat("MaxHealth") / 2
                        && !view().getString("Raid").equals("ACTIVE") && !view().getString("Raid").equals("PREPARING"));
                button(226, 213, 80, tr("recall"), () -> send("recall", selected, 0), owner() && r.contains("Task"));
            } else {
                button(8, 213, 110, tr("assign_guard"), () -> send("role", selected, 1), ready);
                button(122, 213, 110, tr("assign_resident"), () -> send("role", selected, 0), ready);
                button(236, 213, 110, tr("assign_supply"), () -> send("role", selected, 2), ready)
                        .setTooltip(Tooltip.create(tr("supply_info")));
            }
            paging(residents().size(), 5);
        } else if (tab == 4 && view().contains("Visitor")) {
            MerchantOffers offers = new MerchantOffers(view().getCompound("Visitor").getCompound("Offers"));
            for (int i = 0; i < offers.size(); i++) {
                int index = i;
                button(313, 89 + i * 29, 77, tr("trade"), () -> send("trade", null, index), !offers.get(i).isOutOfStock());
            }
        } else if (tab == 5) {
            ListTag storages = view().getList("Storages", Tag.TAG_COMPOUND);
            for (int i = 0; i < storages.size(); i++) {
                int index = i;
                button(132, 72 + i * 23, 62, tr("unlink"), () -> send("unlink", null, index), owner());
            }
            ListTag available = view().getList("NearbyStorages", Tag.TAG_COMPOUND);
            clampPage(Math.max(view().getList("Stock", Tag.TAG_COMPOUND).size(), available.size()), 4);
            for (int i = 0; i < 4 && page * 4 + i < available.size(); i++) {
                long pos = available.getCompound(page * 4 + i).getLong("Pos");
                button(324, 72 + i * 23, 66, tr("link"), () -> send("link", new UUID(0, pos), 0), owner() && storages.size() < 4);
            }
            button(8, 215, 140, tr("collect", view().getInt("Pending")), () -> send("collect", null, 0), owner() && view().getInt("Pending") > 0);
            paging(Math.max(view().getList("Stock", Tag.TAG_COMPOUND).size(), available.size()), 4);
        }
    }
    private void clampPage(int size, int perPage) { page = Math.min(page, Math.max(0, (size - 1) / perPage)); }
    private void paging(int size, int perPage) {
        int max = Math.max(0, (size - 1) / perPage);
        if (page > max) page = max;
        button(318, 241, 32, Component.literal("<"), () -> { page--; refresh(); }, page > 0);
        button(358, 241, 32, Component.literal(">"), () -> { page++; refresh(); }, page < max);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);
        graphics.pose().pushPose(); graphics.pose().scale(uiScale, uiScale, 1);
        super.render(graphics, (int) (mouseX / uiScale), (int) (mouseY / uiScale), delta);
        renderTooltip(graphics, (int) (mouseX / uiScale), (int) (mouseY / uiScale));
        graphics.pose().popPose();
    }
    @Override protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF202A2C);
        graphics.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + imageHeight - 3, 0xFFF0E4CB);
        graphics.fill(leftPos + 7, topPos + 49, leftPos + imageWidth - 7, topPos + imageHeight - 30, 0xFFE0D1B2);
    }
    private void line(GuiGraphics graphics, int x, int y, Component text) { graphics.drawString(font, text, x, y, 0x252D2F, false); }
    private Component status(CompoundTag r) {
        if (r.contains("Task")) return r.getBoolean("Returning") ? tr("returning") : tr("expedition", tr("task." + r.getString("Task").toLowerCase(java.util.Locale.ROOT)), r.getLong("Remaining") / 20);
        if (r.getBoolean("OtherDimension")) return tr("other_dimension");
        if (!r.getBoolean("Loaded")) return tr("unloaded");
        return tr(r.getBoolean("InCamp") ? "in_camp" : "outside");
    }
    private String name(CompoundTag entry) {
        if (!entry.getString("DisplayName").isEmpty()) {
            try {
                Component component = Component.Serializer.fromJson(entry.getString("DisplayName"));
                if (component != null) return component.getString();
            } catch (com.google.gson.JsonParseException ignored) {}
        }
        return entry.getString("Name");
    }
    private void residentLine(GuiGraphics graphics, CompoundTag r, int y) {
        int color = r.hasUUID("Id") && r.getUUID("Id").equals(selected) ? 0xFFCAD2A1 : 0xFFD9C8A3;
        graphics.fill(8, y - 3, 322, y + 17, color);
        String name = font.plainSubstrByWidth(name(r), 114);
        line(graphics, 12, y, Component.literal(name));
        line(graphics, 129, y, tr("role." + r.getString("Role").toLowerCase(java.util.Locale.ROOT)));
        graphics.drawString(font, String.format(java.util.Locale.ROOT, "%.0f/%.0f", r.getFloat("Health"), r.getFloat("MaxHealth")),
                205, y, r.getFloat("Health") < r.getFloat("MaxHealth") ? 0x9C3530 : 0x252D2F, false);
        line(graphics, 129, y + 10, status(r));
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        line(graphics, 8, 8, title);
        line(graphics, 300, 8, tr(view().getBoolean("Owner") ? "owner" : "guest"));
        if (view().isEmpty()) { line(graphics, 12, 60, tr("loading")); return; }
        if (tab == 0) {
            line(graphics, 12, 60, tr("faction", Component.translatable("faction.changed_synergy." + view().getString("Faction"))));
            line(graphics, 12, 83, tr("population", residents().size(), 16));
            line(graphics, 12, 106, tr("prosperity", view().getInt("Prosperity"), view().getInt("Reputation")));
            line(graphics, 12, 129, tr("reserves", view().getInt("Food"), view().getList("Storages", Tag.TAG_COMPOUND).size(), view().getInt("Pending")));
            line(graphics, 12, 152, tr("security", (int) view().getFloat("Threat"), tr("raid." + view().getString("Raid").toLowerCase(java.util.Locale.ROOT))));
            line(graphics, 12, 184, tr("core_help"));
            line(graphics, 12, 201, tr("faction_help"));
        } else if (tab == 1) {
            ListTag list = nearby ? view().getList("Candidates", Tag.TAG_COMPOUND) : residents();
            if (list.isEmpty()) line(graphics, 12, 84, tr(nearby ? "no_candidates" : "no_residents"));
            for (int row = 0; row < 6 && page * 6 + row < list.size(); row++) {
                CompoundTag r = list.getCompound(page * 6 + row);
                if (nearby) line(graphics, 12, 85 + row * 22, Component.literal(font.plainSubstrByWidth(name(r), 302)));
                else residentLine(graphics, r, 83 + row * 22);
            }
        } else if (tab == 2 || tab == 3) {
            if (tab == 2) {
                var task = LatexSettlementData.Task.values()[selectedTask];
                line(graphics, 12, 59, tr("task_info", task.duration / 1200, (int) (task.risk * 100), tr("reward." + task.name().toLowerCase(java.util.Locale.ROOT))));
                line(graphics, 12, 77, tr("task_help"));
            } else {
                line(graphics, 12, 59, tr("security", (int) view().getFloat("Threat"), tr("raid." + view().getString("Raid").toLowerCase(java.util.Locale.ROOT))));
                line(graphics, 12, 77, tr("defense_info", view().getLong("RaidRemaining") / 20, view().getInt("Wave"), view().getInt("Raiders")));
            }
            if (residents().isEmpty()) line(graphics, 12, 100, tr("no_residents"));
            for (int row = 0; row < 5 && page * 5 + row < residents().size(); row++) residentLine(graphics, residents().getCompound(page * 5 + row), 96 + row * 22);
        } else if (tab == 4) {
            if (!view().contains("Visitor")) { line(graphics, 12, 62, tr("no_visitor")); return; }
            line(graphics, 12, 59, tr("visitor_time", view().getLong("VisitorRemaining") / 20));
            line(graphics, 12, 75, tr("trade_help"));
            MerchantOffers offers = new MerchantOffers(view().getCompound("Visitor").getCompound("Offers"));
            for (int i = 0; i < offers.size(); i++) {
                var offer = offers.get(i); int y = 91 + i * 29;
                ItemStack cost = offer.getCostA(), result = offer.getResult();
                graphics.renderItem(cost, 12, y); graphics.renderItem(result, 177, y);
                line(graphics, 32, y + 4, Component.literal(font.plainSubstrByWidth(cost.getHoverName().getString(), 100) + " ×" + cost.getCount()));
                line(graphics, 159, y + 4, Component.literal("→"));
                line(graphics, 197, y + 4, Component.literal(font.plainSubstrByWidth(result.getHoverName().getString(), 56) + " ×" + result.getCount()));
                line(graphics, 281, y + 4, Component.literal("" + (offer.getMaxUses() - offer.getUses())));
            }
        } else if (tab == 5) {
            line(graphics, 12, 56, tr("linked_storage")); line(graphics, 205, 56, tr("nearby_storage"));
            ListTag links = view().getList("Storages", Tag.TAG_COMPOUND), available = view().getList("NearbyStorages", Tag.TAG_COMPOUND);
            for (int i = 0; i < links.size(); i++) {
                var entry = links.getCompound(i); BlockPos pos = BlockPos.of(entry.getLong("Pos"));
                line(graphics, 12, 78 + i * 23, Component.literal(pos.getX() + "," + pos.getY() + "," + pos.getZ() + (entry.getBoolean("Loaded") ? "" : " *")));
            }
            for (int i = 0; i < 4 && page * 4 + i < available.size(); i++) {
                BlockPos pos = BlockPos.of(available.getCompound(page * 4 + i).getLong("Pos"));
                line(graphics, 205, 78 + i * 23, Component.literal(pos.getX() + "," + pos.getY() + "," + pos.getZ()));
            }
            ListTag stock = view().getList("Stock", Tag.TAG_COMPOUND);
            for (int row = 0; row < 4 && page * 4 + row < stock.size(); row++) {
                CompoundTag item = stock.getCompound(page * 4 + row);
                int x = row % 2 == 0 ? 12 : 205, y = 175 + row / 2 * 17;
                line(graphics, x, y, Component.literal(font.plainSubstrByWidth(item.getString("Name"), 138) + " ×" + item.getInt("Count")));
            }
            line(graphics, 155, 221, tr("storage_help"));
        }
        if (tab != 0 && tab != 4) line(graphics, 12, 246, tr("page", page + 1));
    }
}
