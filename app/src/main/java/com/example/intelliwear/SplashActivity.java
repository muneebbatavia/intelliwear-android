package com.example.intelliwear;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Delay for 2 seconds before moving to the main activity
        new Handler().postDelayed(() -> {
            Intent intent = getIntent();
            String nextActivity = intent.getStringExtra("nextActivity");

            if (nextActivity != null) {
                try {
                    Class<?> activityClass = Class.forName(nextActivity);
                    startActivity(new Intent(SplashActivity.this, activityClass));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    startActivity(new Intent(SplashActivity.this, MainActivity.class)); // Default
                }
            } else {
                startActivity(new Intent(SplashActivity.this, MainActivity.class)); // Default
            }

            finish(); // Close SplashActivity
        }, 2000);
    }
}
