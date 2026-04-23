package com.sti.paceloop.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.sti.paceloop.database.AppDao;
import com.sti.paceloop.database.AppDatabase;
import com.sti.paceloop.database.User;
import com.sti.paceloop.database.MoodEntry;
import com.sti.paceloop.database.FocusSession;
import com.sti.paceloop.database.GeneratedMaterial;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Calendar;
import java.util.TimeZone;

public class AppRepository {
    private final AppDao appDao;
    // OPTIMIZATION: Use CachedThreadPool for efficient resource management
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    public AppRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        appDao = db.appDao();
    }

    public void insertUser(User user, Runnable callback) {
        executorService.execute(() -> {
            appDao.insertUser(user);
            if (callback != null) callback.run();
        });
    }

    public void getUser(String username, RepoCallback<User> callback) {
        executorService.execute(() -> {
            User user = appDao.getUser(username);
            callback.onResult(user);
        });
    }

    public void insertMood(MoodEntry moodEntry) {
        executorService.execute(() -> appDao.insertMood(moodEntry));
    }

    public void getAllMoods(RepoCallback<List<MoodEntry>> callback) {
        executorService.execute(() -> {
            List<MoodEntry> moods = appDao.getAllMoods();
            callback.onResult(moods);
        });
    }

    public void insertFocusSession(FocusSession session) {
        executorService.execute(() -> appDao.insertFocusSession(session));
    }

    public LiveData<Integer> getDailySessionCount(int userId) {
        return appDao.getDailySessionCount(userId, getStartOfDay());
    }

    public LiveData<Integer> getDailyFocusTime(int userId) {
        return appDao.getDailyFocusTime(userId, getStartOfDay());
    }

    // OPTIMIZATION: Made time zone aware for accurate daily resets
    private long getStartOfDay() {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public void insertMaterial(GeneratedMaterial material) {
        executorService.execute(() -> appDao.insertMaterial(material));
    }

    public void getAllMaterials(RepoCallback<List<GeneratedMaterial>> callback) {
        executorService.execute(() -> {
            List<GeneratedMaterial> materials = appDao.getAllMaterials();
            callback.onResult(materials);
        });
    }

    public interface RepoCallback<T> {
        void onResult(T result);
    }
}