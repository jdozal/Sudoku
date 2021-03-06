package edu.utep.cs.cs4330.sudoku.model;

/**
 * Square object to store coordinates and values for each square in the board
 */


public class Square {
    public int x;
    public int y;
    private int value;
    public boolean prefilled;
    public boolean added;
    public boolean otherUser;
    public Square(int x, int y, int v) {
        this.x = x;
        this.y = y;
        prefilled = true;
        value = v;
        added = true;
        otherUser = false;
    }
    public int getValue(){ return value;}
    public void setValue(int v){ value = v;}


}

