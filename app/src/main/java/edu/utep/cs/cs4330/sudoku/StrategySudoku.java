package edu.utep.cs.cs4330.sudoku;

import java.util.ArrayList;

import edu.utep.cs.cs4330.sudoku.model.Board;
import edu.utep.cs.cs4330.sudoku.model.Square;

/**
 * Created by jdozal on 3/16/18.
 */

public class StrategySudoku implements Strategy {

    @Override
    public ArrayList<Square> solve(Board board) {
            for (Square sq: board.grid) {
                Square sqSol = new Square(sq.x,sq.y, sq.getValue());
                if(!sq.prefilled){
                    sqSol.prefilled = false;
                }
                sqSol.added = true;
                board.solutionGrid.add(sqSol);
            }
            return board.solutionGrid;
    }
}
