package com.staqueue;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author DELL
 */
public class StackDisplay extends JPanel {

    // --- Dark theme + node colors (match Linked List) ---
    private static final Color NODE_BLUE = new Color(70, 80, 200);
    private static final Color BG_DARK = new Color(24, 24, 26);
    private static final Color FG_WHITE = Color.WHITE;
    private static final Color EMPTY_CELL = Color.WHITE; // <-- empty static cells are white

    public Stack stack;
    private JLabel topLabel;
    public TextInterface textSetter;
    public Stack popStack;
    public StackElement[] rowElement;
    public ArrayList dynamicStack;
    public Box columnBox;

    StackDisplay(boolean isDynamic) {
        super();
        super.setLayout(new BorderLayout());
        super.setBackground(BG_DARK);

        popStack = new Stack(3); // stack used to store recently popped elements, for undo

        topLabel = new JLabel("TOP->"); // Label to point to top element in gui
        topLabel.setFont(new Font("Serif", Font.BOLD, 12));
        topLabel.setForeground(FG_WHITE);

        columnBox = Box.createVerticalBox(); // columnBox to hold all rows
        columnBox.setBackground(BG_DARK);
        columnBox.add(Box.createRigidArea(new Dimension(0, isDynamic ? 60 : 30)));

        if (!isDynamic) {
            stack = new Stack(9); // Object of type Stack class used to simulate the stack
            rowElement = new StackElement[stack.size + 1];

            // actual cells
            for (int i = 0; i < stack.size; i++) {
                rowElement[i] = new StackElement();
                rowElement[i].setOpaque(false);
                rowElement[i].topPanel.setOpaque(false);
                rowElement[i].eltPanel.setOpaque(true);
                rowElement[i].eltPanel.setBackground(EMPTY_CELL); // empty cells start WHITE
                // label text color is irrelevant for empty cells, but set to black just in case
                rowElement[i].elt.setForeground(Color.BLACK);
                columnBox.add(rowElement[i]);
            }

            // last row only to host TOP label (no blue box)
            rowElement[stack.size] = new StackElement();
            rowElement[stack.size].topPanel.add(topLabel);
            rowElement[stack.size].eltPanel.setBorder(null);
            rowElement[stack.size].eltPanel.setOpaque(false);
            rowElement[stack.size].setOpaque(false);
            columnBox.add(rowElement[stack.size]);
        } else {
            dynamicStack = new ArrayList();
            StackElement elt = new StackElement();
            // host TOP label; no blue box here
            elt.topPanel.add(topLabel);
            elt.topPanel.setVisible(true);
            elt.eltPanel.setBorder(null);
            elt.eltPanel.setOpaque(false);
            dynamicStack.add(elt);
            columnBox.add((StackElement) dynamicStack.get(0));
        }

        super.add(columnBox, BorderLayout.CENTER);
        setOpaque(true);
        setBackground(BG_DARK);
    }

    // --- Helpers to tint a cell (occupied vs empty) ---

    private void paintAsOccupied(StackElement se, int value) {
        se.eltPanel.setOpaque(true);
        se.eltPanel.setBackground(NODE_BLUE);
        se.elt.setForeground(FG_WHITE);
        se.elt.setText(Integer.toString(value));
        se.revalidate();
        se.repaint();
    }

    private void paintAsEmpty(StackElement se) {
        se.elt.setText("");
        se.eltPanel.setOpaque(true);
        se.eltPanel.setBackground(EMPTY_CELL); // always WHITE for static stack
        se.revalidate();
        se.repaint();
    }

