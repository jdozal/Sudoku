package edu.utep.cs.cs4330.sudoku;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.ArrayList;
import java.util.List;

import edu.utep.cs.cs4330.sudoku.model.Board;
import edu.utep.cs.cs4330.sudoku.model.Square;

/**
 * A special view class to display a Sudoku board modeled by the
 * {@link edu.utep.cs.cs4330.sudoku.model.Board} class. You need to write code for
 * the <code>onDraw()</code> method.
 *
 * @see edu.utep.cs.cs4330.sudoku.model.Board
 * @author cheon
 */
public class BoardView extends View {

    /** To notify a square selection. */
    public interface SelectionListener {

        /** Called when a square of the board is selected by tapping.
         * @param x 0-based column index of the selected square.
         * @param y 0-based row index of the selected square. */
        void onSelection(int x, int y);
    }

    /** Listeners to be notified when a square is selected. */
    private final List<SelectionListener> listeners = new ArrayList<>();

    /** Number of squares in rows and columns.*/
    private int boardSize = 9;

    /** Board to be displayed by this view. */
    private Board board;

    /** Width and height of each square. This is automatically calculated
     * this view's dimension is changed. */
    private float squareSize;

    /** Translation of screen coordinates to display the grid at the center. */
    private float transX;

    /** Translation of screen coordinates to display the grid at the center. */
    private float transY;

    private int x = -1;
    private int y = 0;

    /** Paint to draw the background of the grid. */
    private final Paint boardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    {
        int boardColor = Color.rgb(201, 186, 145);
        boardPaint.setColor(boardColor);
        boardPaint.setAlpha(80); // semi transparent
    }

    private final Paint selectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    {
        int selectColor = Color.rgb(0, 0, 100);
        selectPaint.setColor(selectColor);
        selectPaint.setAlpha(80); // semi transparent
    }

    private final Paint winPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    {
        int winColor = Color.rgb(0, 186, 0);
        winPaint.setColor(winColor);
        winPaint.setAlpha(80); // semi transparent
    }

    private final Paint paint = new Paint();




    /** Create a new board view to be run in the given context. */
    public BoardView(Context context) { //@cons
        this(context, null);
    }

    /** Create a new board view by inflating it from XML. */
    public BoardView(Context context, AttributeSet attrs) { //@cons
        this(context, attrs, 0);
    }

