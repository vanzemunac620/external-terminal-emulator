package terminal;

import java.util.Arrays;

public class TerminalBuffer {
    private int width;
    private int height;
    public final int maxScrollback;

    private int cursorCol = 0;
    private int cursorRow = 0;

    private CellAttributes currentAttrs = CellAttributes.DEFAULT;

    private final TerminalLine[] screen;
    //!!!!!!!!!!!!!!!
    //tba: scrollback, insert, fill, clear screen, !!cursor action!!
    //!!!!!!!!!!!!!!!
    public TerminalBuffer(int height, int width, int maxScrollback) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Dimensions must be positive");
        this.width = width;
        this.height = height;
        this.maxScrollback = maxScrollback;
        this.screen = new TerminalLine[height];
        for (int i = 0; i < height; i++) screen[i] = new TerminalLine(width);
    }

    public void writeText(String text) {
        TerminalLine line = screen[cursorRow];
        for (int i = 0; i < text.length(); i++) {
            if (cursorCol >= width) break;
            char ch = text.charAt(i);
            int cw = displayWidth(ch);

            if (cw == 2 && cursorCol + 1 >= width) break;

            eraseWideCharAt(line, cursorCol);
            if (cw == 2 && cursorCol + 1 < width) eraseWideCharAt(line, cursorCol + 1);

            line.setCell(cursorCol, new Cell(ch, currentAttrs, cw));
            if (cw == 2 && cursorCol + 1 < width) {
                line.setCell(cursorCol + 1, Cell.continuation);
            }
            cursorCol += cw;
        }
    }

    public void moveCursorDown()
    {
        cursorRow = Math.min(cursorRow + 1, height - 1);
    }
    public void moveCursorLeft()
    {
        cursorCol = Math.max(cursorCol - 1, 0);
    }
    public void moveCursorToStartOfLine()
    {
        cursorCol = 0;
    }
    private void eraseWideCharAt(TerminalLine line, int col) {
        Cell cell = line.getCell(col);
        if (cell.width == 0 && col > 0) {
            // Continuation — erase its leader
            line.setCell(col - 1, Cell.empty);
        } else if (cell.width == 2 && col + 1 < width) {
            // Leader — erase its continuation
            line.setCell(col + 1, Cell.empty);
        }
    }

    /**
     * Returns the display width of {@code ch}: 2 for wide characters (e.g. CJK), 1 otherwise.
     */
    private static int displayWidth(char ch) {
        if ((int) ch >= 0x1100 && (int) ch <= 0x115F) return 2; // Hangul Jamo
        if ((int) ch >= 0x2E80 && (int) ch <= 0x303E) return 2; // CJK Radicals, Kangxi
        if ((int) ch >= 0x3041 && (int) ch <= 0x33FF) return 2; // Hiragana, Katakana, CJK symbols
        if ((int) ch >= 0x3400 && (int) ch <= 0x9FFF) return 2; // CJK Unified Ideographs ext A + main block
        if ((int) ch >= 0xA000 && (int) ch <= 0xA4CF) return 2; // Yi
        if ((int) ch >= 0xAC00 && (int) ch <= 0xD7AF) return 2; // Hangul Syllables
        if ((int) ch >= 0xF900 && (int) ch <= 0xFAFF) return 2; // CJK Compatibility Ideographs
        if ((int) ch >= 0xFE10 && (int) ch <= 0xFE1F) return 2; // Vertical Forms
        if ((int) ch >= 0xFE30 && (int) ch <= 0xFE6F) return 2; // CJK Compatibility Forms
        if ((int) ch >= 0xFF01 && (int) ch <= 0xFF60) return 2; // Fullwidth Forms
        if ((int) ch >= 0xFFE0 && (int) ch <= 0xFFE6) return 2; // Fullwidth Signs
        return 1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TerminalBuffer(").append(width).append('x').append(height)
                .append(", scrollback=").append(maxScrollback).append(")\n");
        sb.append("Cursor: (").append(cursorCol).append(", ").append(cursorRow).append(")\n");

        sb.append("─── Screen ───\n");
        for (int r = 0; r < height; r++) {
            sb.append(r).append(": ").append(screen[r].toString()).append('\n');
        }
        return sb.toString();
    }


}
