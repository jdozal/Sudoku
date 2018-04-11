package edu.utep.cs.cs4330.sudoku.model;

import android.util.Log;
import java.util.ArrayList;
import java.util.Random;
import edu.utep.cs.cs4330.sudoku.Strategy;
import edu.utep.cs.cs4330.sudoku.StrategySudoku;

/**
 * Board object to store list of squares, size and level of current game. Implements adding and removing methods.
 */
public class Board {
	public int size;
	public ArrayList<Square> grid;
	public ArrayList<Square> solutionGrid;
	Level level;
    Strategy strategy;

    public enum Level {
        EASY_9, MEDIUM_9, HARD_9, EASY_4, MEDIUM_4, HARD_4;
    }
	public Board(int s, Level l, final StrategySudoku strategy) {
		grid = new ArrayList<Square>();
		solutionGrid = new ArrayList<Square>();
		this.size = s;
		this.level = l;
		this.strategy = strategy;
		fillGrid();
		printBoard();
		setLevel();
	}

    /**
     * Gets square with coordinates
     * @param x x coordinate
     * @param y y coordinate
     * @return If found, returns Square in (x,y)
     */
	public Square getSquareSol(int x, int y){
        for (Square square : solutionGrid) {
            if (square.x == x && square.y == y)
                return square;
        }
        return null;
    }
	public Square getSquare(int x, int y) {
		for (Square square : grid) {
			if (square.x == x && square.y == y)
				return square;
		}
		System.out.printf("Square with coordinates x = %d, y = %d not found.", x, y);
		return null;
	}

    /**
     * Fills grid with valid sudoku game
     */
	void fillGrid() {
		int initialVal = 0;
		// setting basic sudoku
		for (int i = 0; i < size; i++) {
			int sqrt = (int) Math.sqrt(size);
			initialVal = (i % sqrt == 0) ? (i / sqrt) : initialVal + sqrt;
			for (int j = 0; j < size; j++) {
				Square sq = new Square(i, j, (initialVal + j) % size + 1);
                sq.prefilled = true;
				grid.add(sq);
			}
		}

		// randomizing columns and rows to create unique games in each session
		for (int i = 0; i <= size*3; i++) {
			randomizeColumn();
			randomizeRow();
		}


	}

    /**
     * Implementation of strategy interface
     */
	public void solveBoard(){
	    grid = strategy.solve(this);
    }

    /**
     * Distinguishing from added coordinates and prefilled coordinates
     */
	void setSolution(){
        for (Square sq: grid) {
            Square sqSol = new Square(sq.x,sq.y, sq.getValue());
            if(!sq.prefilled){
                sqSol.prefilled = false;
            }
            sqSol.added = true;
            solutionGrid.add(sqSol);
        }
    }

    void randomizeColumn() {
		Random rand = new Random();
		int sqrt = (int) Math.sqrt(size);
		int group = rand.nextInt(sqrt) * sqrt;
		int col1 = group + rand.nextInt(sqrt);
		int col2 = group + rand.nextInt(sqrt);
		while (col1 == col2)
			col2 = group + rand.nextInt(sqrt);
		// System.out.println(group);
		// System.out.printf("col1= %d, col2=%d\n\n", col1,col2);

		for (int i = 0; i < size; i++) {
			Square sq1 = getSquare(i, col1);
			Square sq2 = getSquare(i, col2);

			int temp = sq1.getValue();
			sq1.setValue(sq2.getValue());
			sq2.setValue(temp);
		}

	}

	void randomizeRow() {
		Random rand = new Random();
		int sqrt = (int) Math.sqrt(size);
		int group = rand.nextInt(sqrt) * sqrt;
		int row1 = group + rand.nextInt(sqrt);
		int row2 = group + rand.nextInt(sqrt);
		while (row1 == row2)
			row2 = group + rand.nextInt(sqrt);

		for (int i = 0; i < size; i++) {
			Square sq1 = getSquare(row1, i);
			Square sq2 = getSquare(row2, i);

			int temp = sq1.getValue();
			sq1.setValue(sq2.getValue());
			sq2.setValue(temp);
		}

	}