    /** Create a new instance by inflating it from XML and apply a class-specific base
     * style from a theme attribute. */
    public BoardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setSaveEnabled(true);
        getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
    }

    /** Set the board to be displayed by this view. */
    public void setBoard(Board board) {
        this.board = board;
        boardSize = board.size;
    }

    /** Draw a 2-D graphics representation of the associated board. */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.translate(transX, transY);
        if (board != null) {
            drawGrid(canvas);
            drawSquares(canvas);
            drawSelection(canvas);
        }
        canvas.translate(-transX, -transY);
    }

    private void drawSelection(Canvas canvas) {
        float squareSize = (float)maxCoord() /boardSize;
        if(x != -1)
            canvas.drawRect(x*squareSize,y*squareSize,x*squareSize+squareSize,y*squareSize+squareSize,selectPaint);

    }

    /** Draw horizontal and vertical grid lines. */
    private void drawGrid(Canvas canvas) {
        final float maxCoord = maxCoord();
        int sqrt = (int)Math.sqrt(board.size);
        if(board.isWin()){
            canvas.drawRect(0, 0, maxCoord, maxCoord, winPaint);
        }
        else {
            canvas.drawRect(0, 0, maxCoord, maxCoord, boardPaint);
        }
        Paint grayPaint = new Paint();
        grayPaint.setColor(Color.GRAY);

        Paint blackPaint = new Paint();
        blackPaint.setColor(Color.BLACK);
        blackPaint.setStrokeWidth(5);

        //Top Line
        canvas.drawLine(0,0,maxCoord,0, blackPaint);
        //Bottom Line
        canvas.drawLine(0,maxCoord-5,maxCoord,maxCoord, blackPaint);
        //Left Line
        canvas.drawLine(0, maxCoord, 0,0, blackPaint);
        //Right Line
        canvas.drawLine(maxCoord,0,maxCoord,maxCoord, blackPaint);

        //Vertical bold lines
        canvas.drawLine(maxCoord/sqrt,0,maxCoord/sqrt,maxCoord,blackPaint);
        canvas.drawLine((maxCoord/sqrt)*2, 0, (maxCoord/sqrt)*2,maxCoord,blackPaint);
        //Horizontal bold lines
        canvas.drawLine(0,maxCoord/sqrt,maxCoord,maxCoord/sqrt,blackPaint);
        canvas.drawLine(0,(maxCoord/sqrt)*2,maxCoord,(maxCoord/sqrt)*2, blackPaint);

        //Vertical
        for (int i = 1; i < boardSize; i++){
            canvas.drawLine((maxCoord/boardSize)*i,0, (maxCoord/boardSize)*i, maxCoord, grayPaint);
        }

        //Horizontal
        for(int i = 1; i < boardSize; i++){
            canvas.drawLine(0,(maxCoord/boardSize)*i,maxCoord,(maxCoord/boardSize)*i,grayPaint);
        }

    }



    /** Draw all the squares (numbers) of the associated board. */
    private void drawSquares(Canvas canvas) {
        // WRITE YOUR CODE HERE ...
        //
        Paint prefilledColor = new Paint();
        Paint textColor = new Paint();
        Paint userColor = new Paint();
        textColor.setColor(Color.MAGENTA);
        textColor.setTextSize(getTextSize());
        userColor.setColor(Color.BLUE);
        userColor.setTextSize(getTextSize());
        prefilledColor.setColor(Color.DKGRAY);
        prefilledColor.setTextSize(getTextSize());
        ArrayList<Integer> currValid= new ArrayList<Integer>();
        int cornerX, cornerY;

        int gridSpacing = getHeight()/ board.size;
        int boardSize = board.size * gridSpacing;

        int startX = (getWidth() - boardSize)/(getWidth()/2);
        int startY = (getHeight() - boardSize)/(getHeight()/2);

        for(int i = 0;i< board.size; i++){
            for(int j = 0; j<board.size; j++){
                Square sqr = board.getSquare(i,j);
                if(sqr.prefilled){
                    canvas.drawText(Integer.toString(sqr.getValue()),(startY + j*gridSpacing)+20,(startX + (i+1)*gridSpacing)-15,prefilledColor);
                } else if(sqr.otherUser){
                    canvas.drawText(Integer.toString(sqr.getValue()),(startY + j*gridSpacing)+20,(startX + (i+1)*gridSpacing)-15,userColor);
                } else if(sqr.added){
                    canvas.drawText(Integer.toString(sqr.getValue()),(startY + j*gridSpacing)+20,(startX + (i+1)*gridSpacing)-15,textColor);
                } else{
                    cornerX = (startY + j*gridSpacing)+5;
                    cornerY = (startX + (i+1)*gridSpacing)-8;
                    currValid = board.getValidNums(i,j);
                    //drawValid(canvas, currValid, cornerX,cornerY);
                }
            }
        }


    }


    private void drawValid(Canvas canvas, ArrayList<Integer> currValid, int cornerX, int cornerY) {
        Paint validPaint = new Paint();
        validPaint.setColor(Color.GRAY);
        int sizeNum = getTextSizeSmall();
        validPaint.setTextSize(sizeNum);
        for(int curr:currValid){
            canvas.drawText(Integer.toString(curr),cornerX,cornerY,validPaint);
            cornerX+= sizeNum;
        }
    }


    /** Overridden here to detect tapping on the board and
     * to notify the selected square if exists. */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                int xy = locateSquare(event.getX(), event.getY());
                if (xy >= 0) {
                    // xy encoded as: x * 100 + y
                    notifySelection(xy / 100, xy % 100);
                    x = xy/100;
                    y = xy%100;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }

    private int getTextSizeSmall() {
        if(board.size == 9){
            return 10;
        }
        return 30;
    }

    private int getTextSize() {
        if(board.size == 9){
            return 20;
        }
        return 100;
    }
    /**
     * Given screen coordinates, locate the corresponding square of the board, or
     * -1 if there is no corresponding square in the board.
     * The result is encoded as <code>x*100 + y</code>, where x and y are 0-based
     * column/row indexes of the corresponding square.
     */
    private int locateSquare(float x, float y) {
        x -= transX;
        y -= transY;
        if (x <= maxCoord() &&  y <= maxCoord()) {
            final float squareSize = lineGap();
            int ix = (int) (x / squareSize);
            int iy = (int) (y / squareSize);
            return ix * 100 + iy;
        }
        return -1;
    }

    /** To obtain the dimension of this view. */
    private final ViewTreeObserver.OnGlobalLayoutListener layoutListener
            =  new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            squareSize = lineGap();
            float width = Math.min(getMeasuredWidth(), getMeasuredHeight());
            transX = (getMeasuredWidth() - width) / 2f;
            transY = (getMeasuredHeight() - width) / 2f;
        }
    };

    /** Return the distance between two consecutive horizontal/vertical lines. */
    protected float lineGap() {
        return Math.min(getMeasuredWidth(), getMeasuredHeight()) / (float) boardSize;
    }

    /** Return the number of horizontal/vertical lines. */
    private int numOfLines() { //@helper
        return boardSize + 1;
    }

    /** Return the maximum screen coordinate. */
    protected float maxCoord() { //@helper
        return lineGap() * (numOfLines() - 1);
    }

    /** Register the given listener. */
    public void addSelectionListener(SelectionListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /** Unregister the given listener. */
    public void removeSelectionListener(SelectionListener listener) {
        listeners.remove(listener);
    }

    /** Notify a square selection to all registered listeners.
     *
     * @param x 0-based column index of the selected square
     * @param y 0-based row index of the selected square
     */
    private void notifySelection(int x, int y) {
        for (SelectionListener listener: listeners) {
            listener.onSelection(x, y);
        }
    }

}
