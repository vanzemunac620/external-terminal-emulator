package terminal;

import java.util.Objects;

public class CellAttributes {
    public static final CellAttributes DEFAULT = new CellAttributes(null, null);

    public final Color foreground;
    public final Color background;

    public CellAttributes(Color background, Color foreground) {
        this.background = background;
        this.foreground = foreground;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CellAttributes a)) return false;
        return foreground == a.foreground && background == a.background;
    }

    @Override
    public int hashCode() {
        return Objects.hash(foreground, background);
    }

    @Override
    public String toString() {
        return "CellAttributes{fg=" + foreground + ", bg=" + background + "}";
    }
}
