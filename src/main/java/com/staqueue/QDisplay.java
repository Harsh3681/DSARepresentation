package com.staqueue;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

public class QDisplay extends JPanel {

    // Theme
    private static final Color BG = new Color(24, 24, 26);
    private static final Color NODE = new Color(70, 80, 200); // occupied -> blue
    private static final Color TEXT = Color.WHITE;
    private static final Color EMPTY = Color.WHITE; // dequeued -> white

    public Queue q;
    private JPanel panel[];
    private JLabel elts[];
    private JLabel front;
    private JPanel[] frontPanel;
    private JLabel rear;
    private JPanel[] rearPanel;
    public TextInterface textSetter;
    public static Boolean q_isRandom;

    // reuse the same padding for borders
    private final Border pad = BorderFactory.createEmptyBorder(5, 5, 0, 5);
    // black outline, as requested
    private final Border blackOutline = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2), pad);

    QDisplay() {
        super();
        super.setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(BG);

        q = new Queue(15);

        elts = new JLabel[q.size + 2];
        panel = new JPanel[q.size + 2];
        front = new JLabel("Front");
        rear = new JLabel("Rear");
        front.setFont(new Font("Serif", Font.BOLD, 12));
        rear.setFont(new Font("Serif", Font.BOLD, 12));
        front.setForeground(TEXT);
        rear.setForeground(TEXT);

        frontPanel = new JPanel[q.size + 2];
        rearPanel = new JPanel[q.size + 2];
        Box eltBox, panelBox[];
        eltBox = Box.createHorizontalBox();
        eltBox.setOpaque(true);
        eltBox.setBackground(BG);
        panelBox = new Box[q.size + 2];

        // left spacer column
        elts[0] = new JLabel("");
        elts[0].setForeground(TEXT);

        panel[0] = new JPanel();
        panel[0].setOpaque(true);
        panel[0].setBackground(BG);
        panel[0].setPreferredSize(new Dimension(55, 45));

        frontPanel[0] = new JPanel();
        frontPanel[0].setOpaque(true);
        frontPanel[0].setBackground(BG);
        frontPanel[0].setPreferredSize(new Dimension(55, 30));

        rearPanel[0] = new JPanel();
        rearPanel[0].setOpaque(true);
        rearPanel[0].setBackground(BG);
        rearPanel[0].setPreferredSize(new Dimension(55, 20));
        rearPanel[0].add(rear);

        panelBox[0] = Box.createVerticalBox();
        panelBox[0].setOpaque(true);
        panelBox[0].setBackground(BG);
        panelBox[0].add(frontPanel[0]);
        panelBox[0].add(panel[0]);
        panelBox[0].add(rearPanel[0]);
        eltBox.add(panelBox[0]);

        // queue cells 1..size
        for (int i = 1; i <= q.size; i++) {
            elts[i] = new JLabel("");
            elts[i].setForeground(TEXT);

            frontPanel[i] = new JPanel();
            frontPanel[i].setOpaque(true);
            frontPanel[i].setBackground(BG);
            frontPanel[i].setPreferredSize(new Dimension(55, 30));

            panel[i] = new JPanel();
            panel[i].setOpaque(true);
            panel[i].setBackground(NODE); // initially blue
            panel[i].setBorder(blackOutline); // black border
            panel[i].setPreferredSize(new Dimension(55, 45));
            panel[i].add(elts[i]);

            rearPanel[i] = new JPanel();
            rearPanel[i].setOpaque(true);
            rearPanel[i].setBackground(BG);
            rearPanel[i].setPreferredSize(new Dimension(55, 20));

            panelBox[i] = Box.createVerticalBox();
            panelBox[i].setOpaque(true);
            panelBox[i].setBackground(BG);
            panelBox[i].add(frontPanel[i]);
            panelBox[i].add(panel[i]);
            panelBox[i].add(rearPanel[i]);

            eltBox.add(panelBox[i]);
        }

        // right spacer column
        frontPanel[q.size + 1] = new JPanel();
        frontPanel[q.size + 1].setOpaque(true);
        frontPanel[q.size + 1].setBackground(BG);
        frontPanel[q.size + 1].setPreferredSize(new Dimension(55, 30));

        rearPanel[q.size + 1] = new JPanel();
        rearPanel[q.size + 1].setOpaque(true);
        rearPanel[q.size + 1].setBackground(BG);
        rearPanel[q.size + 1].setPreferredSize(new Dimension(55, 20));

        elts[q.size + 1] = new JLabel("");
        elts[q.size + 1].setForeground(TEXT);

        panel[q.size + 1] = new JPanel();
        panel[q.size + 1].setOpaque(true);
        panel[q.size + 1].setBackground(BG);
        panel[q.size + 1].add(elts[q.size + 1]);
        panel[q.size + 1].setPreferredSize(new Dimension(55, 45));

        panelBox[q.size + 1] = Box.createVerticalBox();
        panelBox[q.size + 1].setOpaque(true);
        panelBox[q.size + 1].setBackground(BG);
        panelBox[q.size + 1].add(frontPanel[q.size + 1]);
        panelBox[q.size + 1].add(panel[q.size + 1]);
        panelBox[q.size + 1].add(rearPanel[q.size + 1]);
        eltBox.add(panelBox[q.size + 1]);

        frontPanel[1].add(front);

        super.add(eltBox, BorderLayout.CENTER);
        setBackground(BG);
    }

    public void update_insert(int num, Stack undo) {
        if (q.front == q.size) {
            textSetter.setText(
                    "Cannot perform any more operations on the queue. Please try using the undo or the reset buttons to continue. Front = "
                            + q.front + " Rear = " + q.rear);
            return;
        }
        if (q.rear == q.size - 1) {
            textSetter.setText("The queue is full! Cannot insert more elements. Front = " + q.front
                    + " Rear = " + q.rear);
            return;
        }
        if (undo.top == undo.size - 1)
            undo.forgetEarlierChoice();
        undo.push(0);

        rearPanel[q.rear + 1].remove(rear);
        q.insert(num);
        rearPanel[q.rear + 1].add(rear);

        // paint newly occupied slot blue (border already black)
        panel[q.rear + 1].setBackground(NODE);
        elts[q.rear + 1].setText(Integer.toString(num));
        elts[q.rear + 1].setForeground(TEXT);

        textSetter.setText(num + " has been inserted to the queue. Front = "
                + q.front + " Rear = " + q.rear);
    }

    public void update_delete(Stack undo) {
        if (q.front == q.size) {
            textSetter.setText(
                    "Cannot perform any more operations on the queue. Please try using the undo or the reset buttons to continue. Front = "
                            + q.front + " Rear = " + q.rear);
            return;
        }
        if (q.front > q.rear) {
            textSetter.setText("The queue is empty! Cannot remove elements. Front = " + q.front
                    + " Rear = " + q.rear);
            return;
        }
        if (undo.top == undo.size - 1)
            undo.forgetEarlierChoice();
        undo.push(1);

        // turn ONLY the dequeued cell white and clear its label
        frontPanel[q.front + 1].remove(front);
        panel[q.front + 1].setBackground(EMPTY); // <- white
        elts[q.front + 1].setText("");

        int num = q.del();
        frontPanel[q.front + 1].add(front);

        textSetter.setText(num + " has been removed from the queue. Front = "
                + q.front + " Rear = " + q.rear);
    }

    public void undoInsert() {
        // the last inserted cell becomes empty again
        elts[q.rear + 1].setText("");
        panel[q.rear + 1].setBackground(EMPTY); // back to white
        rearPanel[q.rear + 1].remove(rear);
        q.rear--;
        rearPanel[q.rear + 1].add(rear);
        textSetter.setText("Undo successful. Front = " + q.front + " Rear = " + q.rear);
    }

    public void undoDelete() {
        // restore the value and color of the previously deleted cell
        frontPanel[q.front + 1].remove(front);
        q.front--;
        frontPanel[q.front + 1].add(front);

        elts[q.front + 1].setText(Integer.toString(q.getVal(q.front)));
        elts[q.front + 1].setForeground(TEXT);
        panel[q.front + 1].setBackground(NODE); // back to blue

        textSetter.setText("Undo successful. Front = " + q.front + " Rear = " + q.rear);
    }

    public void random(Stack undo) {
        int choice;
        Random getRandom = new Random();
        if (q.rear == q.size - 1)
            choice = 1;
        else if (q.front > q.rear)
            choice = 0;
        else {
            int r = getRandom.nextInt(10 + this.q.nElts);
            choice = (r < 5) ? 0 : 1;
        }
        if (choice == 1)
            update_delete(undo);
        else
            update_insert(getRandom.nextInt(100), undo);
    }

    public void reset() {
        frontPanel[q.front + 1].remove(front);
        rearPanel[q.rear + 1].remove(rear);
        q.front = 0;
        q.rear = -1;

        // reset all cells to "empty but blue by default" as per your UI
        for (int i = 1; i <= q.size; i++) {
            elts[i].setText("");
            panel[i].setBackground(NODE); // start state: blue
            panel[i].setBorder(blackOutline); // ensure black border stays
        }

        frontPanel[1].add(front);
        rearPanel[0].add(rear);
        textSetter.setText("Queue has been reset. Front = " + q.front + " Rear = " + q.rear);
    }
}
