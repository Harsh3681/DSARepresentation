package com.staqueue;

import java.awt.*;
import java.awt.event.KeyEvent;
import javax.swing.*;

/**
 * Application shell: builds the main tabbed UI.
 * Tabs (in order): Stack, Queue, Linked List, Binary Search Tree, BFS, DFS.
 * Only modern panels are wired. All labels use the dark theme.
 */
public class Frame extends JFrame {

    public Frame() {
        super("AlgoNova");
        getContentPane().setBackground(new Color(24, 24, 26));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Dark UI defaults
        UIManager.put("Panel.background", new Color(24, 24, 26));
        UIManager.put("OptionPane.background", new Color(40, 40, 44));
        UIManager.put("OptionPane.messageForeground", Color.WHITE);
        UIManager.put("TabbedPane.contentAreaColor", new Color(24, 24, 26));

        JTabbedPane tabs = new JTabbedPane();

        // 1) Stack
        JScrollPane stackScroll = new JScrollPane();
        StackSetup stackSetup = new StackSetup();
        stackScroll.setViewportView(stackSetup);
        stackSetup.setPreferredSize(tabs.getSize());
        tabs.addTab("Stack", stackScroll);
        tabs.setMnemonicAt(0, KeyEvent.VK_1);

        stackSetup.createStackButton.addActionListener(ae -> {
            stackSetup.isDynamicStack = stackSetup.dynamicStackYes.isSelected();
            stackSetup.numberOfStacks = 1;
            StackFrame stackFrame = new StackFrame(1, stackSetup.isDynamicStack);
            stackScroll.setViewportView(stackFrame);
            tabs.setComponentAt(0, stackScroll);
            stackFrame.stackMenu.settingsBtn.addActionListener(e -> {
                int res = JOptionPane.showConfirmDialog(
                        null,
                        "Are you sure? You will lose all the data in the stack(s).",
                        "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (res == JOptionPane.YES_OPTION) {
                    stackScroll.setViewportView(stackSetup);
                    tabs.setComponentAt(0, stackScroll);
                }
            });
        });

        // 2) Queue
        tabs.addTab("Queue", new QueueFrame());
        tabs.setMnemonicAt(1, KeyEvent.VK_2);

        // 3) Linked List (new visualizer)
        tabs.addTab("Linked List", new LinkedListVisualizerPanel());
        tabs.setMnemonicAt(2, KeyEvent.VK_3);

        // 4) Binary Search Tree
        tabs.addTab("Binary Search Tree (BST)", new BSTVisualizerPanel());
        tabs.setMnemonicAt(3, KeyEvent.VK_4);

        // 5) BFS
        tabs.addTab("Breadth First Search (BFS)", new BFSVisualizerPanel());
        tabs.setMnemonicAt(4, KeyEvent.VK_B);

        // 6) DFS
        tabs.addTab("Depth First Search (DFS)", new DFSVisualizerPanel());
        tabs.setMnemonicAt(5, KeyEvent.VK_D);

        add(tabs);
        setVisible(true);
    }
}