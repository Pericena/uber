package com.p1.uber.activities.client;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.database.DatabaseError;
import com.google.maps.android.SphericalUtil;
import com.p1.uber.R;
import com.p1.uber.activities.MainActivity;
import com.p1.uber.include.MyToolbar;
import com.p1.uber.provides.AuthProvides;
import com.p1.uber.provides.GeofireProvider;
import com.p1.uber.provides.TokenProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import activities.MainActivity;

public class MapClientActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    private AuthProvides mAuth;
    private AuthProvides mGeofireProvider;
    private GeofireProvider mGeofireProvide;

    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocation;

    private  final static int LOCATION_REQUEST_CODE = 1;
    private final static int SETTINGS_REQUEST_CODE = 2;

    private LatLng mCurrentLatLng;

    private Marker mMarker;

    private List<Marker> mDriversMarker= new ArrayList<>();

    private boolean mIsFirstTime=true;

    private AutocompleteSupportFragment mAutocomplete;
    private AutocompleteSupportFragment mAutocompleteDestination;

    private PlacesClient mPlaces;

    private String mOrigin;
    private LatLng mOriginLatLng;

    private String mDestination;
    private LatLng mDestinationLatLng;

    private Button mButtonRequestDriver;

    private TokenProvider mTokenProvider;

    private GoogleMap.OnCameraIdleListener mCamaraListener;

    LocationCallback mLocationCallBack= new LocationCallback(){
        @Override
        public  void onLocationResult(LocationResult locationResult){
            for (Location location: locationResult.getLocations()){
                if (getApplicationContext() !=null){
                    mCurrentLatLng = new LatLng(location.getLatitude(),location.getLongitude());

                    /*
                    if(mMarker != null){
                        mMarker.remove();
                    }

                    mMarker = mMap.addMarker(new MarkerOptions().position(
                            new LatLng(location.getLatitude(),location.getLongitude())
                            )
                                    .title("Tu posicion")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons8_ubicaci_n_del_usuario_96))
                    );

                     */
                    // obtenie la localizacion del usuario en tiempo real
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()

                                    .target(new LatLng(location.getLatitude(), location.getLongitude()))
                                    .zoom(16f)
                                    .build()
                    ));

                    if (mIsFirstTime){
                        mIsFirstTime=false;
                        getActiveDrivers();
                        limitSearch();
                    }
                }
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_client);
        MyToolbar.show(this,"Cliente",false);
        mMapFragment= (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);
        mButtonRequestDriver = findViewById(R.id.btnRequestDriver);

        mTokenProvider = new TokenProvider();

        mGeofireProvide = new GeofireProvider("active_driver");

        mFusedLocation= LocationServices.getFusedLocationProviderClient(this);

        mAuth= new AuthProvides();
        if (!Places.isInitialized()){
            Places.initialize(getApplicationContext(),getResources().getString(R.string.google_maps_key));
        }
        mPlaces = Places.createClient(this);
        InstanceAutoCompleteOrigin();
        InstanceAutoCompleteDestination();
        OnCameraMove();
        mButtonRequestDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestDriver();
            }
        });

        generateToken();
    }

    private void requestDriver() {
        if (mOriginLatLng != null && mDestinationLatLng != null){
            Intent intent = new Intent(MapClientActivity.this,DetailRequestActivity.class);
            intent.putExtra("origin_lat", mOriginLatLng.latitude);
            intent.putExtra("origin_lng", mOriginLatLng.longitude);
            intent.putExtra("destination_lat", mDestinationLatLng.latitude);
            intent.putExtra("destination_lng", mDestinationLatLng.longitude);
            intent.putExtra("origin", mOrigin);
            intent.putExtra("destination", mDestination);
            startActivity(intent);


        }
        else {
            Toast.makeText(this,"Debe seleccionar el lugar de recogida y el destino",Toast.LENGTH_SHORT).show();
        }
    }

    private  void limitSearch(){
        LatLng northSide = SphericalUtil.computeOffset(mCurrentLatLng, 5000,0);
        LatLng southSide = SphericalUtil.computeOffset(mCurrentLatLng, 5000,180);
        mAutocomplete.setCountry("BOL");
        mAutocomplete.setLocationBias(RectangularBounds.newInstance(southSide,northSide));
        mAutocompleteDestination.setCountry("BOL");
        mAutocompleteDestination.setLocationBias(RectangularBounds.newInstance(southSide,northSide));

    }
    private void OnCameraMove(){
        mCamaraListener = new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                try {
                    Geocoder geocoder = new Geocoder(MapClientActivity.this);
                    mOriginLatLng = mMap.getCameraPosition().target;
                    List<Address> addressList = geocoder.getFromLocation(mOriginLatLng.latitude,mOriginLatLng.longitude, 1);
                    String city = addressList.get(0).getLocality();
                    String country = addressList.get(0).getCountryName();
                    String  address = addressList.get(0).getAddressLine(0);
                    mAutocomplete.setText(address + " " + city );
                    mOrigin= address + " " + city ;
                    mAutocompleteDestination.setText(address + " " + city );
                    mDestination= address + " " + city ;

                } catch (Exception e) {
                    Log.d("Error","Menaje de error" + e.getMessage());
                }

            }
        };
    }
    private void InstanceAutoCompleteOrigin(){

        mAutocomplete = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.placeAutoCompleteorigin);
        assert mAutocomplete != null;
        mAutocomplete.setPlaceFields(Arrays.asList(Place.Field.ID,Place.Field.LAT_LNG,Place.Field.NAME));
        mAutocomplete.setHint("Lugar de recogida");
        mAutocomplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {

                mOrigin = place.getName();
                mOriginLatLng = place.getLatLng();
                Log.d("PLACE","Name: " + mOrigin);
                Log.d("PLACE","Lat: " + mOriginLatLng.latitude);
                Log.d("PLACE","Long: " + mOriginLatLng.longitude);
            }

            @Override
            public void onError(@NonNull Status status) {

            }
        });
    }
    private void InstanceAutoCompleteDestination(){
        mAutocompleteDestination = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.placeAutoCompleteDestination);
        assert mAutocompleteDestination != null;
        mAutocompleteDestination.setPlaceFields(Arrays.asList(Place.Field.ID,Place.Field.LAT_LNG,Place.Field.NAME));
        mAutocompleteDestination.setHint("Destino");
        mAutocompleteDestination.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {

                mDestination = place.getName();
                mDestinationLatLng = place.getLatLng();
                Log.d("PLACE","Name: " + mDestination);
                Log.d("PLACE","Lat: " + mDestinationLatLng.latitude);
                Log.d("PLACE","Long: " + mDestinationLatLng.longitude);



            }

            @Override
            public void onError(@NonNull Status status) {

            }
        });
    }

    private  void getActiveDrivers(){
        double radius = 10;
        mGeofireProvide.getActiveDriver(mCurrentLatLng, radius).addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                //ANADIR LOS MARCADORES DE LOS CONDUTORES QUE COENCTAN A LA APLICACION
                for (Marker marker: mDriversMarker){
                    if (marker.getTag() != null){
                        if (marker.getTag().equals(key)){
                            return;
                        }
                    }
                }
                LatLng driverlatlng = new LatLng(location.latitude,location.longitude);
                Marker marker = mMap.addMarker(new MarkerOptions().position(driverlatlng).title("Conductor disponible").icon(BitmapDescriptorFactory.fromResource(R.drawable.icons8_personas_en_coche__vista_lateral_50)));
                marker.setTag(key);
                mDriversMarker.add(marker);

            }

            @Override
            public void onKeyExited(String key) {
                for (Marker marker: mDriversMarker){
                    if (marker.getTag() != null){
                        if (marker.getTag().equals(key)){
                            marker.remove();
                            mDriversMarker.remove(marker);
                            return;
                        }
                    }
                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                // ACTUALIZAR LA POSICION DE CADA CONDUCTOR
                for (Marker marker: mDriversMarker){
                    if (marker.getTag() != null){
                        if (marker.getTag().equals(key)){
                            marker.setPosition(new LatLng(location.latitude,location.longitude));
                        }
                    }
                }
            }



            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap=googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        mMap.setOnCameraIdleListener(mCamaraListener);
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(5);
        startLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode ==LOCATION_REQUEST_CODE){
            if (grantResults.length > 0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                    if (gpsActived()) {
                        mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallBack, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                    else {
                        showAlertDialogNOGPS();
                    }
                }
                else {
                    checkLocationPermission();
                }

            }
            else {
                checkLocationPermission();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_REQUEST_CODE && gpsActived()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
                mFusedLocation.requestLocationUpdates(mLocationRequest,mLocationCallBack, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }

        }
        else  if (requestCode == SETTINGS_REQUEST_CODE && !gpsActived()) {
            showAlertDialogNOGPS();
        }

    }


        private void showAlertDialogNOGPS(){
        AlertDialog.Builder builder= new AlertDialog.Builder(this);
        builder.setMessage("Por favor activa tu ubicaion para continuar")
                .setPositiveButton("Configuraciones", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),SETTINGS_REQUEST_CODE);

                    }
                }).create().show();

    }


    private boolean gpsActived(){
        boolean isActive =false;
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            isActive =true;
        }
        return isActive;
    }


    private void startLocation(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
                if (gpsActived()) {
                    mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallBack, Looper.myLooper());
                    mMap.setMyLocationEnabled(true);
                }
                else{
                    showAlertDialogNOGPS();
                }
            }
            else{
                checkLocationPermission();
            }
        }else{
            if (gpsActived()) {
                mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallBack, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }
            else {
                showAlertDialogNOGPS();
            }
        }


    }

    private void checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                new AlertDialog.Builder(this)
                        .setTitle("Proporciona los permisos para continuar")
                        .setMessage("Esta aplicacion requiere de los permisos de ubicacion para utilizarse")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MapClientActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);

                            }
                        }).create().show();
            }
            else {
                ActivityCompat.requestPermissions(MapClientActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);

            }


        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.driver_menu,menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId()==R.id.action_logout ){
            logout();
        }
        return super.onOptionsItemSelected(item);
    }


    void logout(){
        mAuth.logout();
        Intent intent =new Intent(MapClientActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    void generateToken(){

        mTokenProvider.create(mAuth.getId());
    }
}
