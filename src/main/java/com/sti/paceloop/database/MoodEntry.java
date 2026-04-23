package com.sti.paceloop.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "mood_entries")
public class MoodEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int stressLevel;
    public int sleepHours;
    public int moodLevel;
    public int copingLevel;
    public double burnoutScore;
    public long timestamp;

    public MoodEntry(int stressLevel, int sleepHours, int moodLevel, int copingLevel, double burnoutScore) {
        this.stressLevel = stressLevel;
        this.sleepHours = sleepHours;
        this.moodLevel = moodLevel;
        this.copingLevel = copingLevel;
        this.burnoutScore = burnoutScore;
        this.timestamp = System.currentTimeMillis();
    }
}