package com.example.namragill.kalmanfiltergps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    LocationManager locationManager;
    int i = 0;

    private static final int PERMISSION_REQUEST_CODE_LOCATION = 101;

    FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    LocationRequest locationRequest;

    TextView currentLocation;
    int valueLat = 0;
    int valueLon = 0;
    GoogleApiClient apiClient;
    Marker marker;
    /*
   Initialising values in Kalman filter
   */
    double KGLat; //kalman gain
    double KGLon; //kalman gain
    double ErrEst = 3; // Error in Estimation 8 meter dvs 0,00007186 degrees
    double ErrMea = 5; // Error in Measurment 10 meter dvs 0,00008983 degrees
    double preEstLat = 55.6; //Previous Estimated Value
    double preEstLon = 12.99; //Previous Estimated Value
    double outputEstLat; // New estimated value
    double outputEstLon; // New estimated value
    double newErrorEstLat; //new Error Estimation
    double newErrorEstLon; //new Error Estimation
    double identityEst = 1;
    double degrees = 11.100;

    double latitude;
    double longitude;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        locationRequest = new LocationRequest();
        locationRequest.setInterval(500);//7500 sec use a value for about 10-15 sec for a real app
        locationRequest.setFastestInterval(250);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        ((TextView) (findViewById(R.id.lastlocationfield))).setText("Latitude: " + String.valueOf(location.getLatitude()) +
                                " Longitude" + String.valueOf(location.getLongitude()));

                        preEstLat = location.getLatitude()-0.0003717817536;
                        preEstLon = location.getLongitude()-0.0001141589793;

                    }

                }
            });

        } else {
            Toast.makeText(this, "CHECK LOCATION PERMISSIONS", Toast.LENGTH_LONG).show();
        }

          /*Define the Location Update Callback which is a paremeter in mFusedLocationClient*/
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    if (location != null) {
                        Log.d("Callback", String.valueOf(i));
                        ((TextView) (findViewById(R.id.lastlocationfield))).setText("Latitude: " + String.valueOf(location.getLatitude()) +
                                " Longitude" + String.valueOf(location.getLongitude()) + " " + "\n" + i++);
                       // Log.d("Accuracy", String.valueOf(location.getAccuracy()/degrees));
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        Log.d("Position", "-----------------------------------------------------Viewing current location");
                        Log.d("Position", "-----------------------------------------------------" + location.getLatitude());
                        Log.d("Position", "-----------------------------------------------------" + location.getLongitude());
                        final LatLng latlon = new LatLng(location.getLatitude(), location.getLongitude());
                        CameraUpdate update = CameraUpdateFactory.newLatLng(latlon);
                        mMap.moveCamera(update);
                        KalmanGainLat(ErrEst,ErrMea);
                        KalmanGainLon(ErrEst,ErrMea);

                    }
                }
            }

            ;
        };
    }


    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
            mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, null);
        }
        else{
            Toast.makeText(this, "CHECK LOCATION PERMISSIONS", Toast.LENGTH_LONG).show();
            finish();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        super.onRequestPermissionsResult(requestCode,permissions,grantResults);

        switch (requestCode) {

            case PERMISSION_REQUEST_CODE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(this, "Permission loaded...", Toast.LENGTH_SHORT).show();

                } else {

                    Toast.makeText(getApplicationContext(),"Permission Denied, You cannot access location data.",Toast.LENGTH_LONG).show();

                }
                break;
        }
    }

    @Override
    protected void onResume() {

        super.onResume();
        startLocationUpdates();

    }
    @Override
    protected void onStart() {
        super.onStart();
    }

    /*
     * onPause : Called when the system is about to start resuming a previous activity.
     * */
    @Override
    protected void onPause() {
        try {
            super.onPause();

            /*
            * Stop retrieving locations when we go out of the application.
            * */
            stopLocationUpdates();


        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }




    protected void stopLocationUpdates() {
        //  LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }




    /*
    Calculate Kalman Gain
     */
    private void KalmanGainLon(double errEst, double errMea) {

        KGLon = errEst / (errEst + errMea);
      //  ((TextView)(findViewById(R.id.KalmanLon))).setText("KalmanGain_Longitude" +" " + KGLon);
        newValueLon(this.preEstLon,longitude,KGLon);
    }

    /*
   Calculate Kalman Gain
    */
    private void KalmanGainLat(double errEst, double errMea) {

        KGLat = errEst / (errEst + errMea);
        newValueLat(this.preEstLat,latitude,KGLat);
    }

    /*
  Calculate Current Estimated Value
   */
    private void newValueLat(double preEst, double meaValue, double KG) {
        if (valueLat < 2) {
            outputEstLat = preEst + (KG * (meaValue - preEst));
            this.preEstLat = outputEstLat;
            newErrorLat(this.ErrEst, KG);
            valueLat ++;
        }
        else if (valueLat == 2){
            valueLat =0; //kommer fram till en estimated value after 5 iterations

        }


        }


    /*
Calculate Current Estimated Value
*/
    private void newValueLon(double preEst, double meaValue, double KG) {

        if (valueLon < 2){
            outputEstLon = preEst + (KG*(meaValue - preEst));
            this.preEstLon = outputEstLon;
            newErrorLon(this.ErrEst,KG);
            valueLon ++;
        }
        else if (valueLon == 2){
            ((TextView)(findViewById(R.id.textValue))).setText("New_Latitude " + outputEstLat + " New_Longitude " +" " + outputEstLon);
            setUpdatedpos();
            valueLon = 0;
        }
    }

    private void setUpdatedpos() {
        marker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(outputEstLat, outputEstLon))
                .title("Updated Position"));
    }

    /*
Calculate New Estimated Error
 */
    private void newErrorLat(double errEst, double KG) {

        newErrorEstLat = (identityEst - KG)* errEst;
        this.ErrEst = newErrorEstLat;
    }

    /*
Calculate New Estimated Error
*/
    private void newErrorLon(double errEst, double KG) {

        newErrorEstLon = (identityEst - KG)* errEst;
        this.ErrEst = newErrorEstLon;
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
       mMap = googleMap;
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if(!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))) {
            buildAlertMessageNoGps();
        }
        if (mMap != null) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                apiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();
                apiClient.connect();

                mMap.setMyLocationEnabled(true);

                Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show();
            }
            }
            else{
                Toast.makeText(this, "Check permission", Toast.LENGTH_LONG).show();
            }
        }


    private void buildAlertMessageNoGps() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}

