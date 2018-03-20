package edu.utep.cs.cs4330.sudoku;

import java.util.ArrayList;

import edu.utep.cs.cs4330.sudoku.model.Board;
import edu.utep.cs.cs4330.sudoku.model.Square;

/**
 * Created by jdozal on 3/16/18.
 */

public interface Strategy {
    ArrayList<Square> solve(Board board);

}
