package edu.temple.testshadowskip;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener /*Implementing this lets us have a sensor event listener*/ {

    private SensorManager sensorManager; // an object that lets us get our sensors
    private Sensor gyroscope; // the actual gyroscope
    private Sensor accelerometer; // the actual accelerometer
    private Sensor magneticField; // the actual magnetometer
    private Sensor proximity; // the actual proximity sensor
    private long gyroStart; // time in seconds when the gyroscope fell into ready position
    private long elapsedSeconds; // amount of seconds gyroscope has been in ready position
    private long proxStart; // time in hundredths of a second when the proximity sensor fell into the near position
    private long proxStop; // time in hundredths of a second when the proximity sensor fell into the far position
    private TextView proxStat;  // TextView (label) that shows the status (status can be ready or not ready) of the proximity sensor
    private TextView gyroStat; // TextView (label) that shows the status (status can be ready or not ready) of the gyroscope
    private TextView orienStat; // TextView (label) that shows the status (status can be ready or not ready) of the orientation
    private boolean gyroReady; // boolean to signify ready status of the gyroscope
    private boolean orientationReady; // boolean to signify ready status of the orientation
    private boolean lastTriggerWasPlay; // boolean to signify is the last triggering of the prox read near
    private final float[] accelerometerReading = new float[3]; // readouts from the accelerometer in x y and z directions
    private final float[] magnetometerReading = new float[3]; // readouts from the magnetometer in x y and z directions
    private final float[] rotationMatrix = new float[9]; // don't worry about this, this is something android provides to get the orientation
    private final float[] orientationAngles = new float[3]; // the Azimuth, pitch, and roll of the phone
    private AudioManager am;
    private AudioManager.OnAudioFocusChangeListener af;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Android framework stuff
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        // Defining out UI elements so we can programmatically edit them.
        Button gyroscopeButton = findViewById(R.id.btn_gyro);
        Button orientationButton = findViewById(R.id.btn_orientation);
        Button proximityButton = findViewById(R.id.btn_proximity);
        gyroStat = findViewById(R.id.txt_gyro_stat);
        orienStat = findViewById(R.id.txt_orien_stat);
        proxStat = findViewById(R.id.txt_prox_stat);


        //These next three if statements check if the appropriate sensor appears on the phone. If so, it will
        //      allow the buttons to work.  They open new activities which show the sensor's current readings.
        if(sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            gyroscopeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent gyroIntent = new Intent(MainActivity.this, GyroActivity.class);
                    startActivity(gyroIntent);
                }
            });
        }
        if(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null && sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null){
            orientationButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent orientationIntent = new Intent(MainActivity.this, OrientationActivity.class);
                    startActivity(orientationIntent);
                }
            });
        }
        if(sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null) {
            proximityButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent proximityIntent = new Intent(MainActivity.this, ProximityActivity.class);
                    startActivity(proximityIntent);
                }
            });
        }
        //END BUTTON LOGIC

        // Initializing global variables.
        gyroStart = 0;
        elapsedSeconds = 0;
        proxStart = 0;
        proxStop = 0;
        gyroReady = false;
        orientationReady = false;
        lastTriggerWasPlay = false;
        Context context = this;
        am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        // Assigning listeners for each sensor type (listeners are all the onSensorChange() function)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_UI);



    }

    /**
     * METHOD onSensorChanged - Is triggered when any of the four used sensors register a change
     *                          in their state.
     * @param event - The SensorEvent that corresponds with each status change. Contains
     *                  information on what the current sensor's readout is.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        //If sensor event relates to gyroscope, run gyroChanged function.
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            gyroChanged(event);
        }
        //If sensor event relates to accelerometer, copy these arrays
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading,
                    0, accelerometerReading.length);
        }
        //If sensor isn't accelerometer but is magnetometer, copy these arrays
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading,
                    0, magnetometerReading.length);
        }
        // We update the orientation every time a sensor changes (there is probably a better way to do this).
        updateOrientationAngles();

        //If sensor event relates to proximity sensor, trigger the proxChanged function.
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY){
            proxChanged(event);
        }

    }


    /**
     * METHOD proxChanged - If orientation is ready, then the proximity sensor will be used
     *                          to play or pause music depending on states.  MUST BE CALLED
     *                          INSIDE OF onSensorChanged()
     * @param event - The SensorEvent that correlates to the proximity sensor changing.
     */
    public void proxChanged(SensorEvent event){
        // get distance readout from proximity sensor
        float distance = event.values[0];
        // get time in hundredths of a second when the sensor changed values
        long time = event.timestamp / 10000000;

        //If the phone is face up (with an implication of stability) continue execution
        if (orientationReady){
            // If hand comes close we "start" the timer
            if(distance < 5){
                proxStart = time;
            }
            // If hand moves away we "stop" the timer
            else{
                proxStop = time;
            }

            // If the time from hand entrance to exit was over 1 second we trigger the audio
            // play action. (Can only be done once the audio has been paused because we are not
            // directly accessing the external audio player).
            if(proxStop - proxStart > 100){
                proxStat.setText("Long (Play)");
                am.abandonAudioFocus(null);
            }
            // If the time from hand entering to exiting was under 1 second we pause the music.
            if ((proxStop - proxStart) < 100 && (proxStop - proxStart) > 0){
                proxStat.setText("Short (Pause)");
                am.requestAudioFocus(null,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            }
            // If the time from changes was negative (as in a hand entered, but hasn't exited) we
            // know the sensor is currently measuring the wave time.
            if (proxStop - proxStart < 0){
                proxStat.setText("Measuring...");
            }
        }
        // If the orientation of the phone (with an implication of loss stability) is not face up,
        // we set the proximity status to "not ready" and no audio actions can be triggered.
        if (!orientationReady){
            proxStat.setText("Not Ready");
        }

//        Log.println(Log.ASSERT, "start", String.valueOf(proxStart));
//        Log.println(Log.ASSERT, "stop", String.valueOf(proxStop));
//        Log.println(Log.ASSERT, "diff", String.valueOf(proxStop - proxStart));
    }

    /**
     * METHOD updateOrientationAngles - Takes the rotationMatrix and calculates the current orientation.
     *                                  this data is passed into the orientationAngles global var.
     */
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix, null,
                accelerometerReading, magnetometerReading);
        // "rotationMatrix" now has up-to-date information.
        SensorManager.getOrientation(rotationMatrix, orientationAngles);
        // "orientationAngles" now has up-to-date information.

        // get average orientation of the phone
        float avg = (orientationAngles[1] + orientationAngles[2]) / 2;
        // if the phone is in a face up position and gyro is stable, set orientation to ready
        if(gyroReady && avg < 0.1 && avg > -0.1){
            orientationReady = true;
            orienStat.setText("Ready");
        }
        // if the phone is ever not face up, set orientation to not ready
        if(!(avg < 0.1 && avg > -0.1) || !gyroReady){
            orientationReady = false;
            orienStat.setText("Not ready");
            proxStat.setText("Not Ready");
        }
    }

    /**
     * METHOD onAccuracyChanged - A required function when implementing SensorEventListener
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Empty on purpose, we don't care about accuracy.
    }

    /**
     * METHOD gyroChanged - Calculates is the gyroscope is in a ready state for more than 3 seconds
     *                      MUST BE CALLED INSIDE OF onSensorChanged FOR GYROSCOPE.
     * @param event - The SensorEvent correlated with a change in the gyroscope's readings.
     */
    //NOTES: This function does not actually trigger "continually".  The gyroscope gives "changes"
    // continually because of the small anomalous changes in gyro readings, not because it's actually
    // changing.  We leveraged this to allow for a somewhat "continuous" timer of how long the gyro
    // has been stable within a certain set value range.
    public void gyroChanged(SensorEvent event){
        //Calculate current time in seconds
        long time = event.timestamp;
        long seconds = time / (long)1000000000;
//        Log.println(Log.ASSERT, "Time", String.valueOf(seconds));
        float xValue = event.values[0]; // Current x direction rotational value of the gyroscope
        float yValue = event.values[1]; // Current y direction rotational value of the gyroscope
        float zValue = event.values[2]; // Current z direction rotational value of the gyroscope
        float avg = (xValue + yValue + zValue) / 3; // Average of the 3 rotational values

        // If the phone is stable and just became stable, start timing
        if(avg < 0.01 && avg > -0.01 && elapsedSeconds == 0) {
            gyroStart = seconds;
            elapsedSeconds = 1;
        }
        // If the phone has been stable and still is, count how long it's been
        else if (avg < 0.01 && avg > -0.01 && elapsedSeconds != 0){
            elapsedSeconds = seconds - gyroStart;
        }
        // If the phone has been stable for more than 3 seconds, set gyro to "ready"
        if (elapsedSeconds > 3){
            gyroStat.setText("Ready");
            gyroReady = true;
            elapsedSeconds = seconds - gyroStart;
        }
        // If the phone ever becomes unstable, take gyro out of ready state and signify it has not
        // been ready for any time.  Also make sure prox and orien statuses are "not ready" (this is
        // to ensure that if those sensors' values don't change, they still become unready b/c phone
        // is in motion and readings should not trigger music actions).
        if (!(avg < 0.01 && avg > -0.01)){
            gyroStat.setText("Not Ready");
            gyroReady = false;
            gyroStart = 0;
            elapsedSeconds = 0;
            proxStat.setText("Not Ready");
            orienStat.setText("Not Ready");
            orientationReady = false;
        }
    }
}