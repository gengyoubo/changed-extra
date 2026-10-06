package github.com.gengyoubo.CE.LP.energy;

import github.com.gengyoubo.CE.LP.IOType;
import github.com.gengyoubo.CE.LP.LatexEnergyType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Live endpoints only: energy stays in each tower, never in a global energy pool. */
public final class DimensionTowerChannels {
    public static final int MAX_CHANNEL = 9_999;
    public static final int TRANSFER_PER_TICK = 100;

    public interface Endpoint {
        Object address();
        boolean active();
        int channel();
        LatexEnergyType energyType();
        IOType mode();
        int stored();
        int capacity();
        void moveEnergy(int delta);
    }

    private record Channel(int number, LatexEnergyType type) { }
    private final Map<Object, Endpoint> endpoints = new LinkedHashMap<>();
    private long tick;

    public void register(Endpoint endpoint) {
        endpoints.put(endpoint.address(), endpoint);
    }

    public void unregister(Endpoint endpoint) {
        endpoints.remove(endpoint.address(), endpoint);
    }

    public int peerCount(Endpoint endpoint) {
        int peers = 0;
        for (Endpoint other : endpoints.values()) {
            if (other != endpoint && other.active() && other.channel() == endpoint.channel()
                    && other.energyType() == endpoint.energyType()) peers++;
        }
        return peers;
    }

    public void clear() { endpoints.clear(); }

    public void tick() {
        Map<Channel, List<Endpoint>> channels = new LinkedHashMap<>();
        for (Endpoint endpoint : endpoints.values()) {
            if (endpoint.active() && endpoint.channel() > 0 && endpoint.channel() <= MAX_CHANNEL) {
                channels.computeIfAbsent(new Channel(endpoint.channel(), endpoint.energyType()), ignored -> new ArrayList<>())
                        .add(endpoint);
            }
        }
        for (List<Endpoint> channel : channels.values()) transfer(channel);
        tick++;
    }

    private void transfer(List<Endpoint> channel) {
        List<Endpoint> senders = new ArrayList<>();
        List<Endpoint> receivers = new ArrayList<>();
        for (Endpoint endpoint : channel) {
            if (endpoint.mode() == IOType.INPUT && endpoint.stored() > 0) senders.add(endpoint);
            if (endpoint.mode() == IOType.OUTPUT && endpoint.stored() < endpoint.capacity()) receivers.add(endpoint);
        }
        if (senders.isEmpty() || receivers.isEmpty()) return;
        // Rotate priority so one full or busy receiver cannot monopolize a channel.
        int receiverIndex = (int) Math.floorMod(tick, receivers.size());
        int receiverBudget = TRANSFER_PER_TICK;
        int visited = 0;
        int senderStart = (int) Math.floorMod(tick, senders.size());
        for (int i = 0; i < senders.size() && visited < receivers.size(); i++) {
            Endpoint source = senders.get((senderStart + i) % senders.size());
            int budget = Math.min(TRANSFER_PER_TICK, source.stored());
            while (budget > 0 && visited < receivers.size()) {
                Endpoint target = receivers.get(receiverIndex);
                int amount = Math.min(budget, Math.min(receiverBudget, target.capacity() - target.stored()));
                if (amount > 0) {
                    source.moveEnergy(-amount);
                    target.moveEnergy(amount);
                    budget -= amount;
                    receiverBudget -= amount;
                }
                if (receiverBudget == 0 || target.stored() == target.capacity()) {
                    visited++;
                    receiverIndex = (receiverIndex + 1) % receivers.size();
                    receiverBudget = TRANSFER_PER_TICK;
                }
            }
        }
    }
}
