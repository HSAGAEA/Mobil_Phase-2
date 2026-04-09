package com.sti.paceloop.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.sti.paceloop.R;
import com.sti.paceloop.database.AppDatabase;
import com.sti.paceloop.database.User;

public class LoginActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> login());
        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void login() {
        String username = etUsername.getText().toString();
        String password = etPassword.getText().toString();

        new Thread(() -> {
            User user = AppDatabase.getInstance(this).appDao().getUser(username);
            if (user != null && user.passwordHash.equals(password)) {
                SharedPreferences.Editor editor = getSharedPreferences("PaceLoop", MODE_PRIVATE).edit();
                editor.putInt("userId", user.userId);
                editor.putString("username", user.username);
                editor.apply();

                runOnUiThread(() -> {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Invalid Credentials", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}