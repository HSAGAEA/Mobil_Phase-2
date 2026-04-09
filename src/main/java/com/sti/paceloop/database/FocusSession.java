package com.sti.paceloop.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "focus_sessions")
public class FocusSession {
    @PrimaryKey(autoGenerate = true)
    public int sessionId;
    public int userId;
    public long startTime;
    public int duration; // in minutes
    public double burnoutScore;
    public int breakTime; // in minutes

    public FocusSession(int userId, long startTime, int duration, double burnoutScore, int breakTime) {
        this.userId = userId;
        this.startTime = startTime;
        this.duration = duration;
        this.burnoutScore = burnoutScore;
        this.breakTime = breakTime;
    }
}