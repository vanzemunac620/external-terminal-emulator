package terminal;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class TerminalBufferTest {

    private TerminalBuffer buf;

    @BeforeEach
    void setup() {
        buf = new TerminalBuffer(5, 12, 20);
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class InitialState {

        @Test
        void initialCursorAtOrigin() {
            assertEquals(0, buf.getCursorCol());
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        void initialScreenIsEmpty() {
            for (int row = 0; row < buf.getHeight(); row++) {
                assertEquals("", buf.getScreenLine(row));
            }
        }

        @Test
        void initialScrollbackIsEmpty() {
            assertEquals(0, buf.getScrollbackSize());
        }

        @Test
        void dimensionsCorrect() {
            assertEquals(10, buf.getWidth());
            assertEquals(5, buf.getHeight());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class Attributes {

        @Test
        void setForegroundColor() {
            buf.setForeground(TerminalColor.RED);
            assertEquals(TerminalColor.RED, buf.getCurrentAttrs().foreground);
        }

        @Test
        void setBackgroundColor() {
            buf.setBackground(TerminalColor.BLUE);
            assertEquals(TerminalColor.BLUE, buf.getCurrentAttrs().background);
        }

        @Test
        void setStyle() {
            buf.setStyle(new TextStyle(true, true, false));
            assertTrue(buf.getCurrentAttrs().style.bold);
            assertTrue(buf.getCurrentAttrs().style.italic);
            assertFalse(buf.getCurrentAttrs().style.underline);
        }

        @Test
        void resetAttributesReturnsToDefault() {
            buf.setForeground(TerminalColor.RED);
            buf.setBackground(TerminalColor.GREEN);
            buf.setStyle(new TextStyle(true, false, false));
            buf.resetAttributes();
            assertEquals(CellAttributes.DEFAULT, buf.getCurrentAttrs());
        }

        @Test
        void setAllAttributesAtOnce() {
            buf.setAttributes(TerminalColor.CYAN, TerminalColor.MAGENTA, new TextStyle(false, false, true));
            assertEquals(TerminalColor.CYAN,    buf.getCurrentAttrs().foreground);
            assertEquals(TerminalColor.MAGENTA, buf.getCurrentAttrs().background);
            assertTrue(buf.getCurrentAttrs().style.underline);
        }

        @Test
        void writtenCellsCarryCurrentAttributes() {
            buf.setForeground(TerminalColor.YELLOW);
            buf.setStyle(new TextStyle(true, false, false));
            buf.writeText("A");
            CellAttributes attrs = buf.getScreenCellAttrs(0, 0);
            assertEquals(TerminalColor.YELLOW, attrs.foreground);
            assertTrue(attrs.style.bold);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class CursorMovement {

        @Test
        void setCursorPosToValidPosition() {
            buf.setCursorPos(5, 3);
            assertEquals(5, buf.getCursorRow());
            assertEquals(3, buf.getCursorCol());
        }

        @Test
        void cursorClampedToScreenBoundsOnsetCursorPos() {
            buf.setCursorPos(-1, 100);
            assertEquals(0, buf.getCursorRow());
            assertEquals(buf.getWidth() - 1, buf.getCursorCol());
        }

        @Test
        void moveCursorUp() {
            buf.setCursorPos(3, 0);
            buf.moveCursorUp(2);
            assertEquals(1, buf.getCursorRow());
        }

        @Test
        void moveCursorDown() {
            buf.setCursorPos(2, 0);
            buf.moveCursorDown(2);
            assertEquals(4, buf.getCursorRow());
        }

        @Test
        void moveCursorLeft() {
            buf.setCursorPos(0, 5);
            buf.moveCursorLeft(3);
            assertEquals(2, buf.getCursorCol());
        }

        @Test
        void moveCursorRight() {
            buf.setCursorPos(0, 3);
            buf.moveCursorRight(4);
            assertEquals(7, buf.getCursorCol());
        }

        @Test
        void cursorDoesNotGoAboveTop() {
            buf.setCursorPos(0, 0);
            buf.moveCursorUp(99);
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        void cursorDoesNotGoBelowBottom() {
            buf.setCursorPos(buf.getHeight() - 1, 0);
            buf.moveCursorDown(99);
            assertEquals(buf.getHeight() - 1, buf.getCursorRow());
        }

        @Test
        void cursorDoesNotGoLeftOfColumn0() {
            buf.setCursorPos(0, 0);
            buf.moveCursorLeft(99);
            assertEquals(0, buf.getCursorCol());
        }

        @Test
        void cursorDoesNotGoRightOfLastColumn() {
            buf.setCursorPos(0, buf.getWidth() - 1);
            buf.moveCursorRight(99);
            assertEquals(buf.getWidth() - 1, buf.getCursorCol());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class WriteText {

        @Test
        void writeSimpleString() {
            buf.writeText("Hello");
            assertEquals("Hello", buf.getScreenLine(0));
            assertEquals(5, buf.getCursorCol());
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        void writeOverwritesExistingContent() {
            buf.writeText("AAAAAAAAAA"); // fills line 0
            buf.setCursorPos(0, 2);
            buf.writeText("BB");
            assertEquals('A', buf.getScreenCell(0, 0).ch.charValue());
            assertEquals('A', buf.getScreenCell(0, 1).ch.charValue());
            assertEquals('B', buf.getScreenCell(0, 2).ch.charValue());
            assertEquals('B', buf.getScreenCell(0, 3).ch.charValue());
            assertEquals('A', buf.getScreenCell(0, 4).ch.charValue());
        }

        @Test
        void writeDoesNotWrap() {
            buf.setCursorPos(0, 8);
            buf.writeText("ABCDE"); // only "AB" fits
            assertEquals('A', buf.getScreenCell(0, 8).ch.charValue());
            assertEquals('B', buf.getScreenCell(0, 9).ch.charValue());
            assertEquals(buf.getWidth(), buf.getCursorCol());
        }

        @Test
        void writeAdvancesCursor() {
            buf.setCursorPos(2, 3);
            buf.writeText("XYZ");
            assertEquals(6, buf.getCursorCol());
            assertEquals(2, buf.getCursorRow());
        }

        @Test
        void writeEmptyStringDoesNothing() {
            buf.writeText("");
            assertEquals(0, buf.getCursorCol());
            assertEquals("", buf.getScreenLine(0));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class InsertText {

        @Test
        void insertAtStartShiftsContentRight() {
            buf.writeText("HELLO");
            buf.setCursorPos(0, 0);
            buf.insertText("AB");
            assertEquals('A', buf.getScreenCell(0, 0).ch.charValue());
            assertEquals('B', buf.getScreenCell(0, 1).ch.charValue());
            assertEquals('H', buf.getScreenCell(0, 2).ch.charValue());
        }

        @Test
        void insertInMiddleShiftsContentRight() {
            buf.writeText("ABCDE");
            buf.setCursorPos(0, 2);
            buf.insertText("XX");
            assertEquals('A', buf.getScreenCell(0, 0).ch.charValue());
            assertEquals('B', buf.getScreenCell(0, 1).ch.charValue());
            assertEquals('X', buf.getScreenCell(0, 2).ch.charValue());
            assertEquals('X', buf.getScreenCell(0, 3).ch.charValue());
            assertEquals('C', buf.getScreenCell(0, 4).ch.charValue());
            assertEquals('D', buf.getScreenCell(0, 5).ch.charValue());
            assertEquals('E', buf.getScreenCell(0, 6).ch.charValue());
        }

        @Test
        void insertAdvancesCursor() {
            buf.setCursorPos(1, 1);
            buf.insertText("ABCD");
            assertEquals(5, buf.getCursorCol());
            assertEquals(1, buf.getCursorRow());
        }

        @Test
        void insertOverflowIsDiscarded() {
            buf.writeText("AAAAAAAAAA"); // full line
            buf.setCursorPos(0, 8);
            buf.insertText("BB");
            assertEquals('B', buf.getScreenCell(0, 8).ch.charValue());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class FillLine {

        @Test
        void fillCurrentLineWithChar() {
            buf.fillLine('-');
            for (int col = 0; col < buf.getWidth(); col++) {
                assertEquals('-', buf.getScreenCell(0, col).ch.charValue());
            }
        }

        @Test
        void fillSpecificRow() {
            buf.fillLine(3, '*');
            for (int col = 0; col < buf.getWidth(); col++) {
                assertEquals('*', buf.getScreenCell(3, col).ch.charValue());
            }
            // Other rows untouched
            assertNull(buf.getScreenCell(0, 0).ch);
        }

        @Test
        void fillWithNullClearsLine() {
            buf.writeText("Hello");
            buf.setCursorPos(0, 0);
            buf.fillLine(0, null);
            assertEquals("", buf.getScreenLine(0));
        }

        @Test
        void fillCarriesCurrentAttributes() {
            buf.setBackground(TerminalColor.GREEN);
            buf.fillLine(0, ' ');
            assertEquals(TerminalColor.GREEN, buf.getScreenCellAttrs(0, 0).background);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class insertBottomLine {

        @Test
        void topLineScrollsIntoScrollback() {
            buf.writeText("FirstLine");
            buf.insertBottomLine();
            assertEquals(1, buf.getScrollbackSize());
            assertEquals("FirstLine", buf.getLine(0));
        }

        @Test
        void screenShiftsUp() {
            buf.setCursorPos(0, 0); buf.writeText("Row0");
            buf.setCursorPos(1, 0); buf.writeText("Row1");
            buf.insertBottomLine();
            assertEquals("Row0", buf.getLine(0));
            assertEquals("Row1", buf.getScreenLine(0));
        }

        @Test
        void newBottomLineIsEmpty() {
            buf.insertBottomLine();
            assertEquals("", buf.getScreenLine(buf.getHeight() - 1));
        }

        @Test
        void scrollbackMaxSizeEnforced() {
            TerminalBuffer smallBuf = new TerminalBuffer(5, 10, 3);
            for (int i = 0; i < 10; i++) {
                smallBuf.setCursorPos(0, 0);
                smallBuf.writeText("Line " + i);
                smallBuf.insertBottomLine();
            }
            assertEquals(3, smallBuf.getScrollbackSize());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class ClearOperations {

        @Test
        void clearScreenEmptiesAllScreenRows() {
            buf.writeText("Hello");
            buf.clearScreen();
            for (int row = 0; row < buf.getHeight(); row++) {
                assertEquals("", buf.getScreenLine(row));
            }
        }

        @Test
        void clearScreenResetsCursor() {
            buf.setCursorPos(3, 5);
            buf.clearScreen();
            assertEquals(0, buf.getCursorCol());
            assertEquals(0, buf.getCursorRow());
        }

        @Test
        void clearScreenPreservesScrollback() {
            buf.setCursorPos(0, 0); buf.writeText("SBLine");
            buf.insertBottomLine();
            buf.clearScreen();
            assertEquals(1, buf.getScrollbackSize());
            assertEquals("SBLine", buf.getLine(0));
        }

        @Test
        void clearAllRemovesScreenAndScrollback() {
            buf.setCursorPos(0, 0); buf.writeText("SBLine");
            buf.insertBottomLine();
            buf.writeText("ScreenLine");
            buf.clearAll();
            assertEquals(0, buf.getScrollbackSize());
            assertEquals("", buf.getScreenLine(0));
            assertEquals(0, buf.getCursorCol());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class ContentAccess {

        @Test
        void getScreenCellCharacter() {
            buf.writeText("ABC");
            assertEquals('A', buf.getScreenCell(0, 0).ch.charValue());
            assertEquals('B', buf.getScreenCell(0, 1).ch.charValue());
            assertEquals('C', buf.getScreenCell(0, 2).ch.charValue());
            assertNull(buf.getScreenCell(3, 0).ch);
        }

        @Test
        void getScreenContentAsString() {
            buf.setCursorPos(0, 0); buf.writeText("Row0");
            buf.setCursorPos(1, 0); buf.writeText("Row1");
            String content = buf.getScreenContent();
            assertTrue(content.contains("Row0"));
            assertTrue(content.contains("Row1"));
        }

        @Test
        void getAllContentIncludesScrollback() {
            buf.setCursorPos(0, 0); buf.writeText("SB");
            buf.insertBottomLine();
            buf.setCursorPos(0, 0); buf.writeText("Screen");
            String all = buf.getAllContent();
            assertTrue(all.contains("SB"));
            assertTrue(all.contains("Screen"));
        }

        @Test
        void getLineFromScrollback() {
            buf.setCursorPos(0, 0); buf.writeText("InScrollback");
            buf.insertBottomLine();
            assertEquals("InScrollback", buf.getLine(0));
        }

        @Test
        void getCellAttrsFromScrollback() {
            buf.setForeground(TerminalColor.RED);
            buf.writeText("R");
            buf.insertBottomLine();
            int sbRow = buf.getScrollbackSize() - 1;
            assertEquals(TerminalColor.RED, buf.getCellAttrs(0, sbRow).foreground);
        }

        @Test
        void totalLinesIsScrollbackPlusHeight() {
            for (int i = 0; i < 3; i++) buf.insertBottomLine();
            assertEquals(3 + buf.getHeight(), buf.getTotalLines());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class WideCharacters {

        @Test
        void wideCharOccupiesTwoCells() {
            buf.writeText("日"); // CJK ideograph, width=2
            assertEquals('日', buf.getScreenCell(0, 0).ch.charValue());
            assertEquals(2, buf.getScreenCell(0, 0).width);
            assertEquals(0, buf.getScreenCell(0, 1).width); // continuation
            assertEquals(2, buf.getCursorCol());
        }

        @Test
        void multipleWideCharsAdvanceCursorByTwoEach() {
            buf.writeText("日本");
            assertEquals('日', buf.getScreenCell(0, 0).ch.charValue());
            assertEquals('本', buf.getScreenCell(0, 2).ch.charValue());
            assertEquals(4, buf.getCursorCol());
        }

        @Test
        void wideCharAtEdgeIsSkipped() {
            buf.setCursorPos(0, 11); // last column (width=10)
            buf.writeText("日");  // needs 2 cols — doesn't fit
            assertNull(buf.getScreenCell(0, 9).ch);
            assertEquals(11, buf.getCursorCol());
        }

        @Test
        void overwritingWideCharClearsContinuation() {
            buf.writeText("日"); // cells 0,1
            buf.setCursorPos(0, 0);
            buf.writeText("A"); // overwrites cell 0
            assertEquals('A', buf.getScreenCell(0, 0).ch.charValue());
            assertEquals(1, buf.getScreenCell(0, 0).width);
            assertNull(buf.getScreenCell(0, 1).ch);
            assertNotEquals(0, buf.getScreenCell(0, 1).width);
        }

        @Test
        void writingOverContinuationErasesLeader() {
            buf.writeText("日"); // cells 0,1
            buf.setCursorPos(0, 1);
            buf.writeText("B"); // write over continuation
            assertEquals('B', buf.getScreenCell(0, 1).ch.charValue());
            assertNull(buf.getScreenCell(0, 0).ch);
            assertNotEquals(2, buf.getScreenCell(0, 0).width);
        }

        @Test
        void wideCharDisplayString() {
            buf.writeText("AB日CD");
            assertEquals("AB日CD", buf.getScreenLine(0));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class Resize {

        @Test
        void resizeNarrowerTruncatesLines() {
            buf.writeText("ABCDEFGHIJ"); // 10 chars
            buf.resize(5, 5);
            assertEquals(5, buf.getWidth());
            assertEquals("ABCDE", buf.getScreenLine(0));
        }

        @Test
        void resizeWiderPadsWithEmptyCells() {
            buf.writeText("Hello");
            buf.resize(5, 15);
            assertEquals(15, buf.getWidth());
            assertEquals("Hello", buf.getScreenLine(0));
        }

        @Test
        void resizeTallerAddsEmptyLines() {
            buf.resize(8, 10);
            assertEquals(8, buf.getHeight());
            assertEquals("", buf.getScreenLine(7));
        }

        @Test
        void resizeShorterMovesTopLinesToScrollback() {
            buf.setCursorPos(0, 0); buf.writeText("Row0");
            buf.setCursorPos(1, 0); buf.writeText("Row1");
            buf.setCursorPos(2, 0); buf.writeText("Row2");
            buf.resize(3, 10);
            assertEquals(3, buf.getHeight());
            assertEquals(2, buf.getScrollbackSize());
            assertEquals("Row0", buf.getLine(0));
            assertEquals("Row1", buf.getLine(1));
            assertEquals("Row2", buf.getScreenLine(0));
        }

        @Test
        void cursorClampedAfterResize() {
            buf.setCursorPos(4, 9);
            buf.resize(3, 5);
            assertTrue(buf.getCursorCol() < 5);
            assertTrue(buf.getCursorRow() < 3);
        }

        @Test
        void resizeAlsoResizesScrollbackLines() {
            buf.setCursorPos(0, 0); buf.writeText("SCROLLBACK");
            buf.insertBottomLine();
            buf.resize(5, 5);
            assertEquals("SCROL", buf.getLine(0));
        }

        @Test
        void resizePreservesContent() {
            buf.setCursorPos(0, 0); buf.writeText("Hello");
            buf.setCursorPos(1, 0); buf.writeText("World");
            buf.resize(10, 20);
            assertEquals("Hello", buf.getScreenLine(0));
            assertEquals("World", buf.getScreenLine(1));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    class EdgeCases {

        @Test
        void oneByOneBufferWorks() {
            TerminalBuffer tiny = new TerminalBuffer(1, 1, 10);
            tiny.writeText("A");
            assertEquals('A', tiny.getScreenCell(0, 0).ch.charValue());
            tiny.insertBottomLine();
            assertEquals("A", tiny.getLine(0));
        }

        @Test
        void fillAndInsertLineInteraction() {
            buf.fillLine(0, '-');
            buf.insertBottomLine();
            assertEquals("------------", buf.getLine(0));
        }

        @Test
        void writeAtMaxColThenInsertLine() {
            buf.setCursorPos(0, buf.getWidth() - 1);
            buf.writeText("Z");
            buf.insertBottomLine();
            String sbLine = buf.getLine(0);
            assertTrue(sbLine.stripTrailing().endsWith("Z"));
        }

        @Test
        void all16ColorsCanBeSet() {
            for (TerminalColor color : TerminalColor.values()) {
                buf.setForeground(color);
                assertEquals(color, buf.getCurrentAttrs().foreground);
            }
        }

        @Test
        void scrollbackDoesNotExceedMaxScrollback() {
            TerminalBuffer b = new TerminalBuffer(2, 10, 5);
            for (int i = 0; i < 20; i++) {
                b.setCursorPos(0, 0);
                b.writeText("line " + i);
                b.insertBottomLine();
            }
            assertTrue(b.getScrollbackSize() <= 5);
        }

        @Test
        void getLineFromScreenViaAbsoluteIndex() {
            buf.setCursorPos(0, 0);
            buf.writeText("ScreenRow0");
            // No scrollback, so absolute row 0 == screen row 0
            assertEquals("ScreenRow0", buf.getLine(0));
        }

        @Test
        void insertTextAtLineBoundary() {
            buf.setCursorPos(0, buf.getWidth() - 1);
            buf.insertText("X");
            assertEquals('X', buf.getScreenCell(0, buf.getWidth() - 1).ch.charValue());
        }
    }
}
