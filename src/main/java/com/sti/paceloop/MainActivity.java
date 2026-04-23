package com.sti.paceloop;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.sti.paceloop.activities.SplashActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Redirect to SplashActivity or remove this file if not needed
        startActivity(new Intent(this, SplashActivity.class));
        finish();
    }
}