package com.staqueue;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Binary Search Tree visualizer with animations.
 * UI updates:
 * - Dry Run (pseudocode) moved into a slim right panel (like BFS/DFS).
 * - Bottom area shows status (left) and traversal output (center).
 * Functionality otherwise unchanged.
 */
public class BSTVisualizerPanel extends JPanel {

    // ===== Model =====
    private static class Node {
        int key;
        Node left, right;
        int x, y; // layout position

        Node(int key) {
            this.key = key;
        }
    }

    private Node root;
    private final Set<Integer> keys = new LinkedHashSet<>(); // prevent duplicates

    // ===== UI =====
    private final JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    private final JTextField txtValue = new JTextField(6);
    private final JButton btnInsert = new JButton("Insert");
    private final JButton btnDelete = new JButton("Delete");

    private final JLabel lblSearch = new JLabel("Search:");
    private final JTextField txtSearch = new JTextField(6);
    private final JButton btnSearch = new JButton("Find");

    private final JButton btnRandom = new JButton("Random 10");
    private final JButton btnNew = new JButton("New Tree");
    private final JButton btnClear = new JButton("Clear");
    private final JLabel lblCount = new JLabel("Nodes: 0");
    private final JLabel lblHeight = new JLabel("Height: 0");

    private final JButton btnInorder = new JButton("Inorder");
    private final JButton btnPreorder = new JButton("Preorder");
    private final JButton btnPostorder = new JButton("Postorder");
    private final JTextArea txtTraversal = new JTextArea(2, 40);
    private final JScrollPane traversalScroll = new JScrollPane(txtTraversal);

    // bottom status + pseudocode (pseudocode goes to right panel now)
    private final JLabel statusLabel = new JLabel("Ready.");
    private final DefaultListModel<String> pseudoModel = new DefaultListModel<>();
    private final JList<String> pseudoList = new JList<>(pseudoModel);
    private final JLabel lblSpeed = new JLabel("  Speed:");
    private final JSlider speed = new JSlider(100, 1200, 500);
    private final JButton btnStep = new JButton("Step");
    private final JButton btnReset = new JButton("Reset");

    // canvas + scroll
    private final DrawPanel canvas = new DrawPanel();
    private JScrollPane canvasScroll;

