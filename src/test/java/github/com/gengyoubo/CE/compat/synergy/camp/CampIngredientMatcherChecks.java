package github.com.gengyoubo.CE.compat.synergy.camp;

import java.util.Arrays;
import java.util.Random;

/** Standalone checks: no Minecraft bootstrap or external test dependencies. */
public final class CampIngredientMatcherChecks {
    public static void main(String[] args) {
        check(new boolean[][]{{true}, {true}}, new int[]{1}); // One carrot cannot pay for two recipe cells.
        check(new boolean[][]{{true}, {true}}, new int[]{2});
        check(new boolean[][]{{true, true}, {true, false}}, new int[]{1, 1}); // Save the exclusive ingredient.
        check(new boolean[][]{{true, true}, {true, true}, {true, true}}, new int[]{1, 1});
        check(new boolean[][]{{false, true}}, new int[]{5, 0});
        check(new boolean[0][0], new int[0]);
        check(new boolean[][]{{}}, new int[0]);
        Random random = new Random(0xCA4F17E);
        for (int trial = 0; trial < 10000; trial++) {
            int slots = random.nextInt(6), cells = random.nextInt(7);
            boolean[][] accepts = new boolean[cells][slots]; int[] counts = new int[slots];
            for (int slot = 0; slot < slots; slot++) counts[slot] = random.nextInt(4);
            for (int cell = 0; cell < cells; cell++) for (int slot = 0; slot < slots; slot++) accepts[cell][slot] = random.nextBoolean();
            check(accepts, counts);
        }
        System.out.println("Passed 10007 ingredient allocation checks (shared quantities, alternatives, shortage, conservation, input immutability).");
    }
    private static void check(boolean[][] accepts, int[] counts) {
        int[] before = counts.clone(); boolean[][] matrix = Arrays.stream(accepts).map(boolean[]::clone).toArray(boolean[][]::new);
        int[] match = CampIngredientMatcher.match(accepts, counts);
        if ((match != null) != feasible(accepts, counts, new int[counts.length], 0)) throw new AssertionError("Feasibility differs from exhaustive allocation");
        if (!Arrays.equals(counts, before) || !Arrays.deepEquals(accepts, matrix)) throw new AssertionError("Changed ingredient inventory/alternatives");
        if (match == null) return;
        int[] used = new int[counts.length];
        if (match.length != accepts.length) throw new AssertionError("Missing recipe cells");
        for (int cell = 0; cell < match.length; cell++) {
            int slot = match[cell];
            if (slot < 0 || slot >= counts.length || !accepts[cell][slot] || ++used[slot] > counts[slot])
                throw new AssertionError("Unpaid or incompatible ingredient");
        }
    }
    private static boolean feasible(boolean[][] accepts, int[] counts, int[] used, int cell) {
        if (cell == accepts.length) return true;
        for (int slot = 0; slot < counts.length; slot++) if (accepts[cell][slot] && used[slot] < counts[slot]) {
            used[slot]++;
            if (feasible(accepts, counts, used, cell + 1)) { used[slot]--; return true; }
            used[slot]--;
        }
        return false;
    }
}
