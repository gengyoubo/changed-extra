package github.com.gengyoubo.CE.items;

/** Material values sit between the reference iron and diamond spears. Speeds are blocks/second. */
public final class SpearCombatRules {
    public static final float JAB_DAMAGE = 3.5F;
    public static final double ATTACK_SPEED = 1.0;
    public static final int DURABILITY = 768;
    public static final double MIN_REACH = 2.0, MAX_REACH = 4.5, CREATIVE_REACH = 6.5, HITBOX_MARGIN = 0.125;
    // Item.Properties.spear in 1.21.11: interpolate iron/diamond timings and multiplier.
    // Condition durations begin AFTER the warmup, and include the endpoint tick.
    public static final int WARMUP = 11, TIRED = 55, DISENGAGED = 133, DAMAGE_DURATION = 213;
    public static final int USE_DURATION = WARMUP + DAMAGE_DURATION + 1;
    public static final double DAMAGE_MULTIPLIER = 1.0125;
    public static final int JAB_COOLDOWN = 20, TARGET_COOLDOWN = 10;
    public static boolean inReach(double distance, boolean creative) {
        return Double.isFinite(distance) && distance >= MIN_REACH - HITBOX_MARGIN
                && distance <= (creative ? CREATIVE_REACH : MAX_REACH);
    }
    public static float chargeDamage(double baseDamage, double closingSpeed, int ticks) {
        if (!Double.isFinite(closingSpeed) || !Double.isFinite(baseDamage) || ticks < WARMUP
                || ticks - WARMUP > DAMAGE_DURATION || closingSpeed < 4.6) return 0;
        // Preserve vanilla's ceil and base attribute formula; bound teleport/excessive-speed damage.
        return (float) (Math.max(0, baseDamage) + Math.ceil(DAMAGE_MULTIPLIER * Math.min(closingSpeed, 20)));
    }
    public static boolean canKnockback(int ticks, double speed) {
        return ticks >= WARMUP && ticks - WARMUP <= DISENGAGED && speed >= 5.1;
    }
    public static boolean canDismount(int ticks, double speed) {
        return ticks >= WARMUP && ticks - WARMUP <= TIRED && speed >= 7.75;
    }
    private SpearCombatRules() { }
}
