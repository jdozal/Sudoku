package edu.utep.cs.cs4330.sudoku;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.preference.PreferenceManager;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import static android.provider.Settings.NameValueTable.NAME;

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

    private Socket socket;

    private static final int CONNECTION_TIMEOUT = 5000; // in milliseconds

    private Handler handler;
    public static final java.util.UUID MY_UUID = java.util.UUID.fromString("DEADBEEF-0000-0000-0000-000000000000");

    private BluetoothAdapter BA;

    private  BluetoothSocket mmSocket;

    private  BluetoothServerSocket serverSocket;

    private List<BluetoothDevice> listDevices;

    private PrintStream logger;
    private OutputStream outSt;

    private NetworkAdapter na;
    private NetworkAdapter.MessageListener naMess;

    private BluetoothDevice selectedDevice;

    private String ip;
    private String peer_ip;
    private String peer_port;
    
    private ArrayList<String> nameDevices;

    private Board board;

    private BoardView boardView;

    String server = "";


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
        naMess = null;

        // Getting ip
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        Log.d("ip_number", ip);

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

        openBT();
    }

    private void openBT() {
        outSt = new ByteArrayOutputStream(1024);
        logger = new PrintStream(outSt);
        naMess = new NetworkAdapter.MessageListener() {
            @Override
            public void messageReceived(NetworkAdapter.MessageType type, int x, int y, int z, int[] others) {
                switch (type.header){
                    case "join:":
                        Log.d("Progress", "Progress");

                        ArrayList<Integer> param = new ArrayList<Integer>();
                        for (int i = 0; i < board.size; i++) {
                            for (int j = 0; j < board.size; j++) {
                                if (board.getSquare(x,y).getValue() > 0) {
                                    param.add(i);
                                    param.add(j);
                                    param.add(board.getSquare(x,y).getValue());
                                    int[] temp = {i, j};
//                                    for (int[] space: hint) {
//                                        if(space[0] == temp[0] && space[1] == temp[1]){
//                                            Log.d("Hint", "Added");
//                                            param.add(1);
//                                        }
//                                    }
//                                    if(param.size() % 4 != 0){
//                                        param.add(0);
//                                    }
                                    param.add(0);
                                }
                            }
                        }
                        int[] ret = new int[param.size()];
                        for (int i=0; i < ret.length; i++)
                        {
                            ret[i] = param.get(i).intValue();
                        }
                        na.writeJoinAck(board.size, ret);
                        break;
                    case "join_ack:":
                        Log.d("Progress", "Progress");
//                        board.size = x;
//                        board.player = new int[y][y];
//                        hint = new ArrayList<>();
//                        for(int i = 0; i < others.length; i = i + 4){
//                            board.player[others[i]][others[i + 1]] = others[i + 2];
//                            if(others[i + 3] == 1){
//                                int[] temp = {others[i], others[i + 1]};
//                                hint.add(temp);
//                            }
//                        }
//                        boardView.setHint(hint);
//                        boardView.postInvalidate();
                        break;
                    case "new:":

                        break;
                    case "new_ack:":

                        break;
                    case "fill:":
                        Log.d("Progress", "Progress");
//                        board.player[x][y] = z;
//                        na.writeFillAck(x, y, z);
//                        boardView.postInvalidate();
                        break;
                    case "fill_ack:":
                        Log.d("Confirmation", "Indeed");
                        break;
                    case "quit:":

                        break;
                }
            }
        };
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
        p_ip.setHint(ip);
        layout.addView(p_ip); // Notice this is an add method

        // Add another TextView here for the "Description" label
        final EditText p_port = new EditText(this);
        p_port.setHint("8000");
        layout.addView(p_port); // Another add method

        builder.setTitle("Pairing");
        builder.setView(layout);

        // Set up the buttons
        builder.setPositiveButton("CONNECT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                peer_ip = p_ip.getText().toString();
                Log.d("msn", peer_ip);
                peer_port = p_port.getText().toString();
                Log.d("msn", peer_port);
                connectToServer(peer_ip, Integer.getInteger(peer_port));

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
                        ConnectThread(selectedDevice);
                        onClient(view);
                        break;
                    case 1:
                        try {
                            onServer(view);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
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
    private Socket createSocket(String host, int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT);
            return socket;
        } catch (Exception e) {
            Log.d("socket", e.toString());
        }
        return null;
    }

    private void sendMessage(String msg) {
        if (socket == null) {
            toastCenter("Not connected!");
            return;
        }

    }

    private void connectToServer(String server, int port) {
        new Thread(() -> {
            socket = createSocket(server, port);
            if (socket != null) {
                try {
                    socket.connect(new InetSocketAddress(server, port), 3000); // in milliseconds
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            handler.post(() -> toastCenter(socket != null ? "Connected." : "Failed to connect!"));
        }).start();
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

        String[] connect = {"Wifi p2p","Bluetooth"};
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
    public void onBT(View view) throws IOException {
        if (!BA.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
            Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            startActivityForResult(getVisible, 0);
            AcceptThread();
            runServer();
        }
    }
    public void AcceptThread(){
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = BA.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e("Not listening", "Socket's listen() method failed", e);
            }
            serverSocket = tmp;
    }

    public void runServer() throws IOException {
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned.
        while (true) {
            try {
                Log.e("test", "Socket's accept()");
                socket = serverSocket.accept(10000);
            } catch (IOException e) {
                Log.e("Not accepting", "Socket's accept() method failed", e);
                break;
            }

            if (socket != null) {
                // A connection was accepted. Perform work associated with
                // the connection in a separate thread.
                toast("Connected");
                na = new NetworkAdapter(socket, logger);
                na.setMessageListener(naMess);
                na.receiveMessagesAsync();
                serverSocket.close();
                break;
            }
            else {
                toast("Null socket");
            }
        }
    }

    public void runClient() {
        // Cancel discovery because it otherwise slows down the connection.
        BA.cancelDiscovery();

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e("Close socket", "Could not close the client socket", closeException);
            }
            return;
        }

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.

        toast("Connected");
        if(mmSocket == null){
            toast("Null client");
        }else {
            na = new NetworkAdapter(mmSocket, logger);
            na.setMessageListener(naMess);
            na.receiveMessagesAsync();
            na.writeJoin();
        }
    }

    public void ConnectThread(BluetoothDevice device) {
        BluetoothSocket tmp = null;
        selectedDevice = device;
        try {
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        }
        catch (IOException e) {
            Log.e("error_socket", "Socket: " + tmp.toString() + " create() failed", e);
        }

        mmSocket = tmp;
        Log.d("socket", selectedDevice.toString());
    }

    //Client Functions
    public void onClient(View v){
        if (!BA.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            listDevices = new ArrayList<BluetoothDevice>();
            nameDevices = new ArrayList<String>();
            for (BluetoothDevice b : BA.getBondedDevices()) {
                listDevices.add(b);
                nameDevices.add(b.getName());
            }
            Toast.makeText(getApplicationContext(), "Turned on",Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
            listDevices = new ArrayList<BluetoothDevice>();
            nameDevices = new ArrayList<String>();
            for (BluetoothDevice b : BA.getBondedDevices()) {
                listDevices.add(b);
                nameDevices.add(b.getName());
            }
        }
    }

    //Server Functions
    public void onServer(View v) throws IOException {
        if (!BA.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
            Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            startActivityForResult(getVisible, 0);
            AcceptThread();
            runServer();
        }
    }

}
