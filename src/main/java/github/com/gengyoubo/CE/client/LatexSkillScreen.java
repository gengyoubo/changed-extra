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
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** One continuous graph retains learned history across form changes. */
public final class LatexSkillScreen extends Screen {
    private static final int COLUMN = 150, ROW = 64, TOP = 36;
    private final Screen parent;
    private CompoundTag data = new CompoundTag();
    private double panX, panY, zoom = 0.85;
    private double pressX, pressY;
    private boolean positioned, dragging, moved, pending;
    private String pressedNode;
    private int refreshTicks;
    private String viewedType = "any";
    private Button layerButton;
    private boolean previewAll;
    private boolean layoutDirty = true;
    private int visualRevision = -1;
    private List<CompoundTag> layoutNodes = List.of();
    private List<SkillRegion> layoutRegions = List.of();

    public LatexSkillScreen(Screen parent) {
        super(Component.translatable("screen.changede.skills.title"));
        this.parent = parent;
    }

    public static void receive(CompoundTag data) {
        if (Minecraft.getInstance().screen instanceof LatexSkillScreen screen) {
            String previousActual = screen.data.getString("latex_type");
            boolean changedForm = !screen.data.getString("form").equals(data.getString("form"));
            screen.data = data;
            screen.layoutDirty = true;
            if (changedForm) screen.cancelPress();
            String nextType = SkillCanvasLayer.select(previousActual, data.getString("latex_type"), screen.viewedType, screen.layers());
            if (!nextType.equals(screen.viewedType)) screen.changeLayer(nextType);
            screen.updateLayerButton();
            screen.pending = false;
            if (!screen.positioned && !screen.nodes().isEmpty()) screen.centerRoot();
        }
    }

    private List<CompoundTag> allNodes() {
        List<CompoundTag> result = new ArrayList<>();
        for (Tag tag : data.getList("nodes", Tag.TAG_COMPOUND)) result.add((CompoundTag) tag);
        return result;
    }

    private List<CompoundTag> nodes() {
        if (layoutDirty || visualRevision != SkillVisuals.revision()) {
            var visible=allNodes().stream().filter(n -> SkillCanvasLayer.visibleApplicable(n.getBoolean("applicable"),
                    n.getString("scope").equals("global"),previewAll)).toList();
            var layout=SkillBranchLayout.arrange(visible.stream().map(n->new SkillBranchLayout.Node(
                    n.getString("id"),n.getString("tree"),n.getDouble("x"),n.getDouble("y"),
                    n.getString("scope").equals("global"))).toList(),SkillVisuals.branchPadding());
            layoutNodes=visible.stream().map(n->{
                CompoundTag positioned=n.copy();
                positioned.putDouble("x",layout.nodes().get(n.getString("id")).x());
                return positioned;
            }).toList();
            layoutRegions=SkillVisuals.resolveRegions(layout);
            visualRevision=SkillVisuals.revision();
            layoutDirty=false;
        }
        return layoutNodes;
    }

    private void cancelPress() { dragging=false; pressedNode=null; }

    private Set<String> layers() {
        Set<String> result = new TreeSet<>();
        for (CompoundTag node : allNodes()) {
            String type = node.getString("latex_type");
            if (!type.isEmpty() && !type.equals("any")) result.add(type);
        }
        return result;
    }

    private void changeLayer(String type) {
        viewedType = type;
        dragging = false;
        pressedNode = null;
        updateLayerButton();
    }

    private void updateLayerButton() {
        if (layerButton == null) return;
        layerButton.visible = true;
        layerButton.active = true;
        layerButton.setMessage(Component.translatable(previewAll ? "screen.changede.skills.preview_all" : "screen.changede.skills.preview_matching"));
    }

    private void nextLayer() {
        previewAll = !previewAll;
        layoutDirty = true;
        cancelPress();
        updateLayerButton();
    }

    private void centerRoot() {
        List<CompoundTag> nodes = nodes();
        CompoundTag root = nodes.stream().filter(n -> n.getList("parents", Tag.TAG_STRING).isEmpty()).findFirst().orElse(new CompoundTag());
        zoom = 0.85;
        panX = width / 2.0 - root.getDouble("x") * COLUMN * zoom;
        panY = TOP + 30 - root.getInt("y") * ROW * zoom;
        positioned = !nodes.isEmpty();
    }

