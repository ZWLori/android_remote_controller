package com.example.wanluzhang.mdp_17;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.support.v7.app.ActionBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MainActivity : ";

    // msg type sent from the Bluetooth Service Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // key names received from the Bluetooth Service Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 2;
    private static final int REQUEST_ENABLE_BLUETOOTH = 3;

    private String mConnectedDevice= "";
    private BluetoothAdapter BA = null;
    private Bluetooth chatService = null;
    private Sensor accelerometer;
    private SensorManager SM;
    private Handler customerHandler = new Handler();
    private Arena arena;
    private ArenaThread thread;
    private String gridString = "GRID 20 15 2 18 2 19 0 0 0 0 0 0 0";
    private int[] intArray = new int[300];

    // AMD
    private ListView tConversationView;
    private ListView fConversationView;
    private Button tSendButton;
    private ArrayAdapter<String> tConversationAA;
    private ArrayAdapter<String> fConversationAA;

    String decodeString;
    // f1, f2 configuration
    SharedPreferences preferences;
    Handler mMyHandler = new Handler();
    TextView robotStatus, exploreTime, fastestTime;
    EditText x_coordinate, y_coordinate, direction;
    EditText TextAMD;
    ToggleButton autoManaul, explore, fastest;
    ToggleButton tiltSensing;
    Button update;
    Button f1, f2;
    ImageButton up, left, right;
    RelativeLayout arenaDisplay;

    boolean autoUpdate = true;
    boolean tilt = false;
    int[][] obstacleArray = new int[20][15];
    int[][] spArray = new int[20][15];
    ArrayList obstacleSensor = new ArrayList();
    private long startTimeExplore = 0L;
    private long startTimeFastest = 0L;
    long timeBuffExplore = 0L;
    long timeBuffFastest = 0L;
    long timeInMillisecondsExplore = 0L;
    long timeInMillisecondsFastest = 0L;
    long updateTimeExplore = 0L;
    long updateTimeFastest = 0L;
    StringBuffer outStringBuff;
    JSONObject jsonObj;

    // fastest path
    String dir = "";
    int run = 0;
    List<String> spSteps;

    // robot default position
    int xStatus = 2;
    int yStatus = 19;
    int dStatus = 180;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.e(TAG, "+++ ON CREATE +++");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        BA = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (BA == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Tilt Sensing
        SM = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        SM.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        // data recorded in the SettingActivity
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // assign UI elements
        robotStatus = (TextView)findViewById(R.id.text_robotStatus);

        exploreTime = (TextView)findViewById(R.id.timer_explore);
        fastestTime = (TextView)findViewById(R.id.timer_fastest);

        x_coordinate = (EditText)findViewById(R.id.coor_x);
        y_coordinate = (EditText)findViewById(R.id.coor_y);
        direction = (EditText)findViewById(R.id.dir);

        TextAMD = (EditText)findViewById(R.id.send_text);
        autoManaul = (ToggleButton)findViewById(R.id.btn_automanual);
        update = (Button)findViewById(R.id.btn_update);
        explore = (ToggleButton)findViewById(R.id.btn_explore);
        fastest = (ToggleButton)findViewById(R.id.btn_fastest);
        tiltSensing = (ToggleButton)findViewById(R.id.tilt_btn);

        f1 = (Button)findViewById(R.id.btn_f1);
        f2 = (Button)findViewById(R.id.btn_f2);

        up = (ImageButton)findViewById(R.id.btn_up);
        left = (ImageButton)findViewById(R.id.btn_left);
        right = (ImageButton)findViewById(R.id.btn_right);

        // initializing the environment
        init();

        // setup the on click listeners for each button
        f1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "F1 clicked");
                sendMessage(preferences.getString("F1Command", ""));
            }
        });
        f2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "F2 clicked");
                sendMessage(preferences.getString("F2Command", ""));
            }
        });

        up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Up arrow clicked");
                goStraight();
            }
        });
        left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Left arrow clicked");
                turnLeft();
            }
        });
        right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Right arrow clicked");
                turnRight();
            }
        });

        // the update button only can be used when the auto button is set to off
        update.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    if(obstacleArray != null){
                        autoUpdate = true;
                        arena.setObstacles(obstacleArray);
                    }
                    if(decodeString != null){
                        autoUpdate = true;
                        System.out.println(decodeString);
                        updateGridArray(toIntArray(decodeString));
                    }
                    autoUpdate = false;
                    Toast.makeText(MainActivity.this, "Map Updated", Toast.LENGTH_SHORT).show();

                } catch(Exception e){
                    Toast.makeText(MainActivity.this, "Already Updated", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if(autoUpdate){
//            update.setBackgroundResource(R.drawable.enabled_btn);
//            Toast.makeText(MainActivity.this, "Auto Update : ON", Toast.LENGTH_SHORT).show();
            autoManaul.setChecked(true);
        }

        // 4 toggle buttons, set the toggle situations and the button style
        autoManaul.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){   // AUTO motion
                    update.setEnabled(false);
                    update.setBackgroundColor(Color.parseColor("#efa1bfc7"));
                    autoUpdate = true;
                    Toast.makeText(MainActivity.this, "Auto Update : ON", Toast.LENGTH_SHORT).show();
                }
                else{           // Manual motion
                    autoUpdate = false;
                    update.setEnabled(true);
                    update.setBackgroundResource(R.drawable.enabled_btn);
                    Toast.makeText(MainActivity.this, "Auto Update : OFF", Toast.LENGTH_SHORT).show();
                }
            }
        });

        explore.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    String sendPos = x_coordinate.getText().toString() + ","
                            + y_coordinate.getText().toString() + ","
                            + direction.getText().toString();
                    sendMessage("start," + sendPos);
                    startTimeExplore = SystemClock.uptimeMillis();
                    customerHandler.post(updateTimerThreadExplore);
                }
                else{
                    timeBuffExplore += timeInMillisecondsExplore;
                    customerHandler.removeCallbacks(updateTimerThreadExplore);
                }
            }
        });

        fastest.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    sendMessage("sp");
