package com.staqueue;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author DELL
 */
public class Stack {

    public int top;
    private int arr[];
    private int[][] array;
    public int size;
    public int nElts;

    public Stack(int sz) {
        top = -1;
        size = sz;
        arr = new int[size];
        nElts = 0;
    }

    public Stack(int row, int column) {
        top = -1;
        size = row;
        array = new int[row][column];
        nElts = 0;
    }

    public void push(int elt) {
        arr[++top] = elt;
        nElts++;
    }

    public void push(int[] elt) {
        array[++top] = elt;
        nElts++;
    }

    public int pop() {
        nElts--;
        return arr[top--];
    }

    public int[] popArr() {
        nElts--;
        return array[top--];
    }

    public int getVal(int i) {
        return arr[i];
    }

    public int[] getValue(int i) {
        return array[i];
    }

    public boolean isEmpty() {
        return nElts == 0;
    }

    // For undo option in Stack and Queue, to remove earlier choices when number of
    // operations > 3
    public void forgetEarlierChoice() {
        if (isEmpty()) {
            throw new IllegalStateException("Stack is empty");
        }
        for (int i = 0; i < this.top; i++) {
            this.arr[i] = this.arr[i + 1];
        }
        this.top--;
        this.nElts--;
    }

    public void forgetEarlyrChoice() {
        if (isEmpty()) {
            throw new IllegalStateException("Stack is empty");
        }
        for (int i = 0; i < this.top; i++) {
            this.array[i] = this.array[i + 1];
        }
        this.top--;
        this.nElts--;
    }
};
