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
public class Queue {
    public int front;
    public int rear;
    private int arr[];
    public int size, nElts;
    
    public Queue(int sz){
        front=0; rear=-1;
        size=sz;
        arr=new int[size];
    }
    
    public void insert(int elt){
        arr[++rear]=elt;
        nElts++;
    }
    
    public int del(){
        nElts--;
        return arr[front++];
    }
    
    public int getVal(int i){
        return arr[i];
    }
};
