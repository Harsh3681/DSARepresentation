package com.staqueue;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Depth-First Search visualizer (original UI preserved).
 * Adds a naive-friendly "Dry Run (pseudocode)" panel on the right that
 * highlights the current step as the animation runs.
 */
public class DFSVisualizerPanel extends JPanel {

    // --- Model ---
    private static class Node {
        final int id;
        int x, y;

        Node(int id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    private static class Edge {
        int u, v;
        boolean directed;

        Edge(int u, int v) {
            this(u, v, false);
        }

        Edge(int u, int v, boolean directed) {
            this.u = u;
            this.v = v;
            this.directed = directed;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Edge))
                return false;
            Edge e = (Edge) o;
            return e.u == u && e.v == v && e.directed == directed;
        }

        @Override
        public int hashCode() {
            return Objects.hash(u, v, directed);
        }
    }

    private final java.util.List<Node> nodes = new ArrayList<>();
    private final java.util.Set<Edge> edges = new LinkedHashSet<>(); // stable iteration
    private final Map<Integer, java.util.List<Integer>> adj = new HashMap<>();

    // DFS state
    private int startNode = -1;
    private boolean[] visited;
    private int[] parent;
    private java.util.List<Integer> order = new ArrayList<>();
    private ArrayDeque<Integer> stack = new ArrayDeque<>(); // use as LIFO

    // Animation
    private javax.swing.Timer timer;
    private int animDelay = 500; // ms per step

    // --- UI (original controls retained) ---
    private final JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    private final JButton btnNew = new JButton("New Graph");
    private final JButton btnRandom = new JButton("Random");
    private final JButton btnAddNode = new JButton("Add Node");
    private final JButton btnAddEdge = new JButton("Add Edge");
    private final JButton btnMove = new JButton("Move");
    private final JButton btnDelete = new JButton("Delete");
    private final JButton btnClear = new JButton("Clear");
    private final JComboBox<Integer> startSelector = new JComboBox<>();
    private final JCheckBox chkDirected = new JCheckBox("Directed");
    private final JButton btnRun = new JButton("Run DFS");
    private final JButton btnStep = new JButton("Step");
    private final JButton btnReset = new JButton("Reset");
    private final JSlider speed = new JSlider(100, 1200, animDelay);

    private final JLabel lblStart = new JLabel("  Start:");
    private final JLabel lblSpeed = new JLabel("  Speed:");
    private final JLabel queueLabel = new JLabel("Stack: []");
    private final JLabel orderLabel = new JLabel("Order: []");
    private final DrawPanel canvas = new DrawPanel();

    // >>> Added: larger font for bottom output labels <<<
    private static final Font STATUS_FONT = new Font(Font.MONOSPACED, Font.BOLD, 16);

    private enum Mode {
        ADD_NODE, ADD_EDGE, MOVE, DELETE
    }

    private Mode mode = Mode.ADD_NODE;
    private int hoverNode = -1;
    private int draggingNode = -1;
    private int pendingEdgeStart = -1;

    private boolean directedMode = false;
    private Edge lastMove = null;

    // Geometry
    private static final int R = 20;
    private static final int W = 900, H = 540;

    // ---------- Dry Run additions ----------
    private final DefaultListModel<String> pseudoModel = new DefaultListModel<>();
    private final JList<String> pseudoList = new JList<>(pseudoModel);

    private enum Phase {
        INIT, WHILE_CHECK, POP, PREP_LOOP, CHECK_NBR, PUSH_NBR, DONE
    }

    private Phase phase = Phase.INIT;
    private int current = -1;
    private java.util.List<Integer> nbrs = Collections.emptyList();
    private int nbrIdx = -1;

