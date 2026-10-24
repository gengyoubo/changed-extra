package github.com.gengyoubo.CE.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import github.com.gengyoubo.CE.LP.network.CENetwork;
import github.com.gengyoubo.CE.skill.SkillTreePacket;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** One continuous graph retains learned history across form changes. */
public final class LatexSkillScreen extends Screen {
    private static final int COLUMN = 150, ROW = 64, TOP = 36;
    /** Node frames from tools/generate_machine_gui.py: greyscale frames plus white accents tinted per state. */
    private static final ResourceLocation NODES = ResourceLocation.fromNamespaceAndPath("changede", "textures/gui/skill_nodes.png");
    private static final int ATLAS_W = 296, ATLAS_H = 164, SMALL_W = 40, SMALL_H = 32, KEY_W = 148, KEY_H = 44;
    private static final int PAW_U = 240, PAW_W = 12, PAW_H = 11;
    private static final List<String> MOTIFS = List.of("grid", "latex", "wings");
    private final Screen parent;
    private CompoundTag data = new CompoundTag();
    private double panX, panY, zoom = 0.85;
    private double pressX, pressY;
    private boolean positioned, dragging, moved, pending;
    private String pressedNode;
    private int refreshTicks;
    private String viewedType = "any";
    private boolean layoutDirty = true;
    private int visualRevision = -1;
    private List<CompoundTag> layoutNodes = List.of();
    private List<SkillRegion> layoutRegions = List.of();

    public LatexSkillScreen(Screen parent) {
        super(Component.translatable("screen.changede.skills.title"));
        this.parent = parent;
    }

