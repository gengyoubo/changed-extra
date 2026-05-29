package github.com.gengyoubo.CE.events;

import net.ltxprogrammer.changed.entity.beast.AbstractDarkLatexEntity;
import net.ltxprogrammer.changed.entity.beast.DarkLatexWolfPup;

public class DarkLatexWolfPupSupportEvents {
    private static final double SUPPORT_RADIUS = 16.0D;

    public static boolean hasNonPupDarkLatexSupport(DarkLatexWolfPup pup) {
        if (pup.level().isClientSide()) {
            return false;
        }

        return !pup.level().getEntitiesOfClass(AbstractDarkLatexEntity.class,
                pup.getBoundingBox().inflate(SUPPORT_RADIUS),
                entity -> entity != pup && entity.isAlive() && !(entity instanceof DarkLatexWolfPup)).isEmpty();
    }
}
