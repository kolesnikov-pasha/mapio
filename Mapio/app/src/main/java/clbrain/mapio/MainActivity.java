package clbrain.mapio;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int DEFAULT_ZOOM = 17;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    //firebase reference
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    //for requests
    private int color;
    private Timer mTimer = new Timer();
    private Toast internetFailure;
    private boolean mLocationPermissionGranted;
    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback = new LocationCallback();
    private LocationRequest mLocationRequest = new LocationRequest();
    private Location mLastKnownLocation;
    //map and polygons
    private List<SquaresData> squaresDataList = new ArrayList<>();
    private TreeMap<Coordinates, Polygon> polygonTreeMap = new TreeMap<>();
    private TreeSet<SquaresData> allSquaresDataList = new TreeSet<>();
    private SupportMapFragment mapFragment = null;
    private GoogleMap mMap;

    private void getFrameData() {
        FrameData bounds = getBounds();
        Call<SquaresDataList> call = Requests.apiServices.getFrameData(bounds.getLeft_bottom_corner_latitude(),
                bounds.getLeft_bottom_corner_longitude(), bounds.getRight_top_corner_latitude(), bounds.getRight_top_corner_longitude());
        call.enqueue(new Callback<SquaresDataList>() {
            @Override
            public void onResponse(@NonNull Call<SquaresDataList> call, @NonNull Response<SquaresDataList> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        Log.i("SQUARES", response.body().getSquares().toString() + " ");
                        ArrayList<SquaresData> squaresList = (ArrayList<SquaresData>) response.body().getSquares();
                        for (int i = 0; i < squaresList.size(); i++) {
                            if (allSquaresDataList.contains(squaresList.get(i))) {
                                squaresList.remove(squaresList.get(i));
                            }
                        }
                        squaresDataList = squaresList;
                        allSquaresDataList.addAll(squaresList);
                    }
                } else {
                    internetFailure.show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<SquaresDataList> call, @NonNull Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void sendDropBomb() {
        Double latitude = 0.0, longitude = 0.0;
        try {
            latitude = mMap.getMyLocation().getLatitude();
            longitude = mMap.getMyLocation().getLongitude();
        } catch (Exception e) {
            try {
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
                        LocationManager.GPS_PROVIDER, 200, 1, locationListener);
                latitude = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude();
                longitude = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude();
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 200, 1, locationListener);
                latitude = (latitude + locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLatitude()) / 2.0;
                longitude = (longitude + locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLongitude()) / 2.0;
                locationManager.removeUpdates(locationListener);
            } catch (Exception exp) {
                Toast.makeText(getApplicationContext(), "Oooops.. we have some problem", Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), "Sorry!", Toast.LENGTH_SHORT).show();
            }
        }
        Requests.apiServices.sendDropBomb(new SendCoordinates(user.getUid(), latitude, longitude)).enqueue(new Callback<StringStatus>() {
            @Override
            public void onResponse(@NonNull Call<StringStatus> call, @NonNull Response<StringStatus> response) {
                if (response.isSuccessful()) {
                } else {
                    Toast.makeText(getApplicationContext(), "Sending coordinates error...", Toast.LENGTH_SHORT).show();
                    Toast.makeText(getApplicationContext(), "Sorry!!!", Toast.LENGTH_SHORT).show();
                    Toast.makeText(getApplicationContext(), "Leave your angry comment in GooglePlay:)", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<StringStatus> call, @NonNull Throwable t) {
                internetFailure.show();
            }
        });
    }

    private void sendCoordinates() {
        Double latitude = 0.0, longitude = 0.0;
        try {
            SendCoordinates coordinates = getDeviceCoordinates();
            latitude = coordinates.getLatitude();
            longitude = coordinates.getLongitude();
            Log.i("COORD", latitude + "; " + longitude);
        } catch (Exception e) {
            try {
                latitude = mMap.getMyLocation().getLatitude();
                longitude = mMap.getMyLocation().getLongitude();
            } catch (Exception j) {
                try {
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
                            LocationManager.GPS_PROVIDER, 200, 1, locationListener);
                    latitude = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude();
                    longitude = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude();
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 200, 1, locationListener);
                    latitude = (latitude + locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLatitude()) / 2.0;
                    longitude = (longitude + locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLongitude()) / 2.0;
                    locationManager.removeUpdates(locationListener);
                } catch (Exception exp) {
                    Toast.makeText(getApplicationContext(), "Oooops.. we have some problem", Toast.LENGTH_SHORT).show();
                    Toast.makeText(getApplicationContext(), "Sorry!", Toast.LENGTH_SHORT).show();
                }
            }
        }
        Requests.apiServices.sendCoordinates(new SendCoordinates(user.getUid(), latitude, longitude)).enqueue(new Callback<StringStatus>() {
            @Override
            public void onResponse(@NonNull Call<StringStatus> call, @NonNull Response<StringStatus> response) {
                if (response.isSuccessful()) {
                } else {
                    Toast.makeText(getApplicationContext(), "Sending coordinates error...", Toast.LENGTH_SHORT).show();
                    Toast.makeText(getApplicationContext(), "Sorry!!!", Toast.LENGTH_SHORT).show();
                    Toast.makeText(getApplicationContext(), "Leave your angry comment in GooglePlay:)", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<StringStatus> call, @NonNull Throwable t) {
                internetFailure.show();
            }
        });
    }

    private void getSquaresData() {
        Call<SquaresDataList> call = Requests.apiServices.getSquaresData();
        call.enqueue(new Callback<SquaresDataList>() {
            @Override
            public void onResponse(@NonNull Call<SquaresDataList> call, @NonNull Response<SquaresDataList> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        Log.i("SQUARES", response.body().getSquares().toString() + " ");
                        ArrayList<SquaresData> squaresList = (ArrayList<SquaresData>) response.body().getSquares();
                        for (int i = 0; i < squaresList.size(); i++) {
                            if (allSquaresDataList.contains(squaresList.get(i))) {
                                squaresList.remove(squaresList.get(i));
                            }
                        }
                        squaresDataList = squaresList;
                        allSquaresDataList.addAll(squaresList);
                    }
                } else {
                    internetFailure.show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<SquaresDataList> call, @NonNull Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void getUserColor() {
        String uid = user.getUid();
        Call<clbrain.mapio.Color> call = Requests.apiServices.getUserColor(uid);
        call.enqueue(new Callback<clbrain.mapio.Color>() {
            @Override
            public void onResponse(@NonNull Call<clbrain.mapio.Color> call,
                                   @NonNull Response<clbrain.mapio.Color> response) {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    color = Color.parseColor(response.body().getUser_color());
                    Log.e("COLOR", color + "");
                } else {
                    Toast.makeText(getApplicationContext(), "We can't get your color", Toast.LENGTH_SHORT).show();
                    Toast.makeText(getApplicationContext(), "Sorry!!!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<clbrain.mapio.Color> call, @NonNull Throwable t) {
                t.printStackTrace();
            }
        });
    }

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        internetFailure = Toast.makeText(getApplicationContext(), "Check your internet connection", Toast.LENGTH_SHORT);
        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_main);
        //set onClickListeners
        Button btnDropBomb = findViewById(R.id.drop_bomb);
        btnDropBomb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendDropBomb();
            }
        });
        setOnClickListeners();
        // Construct a FusedLocationProviderClient.
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        startLocationUpdates();
        // Build the map.
        for (int i = 0; mapFragment == null; i++) {
            mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            if (i == 1) {
                Toast.makeText(getApplicationContext(), "Loading...", Toast.LENGTH_SHORT).show();
            }
        }
        mapFragment.getMapAsync(MainActivity.this);
        // making requests
        getUserColor();
        TimerTask timerTask = new MyTimerTask();
        mTimer.schedule(timerTask, 200, 5000);
        findViewById(R.drawable.round).setBackgroundColor(Color.RED);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            super.onSaveInstanceState(outState);
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
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
        //mMap.setMinZoomPreference(17);
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
                        (FrameLayout) findViewById(R.id.map), false);

                TextView title = infoWindow.findViewById(R.id.title);
                title.setText(marker.getTitle());

                TextView snippet = infoWindow.findViewById(R.id.snippet);
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
    }

    public void setOnClickListeners(){
        findViewById(R.id.btn_main_activity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            }
        });
        findViewById(R.id.btn_shop_activity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), ShopActivity.class));
            }
        });
        findViewById(R.id.btn_profile_activity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
            }
        });
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "Turn on GPS, please", Toast.LENGTH_SHORT).show();
            Toast.makeText(getApplicationContext(), "Or leave angry comment in GoodlePlay:)", Toast.LENGTH_SHORT).show();
            return;
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback, Looper.myLooper());
    }

    private SendCoordinates getDeviceCoordinates() {
        final SendCoordinates[] ans = new SendCoordinates[1];
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "Turn on GPS, please", Toast.LENGTH_SHORT).show();
        } else {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        Log.e("COORD", location.getLatitude() + "; " + location.getLongitude());
                        ans[0] = new SendCoordinates(user.getUid(), location.getLatitude(), location.getLongitude());
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Turn on GPS, please", Toast.LENGTH_SHORT).show();
                        Toast.makeText(getApplicationContext(),
                                "Or leave angry comment in GoodlePlay:)", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        return ans[0];
    }


    private void getDeviceLocation() {
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationClient.getLastLocation();
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

    @NonNull
    private FrameData getBounds() {
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        LatLng boundsNorthEast = bounds.northeast;
        LatLng boundsSouthWest = bounds.southwest;
        return new FrameData(boundsSouthWest.latitude, boundsSouthWest.longitude,
                boundsNorthEast.latitude, boundsNorthEast.longitude);
    }

    //draw polygons
    private void drawPolygons() {
        double deltaLatitude = 1.0 / 3600, deltaLongitude = 1.0 / 2400;//Дельта для формироваия квадратиков
        for (int i = 0; i < squaresDataList.size(); i++) {
            Coordinates coordinates = new Coordinates(squaresDataList.get(i).getVertical_id(),
                    squaresDataList.get(i).getHorizontal_id());
            if (!polygonTreeMap.containsKey(coordinates)) {//если полигона с такими координатами еще нет
                polygonTreeMap.put(coordinates, null);
                PolygonOptions polygonOptions = new PolygonOptions()
                        .add(new LatLng(coordinates.vertical_id / 3600.0 + deltaLatitude, coordinates.horizontal_id / 2400.0))
                        .add(new LatLng(coordinates.vertical_id / 3600.0, coordinates.horizontal_id / 2400.0))
                        .add(new LatLng(coordinates.vertical_id / 3600.0, coordinates.horizontal_id / 2400.0 + deltaLongitude))
                        .add(new LatLng(coordinates.vertical_id / 3600.0 + deltaLatitude, coordinates.horizontal_id / 2400.0 + deltaLongitude))
                        .strokeColor(Color.argb(100, 0, 0, 0)).strokeWidth(2)
                        .fillColor(Color.parseColor(squaresDataList.get(i).getColor()));
                polygonTreeMap.put(coordinates, mMap.addPolygon(polygonOptions));
            } else if (polygonTreeMap.get(coordinates).getFillColor() //иначе если у этого полигона другой цвет перекрась
                    != Color.parseColor(squaresDataList.get(i).getColor())) {
                polygonTreeMap.get(coordinates).setFillColor(Color.parseColor(squaresDataList.get(i).getColor()));
            }
        }
    }

    //Location button view
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
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTimer.cancel();
    }

    class Coordinates implements Comparable<Coordinates> {

        Integer vertical_id, horizontal_id;

        private Coordinates(Integer vertical_id, Integer horizontal_id) {
            this.vertical_id = vertical_id;
            this.horizontal_id = horizontal_id;
        }

        private Coordinates() {
        }

        @Override
        public String toString() {
            return vertical_id + "; " + horizontal_id;
        }

        @Override
        public int compareTo(@NonNull Coordinates coordinates) {
            int h1 = coordinates.horizontal_id;
            int v1 = coordinates.vertical_id;
            int h2 = this.horizontal_id;
            int v2 = this.vertical_id;
            if (h1 == h2 && v1 == v2) return 0;
            else if (h1 > h2 || (h1 == h2 && v1 > v2)) return 1;
            else return -1;
        }
    }

    class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    sendCoordinates();
                    getFrameData();
                    drawPolygons();
                }
            });
        }
    }
}
