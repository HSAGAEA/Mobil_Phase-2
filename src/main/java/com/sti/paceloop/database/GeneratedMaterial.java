package com.sti.paceloop.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "generated_materials")
public class GeneratedMaterial {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String fileName;
    public String reviewerContent;
    public String quizContent;
    public String answerKeyContent;
    public long timestamp;
    public String difficulty;
}