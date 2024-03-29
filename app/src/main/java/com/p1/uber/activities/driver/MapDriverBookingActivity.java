package com.p1.uber.activities.driver;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.p1.uber.R;
import com.p1.uber.modelos.FCMBody;
import com.p1.uber.modelos.FCMResponce;
import com.p1.uber.provides.AuthProvides;
import com.p1.uber.provides.ClientBookingProvider;
import com.p1.uber.provides.ClientProvider;
import com.p1.uber.provides.GeofireProvider;
import com.p1.uber.provides.GoogleApiProvider;
import com.p1.uber.provides.NotificationProvider;
import com.p1.uber.provides.TokenProvider;
import com.p1.uber.utils.DecodePoints;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapDriverBookingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    private AuthProvides mAuth;

    private GeofireProvider mGeoFireProvider;
    private ClientProvider mClientProvider;
    private ClientBookingProvider mClientBookingProvider;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocation;

    private  final static int LOCATION_REQUEST_CODE = 1;
    private final static int SETTINGS_REQUEST_CODE = 2;

    private Marker mMarker;
    private LatLng mCurrentLatLng;

    private TokenProvider mTokenProvider;

    private TextView mTextViewClientBooking;
    private TextView mTextViewEmailClientBooking;
    private TextView mTextViewOriginClientBooking;
    private TextView mTextViewDestinationClientBooking;
    private LatLng mOriginLatLng;
    private LatLng mDestinationLatLng;

    private GoogleApiProvider mGoogleApiProvider;

    private List mPolylineList;
    private PolylineOptions mPolylineOptions;

    private String mExtraClientId;

    private boolean mIsFirstTime = true;
    private boolean mIsCLoseToClient = false;

    private Button mButtonStartBooking;
    private Button mButtonFinishBooking;
    private NotificationProvider mNotificationProvider;

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


                    if (mIsFirstTime){
                        mIsFirstTime=false;
                        getClientBooking();
                    }

                }
            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_driver_booking);

        mMapFragment= (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);
        mAuth= new AuthProvides();
        mGeoFireProvider = new GeofireProvider("drivers_working");
        mTokenProvider = new TokenProvider();
        mFusedLocation= LocationServices.getFusedLocationProviderClient(this);

        mClientProvider = new ClientProvider();
        mClientBookingProvider = new ClientBookingProvider();
        mTextViewClientBooking = findViewById(R.id.textViewClientBooking);
        mTextViewEmailClientBooking = findViewById(R.id.textViewEmailClientBooking);
        mTextViewOriginClientBooking = findViewById(R.id.textViewOriginClientBooking);
        mTextViewDestinationClientBooking = findViewById(R.id.textViewDestinationClientBooking);
        mButtonStartBooking = findViewById(R.id.btnStartBooking);
        mButtonFinishBooking = findViewById(R.id.btnFinishBooking);

       // mButtonStartBooking.setEnabled(false);
        mNotificationProvider = new NotificationProvider();


        mGoogleApiProvider = new GoogleApiProvider(MapDriverBookingActivity.this);
        mExtraClientId = getIntent().getStringExtra("idClient");
        getClient();

        mButtonStartBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsCLoseToClient){
                    startBooking();
                }
                else {
                    Toast.makeText(MapDriverBookingActivity.this, "Debes estar mas cerca a la posicion de recogida", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mButtonFinishBooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishBooking();
            }
        });

    }

    private void finishBooking() {
        mClientBookingProvider.updateStatus(mExtraClientId,"finish");
        mClientBookingProvider.updateIdHistoryBooking(mExtraClientId);
        sendNotification("Viaje finalizado");
        if (mFusedLocation != null){
            mFusedLocation.removeLocationUpdates(mLocationCallBack);
        }
        mGeoFireProvider.removeLocation(mAuth.getId());
        Intent intent = new Intent(MapDriverBookingActivity.this,CalificationClientActivity.class);
        intent.putExtra("idClient",mExtraClientId);
        startActivity(intent);
        finish();
    }

    private double getDistanceBetween(LatLng clientLatLng, LatLng driverLatLng){
        double distance = 0;
        Location clientLocation = new Location("");
        Location driverLocation = new Location("");
        clientLocation.setLatitude(clientLatLng.latitude);
        clientLocation.setLongitude(clientLatLng.longitude);
        driverLocation.setLatitude(driverLatLng.latitude);
        driverLocation.setLongitude(driverLatLng.longitude);
        distance = clientLocation.distanceTo(driverLocation);
        return distance;

    }

    private void startBooking() {
        mClientBookingProvider.updateStatus(mExtraClientId,"start");
        mButtonStartBooking.setVisibility(View.GONE);
        mButtonFinishBooking.setVisibility(View.VISIBLE);
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(mDestinationLatLng).title("Destino").icon(BitmapDescriptorFactory.fromResource(R.drawable.icons8_mapa_de_pin)));
        drawRoute(mDestinationLatLng);
        sendNotification("Viaje iniciado");

    }

    private void getClientBooking() {
        mClientBookingProvider.getClientBooking(mExtraClientId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    String destination = snapshot.child("destination").getValue().toString();
                    String origin = snapshot.child("origin").getValue().toString();
                    double destinationLat = Double.parseDouble(snapshot.child("destinationLat").getValue().toString());
                    double destinationLng = Double.parseDouble(snapshot.child("destinationLng").getValue().toString());

                    double originLat = Double.parseDouble(snapshot.child("originLat").getValue().toString());
                    double originLng = Double.parseDouble(snapshot.child("originLng").getValue().toString());
                    mOriginLatLng = new LatLng(originLat,originLng);
                    mDestinationLatLng = new LatLng(destinationLat,destinationLng);
                    mTextViewOriginClientBooking.setText("recoger en: " + origin);
                    mTextViewDestinationClientBooking.setText("destino: " + destination);
                    mMap.addMarker(new MarkerOptions().position(mOriginLatLng).title("Recoger aqui").icon(BitmapDescriptorFactory.fromResource(R.drawable.icons8_mapa_de_pin_100)));
                    drawRoute(mOriginLatLng);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void drawRoute(LatLng latLng){
        mGoogleApiProvider.getDirections(mCurrentLatLng, latLng).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                try {

                    JSONObject jsonObject = new JSONObject(response.body());
                    JSONArray jsonArray =  jsonObject.getJSONArray("routes");
                    JSONObject route = jsonArray.getJSONObject(0);
                    JSONObject polylines = route.getJSONObject("overview_polyline");
                    String points = polylines.getString("points");
                    mPolylineList = DecodePoints.decodePoly(points);
                    mPolylineOptions = new PolylineOptions();
                    mPolylineOptions.color(Color.DKGRAY);
                    mPolylineOptions.width(8f);
                    mPolylineOptions.startCap(new SquareCap());
                    mPolylineOptions.jointType(JointType.ROUND);
                    mPolylineOptions.addAll(mPolylineList);
                    mMap.addPolyline(mPolylineOptions);

                    JSONArray legs = route.getJSONArray("legs");
                    JSONObject leg = legs.getJSONObject(0);
                    JSONObject distance = leg.getJSONObject("distance");
                    JSONObject duration = leg.getJSONObject("duration");
                    String distanceText = distance.getString("text");
                    String durationText = duration.getString("text");




                } catch (Exception e) {
                    Log.d("Error", "Error encontrado" + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

            }
        });
    }

    private void getClient() {
        mClientProvider.getClient(mExtraClientId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    String email = snapshot.child("email").getValue().toString();
                    String name = snapshot.child("name").getValue().toString();
                    mTextViewClientBooking.setText(name);
                    mTextViewEmailClientBooking.setText(email);

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
            if (!mIsCLoseToClient){
                if (mOriginLatLng != null && mCurrentLatLng != null){
                    double distance = getDistanceBetween(mOriginLatLng,mCurrentLatLng);
                    if (distance <= 200){
                      //  mButtonStartBooking.setEnabled(true);
                        mIsCLoseToClient = true;
                        Toast.makeText(this, "Estas cerca a la posicion de recogida", Toast.LENGTH_SHORT).show();
                    }
                }

            }

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
        startLocation();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED) {
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

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Proporciona los permisos para continuar")
                        .setMessage("Esta aplicacion requiere de los permisos de ubicacion para utilizarse")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MapDriverBookingActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);

                            }
                        }).create().show();
            } else {
                ActivityCompat.requestPermissions(MapDriverBookingActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);

            }


        }

    }

        private void sendNotification(final String status) {
            mTokenProvider.getTokens(mExtraClientId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                        String token = snapshot.child("token").getValue().toString();
                        Map<String, String> map = new HashMap<>();
                        map.put("title", "ESTADO DE TU VIAJE");
                        map.put("body",
                                "Tu estado del viaje es: " + status);
                        FCMBody fcmbody = new FCMBody(token, "high", "4500s",  map);
                        mNotificationProvider.sendNotification(fcmbody).enqueue(new Callback<FCMResponce>() {
                            @Override
                            public void onResponse(Call<FCMResponce> call, Response<FCMResponce> response) {
                                if (response.body() != null) {
                                    if (response.body().getSuccess() != 1) {
                                        Toast.makeText(MapDriverBookingActivity.this, "No se pudo mandar la notificacion", Toast.LENGTH_SHORT).show();

                                    }

                                }
                                else {
                                    Toast.makeText(MapDriverBookingActivity.this, "No se pudo mandar la notificacion", Toast.LENGTH_SHORT).show();

                                }


                            }

                            @Override
                            public void onFailure(Call<FCMResponce> call, Throwable t) {
                                Log.d("Error", "Error" + t.getMessage());
                            }
                        });
                    }
                    else {
                        Toast.makeText(MapDriverBookingActivity.this, "No se pudo enviar la notificacion porque el conductor no tiene un token de sesion", Toast.LENGTH_SHORT).show();

                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        }
    }
