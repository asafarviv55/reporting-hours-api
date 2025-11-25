package com.beans;

public class Project {

    private int projectId;
    private String projectName;
    private String clientName;
    private String description;
    private double hourlyRate;
    private double budgetHours;
    private String status;
    private String projectManager;

    public Project() {
    }

    public Project(int projectId, String projectName, String clientName, String description,
                   double hourlyRate, double budgetHours, String status, String projectManager) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.clientName = clientName;
        this.description = description;
        this.hourlyRate = hourlyRate;
        this.budgetHours = budgetHours;
        this.status = status;
        this.projectManager = projectManager;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public double getBudgetHours() {
        return budgetHours;
    }

    public void setBudgetHours(double budgetHours) {
        this.budgetHours = budgetHours;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProjectManager() {
        return projectManager;
    }

    public void setProjectManager(String projectManager) {
        this.projectManager = projectManager;
    }
}
