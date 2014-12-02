package com.backyardbrains.roboroach;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.ToggleButton;
import 	android.widget.ViewFlipper;
import android.view.ViewGroup;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.SystemClock;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.*;
import com.androidplot.xy.XYPlot;
import com.androidplot.Plot;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.*;
import com.jjoe64.graphview.*;
import com.jjoe64.graphview.GraphView.GraphViewData;
import java.text.DecimalFormat;
import java.util.Observable;
import java.util.Observer;

public class RoboRoachActivity extends Activity implements RoboRoachManagerCallbacks {
    private static final long SCANNING_TIMEOUT = 5 * 1000; /* 5 seconds */
    private static final int ENABLE_BT_REQUEST_ID = 1;

    private final static String TAG = RoboRoachActivity.class.getSimpleName();
    private boolean mScanning = false;
    private boolean mTurning = false;
    private boolean mOnSettingsScreen = false;
    private boolean mAdvanced = false;
    private boolean connected = false;

    private String mDeviceAddress;

    private Handler mHandler = new Handler();
    private RoboRoachManager mRoboRoachManager = null;
    private ViewHolder viewHolder;
    private Runnable mGATTUpdate;
    private int mGATTFreq = 0;

    private GestureDetector gestureDetector;

    Runnable scanTimeout = new Runnable() {
        @Override
        public void run() {
            if(mRoboRoachManager == null) return;
            mScanning = false;
            mRoboRoachManager.stopScanning();
            invalidateOptionsMenu();
        }
    };
    boolean first = true;
    private Accelerometer accel;
    private XYPlot dynamicPlot;
   // private MyPlotUpdater plotUpdater;
    private ToggleButton accelSwitch;
    private ToggleButton btnRecord;
    private RoboRoachLog eventLog;
    private boolean recording = false;
    boolean plotDisplayed = false;
    protected void toggleAccel(){
        if(mRoboRoachManager.isAccelEnabled())
        mRoboRoachManager.toggleAccel();
        mRoboRoachManager.requestAccelValues();
    }
    GraphView graphView;
    GraphViewSeries xSeries;
    GraphViewSeries ySeries;