    @Override
    protected void init() {
        layerButton = addRenderableWidget(Button.builder(Component.empty(), b -> nextLayer()).bounds(6, 6, 98, 20).build());
        updateLayerButton();
        addRenderableWidget(Button.builder(Component.translatable("screen.changede.skills.next_branch"), b -> {
            var keys=nodes().stream().filter(n->n.getBoolean("key")).toList();
            if(keys.isEmpty())return;
            int nearest=0; double distance=Double.MAX_VALUE;
            for(int i=0;i<keys.size();i++){
                double d=Math.hypot(keys.get(i).getDouble("x")*COLUMN*zoom+panX-width/2.0,
                        keys.get(i).getInt("y")*ROW*zoom+panY-(TOP+50));
                if(d<distance){distance=d;nearest=i;}
            }
            var key=keys.get((nearest+1)%keys.size());
            panX=width/2.0-key.getDouble("x")*COLUMN*zoom;
            panY=TOP+50-key.getInt("y")*ROW*zoom;
        }).bounds(108,6,70,20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.changede.skills.center"), b -> centerRoot())
                .bounds(width - 64, 6, 58, 20).build());
        centerRoot();
    }

    private boolean inCanvas(double x, double y) { return x >= 4 && x < width - 4 && y >= TOP && y < height - 32; }

    private CompoundTag hit(double mouseX, double mouseY) {
        if (!inCanvas(mouseX, mouseY)) return null;
        double x = (mouseX - panX) / zoom, y = (mouseY - panY) / zoom;
        for (CompoundTag node : nodes()) {
            double dx = x - node.getDouble("x") * COLUMN, dy = y - node.getInt("y") * ROW;
            if (node.getBoolean("key") ? Math.abs(dx) <= 62 && Math.abs(dy) <= 16 : dx * dx + dy * dy <= 14 * 14)
                return node;
        }
        return null;
    }

    private boolean available(CompoundTag node) {
        return !pending && node.getBoolean("purchasable");
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

    private static String state(CompoundTag node) {
        return node.getBoolean("active") ? "active" : node.getBoolean("unlocked") ? "dormant"
                : node.getBoolean("purchasable") ? "available" : "locked";
    }

    private static int stateColor(Map<String, Double> weights, String state) {
        return SkillVisuals.color(weights, t -> switch (state) {
            case "active" -> t.active();
            case "dormant" -> t.dormant();
            case "available" -> t.available();
            default -> t.locked();
        });
    }

    private void line(GuiGraphics graphics, int x1, int y1, int x2, int y2, String state) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        int color = 0;
        String motif = "grid";
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : (double) i / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t), y = (int) Math.round(y1 + (y2 - y1) * t);
            if (i % 16 == 0) {
                var weights = SkillVisuals.weights(layoutRegions, x / (double) COLUMN, y / (double) ROW, viewedType);
                color = stateColor(weights, state);
                motif = SkillVisuals.theme(SkillVisuals.dominant(weights)).motif();
            }
            graphics.fill(x, y, x + (motif.equals("latex") ? 3 : 2), y + 2, color);
            if (motif.equals("wings") && i % 16 < 8) graphics.fill(x + 4, y, x + 5, y + 1, color);
        }
    }

    private static void circle(GuiGraphics graphics, int x, int y, int radius, int color) {
        for (int dx = -radius; dx <= radius; dx++) {
            int dy = (int) Math.sqrt(radius * radius - dx * dx);
            graphics.fill(x + dx, y - dy, x + dx + 1, y + dy + 1, color);
        }
    }

    private static void diamond(GuiGraphics graphics, int x, int y, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = radius - Math.abs(dy);
            graphics.fill(x - half, y + dy, x + half + 1, y + dy + 1, color);
        }
    }

    private static ItemStack icon(CompoundTag node) {
        for (Tag value : node.getList("rewards", Tag.TAG_COMPOUND)) {
            String attribute = ((CompoundTag) value).getString("attribute");
            if (attribute.endsWith("max_health")) return new ItemStack(Items.APPLE);
            if (attribute.endsWith("armor") || attribute.endsWith("armor_toughness")) return new ItemStack(Items.SHIELD);
            if (attribute.endsWith("attack_damage") || attribute.contains("damage_vs")) return new ItemStack(Items.IRON_SWORD);
            if (attribute.endsWith("landing_resistance")) return new ItemStack(Items.SLIME_BALL);
            if (attribute.endsWith("flight_control")) return new ItemStack(Items.ELYTRA);
            if (attribute.endsWith("regeneration_speed")) return new ItemStack(Items.GOLDEN_APPLE);
            if (attribute.endsWith("exhaustion_reduction")) return new ItemStack(Items.BREAD);
            if (attribute.endsWith("food_saturation")) return new ItemStack(Items.HONEY_BOTTLE);
            if (attribute.endsWith("saturation_capacity")) return new ItemStack(Items.MUSHROOM_STEW);
            if (attribute.endsWith("white_latex_resistance")) return new ItemStack(Items.SHIELD);
            if (attribute.endsWith("white_fog_resistance")) return new ItemStack(Items.GLASS_BOTTLE);
            if (attribute.endsWith("armor_adaptation")) return new ItemStack(Items.IRON_CHESTPLATE);
            if (attribute.endsWith("weapon_adaptation") || attribute.endsWith("latex_weapon_damage")) return new ItemStack(Items.IRON_SWORD);
        }
        return new ItemStack(Items.FEATHER);
    }

    private Component requirement(CompoundTag tag, Map<String, CompoundTag> byId) {
        String type = tag.getString("type"), subject = tag.getString("subject");
        Component detail;
        if (type.equals("changede:parent") || type.equals("changede:inactive_parent")) {
            CompoundTag parent = byId.get(subject);
            detail = Component.translatable("screen.changede.skills.reason." + (type.endsWith("inactive_parent") ? "inactive_parent" : "parent"),
                    parent == null ? Component.literal(subject) : Component.translatable(parent.getString("title")));
        } else if (type.equals("changede:experience")) {
            detail = Component.translatable("screen.changede.skills.reason.experience", tag.getInt("current"), tag.getInt("required"));
        } else if (type.equals("changede:latex_type")) {
            detail = Component.translatable("screen.changede.skills.reason.latex_type", Component.translatable("screen.changede.skills.type." + subject));
        } else if (type.equals("changede:entity_tag")) {
            ResourceLocation id = ResourceLocation.tryParse(subject);
            String key = id == null ? subject : "skill_tag." + id.getNamespace() + "." + id.getPath().replace('/', '.');
            detail = Component.translatable("screen.changede.skills.reason.form", Component.translatableWithFallback(key, subject));
        } else if (type.equals("changede:form")) {
            detail = Component.translatable("screen.changede.skills.reason.form", Component.translatableWithFallback("skill_branch." + subject.replace(':','.'),subject));
        } else if (type.equals("changede:latex_form") || type.equals("changede:player_state")) {
            detail = Component.translatable("screen.changede.skills.reason." + type.substring(type.indexOf(':') + 1));
        } else detail = Component.translatable("screen.changede.skills.reason.other", type, subject);
        return Component.literal(tag.getBoolean("met") ? "✓ " : "✗ ").append(detail)
                .withStyle(tag.getBoolean("met") ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private Component reward(CompoundTag tag) {
        if (tag.getString("type").equals("changede:mechanic"))
            return Component.translatable("skill.changede.mechanic." + tag.getString("effect"), tag.getDouble("value")).withStyle(ChatFormatting.AQUA);
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("attribute"));
        var attribute = id == null ? null : ForgeRegistries.ATTRIBUTES.getValue(id);
        Component label = attribute == null ? Component.literal(tag.getString("attribute")) : Component.translatable(attribute.getDescriptionId());
        double amount = tag.getDouble("amount") * (tag.getInt("operation") == 0 ? 1 : 100);
        String growth = switch (tag.getString("attribute")) {
            case "changede:regeneration_speed" -> "regeneration";
            case "changede:exhaustion_reduction" -> "exhaustion";
            case "changede:food_saturation" -> "food_saturation";
            case "changede:saturation_capacity" -> "saturation_capacity";
            case "changede:white_latex_resistance" -> "white_latex_resistance";
            case "changede:white_fog_resistance" -> "white_fog_resistance";
            default -> "";
        };
        if (!growth.isEmpty() && tag.getInt("operation") == 0) {
            String value = String.format(Locale.ROOT, "%.2f", tag.getDouble("amount") * (growth.equals("saturation_capacity") ? 1 : 100))
                    .replaceAll("\\.?0+$", "");
            return Component.translatable("screen.changede.skills.reward." + growth, value).withStyle(ChatFormatting.AQUA);
        }
        String value = String.format(Locale.ROOT, "%.2f", amount).replaceAll("\\.?0+$", "") + (tag.getInt("operation") == 0 ? "" : "%");
        return Component.translatable("screen.changede.skills.reward.attribute", label, value).withStyle(ChatFormatting.AQUA);
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
        LatexSkillBackground.render(graphics, panX, panY, zoom,
                4, TOP, width - 4, height - 32, COLUMN, ROW, viewedType, layoutRegions);
        for (CompoundTag node : nodes) for (Tag parentId : node.getList("parents", Tag.TAG_STRING)) {
            CompoundTag prerequisite = byId.get(parentId.getAsString());
            if (prerequisite == null) continue;
            int x1 = (int)Math.round(prerequisite.getDouble("x") * COLUMN), y1 = prerequisite.getInt("y") * ROW;
            int x2 = (int)Math.round(node.getDouble("x") * COLUMN), y2 = node.getInt("y") * ROW;
            // Cull distant edges before rasterizing; very deep datapacks must remain cheap to pan.
            if (Math.max(y1, y2) * zoom + panY < TOP || Math.min(y1, y2) * zoom + panY > height - 32
                    || Math.max(x1, x2) * zoom + panX < 4 || Math.min(x1, x2) * zoom + panX > width - 4) continue;
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
                    (int) (x1 + delta[0] * end), (int) (y1 + delta[1] * end), state(node));
        }
        for (CompoundTag node : nodes) {
            int x = (int)Math.round(node.getDouble("x") * COLUMN), y = node.getInt("y") * ROW;
            if (x * zoom + panX < -100 || x * zoom + panX > width + 100
                    || y * zoom + panY < TOP - 50 || y * zoom + panY > height + 50) continue;
            var weights = SkillVisuals.weights(layoutRegions, node.getDouble("x"), node.getInt("y"), viewedType);
            String state = state(node), motif = SkillVisuals.theme(SkillVisuals.dominant(weights)).motif();
            int color = stateColor(weights, state), surface = SkillVisuals.color(weights, SkillVisuals.Theme::surface);
            Component label = Component.translatable(node.getString("title"));
            if (node.getBoolean("key")) {
                graphics.fill(x - 62, y - 16, x + 62, y + 16, color);
                graphics.fill(x - 60, y - 14, x + 60, y + 14, surface);
                if (motif.equals("wings")) {
                    diamond(graphics, x - 66, y, 7, color);
                    diamond(graphics, x + 66, y, 7, color);
                } else if (motif.equals("latex")) {
                    circle(graphics, x - 62, y, 5, color);
                    circle(graphics, x + 62, y, 5, color);
                }
                var lines = font.split(label, 116);
                for (int i = 0; i < Math.min(lines.size(), 2); i++) {
                    var text = lines.get(i);
                    graphics.drawString(font, text, x - font.width(text) / 2, y - Math.min(lines.size(), 2) * 4 + i * 9,
                            state.equals("dormant") ? color : 0xFFFFFF, false);
                }
            } else {
                if (motif.equals("wings")) {
                    diamond(graphics, x, y, 14, color);
                    diamond(graphics, x, y, 11, surface);
                    graphics.fill(x - 19, y - 4, x - 15, y - 2, color);
                    graphics.fill(x + 16, y - 4, x + 20, y - 2, color);
                } else {
                    circle(graphics, x, y, 12, color);
                    circle(graphics, x, y, motif.equals("latex") ? 8 : 10, surface);
                }
                graphics.renderItem(icon(node), x - 8, y - 8);
            }
            // Filled, hollow and star markers distinguish states even in monochrome packs.
            if (state.equals("active")) circle(graphics, x + 10, y + 10, 3, color);
            else if (state.equals("dormant")) {
                circle(graphics, x + 10, y + 10, 3, color);
                circle(graphics, x + 10, y + 10, 1, surface);
            } else if (state.equals("available")) {
                diamond(graphics, x + 10, y + 10, 4, color);
                graphics.fill(x + 9, y + 5, x + 11, y + 16, color);
            }
        }
        graphics.pose().popPose();
        graphics.disableScissor();
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.changede.skills.legend"), width / 2, 25, 0xB9BED0);
        graphics.drawCenteredString(font, Component.translatable("screen.changede.skills.experience", data.getInt("experience")), width / 2, height - 26, 0xBFE8FF);
        graphics.drawCenteredString(font, Component.translatable("screen.changede.skills.navigation"), width / 2, height - 13, 0xAABBCD);
        if (nodes.isEmpty()) graphics.drawCenteredString(font, Component.translatable("screen.changede.skills.empty"), width / 2, TOP + 50, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
        CompoundTag hovered = hit(mouseX, mouseY);
        if (hovered != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable(hovered.getString("title")));
            tooltip.add(Component.translatable(hovered.getString("description")));
            tooltip.add(Component.translatable("screen.changede.skills.cost", hovered.getInt("cost")));
            String state = hovered.getBoolean("active") ? "learned" : hovered.getBoolean("unlocked") ? "inactive"
                    : hovered.getBoolean("purchasable") ? "unlock" : "locked";
            tooltip.add(Component.translatable("screen.changede.skills." + state));
            tooltip.add(Component.translatable("screen.changede.skills.requirements").withStyle(ChatFormatting.GRAY));
            for (Tag tag : hovered.getList("requirements", Tag.TAG_COMPOUND)) tooltip.add(requirement((CompoundTag) tag, byId));
            tooltip.add(Component.translatable("screen.changede.skills.effects").withStyle(ChatFormatting.GRAY));
            boolean effect = false;
            for (Tag tag : hovered.getList("rewards", Tag.TAG_COMPOUND)) if (!((CompoundTag) tag).getString("type").equals("changede:none")) {
                tooltip.add(reward((CompoundTag) tag));
                effect = true;
            }
            if (!effect) tooltip.add(Component.translatable("screen.changede.skills.reward.none"));
            if (hovered.getString("id").equals("changede:feline_nine_lives"))
                tooltip.add(Component.translatable("screen.changede.skills.lives", data.getInt("lives")));
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
