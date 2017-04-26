package com.example.bing2013.draft2_linear_acceleration;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.view.View.OnClickListener;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationServices;
import com.opencsv.CSVWriter;

public class MainActivity extends AppCompatActivity implements OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // The textview is for displaying vectors in Device Coordinate and World Coordinate.
    private TextView xText, yText, zText;

    // The counter for files
    private int linearCounter = 0;
    private int locationCounter = 0;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // Location Permissions
    private static final int REQUEST_LOCATION = 1;
    private static String[] PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    // Manager and listener for motion sensor
    private SensorManager sensorManager;
    private SensorEventListener linearSensorEventListener;


    // Manager and listener for location
    private LocationManager locationManager;
    private LocationListener locationListener;
    private String gpsProvider;
    private String networkProvider;

    // The button controls when the program stop saving data to csv file
    private Button startButton, stopButton, bumpButton, gpsNetButton, gpsButton, saveButton, button2;

    // Animation for pressing the bump button
    Animation myAnim;

    // If the sensor is started, then it will be true, otherwise it is false
    private boolean started = false;

    // ArrayList that stores all the data
    List<String[]> linearData;
    List<String[]> gpsData;
    List<String[]> networkData;
    List<String[]> bumpData;
    List<String[]> button2Data;

    // Instance variables created for calculation inside of onSensorChanged
    long initialTime;
    long preTime;
    float[] preXyz;
    float[] preDxyzDivDt;
    float[] preXyzPos;

    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;

    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {
        //TODO
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //TODO
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check the permission of saving file into Android Device.
        // If it does not have permission, it will go ahead request one.
        verifyStoragePermissions(this);

        // Check the permission of location updates into Android Device.
        // If it does not have permission, it will go ahead request one.
        verifyLocationPermissions(this);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }

        // Initialize Textviews
        xText = (TextView) findViewById(R.id.xText);
        yText = (TextView) findViewById(R.id.yText);
        zText = (TextView) findViewById(R.id.zText);

        // Initialize the list of string array
        linearData = new ArrayList<>();
        gpsData = new ArrayList<>();
        networkData = new ArrayList<>();
        bumpData = new ArrayList<>();
        button2Data = new ArrayList<>();

        // Initialize the stop button
        startButton = (Button) findViewById(R.id.startButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        bumpButton = (Button) findViewById(R.id.bumpButton);
        gpsNetButton = (Button) findViewById(R.id.gpsNetButton);
        gpsButton = (Button) findViewById(R.id.gpsButton);
        saveButton = (Button) findViewById(R.id.saveButton);
        button2 = (Button) findViewById(R.id.button2);

        // Set buttons' listener
        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        bumpButton.setOnClickListener(this);
        gpsNetButton.setOnClickListener(this);
        gpsButton.setOnClickListener(this);
        saveButton.setOnClickListener(this);
        button2.setOnClickListener(this);

        // Set the enabled state of this view.
        // In the beginning, we would like user to choose the environment, then start recording.
        startButton.setEnabled(false);
        stopButton.setEnabled(false);
        bumpButton.setEnabled(false);
        gpsNetButton.setEnabled(true);
        gpsButton.setEnabled(true);
        saveButton.setEnabled(false);
        button2.setEnabled(false);

        // Animation for pressing the bump button
        myAnim = AnimationUtils.loadAnimation(this, R.anim.milkshake);

        // Acquire a reference to the system Sensor Manager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Initialize variables
        initialTime = 0L;
        preTime = 0L;
        preXyz = new float[3];
        preDxyzDivDt = new float[3];
        preXyzPos = new float[3];


        // Define a listener that responds to sensor changes
        linearSensorEventListener = new SensorEventListener() {

            @Override
            public void onSensorChanged(SensorEvent event) {

                // Called when the sensor changed

                long systemCurrentTime = System.currentTimeMillis();
                // system current Time - initial time
                long currentTime = 0L;
                // the time interval between current time and previous time
                long dt = 0L;

                // Calculate the current time, and time interval
                if (initialTime == 0) {
                    initialTime = systemCurrentTime;
                } else {
                    currentTime = systemCurrentTime - initialTime;
                    dt = currentTime - preTime;
                }
                preTime = currentTime;

                float dtFloat = (float) dt / (float) 1000;

                // Reformat acceleration in X, Y, and Z from m/s^2 to mm/s^2
                float[] xyz = new float[3];
                for (int i = 0; i < 3; i++) {
                    xyz[i] = event.values[i] * 1000;
                }

                float[] dxyzDivDt = new float[3];

                float[] dxyz = new float[3];

                float[] xyzPos = new float[3];

                int projectedBump = 0;

                // Calculate dx/dt, dy/dt, and dz/dt
                if (preXyz[2] != 0.0) {

                    for (int i = 0; i < 3; i++) {
                        dxyzDivDt[i] = (preXyz[i] - xyz[i]) / dtFloat;
                    }
                }
                preXyz = xyz;

                // Calculate dx, dy, and dz
                if (preDxyzDivDt[2] != 0.0) {

                    for (int i = 0; i < 3; i++) {
                        dxyz[i] = (preDxyzDivDt[i] - dxyzDivDt[i]) / dtFloat;
                    }
                }
                preDxyzDivDt = dxyzDivDt;

                // Calculate xpos, ypos, and zpos
                if (dxyz[2] != 0.0) {

                    for (int i = 0; i < 3; i++) {
                        xyzPos[i] = preXyzPos[i] - (dxyz[i] / ((float) 1000));
                    }
                }
                preXyzPos = xyzPos;

                // Define the projected bump
                if (Math.abs(Math.round(dxyz[0])) > 150000) {
                    projectedBump = 400000;
                }

                // Reformat the result
                String sX = String.format(Locale.US, "%.3f", xyz[0]);
                String sY = String.format(Locale.US, "%.3f", xyz[1]);
                String sZ = String.format(Locale.US, "%.3f", xyz[2]);

                String sDxDivDt = String.format(Locale.US, "%.3f", dxyzDivDt[0]);
                String sDyDivDt = String.format(Locale.US, "%.3f", dxyzDivDt[1]);
                String sDzDivDt = String.format(Locale.US, "%.3f", dxyzDivDt[2]);

                String sdx = String.format(Locale.US, "%.3f", dxyz[0]);
                String sdy = String.format(Locale.US, "%.3f", dxyz[1]);
                String sdz = String.format(Locale.US, "%.3f", dxyz[2]);

                String sxPos = String.format(Locale.US, "%.3f", xyzPos[0]);
                String syPos = String.format(Locale.US, "%.3f", xyzPos[1]);
                String szPos = String.format(Locale.US, "%.3f", xyzPos[2]);

                // Save the data into ArrayList
                linearData.add(new String[]{Double.toString(((double) currentTime) / 1000.0),
                        sX, sY, sZ, " ",
                        sDxDivDt, sDyDivDt, sDzDivDt, " ",
                        sdx, sdy, sdz, " ",
                        sxPos, syPos, szPos, " ",
                        Integer.toString(0), Integer.toString(projectedBump)});

                // Display acceleration into screen
                xText.setText("X: " + sX + "mm/s^2");
                yText.setText("Y: " + sY + "mm/s^2");
                zText.setText("Z: " + sZ + "mm/s^2");
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // Not in use
            }
        };

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                // Called when a new location is found by the network location provider or GPS.
                String time = Double.toString((double) (location.getTime() - initialTime) / 1000.0);
                String latitude = Double.toString(location.getLatitude());
                String longitude = Double.toString(location.getLongitude());
                String accuracy = String.format(Locale.US, "%.0f", location.getAccuracy() * 100);
                String provider = location.getProvider();

                if (provider.equals("network")) {
                    networkData.add(new String[]{time, latitude, longitude, accuracy, provider});
                } else {
                    gpsData.add(new String[]{time, latitude, longitude, accuracy, provider});
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // Not in use
            }

            @Override
            public void onProviderEnabled(String provider) {
                // Not in use
            }

            @Override
            public void onProviderDisabled(String provider) {
                // Not in use
            }
        };
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.gpsNetButton:

                // Once user press the inside button, it shows the device is inside.
                // So we wll use Cell or Wifi as location provider
                gpsNetButton.setEnabled(false);
                gpsButton.setEnabled(false);
                startButton.setEnabled(true);

                networkProvider = LocationManager.NETWORK_PROVIDER;
                gpsProvider = LocationManager.GPS_PROVIDER;

                break;

            case R.id.gpsButton:

                // Once user press the outside button, it shows the device is outside.
                // So we wll use Cell or Wifi as location provider
                gpsNetButton.setEnabled(false);
                gpsButton.setEnabled(false);
                startButton.setEnabled(true);

                networkProvider = null;
                gpsProvider = LocationManager.GPS_PROVIDER;

                break;

            case R.id.startButton:

                // Once user presses the start button,
                // the start button will be disabled,
                // the stop button will be enabled.

                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                bumpButton.setEnabled(true);
                button2.setEnabled(true);
                saveButton.setEnabled(true);

                // Active the sensors
                started = true;

                Sensor linear = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
                //Sensor linear = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                sensorManager.registerListener(linearSensorEventListener, linear, SensorManager.SENSOR_DELAY_NORMAL);

                // Active the location updates
                if (locationManager != null) {

                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                        if (networkProvider == null) {
                            locationManager.requestLocationUpdates(gpsProvider, 5000, 10f, locationListener);
                        } else {
                            locationManager.requestLocationUpdates(networkProvider, 5000, 10f, locationListener);
                            locationManager.requestLocationUpdates(gpsProvider, 5000, 10f, locationListener);
                        }
                    }

                }

                break;

            case R.id.stopButton:

                // Once user presses the stop button,
                // the start button will be enabled,
                // the stop button will be disabled,
                gpsNetButton.setEnabled(true);
                gpsButton.setEnabled(true);
                stopButton.setEnabled(false);
                bumpButton.setEnabled(false);
                button2.setEnabled(false);
                saveButton.setEnabled(false);

                // Make sensor stop recording data
                started = false;
                sensorManager.unregisterListener(linearSensorEventListener);

                // Stop updating locations
                locationManager.removeUpdates(locationListener);

                linearData.clear();
                gpsData.clear();
                networkData.clear();
                bumpData.clear();
                button2Data.clear();

                mGoogleApiClient.disconnect();

                break;

            case R.id.bumpButton:
                // If you hit findBump, it will record the time
                v.startAnimation(myAnim);
                bumpData.add(new String[]{Double.toString(((double) (System.currentTimeMillis() - initialTime)) / 1000.0), "Bump!", String.valueOf(mLastLocation.getLatitude()),
                        String.valueOf(mLastLocation.getLongitude()), String.valueOf(mLastLocation.getAccuracy()), String.valueOf(mLastLocation.getProvider())});
                break;

            case R.id.button2:
                v.startAnimation(myAnim);
                button2Data.add(new String[]{Double.toString(((double) (System.currentTimeMillis() - initialTime)) / 1000.0), "Button2!", String.valueOf(mLastLocation.getLatitude()),
                        String.valueOf(mLastLocation.getLongitude()), String.valueOf(mLastLocation.getAccuracy()), String.valueOf(mLastLocation.getProvider())});
                break;

            case R.id.saveButton:

                // Save the data to local drive
                try {

                    // Initialize the files
                    String linearFileName = "linear" + Integer.toString(linearCounter) + ".csv";
                    linearCounter++;

                    String gpsFileName = "gps" + Integer.toString(locationCounter) + ".csv";

                    String bumpButtonFileName = "bump" + Integer.toString(locationCounter) + ".csv";
                    String button2FileName = "button2_ " + Integer.toString(locationCounter) + ".csv";

                    File linearCsvFile = getFileDir(linearFileName);
                    File gpsCsvFile = getFileDir(gpsFileName);
                    File bumpButtonFile = getFileDir(bumpButtonFileName);
                    File button2File = getFileDir(button2FileName);

                    CSVWriter linearWriter = new CSVWriter(new FileWriter(linearCsvFile));
                    CSVWriter gpsWriter = new CSVWriter(new FileWriter(gpsCsvFile));
                    CSVWriter bumpWriter = new CSVWriter(new FileWriter(bumpButtonFile));
                    CSVWriter button2Writer = new CSVWriter(new FileWriter(button2File));

                    linearWriter.writeAll(linearData);
                    linearWriter.close();

                    gpsWriter.writeAll(gpsData);
                    gpsWriter.close();

                    bumpWriter.writeAll(bumpData);
                    bumpWriter.close();

                    button2Writer.writeAll(button2Data);
                    button2Writer.close();

                    linearData.clear();
                    gpsData.clear();
                    bumpData.clear();
                    button2Data.clear();

                    if (networkProvider != null) {

                        String networkFileName = "network" + Integer.toString(locationCounter) + ".csv";
                        File networkCsvFile = getFileDir(networkFileName);
                        CSVWriter networkWriter = new CSVWriter(new FileWriter(networkCsvFile));
                        networkWriter.writeAll(networkData);
                        networkWriter.close();
                        networkData.clear();
                    }

                    locationCounter++;

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            default:
                break;
        }
    }

    /* Checks if external storage if available for read and write */
    public boolean isExternalStorageWritable() {

        // Get the state of the external storage of the device
        String state = Environment.getExternalStorageState();
        // If the returned state is equal to MEDIA_MOUNTED, then you can read and write your files.
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    // Save the file into external storage as public files
    public File getFileDir(String fileName) {

        if (isExternalStorageWritable()) {

            // Get the absolute address of external storage in the Android Device,
            // and add a folder named sensorData which we will create later.
            String path = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/sensorData";

            // Assign the path to a File object, and create the sensorData folder.
            File dir = new File(path);
            dir.mkdir();

            // Create a new CSV file with the argument as the CSV file name.
            return new File(dir, fileName);
        }

        return null;

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister sensor listener in order to save battery
        if (started) {
            sensorManager.unregisterListener(linearSensorEventListener);
        }
    }

    /**
     * Checks if the app has permission to write to device storage
     * <p/>
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    /**
     * Checks if the app has permission to request location call
     * <p/>
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyLocationPermissions(Activity activity) {

        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_LOCATION,
                    REQUEST_LOCATION
            );
        }
    }

}
