package github.com.gengyoubo.CE.compat.maid;

import net.ltxprogrammer.changed.entity.ChangedEntity;

/** Identity bridge with no dependency on TLM: work adapters always refer to their real body. */
public interface MaidWorkBody {
    ChangedEntity workBody();
}
