import terminal.TerminalBuffer;

public class Main {
    public static void main(String[] args) {
        TerminalBuffer tb = new TerminalBuffer(3,11, 5);
        tb.writeText("Hello World");
        tb.moveCursorDown();
        tb.moveCursorToStartOfLine();
        tb.writeText("aacc");
        tb.insertBottomLine();
        System.out.println(tb);
        tb.scrollUpOneLine();
        System.out.println(tb);
        tb.fillLine('a');
        tb.setCursorPos(1, 2);
        tb.insertText("bb");
        tb.setCursorPos(1, 6);
        System.out.println(tb);
        tb.resize(2, 6);
        System.out.println(tb);
    }
}
