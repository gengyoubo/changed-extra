package github.com.gengyoubo.CE.compat.synergy.camp;

import java.util.Arrays;

/** Bounded allocation of ingredient alternatives against shared real slot capacities. */
public final class CampIngredientMatcher {
    private CampIngredientMatcher() {}
    public static int[] match(boolean[][] accepts, int[] capacities) {
        int[] chosen = new int[accepts.length]; Arrays.fill(chosen, -1);
        int[] remaining = capacities.clone();
        return search(accepts, remaining, chosen, 0, new int[]{16384}) ? chosen : null;
    }
    private static boolean search(boolean[][] accepts, int[] counts, int[] chosen, int placed, int[] budget) {
        if (--budget[0] < 0) return false;
        if (placed == accepts.length) return true;
        int row = -1, fewest = Integer.MAX_VALUE;
        for (int i = 0; i < chosen.length; i++) if (chosen[i] < 0) {
            int options = 0;
            for (int j = 0; j < counts.length; j++) if (counts[j] > 0 && accepts[i][j]) options++;
            if (options == 0) return false;
            if (options < fewest) { fewest = options; row = i; }
        }
        for (int source = 0; source < counts.length; source++) if (counts[source] > 0 && accepts[row][source]) {
            counts[source]--; chosen[row] = source;
            if (search(accepts, counts, chosen, placed + 1, budget)) return true;
            counts[source]++; chosen[row] = -1;
        }
        return false;
    }
}
