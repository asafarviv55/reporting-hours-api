package com.beans;

import java.sql.Timestamp;

public class TimeEntry {

    private int entryId;
    private String userId;
    private int projectId;
    private Timestamp startTime;
    private Timestamp endTime;
    private double hoursWorked;
    private boolean isBillable;
    private String description;
    private String entryType;
    private Timestamp createdAt;

    public TimeEntry() {
    }

    public TimeEntry(int entryId, String userId, int projectId, Timestamp startTime,
                     Timestamp endTime, double hoursWorked, boolean isBillable,
                     String description, String entryType, Timestamp createdAt) {
        this.entryId = entryId;
        this.userId = userId;
        this.projectId = projectId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.hoursWorked = hoursWorked;
        this.isBillable = isBillable;
        this.description = description;
        this.entryType = entryType;
        this.createdAt = createdAt;
    }

    public int getEntryId() {
        return entryId;
    }

    public void setEntryId(int entryId) {
        this.entryId = entryId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public double getHoursWorked() {
        return hoursWorked;
    }

    public void setHoursWorked(double hoursWorked) {
        this.hoursWorked = hoursWorked;
    }

    public boolean isBillable() {
        return isBillable;
    }

    public void setIsBillable(boolean isBillable) {
        this.isBillable = isBillable;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public double calculateDuration() {
        if (startTime != null && endTime != null) {
            long milliseconds = endTime.getTime() - startTime.getTime();
            return milliseconds / (1000.0 * 60 * 60);
        }
        return hoursWorked;
    }
}
