package com.p1.uber.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.p1.uber.R;
import com.p1.uber.activities.client.RegisterMainActivity;
import com.p1.uber.activities.driver.RegisterDriverActivity;

public class SelectOptionAuthActivity extends AppCompatActivity {
    Toolbar mToolbar;
    Button mButtonGoToLogin;
    Button mButtonGoToRegister;
    SharedPreferences mPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_option_auth);
        mToolbar=findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Seleccionar Opcion");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mButtonGoToLogin=findViewById(R.id.btnGoToLogin);
        mButtonGoToRegister=findViewById(R.id.GoToRegister);
        mPref = getApplicationContext().getSharedPreferences( "typeUser",MODE_PRIVATE);


        mButtonGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToLogin();
            }
        });
        mButtonGoToRegister.setOnClickListener(new View.OnClickListener() {
        @Override
            public void onClick(View view){
            goToRegister();
        }
        });
        mPref = getApplicationContext().getSharedPreferences("typeUser", MODE_PRIVATE);

    }
    public void goToLogin(){
        Intent intent= new Intent(SelectOptionAuthActivity.this, LoginActivity.class);
        startActivity(intent);
    }
    public void goToRegister(){
        String typeUser =mPref.getString("user","");
        if (typeUser.equals("activities/driver")){
            Intent intent= new Intent(SelectOptionAuthActivity.this, RegisterDriverActivity.class);
            startActivity(intent);
        }
        else {
            Intent intent= new Intent(SelectOptionAuthActivity.this, RegisterMainActivity.class);
            startActivity(intent);
        }
    }
}