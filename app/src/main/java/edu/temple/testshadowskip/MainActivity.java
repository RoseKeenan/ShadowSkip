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

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor gyroscope;
    private Sensor accelerometer;
    private Sensor magneticField;
    private long gyroStart;
    private long elapsedSeconds;
    private TextView proxStat;
    private TextView gyroStat;
    private TextView orienStat;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

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

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            gyroChanged(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void gyroChanged(SensorEvent event){
        long time = event.timestamp;
        long seconds = time / (long)1000000000;
//        Log.println(Log.ASSERT, "Time", String.valueOf(seconds));
        float xValue = event.values[0];
        float yValue = event.values[1];
        float zValue = event.values[2];
        float avg = (xValue + yValue + zValue) / 3;

        if(avg < 0.01 && avg > -0.01 && elapsedSeconds == 0) {
            gyroStart = seconds;
            elapsedSeconds = 1;
        }
        else if (avg < 0.01 && avg > -0.01 && elapsedSeconds != 0){
            elapsedSeconds = seconds - gyroStart;
        }

        if (elapsedSeconds > 5){
            gyroStat.setText("Ready");
            elapsedSeconds = seconds - gyroStart;
        }

        if (!(avg < 0.01 && avg > -0.01)){
            gyroStat.setText("Not Ready");
            gyroStart = 0;
            elapsedSeconds = 0;
        }

        Log.println(Log.ASSERT, "elapsed", String.valueOf(elapsedSeconds));
        Log.println(Log.ASSERT, "gyroStart", String.valueOf(gyroStart));


    }
}