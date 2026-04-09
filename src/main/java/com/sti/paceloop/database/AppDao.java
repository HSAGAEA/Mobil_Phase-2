package com.sti.paceloop.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface AppDao {
    @Insert
    void insertUser(User user);

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User getUser(String username);

    @Insert
    void insertMood(MoodEntry moodEntry);

    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC")
    List<MoodEntry> getAllMoods();

    @Insert
    void insertFocusSession(FocusSession session);

    @Query("SELECT SUM(duration) FROM focus_sessions WHERE userId = :userId")
    int getTotalFocusTime(int userId);

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE userId = :userId")
    int getTotalSessions(int userId);

    @Query("SELECT AVG(burnoutScore) FROM mood_entries")
    double getAverageBurnoutScore();
    
    @Query("SELECT * FROM mood_entries WHERE timestamp > :since ORDER BY timestamp ASC")
    List<MoodEntry> getRecentMoods(long since);
}