    /**
     * Removes squares from current valid game based on difficulty level
     * @param number number of squares to be removed
     */
	void removeSquares(int number) {
		Random rand = new Random();
		int x = 0;
		int y = 0;
		for (int i = 0; i < number; i++) {
			x = rand.nextInt(size);
			y = rand.nextInt(size);
			while (!getSquare(x, y).prefilled) {
				x = rand.nextInt(size);
				y = rand.nextInt(size);
				// System.out.printf("x=%d, y=%d, i=%d\n", x,y,i);
			}
			getSquare(x, y).prefilled = false;
			getSquare(x, y).added = false;
		}
		setSolution();
	}

    /**
     * Checks if current board is a winning game
     * @return true if complete game
     */
	public boolean isWin() {
		for (Square square : grid) {
			if (!square.added)
				return false;
		}
		return true;
	}

    /**
     * Method to remove squares from board
     * @param x x coordinate
     * @param y y coordinate
     * @return message to be displayed in app
     */
	public String removeNumber(int x, int y) {
		Square sqr = getSquare(x, y);
		if(sqr.prefilled){
		    return "PREFILLED VALUES CAN'T BE REMOVED";
        }
		sqr.added = false;
		return "NUMBER REMOVED";
	}

    /**
     * Method to add numbers to board
     * @param x x coordinate
     * @param y y coordinate
     * @param v value to be added
     * @return warning message if is conflicting
     */
	public String addNumber(int x, int y, int v) {
		Square sqr;

        // check pre filled
        if(getSquare(x, y).prefilled){
            System.out.println("PREFILLED");
            return "PREFILLED";
        }

        // check row
        if (!inRow(x, v)) {
            System.out.println("SAME ROW");
            return "SAME ROW";
        }

        // check column
        if (!inColumn(y, v)) {
            System.out.println("SAME COLUMN");
            return "SAME COLUMN";
        }

        // check square
        if (!inSquare(x, y, v)) {
            System.out.println("SAME SQUARE");
            return "SAME SQUARE";
        }
        sqr = getSquare(x, y);
        sqr.added = true;
        sqr.setValue(v);
        printBoard();
        return null;

	}

    /**
     * Check if value to be added is present in the same row
     */
	public boolean inRow(int x, int v) {
		Square sqr;
		for (int col = 0; col < size; col++) {
			sqr = getSquare(x, col);
			if (sqr.getValue() == v && sqr.added) {
				return false;
			}
		}
		return true;
	}

    /**
     * Check if value to be added is present in the same column
     */
	public boolean inColumn(int y, int v) {
		Square sqr;
		for (int row = 0; row < size; row++) {
			sqr = getSquare(row, y);
			if (sqr.getValue() == v && sqr.added) {
				return false;
			}
		}
		return true;
	}

    /**
     * Check if value to be added is present in the same 3x3 or 2x2 square
     */
	public boolean inSquare(int x, int y, int v){
		int sqrt = (int) Math.sqrt(size);
		int row = (int) (Math.floor((y / sqrt))) * sqrt;
        int col = (int) (Math.floor((x / sqrt))) * sqrt;
        Square sqr;
        for (int i = row; i < row + sqrt; i++) {
            for (int j = col; j < col + sqrt; j++) {
            	sqr = getSquare(j, i);
                if (sqr.getValue() == v && sqr.added)
                    return false;
            }
        }
        return true;
	}

