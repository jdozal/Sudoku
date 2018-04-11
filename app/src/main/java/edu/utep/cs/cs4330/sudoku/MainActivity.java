package edu.utep.cs.cs4330.sudoku;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.ViewGroup;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.utep.cs.cs4330.sudoku.model.Board;

/**
 * HW1 template for developing an app to play simple Sudoku games.
 * You need to write code for three callback methods:
 * newClicked(), numberClicked(int) and squareSelected(int,int).
 * Feel free to improved the given UI or design your own.
 *
 * <p>
 *  This template uses Java 8 notations. Enable Java 8 for your project
 *  by adding the following two lines to build.gradle (Module: app).
 * </p>
 *
 * <pre>
 *  compileOptions {
 *  sourceCompatibility JavaVersion.VERSION_1_8
 *  targetCompatibility JavaVersion.VERSION_1_8
 *  }
 * </pre>
 *
 * @author Yoonsik Cheon
 */
public class MainActivity extends AppCompatActivity {

    public static final java.util.UUID MY_UUID = java.util.UUID.fromString("DEADBEEF-0000-0000-0000-000000000000");

    private BluetoothAdapter BA;

    private  BluetoothSocket mmSocket;

    private List<BluetoothDevice> listDevices;

    private BluetoothDevice selectecDevice;

    private int temp;

    private ArrayList<String> nameDevices;

    private Board board;

    private BoardView boardView;

    /** All the number buttons. */
    private List<View> numberButtons;
    private static final int[] numberIds = new int[] {
            R.id.n0, R.id.n1, R.id.n2, R.id.n3, R.id.n4,
            R.id.n5, R.id.n6, R.id.n7, R.id.n8, R.id.n9
    };

    /** Width of number buttons automatically calculated from the screen size. */
    private static int buttonWidth;

    private int squareX;
    private int squareY;

    /** Size and level of current game, controlled in the SettingsActivity */
    private int size;
    private Board.Level level;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        // enter the key from your xml and the default value
        String set_level  = sharedPreferences.getString("level_list", "0");
        String set_size  = sharedPreferences.getString("size_list", "9");
        size = Integer.parseInt(set_size);
        setLevel(Integer.parseInt(set_level));
        board = new Board(size, level, new StrategySudoku());
        boardView = findViewById(R.id.boardView);
        boardView.setBoard(board);
        boardView.addSelectionListener(this::squareSelected);
        listDevices = new ArrayList<BluetoothDevice>();
        nameDevices = new ArrayList<String>();
        selectecDevice = null;

        BA = BluetoothAdapter.getDefaultAdapter();

