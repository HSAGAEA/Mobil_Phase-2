package com.sti.paceloop.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.sti.paceloop.database.AppDao;
import com.sti.paceloop.database.AppDatabase;
import com.sti.paceloop.database.MoodEntry;
import java.util.List;

public class AnalyticsViewModel extends AndroidViewModel {
    private final AppDao appDao;

    public AnalyticsViewModel(@NonNull Application application) {
        super(application);
        appDao = AppDatabase.getInstance(application).appDao();
    }

    public LiveData<List<MoodEntry>> getRecentMoods() {
        long weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
        return appDao.getRecentMoods(weekAgo);
    }

    public LiveData<Integer> getTotalFocusTime(int userId) {
        return appDao.getTotalFocusTime(userId);
    }

    public LiveData<Double> getAverageBurnout() {
        return appDao.getAverageBurnoutScore();
    }
}
