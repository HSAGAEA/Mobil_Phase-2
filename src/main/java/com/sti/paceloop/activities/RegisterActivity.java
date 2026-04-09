package com.sti.paceloop.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.sti.paceloop.R;
import com.sti.paceloop.database.AppDatabase;
import com.sti.paceloop.database.User;

public class RegisterActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUsername = findViewById(R.id.etRegUsername);
        etPassword = findViewById(R.id.etRegPassword);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvLogin = findViewById(R.id.tvLogin);

        btnRegister.setOnClickListener(v -> register());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void register() {
        String username = etUsername.getText().toString();
        String password = etPassword.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            User existing = AppDatabase.getInstance(this).appDao().getUser(username);
            if (existing == null) {
                AppDatabase.getInstance(this).appDao().insertUser(new User(username, password));
                runOnUiThread(() -> {
                    Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } else {
                runOnUiThread(() -> Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}