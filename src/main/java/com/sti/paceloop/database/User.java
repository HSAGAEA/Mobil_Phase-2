package com.sti.paceloop.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int userId;
    public String username;
    public String passwordHash;
    public long createdAt;

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = System.currentTimeMillis();
    }
}