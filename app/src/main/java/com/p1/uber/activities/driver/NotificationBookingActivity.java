package com.p1.uber.activities.driver;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.p1.uber.R;

import com.p1.uber.provides.AuthProvides;
import com.p1.uber.provides.ClientBookingProvider;
import com.p1.uber.provides.GeofireProvider;

public class NotificationBookingActivity extends AppCompatActivity {

    private TextView mTextViewDestination;
    private TextView mTextViewOrigin;
    private TextView mTextViewMin;
    private TextView mTextViewDistance;
    private TextView mTextViewCounter;
    private Button mButtonAccept;
    private Button mButtonCancel;

    private ClientBookingProvider mClientBookingProvider;
    private GeofireProvider mGeofireProvider;
    private AuthProvides mAuthProvider;

    private String mExtraIdClient;
    private String mExtraOrigin;
    private String mExtraDestination;
    private String mExtraDistance;
    private String mExtraMin;

    private MediaPlayer mMediaPlayer;
    private int mCounter = 10;
    private Handler mHandler;
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            mCounter = mCounter -1 ;
            mTextViewCounter.setText(String.valueOf(mCounter));
            if (mCounter >0){
                initTime();
            }
            else {
                cancelBooking();
            }
        }
    };
    private ValueEventListener mListener;

    private void initTime() {
        mHandler = new Handler();
        mHandler.postDelayed(runnable, 1000);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_booking);

           mTextViewDestination = findViewById(R.id.textViewDestination);
           mTextViewOrigin = findViewById(R.id.textViewOrigin);
           mTextViewMin = findViewById(R.id.textViewMin);
           mTextViewCounter = findViewById(R.id.textViewCounter);
           mTextViewDistance = findViewById(R.id.textViewDistance);
           mButtonAccept = findViewById(R.id.btnAcceptBooking);
           mButtonCancel = findViewById(R.id.btnCancelBooking);

           mExtraIdClient = getIntent().getStringExtra("idClient");
           mExtraOrigin = getIntent().getStringExtra("origin");
           mExtraDestination = getIntent().getStringExtra("destination");
           mExtraDistance = getIntent().getStringExtra("distance");
           mExtraMin = getIntent().getStringExtra("min");

           mTextViewDestination.setText(mExtraDestination);
           mTextViewOrigin.setText(mExtraOrigin);
           mTextViewDistance.setText(mExtraDistance);
           mTextViewMin.setText(mExtraMin);
           mClientBookingProvider = new ClientBookingProvider();


        mMediaPlayer = MediaPlayer.create(this,R.raw.ringtone);
            mMediaPlayer.setLooping(true);
           getWindow().addFlags(
                   WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                   WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                   WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
           );
           initTime();

           checkIfClientCancelBooking();

        mButtonAccept.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   acceptBooking();
               }
           });

           mButtonCancel.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {
                   cancelBooking();
               }
           });
    }

    private void checkIfClientCancelBooking(){
       mListener =  mClientBookingProvider.getClientBooking(mExtraIdClient).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()){
                    Toast.makeText(NotificationBookingActivity.this, "El cliene cancelo el viaje", Toast.LENGTH_LONG).show();
                    if (mHandler != null) mHandler.removeCallbacks(runnable);
                    Intent intent = new Intent(NotificationBookingActivity.this,MapDriverActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void cancelBooking() {
        if (mHandler != null) mHandler.removeCallbacks(runnable);
        mClientBookingProvider.updateStatus(mExtraIdClient, "cancel");

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(2);
        Intent intent = new Intent(NotificationBookingActivity.this,MapDriverActivity.class);
        startActivity(intent);
        finish();
    }

    private void acceptBooking() {
        if (mHandler != null) mHandler.removeCallbacks(runnable);
        mAuthProvider =  new AuthProvides();
        mGeofireProvider = new GeofireProvider("active_driver");
        mGeofireProvider.removeLocation(mAuthProvider.getId());


        mClientBookingProvider = new ClientBookingProvider();
        mClientBookingProvider.updateStatus(mExtraIdClient, "accept");

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(2);

        Intent intent1 = new Intent(NotificationBookingActivity.this, MapDriverBookingActivity.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent1.setAction(Intent.ACTION_RUN);
        intent1.putExtra("idClient",mExtraIdClient);
        startActivity(intent1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMediaPlayer != null){
            if (mMediaPlayer.isPlaying()){
                mMediaPlayer.pause();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mMediaPlayer != null){
            if (mMediaPlayer.isPlaying()){
                mMediaPlayer.release();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMediaPlayer != null){
            if (!mMediaPlayer.isPlaying()){
                mMediaPlayer.start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHandler != null) mHandler.removeCallbacks(runnable);

        if (mMediaPlayer != null){
            if (mMediaPlayer.isPlaying()){
                mMediaPlayer.pause();
            }
        }
        if (mListener != null){
            mClientBookingProvider.getClientBooking(mExtraIdClient);
        }

    }
}