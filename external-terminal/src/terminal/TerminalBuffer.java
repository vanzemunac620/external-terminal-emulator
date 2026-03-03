package terminal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class TerminalBuffer {
    private int width;
    private int height;
    public final int maxScrollback;

    private int cursorCol = 0;
    private int cursorRow = 0;

    private CellAttributes currentAttrs = CellAttributes.DEFAULT;

    private final TerminalLine[] screen;

    private final Deque<TerminalLine> scrollback = new ArrayDeque<>();

    //!!!!!!!!!!!!!!!
    //tba: text styles, resize
    //!!!!!!!!!!!!!!!
    public TerminalBuffer(int height, int width, int maxScrollback) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Dimensions must be positive");
        this.width = width;
        this.height = height;
        this.maxScrollback = maxScrollback;
        this.screen = new TerminalLine[height];
        for (int i = 0; i < height; i++) screen[i] = new TerminalLine(width);
    }

    //-------------------------text editing action----------------------------------
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

    public void insertBottomLine()
    {
        TerminalLine topLine = screen[0];

        for (int i = 0; i < height - 1; i++) screen[i] = screen[i + 1];
        screen[height - 1] = new TerminalLine(width);

        scrollback.addLast(topLine);
        while (scrollback.size() > maxScrollback) scrollback.removeFirst();
    }

    public void insertText(String text) {
        TerminalLine line = screen[cursorRow];

        List<Cell> insertCells = textToCells(text);
        int insertLen = 0;
        for (Cell c : insertCells) insertLen += Math.max(c.width, 1);

        Cell[] oldCells = new Cell[width];
        for (int i = 0; i < width; i++) oldCells[i] = line.getCell(i);

        int col = cursorCol;
        for (Cell cell : insertCells) {
            if (col >= width) break;
            int cw = Math.max(cell.width, 1);
            if (cw == 2 && col + 1 >= width) break;
            line.setCell(col, cell);
            if (cw == 2 && col + 1 < width) line.setCell(col + 1, Cell.continuation);
            col += cw;
        }

        int srcCol = cursorCol;
        int dstCol = cursorCol + insertLen;
        while (srcCol < width && dstCol < width) {
            Cell cell = oldCells[srcCol];
            int cw = Math.max(cell.width, 1);
            if (cell.width == 2 && dstCol + 1 >= width) {
                line.setCell(dstCol, Cell.empty);
            } else {
                line.setCell(dstCol, cell);
                if (cell.width == 2 && dstCol + 1 < width) {
                    line.setCell(dstCol + 1, Cell.continuation);
                }
            }
            srcCol += cw;
            dstCol += cw;
        }

        cursorCol = Math.min(cursorCol + insertLen, width);
    }
    public void scrollUpOneLine()
    {
        if(scrollback.isEmpty()) return;

        cursorRow = height - 1;
        while(checkEmptyLine(cursorRow)) cursorRow--;
        TerminalLine bottomLine = screen[cursorRow];
        if(cursorRow >= height - 1) return;

        for(int i = height - 1; i >= 1; i--) screen[i] = screen[i - 1];
        screen[0] = scrollback.removeLast();
    }

    public void fillLine(int row, Character ch) {
        if (row < 0 || row >= height) throw new IndexOutOfBoundsException("Row " + row + " out of range");
        TerminalLine line = screen[row];
        for (int col = 0; col < width; col++) {
            line.setCell(col, new Cell(ch, currentAttrs, 1));
        }
    }
    public void fillLine(Character ch) {
        fillLine(cursorRow, ch);
    }


    public void clearScreen() {
        for (int i = 0; i < height; i++) screen[i] = new TerminalLine(width);
        cursorCol = 0;
        cursorRow = 0;
    }

    public void clearAll() {
        clearScreen();
        scrollback.clear();
    }

    //---------------------cursor movement---------------------------

    public void setCursorPos(int col, int row)
    {
        cursorCol = Math.clamp(col, 0, width - 1);
        cursorRow = Math.clamp(row, 0, height - 1);
    }

    public void moveCursorUp(int n) {
        cursorRow = Math.max(0, cursorRow - n);
    }
    public void moveCursorUp() { moveCursorUp(1); }

    public void moveCursorDown(int n) {
        cursorRow = Math.min(height - 1, cursorRow + n);
    }
    public void moveCursorDown() { moveCursorDown(1); }

    public void moveCursorLeft(int n) {
        cursorCol = Math.max(0, cursorCol - n);
    }
    public void moveCursorLeft() { moveCursorLeft(1); }

    public void moveCursorRight(int n) {
        cursorCol = Math.min(width - 1, cursorCol + n);
    }
    public void moveCursorRight() { moveCursorRight(1); }

    public void moveCursorToStartOfLine()
    {
        cursorCol = 0;
    }

    public void moveCursorToEndOfText(TerminalLine line){
        cursorCol = Math.min(line.getCells().length, width - 1);
    }
    
    //-----------------relative content access----------------------------

    public Cell getScreenCell(int col, int row) {
        return screen[row].getCell(col);
    }

    public CellAttributes getScreenCellAttrs(int col, int row) {
        return screen[row].getCell(col).attrs;
    }

    public String getScreenLine(int row) {
        return screen[row].toString();
    }

    public String getScreenContent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < height; i++) {
            if (i > 0) sb.append('\n');
            sb.append(screen[i].toString());
        }
        return sb.toString();
    }
    
    //-----------------absolute content access----------------------------

    public int getTotalLines() { return scrollback.size() + height; }

    private TerminalLine lineAt(int absRow) {
        int sbSize = scrollback.size();
        if (absRow < sbSize) {
            int idx = 0;
            for (TerminalLine line : scrollback) {
                if (idx == absRow) return line;
                idx++;
            }
            throw new IndexOutOfBoundsException("absRow " + absRow);
        }
        return screen[absRow - sbSize];
    }

    public Cell getCell(int col, int absRow) {
        return lineAt(absRow).getCell(col);
    }

    public CellAttributes getCellAttrs(int col, int absRow) {
        return lineAt(absRow).getCell(col).attrs;
    }

    public String getLine(int absRow) {
        return lineAt(absRow).toString();
    }

    public String getAllContent() {
        int total = getTotalLines();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < total; i++) {
            if (i > 0) sb.append('\n');
            sb.append(getLine(i));
        }
        return sb.toString();
    }

    //--------------------wide char action----------------------------

    private void eraseWideCharAt(TerminalLine line, int col) {
        Cell cell = line.getCell(col);
        if (cell.width == 0 && col > 0) {
            line.setCell(col - 1, Cell.empty);
        } else if (cell.width == 2 && col + 1 < width) {
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

    public boolean checkEmptyLine(int row)
    {
        Cell[] rowCells = screen[row].getCells();

        return rowCells[0].equals(Cell.empty);
    }

    private List<Cell> textToCells(String text) {
        List<Cell> result = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int cw = displayWidth(ch);
            result.add(new Cell(ch, currentAttrs, cw));
            if (cw == 2) result.add(Cell.continuation);
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TerminalBuffer(").append(width).append('x').append(height)
                .append(", scrollback=").append(scrollback.size()).append('/').append(maxScrollback).append(")\n");
        sb.append("Cursor: (").append(cursorCol).append(", ").append(cursorRow).append(")\n");

        sb.append("─── Scrollback ───\n");
        int i = 0;
        for (TerminalLine line : scrollback) {
            sb.append(i++).append(": ").append(line.toString()).append('\n');
        }

        sb.append("───── Screen ─────\n");
        for (int r = 0; r < height; r++) {
            sb.append(r).append(": ").append(screen[r].toString()).append('\n');
        }
        return sb.toString();
    }


}