//                    try {
//                        runSp();
//                    }catch(JSONException e){
//                        e.printStackTrace();
//                    }
                    startTimeFastest = SystemClock.uptimeMillis();
                    customerHandler.post(updateTimerThreadFastest);
                }
                else{
                    timeBuffFastest += timeInMillisecondsFastest;
                    customerHandler.removeCallbacks(updateTimerThreadFastest);
                }
            }
        });

        tiltSensing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
//                if(isChecked){
//                    onResume();
//                    tilt = true;
//                }
//                else{
//                    onPause();
//                    tilt = false;
//                }
            }
        });

    }

    // init the whole android environment
    private void init(){
        // default value for the robot position
        gridString = "GRID 20 15 2 18 2 19 0 0 0 0 0 0 0";
        x_coordinate.setText("2", TextView.BufferType.EDITABLE);
        y_coordinate.setText("19", TextView.BufferType.EDITABLE);
        direction.setText("180");
        intArray = toIntArray(gridString);
        arena = new Arena(this, intArray);
        arena.setClickable(false);
        arena.setGridArray(intArray);
        for(int x = 0; x < 20; x++){
            for(int y = 0; y < 15; y++){
                obstacleArray[x][y] = 0;
                spArray[x][y] = 0;
            }
        }

        arena.setObstacles(obstacleArray);
        arena.setSpArray(spArray);
        arenaDisplay = (RelativeLayout) findViewById(R.id.arenaView);
        arenaDisplay.addView(arena);
    }

    @Override
    public void onStart(){
        super.onStart();
        Log.e(TAG, "++ ON START ++");
        if(!BA.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, REQUEST_ENABLE_BLUETOOTH);
            Toast.makeText(getApplicationContext(), "Disabled bluetooth", Toast.LENGTH_SHORT).show();
        }
        else{
            if(chatService == null){
                setupChat();
            }
            Toast.makeText(getApplicationContext(),"Enabled bluetooth", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupChat(){
        Log.d(TAG, "setupChat()");
        // initialize the array for the conversation thread
        tConversationAA = new ArrayAdapter<String>(this, R.layout.text);
        fConversationAA = new ArrayAdapter<String>(this, R.layout.text);

        tConversationView = (ListView) findViewById(R.id.listView_to);
        tConversationView.setAdapter(tConversationAA);
        fConversationView = (ListView) findViewById(R.id.listView_from);
        fConversationView.setAdapter(fConversationAA);

//        tSendButton = (Button) findViewById(R.id.send_btn);
//        tSendButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                String msg = TextAMD.getText().toString();
//                sendMessage(msg);
//                TextAMD.setText("");
//            }
//        });

        // initiate the bluetooth service for connections
        chatService = new Bluetooth(this, mHandler);
        // initiate the buffer for outgoing msg
        outStringBuff = new StringBuffer("");
    }

    @Override
    public synchronized void onResume(){
        Log.d(TAG, "++ ON RESUME ++");
        super.onResume();
        // Resume the BT when it first fail onStart()
        if (chatService != null) {
            if (chatService.getState() == Bluetooth.STATE_IDLE) {
                // Start the Bluetooth chat services
                chatService.start();
            }
        }
        // Resume the Tilt Sensing
        SM.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause(){
        Log.d(TAG, "++ ON PAUSE ++");
        super.onPause();
        // sensor on pause
        SM.unregisterListener(this);
    }

    @Override
    public void onStop(){
        Log.d(TAG, "++ ON STOP ++");
        super.onStop();
    }

    @Override
    public void onDestroy(){
        Log.d(TAG, "++ ON DESTROY ++");
        super.onDestroy();
        if(chatService != null){
            chatService.stop();
        }
    }

    private void ensureDiscoverable(){
        Log.d(TAG, "ensure discoverable");
        if(BA.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent requireDiscoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            requireDiscoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(requireDiscoverable);
        }
    }

    private void sendMessage(String string) {
        Log.d(TAG, "sendMessage()" + string);
        // Check that we're actually connected before trying anything
        if(chatService.getState() != Bluetooth.STATE_CONNECTED){
            Toast.makeText(this, "Bluetooth Not Connected", Toast.LENGTH_SHORT).show();
            return;
        }

        if(string.length() > 0){
            string = "pa" + string + "\n";
            byte[] msgSend = string.getBytes();
            chatService.write(msgSend);
            // reset buffer to zero
            outStringBuff.setLength(0);
        }
    }

    private TextView.OnEditorActionListener writeListener = new TextView.OnEditorActionListener(){
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event){
            if(actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP){
                String msg = view.getText().toString();
                sendMessage(msg);
            }
            return true;
        }
    };

    private Runnable updateTimerThreadExplore = new Runnable() {
        @Override
        public void run() {
            timeInMillisecondsExplore = SystemClock.uptimeMillis() - startTimeExplore;
            updateTimeExplore = timeBuffExplore + timeInMillisecondsExplore;

            int sec = (int) (updateTimeExplore/1000);
            int min = sec/60;
            sec %= 60;
            int millisecond = (int) (updateTimeExplore % 1000);
            int milli = millisecond / 10;
            if(min < 10){
                exploreTime.setText("0" + min + ":" + sec + ":" + milli);
            }
            else{
                exploreTime.setText(min + ":" + sec + ":" + milli);
            }
            customerHandler.post(this);
        }
    };

    private Runnable updateTimerThreadFastest = new Runnable() {
        @Override
        public void run() {
            timeInMillisecondsFastest = SystemClock.uptimeMillis() - startTimeFastest;
            updateTimeFastest = timeBuffFastest + timeInMillisecondsFastest;

            int sec = (int) (updateTimeFastest/1000);
            int min = sec/60;
            sec %= 60;
            int millisecond = (int) (updateTimeFastest % 1000);
            int milli = millisecond / 10;
            if(min < 10){
                fastestTime.setText("0" + min + ":" + sec + ":" + milli);
            }
            else{
                fastestTime.setText(min + ":" + sec + ":" + milli);
            }
            customerHandler.post(this);
        }
    };

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            robotStatus.setText("Stopped");
        }
    };

    public void goStraight(){
        sendMessage("f");
        try {
            decodeString = decodeRobotString_algo("{go:[F]}");
            if(decodeString != null)
                updateGridArray(toIntArray(decodeString));
        } catch (JSONException e){}
        // 1 sec later, set robot status back to stopped
        mMyHandler.postDelayed(mRunnable, 1000);
    }

    public void turnLeft(){
        sendMessage("l");
        try {
            decodeString = decodeRobotString_algo("{go:[L]}");
            if(decodeString != null)
                updateGridArray(toIntArray(decodeString));
        } catch (JSONException e){}
        mMyHandler.postDelayed(mRunnable, 1000);
    }

    public void turnRight(){
        sendMessage("r");
        try {
            decodeString = decodeRobotString_algo("{go:[R]}");
            if(decodeString != null)
                updateGridArray(toIntArray(decodeString));
        } catch (JSONException e){}
        mMyHandler.postDelayed(mRunnable, 1000);
    }

    public void start(View v){
        // pastart,x,y,d\n
//        String sendPos = x_coordinate.getText().toString() + ","
//                + y_coordinate.getText().toString() + ","
//                + direction.getText().toString();
//        sendMessage("pastart," + sendPos + "\n");
    }

    /*
        parse a string into int array
     */
    public int[] toIntArray(String s){
//        Log.d("toIntArray()", s);
        String[] stringArray = s.split(" ");
        int len = stringArray.length-1;
        int[] intArray = new int[len];

        for(int i = 1; i < len; i++){
            intArray[i-1] = Integer.parseInt(stringArray[i]);
        }
        return intArray;
    }


    private final void setStatus(CharSequence subTitle) {
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        //final ActionBar actionBar = this.getActionBar();
        if (actionBar == null) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /*
        create handler to get info back from the Bluethooth Chat Service
     */
    private final Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch(msg.what){
                case MESSAGE_STATE_CHANGE:
                    Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case Bluetooth.STATE_CONNECTED:
                            setStatus("Connected to " + mConnectedDevice);
                            tConversationAA.clear();
                            break;
                        case Bluetooth.STATE_CONNECTING:
                            setStatus("Connecting");
                            break;
                        case Bluetooth.STATE_LISTEN:
                        case Bluetooth.STATE_IDLE:
                            setStatus("Disconnected");
                            break;
                    }
                    break;

                case MESSAGE_READ:
                    byte[] read = (byte[]) msg.obj;
                    String readMsg = new String(read, 0, msg.arg1);
                    if(readMsg.contains("grid")){
                        Log.d(TAG, "receive the grid string");
                        try {
                            // the readMessage is in a hex format
                            fConversationAA.add(mConnectedDevice + " : " + readMsg);
                            stepMovement(readMsg.split("grid")[0]);
                            String grid = readMsg.split("grid")[1];
                            obstacleArray = decodeObstacleString(grid);
                            updateObstacleArray(obstacleArray);
                        } catch (JSONException e){
                            e.printStackTrace();
                        }
                    }
                    else if(readMsg.contains("sp")){
                        try{
                            fConversationAA.add(mConnectedDevice + " : " + readMsg);
                            String[] fastestSteps = readMsg.replace("sp","").split(",");
                            //spSteps = Arrays.asList(fastestSteps);
                            drawSp(fastestSteps);
                        } catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                    else if(readMsg.contains("#")){
                        Log.d(TAG, "receiving step command");
                        try {
                            fConversationAA.add(mConnectedDevice + " : " + readMsg);
                            stepMovement(readMsg);
                        } catch(JSONException e){
                            e.printStackTrace();
                        }
                    }
                    else if(readMsg.contains("go")){
                        try{
                            fConversationAA.add(mConnectedDevice + " : " + readMsg);
                            decodeString = decodeRobotString_algo(readMsg);
                            if(decodeString != null)
                                updateGridArray(toIntArray(decodeString));
                        } catch(JSONException e){   // json generating error
                            e.printStackTrace();
                        }
                    }
                    else{
                        fConversationAA.add(mConnectedDevice + " : " + readMsg);
                        decodeString = readMsg;
                    }
                    break;

                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    tConversationAA.add("Group 17:  " + writeMessage);
                    Toast.makeText(MainActivity.this, "SEND", Toast.LENGTH_SHORT).show();
                    break;

                case MESSAGE_DEVICE_NAME:
                    mConnectedDevice = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected with " + mConnectedDevice, Toast.LENGTH_SHORT).show();
                    break;

                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    /*
        decode each step received and update the map to show robot movement
     */
    public void stepMovement(String msg) throws JSONException{
        String step = msg.replace("#", "");
        move(step);
        if (dir.equals("F")) {
            Log.d("run:", String.valueOf(run));
            for (int i = 0; i < run; i++) {
                decodeString = decodeRobotString_algo("{go:[F]}");
                updateGridArray(toIntArray(decodeString));
            }
        } else if (dir.equals("L")) {
            decodeString = decodeRobotString_algo("{go:[L]}");
            updateGridArray(toIntArray(decodeString));
        } else if (dir.equals("R")) {
            decodeString = decodeRobotString_algo("{go:[R]}");
            updateGridArray(toIntArray(decodeString));
        } else if (dir.equals("l")) {
            decodeString = decodeRobotString_algo("{go:[F]}");
            updateGridArray(toIntArray(decodeString));
            decodeString = decodeRobotString_algo("{go:[F]}");
            updateGridArray(toIntArray(decodeString));
            decodeString = decodeRobotString_algo("{go:[L]}");
            updateGridArray(toIntArray(decodeString));
            decodeString = decodeRobotString_algo("{go:[F]}");
            updateGridArray(toIntArray(decodeString));
            decodeString = decodeRobotString_algo("{go:[F]}");
            updateGridArray(toIntArray(decodeString));
        } else if (dir.equals("r")) {
            decodeString = decodeRobotString_algo("{go:[F]}");
            updateGridArray(toIntArray(decodeString));
            decodeString = decodeRobotString_algo("{go:[F]}");
            updateGridArray(toIntArray(decodeString));
            decodeString = decodeRobotString_algo("{go:[R]}");
            updateGridArray(toIntArray(decodeString));
            decodeString = decodeRobotString_algo("{go:[F]}");
            updateGridArray(toIntArray(decodeString));
            decodeString = decodeRobotString_algo("{go:[F]}");
            updateGridArray(toIntArray(decodeString));
        }
    }

    /*
        for each step, check its direction. if it's forward command, calculate the number followed by "F"
     */
    public void move(String s){
        String numMove = s;
        Log.d("numMove: ",numMove);

        dir = numMove.substring(0, 1);
        numMove = numMove.replace(dir, "");
        numMove = numMove.replace(",B,", "");
        if(numMove == ""){
            run = 0;
            return;
        }
        run = Integer.parseInt(numMove);

        //run = Integer.parseInt(numMove.substring(2, 3));
        Log.d("run:",numMove);
    }

    /*
        draw out the shortest path
    */
    public void drawSp(String[] steps){
        int d = 180;
        int movement = 0;
        // set a pointer pointing to the drawing square
        int xptr = 2;
        int yptr = 19;
        for(String step : steps){
            // check direction first
            if(d == 0){
                if (step.contains("R"))
                    d = 90;
                else if (step.contains("L"))
                    d = 270;
            }
            else if(d == 90){
                if (step.contains("R"))
                    d = 180;
                else if (step.contains("L"))
                    d = 0;
            }
            else if(d == 180){
                if (step.contains("R"))
                    d = 270;
                else if (step.contains("L"))
                    d = 90;
            }
            else if(d == 270){
                if (step.contains("R"))
                    d = 0;
                else if (step.contains("L"))
                    d = 180;
            }
            // set the moving steps
            if (step.contains("F")){
                movement = Integer.parseInt(step.replace("F", ""));
                // update the array based on the direction
                if(d == 0){
                    for(int i = 0; i < movement; i++) {
                        yptr++;
                        spArray[yptr][xptr] = 1;
                    }
                }
                if(d == 90){
                    for(int i = 0; i < movement; i++) {
                        xptr--;
                        spArray[yptr][xptr] = 1;
                    }
                }
                if(d == 180){
                    for(int i = 0; i < movement; i++) {
                        yptr--;
                        spArray[yptr][xptr] = 1;
                    }
                }
                if(d == 270){
                    for(int i = 0; i < movement; i++) {
                        xptr++;
                        spArray[yptr][xptr] = 1;
                    }
                }
            }
        }
        sendMessage("runsp");
    }

    // not necessary for now
    public void runSp() throws JSONException{
        Log.d(TAG, "runSp()");
        for(String step: spSteps){
            move(step);
            if(dir.equals("F")){
                Log.d("run:",String.valueOf(run));
                for(int i = 0; i < run; i++){
                    decodeString = decodeRobotString_algo("{go:[F]}");
                    updateGridArray(toIntArray(decodeString));
                }
            }
            else if(dir.equals("L")){
                decodeString = decodeRobotString_algo("{go:[L]}");
                updateGridArray(toIntArray(decodeString));
            }
            else if(dir.equals("R")){
                decodeString = decodeRobotString_algo("{go:[R]}");
                updateGridArray(toIntArray(decodeString));
            }
            try {
                Thread.sleep(300);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /*
        decode the json format string and update the robot status correspondingly
     */
    public String decodeRobotString_algo(String s)throws JSONException{
        jsonObj = new JSONObject(s);
        String decode = jsonObj.getString("go");
        decode = decode.replace("[","");
        decode = decode.replace("]","");
        int robotX = xStatus;
        int robotY = yStatus;
        int robotD = dStatus;

        // move forward
        if(decode.equals("\"F\"")){
            switch(dStatus){
                case 0:
                    robotY = yStatus + 1;
                    break;
                case 90:
                    robotX = xStatus - 1;
                    break;
                case 180:
                    robotY = yStatus - 1;
                    break;
                case 270:
                    robotX = xStatus + 1;
                    break;
            }
        }
        // turn left
        if(decode.equals("\"L\"")){
            switch(dStatus){
                case 0:
                    robotD = 270;
                    break;
                case 90:
                    robotD = 0;
                    break;
                case 180:
                    robotD = 90;
                    break;
                case 270:
                    robotD = 180;
                    break;
            }
        }
        // turn right
        if(decode.equals("\"R\"")){
            switch(dStatus){
                case 0:
                    robotD = 90;
                    break;
                case 90:
                    robotD = 180;
                    break;
                case 180:
                    robotD = 270;
                    break;
                case 270:
                    robotD = 0;
                    break;
            }
        }
        // check whether the robot is moving out of bound
        if(robotX < 2 || robotX > 14 || robotY < 2 || robotY > 19)
            return null;
        return decodeRobotString_(robotX, robotY, robotD);
    }

    // AMD json decoding
    public String decodeRobotString(String s)throws JSONException{
        jsonObj = new JSONObject(s);
        String decode = jsonObj.getString("go");
        decode = decode.replace("[", "");
        decode = decode.replace("]", "");
        String array[] = decode.split(",");
        Integer robotX = Integer.parseInt(array[0]);
        Integer robotY = Integer.parseInt(array[1]);
        Integer robotD = Integer.parseInt(array[2]);

        return decodeRobotString_(robotX, robotY, robotD);
    }

    /*
        used to set the robot status and update the grid string
     */
    public String decodeRobotString_(int x, int y, int d){
        String decode = "";
        String hx = "";
        String hy = "";
        String bx = String.valueOf(x);
        String by = String.valueOf(y);

        x_coordinate.setText(x+"");
        y_coordinate.setText(y+"");

        if (d == 0){
            hx = String.valueOf(x);
            hy = String.valueOf(y+1);

            direction.setText("0");

            if(dStatus == 270){
                robotStatus.setText("Turn Right");
            }
            if(dStatus == 90){
                robotStatus.setText("Turn Left");
            }
            if(y < yStatus){
                robotStatus.setText("Moving Foward");
            }
            if(y > yStatus){
                robotStatus.setText("Moving Backward");
            }

        }
        else if (d == 90){
            hx = String.valueOf(x-1);
            hy = String.valueOf(y);

            direction.setText("90");
            if(dStatus == 0){
                robotStatus.setText("Turn Right");
            }
            if(dStatus == 180){
                robotStatus.setText("Turn Left");
            }
            if(x > xStatus){
                robotStatus.setText("Moving Foward");
            }
            if(x > xStatus){
                robotStatus.setText("Moving Backward");
            }

        }
        else if (d == 180){
            hx = String.valueOf(x);
            hy = String.valueOf(y-1);

            direction.setText("180");
            if(dStatus == 90){
                robotStatus.setText("Turn Right");
            }
            if(dStatus == 270){
                robotStatus.setText("Turn Left");
            }
            if(y > yStatus){
                robotStatus.setText("Moving Foward");
            }
            if(y < yStatus){
                robotStatus.setText("Moving Backward");
            }

        }
        else if (d == 270){
            hx = String.valueOf(x+1);
            hy = String.valueOf(y);
            direction.setText("270");
            if(dStatus == 180){
                robotStatus.setText("Turn Right");
            }
            if(dStatus == 0){
                robotStatus.setText("Turn Left");
            }
            if(x < xStatus){
                robotStatus.setText("Moving Foward");
            }
            if(x > xStatus){
                robotStatus.setText("Moving Backward");
            }

        }
        // delay the status updating by 1 sec
        mMyHandler.postDelayed(mRunnable, 1000);
        decode = "GRID 20 15 " + hx + " " + hy + " " + bx + " " + by + " 0 0 0 0 0 0 0 0";

        xStatus = x;
        yStatus = y;
        dStatus = d;
        Log.d(TAG, "Grid decode: " + decode);
        return decode;
    }

    /*
        decode the grid string: add up the unexplored-explored with the empty-obstacle and invert it to get the result
     */
    public int[][] decodeObstacleString(String s){
        Log.d(TAG, "decodeObstacleString: " + s);
        String decode = s.replace("grid", "");
        String[] unexploredExplored;
        String[] emptyObstacle;
        String[] received = decode.split("-");
        unexploredExplored = received[0].split("");
        emptyObstacle = received[1].split("");
        // convert the hex value into binary one
        int[][] exploreArr = convertToInt(unexploredExplored);
        int[][] obsArr = addObstacle(exploreArr,emptyObstacle);
        // reverse the array
        for(int i=0;i<obsArr.length/2;i++){
            int[] temp = obsArr[i];
            obsArr[i] = obsArr[19-i];
            obsArr[19-i] = temp;
        }
        for(int i = 0; i < 20; i++){
            for (int j = 0; j < 15; j++) {
                System.out.print(obsArr[i][j]);
            }
            System.out.println();
        }
        return obsArr;
    }

    public int[][] addObstacle(int[][] myMap, String[] obstacles) {
        String[] binaryTempArray;
        String string = "";
        for (int i = 1; i < obstacles.length; i++){
            int hexToInt = Integer.parseInt(obstacles[i], 16);
            String intToBinary = Integer.toBinaryString(hexToInt);
            // make sure all are 4 bits
            while (intToBinary.length() < 4){
                intToBinary = "0" + intToBinary;
            }
            string += intToBinary;
        }
        binaryTempArray = string.split("");
        String[] binaryArray = Arrays.copyOfRange(binaryTempArray, 1, binaryTempArray.length);
        int ptr = 0;
        int arrLength = binaryArray.length;
        for(int i=0;i<myMap.length;i++) {
            for(int j=0;j<myMap[i].length;j++) {
                if(myMap[i][j]==0) {
                    continue;
                } else {
                    if(ptr==arrLength) break;
                    myMap[i][j] += Integer.parseInt(binaryArray[ptr]);
                    ++ptr;
                }
            }
        }
        return myMap;
    }

    /*
        convert the string array with hax value to 2d int array with binary value
     */
    private int[][] convertToInt(String[] s){
        int[][] obstacleCells = new int[20][15];
        String[] binaryTempArray;
        String string = "";
        for (int i = 1; i < s.length; i++){
            int hexToInt = Integer.parseInt(s[i], 16);
            String intToBinary = Integer.toBinaryString(hexToInt);
            // make sure all are 4 bits
            while (intToBinary.length() < 4){
                intToBinary = "0" + intToBinary;
            }
            string += intToBinary;
        }
        binaryTempArray = string.split("");
        String[] binaryArray = Arrays.copyOfRange(binaryTempArray, 1, binaryTempArray.length);
        for(int i = 0; i < 20; i++){
            for (int j = 0; j < 15; j++) {
                obstacleCells[i][j] = Integer.parseInt(binaryArray[i*15+j]);
            }
        }
        return obstacleCells;
    }

    // not nececssary for now
    public String decodeObstacleArray(String s)throws JSONException{
        jsonObj = new JSONObject(s);
        String decode = jsonObj.getString("Arena");
        Integer count = 0;
        int[] obstacleList = new int[300];
        // get the obstacle array list
        try{
            JSONArray array = new JSONArray(decode);
            for(int x = 0; x < 15; x++){
                for(int y = 0; y < 20; y++){
                    if(array.getJSONArray(x).getString(y).equals("1"))
                        obstacleArray[x][y] = 1;     // is an obstacle
                    else
                        obstacleArray[x][y] = 0;
                    obstacleList[count] = obstacleArray[x][y];
                    count++;
                }
            }
        } catch(JSONException e){
            e.printStackTrace();
        }

        // change the array to hex value
        String output = "";
        for(int i = 0; i < 300; i = i + 4){
            String binaryStr = ""+obstacleList[i]+obstacleList[i+1]+obstacleList[i+2]+obstacleList[i+3];
            int binary = Integer.parseInt(binaryStr, 2);
            String hex = Integer.toHexString(binary);
            output += hex;
        }

        //{"grid":"ffff000000000000000000000000000000000000000000000000000000000000000000000000"}
        //Log.d("Final Coord Obs: ",outputHex);
        output = "{\"grid\":" + "\"" + output + "\"}";

        return output;
    }

    /*
        update the obstacle array(tgt with the explored path) based on the passed in array
     */
    public void updateObstacleArray(int[][] list){
        Log.d(TAG, "updateObstacleArray()");
        if(autoUpdate == true){
            arena.setObstacles(list);
        }
    }

    /*
        update robot position
     */
    public void updateGridArray(int[] array){
        if(autoUpdate == true){
            Log.d("updateGridArray","true");
            arena.setGridArray(array);
        }
    }
    /*
        check for connection result
     */
    public void onActivityResult(int request, int result, Intent data){
        if (true)
            Log.d(TAG, "onActivityResult " + result);
        switch (request){
            case REQUEST_CONNECT_DEVICE:
                System.out.println("onActivityResult");
                if (result == Activity.RESULT_OK){
                    System.out.println("result ok");
                    connectDevice(data);
                }
                break;
            case REQUEST_ENABLE_BLUETOOTH:
                if(result == Activity.RESULT_OK){
                    setupChat();
                }
                else{
                    Toast.makeText(this, "Bluetooth Disabled", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    /*
        connect to the device that is passed in
     */
    public void connectDevice(Intent data){
        // get the connected device's MAC address
        String addr = data.getExtras().getString(BluetoothDevicesActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = BA.getRemoteDevice(addr);
        // connect to the device
        chatService.connect(device, true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        new MenuInflater(getApplication()).inflate(R.menu.top_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        Intent intent = null;
        switch(item.getItemId()){
            case R.id.connect_devices:
                if (!BA.isEnabled()) {
                    Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(turnOn, 0);
                    Toast.makeText(getApplicationContext(), "Bluetooth Enable", Toast.LENGTH_SHORT).show();
                }
                else {
                    intent = new Intent(this, BluetoothDevicesActivity.class);
                    startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
                }
                return true;
            case R.id.reconfiguration:
                Intent reconfigure = new Intent(this, reconfigurationActivity.class);
                startActivityForResult(reconfigure, 0);
            case R.id.discoverable:
                ensureDiscoverable();
                break;
            case R.id.exit:
                BA.disable();
                System.exit(0);
                Toast.makeText(this, "exit", Toast.LENGTH_SHORT).show();
                return true;
        }
        return false;
    }

    /*
        reset the system
     */
    public void reset(View v){
        sendMessage("reset");
        for(int i = 0; i < 20; i ++){
            for(int j = 0; j < 15; j++) {
                obstacleArray[i][j] = 0;
                spArray[i][j] = 0;
            }
        }

        arena.setObstacles(obstacleArray);
        arena.setSpArray(spArray);
//        x_coordinate.setText("2");
//        y_coordinate.setText("2");
//        direction.setText("0");
        xStatus = 2;
        yStatus = 19;
        dStatus = 180;

        tConversationAA.clear();
        fConversationAA.clear();

        init();
        setRobot();

        startTimeFastest = 0L;
        startTimeExplore = 0L;
        timeInMillisecondsFastest = 0L;
        timeInMillisecondsExplore = 0L;
        timeBuffExplore = 0L;
        timeBuffFastest = 0L;
        exploreTime.setText("00:00:00");
        fastestTime.setText("00:00:00");
    }

    /*
        set the postion of the robot
     */
    public void setRobot(){
        String newPos = "{go:[";
        newPos += x_coordinate.getText().toString() + ",";
        newPos += y_coordinate.getText().toString() + ",";
        newPos += direction.getText().toString() + "]}";

        try {
            decodeString = decodeRobotString(newPos);
            updateGridArray(toIntArray(decodeString));
            Toast.makeText(getApplicationContext(), "Robot Set", Toast.LENGTH_SHORT).show();
            Log.d("setPosition: ", newPos);
        } catch (JSONException e) {
            Toast.makeText(getApplicationContext(), "Failed to set robot", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }

    public void setCoord(View view){
        setRobot();
    }


    @Override
    public void onSensorChanged(SensorEvent event){
        if(tilt == false){
            onPause();
        }
        if(event.values[0] > 4){
            turnLeft();
        }
        if(event.values[0] < -5){
            turnRight();
        }
        if(event.values[1] < 0){
            goStraight();
        }
        if(event.values[1] > 8){
            // reverse the direction
            turnLeft();
            turnLeft();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){

    }

    public void visible(){
        Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(getVisible, 0);
    }


}
