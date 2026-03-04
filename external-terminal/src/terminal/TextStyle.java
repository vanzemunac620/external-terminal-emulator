package terminal;

import java.util.Objects;

public final class TextStyle {

    public static final TextStyle DEFAULT = new TextStyle(false, false, false, false, false, false);

    public final boolean bold;
    public final boolean italic;
    public final boolean underline;
    public final boolean blink;
    public final boolean strikethrough;
    public final boolean inverse;

    public TextStyle(boolean bold, boolean italic, boolean underline,
                     boolean blink, boolean strikethrough, boolean inverse) {
        this.bold          = bold;
        this.italic        = italic;
        this.underline     = underline;
        this.blink         = blink;
        this.strikethrough = strikethrough;
        this.inverse       = inverse;
    }

    public TextStyle(boolean bold, boolean italic, boolean underline) {
        this(bold, italic, underline, false, false, false);
    }


    public TextStyle withBold(boolean bold)                   { return new TextStyle(bold, italic, underline, blink, strikethrough, inverse); }
    public TextStyle withItalic(boolean italic)               { return new TextStyle(bold, italic, underline, blink, strikethrough, inverse); }
    public TextStyle withUnderline(boolean underline)         { return new TextStyle(bold, italic, underline, blink, strikethrough, inverse); }
    public TextStyle withBlink(boolean blink)                 { return new TextStyle(bold, italic, underline, blink, strikethrough, inverse); }
    public TextStyle withStrikethrough(boolean strikethrough) { return new TextStyle(bold, italic, underline, blink, strikethrough, inverse); }
    public TextStyle withInverse(boolean inverse)             { return new TextStyle(bold, italic, underline, blink, strikethrough, inverse); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TextStyle s)) return false;
        return bold == s.bold && italic == s.italic && underline == s.underline
                && blink == s.blink && strikethrough == s.strikethrough && inverse == s.inverse;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bold, italic, underline, blink, strikethrough, inverse);
    }

    @Override
    public String toString() {
        return "TextStyle{bold=" + bold + ", italic=" + italic + ", underline=" + underline
                + ", blink=" + blink + ", strikethrough=" + strikethrough + ", inverse=" + inverse + "}";
    }
}
