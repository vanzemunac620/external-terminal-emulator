package gui;

import terminal.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.EnumMap;
import java.util.Map;

public class TerminalBufferApp extends JFrame {

    // ── Colour palette mapping TerminalColor → AWT Color ─────────────────────────

    private static final Map<TerminalColor, Color> TERM_COLORS = new EnumMap<>(TerminalColor.class);
    static {
        TERM_COLORS.put(TerminalColor.BLACK,          new Color(0x1e1e1e));
        TERM_COLORS.put(TerminalColor.RED,            new Color(0xcc3333));
        TERM_COLORS.put(TerminalColor.GREEN,          new Color(0x33aa44));
        TERM_COLORS.put(TerminalColor.YELLOW,         new Color(0xccaa33));
        TERM_COLORS.put(TerminalColor.BLUE,           new Color(0x3388cc));
        TERM_COLORS.put(TerminalColor.MAGENTA,        new Color(0xaa33aa));
        TERM_COLORS.put(TerminalColor.CYAN,           new Color(0x33aacc));
        TERM_COLORS.put(TerminalColor.WHITE,          new Color(0xcccccc));
        TERM_COLORS.put(TerminalColor.BRIGHT_BLACK,   new Color(0x555555));
        TERM_COLORS.put(TerminalColor.BRIGHT_RED,     new Color(0xff5555));
        TERM_COLORS.put(TerminalColor.BRIGHT_GREEN,   new Color(0x55ff55));
        TERM_COLORS.put(TerminalColor.BRIGHT_YELLOW,  new Color(0xffff55));
        TERM_COLORS.put(TerminalColor.BRIGHT_BLUE,    new Color(0x5599ff));
        TERM_COLORS.put(TerminalColor.BRIGHT_MAGENTA, new Color(0xff55ff));
        TERM_COLORS.put(TerminalColor.BRIGHT_CYAN,    new Color(0x55ffff));
        TERM_COLORS.put(TerminalColor.BRIGHT_WHITE,   new Color(0xffffff));
    }

    private static final Color DEFAULT_FG = new Color(0xe0e0e0);
    private static final Color DEFAULT_BG = new Color(0x1a1a2e);
    private static final Color CURSOR_COLOR = new Color(0xe0e0e0, true);
    private static final Color SCROLLBACK_TINT = new Color(0x0d0d1a);
    private static final Color GRID_LINE_COLOR = new Color(0x2a2a3e);


    private final TerminalBuffer buffer;
    private TerminalColor selectedFg = null;  // null = default
    private TerminalColor selectedBg = null;
    private boolean boldActive    = false;
    private boolean italicActive  = false;
    private boolean underlineActive = false;
    private boolean insertMode    = false; // false = overwrite, true = insert


    private TerminalCanvas canvas;
    private JTextArea scrollbackArea;
    private JLabel statusLabel;
    private JLabel cursorLabel;
    private JLabel scrollbackCountLabel;
    private JTextField inputField;
    private JButton colorSwatchFg;
    private JButton colorSwatchBg;
    private JToggleButton boldBtn, italicBtn, underlineBtn;
    private JToggleButton insertModeBtn;

    private boolean cursorVisible = true;
    private final Timer blinkTimer;


    public TerminalBufferApp() {
        super("Terminal Buffer Explorer");
        buffer = new TerminalBuffer(25, 90, 500);
        seedDemoContent();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(new Color(0x0d0d1a));

        add(buildTopBar(),      BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildControlPanel(),BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(500, 600));
        setLocationRelativeTo(null);
        setVisible(true);

        // Cursor blink every 530 ms
        blinkTimer = new Timer(530, e -> {
            cursorVisible = !cursorVisible;
            canvas.repaint();
        });
        blinkTimer.start();

        // Focus the input field so the user can type immediately
        SwingUtilities.invokeLater(() -> inputField.requestFocusInWindow());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI builders
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(0x16213e));
        bar.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel title = new JLabel("Terminal Buffer Explorer");
        title.setForeground(new Color(0x7ec8e3));
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        bar.add(title, BorderLayout.WEST);

