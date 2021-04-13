package edu.temple.testshadowskip;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
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
    private long gyroStart; // time in seconds when the gyroscope fell into ready position
    private long elapsedSeconds; // amount of seconds gyroscope has been in ready position
    private TextView proxStat;  // TextView (label) that shows the status (status can be ready or not ready) of the proximity sensor
    private TextView gyroStat; // TextView (label) that shows the status (status can be ready or not ready) of the gyroscope
    private TextView orienStat; // TextView (label) that shows the status (status can be ready or not ready) of the orientation
    private boolean gyroReady; // boolean to signify ready status of the gyroscope
    private boolean orientationReady;
    private final float[] accelerometerReading = new float[3]; // readouts from the accelerometer in x y and z directions
    private final float[] magnetometerReading = new float[3]; // readouts from the magnetometer in x y and z directions
    private final float[] rotationMatrix = new float[9]; // don't worry about this, this is something android provides to get the orientation
    private final float[] orientationAngles = new float[3]; // the Azimuth, pitch, and roll of the phone



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Button gyroscopeButton = findViewById(R.id.btn_gyro);
        Button orientationButton = findViewById(R.id.btn_orientation);
        Button proximityButton = findViewById(R.id.btn_proximity);
        gyroStat = findViewById(R.id.txt_gyro_stat);
        orienStat = findViewById(R.id.txt_orien_stat);
        proxStat = findViewById(R.id.txt_prox_stat);



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

        gyroStart = (long) 0;
        elapsedSeconds = 0;
        gyroReady = false;
        orientationReady = false;


        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            gyroChanged(event);
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading,
                    0, accelerometerReading.length);
        }
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading,
                    0, magnetometerReading.length);
        }
        updateOrientationAngles();

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
        }

        Log.println(Log.ASSERT, "elapsed", String.valueOf(elapsedSeconds));
        Log.println(Log.ASSERT, "gyroStart", String.valueOf(gyroStart));


    }
}