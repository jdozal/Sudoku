package edu.utep.cs.cs4330.sudoku;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import java.io.IOException;
import java.net.Socket;
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

    private BluetoothAdapter BA;
    private BluetoothDevice selectedDevice;
    private List<BluetoothDevice> listDevices;

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private IntentFilter mIntentFilter;
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private WifiP2pDevice selectedWifi;

    private static final int PORT_NUMBER = 8000;
    private static final int CONNECTION_TIMEOUT = 5000; // in milliseconds

    private NetworkAdapter networkAdapter;
    private Socket socket;
    private String serverIP;
    private String ip;
    private String peer_port;
    
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
        selectedDevice = null;
        selectedWifi = null;
        peers = new ArrayList<WifiP2pDevice>();

        // Getting ip
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        Log.d("p2p-test", "myIP " + ip);

        // Getting paired BT devices
        BA = BluetoothAdapter.getDefaultAdapter();

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

    private void connectClicked(View view) {
        if (isConnected()) {
            toast("Already connected!");
        } else {
            connectToServer(serverIP, PORT_NUMBER);
        }
    }

    private boolean isConnected() {
        return socket != null;
    }

    private void connectToServer(String server, int port) {
        new Thread(() -> {
                try {
                    socket = new Socket(server, port);
                    //socket.connect(new InetSocketAddress(server, port), 3000); // in milliseconds
                    networkAdapter = new NetworkAdapter(socket);
                    networkAdapter.setMessageListener(new NetworkAdapter.MessageListener() {
                        public void messageReceived(NetworkAdapter.MessageType type, int x, int y, int z, int[] others) {
                            switch (type) {
                                case JOIN:
                                case JOIN_ACK:
                                    Log.d("p2p-test", "JOIN_ACK"); // x (response), y (size), others (board)

                                case NEW: Log.d("p2p-test", "NEW");      // x (size), others (board)
                                    Board newBoard = new Board(x,level,new StrategySudoku(), true);

                                    for(int i = 0; i < others.length-3; i+=4){
                                        newBoard.getSquare(others[i+1],others[i]).setValue(others[i+2]);
                                        if(others[i+3]==1)
                                            newBoard.getSquare(others[i+1],others[i]).prefilled = true;
                                        else
                                            newBoard.getSquare(others[i+1],others[i]).prefilled = false;
                                    }
                                    board = newBoard;
                                    boardView.setBoard(board);
                                    boardView.postInvalidate();
                                    enableButtons();
                                    networkAdapter.writeNewAck(true);
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            toast("Connection successful");
                                        }
                                    });
                                    break;
                                case NEW_ACK: Log.d("p2p-test", "NEW_ACK");  // x (response)
                                    break;
                                case FILL:
                                    Log.d("p2p-test", "FILL");     // x (x), y (y), z (number)
                                    if(z == 0) {
                                        board.removeNumber(y,x);
                                    } else {
                                        board.getSquare(y,x).setValue(z);
                                        board.getSquare(y, x).otherUser = true;
                                    }
                                    boardView.postInvalidate();
                                    break;
                                case FILL_ACK:
                                    Log.d("p2p-test", "FILL_ACK"); // x (x), y (y), z (number)
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            toast("Insertion successful");
                                        }
                                    });
                                    break;
                                case QUIT: Log.d("p2p-test", "QUIT");
                                    break;
                            }
                        }
                    });
                    networkAdapter.receiveMessagesAsync();
                   // socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

        }).start();
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
        networkAdapter.writeFill(squareY,squareX,n);
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

    public void wifiClicked(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        final TextView ip_text = new TextView(this);
        final TextView port_text = new TextView(this);
        final TextView peer_text = new TextView(this);
        final TextView device_text = new TextView(this);

        device_text.setTextColor(Color.BLACK);
        device_text.setText("Device: ");
        device_text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
        layout.addView(device_text);

        ip_text.setTextColor(Color.BLACK);
        ip_text.setText("IP:".concat(ip));
        layout.addView(ip_text);

        port_text.setTextColor(Color.BLACK);
        port_text.setText("Port: 8000");
        layout.addView(port_text);

        peer_text.setTextColor(Color.BLACK);
        peer_text.setText("Peer: ");
        peer_text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
        layout.addView(peer_text);
        // Add a TextView here for the "Title" label, as noted in the comments
        final EditText p_ip = new EditText(this);
        p_ip.setText(ip);
        layout.addView(p_ip); // Notice this is an add method

        // Add another TextView here for the "Description" label
        final EditText p_port = new EditText(this);
        p_port.setText("8000");
        layout.addView(p_port); // Another add method

        builder.setTitle("Pairing");
        builder.setView(layout);

        builder.setNeutralButton("SETTINGS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intentOpenWifiSettings = new Intent();
                intentOpenWifiSettings.setAction(Settings.ACTION_WIFI_SETTINGS);
                startActivity(intentOpenWifiSettings);
            }
        });
        // Set up the buttons
        builder.setPositiveButton("CONNECT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                serverIP = p_ip.getText().toString();
                Log.d("p2p-test", serverIP);
                peer_port = p_port.getText().toString();
                Log.d("p2p-test", peer_port);
                connectClicked(view);
            }

        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }

    public void bluetoothClicked(View view) {
        on(view);
        listDevices.clear();
        nameDevices.clear();
        for(BluetoothDevice b : BA.getBondedDevices()){
            listDevices.add(b);
            nameDevices.add(b.getName());

        }
        Log.d("devices", listDevices.toString());
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Paired Devices");

        String[] arrDevices = nameDevices.toArray(new String[nameDevices.size()]);
        final int[] checkedItem = {0};
        builder.setSingleChoiceItems(arrDevices, checkedItem[0], new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                toastCenter(arrDevices[which]);
                checkedItem[0] = which;
            }
        });

        builder.setNeutralButton("SETTINGS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intentOpenBluetoothSettings = new Intent();
                intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(intentOpenBluetoothSettings);
            }
        });
        builder.setPositiveButton("CONNECT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedDevice = listDevices.get(checkedItem[0]);
                //ConnectThread(selectedDevice);
                bluetoothConnect(view);
                //runClient();
            }
        });
        builder.setNegativeButton("Cancel", null);

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void bluetoothConnect(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select:");

        String[] connect = {"Client","Server"};
        final int[] checkedItem = {0};
        builder.setSingleChoiceItems(connect, checkedItem[0], new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkedItem[0] = which;
            }
        });

        builder.setPositiveButton("Select", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch(checkedItem[0]){
                    case 0:
                        //ConnectThread(selectedDevice);
                        //onClient(view);
                        break;
                    case 1:

                        break;
                    default:
                }
            }
        });
        builder.setNegativeButton("Cancel", null);

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
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

    public void connectOnClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Type:");

        String[] connect = {"Wifi","Bluetooth"};
        final int[] checkedItem = {0};
        builder.setSingleChoiceItems(connect, checkedItem[0], new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkedItem[0] = which;
            }
        });

        builder.setPositiveButton("Select", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch(checkedItem[0]){
                    case 0:
                        wifiClicked(view);
                        break;
                    case 1:
                        bluetoothClicked(view);
                        break;
                    default:
                }
            }
        });
        builder.setNegativeButton("Cancel", null);

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
    }
    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
    }


}