    // ===== NEW: larger fonts for bottom output =====
    private static final Font TRAV_FONT = new Font(Font.MONOSPACED, Font.BOLD, 18); // traversal line
    private static final Font STATUS_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 16); // status text

    // ===== Animation state =====
    private enum Op {
        NONE, SEARCH, INSERT, DELETE, TRAV_IN, TRAV_PRE, TRAV_POST
    }

    private Op currentOp = Op.NONE;
    private javax.swing.Timer timer;
    private int animDelay = 500;

    // search/insert/delete path animation
    private java.util.List<Node> path = new ArrayList<>();
    private int pathIndex = -1; // -1 before first step
    private int deleteStage = 0; // 0=path to target, 1=successor path, 2=apply
    private Node deleteTarget = null;
    private Node deleteSuccessor = null;
    private Integer searchTarget = null;
    private Integer insertValue = null;
    private Integer deleteValue = null;

    // traversal animation (explicit stack)
    private static class Frame {
        Node node;
        int state;

        Frame(Node n, int s) {
            node = n;
            state = s;
        }
    }

    private Deque<Frame> travStack = new ArrayDeque<>();
    private StringBuilder travOut = new StringBuilder();
    private Set<Node> visited = new HashSet<>();
    private Node current = null; // current highlighted node

    // layout constants
    private static final int BASE_W = 940, BASE_H = 560;
    private static final int NODE_R = 18;
    private static final int LEVEL_H = 70;
    private static final int X_GAP = 38; // base gap between in-order slots
    private static final int LEFT_MARGIN = 40, RIGHT_MARGIN = 40;

    public BSTVisualizerPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(28, 28, 30));

        // ----- Top bar (unchanged position/layout) -----
        topBar.setBackground(new Color(40, 40, 44));
        topBar.setBorder(new EmptyBorder(6, 8, 6, 8));
        for (JLabel l : new JLabel[] { lblSearch, lblCount, lblHeight, lblSpeed }) {
            l.setForeground(Color.WHITE);
        }

        txtTraversal.setEditable(false);
        txtTraversal.setLineWrap(true);
        txtTraversal.setWrapStyleWord(true);
        traversalScroll.setBorder(new EmptyBorder(4, 8, 4, 8));
        // >>> apply larger font to traversal output
        txtTraversal.setFont(TRAV_FONT);

        // Build top bar (2 rows x 3 columns, evenly spaced) — same as before
        topBar.setLayout(new GridLayout(2, 3, 12, 6));
        JPanel pnlValue = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JPanel pnlSearch = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JPanel pnlGen = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JPanel pnlTrav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JPanel pnlStats = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        JPanel pnlControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        for (JPanel p : new JPanel[] { pnlValue, pnlSearch, pnlGen, pnlTrav, pnlStats, pnlControls })
            p.setOpaque(false);

        JLabel valueLabel = new JLabel("Value:");
        valueLabel.setForeground(Color.WHITE);
        pnlValue.add(valueLabel);
        pnlValue.add(txtValue);
        pnlValue.add(btnInsert);
        pnlValue.add(btnDelete);

        pnlSearch.add(lblSearch);
        pnlSearch.add(txtSearch);
        pnlSearch.add(btnSearch);
        pnlGen.add(btnRandom);
        pnlGen.add(btnNew);
        pnlGen.add(btnClear);

        pnlTrav.add(btnInorder);
        pnlTrav.add(btnPreorder);
        pnlTrav.add(btnPostorder);
        pnlStats.add(lblCount);
        pnlStats.add(lblHeight);

        pnlControls.add(btnStep);
        pnlControls.add(btnReset);
        pnlControls.add(lblSpeed);
        pnlControls.add(speed);

        topBar.add(pnlValue);
        topBar.add(pnlGen);
        topBar.add(pnlControls);
        topBar.add(pnlSearch);
        topBar.add(pnlTrav);
        topBar.add(pnlStats);
        add(topBar, BorderLayout.NORTH);

        // ----- Right: Dry Run (pseudocode) panel -----
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(new Color(40, 40, 44));
        right.setBorder(new EmptyBorder(8, 8, 8, 8));
        JLabel title = new JLabel("Dry Run (Binary Search Tree)");
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

        // ----- Bottom: status (left) + traversal output (center) -----
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(new Color(40, 40, 44));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        left.setBackground(new Color(40, 40, 44));
        statusLabel.setForeground(Color.WHITE);
        // >>> apply larger font to status messages
        statusLabel.setFont(STATUS_FONT);
        left.add(statusLabel);
        bottom.add(left, BorderLayout.WEST);
        bottom.add(traversalScroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // ----- Center: canvas (scrollable) -----
        canvas.setPreferredSize(new Dimension(BASE_W, BASE_H));
        canvasScroll = new JScrollPane(canvas);
        canvasScroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        canvasScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        canvasScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        canvasScroll.getHorizontalScrollBar().setUnitIncrement(24);
        add(canvasScroll, BorderLayout.CENTER);

        // ----- Wire actions -----
        btnInsert.addActionListener(e -> startSearchOrInsert(true));
        btnDelete.addActionListener(e -> startDelete());
        btnSearch.addActionListener(e -> startSearch());
        txtValue.addActionListener(e -> btnInsert.doClick());
        txtSearch.addActionListener(e -> btnSearch.doClick());

        btnRandom.addActionListener(e -> {
            clear();
            randomFill(10);
            relayoutAndRefresh();
        });
        btnClear.addActionListener(e -> {
            clear();
            relayoutAndRefresh();
        });
        btnNew.addActionListener(e -> {
            clear();
            randomFill(8 + new Random().nextInt(5));
            relayoutAndRefresh();
        });

        btnInorder.addActionListener(e -> startTraversal(Op.TRAV_IN));
        btnPreorder.addActionListener(e -> startTraversal(Op.TRAV_PRE));
        btnPostorder.addActionListener(e -> startTraversal(Op.TRAV_POST));

        btnStep.addActionListener(e -> stepOnce());
        btnReset.addActionListener(e -> resetAnim());

        speed.addChangeListener(e -> {
            animDelay = speed.getValue();
            if (timer != null)
                timer.setDelay(animDelay);
        });

        relayoutAndRefresh();
    }

    // ===== Basic BST operations (no animation) =====
    private void clear() {
        root = null;
        keys.clear();
        txtTraversal.setText("");
        txtValue.setText("");
        txtSearch.setText("");
        visited.clear();
        current = null;
    }

    private void randomFill(int n) {
        Random rnd = new Random();
        int attempts = 0;
        while (keys.size() < n && attempts < n * 12) {
            int v = rnd.nextInt(99) + 1; // 1..99
            insertRaw(v);
            attempts++;
        }
    }

    private Integer readInt(String s) {
        try {
            if (s == null || s.trim().isEmpty())
                return null;
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ex) {
            Toolkit.getDefaultToolkit().beep();
            return null;
        }
    }

    private void insertRaw(int key) {
        if (keys.contains(key))
            return;
        keys.add(key);
        root = insertRec(root, key);
    }

    private Node insertRec(Node node, int key) {
        if (node == null)
            return new Node(key);
        if (key < node.key)
            node.left = insertRec(node.left, key);
        else if (key > node.key)
            node.right = insertRec(node.right, key);
        return node;
    }

    private void deleteRaw(int key) {
        if (!keys.contains(key))
            return;
        keys.remove(key);
        root = deleteRec(root, key);
    }

    private Node deleteRec(Node node, int key) {
        if (node == null)
            return null;
        if (key < node.key)
            node.left = deleteRec(node.left, key);
        else if (key > node.key)
            node.right = deleteRec(node.right, key);
        else {
            if (node.left == null)
                return node.right;
            if (node.right == null)
                return node.left;
            Node succ = minNode(node.right);
            node.key = succ.key;
            node.right = deleteRec(node.right, succ.key);
        }
        return node;
    }

    private Node minNode(Node n) {
        while (n != null && n.left != null)
            n = n.left;
        return n;
    }

    private int count(Node n) {
        return (n == null) ? 0 : 1 + count(n.left) + count(n.right);
    }

    private int height(Node n) {
        return (n == null) ? 0 : 1 + Math.max(height(n.left), height(n.right));
    }

    private java.util.List<Integer> inorderList() {
        java.util.List<Integer> out = new ArrayList<>();
        inorderRec(root, out);
        return out;
    }

    private void inorderRec(Node n, java.util.List<Integer> out) {
        if (n == null)
            return;
        inorderRec(n.left, out);
        out.add(n.key);
        inorderRec(n.right, out);
    }

    private java.util.List<Integer> preorderList() {
        java.util.List<Integer> out = new ArrayList<>();
        preorderRec(root, out);
        return out;
    }

    private void preorderRec(Node n, java.util.List<Integer> out) {
        if (n == null)
            return;
        out.add(n.key);
        preorderRec(n.left, out);
        preorderRec(n.right, out);
    }

    private java.util.List<Integer> postorderList() {
        java.util.List<Integer> out = new ArrayList<>();
        postorderRec(root, out);
        return out;
    }

    private void postorderRec(Node n, java.util.List<Integer> out) {
        if (n == null)
            return;
        postorderRec(n.left, out);
        postorderRec(n.right, out);
        out.add(n.key);
    }

    private void showTraversalInstant(String name, java.util.List<Integer> list) {
        txtTraversal.setText(name + ": " + join(list, " \u2192 "));
        status("Ready.");
    }

    private String join(java.util.List<Integer> list, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0)
                sb.append(sep);
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    // ===== Animated operations =====
    private void startSearch() {
        Integer val = readInt(txtSearch.getText());
        if (val == null)
            return;
        resetAnim();
        setPseudoSearch();
        currentOp = Op.SEARCH;
        searchTarget = val;
        path = buildPath(val);
        pathIndex = -1;
        status("Searching " + val + "...");
        selectPseudo(1);
        runTimer(this::searchTick);
    }

    // Reuse same insert UI action for "insert or show duplication"
    private void startSearchOrInsert(boolean insert) {
        Integer val = readInt(txtValue.getText());
        if (val == null)
            return;
        if (insert && keys.contains(val)) {
            setPseudoInsert();
            status("Value already exists.");
            return;
        }
        resetAnim();
        setPseudoInsert();
        currentOp = Op.INSERT;
        insertValue = val;
        path = buildPath(val);
        pathIndex = -1;
        status("Inserting " + val + "...");
        runTimer(this::insertTick);
    }

    private void startDelete() {
        Integer val = readInt(txtValue.getText());
        if (val == null)
            return;
        resetAnim();
        setPseudoDelete();
        currentOp = Op.DELETE;
        deleteValue = val;
        path = buildPath(val);
        pathIndex = -1;
        deleteStage = 0;
        deleteTarget = null;
        deleteSuccessor = null;
        status("Deleting " + val + "...");
        runTimer(this::deleteTick);
    }

    private java.util.List<Node> buildPath(int key) {
        java.util.List<Node> p = new ArrayList<>();
        Node cur = root;
        while (cur != null) {
            p.add(cur);
            if (key == cur.key)
                break;
            cur = (key < cur.key) ? cur.left : cur.right;
        }
        return p;
    }

    private boolean searchTick() {
        if (path == null)
            path = Collections.emptyList();
        if (pathIndex + 1 >= path.size()) {
            selectPseudo(2);
            status("Value " + searchTarget + " is not in the BST.");
            return false;
        }
        pathIndex++;
        Node cur = path.get(pathIndex);
        ensureVisible(cur);
        current = cur;
        visited.add(cur);
        if (cur.key == searchTarget) {
            selectPseudo(3);
            status("Found " + searchTarget + ".");
            return false;
        } else if (searchTarget > cur.key) {
            selectPseudo(4);
            return true;
        } else {
            selectPseudo(5);
            return true;
        }
    }

    private boolean insertTick() {
        if (root == null) {
            selectPseudo(1);
            insertRaw(insertValue);
            relayoutAndRefresh();
            status("Inserted " + insertValue + " at root.");
            return false;
        }
        if (path == null)
            path = Collections.emptyList();
        if (pathIndex + 1 >= path.size()) {
            selectPseudo(4);
            insertRaw(insertValue);
            relayoutAndRefresh();
            status("Inserted " + insertValue + ".");
            return false;
        }
        pathIndex++;
        Node cur = path.get(pathIndex);
        ensureVisible(cur);
        current = cur;
        visited.add(cur);
        if (insertValue == cur.key) {
            status("Value already exists.");
            return false;
        } else if (insertValue > cur.key) {
            selectPseudo(3);
            return true;
        } else {
            selectPseudo(2);
            return true;
        }
    }

    private boolean deleteTick() {
        if (deleteStage == 0) {
            if (path == null || path.isEmpty() || pathIndex + 1 >= path.size()) {
                selectPseudo(1);
                status("Value " + deleteValue + " is not in the BST.");
                return false;
            }
            pathIndex++;
            Node cur = path.get(pathIndex);
            ensureVisible(cur);
            current = cur;
            visited.add(cur);
            if (cur.key == deleteValue) {
                deleteTarget = cur;
                if (cur.left == null || cur.right == null) {
                    selectPseudo(5);
                    deleteRaw(deleteValue);
                    relayoutAndRefresh();
                    status("Deleted " + deleteValue + ".");
                    return false;
                } else {
                    selectPseudo(7);
                    deleteStage = 1;
                    // successor path
                    java.util.List<Node> sp = new ArrayList<>();
                    Node n = cur.right;
                    while (n != null) {
                        sp.add(n);
                        if (n.left == null)
                            break;
                        n = n.left;
                    }
                    path = sp;
                    pathIndex = -1;
                    return true;
                }
            } else if (deleteValue > cur.key) {
                selectPseudo(3);
                return true;
            } else {
                selectPseudo(2);
                return true;
            }
        } else if (deleteStage == 1) {
            if (pathIndex + 1 >= path.size()) {
                if (!path.isEmpty())
                    deleteSuccessor = path.get(path.size() - 1);
                selectPseudo(5);
                if (deleteTarget != null && deleteTarget.left != null && deleteTarget.right != null) {
                    int succKey = minNode(deleteTarget.right).key;
                    deleteTarget.key = succKey;
                    root = deleteRec(root, succKey);
                    keys.remove(deleteValue);
                }
                relayoutAndRefresh();
                status("Deleted " + deleteValue + ".");
                return false;
            }
            pathIndex++;
            Node cur = path.get(pathIndex);
            ensureVisible(cur);
            current = cur;
            visited.add(cur);
            return true;
        }
        return false;
    }

    // ===== Traversal animations =====
    private void startTraversal(Op op) {
        if (op != Op.TRAV_IN && op != Op.TRAV_PRE && op != Op.TRAV_POST)
            return;
        resetAnim();
        currentOp = op;
        visited.clear();
        current = null;
        travOut = new StringBuilder();
        travStack.clear();
        travStack.push(new Frame(root, 0));
        if (op == Op.TRAV_IN)
            setPseudoInorder();
        else if (op == Op.TRAV_PRE)
            setPseudoPreorder();
        else
            setPseudoPostorder();
        txtTraversal.setText("");
        status(opName(op) + " traversal...");
        runTimer(() -> traversalTick(op));
    }

    private String opName(Op op) {
        switch (op) {
            case TRAV_IN:
                return "Inorder";
            case TRAV_PRE:
                return "Preorder";
            case TRAV_POST:
                return "Postorder";
            default:
                return "";
        }
    }

    private boolean traversalTick(Op op) {
        if (travStack.isEmpty()) {
            txtTraversal.setText(opName(op) + ": " + travOut.toString());
            status("Traversal finished.");
            return false;
        }
        Frame f = travStack.peek();
        Node n = f.node;
        if (op == Op.TRAV_IN) {
            if (f.state == 0) {
                selectPseudo(1);
                if (n == null) {
                    travStack.pop();
                    return true;
                }
                f.state = 1;
                return true;
            } else if (f.state == 1) {
                selectPseudo(2);
                travStack.push(new Frame(n.left, 0));
                f.state = 2;
                return true;
            } else if (f.state == 2) {
                selectPseudo(3);
                visitNode(n);
                f.state = 3;
                return true;
            } else if (f.state == 3) {
                selectPseudo(4);
                travStack.push(new Frame(n.right, 0));
                f.state = 4;
                return true;
            } else {
                travStack.pop();
                return true;
            }
        } else if (op == Op.TRAV_PRE) {
            if (f.state == 0) {
                selectPseudo(1);
                if (n == null) {
                    travStack.pop();
                    return true;
                }
                f.state = 1;
                return true;
            } else if (f.state == 1) {
                selectPseudo(2);
                visitNode(n);
                f.state = 2;
                return true;
            } else if (f.state == 2) {
                selectPseudo(3);
                travStack.push(new Frame(n.left, 0));
                f.state = 3;
                return true;
            } else if (f.state == 3) {
                selectPseudo(4);
                travStack.push(new Frame(n.right, 0));
                f.state = 4;
                return true;
            } else {
                travStack.pop();
                return true;
            }
        } else { // TRAV_POST
            if (f.state == 0) {
                selectPseudo(1);
                if (n == null) {
                    travStack.pop();
                    return true;
                }
                f.state = 1;
                return true;
            } else if (f.state == 1) {
                selectPseudo(2);
                travStack.push(new Frame(n.left, 0));
                f.state = 2;
                return true;
            } else if (f.state == 2) {
                selectPseudo(3);
                travStack.push(new Frame(n.right, 0));
                f.state = 3;
                return true;
            } else if (f.state == 3) {
                selectPseudo(4);
                visitNode(n);
                f.state = 4;
                return true;
            } else {
                travStack.pop();
                return true;
            }
        }
    }

    private void visitNode(Node n) {
        current = n;
        visited.add(n);
        ensureVisible(n);
        if (travOut.length() > 0)
            travOut.append(" \u2192 ");
        travOut.append(n.key);
        txtTraversal.setText(opName(currentOp) + ": " + travOut.toString());
    }

    private void runTimer(java.util.function.Supplier<Boolean> tick) {
        if (timer != null)
            timer.stop();
        timer = new javax.swing.Timer(animDelay, e -> {
            boolean cont = tick.get();
            canvas.repaint();
            if (!cont)
                timerStop();
        });
        timer.start();
    }

    private void stepOnce() {
        if (currentOp == Op.NONE)
            return;
        if (timer != null && timer.isRunning())
            timer.stop();
        boolean cont;
        switch (currentOp) {
            case SEARCH:
                cont = searchTick();
                break;
            case INSERT:
                cont = insertTick();
                break;
            case DELETE:
                cont = deleteTick();
                break;
            case TRAV_IN:
                cont = traversalTick(Op.TRAV_IN);
                break;
            case TRAV_PRE:
                cont = traversalTick(Op.TRAV_PRE);
                break;
            case TRAV_POST:
                cont = traversalTick(Op.TRAV_POST);
                break;
            default:
                cont = false;
        }
        canvas.repaint();
        if (!cont)
            timerStop();
    }

    private void resetAnim() {
        if (timer != null && timer.isRunning())
            timer.stop();
        currentOp = Op.NONE;
        path = new ArrayList<>();
        pathIndex = -1;
        deleteStage = 0;
        deleteTarget = null;
        deleteSuccessor = null;
        pseudoModel.clear();
        pseudoList.clearSelection();
        visited.clear();
        current = null;
    }

    private void timerStop() {
        if (timer != null)
            timer.stop();
    }

    // ===== Pseudocode =====
    private void setPseudoSearch() {
        setPseudo("Search(x)",
                "if this == null",
                "    return null",
                "else if this key == x",
                "    return this",
                "else if this key < x",
                "    search right",
                "else search left");
    }

    private void setPseudoInsert() {
        setPseudo("Insert(x)",
                "if root == null: root = new Node(x)",
                "cur = root",
                "if x < cur.key: go left",
                "else go right",
                "repeat until null, then attach new node");
    }

    private void setPseudoDelete() {
        setPseudo("Delete(x)",
                "if this == null: return null",
                "else if x < key: delete left",
                "else if x > key: delete right",
                "else: // found",
                "    if left == null: return right",
                "    if right == null: return left",
                "    succ = min(right)",
                "    key = succ.key",
                "    delete succ.key in right");
    }

    private void setPseudoInorder() {
        setPseudo("Inorder(root)",
                "if root == null: return",
                "inorder(root.left)",
                "visit(root)",
                "inorder(root.right)");
    }

    private void setPseudoPreorder() {
        setPseudo("Preorder(root)",
                "if root == null: return",
                "visit(root)",
                "preorder(root.left)",
                "preorder(root.right)");
    }

    private void setPseudoPostorder() {
        setPseudo("Postorder(root)",
                "if root == null: return",
                "postorder(root.left)",
                "postorder(root.right)",
                "visit(root)");
    }

    private void setPseudo(String title, String... lines) {
        pseudoModel.clear();
        pseudoModel.addElement("// " + title);
        for (String ln : lines)
            pseudoModel.addElement(ln);
        pseudoList.setSelectedIndex(0);
    }

    private void selectPseudo(int idx) {
        if (idx >= 0 && idx < pseudoModel.size())
            pseudoList.setSelectedIndex(idx);
    }

    private void status(String text) {
        statusLabel.setText(text);
    }

    // ===== Layout =====
    private void relayoutAndRefresh() {
        layoutTree();
        refreshStats();
        canvas.repaint();
    }

    private void refreshStats() {
        lblCount.setText("Nodes: " + count(root));
        lblHeight.setText("Height: " + height(root));
    }

    private void layoutTree() {
        if (root == null) {
            canvas.setPreferredSize(new Dimension(BASE_W, BASE_H));
            canvas.revalidate();
            return;
        }
        Map<Node, Integer> xIndex = new HashMap<>();
        int[] counter = new int[] { 0 };
        assignInorderX(root, xIndex, counter);
        int total = Math.max(1, counter[0]);
        int width = Math.max(BASE_W - LEFT_MARGIN - RIGHT_MARGIN, total * X_GAP);
        assignXY(root, xIndex, width, 0);

        int desiredW = LEFT_MARGIN + width + RIGHT_MARGIN;
        canvas.setPreferredSize(new Dimension(Math.max(BASE_W, desiredW), BASE_H));
        canvas.revalidate();
    }

    private void assignInorderX(Node n, Map<Node, Integer> map, int[] counter) {
        if (n == null)
            return;
        assignInorderX(n.left, map, counter);
        map.put(n, counter[0]++);
        assignInorderX(n.right, map, counter);
    }

    private void assignXY(Node n, Map<Node, Integer> xIndex, int width, int depth) {
        if (n == null)
            return;
        int idx = xIndex.get(n);
        int total = Math.max(1, xIndex.size());
        int x = LEFT_MARGIN + (int) (idx * ((width - 1.0) / Math.max(1, total - 1)));
        int y = 60 + depth * LEVEL_H;
        n.x = x;
        n.y = y;
        assignXY(n.left, xIndex, width, depth + 1);
        assignXY(n.right, xIndex, width, depth + 1);
    }

    private void ensureVisible(Node n) {
        if (n == null)
            return;
        Rectangle r = new Rectangle(n.x - 40, n.y - 40, 80, 80);
        canvas.scrollRectToVisible(r);
    }

    // ===== Drawing =====
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

            // edges
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(115, 115, 120));
            drawEdges(g2, root);

            // nodes
            drawNodes(g2, root);

            g2.dispose();
        }

        private void drawEdges(Graphics2D g2, Node n) {
            if (n == null)
                return;
            if (n.left != null)
                g2.drawLine(n.x, n.y, n.left.x, n.left.y);
            if (n.right != null)
                g2.drawLine(n.x, n.y, n.right.x, n.right.y);
            drawEdges(g2, n.left);
            drawEdges(g2, n.right);
        }

        private void drawNodes(Graphics2D g2, Node n) {
            if (n == null)
                return;
            drawNodes(g2, n.left);

            boolean isCurrent = (current == n);
            boolean isVisited = visited.contains(n);

            // Fallback to path coloring for search/insert/delete
            if (!isVisited && path != null && !path.isEmpty()) {
                for (int i = 0; i <= pathIndex && i < path.size(); i++) {
                    if (path.get(i) == n) {
                        isVisited = true;
                        if (i == pathIndex)
                            isCurrent = true;
                        break;
                    }
                }
            }

            if (isCurrent)
                g2.setColor(new Color(255, 170, 60)); // current step (orange)
            else if (isVisited)
                g2.setColor(new Color(235, 140, 40)); // visited path
            else
                g2.setColor(new Color(70, 80, 200)); // default

            g2.fillOval(n.x - NODE_R, n.y - NODE_R, NODE_R * 2, NODE_R * 2);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(n.x - NODE_R, n.y - NODE_R, NODE_R * 2, NODE_R * 2);

            String label = String.valueOf(n.key);
            FontMetrics fm = g2.getFontMetrics();
            int tx = n.x - fm.stringWidth(label) / 2;
            int ty = n.y + fm.getAscent() / 2 - 2;
            g2.drawString(label, tx, ty);

            drawNodes(g2, n.right);
        }
    }
}
