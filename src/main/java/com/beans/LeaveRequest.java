package com.beans;

import java.sql.Timestamp;

public class LeaveRequest {

    private int leaveId;
    private String userId;
    private String leaveType;
    private Timestamp startDate;
    private Timestamp endDate;
    private double totalDays;
    private String status;
    private String reason;
    private String approverId;
    private String approverComments;
    private Timestamp requestedAt;
    private Timestamp reviewedAt;

    public LeaveRequest() {
    }

    public LeaveRequest(int leaveId, String userId, String leaveType, Timestamp startDate,
                       Timestamp endDate, double totalDays, String status, String reason,
                       String approverId, String approverComments, Timestamp requestedAt,
                       Timestamp reviewedAt) {
        this.leaveId = leaveId;
        this.userId = userId;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalDays = totalDays;
        this.status = status;
        this.reason = reason;
        this.approverId = approverId;
        this.approverComments = approverComments;
        this.requestedAt = requestedAt;
        this.reviewedAt = reviewedAt;
    }

    public int getLeaveId() {
        return leaveId;
    }

    public void setLeaveId(int leaveId) {
        this.leaveId = leaveId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public Timestamp getStartDate() {
        return startDate;
    }

    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public Timestamp getEndDate() {
        return endDate;
    }

    public void setEndDate(Timestamp endDate) {
        this.endDate = endDate;
    }

    public double getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(double totalDays) {
        this.totalDays = totalDays;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getApproverId() {
        return approverId;
    }

    public void setApproverId(String approverId) {
        this.approverId = approverId;
    }

    public String getApproverComments() {
        return approverComments;
    }

    public void setApproverComments(String approverComments) {
        this.approverComments = approverComments;
    }

    public Timestamp getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Timestamp requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
