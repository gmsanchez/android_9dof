package com.NineDof.tim.ninedof;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.*;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class MyActivity extends Activity implements SensorEventListener {
    int i = 0;
    private SensorManager sensorManager;
    private Sensor accSensor, gyroSensor, magSensor;
    private long lastUpdate;
    TextView labelaccx;
    TextView labelaccy;
    TextView labelaccz;
    TextView labeldt;
    private XYPlot plot;

    SimpleXYSeries seriesAccx;
    SimpleXYSeries seriesAccy;
    SimpleXYSeries seriesAccz;
    private static final int HISTORY_SIZE = 500;

    int plotCount = 0;

    private float[] gyro = new float[3];
    private float[] magnet = new float[3];
    private float[] accel = new float[3];

    MadgwickAHRS madgwickAHRS = new MadgwickAHRS(0.01f, 0.041f);
    private Timer madgwickTimer = new Timer();
    double lpPitch=0,lpRpll=0,lpYaw=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorManager.registerListener(this, accSensor, 0);//every 5ms
        sensorManager.registerListener(this, gyroSensor, 0);
        sensorManager.registerListener(this, magSensor, 0);

        labelaccx = (TextView) findViewById(R.id.labelaccx);
        labelaccy = (TextView) findViewById(R.id.labelaccy);
        labelaccz = (TextView) findViewById(R.id.labelaccz);
        labeldt = (TextView) findViewById(R.id.labeldt);

        plot = (XYPlot) findViewById(R.id.mySimpleXYPlot);

        plot.setDomainStepValue(5);
        plot.setTicksPerRangeLabel(3);
        plot.setRangeBoundaries(-100, 100, BoundaryMode.FIXED);
        plot.setDomainBoundaries(0, 500, BoundaryMode.FIXED);
        seriesAccx = new SimpleXYSeries("accx");
        seriesAccx.useImplicitXVals();
        plot.addSeries(seriesAccx, new LineAndPointFormatter(
                Color.rgb(100, 100, 0), null, null, null));
        seriesAccy = new SimpleXYSeries("accy");
        seriesAccy.useImplicitXVals();
        plot.addSeries(seriesAccy, new LineAndPointFormatter(
                Color.rgb(0, 100, 200), null, null, null));
        seriesAccz = new SimpleXYSeries("accz");
        seriesAccz.useImplicitXVals();
        plot.addSeries(seriesAccz, new LineAndPointFormatter(
                Color.rgb(100, 0, 200), null, null, null));
        lastUpdate = System.currentTimeMillis();

        madgwickTimer.scheduleAtFixedRate(new DoMadgwick(),
                1000, 10);


    }

    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accSensor, 0);
        sensorManager.registerListener(this, gyroSensor, 0);
        sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public synchronized void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(sensorEvent.values, 0, accel, 0, 3);

          /*  Long now = System.currentTimeMillis();
            labeldt.setText(Long.toString(now - lastUpdate));
            lastUpdate = now;

            float[] accvalues = sensorEvent.values;
            //  labelaccx.setText(Double.toString(accvalues[0]));
            //  labelaccy.setText(Double.toString(accvalues[1]));
            //  labelaccz.setText(Double.toString(accvalues[2]));
            if (seriesAccx.size() > HISTORY_SIZE) {
                seriesAccx.removeFirst();
                seriesAccy.removeFirst();
                seriesAccz.removeFirst();
            }

            //add the latest history sample:
            seriesAccx.addLast(null, accvalues[0]);
           seriesAccy.addLast(null, accvalues[1]);
            seriesAccz.addLast(null, accvalues[2]);

            // redraw the Plots:
            plot.redraw();
       */
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            System.arraycopy(sensorEvent.values, 0, gyro, 0, 3);

        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(sensorEvent.values, 0, magnet, 0, 3);

        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    class DoMadgwick extends TimerTask {

        public void run() {
            Long now = System.currentTimeMillis();
            madgwickAHRS.SamplePeriod=(now - lastUpdate)/1000.0f;
            lastUpdate=now;
             madgwickAHRS.Update(gyro[0], gyro[1], gyro[2], accel[0], accel[1], accel[2], magnet[0], magnet[1], magnet[2]);
            if (seriesAccx.size() > HISTORY_SIZE) {
                seriesAccx.removeFirst();
                seriesAccy.removeFirst();
                seriesAccz.removeFirst();
            }

            //add the latest history sample:
            lpPitch=lpPitch*0.2+madgwickAHRS.MadgPitch*0.8;
            lpRpll=lpRpll*0.2+madgwickAHRS.MadgRoll*0.8;
            lpYaw=lpYaw*0.2+madgwickAHRS.MadgYaw*0.8;
              seriesAccx.addLast(null, lpPitch);
               seriesAccy.addLast(null, lpRpll);
              seriesAccz.addLast(null, lpYaw);
          //  seriesAccx.addLast(null, gyro[0]);
         //   seriesAccy.addLast(null, gyro[1]);
          //  seriesAccz.addLast(null, gyro[2]);
            plot.post(new Runnable() {
                public void run() {
            /* the desired UI update */
                 //   labelaccx.setText(Double.toString(madgwickAHRS.MadgPitch));
                  //  labelaccy.setText(Double.toString(madgwickAHRS.MadgRoll));
                  //  labelaccz.setText(Double.toString(madgwickAHRS.MadgYaw));
                   //labeldt.setText(Double.toString(madgwickAHRS.SamplePeriod));
                    plot.redraw();
                }
            });
        }

    }

}