        for(BluetoothDevice b : BA.getBondedDevices()){
            listDevices.add(b);
            nameDevices.add(b.getName());

        }
        Log.d("devices", listDevices.toString());
        numberButtons = new ArrayList<>(numberIds.length);
        for (int i = 0; i < numberIds.length; i++) {
            final int number = i; // 0 for delete button
            View button = findViewById(numberIds[i]);
            button.setOnClickListener(e -> numberClicked(number));
            numberButtons.add(button);
            setButtonWidth(button);
        }
        enableButtons();
    }

    public void on(View v){
        if (!BA.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on",Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
        }
    }


    public void ConnectThread(BluetoothDevice device) {
        BluetoothSocket tmp = null;
        try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        }
        catch (IOException e) {
            Log.e("error_socket", "Socket: " + tmp.toString() + " create() failed", e);
        }

        mmSocket = tmp;
        Log.d("socket", mmSocket.toString());
    }

    public void off(View v){
        BA.disable();
        Toast.makeText(getApplicationContext(), "Turned off" ,Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    /**
     * Sets level based on user size input in the Settings Activity
     * @param lvl current level
     */
    public void setLevel(int lvl){
        // options for 9x9 board
        if(size == 9){
            switch (lvl) {
                case 0:  level = Board.Level.EASY_9;
                    break;
                case 1:  level = Board.Level.MEDIUM_9;
                    break;
                case 2:  level = Board.Level.HARD_9;
                    break;
                default:
                    break;
            }
        }
        // options for 4x4 board
        else{
            switch (lvl) {
                case 0:  level = Board.Level.EASY_4;
                    break;
                case 1:  level = Board.Level.MEDIUM_4;
                    break;
                case 2:  level = Board.Level.HARD_4;
                    break;
                default:
                    break;
            }
        }

    }

    /**
     * Method to solve current board
     */
    public void solve(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Are you sure you want to solve the current Sudoku?");
        alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                board.solveBoard();
                boardView.postInvalidate();
                board.printBoard();
            }
        });
        alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }


    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.solve:
                solve();
                return true;
            case R.id.check:
                checkSolution();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void checkSolution(){
        toastCenter(board.check());

    }
    /** Callback to be invoked when the new button is tapped. */
    public void newClicked(View view) {
        //Restart Activity

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Are you sure you want to start a new game?");
        alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /** Callback to be invoked when a number button is tapped.
     *
     * @param n Number represented by the tapped button
     *          or 0 for the delete button.
     */
    public void numberClicked(int n) {
        String message = "";
        if(n == 0){
            message = board.removeNumber(squareX,squareY);
        }
        else{
            message = board.addNumber(squareX,squareY,n);
        }
        if(message!=null) toast(message);
        boardView.postInvalidate();
        if(board.isWin()){
            toastCenter("YOU WIN");
        }
    }

    /**
     * Callback to be invoked when a square is selected in the board view.
     *
     * @param x 0-based column index of the selected square.
     * @param x 0-based row index of the selected square.
     */
    private void squareSelected(int x, int y) {
        squareY = x;
        squareX = y;
        enableButtons();
        disableButtons();
        boardView.postInvalidate();
        //toast(String.format("Square selected: (%d, %d)", x, y));
    }

    /**
     * Enable all buttons except for the las 5 numbers in the case of a 4x4 grid
     */
    private void enableButtons(){

        // enabling all numbers
        for (int i  = 0; i < numberIds.length; i++){
            View button = findViewById(numberIds[i]);
            button.setEnabled(true);
        }

        // disabling 5,6,7,8,9 for a 4x4 grid
        if(size == 4){
            for(int j = 5; j < numberIds.length; j++) {
                View button = findViewById(numberIds[j]);
                button.setEnabled(false);
            }
        }
    }

    /**
     * Disables buttons for invalid numbers in current selection
     */
    private void disableButtons(){
        // disable all buttons if current selection is prefilled value
        if(board.getSquare(squareX,squareY).prefilled){
            for(int i = 0; i < numberIds.length; i++) {
                View button = findViewById(numberIds[i]);
                button.setEnabled(false);
            }
            return;
        }
        ArrayList<Integer> invalid = board.getInvalidNums(squareX,squareY);
        for(int curr: invalid){
            View button = findViewById(numberIds[curr]);
            button.setEnabled(false);
        }
    }
    /** Show a toast message. */
    private void toast(String msg) {
        Toast toast=Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP,0,130);
        toast.show();


    }

    /** Show a toast message (Center of screen). */
    private void toastCenter(String msg) {
        Toast toast=Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();


    }
    @Override
    public void onBackPressed() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Are you sure you want a new game?");
        alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /** Set the width of the given button calculated from the screen size. */
    private void setButtonWidth(View view) {
        if (buttonWidth == 0) {
            final int distance = 2;
            int screen = getResources().getDisplayMetrics().widthPixels;
            buttonWidth = (screen - ((9 + 1) * distance)) / 9; // 9 (1-9)  buttons in a row
        }
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = buttonWidth;
        view.setLayoutParams(params);
    }

    public void settingsClicked(View view) {
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }

    public void bluetoothClicked(View view) {

        on(view);
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Paired Devices");

        String[] arrDevices = nameDevices.toArray(new String[nameDevices.size()]);
        int checkedItem = 0;
        builder.setSingleChoiceItems(arrDevices, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                toastCenter(arrDevices[which]);
                temp = which;
            }
        });

        builder.setPositiveButton("CONNECT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectecDevice = listDevices.get(temp);
                Log.d("devices", selectecDevice.getAddress());
                ConnectThread(selectecDevice);
            }
        });
        builder.setNegativeButton("Cancel", null);

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();

    }
}
