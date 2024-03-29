package com.p1.uber.activities.driver;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.p1.uber.R;
import com.p1.uber.activities.MainActivity;

import com.p1.uber.include.MyToolbar;
import com.p1.uber.provides.AuthProvides;
import com.p1.uber.provides.GeofireProvider;
import com.p1.uber.provides.TokenProvider;

//import activities.MainActivity;

public class MapDriverActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    private AuthProvides mAuth;

    private GeofireProvider mGeoFireProvider;

    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocation;

    private  final static int LOCATION_REQUEST_CODE = 1;
    private final static int SETTINGS_REQUEST_CODE = 2;

    private Marker mMarker;

    private Button mButtonConnect;
    private boolean mIsConnect = false;

    private LatLng mCurrentLatLng;

    private TokenProvider mTokenProvider;

    private ValueEventListener mListener;


    LocationCallback mLocationCallBack= new LocationCallback(){
        @Override
        public  void onLocationResult(LocationResult locationResult){
            for (Location location: locationResult.getLocations()){
                if (getApplicationContext() !=null){

                    mCurrentLatLng = new LatLng(location.getLatitude(),location.getLongitude());


                    if(mMarker != null){
                        mMarker.remove();
                    }

                    mMarker = mMap.addMarker(new MarkerOptions().position(
                            new LatLng(location.getLatitude(),location.getLongitude())
                    )
                            .title("Tu posicion")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons8_personas_en_coche__vista_lateral_50))
                    );


                    // obtenie la localizacion del usuario en tiempo real
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                            .target(new LatLng(location.getLatitude(), location.getLongitude()))
                            .zoom(16f)
                            .build()
                    ));

                    updateLocation();

                }
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_driver);
        mMapFragment= (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);
        MyToolbar.show(this,"Conductor",false);
        mAuth= new AuthProvides();
        mGeoFireProvider = new GeofireProvider("active_driver");
        mTokenProvider = new TokenProvider();

        mButtonConnect = findViewById(R.id.btnConnect);
        mButtonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsConnect){
                    disconnect();
                }
                else {
                    startLocation();
                }
            }
        });

        mFusedLocation= LocationServices.getFusedLocationProviderClient(this);

        generateToken();

        isDriverWorking();

    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (mListener != null){
            mGeoFireProvider.isDriverWorking(mAuth.getId()).removeEventListener(mListener);
        }
    }
    private void isDriverWorking() {
       mListener = mGeoFireProvider.isDriverWorking(mAuth.getId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    disconnect();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private void updateLocation(){
        if (mAuth.existSesion() && mCurrentLatLng !=null){

        mGeoFireProvider.saveLocation(mAuth.getId(),mCurrentLatLng);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap=googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(5);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(false);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode ==LOCATION_REQUEST_CODE){
            if (grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                     if (gpsActived()) {
                         mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallBack, Looper.myLooper());
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
            }

         }
        else {
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


    private void disconnect(){

        if (mFusedLocation != null){
            mButtonConnect.setText("Conectarse");
            mIsConnect = false;
            mFusedLocation.removeLocationUpdates(mLocationCallBack);
            if (mAuth.existSesion()) {
                mGeoFireProvider.removeLocation(mAuth.getId());
            }
        }

        else {
            Toast.makeText(this,"No te puedes desconectar",Toast.LENGTH_SHORT).show();
        }

    }

    private void startLocation(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
                if (gpsActived()) {
                    mButtonConnect.setText("Desconectarse");
                    mIsConnect = true;
                    mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallBack, Looper.myLooper());
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
                                    ActivityCompat.requestPermissions(MapDriverActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);

                                }
                            }).create().show();
                }
                else {
                    ActivityCompat.requestPermissions(MapDriverActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);

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
        disconnect();
        mAuth.logout();
        Intent intent =new Intent(MapDriverActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    void generateToken(){
        mTokenProvider.create(mAuth.getId());
    }
}