package terminal;

import java.util.Objects;

/// A single cell in the terminal grid. Immutable value object.
///
///     - `ch` — the character stored in this cell, or `null` for an empty cell.
///     - `attrs` — visual attributes (colors, style flags).
///     - `width` — display width:
///
///   - 1 = normal character
///     - 2 = wide-character leader (occupies this cell and the next)
///     - 0 = wide-character continuation (right half of a wide char, no visible glyph)
///
///
///
public final class Cell {

    public static final Cell empty = new Cell(null, CellAttributes.DEFAULT, 1);

    public static final Cell continuation = new Cell(null, CellAttributes.DEFAULT, 0);

    public final Character ch;
    public final CellAttributes attrs;
    public final int width;

    public Cell(Character ch, CellAttributes attrs, int width) {
        this.ch    = ch;
        this.attrs = (attrs != null) ? attrs : CellAttributes.DEFAULT;
        this.width = width;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cell c)) return false;
        return width == c.width && Objects.equals(ch, c.ch) && attrs.equals(c.attrs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ch, attrs, width);
    }

    @Override
    public String toString() {
        return "Cell{ch=" + ch + ", width=" + width + ", attrs=" + attrs + "}";
    }
}
