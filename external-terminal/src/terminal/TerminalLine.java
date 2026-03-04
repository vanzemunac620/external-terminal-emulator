package terminal;

public class TerminalLine {
    private final int width;
    private final Cell[] cells;

    public TerminalLine(int width) {
        this.width = width;
        this.cells = new Cell[width];
        for (int i = 0; i < width; i++) {
            cells[i] = Cell.EMPTY;
        }
    }

    public void setCell(int col, Cell cell) {
        if (col < 0 || col >= width) {
            throw new IndexOutOfBoundsException("Column " + col + " out of range [0, " + width + ")");
        }
        cells[col] = cell;
    }

    public int getWidth() {
        return width;
    }

    public Cell getCell(int col) {
        if (col < 0 || col >= width) {
            throw new IndexOutOfBoundsException("Column " + col + " out of range [0, " + width + ")");
        }
        return cells[col];
    }

    public Cell[] getCells() {
        return cells;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int col = 0;
        while (col < width) {
            Cell cell = cells[col];
            if (cell.width == 0) {
                col++;
            } else if (cell.ch != null) {
                sb.append(cell.ch);
                col += cell.width;
            } else {
                sb.append(' ');
                col++;
            }
        }

        int end = sb.length();
        while (end > 0 && sb.charAt(end - 1) == ' ') end--;
        return sb.substring(0, end);
    }
}
