package com.sti.paceloop.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.sti.paceloop.R;
import com.sti.paceloop.viewmodel.LoginViewModel;

public class LoginActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private LoginViewModel viewModel;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        progressBar = findViewById(R.id.loginProgressBar); // Ensure this exists in layout
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);

        setupObservers();

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            viewModel.login(username, password);
        });

        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void setupObservers() {
        viewModel.getUserLiveData().observe(this, user -> {
            if (user != null) {
                saveSession(user.userId, user.username);
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });

        viewModel.getErrorLiveData().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getLoadingLiveData().observe(this, isLoading -> {
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void saveSession(int userId, String username) {
        SharedPreferences.Editor editor = getSharedPreferences("PaceLoop", MODE_PRIVATE).edit();
        editor.putInt("userId", userId);
        editor.putString("username", username);
        editor.putBoolean("isLoggedIn", true);
        editor.apply();
    }
}
