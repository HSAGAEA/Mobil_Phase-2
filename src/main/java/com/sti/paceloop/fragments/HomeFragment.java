package com.sti.paceloop.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.sti.paceloop.R;
import com.sti.paceloop.viewmodel.HomeViewModel;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvTimer, tvAiAdvice, tvFocusScore, tvUserStatus, tvDailyProgress;
    private Button btnStartPause, btnReset, btnMoodCheck;
    private CircularProgressIndicator timerProgress;
    private LinearProgressIndicator dailyProgressBar;
    private CountDownTimer countDownTimer;
    private HomeViewModel viewModel;

    private boolean isTimerRunning = false;
    private boolean isFocusMode = true;
    private long timeLeftInMillis = 1500000; // 25 mins
    private long totalSessionTime = 1500000;

    // OPTIMIZATION: Anti-cheat variables
    private int interactionCount = 0;
    private int idleSeconds = 0;
    private long lastTouchTime = 0;

    private Handler idleHandler = new Handler(Looper.getMainLooper());
    private Runnable idleRunnable;

    private int sessionInSeries = 0;
    private int dailySessions = 0;
    private final int MAX_DAILY_SESSIONS = 8;
    private boolean isRestingForced = false;
    private String currentStatus = "Fresh";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        SharedPreferences prefs = requireActivity().getSharedPreferences("PaceLoop", Context.MODE_PRIVATE);
        viewModel.setUserId(prefs.getInt("userId", -1));

        initViews(view);
        setupObservers();
        setupActivityTracking(view);

        btnStartPause.setOnClickListener(v -> {
            if (isTimerRunning) pauseTimer(false);
            else startTimer();
        });

        btnReset.setOnClickListener(v -> resetTimer());
        btnMoodCheck.setOnClickListener(v -> showMoodDialog());

        return view;
    }

    private void initViews(View view) {
        tvTimer = view.findViewById(R.id.tvTimer);
        tvAiAdvice = view.findViewById(R.id.tvAiAdvice);
        tvFocusScore = view.findViewById(R.id.tvFocusScore);
        tvUserStatus = view.findViewById(R.id.tvUserStatus);
        tvDailyProgress = view.findViewById(R.id.tvDailyProgress);
        btnStartPause = view.findViewById(R.id.btnStartPause);
        btnReset = view.findViewById(R.id.btnReset);
        btnMoodCheck = view.findViewById(R.id.btnMoodCheck);
        timerProgress = view.findViewById(R.id.timerProgress);
        dailyProgressBar = view.findViewById(R.id.dailyProgressBar);

        dailyProgressBar.setMax(MAX_DAILY_SESSIONS);
    }

    private void setupObservers() {
        viewModel.getAiRecommendation().observe(getViewLifecycleOwner(), rec -> tvAiAdvice.setText(rec));

        viewModel.getDailySessionCount().observe(getViewLifecycleOwner(), count -> {
            dailySessions = count != null ? count : 0;
            updateDailyProgress();
        });

        viewModel.getFocusPoints().observe(getViewLifecycleOwner(), points -> tvFocusScore.setText(points + " pts"));

        viewModel.getFatigueStatus().observe(getViewLifecycleOwner(), status -> {
            currentStatus = status;
            tvUserStatus.setText(status);

            if ("Burned Out".equals(status)) {
                tvUserStatus.setTextColor(Color.RED);
                if (!isTimerRunning) {
                    btnStartPause.setEnabled(false);
                    btnStartPause.setAlpha(0.5f);
                }
                tvAiAdvice.setText("Status: Burned Out. Rest is mandatory before starting a new session.");
            } else if ("Tired".equals(status)) {
                tvUserStatus.setTextColor(Color.parseColor("#FFA500"));
                btnStartPause.setEnabled(true);
                btnStartPause.setAlpha(1.0f);
            } else {
                tvUserStatus.setTextColor(Color.parseColor("#4CAF50"));
                btnStartPause.setEnabled(true);
                btnStartPause.setAlpha(1.0f);
            }
        });

        viewModel.getBurnoutScore().observe(getViewLifecycleOwner(), score -> {
            if (score >= 7.8 && isFocusMode && !isTimerRunning) {
                timeLeftInMillis = 900000; // Adaptive Timer: 15 mins for high stress
                totalSessionTime = 900000;
                updateCountDownText();
            }
        });
    }

    private void setupActivityTracking(View view) {
        View root = view.findViewById(R.id.rootLayout);
        root.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                long currentTime = System.currentTimeMillis();
                // OPTIMIZATION: Anti-cheat debounce (500ms) to ignore random spam tapping
                if (currentTime - lastTouchTime > 500) {
                    interactionCount++;
                    idleSeconds = 0;
                    lastTouchTime = currentTime;
                }
            }
            return false;
        });

        idleRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTimerRunning && isFocusMode) {
                    idleSeconds++;
                    // Soft pause instead of hard stop on deep idle
                    if (idleSeconds >= 120) {
                        pauseTimer(true);
                        Toast.makeText(getContext(), "Session paused due to prolonged inactivity", Toast.LENGTH_LONG).show();
                    } else if (idleSeconds % 45 == 0) { // Query AI sparingly
                        viewModel.detectFatigue(idleSeconds, interactionCount, 0);
                    }
                }
                idleHandler.postDelayed(this, 1000);
            }
        };
    }

    private void startTimer() {
        if ("Burned Out".equals(currentStatus) && isFocusMode) {
            showLimitReachedDialog("You are Burned Out. Please take a long rest.");
            return;
        }
        if (dailySessions >= MAX_DAILY_SESSIONS && isFocusMode) {
            showLimitReachedDialog("Daily limit reached. Time to rest your mind.");
            return;
        }
        if (isRestingForced) {
            Toast.makeText(getContext(), "Forced Rest active. Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }

        long targetEndTime = System.currentTimeMillis() + timeLeftInMillis;
        viewModel.setTimerState(true, targetEndTime);

        countDownTimer = new CountDownTimer(timeLeftInMillis, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                handleSessionFinish();
            }
        }.start();
        isTimerRunning = true;
        btnStartPause.setText("Pause");
        btnStartPause.setEnabled(true);
        btnStartPause.setAlpha(1.0f);
    }

    private void pauseTimer(boolean isAutoPause) {
        if (countDownTimer != null) countDownTimer.cancel();
        isTimerRunning = false;
        viewModel.setTimerState(false, 0);
        btnStartPause.setText("Resume");
    }

    private void resetTimer() {
        pauseTimer(false);
        Double score = viewModel.getBurnoutScore().getValue();
        if (isFocusMode) {
            timeLeftInMillis = (score != null && score >= 7.8) ? 900000 : 1500000;
            totalSessionTime = timeLeftInMillis;
            interactionCount = 0;
            idleSeconds = 0;
        } else {
            timeLeftInMillis = 300000;
            totalSessionTime = 300000;
        }
        updateCountDownText();
        btnStartPause.setText("Start");
    }

    private void updateCountDownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        int progress = (int) ((timeLeftInMillis * 100) / totalSessionTime);
        timerProgress.setProgress(Math.max(0, Math.min(progress, 100)));
    }

    private void handleSessionFinish() {
        isTimerRunning = false;
        viewModel.setTimerState(false, 0);
        btnStartPause.setText("Start");

        if (isFocusMode) {
            // Validate minimum viable interaction for completion
            if (interactionCount >= 3) {
                completeFocusSession();
            } else {
                markIncomplete();
            }
        } else {
            completeBreak();
        }
    }

    private void completeFocusSession() {
        dailySessions++;
        sessionInSeries++;
        saveSession(true);

        new AlertDialog.Builder(requireContext())
                .setTitle("Session Complete!")
                .setMessage("Great job! Proceed to review or take a break?")
                .setCancelable(false)
                .setPositiveButton("Continue (No Break)", (d, w) -> {
                    viewModel.penalizeSkippedBreak();
                    startTimer();
                })
                .setNegativeButton("Take Break", (d, w) -> startBreak())
                .show();

        if (sessionInSeries >= 4) {
            triggerForcedRest();
        }
    }

    private void markIncomplete() {
        saveSession(false);
        Toast.makeText(getContext(), "Session Incomplete: Insufficient activity detected.", Toast.LENGTH_LONG).show();
        resetTimer();
    }

    private void startBreak() {
        isFocusMode = false;
        if (sessionInSeries >= 4) {
            timeLeftInMillis = 1800000; // 30 min long break
            totalSessionTime = 1800000;
            sessionInSeries = 0;
        } else {
            timeLeftInMillis = 300000; // 5 min short break
            totalSessionTime = 300000;
        }
        updateCountDownText();
        startTimer();
        tvAiAdvice.setText("Break time! Step away from the screen.");
    }

    private void completeBreak() {
        isFocusMode = true;
        resetTimer();
        Toast.makeText(getContext(), "Break over! Time to focus.", Toast.LENGTH_SHORT).show();
    }

    private void triggerForcedRest() {
        isRestingForced = true;
        pauseTimer(true);
        tvAiAdvice.setText("Forced Rest: You've completed 4 sessions. Take 15 mins to recharge.");
        new Handler(Looper.getMainLooper()).postDelayed(() -> isRestingForced = false, 900000);
    }

    private void showLimitReachedDialog(String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Action Restricted")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void updateDailyProgress() {
        if (tvDailyProgress != null && dailyProgressBar != null) {
            tvDailyProgress.setText("Daily Goal: " + dailySessions + "/" + MAX_DAILY_SESSIONS + " sessions");
            dailyProgressBar.setProgress(dailySessions);
        }
    }

    private void saveSession(boolean completed) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("PaceLoop", Context.MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);
        int duration = (int) (totalSessionTime / 60000);
        viewModel.saveFocusSession(userId, duration, completed, completed ? 10 : 0);
        interactionCount = 0;
        idleSeconds = 0;
    }

    private void showMoodDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_mood);
        EditText etStress = dialog.findViewById(R.id.etStress);
        EditText etSleep = dialog.findViewById(R.id.etSleep);
        EditText etMood = dialog.findViewById(R.id.etMood);
        EditText etCoping = dialog.findViewById(R.id.etCoping);
        Button btnCalculate = dialog.findViewById(R.id.btnCalculate);

        btnCalculate.setOnClickListener(v -> {
            try {
                int stress = Math.min(10, Math.max(0, Integer.parseInt(etStress.getText().toString())));
                int sleep = Math.min(24, Math.max(0, Integer.parseInt(etSleep.getText().toString())));
                int mood = Math.min(5, Math.max(0, Integer.parseInt(etMood.getText().toString())));
                int coping = Math.min(5, Math.max(0, Integer.parseInt(etCoping.getText().toString())));
                viewModel.submitMood(stress, sleep, mood, coping);
                dialog.dismiss();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Please fill valid numbers in all fields", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    // OPTIMIZATION: Lifecycle methods handle background survival and memory leaks
    @Override
    public void onResume() {
        super.onResume();
        idleHandler.post(idleRunnable);

        if (viewModel.isTimerRunning()) {
            long target = viewModel.getTargetEndTime();
            long remaining = target - System.currentTimeMillis();
            if (remaining > 0) {
                timeLeftInMillis = remaining;
                startTimer();
            } else {
                timeLeftInMillis = 0;
                handleSessionFinish();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        idleHandler.removeCallbacksAndMessages(null);
        if (isTimerRunning && countDownTimer != null) {
            countDownTimer.cancel();
            // Do not alter viewModel targetEndTime so it can survive background state
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) countDownTimer.cancel();
        idleHandler.removeCallbacksAndMessages(null);
    }
}