package github.com.gengyoubo.CE.LP;

import github.com.gengyoubo.CE.LP.energy.DimensionTowerChannels;
import github.com.gengyoubo.CE.LP.energy.DimensionTowerChunks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class DimensionTowerChecks {
    private static void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
    }

    private record Address(String dimension, int x, int z) { }

    private static final class Tower implements DimensionTowerChannels.Endpoint {
        private final Address address;
        private final LatexEnergyType type;
        private final IOType mode;
        private boolean enabled = true;
        private int channel = 1;
        private int energy;

        Tower(String dimension, int x, LatexEnergyType type, IOType mode, int energy) {
            address = new Address(dimension, x, 0);
            this.type = type;
            this.mode = mode;
            this.energy = energy;
        }
        @Override public Object address() { return address; }
        @Override public boolean active() { return enabled; }
        @Override public int channel() { return channel; }
        @Override public LatexEnergyType energyType() { return type; }
        @Override public IOType mode() { return mode; }
        @Override public int stored() { return energy; }
        @Override public int capacity() { return 50_000; }
        @Override public void moveEnergy(int delta) {
            energy += delta;
            check(energy >= 0 && energy <= capacity(), "Buffer bounds violated");
        }
    }

    public static void main(String[] args) {
        footprint();
        channels();
        distribution();
        randomizedConservation();
        System.out.println("All dimension tower checks passed");
    }

    private static void footprint() {
        check(DimensionTowerChunks.around(8, 8).size() == 1, "Middle needs one chunk");
        check(DimensionTowerChunks.around(15, 8).size() == 2, "East edge needs two chunks");
        check(DimensionTowerChunks.around(0, 8).size() == 2, "West edge needs two chunks");
        check(DimensionTowerChunks.around(15, 15).size() == 4, "Corner needs four chunks");
        check(DimensionTowerChunks.around(-1, -1).size() == 4, "Negative corner uses floor division");
        for (int x = -64; x <= 64; x++) for (int z = -64; z <= 64; z++) {
            var chunks = DimensionTowerChunks.around(x, z);
            check(chunks.size() <= 4, "Footprint exceeds four tickets");
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                check(chunks.contains(new DimensionTowerChunks.Chunk((x + dx) >> 4, (z + dz) >> 4)),
                        "Neighbor must be anchored across positive and negative boundaries");
            }
        }
    }

    private static void channels() {
        var network = new DimensionTowerChannels();
        var input = new Tower("overworld", 0, LatexEnergyType.LP, IOType.INPUT, 1_000);
        var output = new Tower("latex_space", 0, LatexEnergyType.LP, IOType.OUTPUT, 0);
        network.register(input);
        network.register(output);
        network.register(input);
        check(network.peerCount(input) == 1, "Registration must be idempotent");
        network.tick();
        check(input.energy == 0 && output.energy == 1_000, "Same coordinates in different dimensions must transfer all available energy");

        var wrongType = new Tower("latex_space", 1, LatexEnergyType.WLP, IOType.OUTPUT, 0);
        var wrongChannel = new Tower("latex_space", 2, LatexEnergyType.LP, IOType.OUTPUT, 0);
        wrongChannel.channel = 2;
        var unassigned = new Tower("latex_space", 3, LatexEnergyType.LP, IOType.OUTPUT, 0);
        unassigned.channel = 0;
        var stopped = new Tower("latex_space", 4, LatexEnergyType.LP, IOType.NONE, 0);
        for (var tower : List.of(wrongType, wrongChannel, unassigned, stopped)) network.register(tower);
        input.energy = 1_000;
        output.enabled = false;
        network.tick();
        check(wrongType.energy == 0 && wrongChannel.energy == 0 && unassigned.energy == 0 && stopped.energy == 0,
                "Energy type, channel, unset channel and mode must isolate endpoints");
        int before = input.energy;
        network.tick();
        check(input.energy == before, "Inactive/unloaded receiver must not drain a source");
        output.enabled = true;
        output.channel = 9;
        network.tick();
        check(input.energy == before, "Channel changes must take effect immediately");
        output.channel = 1;

        var replacement = new Tower("latex_space", 0, LatexEnergyType.LP, IOType.OUTPUT, 49_990);
        network.register(replacement);
        network.unregister(output);
        network.tick();
        check(replacement.energy == 50_000 && input.energy == before - 10,
                "Stale unload must not unregister replacement; full receivers consume only free space");
        network.unregister(replacement);
        before = input.energy;
        network.tick();
        check(input.energy == before, "Removed endpoint must stop participating");
        network.clear();
        check(network.peerCount(input) == 0, "Shutdown must clear server registry");

        for (LatexEnergyType type : LatexEnergyType.values()) {
            var source = new Tower("overworld", type.ordinal(), type, IOType.INPUT, 50_000);
            var target = new Tower("latex_space", type.ordinal(), type, IOType.OUTPUT, 0);
            network.register(source);
            network.register(target);
            network.tick();
            check(source.energy == 0 && target.energy == 50_000, type + " must transfer a full buffer in one tick without conversion");
            network.clear();
        }
    }

    private static void distribution() {
        var network = new DimensionTowerChannels();
        var source = new Tower("overworld", 0, LatexEnergyType.DLP, IOType.INPUT, 50_000);
        var targets = new ArrayList<Tower>();
        network.register(source);
        for (int i = 0; i < 7; i++) {
            var target = new Tower("latex_space", i, LatexEnergyType.DLP, IOType.OUTPUT, 45_000);
            targets.add(target);
            network.register(target);
        }
        network.tick();
        check(source.energy == 15_000, "One source must fill multiple receivers in one tick");
        for (var target : targets) check(target.energy == 50_000, "Receivers must fill only to capacity");

        // Restore demand to verify that priority still rotates without a rate limit.
        for (var target : targets) target.energy = 0;
        source.energy = 1_000;
        network.tick();
        check(targets.get(1).energy == 1_000 && source.energy == 0, "Receiver priority must rotate between ticks");

        network.clear();
        var receiver = new Tower("latex_space", 0, LatexEnergyType.WLP, IOType.OUTPUT, 48_500);
        network.register(receiver);
        List<Tower> senders = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            var sender = new Tower("overworld", i, LatexEnergyType.WLP, IOType.INPUT, 1_000);
            senders.add(sender);
            network.register(sender);
        }
        network.tick();
        check(receiver.energy == 50_000, "Multiple sources must fill one receiver in one tick");
        check(senders.get(2).energy == 0 && senders.get(3).energy == 500,
                "Sources must drain only the available receiver space in rotating order");
        check(senders.stream().mapToInt(sender -> sender.energy).sum() == 3_500,
                "Excess source energy must remain stored");
        receiver.energy = 0;
        network.tick();
        check(receiver.energy == 3_500 && senders.stream().allMatch(sender -> sender.energy == 0),
                "Receiver must accept all remaining source buffers without a shared tick budget");
    }

    private static void randomizedConservation() {
        Random random = new Random(35);
        for (int trial = 0; trial < 100; trial++) {
            var network = new DimensionTowerChannels();
            List<Tower> towers = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                var tower = new Tower(i % 2 == 0 ? "overworld" : "latex_space", i,
                        LatexEnergyType.values()[random.nextInt(3)], random.nextBoolean() ? IOType.INPUT : IOType.OUTPUT,
                        random.nextInt(50_001));
                tower.channel = 1 + random.nextInt(4);
                network.register(tower);
                towers.add(tower);
            }
            for (int tick = 0; tick < 30; tick++) {
                long total = towers.stream().mapToLong(tower -> tower.energy).sum();
                network.tick();
                check(total == towers.stream().mapToLong(tower -> tower.energy).sum(), "No energy creation or loss");
                for (Tower source : towers) {
                    if (source.mode != IOType.INPUT || source.energy == 0) continue;
                    for (Tower target : towers) {
                        if (target.mode == IOType.OUTPUT && target.channel == source.channel && target.type == source.type) {
                            check(target.energy == target.capacity(),
                                    "Available energy must fill every receiver in the channel before the tick ends");
                        }
                    }
                }
            }
        }
    }
}
