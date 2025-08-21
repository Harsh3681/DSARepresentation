package com.staqueue;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.NumberFormat;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.NumberFormatter;

/**
 *
 * @author DELL
 */
public class StackMenu extends JPanel {

    public JButton pushButton;
    public JButton popButton;
    public JButton resetButton;
    public JButton undoButton;
    public JButton randomButton;
    public JButton settingsBtn;
    public JFormattedTextField text;

    public StackMenu(int numberOfStacks) {

        super();

        // Border for Stack Menu Panel
        Border empty = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        Border titled = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
        titled = BorderFactory.createTitledBorder(titled, "Stack Menu", TitledBorder.CENTER,
                TitledBorder.DEFAULT_JUSTIFICATION,
                new Font("Serif", Font.PLAIN, 25), Color.WHITE);
        Border line = BorderFactory.createLineBorder(Color.DARK_GRAY);
        Border compound = BorderFactory.createCompoundBorder(titled, empty);

        super.setBorder(BorderFactory.createCompoundBorder(compound, line));

        Box menuBox = Box.createHorizontalBox();

        String numbers[] = new String[numberOfStacks];
        for (int i = 1; i <= numberOfStacks; i++)
            numbers[i - 1] = Integer.toString(i);

        JPanel pushPanel = new JPanel(new FlowLayout()); // Panel for textfield and push button

        // Text field to enter a number to push
        NumberFormat format = NumberFormat.getInstance();
        NumberFormatter formatter = new NumberFormatter(format);
        formatter.setValueClass(Integer.class);
        formatter.setMinimum(Integer.MIN_VALUE);
        formatter.setMaximum(Integer.MAX_VALUE);
        text = new JFormattedTextField(formatter); // TextField which only accepts valid integers
        text.setPreferredSize(new Dimension(80, 30));
        text.setToolTipText("enter a number to be pushed");

        // Push button properties
        pushButton = new JButton("Push");
        pushButton.setPreferredSize(new Dimension(70, 30));
        pushPanel.add(text);
        pushPanel.add(pushButton);
        pushPanel.setBackground(Color.lightGray);
        menuBox.add(pushPanel);

        // Pop button properties
        popButton = new JButton("Pop");
        popButton.setMinimumSize(new Dimension(150, 30));
        popButton.setPreferredSize(popButton.getMinimumSize());

        JPanel popPanel = new JPanel();
        popPanel.setLayout(new FlowLayout());
        popPanel.add(popButton);
        popPanel.setBackground(Color.lightGray);
        menuBox.add(popPanel);

        // Random button properties
        // randomButton = new JButton("Random");
        // randomButton.setMinimumSize(new Dimension(150, 30));
        // randomButton.setPreferredSize(randomButton.getMinimumSize());

        // JPanel randomPanel = new JPanel();
        // randomPanel.setLayout(new FlowLayout());
        // //randomPanel.add(randomButton);
        // randomPanel.setBackground(Color.lightGray);
        // menuBox.add(randomPanel);

        // Undo button properties
        undoButton = new JButton("Undo");
        undoButton.setMinimumSize(new Dimension(150, 30));
        undoButton.setPreferredSize(undoButton.getMinimumSize());

        JPanel undoPanel = new JPanel();
        undoPanel.setLayout(new FlowLayout());
        undoPanel.add(undoButton);
        undoPanel.setBackground(Color.lightGray);
        menuBox.add(undoPanel);

        // Reset button properties
        resetButton = new JButton("Reset");
        resetButton.setMinimumSize(new Dimension(150, 30));
        resetButton.setPreferredSize(resetButton.getMinimumSize());

        JPanel resetPanel = new JPanel();
        resetPanel.setLayout(new FlowLayout());
        resetPanel.add(resetButton);
        resetPanel.setBackground(Color.lightGray);
        menuBox.add(resetPanel);

        JPanel settingsPanel = new JPanel(new FlowLayout());
        settingsPanel.setBackground(Color.lightGray);
        settingsBtn = new JButton("Change Settings");
        settingsBtn.setMinimumSize(new Dimension(150, 30));
        settingsBtn.setPreferredSize(resetButton.getMinimumSize());
        settingsPanel.add(settingsBtn);
        menuBox.add(settingsPanel);

        super.add(menuBox);
    }
}
