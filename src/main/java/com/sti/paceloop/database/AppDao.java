package com.sti.paceloop.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(User user);

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User getUser(String username);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMood(MoodEntry moodEntry);

    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC LIMIT 50") // OPTIMIZATION: Limit to recent for performance
    List<MoodEntry> getAllMoods();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertFocusSession(FocusSession session);

    @Query("SELECT COALESCE(SUM(duration), 0) FROM focus_sessions WHERE userId = :userId")
    LiveData<Integer> getTotalFocusTime(int userId);

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE userId = :userId")
    LiveData<Integer> getTotalSessions(int userId);

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE userId = :userId AND timestamp >= :startOfDay AND isCompleted = 1")
    LiveData<Integer> getDailySessionCount(int userId, long startOfDay);

    @Query("SELECT COALESCE(SUM(duration), 0) FROM focus_sessions WHERE userId = :userId AND timestamp >= :startOfDay AND isCompleted = 1")
    LiveData<Integer> getDailyFocusTime(int userId, long startOfDay);

    @Query("SELECT AVG(burnoutScore) FROM mood_entries")
    LiveData<Double> getAverageBurnoutScore();

    @Query("SELECT * FROM mood_entries WHERE timestamp > :since ORDER BY timestamp ASC")
    LiveData<List<MoodEntry>> getRecentMoods(long since);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMaterial(GeneratedMaterial material);

    @Query("SELECT * FROM generated_materials ORDER BY timestamp DESC")
    List<GeneratedMaterial> getAllMaterials();
}