package com.sti.paceloop.database;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// OPTIMIZATION: Added indices for faster daily queries and aggregation
@Entity(tableName = "focus_sessions", indices = {@Index("userId"), @Index("timestamp")})
public class FocusSession {
    @PrimaryKey(autoGenerate = true)
    public int sessionId;
    public int userId;
    public long startTime;
    public int duration; // in minutes
    public double burnoutScore;
    public int breakTime; // in minutes
    public boolean isCompleted;
    public int focusScore;
    public long timestamp; // for daily filtering

    public FocusSession(int userId, long startTime, int duration, double burnoutScore, int breakTime) {
        this.userId = userId;
        this.startTime = startTime;
        this.duration = duration;
        this.burnoutScore = burnoutScore;
        this.breakTime = breakTime;
        this.isCompleted = true; // default for backward compatibility
        this.timestamp = System.currentTimeMillis();
    }
}