    // Function to update GUI when push button is pressed
    public void update_push(int num, Stack undo, int stackNumber, boolean isDynamic) {

        if (undo.nElts == undo.size) {
            undo.forgetEarlierChoice(); // Deleting earlier options - push or pop
        }

        undo.push(0); // 0 for push
        if (!isDynamic) {
            // Check if stack is full
            if (stack.nElts == stack.size) {
                // Stack full message
                textSetter.setText("Stack " + Integer.toString(stackNumber + 1)
                        + " is full! Cannot push more elements. Top = " + Integer.toString(stack.top));
                return;
            }

            rowElement[stack.size - stack.nElts].topPanel.remove(topLabel); // Remove topLabel from current position
            stack.push(num); // Push new element in actual stack
            rowElement[stack.size - stack.nElts].topPanel.add(topLabel); // topLabel points to new top

            // color the newly occupied cell
            paintAsOccupied(rowElement[stack.size - stack.top - 1], num);

            // Display message of successful push
            textSetter
                    .setText(Integer.toString(num) + " has been pushed on to Stack " + Integer.toString(stackNumber + 1)
                            + ". Top = " + Integer.toString(stack.top));
        } else {
            StackElement elt = new StackElement(num);
            // visuals for the new dynamic node
            elt.eltPanel.setOpaque(true);
            elt.eltPanel.setBackground(NODE_BLUE);
            elt.elt.setForeground(FG_WHITE);

            ((StackElement) dynamicStack.get(dynamicStack.size() - 1)).topPanel.remove(topLabel);
            elt.topPanel.add(topLabel);
            dynamicStack.add(elt);
            columnBox.add((StackElement) dynamicStack.get(dynamicStack.size() - 1), 1);

            textSetter
                    .setText(Integer.toString(num) + " has been pushed on to Stack " + Integer.toString(stackNumber + 1)
                            + ". Top = " + Integer.toString(dynamicStack.size() - 2));
        }
    }

    public void update_pop(Stack undo, int stackNumber, boolean isDynamic) {

        // Check if stack is empty
        if (!isDynamic && stack.top == -1) {
            textSetter.setText("Stack " + Integer.toString(stackNumber + 1) +
                    " is empty! Cannot pop any element. Top = " + Integer.toString(stack.top));
            return;
        }
        if (isDynamic && dynamicStack.size() == 1) {
            textSetter.setText("Stack " + Integer.toString(stackNumber + 1) +
                    " is empty! Cannot pop any element. Top = " + Integer.toString(dynamicStack.size() - 2));
            return;
        }

        if (undo.nElts == undo.size) {
            undo.forgetEarlierChoice(); // Deleting earlier options - push or pop
        }
        if (popStack.nElts == popStack.size) {
            popStack.forgetEarlierChoice(); // Deleting elements popped earlier from temporary stack
        }

        undo.push(1);// 1 for pop

        if (!isDynamic) {
            // make the current top cell empty (WHITE)
            paintAsEmpty(rowElement[stack.size - stack.top - 1]);

            rowElement[stack.size - stack.nElts].topPanel.remove(topLabel); // Remove topLabel from current position
            int num = stack.pop(); // Popping element from actual stack (updates top & nElts)
            popStack.push(num); // Storing recently popped element for undo purpose
            rowElement[stack.size - stack.nElts].topPanel.add(topLabel); // Updating topLabel to point to new top

            // Display message of successful pop
            textSetter
                    .setText(Integer.toString(num) + " has been popped from Stack " + Integer.toString(stackNumber + 1)
                            + ". Top = " + Integer.toString(stack.top));
        } else {
            StackElement elt = (StackElement) dynamicStack.get(dynamicStack.size() - 1);
            elt.topPanel.remove(topLabel);
            popStack.push(elt.val);
            dynamicStack.remove(dynamicStack.size() - 1);
            columnBox.remove(1);

            textSetter.setText(
                    Integer.toString(elt.val) + " has been popped from Stack " + Integer.toString(stackNumber + 1)
                            + ". Top = " + Integer.toString(dynamicStack.size() - 2));

            elt = (StackElement) dynamicStack.get(dynamicStack.size() - 1);
            elt.topPanel.add(topLabel);
        }
    }

    public void undoPush(int stackNumber, boolean isDynamic) {
        if (!isDynamic) {
            // Remove recently pushed element visuals (back to WHITE)
            paintAsEmpty(rowElement[stack.size - stack.top - 1]);
            rowElement[stack.size - stack.nElts].topPanel.remove(topLabel); // Remove topLabel from current top
            stack.pop(); // Remove element from actual stack
            rowElement[stack.size - stack.nElts].topPanel.add(topLabel); // topLabel points to new top
            textSetter.setText("Undo on Stack " + Integer.toString(stackNumber + 1)
                    + " successful. Top = " + Integer.toString(stack.top));
        } else {
            StackElement elt = (StackElement) dynamicStack.get(dynamicStack.size() - 1);
            elt.topPanel.remove(topLabel);
            dynamicStack.remove(dynamicStack.size() - 1);
            columnBox.remove(1);

            textSetter.setText("Undo on Stack " + Integer.toString(stackNumber + 1)
                    + " successful. Top = " + Integer.toString(dynamicStack.size() - 2));

            elt = (StackElement) dynamicStack.get(dynamicStack.size() - 1);
            elt.topPanel.add(topLabel);
        }
    }

