package com.staqueue;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

/**
 *
 * @author DELL
 */
public class QueueFrame extends JPanel {

    // ----- Dark theme -----
    private static final Color BG = new Color(24, 24, 26);
    private static final Color SURFACE = new Color(40, 40, 44);
    private static final Color TEXT = Color.WHITE;

    private static final Color MESSAGE_ACCENT = new Color(255, 215, 0); // gold/yellow

    Stack choiceStack;
    boolean isRandom;
    QDisplay qDisplay;
    JPanel qDisplayPanel;
    QueueMenu qMenu;
    JPanel qMenuPanel;
    JPanel qPanel;
    JLabel qMessage;
    JPanel qMessagePanel;

    public QueueFrame() {

        // Fill parent like Stack: use BorderLayout on *this* panel
        setLayout(new BorderLayout());
        setBackground(BG);

        choiceStack = new Stack(3);
        isRandom = false;

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BG);

        qPanel = new JPanel(new BorderLayout());
        qPanel.setOpaque(true);
        qPanel.setBackground(BG);

        // Queue title and styling
        JLabel qTitle = new JLabel("<HTML><U>DATA STRUCTURES : DEMONSTRATION OF THE QUEUE</U></HTML>");
        qTitle.setFont(new Font("Baskerville Old Face", Font.PLAIN, 28));
        qTitle.setForeground(Color.YELLOW);
        qTitle.setPreferredSize(new Dimension(800, 80));

        JPanel qTitlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        qTitlePanel.setBackground(SURFACE);
        qTitlePanel.add(qTitle);

        // Queue message panel and styling
        qMessage = new JLabel(">>>Welcome to Queue Demo. An empty queue has been made. Front = 0, Rear = -1");
        qMessage.setForeground(MESSAGE_ACCENT); // or Color.YELLOW, or a custom gold
        qMessage.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 20));

        qMessagePanel = new JPanel();
        qMessagePanel.setBackground(BG);
        qMessagePanel.add(qMessage);

        // Queue menu and styling
        qMenu = new QueueMenu();
        qMenu.setOpaque(true);
        qMenu.setBackground(SURFACE);
        // make any labels in the menu white
        for (Component c : qMenu.getComponents()) {
            if (c instanceof JLabel)
                ((JLabel) c).setForeground(TEXT);
        }

        qMenuPanel = new JPanel();
        qMenuPanel.setBorder(BorderFactory.createEtchedBorder(Color.DARK_GRAY, Color.LIGHT_GRAY));
        qMenuPanel.setBackground(SURFACE);
        // Let layout manage width (remove hard coded preferred size)
        // qMenu.setPreferredSize(new Dimension(1350, 100));
        qMenuPanel.add(qMenu);
        qMenuPanel.add(Box.createRigidArea(new Dimension(0, 100)));

        Box topDisplay = Box.createVerticalBox();
        topDisplay.setOpaque(true);
        topDisplay.setBackground(BG);
        topDisplay.add(qTitlePanel);
        topDisplay.add(qMessagePanel);
        topDisplay.add(qMenuPanel);

        qPanel.add(topDisplay, BorderLayout.NORTH);

        // Queue display and styling
        qDisplay = new QDisplay();
        qDisplayPanel = new JPanel();
        qDisplayPanel.setOpaque(true);
        qDisplayPanel.setBackground(BG);
        qDisplayPanel.add(Box.createRigidArea(new Dimension(0, 425)));
        qDisplayPanel.add(qDisplay);
        qDisplayPanel.setBorder(BorderFactory.createEtchedBorder(Color.DARK_GRAY, Color.LIGHT_GRAY));

        Box qDisplayBox = Box.createVerticalBox();
        qDisplayBox.setOpaque(true);
        qDisplayBox.setBackground(BG);
        qDisplayBox.add(qDisplayPanel);

        scrollPane.setViewportView(qDisplayBox);
        scrollPane.setVisible(true);

        qPanel.add(scrollPane, BorderLayout.CENTER);

        // add the fully managed panel into this (BorderLayout.CENTER)
        add(qPanel, BorderLayout.CENTER);

        // Interface to set the text in Queue Message Panel
        qDisplay.textSetter = new TextInterface() {
            public void setText(String str) {
                qMessage.setText(">>>" + str);
            }
        };

        addQueueActionListeners(); // Function to add action listeners to queue menu buttons
    }

    private void addQueueActionListeners() {

        // Queue Insert
        qMenu.insertButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String str = qMenu.text.getText();
                qMenu.text.setValue(null);
                if (isNum(str)) {
                    str = str.replace(",", "");
                    int num = Integer.parseInt(str);
                    qDisplay.update_insert(num, choiceStack);
                    qMenu.resetButton.setEnabled(true);
                    qMenu.resetEnabled = true;
                } else {
                    qDisplay.textSetter.setText("Invalid number");
                }
                if (choiceStack.top != -1) {
                    qMenu.undoButton.setEnabled(true);
                    qMenu.undoEnabled = true;
                }
                qPanel.updateUI();
            }
        });

        // Queue Delete
        qMenu.deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                qDisplay.update_delete(choiceStack);
                if (choiceStack.top != -1) {
                    qMenu.undoButton.setEnabled(true);
                }
                if (qDisplay.q.rear == -1 && qDisplay.q.front == 0 && choiceStack.top == -1) {
                    qMenu.resetButton.setEnabled(false);
                }
                qPanel.updateUI();
            }
        });

        // Queue Undo
        qMenu.undoButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int choice = choiceStack.pop();
                if (choice == 0) {
                    qDisplay.undoInsert();
                    if (qDisplay.q.rear == -1 && qDisplay.q.front == 0 && choiceStack.top == -1) {
                        qMenu.resetButton.setEnabled(false);
                    }
                } else {
                    qDisplay.undoDelete();
                    qMenu.resetButton.setEnabled(true);
                }
                if (choiceStack.top == -1) {
                    qMenu.undoButton.setEnabled(false);
                    qMenu.undoEnabled = false;
                }
                qPanel.updateUI();
            }
        });

        // Queue Reset
        qMenu.resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                qDisplay.reset();
                choiceStack.top = -1;
                qMenu.undoButton.setEnabled(false);
                qMenu.undoEnabled = false;
                qMenu.resetButton.setEnabled(false);
                qMenu.resetEnabled = false;
                qPanel.updateUI();
            }
        });
    }

    // Function to implement automatic random operations in Queue
    private void startQueueRandom() {
        SwingWorker<Void, Void> q_random;
        q_random = new SwingWorker<Void, Void>() {
            protected Void doInBackground() throws Exception {
                if (qDisplay.q.front == qDisplay.q.size) {
                    qDisplay.textSetter.setText(
                            "Cannot perform any more operations on the queue. Please try using the undo or the reset buttons to continue. Front = "
                                    + Integer.toString(qDisplay.q.front) + " Rear = "
                                    + Integer.toString(qDisplay.q.rear));
                    qMenu.randomButton.doClick();
                    return null;
                }
                Thread.sleep(100);
                qDisplay.random(choiceStack);
                if (choiceStack.top != -1) {
                    qMenu.undoEnabled = true;
                }
                qMenu.resetEnabled = true;
                qPanel.updateUI();
                Thread.sleep(750);
                return null;
            }

            protected void done() {
                if (isRandom) {
                    startQueueRandom();
                }
            }
        };
        q_random.execute();
    }

    // Function to check if the input string is a valid number or not
    private static boolean isNum(String str) {
        str = str.replace(",", "");
        try {
            Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}
