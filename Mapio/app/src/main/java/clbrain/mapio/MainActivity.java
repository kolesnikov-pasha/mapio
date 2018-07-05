package clbrain.mapio;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.maps.model.MapStyleOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {


    private GoogleMap mMap;
    private int polygonsCount = 0;
    private int color;
    ArrayList<PolygonOptions> polygonOptions = new ArrayList<>();

    //firebase reference
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    //for requests
    class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    sendCoordinates();
                    getSquaresData();
                    //init();
                }
            });
        }
    }
    private Toast internetFailure = null;
    private List<SquaresData> squaresDataList = new ArrayList<>();

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    //For drawing polylines at the map
    SupportMapFragment mapFragment;

    private void sendCoordinates(){
        LocationManager locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new MyLocationListener();
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        assert locationManager != null;
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        Double latitude = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude();
        Double longitude = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude();
        Requests.apiServices.sendCoordinates(new SendCoordinates(user.getUid(), latitude, longitude)).enqueue(new Callback<StringStatus>() {
            @Override
            public void onResponse(@NonNull Call<StringStatus> call, @NonNull Response<StringStatus> response) {
                if (response.isSuccessful()) {
                    Log.i("COORD_SEND", response.body().getStatus());
                }
                else {
                    internetFailure.show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<StringStatus> call, @NonNull Throwable t) {
                internetFailure.show();
            }
        });
    }

    private void getSquaresData(){
        Call<SquaresDataList> call = Requests.apiServices.getSquaresData();
        call.enqueue(new Callback<SquaresDataList>() {
            @Override
            public void onResponse(@NonNull Call<SquaresDataList> call, @NonNull Response<SquaresDataList> response) {
                if (response.isSuccessful()){
                    Log.i("SQUARES", response.body().getSquares().toString() + " ");
                    squaresDataList = (ArrayList<SquaresData>) response.body().getSquares();
                }
                else{
                    internetFailure.show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<SquaresDataList> call, @NonNull Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void getUserColor(){
        String uid = user.getUid();
        Call<clbrain.mapio.Color> call = Requests.apiServices.getUserColor(uid);
        call.enqueue(new Callback<clbrain.mapio.Color>() {
            @Override
            public void onResponse(@NonNull Call<clbrain.mapio.Color> call,
                                   @NonNull Response<clbrain.mapio.Color> response) {
                if (response.isSuccessful()){
                    assert response.body() != null;
                    color = Color.parseColor(response.body().getUser_color());
                    findViewById(R.id.picked_color_view).findViewById(R.id.color).setBackgroundColor(color);
                    Log.e("COLOR",  color + "");
                }
                else{
                    internetFailure.show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<clbrain.mapio.Color> call, @NonNull Throwable t) {
                t.printStackTrace();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            CameraPosition mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
        internetFailure = Toast.makeText(getApplicationContext(), "Check your internet connection", Toast.LENGTH_SHORT);
        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_main);
        // Construct a GeoDataClient
        GeoDataClient mGeoDataClient = Places.getGeoDataClient(this, null);

        // Construct a PlaceDetectionClient.
        PlaceDetectionClient mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Build the map.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        // making requests
        getUserColor();
        Timer mTimer = new Timer();
        MyTimerTask timerTask = new MyTimerTask();
        mTimer.schedule(timerTask, 2000, 5000);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        mMap.setMaxZoomPreference(8);

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
                        (FrameLayout) findViewById(R.id.map), false);

                TextView title = infoWindow.findViewById(R.id.title);
                title.setText(marker.getTitle());

                TextView snippet =  infoWindow.findViewById(R.id.snippet);
                snippet.setText(marker.getSnippet());

                return infoWindow;
            }
        });
        mMap.setBuildingsEnabled(true);
        mMap.setMapStyle(new MapStyleOptions(getResources()
                .getString(R.string.style_json)));

        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();
        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
        //Отрисовка
        //init();

    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();

                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        } else {
                            Log.d("LOC", "Current location is null. Using defaults.");
                            Log.e("LOC", "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }


    private void init() {

        LocationManager locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new MyLocationListener();
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        assert locationManager != null;
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 2000, 5, locationListener);
        double deltaLatitude = 1.0 / 3600, deltaLongitude = 1.0 / 2400;//Дельта для формироваия квадратиков
        for (int i = 0; i < squaresDataList.size(); i++) {
            polygonOptions.add(new PolygonOptions()
                    .add(new LatLng(squaresDataList.get(i).getVertical_id() / 3600.0 + deltaLatitude, squaresDataList.get(i).getHorizontal_id() / 2400.0))
                    .add(new LatLng(squaresDataList.get(i).getVertical_id() / 3600.0,squaresDataList.get(i).getHorizontal_id() / 2400.0))
                    .add(new LatLng(squaresDataList.get(i).getVertical_id() / 3600.0, squaresDataList.get(i).getHorizontal_id() / 2400.0 + deltaLongitude))
                    .add(new LatLng(squaresDataList.get(i).getVertical_id() / 3600.0 + deltaLatitude,squaresDataList.get(i).getHorizontal_id() / 2400.0 + deltaLongitude))
                    .strokeColor(Color.argb(100, 0, 0, 0)).strokeWidth(2)
                    .fillColor(color));
            mMap.addMarker(new MarkerOptions().position(new LatLng(squaresDataList.get(i).getVertical_id() / 3600.0, squaresDataList.get(i).getHorizontal_id() / 2400.0)));
            mMap.addPolygon(polygonOptions.get(polygonsCount));
            polygonsCount++;
        }
    }


    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }
}
