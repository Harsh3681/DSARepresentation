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
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author DELL
 */
public class StackSetup extends JPanel {

    public boolean isDynamicStack;
    public int numberOfStacks;
    public JLabel stackMessage;
    public JPanel stackMessagePanel;
    public JButton createStackButton;
    public JCheckBox dynamicStackYes;
    public JCheckBox dynamicStackNo;
    public JComboBox stackNumberComboBox;

    public StackSetup() {

        super.setBackground(Color.BLACK);

        isDynamicStack = true;
        numberOfStacks = 1;

        JPanel stackPanel = new JPanel();
        stackPanel.setLayout(new BorderLayout());

        JLabel stackTitle = new JLabel("<HTML><U>DATA STRUCTURES : DEMONSTRATION OF THE STACK</U></HTML>");
        stackTitle.setFont(new Font("Baskerville Old Face", Font.PLAIN, 28));
        stackTitle.setForeground(Color.yellow);
        stackTitle.setPreferredSize(new Dimension(800, 80));
        JPanel stackTitlePanel = new JPanel();
        stackTitlePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        stackTitlePanel.setBackground(Color.darkGray);
        stackTitlePanel.add(stackTitle);

        stackMessage = new JLabel(">>>Please select the settings for the stack and press the create stack button");
        stackMessage.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 20));
        stackMessage.setForeground(Color.ORANGE);
        stackMessagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

        stackMessagePanel.setBackground(Color.BLACK);
        stackMessagePanel.add(stackMessage);

        Box topDisplay = Box.createVerticalBox();
        topDisplay.add(stackTitlePanel);
        topDisplay.add(stackMessagePanel);

        stackPanel.add(topDisplay, BorderLayout.NORTH);

        Box verticalBox = Box.createVerticalBox();
        verticalBox.add(Box.createRigidArea(new Dimension(0, 100)));
        verticalBox.add(Box.createRigidArea(new Dimension(0, 10)));

        Box horizontalBox = Box.createHorizontalBox();

        JLabel dynamicStackLabel = new JLabel("Dynamic Stack :  ");
        dynamicStackLabel.setFont(new Font("Baskerville Old Face", Font.PLAIN, 28));
        dynamicStackLabel.setForeground(Color.WHITE);

        dynamicStackYes = new JCheckBox("Yes");
        dynamicStackNo = new JCheckBox("No");

        ButtonGroup dynamicStackBtnGroup = new ButtonGroup();
        dynamicStackBtnGroup.add(dynamicStackYes);
        dynamicStackBtnGroup.add(dynamicStackNo);

        dynamicStackYes.setSelected(true);
        dynamicStackNo.setSelected(false);

        horizontalBox.add(dynamicStackLabel);
        horizontalBox.add(dynamicStackYes);
        horizontalBox.add(dynamicStackNo);

        verticalBox.add(horizontalBox);

        verticalBox.add(Box.createRigidArea(new Dimension(0, 10)));

        createStackButton = new JButton("Create Stack");
        createStackButton.setPreferredSize(new Dimension(150, 40));
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(createStackButton);

        verticalBox.add(buttonPanel);
        verticalBox.add(Box.createRigidArea(new Dimension(0, 100)));

        JPanel alignPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 400, 0));
        alignPanel.add(verticalBox);

        stackPanel.add(alignPanel);

        super.add(stackPanel);

    }

    public void reset() {

        isDynamicStack = true;
        numberOfStacks = 1;

        dynamicStackYes.setSelected(true);
        dynamicStackNo.setSelected(false);
    }

}
