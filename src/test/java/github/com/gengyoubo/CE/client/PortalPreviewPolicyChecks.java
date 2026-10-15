package github.com.gengyoubo.CE.client;

import github.com.gengyoubo.CE.util.BoundedRequestTracker;
import github.com.gengyoubo.CE.util.BudgetedWorkQueue;

public final class PortalPreviewPolicyChecks {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        var requests = new BoundedRequestTracker<String>(200, 2);
        check(requests.shouldRequest("a", 10), "First request must be accepted");
        check(!requests.shouldRequest("a", 209), "Requests must respect cooldown");
        check(requests.shouldRequest("a", 210), "Cooldown boundary must be accepted");
        check(requests.shouldRequest("a", 0), "Restarted world clock must permit requests");
        requests.shouldRequest("b", 0);
        requests.shouldRequest("c", 0);
        check(requests.size() == 2 && requests.isReady("a", 1), "Tracker must evict old entries at capacity");
        requests.pruneBefore(1);
        check(requests.size() == 0, "Expired requests must be removed");
        requests.shouldRequest("a", 0);
        requests.clear();
        check(requests.shouldRequest("a", 0), "Logout cleanup must reset cooldowns");

        var queue = new BudgetedWorkQueue(2, 3);
        var first = new Job(10);
        var second = new Job(10);
        check(queue.offer(first) && queue.offer(second) && !queue.offer(new Job(1)), "Queue must enforce capacity");
        check(queue.tick(6) == 6 && first.remaining == 7 && second.remaining == 7, "Work must share a bounded tick budget");
        check(queue.tick(0) == 0 && first.remaining == 7, "Zero budget must not advance jobs");
        while (!first.finished || !second.finished) check(queue.tick(5) <= 5, "Jobs must not exceed global budget");
        check(first.completions == 1 && second.completions == 1, "Jobs must complete exactly once");
        var invalid = new Job(1);
        invalid.valid = false;
        queue.offer(invalid);
        check(queue.tick(5) == 0 && !invalid.finished, "Invalid jobs must be discarded without work");
        var cleared = new Job(1);
        queue.offer(cleared);
        queue.clear();
        check(queue.tick(5) == 0 && cleared.remaining == 1, "Lifecycle cleanup must discard pending jobs");
        System.out.println("All portal preview policy checks passed");
    }

    private static final class Job implements BudgetedWorkQueue.Work {
        int remaining;
        int completions;
        boolean valid = true;
        boolean finished;
        Job(int remaining) { this.remaining = remaining; }
        public boolean isValid() { return valid; }
        public boolean isComplete() { return remaining == 0; }
        public int step(int budget) {
            int used = Math.min(budget, remaining);
            remaining -= used;
            return used;
        }
        public void complete() { finished = true; completions++; }
    }
}
