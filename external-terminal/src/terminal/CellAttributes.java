package terminal;

import java.util.Objects;

public class CellAttributes {
    public static final CellAttributes DEFAULT = new CellAttributes(null, null, TextStyle.DEFAULT);

    public final Color foreground;
    public final Color background;
    public final TextStyle style;

    public CellAttributes(Color background, Color foreground, TextStyle style) {
        this.background = background;
        this.foreground = foreground;
        this.style = (style != null) ? style : TextStyle.DEFAULT;
    }

    public CellAttributes withForeground(Color foreground) { return new CellAttributes(foreground, background, style); }
    public CellAttributes withBackground(Color background) { return new CellAttributes(foreground, background, style); }
    public CellAttributes withStyle(TextStyle style)           { return new CellAttributes(foreground, background, style); }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CellAttributes a)) return false;
        return foreground == a.foreground && background == a.background && style.equals(a.style);
    }

    @Override
    public int hashCode() {
        return Objects.hash(foreground, background, style);
    }

    @Override
    public String toString() {
        return "CellAttributes{fg=" + foreground + ", bg=" + background + ", style=" + style + "}";
    }
}
