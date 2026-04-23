package com.sti.paceloop.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.sti.paceloop.database.FocusSession;
import com.sti.paceloop.database.MoodEntry;
import com.sti.paceloop.repository.AppRepository;
import com.sti.paceloop.utils.GeminiAIManager;

public class HomeViewModel extends AndroidViewModel {
    private final AppRepository repository;
    private final GeminiAIManager geminiManager;

    private final MutableLiveData<String> aiRecommendation = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingAi = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> burnoutScore = new MutableLiveData<>(0.0);
    private final MutableLiveData<Integer> userId = new MutableLiveData<>(-1);

    private final MutableLiveData<Integer> focusPoints = new MutableLiveData<>(0);
    private final MutableLiveData<String> fatigueStatus = new MutableLiveData<>("Fresh");

    // OPTIMIZATION: Timer survival states
    private long targetEndTime = 0;
    private boolean isTimerRunning = false;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);
        geminiManager = new GeminiAIManager();
    }

    public void setUserId(int id) { userId.setValue(id); }
    public LiveData<Integer> getDailySessionCount() { return Transformations.switchMap(userId, id -> id == -1 ? new MutableLiveData<>(0) : repository.getDailySessionCount(id)); }
    public LiveData<Integer> getDailyFocusTime() { return Transformations.switchMap(userId, id -> id == -1 ? new MutableLiveData<>(0) : repository.getDailyFocusTime(id)); }
    public LiveData<String> getAiRecommendation() { return aiRecommendation; }
    public LiveData<Boolean> getIsLoadingAi() { return isLoadingAi; }
    public LiveData<String> getErrorLiveData() { return errorLiveData; }
    public LiveData<Double> getBurnoutScore() { return burnoutScore; }
    public LiveData<Integer> getFocusPoints() { return focusPoints; }
    public LiveData<String> getFatigueStatus() { return fatigueStatus; }

    // Timer State getters/setters for lifecycle survival
    public void setTimerState(boolean running, long targetEnd) {
        this.isTimerRunning = running;
        this.targetEndTime = targetEnd;
    }
    public boolean isTimerRunning() { return isTimerRunning; }
    public long getTargetEndTime() { return targetEndTime; }

    public void submitMood(int stress, int sleep, int mood, int coping) {
        double rawScore = (stress * 0.4) + ((10 - sleep) * 0.3) + ((5 - mood) * 0.2) + ((5 - coping) * 0.1);

        // OPTIMIZATION: Smoothing algorithm (EMA) to prevent erratic status jumps
        Double currentScore = burnoutScore.getValue();
        double smoothedScore = (currentScore == null || currentScore == 0.0) ? rawScore : (currentScore * 0.6) + (rawScore * 0.4);

        burnoutScore.setValue(smoothedScore);

        // Dynamic thresholding
        if (smoothedScore >= 7.8) {
            fatigueStatus.setValue("Burned Out");
        } else if (smoothedScore >= 4.5) {
            fatigueStatus.setValue("Tired");
        } else {
            fatigueStatus.setValue("Fresh");
        }

        MoodEntry entry = new MoodEntry(stress, sleep, mood, coping, smoothedScore);
        repository.insertMood(entry);
        fetchAiRecommendation(stress, sleep, mood, coping, smoothedScore);
    }

    private void fetchAiRecommendation(int stress, int sleep, int mood, int coping, double score) {
        isLoadingAi.setValue(true);
        geminiManager.getRecommendation(stress, sleep, mood, coping, score, new GeminiAIManager.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                isLoadingAi.postValue(false);
                aiRecommendation.postValue(response);
            }
            @Override
            public void onError(Throwable throwable) {
                isLoadingAi.postValue(false);
                aiRecommendation.postValue(getLocalRecommendation(score));
                errorLiveData.postValue("AI is currently unavailable.");
            }
        });
    }

    public void detectFatigue(int idleTime, int interactionCount, int mistakes) {
        geminiManager.detectFatigue(idleTime, interactionCount, mistakes, new GeminiAIManager.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                if (response.contains("fatigue") || response.contains("break")) {
                    aiRecommendation.postValue(response);
                    if (!"Burned Out".equals(fatigueStatus.getValue())) {
                        fatigueStatus.postValue("Tired");
                    }
                }
            }
            @Override public void onError(Throwable throwable) {}
        });
    }

    private String getLocalRecommendation(double score) {
        if (score >= 7.8) return "Burnout risk is high. Please prioritize a 15-minute rest now.";
        if (score >= 4.5) return "You're doing okay, but consider taking a short screen break.";
        return "You're in the zone! Keep up the excellent work.";
    }

    public void saveFocusSession(int userId, int duration, boolean isCompleted, int focusScore) {
        Double currentScore = burnoutScore.getValue();
        FocusSession session = new FocusSession(userId, System.currentTimeMillis(), duration, currentScore != null ? currentScore : 0.0, 5);
        session.isCompleted = isCompleted;
        session.focusScore = focusScore;
        repository.insertFocusSession(session);

        if (isCompleted) {
            Integer current = focusPoints.getValue();
            focusPoints.setValue((current != null ? current : 0) + 10);
        }
    }

    public void penalizeSkippedBreak() {
        Integer current = focusPoints.getValue();
        focusPoints.setValue(Math.max(0, (current != null ? current : 0) - 5));
    }
}