    protected void initializePlot(){
        dynamicPlot = (XYPlot) findViewById(R.id.dynamicXYPlot);
        accelSwitch = (ToggleButton) findViewById(R.id.btnAccel);
        btnRecord = (ToggleButton) findViewById(R.id.btnRecord);
        accelSwitch.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(connected)
                    mRoboRoachManager.setAccelEnabled(isChecked);
                accelSwitch.setChecked(mRoboRoachManager.isAccelEnabled());

            }
        });
        btnRecord.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                recording = isChecked;
                if(recording){
                    eventLog = new RoboRoachLog();
                }
                if(!recording){
                  /*  File file   = null;
                    File root   = Environment.getExternalStorageDirectory();
                    if (root.canWrite()){
                        File dir    =   new File (root.getAbsolutePath() + "/RoboRoach");
                        dir.mkdirs();
                        file   =   new File(dir, "Data.csv");
                        FileOutputStream out   =   null;
                        try {
                            out = new FileOutputStream(file);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        try {
                            out.write(eventLog.toString().getBytes());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } */


//                    Uri u1                          =   Uri.fromFile(file);

                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_SUBJECT, "RoboRoach Data");
                    sendIntent.putExtra(Intent.EXTRA_TEXT, eventLog.toString());
                    sendIntent.setType("text/richtext");
                    startActivity(sendIntent);
                }
            }
        });


        //  accel.pushX(0);
        //  accel.pushY(0);
        // only display whole numbers in domain labels
        dynamicPlot.getGraphWidget().setDomainValueFormat(new DecimalFormat("0"));

        // getInstance and position datasets:
        AccelSeries xseries = new AccelSeries(accel, 0, "X");
        AccelSeries yseries = new AccelSeries(accel, 1, "Y");

        LineAndPointFormatter formatter1 = new LineAndPointFormatter(
                Color.rgb(0, 0, 0), null, null, null);
        formatter1.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
        formatter1.getLinePaint().setStrokeWidth(10);
        dynamicPlot.addSeries(xseries,
                formatter1);

        LineAndPointFormatter formatter2 =
                new LineAndPointFormatter(Color.rgb(0, 0, 200), null, null, null);
        formatter2.getLinePaint().setStrokeWidth(10);
        formatter2.getLinePaint().setStrokeJoin(Paint.Join.ROUND);

        //formatter2.getFillPaint().setAlpha(220);
        dynamicPlot.addSeries(yseries, formatter2);


        // thin out domain tick labels so they dont overlap each other:
        dynamicPlot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        dynamicPlot.setDomainStepValue(5);

        dynamicPlot.setRangeStepMode(XYStepMode.INCREMENT_BY_VAL);
        dynamicPlot.setRangeStepValue(10);

        dynamicPlot.setRangeValueFormat(new DecimalFormat("###.#"));

        // uncomment this line to freeze the range boundaries:
        dynamicPlot.setRangeBoundaries(-100, 100, BoundaryMode.FIXED);

        // create a dash effect for domain and range grid lines:
        DashPathEffect dashFx = new DashPathEffect(
                new float[] {PixelUtils.dpToPix(3), PixelUtils.dpToPix(3)}, 0);
        dynamicPlot.getGraphWidget().getDomainGridLinePaint().setPathEffect(dashFx);
        dynamicPlot.getGraphWidget().getRangeGridLinePaint().setPathEffect(dashFx);



    }
    // Accelerometer has been updated, display new values.
    public void onAccelUpdated(){
        if(plotDisplayed) {
            ProgressBar prX = (ProgressBar) findViewById(R.id.progressX);
            ProgressBar prY = (ProgressBar) findViewById(R.id.progressY);
            prX.setProgress(accel.getX());
            prY.setProgress(accel.getY());
            dynamicPlot.redraw();
        }
        if(recording){
            eventLog.write(accel);
        }

    }
    protected void showPlot(){
        ViewFlipper vf = (ViewFlipper) findViewById(R.id.viewFlipper);
        vf.setDisplayedChild(2);
        plotDisplayed = true;
       /* if(first) {
            first = false;
            initializePlot(); */



      //  dynamicPlot.redraw();


    }
    protected void hidePlot(){
        plotDisplayed = false;
        ViewFlipper vf = (ViewFlipper) findViewById(R.id.viewFlipper);
        vf.setDisplayedChild(0);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");


        setContentView(R.layout.roboroach_main);
      /*  // initialize our XYPlot reference:
        plot = (XYPlot) findViewById(R.id.mySimpleXYPlot);

        // Create a couple arrays of y-values to plot:
        Number[] series1Numbers = {1, 8, 5, 2, 7, 4};
        Number[] series2Numbers = {4, 6, 3, 8, 2, 10};

        // Turn the above arrays into XYSeries':
        XYSeries series1 = new SimpleXYSeries(
                Arrays.asList(series1Numbers),          // SimpleXYSeries takes a List so turn our array into a List
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // Y_VALS_ONLY means use the element index as the x value
                "Series1");                             // Set the display title of the series

        // same as above
        XYSeries series2 = new SimpleXYSeries(Arrays.asList(series2Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Series2");

        // Create a formatter to use for drawing a series using LineAndPointRenderer
        // and configure it from xml:
       /* LineAndPointFormatter series1Format = new LineAndPointFormatter();
        series1Format.setPointLabelFormatter(new PointLabelFormatter());
        series1Format.configure(getApplicationContext(),
                R.xml.line_point_formatter_with_plf1); */
       /* LineAndPointFormatter series1Format = new LineAndPointFormatter(Color.RED, Color.GREEN, Color.BLUE, null);

        // add a new series' to the xyplot:
        plot.addSeries(series1, series1Format);

        // same as above:
       /* LineAndPointFormatter series2Format = new LineAndPointFormatter();
        series2Format.setPointLabelFormatter(new PointLabelFormatter());
        series2Format.configure(getApplicationContext(),
                R.xml.line_point_formatter_with_plf2);
        plot.addSeries(series2, series2Format); */
     /*   LineAndPointFormatter series2Format = new LineAndPointFormatter(Color.RED, Color.GREEN, Color.BLUE, null);
        // reduce the number of range labels
        plot.setTicksPerRangeLabel(3);
        plot.getGraphWidget().setDomainLabelOrientation(-45);

        GraphViewSeries xSeries = new GraphViewSeries(new GraphViewData[] {
                new GraphViewData(0, 0)
        });
        GraphViewSeries ySeries = new GraphViewSeries(new GraphViewData[] {
                new GraphViewData(0, 0)
        });
        graphView = new LineGraphView(
                this // context
                , "Acceleration" // heading
        );
        graphView.addSeries(xSeries);
        graphView.addSeries(ySeries);

        LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
        layout.addView(graphView);
        */
        mRoboRoachManager = new RoboRoachManager(this, this);
        accel = new Accelerometer(this);
        mRoboRoachManager.setAccel(accel);
        initializePlot();

        // check if we have BT and BLE on board
        if(mRoboRoachManager.checkBleHardwareAvailable() == false) {
            bleMissing();
        }
        accelSwitch = (ToggleButton) findViewById(R.id.btnAccel);

        accelSwitch.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(connected)
                    mRoboRoachManager.setAccelEnabled(isChecked);
                accelSwitch.setChecked(mRoboRoachManager.isAccelEnabled());

            }
        });

        viewHolder = new ViewHolder();
        viewHolder.roachImage = (ImageView) findViewById(R.id.imageRoach);
        viewHolder.backpackImage = (ImageView) findViewById(R.id.imageBackpack);
        viewHolder.goLeftText = (TextView) findViewById(R.id.textGoLeft);
        viewHolder.goRightText = (TextView) findViewById(R.id.textGoRight);
        viewHolder.configText = (TextView) findViewById(R.id.textConfig);
        viewHolder.Duration = (SeekBar) findViewById(R.id.sbDuration);
        viewHolder.Gain  = (SeekBar) findViewById(R.id.sbGain);
        viewHolder.Frequecy = (SeekBar) findViewById(R.id.sbFrequency);
        viewHolder.PulseWidth = (SeekBar) findViewById(R.id.sbPulseWidth);
        viewHolder.RandomMode = (Switch) findViewById(R.id.swRandomMode);
        viewHolder.Advanced = (CheckBox) findViewById(R.id.chkAdvanced);
        viewHolder.Left = (CheckBox) findViewById(R.id.chkLeft);
        viewHolder.Right = (CheckBox) findViewById(R.id.chkRight);
        viewHolder.Send = (Button) findViewById(R.id.btnSend);
        viewHolder.roachImage.setVisibility(View.VISIBLE);
        viewHolder.backpackImage.setVisibility(View.INVISIBLE);


        viewHolder.Advanced.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener(){
            public void onCheckedChanged(CompoundButton checkBox, boolean fromUser){
                setAdvanced(checkBox.isChecked());
            }
        });

        viewHolder.Send.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View button) {
                if (mAdvanced) {
                    if (viewHolder.Left.isChecked() && viewHolder.Right.isChecked()) {
                        mRoboRoachManager.stimBoth();
                        if(recording)
                            eventLog.write("Stimulating Both");
                    } else {
                        if (viewHolder.Left.isChecked()) {
                            mRoboRoachManager.turnLeft();
                            eventLog.write("Stimulating Left");

                        }
                        SystemClock.sleep(200);
                        if (viewHolder.Right.isChecked()) {
                            mRoboRoachManager.turnRight();
                            eventLog.write("Stimulating Right");
                        }
                    }
                    onSend();

                }

            }
        });
        viewHolder.Frequecy.setMax(150);
        viewHolder.Frequecy.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {

                mGATTFreq = seekBar.getProgress();
                if ( mGATTFreq < 1 ) mGATTFreq = 1;

                //If the new freq it's greater than 1/2 the
                if ((float) mRoboRoachManager.getRoboRoachPulseWidth() > (float)500/mGATTFreq )
                {
                    mGATTUpdate = new Runnable() {
                        @Override
                        public void run() {

                            float newFreq  = (float) (1000/mGATTFreq);
                            int newPW = (int)  newFreq/2;
                            mRoboRoachManager.updatePulseWidth(newPW);
                            viewHolder.configText.setText(mRoboRoachManager.getRoboRoachConfigurationString());

                        }
                    };
                    mHandler.postDelayed(mGATTUpdate, 500);
                }

                viewHolder.PulseWidth.setMax(1000 / mGATTFreq);
                mRoboRoachManager.updateFrequency(mGATTFreq);
                viewHolder.configText.setText(mRoboRoachManager.getRoboRoachConfigurationString());


            }
        });
        viewHolder.Gain.setMax(100);
        viewHolder.Gain.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {

                float roundedGain = Math.round((float) seekBar.getProgress() / 5.0f) * 5.0f;
                mRoboRoachManager.updateGain((int) roundedGain);
                viewHolder.configText.setText(mRoboRoachManager.getRoboRoachConfigurationString());
            }
        });

        viewHolder.RandomMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mRoboRoachManager.isConnected()) {

                    mRoboRoachManager.updateRandomMode(isChecked);
                    viewHolder.PulseWidth.setEnabled(!viewHolder.RandomMode.isChecked());
                    viewHolder.Frequecy.setEnabled(!viewHolder.RandomMode.isChecked());
                    viewHolder.configText.setText(mRoboRoachManager.getRoboRoachConfigurationString());
                }
            }
        });
         
        viewHolder.PulseWidth.setMax(50);
        viewHolder.PulseWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                mRoboRoachManager.updatePulseWidth(seekBar.getProgress());
                viewHolder.configText.setText( mRoboRoachManager.getRoboRoachConfigurationString());
            }
        });


        viewHolder.Duration.setMax(1000);
        viewHolder.Duration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                float roundedDuration = Math.round((float) seekBar.getProgress() / 10.0f) * 10.0f;
                //if ( roundedDuration < 10 ) roundedDuration = 10;

                mRoboRoachManager.updateDuration((int) roundedDuration);
                viewHolder.configText.setText(mRoboRoachManager.getRoboRoachConfigurationString());
            }
        });

        Typeface typeFace=Typeface.createFromAsset(getAssets(),"fonts/comicbook.ttf");
        viewHolder.goLeftText.setTypeface(typeFace);
        viewHolder.goRightText.setTypeface(typeFace);
        viewHolder.configText.setTypeface(typeFace);

        viewHolder.configText.setText("");

        final Button button = (Button) findViewById(R.id.btnSaveSettings);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "Updating RoboRoach Settings");
                mRoboRoachManager.updateFrequency(viewHolder.Frequecy.getProgress());



                // Perform action on click
                ViewFlipper vf = (ViewFlipper) findViewById( R.id.viewFlipper );
                vf.setDisplayedChild(0);
                mOnSettingsScreen = false;
            }
        });



        gestureDetector = new GestureDetector( this.getBaseContext(),
                new SwipeGestureDetector());

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

    }

    public void setAdvanced(boolean isAdv){
        mAdvanced = isAdv;
        if(isAdv){
            viewHolder.goLeftText.setVisibility(View.GONE);
            viewHolder.goRightText.setVisibility(View.GONE);
            viewHolder.Left.setVisibility(View.VISIBLE);
            viewHolder.Right.setVisibility(View.VISIBLE);
            viewHolder.Send.setVisibility(View.VISIBLE);
        }
        else{
            viewHolder.goLeftText.setVisibility(View.VISIBLE);
            viewHolder.goRightText.setVisibility(View.VISIBLE);
            viewHolder.Left.setVisibility(View.GONE);
            viewHolder.Right.setVisibility(View.GONE);
            viewHolder.Send.setVisibility(View.GONE);
        }
    }
    public void updateSettingConstraints() {


        //if ( self.roboRoach.randomMode.boolValue ){
        //    [freqSlider setEnabled:NO];
        //    [pulseWidthSlider setEnabled:NO];
        //}else{
        //    [freqSlider setEnabled:YES];
        //    [pulseWidthSlider setEnabled:YES];
        //}

    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");

        // on every Resume check if BT is enabled (user could turn it off while app was in background etc.)
        if(mRoboRoachManager.isBtEnabled() == false) {
            // BT is not turned on - ask user to make it enabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, ENABLE_BT_REQUEST_ID);
            // see onActivityResult to check what is the status of our request
        }

        if(!mRoboRoachManager.initialize()) {
            finish();
        }
        invalidateOptionsMenu();

    };
    protected void onSend(){
        Toast.makeText(this, "Sending Signal", Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");



        if (mRoboRoachManager.isConnected()) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRoboRoachManager.stopMonitoringRssiValue();
                    mRoboRoachManager.disconnect();
                    mRoboRoachManager.close();
                }
            });

        } else if (mScanning) {
            mScanning = false;
            mRoboRoachManager.stopScanning();
        }

        invalidateOptionsMenu();

    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (mRoboRoachManager.isConnected()){
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(null);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            menu.findItem(R.id.menu_settings).setVisible(true);

        }
        else{
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_settings).setVisible(false);

            if (mScanning) {
                menu.findItem(R.id.menu_stop).setVisible(true);
                menu.findItem(R.id.menu_scan).setVisible(false);
                menu.findItem(R.id.menu_refresh).setActionView(
                        R.layout.actionbar_progress);
            } else {
                menu.findItem(R.id.menu_stop).setVisible(false);
                menu.findItem(R.id.menu_scan).setVisible(true);
                menu.findItem(R.id.menu_refresh).setActionView(null);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mScanning = true;
                invalidateOptionsMenu();
                mRoboRoachManager.startScanning();
                break;
            case R.id.menu_stop:
                mScanning = false;
                mRoboRoachManager.stopScanning();
                invalidateOptionsMenu();
                break;
            case R.id.menu_disconnect:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mRoboRoachManager.disconnect();
                        mRoboRoachManager.close();
                        if (mOnSettingsScreen) {
                            ViewFlipper vf = (ViewFlipper) findViewById( R.id.viewFlipper );
                            vf.setDisplayedChild(0);
                            mOnSettingsScreen = false;
                        }
                            invalidateOptionsMenu();
                    }
                });
                break;
            case R.id.menu_settings:
                if (!mOnSettingsScreen) {
                ViewFlipper vf = (ViewFlipper) findViewById( R.id.viewFlipper );
                vf.setDisplayedChild(1);
                mOnSettingsScreen = true;
                }
                break;
            case R.id.menu_accel:
                if(!plotDisplayed) showPlot();
                else               hidePlot();
                break;
        }
        return true;
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_roboroach_main, container, false);
            return rootView;
        }
    }

    public void uiDeviceConnected(final BluetoothGatt gatt,
                                  final BluetoothDevice device)
    {
        connected = true;
        Log.d(TAG, "uiDeviceConnected()");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //mDeviceStatus.setText("connected");
                viewHolder.backpackImage.setImageAlpha(150);
                viewHolder.backpackImage.setVisibility(View.VISIBLE);
                if(accelSwitch != null) accelSwitch.setEnabled(true);
                invalidateOptionsMenu();
            }
        });
    }

    public void uiDeviceDisconnected(final BluetoothGatt gatt,
                                     final BluetoothDevice device)
    {
        connected = false;
        Log.d(TAG, "uiDeviceDisconnected()");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewHolder.configText.setText("");
                viewHolder.backpackImage.setVisibility(View.INVISIBLE);
            if(accelSwitch != null) accelSwitch.setEnabled(false);
            }
        });

        invalidateOptionsMenu();
    }

    @Override
    public void uiServicesFound() {


        mRoboRoachManager.requestRoboRoachParameters();
    }

    @Override
    public void uiRoboRoachPropertiesUpdated() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                viewHolder.configText.setText( mRoboRoachManager.getRoboRoachConfigurationString());
                viewHolder.backpackImage.setImageAlpha(255);
                viewHolder.backpackImage.setVisibility(View.VISIBLE);

                viewHolder.Frequecy.setProgress(mRoboRoachManager.getRoboRoachFrequency());
                viewHolder.Gain.setProgress(mRoboRoachManager.getRoboRoachGain());
                viewHolder.PulseWidth.setProgress(mRoboRoachManager.getRoboRoachPulseWidth());
                viewHolder.Duration.setProgress(mRoboRoachManager.getRoboRoachDuration());
                viewHolder.RandomMode.setChecked( mRoboRoachManager.getRoboRoachRandomMode());

                viewHolder.PulseWidth.setEnabled(!viewHolder.RandomMode.isChecked());
                viewHolder.Frequecy.setEnabled(!viewHolder.RandomMode.isChecked());

                invalidateOptionsMenu();
            }
        });
    }

    @Override
    public void uiDeviceFound(BluetoothDevice device, int rssi, byte[] record) {
        Log.d(TAG, "uiDeviceFound()");

        if(mHandler!=null){
            mHandler.removeCallbacks(scanTimeout);
        }

        mDeviceAddress = device.getAddress();


        if (device == null) return;

        if (device.getName().equals("RoboRoach")) {

            Log.d(TAG, "uiDeviceFound() ...Found a RoboRoach!");

            // adding to the UI have to happen in UI thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    viewHolder.backpackImage.setImageAlpha(60);  //slowly builds up until connection
                    viewHolder.backpackImage.setVisibility(View.VISIBLE);

                    Log.d(TAG, "uiDeviceFound() ... mDeviceAddress = " + mDeviceAddress);

                    if (mScanning) {
                        mScanning = false;
                        invalidateOptionsMenu();
                        mRoboRoachManager.stopScanning();
                        Log.d(TAG, "uiDeviceFound() ... mRoboRoachManager.stopScanning()");

                    }

                    Log.d(TAG, "uiDeviceFound() ... about to call mRoboRoachManager.connect()");
                    mRoboRoachManager.connect(mDeviceAddress);
                    Log.d(TAG, "uiDeviceFound() ... finished calling mRoboRoachManager.connect()");
                }
            });



        } else {

            Log.d(TAG, "uiDeviceFound() ... Found a non-RoboRoach :( !");

        }
    }


    static class ViewHolder {
        TextView goLeftText;
        TextView goRightText;
        TextView configText;
        ImageView roachImage;
        ImageView backpackImage;

        ToggleButton AccelSwitch;
        CheckBox Left;
        CheckBox Right;
        SeekBar Frequecy;
        CheckBox Advanced;
        SeekBar Duration;
        SeekBar PulseWidth;
        SeekBar Gain;
        Switch  RandomMode;
        Button Send;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }


    private void onLeftSwipe() {
        // Do something
        Log.d(TAG, "onLeftSwipe()");
        mRoboRoachManager.requestAccelValues();
        dynamicPlot.redraw();
        if (mRoboRoachManager.isConnected() && !mOnSettingsScreen) {
            if(!mTurning){
                mRoboRoachManager.turnLeft();
                if(recording)
                    eventLog.write("Stimulating Left");
            }
        }
    }
   
  /* private void onUpSwipe() {
        // Do something
        Log.d(TAG, "onUpSwipe()");
        if (mRoboRoachManager.isConnected() && !mOnSettingsScreen) {
            if(!mTurning) mRoboRoachManager.turnUp();
        }
    }
        private void onDownSwipe() {
            // Do something
            Log.d(TAG, "onDownSwipe()");
            if (mRoboRoachManager.isConnected() && !mOnSettingsScreen) {
                if(!mTurning) mRoboRoachManager.turnDown();
            }
        } */
    private void onRightSwipe() {
        // Do something
        Log.d(TAG, "onRightSwipe()");
        if (mRoboRoachManager.isConnected() && !mOnSettingsScreen) {
            if(!mTurning){
                mRoboRoachManager.turnRight();
                if(recording)
                    eventLog.write("Stimulating Right");

            }
        }
        showPlot();
    }

    @Override
    public void uiLeftTurnSentSuccessfully(final int stimulusDuration) {
        //if(!mAdvanced) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    viewHolder.goLeftText.setVisibility(View.VISIBLE);
                    mTurning = true;
                    addTurnCommandTimeout(stimulusDuration);
                }
            });
       // }
    }

    @Override
    public void uiRightTurnSentSuccessfully(final int stimulusDuration) {
      //  if(!mAdvanced) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    viewHolder.goRightText.setVisibility(View.VISIBLE);
                    mTurning = true;
                    addTurnCommandTimeout(stimulusDuration);
                }
            });
        //}
    }


    /* make sure that potential scanning will take no longer
 * than <SCANNING_TIMEOUT> seconds from now on */
    private void addTurnCommandTimeout( int timeoutInMS ) {
        Runnable timeout = new Runnable() {
            @Override
            public void run() {
                viewHolder.goRightText.setVisibility(View.INVISIBLE);
                viewHolder.goLeftText.setVisibility(View.INVISIBLE);
                mTurning = false;
            }
        };
        mHandler.postDelayed(timeout, timeoutInMS);
    }

    /* make sure that potential scanning will take no longer
* than <SCANNING_TIMEOUT> seconds from now on */
    private void addScanningTimeout() {

        mHandler.postDelayed(scanTimeout, SCANNING_TIMEOUT);
    }


    /* check if user agreed to enable BT */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // user didn't want to turn on BT
        if (requestCode == ENABLE_BT_REQUEST_ID) {
            if(resultCode == Activity.RESULT_CANCELED) {
                btDisabled();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void btDisabled() {
        Toast.makeText(this, "Sorry, Your bluetooth needs to be turned ON for your RoboRoach to work!", Toast.LENGTH_LONG).show();
        finish();
    }

    private void bleMissing() {
        Toast.makeText(this, "BLE Hardware is required for your RoboRoach. Please try on another device.", Toast.LENGTH_LONG).show();
        finish();
    }

    // Private class for gestures
    private class SwipeGestureDetector
            extends SimpleOnGestureListener {
        // Swipe properties, you can change it to make the swipe
        // longer or shorter and speed
        private static final int SWIPE_MIN_DISTANCE = 60;
        private static final int SWIPE_MAX_OFF_PATH = 200;
        private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2,
                               float velocityX, float velocityY) {
            if(!mAdvanced) {
                try {
                    float diffy = e1.getY() - e2.getY();
                    float diff = e1.getX() - e2.getX();

                  //  if (diffAbs > SWIPE_MAX_OFF_PATH)
                   //    return false;
                 /*   if (diffy > SWIPE_MIN_DISTANCE
                            && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                        RoboRoachActivity.this.onUpSwipe();
                        if (-diffy > SWIPE_MIN_DISTANCE
                                && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                            RoboRoachActivity.this.onDownSwipe();
                    // Left swipe */
                    if (diff > SWIPE_MIN_DISTANCE
                            && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                        RoboRoachActivity.this.onLeftSwipe();

                        // Right swipe
                    } else if (-diff > SWIPE_MIN_DISTANCE
                            && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                        RoboRoachActivity.this.onRightSwipe();
                    }}
                           catch (Exception e) {
                    Log.e("RoboRoachActivity", "Error on gestures");
                }
                return false;
            }
            return false;
        }
 
    }


    class AccelSeries implements XYSeries{
        private Accelerometer accel;
        private int seriesIndex;
        private String title;

        public AccelSeries(Accelerometer accel, int seriesIndex, String title) {
            this.accel = accel;
            this.seriesIndex = seriesIndex;
            this.title = title;
        }
        public String getTitle() {
            return title;
        }

        @Override
        public int size() {
            return 10;
        }

        @Override
        public Number getX(int index) {
            return index;
        }

        @Override
        public Number getY(int index) {
            if(seriesIndex == 0)
                return accel.getX(index);
            else
                return accel.getY(index);
        }
    }
}