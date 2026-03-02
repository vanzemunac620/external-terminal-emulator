import terminal.TerminalBuffer;

public class Main {
    public static void main(String[] args) {
        TerminalBuffer tb = new TerminalBuffer(10,11, 5);
        tb.writeText("Hello World");
        tb.moveCursorDown();
        tb.moveCursorToStartOfLine();
        tb.writeText("vanzemunac");
        System.out.println(tb);
    }
}
