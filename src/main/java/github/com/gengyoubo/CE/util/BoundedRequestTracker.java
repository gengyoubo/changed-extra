package github.com.gengyoubo.CE.util;

import java.util.LinkedHashMap;
import java.util.Map;

/** Tick-based cooldowns with bounded memory and support for a restarted world clock. */
public final class BoundedRequestTracker<K> {
    private final long interval;
    private final int capacity;
    private final Map<K, Long> requests = new LinkedHashMap<>(16, 0.75F, true);

    public BoundedRequestTracker(long interval, int capacity) {
        if (interval < 0 || capacity < 1) {
            throw new IllegalArgumentException("Invalid request tracker bounds");
        }
        this.interval = interval;
        this.capacity = capacity;
    }

    public boolean shouldRequest(K key, long now) {
        if (!isReady(key, now)) {
            return false;
        }
        requests.put(key, now);
        if (requests.size() > capacity) {
            requests.remove(requests.keySet().iterator().next());
        }
        return true;
    }

    public boolean isReady(K key, long now) {
        Long last = requests.get(key);
        return last == null || now < last || now - last >= interval;
    }

    public void pruneBefore(long cutoff) {
        requests.values().removeIf(last -> last < cutoff);
    }

    public void clear() {
        requests.clear();
    }

    public int size() {
        return requests.size();
    }
}
