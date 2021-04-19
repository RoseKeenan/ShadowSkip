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
        Button gyroscopeButton = findViewById(R.id.btn_gyro);
        Button orientationButton = findViewById(R.id.btn_orientation);
        Button proximityButton = findViewById(R.id.btn_proximity);
        gyroStat = findViewById(R.id.txt_gyro_stat);
        orienStat = findViewById(R.id.txt_orien_stat);
        proxStat = findViewById(R.id.txt_prox_stat);


        //These next three if statements check if the sensor appears on the phone. If so, it will
        //      allow the buttons to work.
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




        gyroStart = 0;
        elapsedSeconds = 0;
        proxStart = 0;
        proxStop = 0;
        gyroReady = false;
        orientationReady = false;
        lastTriggerWasPlay = false;

        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_UI);

        Context context = this;
        am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        af = new AudioManager.OnAudioFocusChangeListener() {

            public void onAudioFocusChange(int focusChange) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    // Lower the volume
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    // Raise it back to normal
                }
            }

        };
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //If sensor event relates to gyroscope, run gyroChanged function/
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
        updateOrientationAngles();

        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY){
            proxChanged(event);
        }

    }


    public void proxChanged(SensorEvent event){
        float distance = event.values[0];
        long time = event.timestamp / 10000000;

        if (orientationReady){
            if(distance < 5){
                proxStart = time;
            }
            else{
                proxStop = time;
            }

            if(proxStop - proxStart > 100){
                proxStat.setText("Long");
                am.abandonAudioFocus(null);
            }
            if ((proxStop - proxStart) < 100 && (proxStop - proxStart) > 0){
                proxStat.setText("Short");
                am.requestAudioFocus(null,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            }
            if (proxStop - proxStart < 0){
                proxStat.setText("Measuring...");
            }
        }
        if (!orientationReady){
            proxStat.setText("Not Ready");
        }

//        Log.println(Log.ASSERT, "start", String.valueOf(proxStart));
//        Log.println(Log.ASSERT, "stop", String.valueOf(proxStop));
//        Log.println(Log.ASSERT, "diff", String.valueOf(proxStop - proxStart));
    }

    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix, null,
                accelerometerReading, magnetometerReading);

        // "rotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(rotationMatrix, orientationAngles);

        // "orientationAngles" now has up-to-date information.
        float avg = (orientationAngles[1] + orientationAngles[2]) / 2;
        if(gyroReady && avg < 0.1 && avg > -0.1){
            orientationReady = true;
            orienStat.setText("Ready");
        }
        if(!(avg < 0.1 && avg > -0.1) || !gyroReady){
            orientationReady = false;
            orienStat.setText("Not ready");
            proxStat.setText("Not Ready");
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void gyroChanged(SensorEvent event){
        long time = event.timestamp;
        long seconds = time / (long)1000000000;
//        Log.println(Log.ASSERT, "Time", String.valueOf(seconds));
        float xValue = event.values[0]; // Current rotational values of the gyroscope
        float yValue = event.values[1];
        float zValue = event.values[2];
        float avg = (xValue + yValue + zValue) / 3; // Average of the 3 rotational values

        if(avg < 0.01 && avg > -0.01 && elapsedSeconds == 0) {
            gyroStart = seconds;
            elapsedSeconds = 1;
        }
        else if (avg < 0.01 && avg > -0.01 && elapsedSeconds != 0){
            elapsedSeconds = seconds - gyroStart;
        }

        if (elapsedSeconds > 5){
            gyroStat.setText("Ready");
            gyroReady = true;
            elapsedSeconds = seconds - gyroStart;
        }

        if (!(avg < 0.01 && avg > -0.01)){
            gyroStat.setText("Not Ready");
            gyroReady = false;
            gyroStart = 0;
            elapsedSeconds = 0;
            proxStat.setText("Not Ready");
            orienStat.setText("Not Ready");
            orientationReady = false;

        }

//        Log.println(Log.ASSERT, "elapsed", String.valueOf(elapsedSeconds));
//        Log.println(Log.ASSERT, "gyroStart", String.valueOf(gyroStart));
    }



}