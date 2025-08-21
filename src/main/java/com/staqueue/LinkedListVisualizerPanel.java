package com.staqueue;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Linked List visualizer (Singly + Doubly).
 * Matches the BFS/DFS UI pattern:
 * - Dry Run (pseudocode) panel at right (line-by-line highlight)
 * - Bottom status bar only (no pseudocode there)
 * - Step/Reset controls and timer-driven stepper
 * - Search runs as a fine-grained phased animation
 * - Insert/Remove are tied to specific pseudocode lines via an actions map
 */
public class LinkedListVisualizerPanel extends JPanel {

    // ---------- Model ----------
    private static class Node {
        int val;
        Node next;
        Node prev; // used when doubly = true
        int x, y;

        Node(int v) {
            this.val = v;
        }
    }

    private Node head;
    private boolean doubly = false;

    // ---------- UI ----------
    private final JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));

    // Create
    private final JLabel lblCreate = new JLabel("Create:");
    private final JComboBox<String> createType = new JComboBox<>(new String[] { "User", "Random", "Empty" });
    private final JTextField createInput = new JTextField(16); // "1,4,9"
    private final JButton btnCreate = new JButton("Apply");

    // Search
    private final JLabel lblSearch = new JLabel("Search:");
    private final JTextField searchInput = new JTextField(5);
    private final JButton btnSearch = new JButton("Find");

    // Insert
    private final JLabel lblInsert = new JLabel("Insert:");
    private final JLabel lblIdx = new JLabel("Index");
    private final JTextField insertIndex = new JTextField(3);
    private final JLabel lblVal = new JLabel("Value");
    private final JTextField insertValue = new JTextField(5);
    private final JButton btnInsert = new JButton("Insert");

    // Remove
    private final JLabel lblRemove = new JLabel("Remove:");
    private final JTextField removeValue = new JTextField(5);
    private final JButton btnRemove = new JButton("Remove");

    // Toggles / speed
    private final JCheckBox chkDoubly = new JCheckBox("Doubly");
    private final JLabel lblSpeed = new JLabel("  Speed:");
    private final JSlider speed = new JSlider(100, 1200, 500);
    private final JButton btnStep = new JButton("Step");
    private final JButton btnReset = new JButton("Reset");

    // Status (bottom) + Dry run (right)
    private final JLabel statusLabel = new JLabel("Ready.");
    private final DefaultListModel<String> pseudoModel = new DefaultListModel<>();
    private final JList<String> pseudoList = new JList<>(pseudoModel);

    // Canvas + scroll
    private final DrawPanel canvas = new DrawPanel();
    private JScrollPane canvasScroll;

    // ---------- Animation state ----------
    private javax.swing.Timer timer;
    private int animDelay = 500;
    private int pc = -1; // program counter for pseudocode lines
    private Map<Integer, Runnable> actions = new HashMap<>();
    private Integer cursorIndex = null; // for highlighting during search

    // Search stepper state (so we traverse all nodes with animation)
    private Integer searchTarget = null;
    private int searchIndex = 0; // current index during search
    private int searchPhase = 0; // 0=while-check, 1=compare, 2=increment

    // ---------- Layout constants ----------
    private static final int W = 1000, H = 520;
    private static final int NODE_W = 54, NODE_H = 34;
    private static final int GAP_X = 50;
    private static final int START_X = 50, START_Y = 120;

    public LinkedListVisualizerPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(28, 28, 30));

        // Top bar (2x3 grid like BFS/DFS)
        topBar.setBackground(new Color(40, 40, 44));
        topBar.setBorder(new EmptyBorder(6, 8, 6, 8));
        for (JLabel l : new JLabel[] { lblCreate, lblSearch, lblInsert, lblRemove, lblSpeed }) {
            l.setForeground(Color.WHITE);
            l.setOpaque(false);
        }
        chkDoubly.setForeground(Color.WHITE);
        chkDoubly.setOpaque(false);
        statusLabel.setForeground(Color.WHITE);

        // Emphasize idx/val labels
        Color warn = new Color(220, 60, 60);
        lblIdx.setForeground(warn);
        lblVal.setForeground(warn);

        // Build top bar grid
        topBar.setLayout(new GridLayout(2, 3, 12, 6));
        JPanel pnlCreate = row(lblCreate, createType, createInput, btnCreate);
        JPanel pnlInsert = row(lblInsert, lblIdx, insertIndex, lblVal, insertValue, btnInsert);
        JPanel pnlSearch = row(lblSearch, searchInput, btnSearch);
        JPanel pnlRemove = row(lblRemove, removeValue, btnRemove);
        JPanel pnlMode = row(chkDoubly);
        JPanel pnlCtrl = row(btnStep, btnReset, lblSpeed, speed);

        topBar.add(pnlCreate);
        topBar.add(pnlInsert);
        topBar.add(pnlCtrl);
        topBar.add(pnlSearch);
        topBar.add(pnlRemove);
        topBar.add(pnlMode);
        add(topBar, BorderLayout.NORTH);

        // Right: Dry Run (pseudocode) panel
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(new Color(40, 40, 44));
        right.setBorder(new EmptyBorder(8, 8, 8, 8));
        JLabel title = new JLabel("Dry Run (Linked List)");
        title.setForeground(Color.WHITE);
        title.setBorder(new EmptyBorder(0, 0, 6, 0));
        pseudoList.setBackground(new Color(32, 32, 36));
        pseudoList.setForeground(Color.WHITE);
        pseudoList.setSelectionBackground(new Color(75, 110, 175));
        JScrollPane pseudoScroll = new JScrollPane(pseudoList);
        pseudoScroll.setPreferredSize(new Dimension(360, 220)); // slim like BFS/DFS
        right.add(title, BorderLayout.NORTH);
        right.add(pseudoScroll, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        // Bottom: status only
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(new Color(40, 40, 44));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        left.setBackground(new Color(40, 40, 44));
        left.add(statusLabel);
        bottom.add(left, BorderLayout.WEST);
        add(bottom, BorderLayout.SOUTH);

        // Center: canvas (scrollable)
        canvas.setPreferredSize(new Dimension(W, H));
        canvasScroll = new JScrollPane(canvas);
        canvasScroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        canvasScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        canvasScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        canvasScroll.getHorizontalScrollBar().setUnitIncrement(24);
        add(canvasScroll, BorderLayout.CENTER);

        // Wire actions
        btnCreate.addActionListener(e -> onCreate());
        btnSearch.addActionListener(e -> onSearch());
        btnInsert.addActionListener(e -> onInsert());
        btnRemove.addActionListener(e -> onRemove());

        chkDoubly.addActionListener(e -> {
            doubly = chkDoubly.isSelected();
            rebuildBackLinks();
            layoutList();
            canvas.repaint();
        });

        btnStep.addActionListener(e -> stepOnce());
        btnReset.addActionListener(e -> resetAnim());

        speed.addChangeListener(e -> {
            animDelay = speed.getValue();
            if (timer != null)
                timer.setDelay(animDelay);
        });

        // Initialize
        layoutList();
        resetAnim();
        updateStatus();
    }

    private JPanel row(Component... cs) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setOpaque(false);
        for (Component c : cs)
            p.add(c);
        return p;
    }

    // ---------- Reset animation / pseudocode ----------
    private void resetAnim() {
        if (timer != null && timer.isRunning())
            timer.stop();
        pc = -1;
        pseudoModel.clear();
        actions.clear();
        cursorIndex = null;
        searchTarget = null;
        searchIndex = 0;
        searchPhase = 0;
        pseudoList.clearSelection();
        status("Ready.");
    }

    // ---------- Operations ----------
    private void onCreate() {
        resetAnim();
        String type = (String) createType.getSelectedItem();
        if ("Empty".equals(type)) {
            head = null;
        } else if ("User".equals(type)) {
            List<Integer> vals = parseCSV(createInput.getText());
            buildFrom(vals);
        } else {
            Random rnd = new Random();
            int n = 5 + rnd.nextInt(4); // 5..8
            List<Integer> vals = new ArrayList<>();
            for (int i = 0; i < n; i++)
                vals.add(1 + rnd.nextInt(99));
            buildFrom(vals);
        }
        rebuildBackLinks();
        layoutList();
        updateStatus();
        canvas.repaint();
        loadPseudoCreate(type);
    }

    private void onSearch() {
        resetAnim();
        Integer target = parseInt(searchInput.getText());
        if (target == null)
            return;

        loadPseudoSearch();

        // setup phased search animation
        searchTarget = target;
        searchIndex = 0;
        searchPhase = 0;

        if (timer != null)
            timer.stop();
        timer = new javax.swing.Timer(animDelay, e -> doSearchTick());
        timer.start();
    }

    private void doSearchTick() {
        int n = listSize();
        switch (searchPhase) {
            case 0: // while cur != null
                selectPseudo(2);
                if (searchIndex >= n) {
                    selectPseudo(5);
                    status("Element Not Found.");
                    appendPseudo("// Element Not Found");
                    timerStop();
                    return;
                }
                searchPhase = 1;
                break;

            case 1: // compare at current index
                selectPseudo(3);
                cursorIndex = searchIndex;
                ensureVisible(getNode(cursorIndex));
                canvas.repaint();

                Node nd = getNode(cursorIndex);
                if (nd != null && nd.val == searchTarget) {
                    status("Found at index " + cursorIndex);
                    appendPseudo("// Found at index " + cursorIndex);
                    timerStop();
                    return;
                }
                searchPhase = 2;
                break;

            case 2: // i++, move next
                selectPseudo(4);
                searchIndex++;
                searchPhase = 0;
                break;
        }
    }

    private void onInsert() {
        resetAnim();
        Integer idx = parseInt(insertIndex.getText());
        Integer val = parseInt(insertValue.getText());
        if (idx == null || val == null)
            return;
        if (idx < 0 || idx > listSize()) {
            status("Index out of bounds.");
            return;
        }

        loadPseudoInsert();
        actions = new HashMap<>();
        // Perform the actual insert when line 4 of pseudo is reached
        actions.put(4, () -> doInsert(idx, val));
        runAuto();
    }

    private void onRemove() {
        resetAnim();
        Integer val = parseInt(removeValue.getText());
        if (val == null)
            return;

        loadPseudoRemove();
        actions = new HashMap<>();
        // Do the remove at line 2 (after loop condition line)
        actions.put(2, () -> {
            boolean ok = doRemove(val);
            if (!ok) {
                status("Element Not Found.");
                appendPseudo("// Element Not Found");
            }
        });
        runAuto();
    }

    private void doInsert(int idx, int val) {
        if (idx == 0) {
            Node n = new Node(val);
            n.next = head;
            head = n;
        } else {
            Node prev = getNode(idx - 1);
            Node n = new Node(val);
            n.next = (prev != null) ? prev.next : null;
            if (prev != null)
                prev.next = n;
        }
        rebuildBackLinks();
        layoutList();
        updateStatus();
        canvas.repaint();
    }

    private boolean doRemove(int val) {
        Node dummy = new Node(0);
        dummy.next = head;
        Node prev = dummy, cur = head;
        boolean removed = false;
        while (cur != null) {
            if (cur.val == val) {
                prev.next = cur.next;
                removed = true;
                break;
            }
            prev = cur;
            cur = cur.next;
        }
        head = dummy.next;
        rebuildBackLinks();
        layoutList();
        updateStatus();
        canvas.repaint();
        return removed;
    }

    // ---------- Helpers ----------
    private List<Integer> parseCSV(String s) {
        List<Integer> out = new ArrayList<>();
        if (s == null)
            return out;
        for (String part : s.split(",")) {
            String t = part.trim();
            if (t.isEmpty())
                continue;
            try {
                out.add(Integer.parseInt(t));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    private Integer parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private void buildFrom(List<Integer> vals) {
        head = null;
        Node tail = null;
        for (int v : vals) {
            Node n = new Node(v);
            if (head == null) {
                head = tail = n;
            } else {
                tail.next = n;
                tail = n;
            }
        }
    }

    private int listSize() {
        int c = 0;
        Node t = head;
        while (t != null) {
            c++;
            t = t.next;
        }
        return c;
    }

    private Node getNode(int index) {
        int i = 0;
        Node t = head;
        while (t != null && i < index) {
            t = t.next;
            i++;
        }
        return t;
    }

    private void rebuildBackLinks() {
        if (!doubly) {
            // clear prev pointers
            Node t = head;
            while (t != null) {
                t.prev = null;
                t = t.next;
            }
            return;
        }
        Node prev = null, cur = head;
        while (cur != null) {
            cur.prev = prev;
            prev = cur;
            cur = cur.next;
        }
    }

    private void updateStatus() {
        status("Size: " + listSize());
    }

    private void status(String s) {
        statusLabel.setText(s);
    }

    // ---------- Pseudocode helpers ----------
    private void selectPseudo(int idx) {
        if (idx >= 0 && idx < pseudoModel.size()) {
            pseudoList.setSelectedIndex(idx);
            pc = idx;
        }
    }

    private void appendPseudo(String line) {
        pseudoModel.addElement(line);
        pseudoList.setSelectedIndex(pseudoModel.size() - 1);
    }

    private void setPseudo(String title, String... lines) {
        pseudoModel.clear();
        pseudoModel.addElement("// " + title);
        for (String ln : lines)
            pseudoModel.addElement(ln);
        pseudoList.setSelectedIndex(0);
        pc = 0;
    }

    private void loadPseudoCreate(String type) {
        setPseudo("Create (" + type + ")",
                "i = 0; // reset state",
                "if type == Empty: head = null",
                "else if type == User: parse CSV and build",
                "else: build random list of length 5..8");
    }

    private void loadPseudoSearch() {
        setPseudo("Search(x)",
                "i = 0; cur = head",
                "while cur != null:",
                "    if cur.val == x: return i",
                "    i++, cur = cur.next",
                "return -1");
    }

    private void loadPseudoInsert() {
        setPseudo("Insert(idx, x)",
                "if idx == 0: head = new Node(x, next=head)",
                "else: prev = nodeAt(idx-1)",
                "n = new Node(x); n.next = prev.next; prev.next = n",
                "rebuild back-links if DLL; relayout");
    }

    private void loadPseudoRemove() {
        setPseudo("Remove(x)",
                "prev = dummy -> head; cur = head",
                "while cur != null and cur.val != x: prev = cur; cur = cur.next",
                "if cur != null: prev.next = cur.next",
                "rebuild back-links if DLL; relayout");
    }

    private void runAuto() {
        if (timer != null)
            timer.stop();
        timer = new javax.swing.Timer(animDelay, e -> {
            pc++;
            if (pc >= pseudoModel.size()) {
                timerStop();
                return;
            }
            pseudoList.setSelectedIndex(pc);
            Runnable r = actions.get(pc);
            if (r != null)
                r.run();
            canvas.repaint();
        });
        timer.start();
    }

    private void stepOnce() {
        if (timer != null && timer.isRunning())
            timer.stop();
        if (pc + 1 >= pseudoModel.size())
            return;
        pc++;
        pseudoList.setSelectedIndex(pc);
        Runnable r = actions.get(pc);
        if (r != null)
            r.run();
        canvas.repaint();
    }

    private void timerStop() {
        if (timer != null)
            timer.stop();
    }

    // ---------- Layout + Drawing ----------
    private void layoutList() {
        int x = START_X, y = START_Y;
        Node t = head;
        while (t != null) {
            t.x = x;
            t.y = y;
            x += NODE_W + GAP_X;
            t = t.next;
        }
        // widen canvas with node count to allow horizontal scroll
        int nodes = listSize();
        int width = Math.max(W, START_X + nodes * (NODE_W + GAP_X) + 120);
        canvas.setPreferredSize(new Dimension(width, H));
        canvas.revalidate();
    }

    private void ensureVisible(Node n) {
        if (n == null)
            return;
        Rectangle r = new Rectangle(n.x - 30, n.y - 30, NODE_W + 60, NODE_H + 60);
        canvas.scrollRectToVisible(r);
    }

    private class DrawPanel extends JPanel {
        DrawPanel() {
            setBackground(new Color(24, 24, 26));
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw links first
            drawArrows(g2);

            // Draw nodes
            Node t = head;
            int idx = 0;
            while (t != null) {
                boolean highlight = (cursorIndex != null && cursorIndex == idx);
                drawNode(g2, t, highlight);
                t = t.next;
                idx++;
            }

            // HEAD label
            g2.setColor(Color.WHITE);
            g2.drawString("head", START_X - 35, START_Y + NODE_H / 2 - 8);
            g2.drawLine(START_X - 10, START_Y + NODE_H / 2 - 12, START_X - 2, START_Y + NODE_H / 2 - 12);

            g2.dispose();
        }

        private void drawNode(Graphics2D g2, Node n, boolean highlight) {
            int x = n.x, y = n.y;
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(highlight ? new Color(200, 80, 60) : new Color(70, 80, 200));
            g2.fillRoundRect(x, y, NODE_W, NODE_H, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawRoundRect(x, y, NODE_W, NODE_H, 10, 10);

            String s = String.valueOf(n.val);
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (NODE_W - fm.stringWidth(s)) / 2;
            int ty = y + (NODE_H + fm.getAscent()) / 2 - 4;
            g2.drawString(s, tx, ty);
        }

        private void drawArrows(Graphics2D g2) {
            g2.setColor(new Color(160, 160, 165));
            g2.setStroke(new BasicStroke(2f));
            Node a = head;
            while (a != null && a.next != null) {
                Node b = a.next;
                drawArrow(g2, a.x + NODE_W, a.y + NODE_H / 2, b.x, b.y + NODE_H / 2, 10);
                if (doubly) { // back link for DLL
                    drawArrow(g2, b.x, b.y + NODE_H / 2 + 8, a.x + NODE_W, a.y + NODE_H / 2 + 8, 8);
                }
                a = a.next;
            }
        }

        private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2, int size) {
            g2.drawLine(x1, y1, x2, y2);
            double dx = x2 - x1, dy = y2 - y1;
            double angle = Math.atan2(dy, dx);
            int hx1 = (int) (x2 - size * Math.cos(angle - Math.toRadians(25)));
            int hy1 = (int) (y2 - size * Math.sin(angle - Math.toRadians(25)));
            int hx2 = (int) (x2 - size * Math.cos(angle + Math.toRadians(25)));
            int hy2 = (int) (y2 - size * Math.sin(angle + Math.toRadians(25)));
            g2.drawLine(x2, y2, hx1, hy1);
            g2.drawLine(x2, y2, hx2, hy2);
        }
    }
}
