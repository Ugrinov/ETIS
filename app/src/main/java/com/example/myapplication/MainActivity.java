package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity {

    // это будет именем файла настроек
    public static final String APP_PREFERENCES = "mysettings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences mSettings = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE);
        if(mSettings.contains("surname") && mSettings.contains("password")) {
            setContentView(R.layout.activity_main);

            BottomNavigationView navView = findViewById(R.id.nav_view);
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
            NavigationUI.setupWithNavController(navView, navController);
        }
        else{
            Intent intent = new Intent(this, Auth.class);
            startActivity(intent);
        }
    }

}
