package github.com.gengyoubo.CE.client;

import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.skill.SkillTreePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** One pannable graph for all applicable branches, including cross-definition edges. */
public final class LatexSkillScreen extends Screen {
    private static final int COLUMN = 150, ROW = 64, TOP = 36;
    private final Screen parent;
    private CompoundTag data = new CompoundTag();
    private double panX, panY, zoom = 0.85;
    private double pressX, pressY;
    private boolean positioned, dragging, moved, pending;
    private String pressedNode;
    private int refreshTicks;

    public LatexSkillScreen(Screen parent) {
        super(Component.translatable("screen.changede.skills.title"));
        this.parent = parent;
    }

    public static void receive(CompoundTag data) {
        if (Minecraft.getInstance().screen instanceof LatexSkillScreen screen) {
            if (!screen.data.getString("form").equals(data.getString("form"))) screen.positioned = false;
            screen.data = data;
            screen.pending = false;
            if (!screen.positioned && !screen.nodes().isEmpty()) screen.centerRoot();
        }
    }

    private List<CompoundTag> nodes() {
        List<CompoundTag> result = new ArrayList<>();
        for (Tag tag : data.getList("nodes", Tag.TAG_COMPOUND)) result.add((CompoundTag) tag);
        return result;
    }

    private void centerRoot() {
        List<CompoundTag> nodes = nodes();
        CompoundTag root = nodes.isEmpty() ? new CompoundTag() : nodes.get(0);
        zoom = 0.85;
        panX = width / 2.0 - root.getInt("x") * COLUMN * zoom;
        panY = TOP + 30 - root.getInt("y") * ROW * zoom;
        positioned = !nodes.isEmpty();
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("screen.changede.skills.center"), b -> centerRoot())
                .bounds(width - 64, 6, 58, 20).build());
        centerRoot();
    }

    private boolean inCanvas(double x, double y) { return x >= 4 && x < width - 4 && y >= TOP && y < height - 32; }

    private CompoundTag hit(double mouseX, double mouseY) {
        if (!inCanvas(mouseX, mouseY)) return null;
        double x = (mouseX - panX) / zoom, y = (mouseY - panY) / zoom;
        for (CompoundTag node : nodes()) {
            double dx = x - node.getInt("x") * COLUMN, dy = y - node.getInt("y") * ROW;
            if (node.getBoolean("key") ? Math.abs(dx) <= 62 && Math.abs(dy) <= 16 : dx * dx + dy * dy <= 14 * 14)
                return node;
        }
        return null;
    }

    private boolean available(CompoundTag node) {
        if (pending || node.getBoolean("unlocked")) return false;
        Set<String> enabled = new HashSet<>();
        nodes().stream().filter(n -> n.getBoolean("active")).forEach(n -> enabled.add(n.getString("id")));
        for (Tag parent : node.getList("parents", Tag.TAG_STRING)) if (!enabled.contains(parent.getAsString())) return false;
        return data.getBoolean("creative") || data.getInt("levels") >= node.getInt("cost");
    }

    @Override
    public void tick() {
        if (refreshTicks++ % 20 == 0) CENetwork.sendToServer(new SkillTreePacket.Request(null, null));
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button)) return true;
        if ((button == 0 || button == 1) && inCanvas(x, y)) {
            dragging = true; moved = false; pressX = x; pressY = y;
            CompoundTag node = hit(x, y);
            pressedNode = button == 0 && node != null ? node.getString("id") : null;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (!dragging) return super.mouseDragged(x, y, button, dx, dy);
        if (!moved && Math.hypot(x - pressX, y - pressY) > 3) {
            moved = true; panX += x - pressX; panY += y - pressY;
        } else if (moved) { panX += dx; panY += dy; }
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (!dragging) return super.mouseReleased(x, y, button);
        dragging = false;
        CompoundTag node = hit(x, y);
        if (!moved && button == 0 && node != null && node.getString("id").equals(pressedNode) && available(node)) {
            pending = true;
            CENetwork.sendToServer(new SkillTreePacket.Request(ResourceLocation.tryParse(data.getString("form")),
                    ResourceLocation.tryParse(node.getString("id"))));
        }
        pressedNode = null;
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double amount) {
        if (!inCanvas(x, y)) return super.mouseScrolled(x, y, amount);
        double next = Mth.clamp(zoom * Math.pow(1.15, amount), 0.25, 1.5);
        panX = x - (x - panX) * next / zoom;
        panY = y - (y - panY) * next / zoom;
        zoom = next;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_DOWN) panY -= 40;
        else if (key == GLFW.GLFW_KEY_UP) panY += 40;
        else if (key == GLFW.GLFW_KEY_LEFT) panX += 40;
        else if (key == GLFW.GLFW_KEY_RIGHT) panX -= 40;
        else return super.keyPressed(key, scan, modifiers);
        return true;
    }

    private static void line(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : (double) i / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t), y = (int) Math.round(y1 + (y2 - y1) * t);
            graphics.fill(x, y, x + 2, y + 2, color);
        }
    }

    private static void circle(GuiGraphics graphics, int x, int y, int radius, int color) {
        for (int dx = -radius; dx <= radius; dx++) {
            int dy = (int) Math.sqrt(radius * radius - dx * dx);
            graphics.fill(x + dx, y - dy, x + dx + 1, y + dy + 1, color);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(4, TOP, width - 4, height - 32, 0xC0101824);
        List<CompoundTag> nodes = nodes();
        Map<String, CompoundTag> byId = new HashMap<>();
        nodes.forEach(n -> byId.put(n.getString("id"), n));
        graphics.enableScissor(4, TOP, width - 4, height - 32);
        graphics.pose().pushPose();
        graphics.pose().translate(panX, panY, 0);
        graphics.pose().scale((float) zoom, (float) zoom, 1);
        for (CompoundTag node : nodes) for (Tag parentId : node.getList("parents", Tag.TAG_STRING)) {
            CompoundTag prerequisite = byId.get(parentId.getAsString());
            if (prerequisite == null) continue;
            int x1 = prerequisite.getInt("x") * COLUMN, y1 = prerequisite.getInt("y") * ROW;
            int x2 = node.getInt("x") * COLUMN, y2 = node.getInt("y") * ROW;
            // Cull distant edges before rasterizing; very deep datapacks must remain cheap to pan.
            if (Math.max(y1, y2) * zoom + panY < TOP || Math.min(y1, y2) * zoom + panY > height - 32
                    || Math.max(x1, x2) * zoom + panX < 4 || Math.min(x1, x2) * zoom + panX > width - 4) continue;
            int color = prerequisite.getBoolean("active") ? 0xFF74CE99 : 0xFF4B586A;
            // Clip the line in world coordinates before drawing, including long cross-file connections.
            double start = 0, end = 1;
            double[] from = {x1, y1}, delta = {x2 - x1, y2 - y1};
            double[] low = {(4 - panX) / zoom, (TOP - panY) / zoom};
            double[] high = {(width - 4 - panX) / zoom, (height - 32 - panY) / zoom};
            for (int axis = 0; axis < 2; axis++) if (delta[axis] != 0) {
                double a = (low[axis] - from[axis]) / delta[axis], b = (high[axis] - from[axis]) / delta[axis];
                start = Math.max(start, Math.min(a, b)); end = Math.min(end, Math.max(a, b));
            }
            if (start <= end) line(graphics, (int) (x1 + delta[0] * start), (int) (y1 + delta[1] * start),
                    (int) (x1 + delta[0] * end), (int) (y1 + delta[1] * end), color);
        }
        for (CompoundTag node : nodes) {
            int x = node.getInt("x") * COLUMN, y = node.getInt("y") * ROW;
            if (x * zoom + panX < -100 || x * zoom + panX > width + 100
                    || y * zoom + panY < TOP - 50 || y * zoom + panY > height + 50) continue;
            int color = node.getBoolean("active") ? 0xFF74CE99 : available(node) ? 0xFFE6C66B : 0xFF566478;
            Component label = Component.translatable(node.getString("title"));
            if (node.getBoolean("key")) {
                graphics.fill(x - 62, y - 16, x + 62, y + 16, color);
                graphics.fill(x - 60, y - 14, x + 60, y + 14, 0xFF172335);
                var lines = font.split(label, 116);
                for (int i = 0; i < Math.min(lines.size(), 2); i++) {
                    var text = lines.get(i);
                    graphics.drawString(font, text, x - font.width(text) / 2, y - lines.size() * 4 + i * 9, 0xFFFFFF, false);
                }
            } else {
                circle(graphics, x, y, 12, color);
                circle(graphics, x, y, 9, 0xFF172335);
                graphics.drawCenteredString(font, label, x, y + 17, 0xDFE8F5);
            }
        }
        graphics.pose().popPose();
        graphics.disableScissor();
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.changede.skills.levels", data.getInt("levels")), width / 2, height - 26, 0xBFE8FF);
        graphics.drawCenteredString(font, Component.translatable("screen.changede.skills.navigation"), width / 2, height - 13, 0xAABBCD);
        if (nodes.isEmpty()) graphics.drawCenteredString(font, Component.translatable("screen.changede.skills.empty"), width / 2, TOP + 50, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
        CompoundTag hovered = hit(mouseX, mouseY);
        if (hovered != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable(hovered.getString("title")));
            tooltip.add(Component.translatable(hovered.getString("description")));
            tooltip.add(Component.translatable("screen.changede.skills.cost", hovered.getInt("cost")));
            String state = hovered.getBoolean("active") ? "learned" : hovered.getBoolean("unlocked") ? "inactive" : "unlock";
            tooltip.add(Component.translatable("screen.changede.skills." + state));
            for (Tag id : hovered.getList("parents", Tag.TAG_STRING)) {
                CompoundTag prerequisite = byId.get(id.getAsString());
                tooltip.add(Component.translatable("screen.changede.skills.requires", prerequisite == null
                        ? Component.literal(id.getAsString()) : Component.translatable(prerequisite.getString("title"))));
            }
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