    public static void receive(CompoundTag data) {
        LatexSkillResearchScreen.receive(data);
        if (Minecraft.getInstance().screen instanceof LatexSkillScreen screen) {
            String previousActual = screen.data.getString("latex_type");
            boolean changedForm = !screen.data.getString("form").equals(data.getString("form"));
            screen.data = data;
            screen.layoutDirty = true;
            if (changedForm) screen.cancelPress();
            String nextType = SkillCanvasLayer.select(previousActual, data.getString("latex_type"), screen.viewedType, screen.layers());
            if (!nextType.equals(screen.viewedType)) screen.changeLayer(nextType);
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
                    n.getString("scope").equals("global"),learned(n))).toList();
            var layout=SkillBranchLayout.arrange(visible.stream().map(n->new SkillBranchLayout.Node(
                    n.getString("id"),n.getString("tree"),n.getDouble("x"),n.getDouble("y"),
                    n.getString("scope").equals("global"))).toList(),SkillVisuals.branchPadding());
            // Lay out every applicable node, then drop hidden ones, so revealing a node never moves the rest.
            Map<String, CompoundTag> all = new HashMap<>();
            allNodes().forEach(n -> all.put(n.getString("id"), n));
            layoutNodes=visible.stream().filter(n -> revealed(n, all)).map(n->{
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

    /** Like TC4 research, a node stays hidden until every prerequisite node has been learned. */
    private static boolean revealed(CompoundTag node, Map<String, CompoundTag> all) {
        if (learned(node)) return true;
        for (Tag id : node.getList("parents", Tag.TAG_STRING)) {
            CompoundTag parent = all.get(id.getAsString());
            if (parent == null || !learned(parent)) return false;
        }
        return true;
    }

    private static boolean learned(CompoundTag node) { return node.getBoolean("unlocked") || node.getBoolean("active"); }

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
        }).bounds(6,6,70,20).build());
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

    /** A paw stamped along a link, collected while meshing links and drawn once the mesh is uploaded. */
    private record Paw(float x, float y, float angle, int color, float alpha) { }

    /**
     * Link from (x1, y1) to (x2, y2), drawn between fractions {@code from} and {@code to}. Latex regions get a
     * goo tube with beads flowing towards the child; every other region gets a trail of paw prints.
     */
    private void link(BufferBuilder buffer, Matrix4f pose, List<Paw> paws, int x1, int y1, int x2, int y2,
                      double from, double to, String state, float time) {
        double dx = x2 - x1, dy = y2 - y1, length = Math.hypot(dx, dy);
        if (length < 1 || to <= from) return;
        var weights = SkillVisuals.weights(layoutRegions, (x1 + x2) / 2.0 / COLUMN, (y1 + y2) / 2.0 / ROW, viewedType);
        if (SkillVisuals.theme(SkillVisuals.dominant(weights)).motif().equals("latex"))
            flow(buffer, pose, x1, y1, dx, dy, length, from, to, state, time);
        else pawTrail(paws, x1, y1, dx, dy, length, from, to, state, time);
    }

    /** Dark goo tube with a lit upper edge; learned and available links carry beads of latex downstream. */
    private void flow(BufferBuilder buffer, Matrix4f pose, int x1, int y1, double dx, double dy, double length,
                      double from, double to, String state, float time) {
        float nx = (float) (-dy / length), ny = (float) (dx / length);
        boolean dry = state.equals("locked"), moving = state.equals("active") || state.equals("available");
        float alpha = switch (state) {
            case "active" -> 1F;
            case "dormant" -> 0.85F;
            case "available" -> 0.7F + 0.3F * Mth.sin(time / 6F);
            default -> 0.35F;
        };
        float core = dry ? 0.9F : state.equals("active") ? 1.8F : 1.5F;
        int samples = Math.max(1, (int) (length * (to - from) / 2));
        float px = 0, py = 0;
        int color = 0;
        for (int i = 0; i <= samples; i++) {
            double t = from + (to - from) * i / samples;
            float x = (float) (x1 + dx * t), y = (float) (y1 + dy * t);
            if (i % 8 == 0) color = stateColor(SkillVisuals.weights(layoutRegions, x / COLUMN, y / ROW, viewedType), state);
            // A locked link is a dried trail: broken residue with no shine.
            if (i > 0 && (!dry || (int) (t * length / 4) % 2 == 0)) {
                ribbon(buffer, pose, px, py, x, y, nx, ny, core, 1F, shade(color, 0.45F), alpha);
                if (!dry) ribbon(buffer, pose, px - nx * core * 0.45F, py - ny * core * 0.45F,
                        x - nx * core * 0.45F, y - ny * core * 0.45F, nx, ny, 0.45F, 0.4F, color, alpha * 0.9F);
            }
            px = x;
            py = y;
        }
        if (!moving) return;
        float spacing = 22, speed = state.equals("active") ? 0.9F : 0.45F;
        for (float along = time * speed % spacing; along < length; along += spacing) {
            double t = along / length;
            if (t < from || t > to) continue;
            float bx = (float) (x1 + dx * t), by = (float) (y1 + dy * t);
            float ux = (float) (dx / length * 2.5), uy = (float) (dy / length * 2.5);
            int bead = stateColor(SkillVisuals.weights(layoutRegions, bx / COLUMN, by / ROW, viewedType), state);
            ribbon(buffer, pose, bx - ux, by - uy, bx + ux, by + uy, nx, ny, core * 1.7F, 1.2F, shade(bead, 1.25F), alpha);
            ribbon(buffer, pose, bx - ux - nx, by - uy - ny, bx - nx, by - ny, nx, ny, 0.5F, 0.3F, 0xFFFFFF, alpha * 0.8F);
        }
    }

    /** Alternating left/right prints; on an available link they keep walking towards the child. */
    private void pawTrail(List<Paw> paws, int x1, int y1, double dx, double dy, double length,
                          double from, double to, String state, float time) {
        float nx = (float) (-dy / length), ny = (float) (dx / length);
        // Rotates the sprite's upward toes onto the link direction in y-down screen space.
        float angle = (float) Math.atan2(dx, -dy);
        int count = (int) ((length - 24) / 11);
        if (count < 1) return;
        int head = (int) (time / 4) % (count + 6);
        for (int k = 0; k < count; k++) {
            double t = (17.5 + k * 11) / length;
            if (t < from || t > to) continue;
            int side = k % 2 == 0 ? -1 : 1;
            float x = (float) (x1 + dx * t) + nx * side * 2.6F, y = (float) (y1 + dy * t) + ny * side * 2.6F;
            float alpha = switch (state) {
                case "active" -> 1F;
                case "dormant" -> 0.65F;
                case "available" -> k <= head ? Math.max(0.35F, 1F - (head - k) * 0.15F) : 0.3F;
                default -> 0.28F;
            };
            int color = stateColor(SkillVisuals.weights(layoutRegions, x / COLUMN, y / ROW, viewedType), state);
            paws.add(new Paw(x, y, angle + side * 0.18F, color, alpha));
        }
    }

    private static int shade(int color, float factor) {
        int r = Math.min(255, (int) (((color >> 16) & 255) * factor));
        int g = Math.min(255, (int) (((color >> 8) & 255) * factor));
        int b = Math.min(255, (int) ((color & 255) * factor));
        return r << 16 | g << 8 | b;
    }

    /** One segment: a solid core of half-width {@code core} with edges fading to clear over {@code feather}. */
    private static void ribbon(BufferBuilder buffer, Matrix4f pose, float ax, float ay, float bx, float by,
                               float nx, float ny, float core, float feather, int color, float alpha) {
        float r = ((color >> 16) & 255) / 255F, g = ((color >> 8) & 255) / 255F, b = (color & 255) / 255F;
        float out = core + feather;
        quad(buffer, pose, ax, ay, bx, by, nx, ny, -core, core, r, g, b, alpha, alpha);
        if (feather <= 0) return;
        quad(buffer, pose, ax, ay, bx, by, nx, ny, core, out, r, g, b, alpha, 0F);
        quad(buffer, pose, ax, ay, bx, by, nx, ny, -core, -out, r, g, b, alpha, 0F);
    }

    private static void quad(BufferBuilder buffer, Matrix4f pose, float ax, float ay, float bx, float by, float nx, float ny,
                             float near, float far, float r, float g, float b, float nearAlpha, float farAlpha) {
        buffer.vertex(pose, ax + nx * near, ay + ny * near, 0).color(r, g, b, nearAlpha).endVertex();
        buffer.vertex(pose, ax + nx * far, ay + ny * far, 0).color(r, g, b, farAlpha).endVertex();
        buffer.vertex(pose, bx + nx * far, by + ny * far, 0).color(r, g, b, farAlpha).endVertex();
        buffer.vertex(pose, bx + nx * near, by + ny * near, 0).color(r, g, b, nearAlpha).endVertex();
    }

    private static void sprite(GuiGraphics graphics, int x, int y, int u, int v, int width, int height, int tint) {
        graphics.setColor(((tint >> 16) & 255) / 255F, ((tint >> 8) & 255) / 255F, (tint & 255) / 255F, 1F);
        graphics.blit(NODES, x, y, u, v, width, height, ATLAS_W, ATLAS_H);
        graphics.setColor(1F, 1F, 1F, 1F);
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
        } else if (type.equals("changede:latex_form") || type.equals("changede:player_state") || type.equals("changede:research_table")) {
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
        // Latex links are one batched mesh; flush first so queued background fills stay underneath.
        graphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f pose = graphics.pose().last().pose();
        float time = (Util.getMillis() % 3_600_000L) / 50F;
        List<Paw> paws = new ArrayList<>();
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
            if (start <= end) link(buffer, pose, paws, x1, y1, x2, y2, start, end, state(node), time);
        }
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.enableCull();
        for (Paw paw : paws) {
            graphics.pose().pushPose();
            graphics.pose().translate(paw.x(), paw.y(), 0);
            graphics.pose().mulPose(Axis.ZP.rotation(paw.angle()));
            graphics.setColor(((paw.color() >> 16) & 255) / 255F, ((paw.color() >> 8) & 255) / 255F,
                    (paw.color() & 255) / 255F, paw.alpha());
            graphics.blit(NODES, -PAW_W / 2, -PAW_H / 2, PAW_U, 0, PAW_W, PAW_H, ATLAS_W, ATLAS_H);
            graphics.pose().popPose();
        }
        graphics.setColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
        for (CompoundTag node : nodes) {
            int x = (int)Math.round(node.getDouble("x") * COLUMN), y = node.getInt("y") * ROW;
            if (x * zoom + panX < -100 || x * zoom + panX > width + 100
                    || y * zoom + panY < TOP - 50 || y * zoom + panY > height + 50) continue;
            var weights = SkillVisuals.weights(layoutRegions, node.getDouble("x"), node.getInt("y"), viewedType);
            String state = state(node), motif = SkillVisuals.theme(SkillVisuals.dominant(weights)).motif();
            int color = stateColor(weights, state), surface = SkillVisuals.color(weights, SkillVisuals.Theme::surface);
            Component label = Component.translatable(node.getString("title"));
            int frame = Math.max(0, MOTIFS.indexOf(motif));
            if (node.getBoolean("key")) {
                graphics.fill(x - 61, y - 15, x + 61, y + 15, surface);
                sprite(graphics, x - KEY_W / 2, y - 18, 0, SMALL_H + frame * KEY_H, KEY_W, KEY_H, 0xFFFFFF);
                sprite(graphics, x - KEY_W / 2, y - 18, KEY_W, SMALL_H + frame * KEY_H, KEY_W, KEY_H, color);
                var lines = font.split(label, 116);
                for (int i = 0; i < Math.min(lines.size(), 2); i++) {
                    var text = lines.get(i);
                    graphics.drawString(font, text, x - font.width(text) / 2, y - Math.min(lines.size(), 2) * 4 + i * 9,
                            state.equals("dormant") ? color : 0xFFFFFF, false);
                }
            } else {
                if (motif.equals("wings")) diamond(graphics, x, y, 11, surface);
                else circle(graphics, x, y, 10, surface);
                sprite(graphics, x - SMALL_W / 2, y - SMALL_H / 2, frame * SMALL_W, 0, SMALL_W, SMALL_H, 0xFFFFFF);
                sprite(graphics, x - SMALL_W / 2, y - SMALL_H / 2, (frame + 3) * SMALL_W, 0, SMALL_W, SMALL_H, color);
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