    public DFSVisualizerPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(28, 28, 30));

        // Top bar controls (unchanged)
        topBar.setBackground(new Color(40, 40, 44));
        topBar.setBorder(new EmptyBorder(6, 8, 6, 8));

        btnNew.addActionListener(e -> {
            clear();
            repaint();
        });
        btnRandom.addActionListener(e -> {
            randomize();
            repaint();
        });
        btnAddNode.addActionListener(e -> mode = Mode.ADD_NODE);
        btnAddEdge.addActionListener(e -> {
            mode = Mode.ADD_EDGE;
            pendingEdgeStart = -1;
        });
        btnMove.addActionListener(e -> mode = Mode.MOVE);
        btnDelete.addActionListener(e -> mode = Mode.DELETE);
        btnClear.addActionListener(e -> {
            clear();
            repaint();
        });

        btnRun.addActionListener(e -> runDFS());
        btnStep.addActionListener(e -> stepDFS());
        btnReset.addActionListener(e -> {
            resetDFSState();
            repaint();
        });

        speed.setToolTipText("Animation speed (ms/step)");
        speed.addChangeListener(e -> {
            animDelay = speed.getValue();
            if (timer != null)
                timer.setDelay(animDelay);
        });

        // Style labels/checkbox for dark theme
        lblStart.setForeground(Color.WHITE);
        lblStart.setOpaque(false);
        chkDirected.setOpaque(false);
        chkDirected.setForeground(Color.WHITE);
        chkDirected.addActionListener(e -> toggleDirected(chkDirected.isSelected()));
        lblSpeed.setForeground(Color.WHITE);
        lblSpeed.setOpaque(false);

        // Build top bar (unchanged)
        topBar.add(btnNew);
        topBar.add(btnRandom);
        topBar.add(new JSeparator(SwingConstants.VERTICAL));
        topBar.add(btnAddNode);
        topBar.add(btnAddEdge);
        topBar.add(btnMove);
        topBar.add(btnDelete);
        topBar.add(btnClear);
        topBar.add(lblStart);
        startSelector.setPreferredSize(new Dimension(70, startSelector.getPreferredSize().height));
        topBar.add(startSelector);
        topBar.add(chkDirected);
        topBar.add(btnRun);
        topBar.add(btnStep);
        topBar.add(btnReset);
        topBar.add(lblSpeed);
        topBar.add(speed);

        add(topBar, BorderLayout.NORTH);

        // Bottom status bar (unchanged layout, larger font for labels)
        JPanel status = new JPanel(new GridLayout(1, 2));
        status.setBackground(new Color(40, 40, 44));
        queueLabel.setForeground(Color.WHITE);
        orderLabel.setForeground(Color.WHITE);
        // >>> Apply larger font here <<<
        queueLabel.setFont(STATUS_FONT);
        orderLabel.setFont(STATUS_FONT);
        status.add(pad(queueLabel, 8));
        status.add(pad(orderLabel, 8));
        add(status, BorderLayout.SOUTH);

        // Canvas
        canvas.setPreferredSize(new Dimension(W, H));
        add(canvas, BorderLayout.CENTER);

        // Dry Run panel (new)
        buildDryRunPanel();
        loadPseudo();
        resetDFSState();
    }

    private void buildDryRunPanel() {
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(new Color(40, 40, 44));
        right.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel title = new JLabel("Dry Run (DFS)");
        title.setForeground(Color.WHITE);
        title.setBorder(new EmptyBorder(0, 0, 6, 0));

        pseudoList.setBackground(new Color(32, 32, 36));
        pseudoList.setForeground(Color.WHITE);
        pseudoList.setSelectionBackground(new Color(75, 110, 175));
        JScrollPane sp = new JScrollPane(pseudoList);
        sp.setPreferredSize(new Dimension(360, 220));

        right.add(title, BorderLayout.NORTH);
        right.add(sp, BorderLayout.CENTER);

        add(right, BorderLayout.EAST);
    }

    private void loadPseudo() {
        pseudoModel.clear();
        addPseudo("// Depth-First Search (DFS) — stack version");
        addPseudo("1) Put START on the stack and mark it visited.");
        addPseudo("2) While the stack is not empty:");
        addPseudo("   a) Pop the top node (call it u).");
        addPseudo("   b) For each neighbor v of u (any order):");
        addPseudo("      - If v is new, mark visited and push v.");
        addPseudo("3) When the stack is empty, we are done.");
        pseudoList.setSelectedIndex(0);
    }

    private void addPseudo(String s) {
        pseudoModel.addElement(s);
    }

    private void sel(int idx) {
        if (idx >= 0 && idx < pseudoModel.size())
            pseudoList.setSelectedIndex(idx);
    }

    private JComponent pad(JComponent c, int p) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(new Color(40, 40, 44));
        wrap.setBorder(new EmptyBorder(p, p, p, p));
        wrap.add(c, BorderLayout.CENTER);
        return wrap;
    }

    // Toggle between undirected and directed graph modes.
    private void toggleDirected(boolean on) {
        if (on == directedMode)
            return;
        directedMode = on;

        // Rebuild edges/adj according to new mode
        Map<Integer, java.util.List<Integer>> newAdj = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++)
            newAdj.put(i, new ArrayList<>());
        java.util.Set<Edge> newEdges = new LinkedHashSet<>();

        if (directedMode) {
            // Convert existing undirected edges into a single directed edge with random
            // orientation
            Random rnd = new Random();
            for (Edge e : edges) {
                int u = e.u, v = e.v;
                if (rnd.nextBoolean()) {
                    newEdges.add(new Edge(u, v, true));
                    newAdj.get(u).add(v);
                } else {
                    newEdges.add(new Edge(v, u, true));
                    newAdj.get(v).add(u);
                }
            }
        } else {
            // Merge directed edges into single undirected edges
            boolean[][] seen = new boolean[nodes.size()][nodes.size()];
            for (Edge e : edges) {
                int a = Math.min(e.u, e.v), b = Math.max(e.u, e.v);
                if (!seen[a][b]) {
                    seen[a][b] = true;
                    newEdges.add(new Edge(a, b, false));
                    newAdj.get(a).add(b);
                    newAdj.get(b).add(a);
                }
            }
        }
        edges.clear();
        edges.addAll(newEdges);
        adj.clear();
        adj.putAll(newAdj);
        lastMove = null;
        resetDFSState();
        repaint();
    }

    // --- Core operations (unchanged UI; stepper becomes fine-grained) ---
    private void clear() {
        nodes.clear();
        edges.clear();
        adj.clear();
        startSelector.removeAllItems();
        startNode = -1;
        pendingEdgeStart = -1;
        draggingNode = -1;
        hoverNode = -1;
        lastMove = null;
        resetDFSState();
        if (timer != null)
            timer.stop();
        updateStatusLabels();
        repaint();
        if (canvas != null)
            canvas.repaint();
    }

    private void randomize() {
        clear();
        lastMove = null;
        Random rnd = new Random();
        int n = 8 + rnd.nextInt(5); // 8..12 nodes
        for (int i = 0; i < n; i++) {
            addNode(80 + rnd.nextInt(W - 160), 80 + rnd.nextInt(H - 160));
        }
        // add some random edges
        int m = n + rnd.nextInt(n + 1);
        for (int i = 0; i < m; i++) {
            int u = rnd.nextInt(n);
            int v = rnd.nextInt(n);
            if (u != v) {
                if (directedMode) {
                    if (rnd.nextBoolean())
                        addEdge(u, v);
                    else
                        addEdge(v, u);
                } else {
                    addEdge(u, v);
                }
            }
        }
        if (startSelector.getItemCount() > 0)
            startSelector.setSelectedIndex(0);
    }

    private void addNode(int x, int y) {
        Node node = new Node(nodes.size(), x, y);
        nodes.add(node);
        startSelector.addItem(node.id);
        if (startSelector.getItemCount() == 1)
            startSelector.setSelectedItem(node.id);
        adj.put(node.id, new ArrayList<>());
        resetDFSState();
    }

    private void addEdge(int u, int v) {
        if (u == v)
            return;
        if (directedMode) {
            Edge e = new Edge(u, v, true);
            if (edges.add(e))
                adj.get(u).add(v);
        } else {
            int a = Math.min(u, v), b = Math.max(u, v);
            Edge e = new Edge(a, b, false);
            if (edges.add(e)) {
                adj.get(a).add(b);
                adj.get(b).add(a);
            }
        }
    }

    private void removeEdge(int u, int v) {
        if (directedMode) {
            Edge e = new Edge(u, v, true);
            if (edges.remove(e)) {
                adj.get(u).remove((Integer) v);
            }
        } else {
            int a = Math.min(u, v), b = Math.max(u, v);
            Edge e = new Edge(a, b, false);
            if (edges.remove(e)) {
                adj.get(a).remove((Integer) b);
                adj.get(b).remove((Integer) a);
            }
        }
    }

    private void removeNode(int id) {
        if (id < 0 || id >= nodes.size())
            return;

        // Remove all incident edges first
        java.util.List<Edge> incident = new ArrayList<>();
        for (Edge e : edges) {
            if (e.u == id || e.v == id)
                incident.add(e);
        }
        for (Edge e : incident)
            edges.remove(e);

        // Physically remove the node
        nodes.remove(id);
        adj.remove(id);

        // Rebuild edges and adjacency with reindexed node ids
        Map<Integer, java.util.List<Integer>> newAdj = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++)
            newAdj.put(i, new ArrayList<>());
        java.util.Set<Edge> newEdges = new LinkedHashSet<>();

        for (Edge e : new ArrayList<>(edges)) {
            int nu = e.u - (e.u > id ? 1 : 0);
            int nv = e.v - (e.v > id ? 1 : 0);
            if (directedMode) {
                Edge ne = new Edge(nu, nv, true);
                if (newEdges.add(ne))
                    newAdj.get(nu).add(nv);
            } else {
                int a = Math.min(nu, nv), b = Math.max(nu, nv);
                Edge ne = new Edge(a, b, false);
                if (newEdges.add(ne)) {
                    newAdj.get(a).add(b);
                    newAdj.get(b).add(a);
                }
            }
        }

        edges.clear();
        edges.addAll(newEdges);
        adj.clear();
        adj.putAll(newAdj);

        // Refill start selector
        startSelector.removeAllItems();
        for (int i = 0; i < nodes.size(); i++)
            startSelector.addItem(i);
        if (startSelector.getItemCount() > 0)
            startSelector.setSelectedIndex(0);

        // Reset transient state
        if (pendingEdgeStart == id)
            pendingEdgeStart = -1;
        draggingNode = -1;
        hoverNode = -1;
        lastMove = null;
        resetDFSState();
        repaint();
    }

    private void resetDFSState() {
        visited = new boolean[nodes.size()];
        parent = new int[nodes.size()];
        Arrays.fill(parent, -1);
        order.clear();
        stack.clear();
        // dry run state
        phase = Phase.INIT;
        current = -1;
        nbrs = Collections.emptyList();
        nbrIdx = -1;
        sel(0);
        if (timer != null && timer.isRunning())
            timer.stop();
        updateStatusLabels();
    }

    private void runDFS() {
        if (nodes.isEmpty())
            return;
        Integer sel = (Integer) startSelector.getSelectedItem();
        if (sel == null && startSelector.getItemCount() > 0) {
            startSelector.setSelectedIndex(0);
            sel = (Integer) startSelector.getSelectedItem();
        }
        if (sel == null)
            return;
        startNode = sel;
        resetDFSState();
        timer = new javax.swing.Timer(animDelay, e -> stepDFS());
        timer.start();
        repaint();
    }

    /**
     * Fine-grained stepper for DFS (iterative stack version).
     * Preserves the original semantics (mark visited on push).
     */
    private void stepDFS() {
        if (visited == null || visited.length != nodes.size())
            resetDFSState();

        switch (phase) {
            case INIT: {
                Integer sel = (Integer) startSelector.getSelectedItem();
                if (sel == null && startSelector.getItemCount() > 0) {
                    startSelector.setSelectedIndex(0);
                    sel = (Integer) startSelector.getSelectedItem();
                }
                if (sel == null)
                    return;
                startNode = sel;
                stack.clear();
                order.clear();
                Arrays.fill(visited, false);
                Arrays.fill(parent, -1);

                visited[startNode] = true;
                stack.addLast(startNode);
                sel(1);
                phase = Phase.WHILE_CHECK;
                break;
            }
            case WHILE_CHECK: {
                sel(2);
                if (stack.isEmpty())
                    phase = Phase.DONE;
                else
                    phase = Phase.POP;
                break;
            }
            case POP: {
                sel(3);
                current = stack.removeLast();
                order.add(current);
                nbrs = new ArrayList<>(adj.getOrDefault(current, Collections.emptyList()));
                // stable order for clarity
                Collections.sort(nbrs);
                nbrIdx = 0;
                phase = Phase.PREP_LOOP;
                break;
            }
            case PREP_LOOP: {
                sel(4);
                if (nbrIdx >= nbrs.size())
                    phase = Phase.WHILE_CHECK;
                else
                    phase = Phase.CHECK_NBR;
                break;
            }
            case CHECK_NBR: {
                sel(5);
                int v = nbrs.get(nbrIdx);
                if (!visited[v]) {
                    visited[v] = true;
                    parent[v] = current;
                    stack.addLast(v);
                    lastMove = new Edge(current, v, true);
                    phase = Phase.PUSH_NBR;
                } else {
                    nbrIdx++;
                    phase = Phase.PREP_LOOP;
                }
                break;
            }
            case PUSH_NBR: {
                // keep line 5 highlighted, then advance neighbor pointer
                nbrIdx++;
                phase = Phase.PREP_LOOP;
                break;
            }
            case DONE: {
                sel(6);
                if (timer != null)
                    timer.stop();
                break;
            }
        }

        updateStatusLabels();
        repaint();
    }

    private void updateStatusLabels() {
        java.util.List<Integer> list = new ArrayList<>(stack);
        queueLabel.setText("Stack: " + list.toString());
        orderLabel.setText("Order: " + order.toString());
    }

    // --- Drawing & Interaction (original) ---
    private class DrawPanel extends JPanel {
        DrawPanel() {
            setBackground(new Color(24, 24, 26));
            setOpaque(true);
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    hoverNode = findNodeAt(e.getX(), e.getY());
                    setCursor((mode == Mode.MOVE && hoverNode != -1) ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                            : Cursor.getDefaultCursor());
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (mode == Mode.ADD_NODE) {
                        addNode(e.getX(), e.getY());
                        repaint();
                    } else if (mode == Mode.MOVE) {
                        draggingNode = findNodeAt(e.getX(), e.getY());
                    } else if (mode == Mode.DELETE) {
                        int id = findNodeAt(e.getX(), e.getY());
                        if (id != -1) {
                            removeNode(id);
                            repaint();
                        }
                    } else if (mode == Mode.ADD_EDGE) {
                        int id = findNodeAt(e.getX(), e.getY());
                        if (id != -1) {
                            if (pendingEdgeStart == -1)
                                pendingEdgeStart = id;
                            else if (pendingEdgeStart != id) {
                                addEdge(pendingEdgeStart, id);
                                pendingEdgeStart = -1;
                                repaint();
                            }
                        }
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (mode == Mode.MOVE && draggingNode != -1) {
                        Node nd = nodes.get(draggingNode);
                        nd.x = e.getX();
                        nd.y = e.getY();
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    draggingNode = -1;
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // draw edges
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(115, 115, 120));
            for (Edge e : edges) {
                Node a = nodes.get(e.u);
                Node b = nodes.get(e.v);
                if (e.directed)
                    drawArrow(g2, a.x, a.y, b.x, b.y, 10);
                else
                    g2.drawLine(a.x, a.y, b.x, b.y);
            }
            // highlight last traversed edge
            if (lastMove != null) {
                g2.setColor(new Color(220, 80, 80));
                g2.setStroke(new BasicStroke(3f));
                Node a = nodes.get(lastMove.u);
                Node b = nodes.get(lastMove.v);
                drawArrow(g2, a.x, a.y, b.x, b.y, 12);
            }

            // temporary edge
            if (mode == Mode.ADD_EDGE && pendingEdgeStart != -1 && hoverNode != -1 && hoverNode != pendingEdgeStart) {
                Node a = nodes.get(pendingEdgeStart);
                Node b = nodes.get(hoverNode);
                Stroke ds = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f,
                        new float[] { 6f, 6f }, 0f);
                g2.setStroke(ds);
                g2.drawLine(a.x, a.y, b.x, b.y);
            }

            // draw nodes
            for (int i = 0; i < nodes.size(); i++) {
                Node nd = nodes.get(i);
                boolean inStack = stack.contains(i);
                boolean isVisited = visited != null && visited[i];
                boolean isTop = (!stack.isEmpty() && stack.peekLast() == i);
                Color fill = new Color(70, 80, 200);
                if (isVisited)
                    fill = new Color(60, 160, 80);
                if (inStack)
                    fill = new Color(200, 160, 60);
                if (isTop)
                    fill = new Color(200, 80, 60);
                if (i == hoverNode)
                    fill = fill.brighter();

                g2.setColor(fill);
                g2.fillOval(nd.x - R, nd.y - R, 2 * R, 2 * R);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(nd.x - R, nd.y - R, 2 * R, 2 * R);

                String label = String.valueOf(i);
                FontMetrics fm = g2.getFontMetrics();
                int tx = nd.x - fm.stringWidth(label) / 2;
                int ty = nd.y + fm.getAscent() / 2 - 2;
                g2.drawString(label, tx, ty);
            }

            // legend
            drawLegend(g2);
            g2.dispose();
        }

        private void drawLegend(Graphics2D g2) {
            int x = 12, y = 12;
            g2.setFont(getFont());
            g2.setColor(Color.WHITE);
            g2.drawString("Legend:", x, y + 0);
            y += 6;
            y = legendItem(g2, x, y + 14, new Color(70, 80, 200), "Unvisited");
            y = legendItem(g2, x, y + 14, new Color(200, 160, 60), "In Stack");
            y = legendItem(g2, x, y + 14, new Color(200, 80, 60), "Stack Top");
            y = legendItem(g2, x, y + 14, new Color(60, 160, 80), "Visited");
        }

        private int legendItem(Graphics2D g2, int x, int y, Color c, String name) {
            g2.setColor(c);
            g2.fillRect(x, y, 18, 12);
            g2.setColor(Color.WHITE);
            g2.drawRect(x, y, 18, 12);
            g2.drawString("  " + name, x + 22, y + 11);
            return y;
        }

        private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2, int size) {
            double dx = x2 - x1, dy = y2 - y1;
            double dist = Math.hypot(dx, dy);
            if (dist < 1)
                return;
            double ux = dx / dist, uy = dy / dist;
            int sx1 = (int) (x1 + ux * R * 0.9), sy1 = (int) (y1 + uy * R * 0.9);
            int sx2 = (int) (x2 - ux * R * 0.9), sy2 = (int) (y2 - uy * R * 0.9);
            g2.drawLine(sx1, sy1, sx2, sy2);
            // arrow head at (sx2, sy2)
            double angle = Math.atan2(dy, dx);
            double a1 = angle - Math.toRadians(25);
            double a2 = angle + Math.toRadians(25);
            int hx1 = (int) (sx2 - size * Math.cos(a1));
            int hy1 = (int) (sy2 - size * Math.sin(a1));
            int hx2 = (int) (sx2 - size * Math.cos(a2));
            int hy2 = (int) (sy2 - size * Math.sin(a2));
            g2.drawLine(sx2, sy2, hx1, hy1);
            g2.drawLine(sx2, sy2, hx2, hy2);
        }
    }

    // --- Utilities ---
    private int findNodeAt(int mx, int my) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            Node nd = nodes.get(i);
            int dx = mx - nd.x;
            int dy = my - nd.y;
            if (dx * dx + dy * dy <= R * R)
                return i;
        }
        return -1;
    }
}
