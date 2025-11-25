package com.beans;

import java.sql.Timestamp;

public class TimesheetApproval {

    private int approvalId;
    private String userId;
    private String approverId;
    private Timestamp weekStartDate;
    private Timestamp weekEndDate;
    private String status;
    private String approverComments;
    private Timestamp submittedAt;
    private Timestamp reviewedAt;
    private double totalHours;
    private double billableHours;
    private double nonBillableHours;

    public TimesheetApproval() {
    }

    public TimesheetApproval(int approvalId, String userId, String approverId, Timestamp weekStartDate,
                            Timestamp weekEndDate, String status, String approverComments,
                            Timestamp submittedAt, Timestamp reviewedAt, double totalHours,
                            double billableHours, double nonBillableHours) {
        this.approvalId = approvalId;
        this.userId = userId;
        this.approverId = approverId;
        this.weekStartDate = weekStartDate;
        this.weekEndDate = weekEndDate;
        this.status = status;
        this.approverComments = approverComments;
        this.submittedAt = submittedAt;
        this.reviewedAt = reviewedAt;
        this.totalHours = totalHours;
        this.billableHours = billableHours;
        this.nonBillableHours = nonBillableHours;
    }

    public int getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(int approvalId) {
        this.approvalId = approvalId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getApproverId() {
        return approverId;
    }

    public void setApproverId(String approverId) {
        this.approverId = approverId;
    }

    public Timestamp getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(Timestamp weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public Timestamp getWeekEndDate() {
        return weekEndDate;
    }

    public void setWeekEndDate(Timestamp weekEndDate) {
        this.weekEndDate = weekEndDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApproverComments() {
        return approverComments;
    }

    public void setApproverComments(String approverComments) {
        this.approverComments = approverComments;
    }

    public Timestamp getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Timestamp submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public double getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(double totalHours) {
        this.totalHours = totalHours;
    }

    public double getBillableHours() {
        return billableHours;
    }

    public void setBillableHours(double billableHours) {
        this.billableHours = billableHours;
    }

    public double getNonBillableHours() {
        return nonBillableHours;
    }

    public void setNonBillableHours(double nonBillableHours) {
        this.nonBillableHours = nonBillableHours;
    }
}
