package github.com.gengyoubo.projectextended.common;

import gg.galaxygaming.projectextended.ProjectExtended;
import moze_intel.projecte.utils.text.ILangEntry;
import net.minecraft.Util;

public enum ProjectExtendedLang implements ILangEntry {
    MODE_SHIELD_1("mode","shield.1"),
    MODE_SHIELD_2("mode","shield.2"),
    MODE_SHIELD_3("mode","shield.3"),;
    private final String key;

    ProjectExtendedLang(String type, String path) {
        this(Util.makeDescriptionId(type, ProjectExtended.rl(path)));
    }

    ProjectExtendedLang(String key) {
        this.key = key;
    }

    public String getTranslationKey() {
        return this.key;
    }
}
