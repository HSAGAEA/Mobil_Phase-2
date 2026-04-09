package com.sti.paceloop.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.sti.paceloop.R;
import com.sti.paceloop.database.AppDatabase;
import com.sti.paceloop.database.FocusSession;
import com.sti.paceloop.database.MoodEntry;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvTimer, tvAiAdvice;
    private Button btnStartPause, btnReset, btnMoodCheck;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private long timeLeftInMillis = 1500000; 
    private double currentBurnoutScore = 0.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvTimer = view.findViewById(R.id.tvTimer);
        tvAiAdvice = view.findViewById(R.id.tvAiAdvice);
        btnStartPause = view.findViewById(R.id.btnStartPause);
        btnReset = view.findViewById(R.id.btnReset);
        btnMoodCheck = view.findViewById(R.id.btnMoodCheck);

        btnStartPause.setOnClickListener(v -> {
            if (isTimerRunning) pauseTimer();
            else startTimer();
        });

        btnReset.setOnClickListener(v -> resetTimer());
        btnMoodCheck.setOnClickListener(v -> showMoodDialog());

        return view;
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                btnStartPause.setText("Start");
                saveSession();
                Toast.makeText(getContext(), "Focus session complete!", Toast.LENGTH_SHORT).show();
            }
        }.start();
        isTimerRunning = true;
        btnStartPause.setText("Pause");
    }

    private void pauseTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        isTimerRunning = false;
        btnStartPause.setText("Start");
    }

    private void resetTimer() {
        timeLeftInMillis = 1500000;
        updateCountDownText();
        if (isTimerRunning) pauseTimer();
    }

    private void updateCountDownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void showMoodDialog() {
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_mood);
        
        EditText etStress = dialog.findViewById(R.id.etStress);
        EditText etSleep = dialog.findViewById(R.id.etSleep);
        EditText etMood = dialog.findViewById(R.id.etMood);
        EditText etCoping = dialog.findViewById(R.id.etCoping);
        Button btnCalculate = dialog.findViewById(R.id.btnCalculate);

        btnCalculate.setOnClickListener(v -> {
            try {
                int stress = Integer.parseInt(etStress.getText().toString());
                int sleep = Integer.parseInt(etSleep.getText().toString());
                int mood = Integer.parseInt(etMood.getText().toString());
                int coping = Integer.parseInt(etCoping.getText().toString());

                currentBurnoutScore = (stress * 0.4) + ((10 - sleep) * 0.3) + ((5 - mood) * 0.2) + ((5 - coping) * 0.1);
                
                new Thread(() -> {
                    MoodEntry entry = new MoodEntry(stress, sleep, mood, coping, currentBurnoutScore);
                    AppDatabase.getInstance(getContext()).appDao().insertMood(entry);
                    
                    getActivity().runOnUiThread(() -> {
                        updateAiAdvice();
                        adjustTimerBasedOnBurnout();
                        dialog.dismiss();
                    });
                }).start();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    private void updateAiAdvice() {
        String advice;
        if (currentBurnoutScore >= 7.0) advice = "AI: High Burnout Risk! Focus time reduced to 15m. Take a longer break.";
        else if (currentBurnoutScore >= 4.0) advice = "AI: Moderate Risk. Stick to 25m but don't skip your break.";
        else advice = "AI: Looking good! Keep up the great pace.";
        tvAiAdvice.setText(advice);
    }

    private void adjustTimerBasedOnBurnout() {
        if (currentBurnoutScore >= 7.0) {
            timeLeftInMillis = 900000; // 15 mins
            updateCountDownText();
        }
    }

    private void saveSession() {
        SharedPreferences prefs = getActivity().getSharedPreferences("PaceLoop", Context.MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);
        int duration = 25; // default
        if (currentBurnoutScore >= 7.0) duration = 15;

        int finalDuration = duration;
        new Thread(() -> {
            FocusSession session = new FocusSession(userId, System.currentTimeMillis(), finalDuration, currentBurnoutScore, 5);
            AppDatabase.getInstance(getContext()).appDao().insertFocusSession(session);
        }).start();
    }
}