    public void undoPop(int stackNumber, boolean isDynamic) {
        if (!isDynamic) {
            rowElement[stack.size - stack.nElts].topPanel.remove(topLabel); // Remove topLabel from current top
            int restored = popStack.pop();
            stack.push(restored); // Push back recently popped element
            rowElement[stack.size - stack.nElts].topPanel.add(topLabel); // topLabel points to new top
            // paint as occupied again
            paintAsOccupied(rowElement[stack.size - stack.top - 1], stack.getVal(stack.top));
            textSetter.setText("Undo on Stack " + Integer.toString(stackNumber + 1)
                    + " successful. Top = " + Integer.toString(stack.top));
        } else {
            StackElement elt = new StackElement(popStack.pop());
            elt.eltPanel.setOpaque(true);
            elt.eltPanel.setBackground(NODE_BLUE);
            elt.elt.setForeground(FG_WHITE);

            ((StackElement) dynamicStack.get(dynamicStack.size() - 1)).topPanel.remove(topLabel);
            elt.topPanel.add(topLabel);
            dynamicStack.add(elt);
            columnBox.add((StackElement) dynamicStack.get(dynamicStack.size() - 1), 1);

            textSetter.setText("Undo on Stack " + Integer.toString(stackNumber + 1)
                    + " successful. Top = " + Integer.toString(dynamicStack.size() - 2));
        }
    }

    public void random(Stack undo, int stackNumber, boolean isDynamic) throws InterruptedException {
        int choice;
        Random getRandom = new Random(); // object which gets random numbers
        if (!isDynamic) {
            if (stack.nElts == stack.size) {
                choice = 1; // if stack full, then pop
            } else if (stack.nElts == 0) {
                choice = 0; // if stack empty, then push
            } else {
                int r = getRandom.nextInt(4 + this.stack.nElts); // random number
                choice = (r > this.stack.nElts) ? 0 : 1;
            }
        } else {
            if (dynamicStack.size() == 1) {
                choice = 0;
            } else {
                int r = getRandom.nextInt(4 + dynamicStack.size()); // random number
                choice = (r > dynamicStack.size()) ? 0 : 1;
            }
        }

        if (choice == 1) {
            update_pop(undo, stackNumber, isDynamic);
        } else {
            update_push(getRandom.nextInt(100), undo, stackNumber, isDynamic);
        }
    }

    public void reset(int stackNumber, boolean isDynamic) {

        if (!isDynamic) {
            rowElement[stack.size - stack.nElts].topPanel.remove(topLabel); // remove topLabel from current position

            // Clear actual stack
            stack.nElts = 0;
            stack.top = -1;

            // repaint all cells as EMPTY (WHITE)
            for (int i = 0; i < stack.size; i++) {
                paintAsEmpty(rowElement[i]);
                rowElement[i].elt.setForeground(Color.BLACK); // harmless; ensures legible text if any
            }

            rowElement[stack.size - stack.nElts].topPanel.add(topLabel); // topLabel points to -1
            textSetter.setText("Stack " + Integer.toString(stackNumber + 1)
                    + " has been reset. Top = " + Integer.toString(stack.top));
        } else {
            ((StackElement) dynamicStack.get(dynamicStack.size() - 1)).topPanel.remove(topLabel);
            dynamicStack.clear();

            StackElement elt = new StackElement();
            elt.topPanel.add(topLabel);
            elt.eltPanel.setBorder(null);
            elt.eltPanel.setOpaque(false);
            dynamicStack.add(elt);
            columnBox.add((StackElement) dynamicStack.get(0));

            columnBox.removeAll();
            columnBox.add(Box.createRigidArea(new Dimension(0, 60)));

            textSetter.setText("Stack " + Integer.toString(stackNumber + 1)
                    + " has been reset. Top = " + Integer.toString(dynamicStack.size() - 2));
        }
    }
}
