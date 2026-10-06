package github.com.gengyoubo.CE.util;

import java.util.ArrayDeque;

/** A bounded, fair queue whose per-tick work does not grow with the number of clients. */
public final class BudgetedWorkQueue {
    public interface Work {
        boolean isValid();
        boolean isComplete();
        int step(int budget);
        void complete();
    }

    private final ArrayDeque<Work> jobs = new ArrayDeque<>();
    private final int capacity;
    private final int slice;

    public BudgetedWorkQueue(int capacity, int slice) {
        if (capacity < 1 || slice < 1) {
            throw new IllegalArgumentException("Invalid work queue bounds");
        }
        this.capacity = capacity;
        this.slice = slice;
    }

    public boolean isFull() {
        return jobs.size() >= capacity;
    }

    public boolean offer(Work work) {
        if (isFull()) {
            return false;
        }
        jobs.addLast(work);
        return true;
    }

    public int tick(int budget) {
        int remaining = Math.max(0, budget);
        while (remaining > 0 && !jobs.isEmpty()) {
            Work job = jobs.removeFirst();
            if (!job.isValid()) {
                continue;
            }
            if (!job.isComplete()) {
                int allowance = Math.min(slice, remaining);
                int used = job.step(allowance);
                if (used < 0 || used > allowance) {
                    throw new IllegalStateException("Job exceeded its work budget");
                }
                remaining -= Math.max(1, used);
            }
            if (job.isComplete()) {
                job.complete();
            } else {
                jobs.addLast(job);
            }
        }
        return Math.max(0, budget) - remaining;
    }

    public void clear() {
        jobs.clear();
    }
}