        cursorLabel = new JLabel("Cursor: (0, 0)");
        cursorLabel.setForeground(new Color(0x888888));
        cursorLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        bar.add(cursorLabel, BorderLayout.EAST);

        return bar;
    }

    private JSplitPane buildCenterPanel() {
        canvas = new TerminalCanvas();

        JScrollPane canvasScroll = new JScrollPane(canvas);
        canvasScroll.setBackground(DEFAULT_BG);
        canvasScroll.getViewport().setBackground(DEFAULT_BG);
        canvasScroll.setBorder(BorderFactory.createLineBorder(new Color(0x2a2a3e), 1));

        // Right info panel
        JPanel infoPanel = buildInfoPanel();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvasScroll, infoPanel);
        split.setDividerLocation(720);
        split.setDividerSize(4);
        split.setBackground(new Color(0x0d0d1a));
        split.setBorder(null);
        return split;
    }

    private JPanel buildInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(new Color(0x0d0d1a));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.setPreferredSize(new Dimension(220, 0));

        // Status card
        JPanel statusCard = styledCard("Buffer Status");
        statusLabel = new JLabel("<html>Width: 80<br>Height: 25<br>Scrollback: 0 / 500</html>");
        statusLabel.setForeground(new Color(0xaaaaaa));
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statusCard.add(statusLabel);
        panel.add(statusCard, BorderLayout.NORTH);

        // Scrollback viewer
        JPanel sbCard = styledCard("Scrollback");
        sbCard.setLayout(new BorderLayout(0, 4));

        scrollbackCountLabel = new JLabel("0 lines");
        scrollbackCountLabel.setForeground(new Color(0x7ec8e3));
        scrollbackCountLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sbCard.add(scrollbackCountLabel, BorderLayout.NORTH);

        scrollbackArea = new JTextArea(8, 20);
        scrollbackArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        scrollbackArea.setBackground(SCROLLBACK_TINT);
        scrollbackArea.setForeground(new Color(0x778899));
        scrollbackArea.setCaretColor(new Color(0x778899));
        scrollbackArea.setEditable(false);
        scrollbackArea.setBorder(new EmptyBorder(4, 4, 4, 4));
        JScrollPane sbScroll = new JScrollPane(scrollbackArea);
        sbScroll.setBorder(BorderFactory.createLineBorder(new Color(0x2a2a3e)));
        sbCard.add(sbScroll, BorderLayout.CENTER);
        panel.add(sbCard, BorderLayout.CENTER);

        // Quick actions
        JPanel actionsCard = styledCard("Quick Actions");
        actionsCard.setLayout(new GridLayout(0, 1, 0, 3));

        actionsCard.add(actionButton("Insert empty line at bottom", e -> {
            buffer.insertBottomLine();
            refreshAll();
        }));
        actionsCard.add(actionButton("Clear screen", e -> {
            buffer.clearScreen();
            refreshAll();
        }));
        actionsCard.add(actionButton("Clear all (+ scrollback)", e -> {
            buffer.clearAll();
            refreshAll();
        }));
        actionsCard.add(actionButton("Fill current line with '─'", e -> {
            buffer.fillLine(buffer.getCursorRow(), '─');
            refreshAll();
        }));
        actionsCard.add(actionButton("Reset attributes", e -> {
            buffer.resetAttributes();
            selectedFg = null;
            selectedBg = null;
            boldActive = false;
            italicActive = false;
            underlineActive = false;
            boldBtn.setSelected(false);
            italicBtn.setSelected(false);
            underlineBtn.setSelected(false);
            updateColorSwatches();
        }));

        panel.add(actionsCard, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildControlPanel() {
        JPanel outer = new JPanel(new BorderLayout(0, 4));
        outer.setBackground(new Color(0x16213e));
        outer.setBorder(new EmptyBorder(8, 10, 10, 10));

        // ── Row 1: text input ─────────────────────────────────────────────────
        JPanel inputRow = new JPanel(new BorderLayout(6, 0));
        inputRow.setOpaque(false);

        JLabel inputLabel = new JLabel("Input:");
        inputLabel.setForeground(new Color(0x7ec8e3));
        inputLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        inputRow.add(inputLabel, BorderLayout.WEST);

        inputField = new JTextField();
        inputField.setFont(new Font("Monospaced", Font.PLAIN, 13));
        inputField.setBackground(new Color(0x0d0d1a));
        inputField.setForeground(DEFAULT_FG);
        inputField.setCaretColor(DEFAULT_FG);
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x3a3a5e)),
                new EmptyBorder(4, 6, 4, 6)
        ));
        inputField.addActionListener(e -> commitInput());
        inputRow.add(inputField, BorderLayout.CENTER);

        insertModeBtn = new JToggleButton("OVR");
        insertModeBtn.setFont(new Font("Monospaced", Font.BOLD, 11));
        styleToggleButton(insertModeBtn, new Color(0x885500), new Color(0xff9900));
        insertModeBtn.addActionListener(e -> {
            insertMode = insertModeBtn.isSelected();
            insertModeBtn.setText(insertMode ? "INS" : "OVR");
        });
        inputRow.add(insertModeBtn, BorderLayout.EAST);

        // ── Row 2: attributes + cursor controls ───────────────────────────────
        JPanel attrRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        attrRow.setOpaque(false);

        // FG color
        attrRow.add(dimLabel("FG:"));
        colorSwatchFg = new JButton("default");
        colorSwatchFg.setFont(new Font("Monospaced", Font.PLAIN, 11));
        colorSwatchFg.setPreferredSize(new Dimension(90, 24));
        styleColorSwatch(colorSwatchFg, DEFAULT_FG, DEFAULT_BG);
        colorSwatchFg.addActionListener(e -> pickColor(true));
        attrRow.add(colorSwatchFg);

        // BG color
        attrRow.add(dimLabel("BG:"));
        colorSwatchBg = new JButton("default");
        colorSwatchBg.setFont(new Font("Monospaced", Font.PLAIN, 11));
        colorSwatchBg.setPreferredSize(new Dimension(90, 24));
        styleColorSwatch(colorSwatchBg, DEFAULT_BG, DEFAULT_FG);
        colorSwatchBg.addActionListener(e -> pickColor(false));
        attrRow.add(colorSwatchBg);

        attrRow.add(Box.createHorizontalStrut(8));

        // Style toggles
        boldBtn = new JToggleButton("B");
        boldBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        styleToggleButton(boldBtn, new Color(0x333355), new Color(0x7ec8e3));
        boldBtn.addActionListener(e -> { boldActive = boldBtn.isSelected(); applyAttrs(); });
        attrRow.add(boldBtn);

        italicBtn = new JToggleButton("I");
        italicBtn.setFont(new Font("SansSerif", Font.ITALIC, 12));
        styleToggleButton(italicBtn, new Color(0x333355), new Color(0x7ec8e3));
        italicBtn.addActionListener(e -> { italicActive = italicBtn.isSelected(); applyAttrs(); });
        attrRow.add(italicBtn);

        underlineBtn = new JToggleButton("U");
        underlineBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        styleToggleButton(underlineBtn, new Color(0x333355), new Color(0x7ec8e3));
        underlineBtn.addActionListener(e -> { underlineActive = underlineBtn.isSelected(); applyAttrs(); });
        attrRow.add(underlineBtn);

        attrRow.add(Box.createHorizontalStrut(12));

        // Cursor movement buttons
        attrRow.add(dimLabel("Cursor:"));
        attrRow.add(cursorMoveBtn("◀", () -> { buffer.moveCursorLeft(); refreshAll(); }));
        attrRow.add(cursorMoveBtn("▲", () -> { buffer.moveCursorUp(); refreshAll(); }));
        attrRow.add(cursorMoveBtn("▼", () -> { buffer.moveCursorDown(); refreshAll(); }));
        attrRow.add(cursorMoveBtn("▶", () -> { buffer.moveCursorRight(); refreshAll(); }));

        attrRow.add(Box.createHorizontalStrut(12));

        // Resize controls
        attrRow.add(dimLabel("Resize W:"));
        JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(80, 10, 200, 1));
        styleSpinner(widthSpinner);
        attrRow.add(widthSpinner);

        attrRow.add(dimLabel("H:"));
        JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(24, 4, 60, 1));
        styleSpinner(heightSpinner);
        attrRow.add(heightSpinner);

        JButton resizeBtn = new JButton("Apply");
        styleActionButton(resizeBtn);
        resizeBtn.addActionListener(e -> {
            int nw = (Integer) widthSpinner.getValue();
            int nh = (Integer) heightSpinner.getValue();
            buffer.resize(nh, nw);
            canvas.revalidate();
            refreshAll();
        });
        attrRow.add(resizeBtn);

        outer.add(inputRow, BorderLayout.CENTER);
        outer.add(attrRow, BorderLayout.SOUTH);
        return outer;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Logic helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void commitInput() {
        String text = inputField.getText();
        if (text.isEmpty()) return;
        if (insertMode) {
            buffer.insertText(text);
        } else {
            buffer.writeText(text);
        }
        inputField.setText("");
        refreshAll();
        inputField.requestFocusInWindow();
    }

    private void applyAttrs() {
        buffer.setAttributes(
                selectedFg,
                selectedBg,
                new TextStyle(boldActive, italicActive, underlineActive)
        );
    }

    private void pickColor(boolean isFg) {
        // Build a small popup grid of all 16 terminal colours + "default"
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(new Color(0x16213e));
        popup.setBorder(BorderFactory.createLineBorder(new Color(0x3a3a5e)));
        JPanel grid = new JPanel(new GridLayout(3, 6, 2, 2));
        grid.setBackground(new Color(0x16213e));
        grid.setBorder(new EmptyBorder(6, 6, 6, 6));

        // "default" swatch
        JButton defBtn = new JButton("def");
        defBtn.setFont(new Font("Monospaced", Font.PLAIN, 10));
        defBtn.setBackground(isFg ? DEFAULT_BG : DEFAULT_FG);
        defBtn.setForeground(isFg ? DEFAULT_FG : DEFAULT_BG);
        defBtn.setBorder(BorderFactory.createLineBorder(new Color(0x555577), 1));
        defBtn.addActionListener(e -> {
            if (isFg) { selectedFg = null; } else { selectedBg = null; }
            applyAttrs();
            updateColorSwatches();
            popup.setVisible(false);
        });
        grid.add(defBtn);

        for (TerminalColor tc : TerminalColor.values()) {
            Color awtColor = TERM_COLORS.get(tc);
            JButton btn = new JButton();
            btn.setBackground(awtColor);
            btn.setToolTipText(tc.name());
            btn.setBorder(BorderFactory.createLineBorder(new Color(0x555577), 1));
            btn.setPreferredSize(new Dimension(22, 22));
            btn.addActionListener(e -> {
                if (isFg) { selectedFg = tc; } else { selectedBg = tc; }
                applyAttrs();
                updateColorSwatches();
                popup.setVisible(false);
            });
            grid.add(btn);
        }
        popup.add(grid);
        JButton src = isFg ? colorSwatchFg : colorSwatchBg;
        popup.show(src, 0, src.getHeight());
    }

    private void updateColorSwatches() {
        Color fg = selectedFg != null ? TERM_COLORS.get(selectedFg) : DEFAULT_FG;
        Color bg = selectedBg != null ? TERM_COLORS.get(selectedBg) : DEFAULT_BG;
        colorSwatchFg.setText(selectedFg != null ? selectedFg.name().toLowerCase() : "default");
        colorSwatchBg.setText(selectedBg != null ? selectedBg.name().toLowerCase() : "default");
        styleColorSwatch(colorSwatchFg, fg, bg);
        styleColorSwatch(colorSwatchBg, bg, fg);
    }

    private void refreshAll() {
        canvas.repaint();
        cursorLabel.setText(String.format("Cursor: (%d, %d)", buffer.getCursorCol(), buffer.getCursorRow()));
        statusLabel.setText(String.format(
                "<html>Width: %d<br>Height: %d<br>Scrollback: %d / %d</html>",
                buffer.getWidth(), buffer.getHeight(),
                buffer.getScrollbackSize(), buffer.maxScrollback
        ));
        scrollbackCountLabel.setText(buffer.getScrollbackSize() + " lines");

        // Rebuild scrollback text (show latest lines, oldest at top)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buffer.getScrollbackSize(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(buffer.getLineAbsolute(i));
        }
        scrollbackArea.setText(sb.toString());
        // Scroll to bottom of scrollback view
        scrollbackArea.setCaretPosition(Math.max(0, scrollbackArea.getText().length() - 1));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Demo content
    // ─────────────────────────────────────────────────────────────────────────

    private void seedDemoContent() {
        // Header line
        buffer.setAttributes(TerminalColor.BRIGHT_CYAN, null, new TextStyle(true, false, false));
        buffer.setCursorPos(0, 0);
        buffer.writeText(" Terminal Buffer Explorer — interactive demo ");
        buffer.resetAttributes();

        // Separator
        buffer.setForeground(TerminalColor.BRIGHT_BLACK);
        buffer.setCursorPos(1, 0);
        for (int i = 0; i < buffer.getWidth(); i++) buffer.writeText("─");
        buffer.resetAttributes();

        // Coloured rows
        TerminalColor[] colors = { TerminalColor.RED, TerminalColor.GREEN, TerminalColor.YELLOW,
                TerminalColor.BLUE, TerminalColor.MAGENTA, TerminalColor.CYAN };
        String[] labels = { "  Write mode overwrites cells at the cursor position",
                "  Insert mode shifts content to the right",
                "  Arrow buttons move the cursor around the screen",
                "  Colour swatches set fg/bg for the next write",
                "  B/I/U toggles apply bold, italic, underline styles",
                "  'Insert empty line' pushes content into scrollback" };
        for (int i = 0; i < colors.length; i++) {
            buffer.setForeground(colors[i]);
            buffer.setCursorPos(3+i, 0);
            buffer.writeText(labels[i]);
        }
        buffer.resetAttributes();

        buffer.setCursorPos(10, 0);
        buffer.setForeground(TerminalColor.BRIGHT_WHITE);
        buffer.writeText("  Type in the input box below and press Enter to write.");
        buffer.resetAttributes();

        // Scrollback: push a few "history" lines
        for (int i = 0; i < 3; i++) {
            buffer.setCursorPos(0, 0);
            buffer.setForeground(TerminalColor.BRIGHT_BLACK);
            buffer.writeText("(scrollback line " + (i + 1) + ")");
            buffer.resetAttributes();
            buffer.insertBottomLine();
        }

        buffer.setCursorPos(12, 0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Widget / style helpers
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel styledCard(String title) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(0x16213e));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(0x3a3a5e)),
                        title,
                        TitledBorder.LEFT, TitledBorder.TOP,
                        new Font("SansSerif", Font.BOLD, 11),
                        new Color(0x7ec8e3)
                ),
                new EmptyBorder(4, 6, 6, 6)
        ));
        return card;
    }

    private JButton actionButton(String label, ActionListener al) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btn.setBackground(new Color(0x1a1a3e));
        btn.setForeground(new Color(0xcccccc));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x3a3a5e)),
                new EmptyBorder(3, 6, 3, 6)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(al);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(0x2a2a5e)); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(new Color(0x1a1a3e)); }
        });
        return btn;
    }

    private void styleActionButton(JButton btn) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setBackground(new Color(0x224466));
        btn.setForeground(new Color(0x7ec8e3));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x3a6a8e)),
                new EmptyBorder(3, 8, 3, 8)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleToggleButton(JToggleButton btn, Color off, Color on) {
        btn.setFont(btn.getFont());
        btn.setBackground(off);
        btn.setForeground(new Color(0xaaaaaa));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x3a3a5e)),
                new EmptyBorder(3, 7, 3, 7)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addChangeListener(e -> {
            btn.setBackground(btn.isSelected() ? on : off);
            btn.setForeground(btn.isSelected() ? Color.WHITE : new Color(0xaaaaaa));
        });
    }

    private void styleColorSwatch(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setBorder(BorderFactory.createLineBorder(new Color(0x555577), 1));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleSpinner(JSpinner sp) {
        sp.setPreferredSize(new Dimension(55, 24));
        sp.setFont(new Font("Monospaced", Font.PLAIN, 11));
        ((JSpinner.DefaultEditor) sp.getEditor()).getTextField().setBackground(new Color(0x0d0d1a));
        ((JSpinner.DefaultEditor) sp.getEditor()).getTextField().setForeground(DEFAULT_FG);
    }

    private JLabel dimLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(0x7ec8e3));
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        return l;
    }

    private JButton cursorMoveBtn(String label, Runnable action) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btn.setPreferredSize(new Dimension(28, 24));
        btn.setBackground(new Color(0x1a1a3e));
        btn.setForeground(new Color(0xcccccc));
        btn.setBorder(BorderFactory.createLineBorder(new Color(0x3a3a5e)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TerminalCanvas — the cell-grid renderer
    // ─────────────────────────────────────────────────────────────────────────

    private class TerminalCanvas extends JPanel {

        private static final int CELL_W = 9;
        private static final int CELL_H = 16;
        private static final int H_PAD  = 4;
        private static final int V_PAD  = 4;

        private final Font normalFont;
        private final Font boldFont;
        private final Font italicFont;
        private final Font boldItalicFont;

        TerminalCanvas() {
            normalFont    = new Font("Monospaced", Font.PLAIN,  13);
            boldFont      = new Font("Monospaced", Font.BOLD,   13);
            italicFont    = new Font("Monospaced", Font.ITALIC, 13);
            boldItalicFont= new Font("Monospaced", Font.BOLD | Font.ITALIC, 13);
            setBackground(DEFAULT_BG);

            // Allow clicking to move cursor
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    int col = (e.getX() - H_PAD) / CELL_W;
                    int row = (e.getY() - V_PAD) / CELL_H;
                    buffer.setCursorPos(row, col);
                    refreshAll();
                    inputField.requestFocusInWindow();
                }
            });

            // Arrow keys when canvas has focus
            setFocusable(true);
            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_LEFT  -> buffer.moveCursorLeft();
                        case KeyEvent.VK_RIGHT -> buffer.moveCursorRight();
                        case KeyEvent.VK_UP    -> buffer.moveCursorUp();
                        case KeyEvent.VK_DOWN  -> buffer.moveCursorDown();
                    }
                    refreshAll();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(
                    buffer.getWidth()  * CELL_W + H_PAD * 2,
                    buffer.getHeight() * CELL_H + V_PAD * 2
            );
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int bufW = buffer.getWidth();
            int bufH = buffer.getHeight();

            // Fill background
            g.setColor(DEFAULT_BG);
            g.fillRect(0, 0, getWidth(), getHeight());

            // Subtle grid lines
            g.setColor(GRID_LINE_COLOR);
            for (int col = 0; col <= bufW; col++)
                g.drawLine(H_PAD + col * CELL_W, V_PAD, H_PAD + col * CELL_W, V_PAD + bufH * CELL_H);
            for (int row = 0; row <= bufH; row++)
                g.drawLine(H_PAD, V_PAD + row * CELL_H, H_PAD + bufW * CELL_W, V_PAD + row * CELL_H);

            // Render cells
            for (int row = 0; row < bufH; row++) {
                for (int col = 0; col < bufW; col++) {
                    Cell cell = buffer.getScreenCell(row, col);
                    if (cell.width == 0) continue; // continuation — skip, drawn by leader

                    CellAttributes attrs = cell.attrs;
                    TextStyle style = attrs.style;

                    Color fg = attrs.foreground != null ? TERM_COLORS.get(attrs.foreground) : DEFAULT_FG;
                    Color bg = attrs.background != null ? TERM_COLORS.get(attrs.background) : DEFAULT_BG;

                    if (style.inverse) { Color tmp = fg; fg = bg; bg = tmp; }

                    int x = H_PAD + col * CELL_W;
                    int y = V_PAD + row * CELL_H;
                    int cellPixW = CELL_W * cell.width; // wide chars span 2 columns

                    // Background
                    if (!bg.equals(DEFAULT_BG)) {
                        g.setColor(bg);
                        g.fillRect(x, y, cellPixW, CELL_H);
                    }

                    // Character
                    if (cell.ch != null) {
                        Font f;
                        if (style.bold && style.italic) f = boldItalicFont;
                        else if (style.bold)            f = boldFont;
                        else if (style.italic)          f = italicFont;
                        else                            f = normalFont;
                        g.setFont(f);

                        // Dim (BRIGHT_BLACK fg treated as dim)
                        g.setColor(fg);
                        g.drawString(String.valueOf(cell.ch), x + 1, y + CELL_H - 4);

                        // Underline
                        if (style.underline) {
                            g.setColor(fg);
                            g.drawLine(x, y + CELL_H - 2, x + cellPixW - 1, y + CELL_H - 2);
                        }
                        // Strikethrough
                        if (style.strikethrough) {
                            g.setColor(fg);
                            g.drawLine(x, y + CELL_H / 2, x + cellPixW - 1, y + CELL_H / 2);
                        }
                    }
                }
            }

            // Cursor
            if (cursorVisible) {
                int cx = H_PAD + buffer.getCursorCol() * CELL_W;
                int cy = V_PAD + buffer.getCursorRow() * CELL_H;
                g.setColor(new Color(0xe0e0e0, true));
                // Filled block cursor with XOR so it's visible on any background
                g.setXORMode(DEFAULT_BG);
                g.fillRect(cx, cy, CELL_W, CELL_H);
                g.setPaintMode();
                // Bright outline
                g.setColor(new Color(0xffffff));
                g.drawRect(cx, cy, CELL_W - 1, CELL_H - 1);
            }

            // Column/row ruler along the edges (lightweight)
            g.setFont(new Font("Monospaced", Font.PLAIN, 8));
            g.setColor(new Color(0x3a3a5e));
            for (int col = 0; col < bufW; col += 10) {
                g.drawString(String.valueOf(col), H_PAD + col * CELL_W + 1, V_PAD - 1);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Entry point
    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        // Use system look & feel as a base, then we override everything
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        // Global dark defaults
        UIManager.put("Panel.background",      new Color(0x16213e));
        UIManager.put("ScrollPane.background", new Color(0x0d0d1a));
        UIManager.put("SplitPane.background",  new Color(0x0d0d1a));
        UIManager.put("Spinner.background",    new Color(0x0d0d1a));
        UIManager.put("TextField.background",  new Color(0x0d0d1a));
        UIManager.put("TextField.foreground",  new Color(0xe0e0e0));
        UIManager.put("ScrollBar.background",  new Color(0x16213e));
        UIManager.put("ScrollBar.thumb",       new Color(0x2a2a5e));

        SwingUtilities.invokeLater(TerminalBufferApp::new);
    }
}