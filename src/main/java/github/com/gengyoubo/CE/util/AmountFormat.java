package github.com.gengyoubo.CE.util;

/** Shared formatting for current / maximum values in GUI and tooltip text. */
public final class AmountFormat {
    private AmountFormat() { }

    public static String format(Object current, Object maximum) {
        return current + " / " + maximum;
    }

    public static String format(Object current, Object maximum, String unit) {
        return format(current, maximum) + " " + unit;
    }
}
