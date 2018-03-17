package edu.utep.cs.cs4330.sudoku.model;

import java.util.Random;

/** An abstraction of Sudoku puzzle. */
public class Board {

    /** Size of this board (number of columns/rows). */
    public final int size;

    public int[][] grid;
    public boolean[][] prefilled;
    public boolean win;
    public boolean inRow, inCol, inSquare, isPrefilled;




    /** Create a new board of the given size. */
    public Board(int size) {
        this.size = size;

        initializeGrid();
        addRandomNumbers(30);
    }



    public void initializeGrid() {
        grid = new int[size][size];
        prefilled = new boolean[size][size];
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid.length; j++) {
                grid[i][j] = 0;
                prefilled[i][j] = false;
            }
        }

    }

    public void insertNumber(int x, int y, int n) {
            // check if valid number
            if (!checkNum(x, y, n)) {
                System.out.println("Not a valid number");
            }
            System.out.println("Valid number, inserting " + n);
            checkWin();
        printGrid();

    }

    private boolean checkNum(int x, int y, int n) {
        // check if is outside grid or if is not between 1 and 9
        if (n > 9 || x < 0 || y < 0 || x > 8 || y > 8) {
            return false;
        }

        // check column
        if (!checkColumn(x, n)) {
            System.out.println("Number already in column");
            inCol = true;
            return false;
        }

        // check row
        if (!checkRow(y, n)) {
            System.out.println("Number already in row");
            inRow = true;
            return false;
        }

        // check square
        if (!checkSquare(y, x, n)) {
            System.out.println("Number already in square");
            inSquare = true;
            return false;
        }

        // if number passes every test add to grid
        grid[y][x] = n;
        return true;
    }

    private boolean checkSquare(int x, int y, int n) {
        int row = (int) (Math.floor((x/3))) * 3;
        int col = (int) (Math.floor((y/3))) * 3;

        for(int i = row; i < row+3; i++){
            for(int j = col; j < col+3; j++){
                if(grid[i][j] == n)
                    return false;
            }
        }
        return true;
    }


    private boolean checkRow(int y, int n) {
        for (int row = 0; row < grid.length; row++) {
            if (grid[y][row] == n) {
                return false;
            }
        }
        return true;
    }

    private boolean checkColumn(int x, int n) {
        for (int col = 0; col < grid.length; col++) {
            if (grid[col][x] == n) {
                return false;
            }
        }
        return true;
    }

    private void checkWin() {
        int sum = 0;
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid.length; j++) {
                sum += grid[i][j];
                // if there are still empty squares
                if (grid[i][j] == 0) {
                    return;
                }
            }
        }
        // double check if sum of numbers is correct
        if (sum == 405) {
            win = true;
            System.out.println("YOU WIN!");
        }
    }

    private void insertPrefilled(int x, int y){
        prefilled[y][x] = true;
    }

    private void addRandomNumbers(int numbers) {
        Random rand = new Random();
        if (numbers == 0) {
            return;
        }
        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomX = rand.nextInt(10);
        int randomY = rand.nextInt(10);
        int randomN = rand.nextInt(9) + 1;
        if (!checkNum(randomX, randomY, randomN)) {
            addRandomNumbers(numbers);
        } else {
            insertPrefilled(randomX, randomY);
            addRandomNumbers(numbers-1);
        }
    }


    public void printGrid() {
        System.out.println("\n+===+===+===+===+===+===+===+===+===+");
        for (int i = 0; i < grid.length; i++) {
            System.out.print("|");
            for (int j = 0; j < grid.length; j++) {
                if (j % 3 == 2) {
                    System.out.print(" " + ((grid[j][i]==0) ? " ":grid[j][i]) + " !");
                } else {
                    System.out.print(" " + ((grid[j][i]==0) ? " ":grid[j][i]) + " |");
                }
            }
            if (i % 3 == 2) {
                System.out.println("\n+===+===+===+===+===+===+===+===+===+");

            } else {
                System.out.println("\n+---+---+---+---+---+---+---+---+---+");
            }
        }
    }

    public void insertZero(int x, int y){
        if(!prefilled[y][x]) {
            grid[y][x] = 0;
        } else{
            isPrefilled = true;
        }
    }

    /** Return the size of this board. */
    public int size() {
    	return size;
    }

    // WRITE YOUR CODE HERE ..
}