    /**
     * Get list of numbers possible for that selection
     * @param x coordinate x
     * @param y coordinate y
     * @return list of invalid numbers
     */
    public ArrayList<Integer> getValidNums(int x, int y){
        ArrayList<Integer> valid = new ArrayList<Integer>();
        Square sqX,sqY,sqr;
        Square curr =  getSquare(x,y);
        // Add all numbers possible to array
        for(int i = 1; i<= size; i++){
            valid.add(i);
        }

        // Check row and column
        for(int i  = 0; i < size; i++){
            sqX = getSquare(i,y);
            sqY = getSquare(x,i);
            if(sqX.added) valid.remove(new Integer(sqX.getValue()));
            if(sqY.added) valid.remove(new Integer(sqY.getValue()));

        }

        // check 3x3 or 2x2 square
        int sqrt = (int) Math.sqrt(size);
        int row = (int) (Math.floor((y / sqrt))) * sqrt;
        int col = (int) (Math.floor((x / sqrt))) * sqrt;
        for (int i = row; i < row + sqrt; i++) {
            for (int j = col; j < col + sqrt; j++) {
                sqr = getSquare(j, i);
                if(sqr.added) valid.remove(new Integer(sqr.getValue()));
            }
        }
        return valid;
    }

    /**
     * Get list of numbers not possible for that selection
     * @param x coordinate x
     * @param y coordinate y
     * @return list of invalid numbers
     */
	public ArrayList<Integer> getInvalidNums(int x, int y){
	    ArrayList<Integer> invalid = new ArrayList<Integer>();
        Square sqX,sqY,sqr;
        Square curr =  getSquare(x,y);

        // Check row and column
        for(int i  = 0; i < size; i++){
            sqX = getSquare(i,y);
            sqY = getSquare(x,i);
            if(sqX.added) invalid.add(sqX.getValue());
            if(sqY.added) invalid.add(sqY.getValue());

        }

        // check 3x3 or 2x2 square
        int sqrt = (int) Math.sqrt(size);
        int row = (int) (Math.floor((y / sqrt))) * sqrt;
        int col = (int) (Math.floor((x / sqrt))) * sqrt;
        for (int i = row; i < row + sqrt; i++) {
            for (int j = col; j < col + sqrt; j++) {
                sqr = getSquare(j, i);
                if(sqr.added) invalid.add(sqr.getValue());
            }
        }
        return invalid;
    }

    /**
     * Method to set level based on board size
     */
	void setLevel() {
		switch (level) {
		case EASY_9:
			removeSquares(30);
			break;
		case MEDIUM_9:
			removeSquares(40);
			break;
		case HARD_9:
			removeSquares(50);
			break;
		case EASY_4:
			removeSquares(6);
			break;
		case MEDIUM_4:
			removeSquares(9);
			break;
		case HARD_4:
			removeSquares(12);
			break;
		default:
			break;
		}
	}

    /**
     * Method to check if there is a possible solution with the values provided.
     * @return message corresponding to a solution found
     */
	public String check(){
        for (int i = 0; i < this.size; i++) {
            for (int j = 0; j < this.size; j++) {
                Square sq = getSquare(i, j);
                Square sqSol = getSquareSol(i,j);
                if(sq.added&&!sq.prefilled){
                    Log.d("test", String.valueOf(sq.getValue()));
                    Log.d("test2", String.valueOf(sqSol.getValue()));
                    if(sq.getValue() != sqSol.getValue())
                    return "Not a possible solution";
                }
            }
        }
        return "There is a possible solution";
    }

    // debugging purposes
	public void printBoard() {
		System.out.println("\n+===+===+===+===+===+===+===+===+===+");
		for (int i = 0; i < this.size; i++) {
			for (int j = 0; j < this.size; j++) {
				Square sq = getSquare(i, j);
				System.out.print("| " + (sq.added ? sq.getValue() : " ") + " ");
			}
			System.out.print("|");
			if (i % 3 == 2) {
				System.out.println("\n+===+===+===+===+===+===+===+===+===+");

			} else {
				System.out.println("\n+---+---+---+---+---+---+---+---+---+");
			}
		}
	}

}
