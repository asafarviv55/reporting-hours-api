package com.services;

import com.beans.LeaveRequest;
import com.db.Dbfactory;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaveManagementService {

    static Logger logger = Logger.getRootLogger();

    public static boolean submitLeaveRequest(String userId, String leaveType, Timestamp startDate,
                                            Timestamp endDate, String reason) throws SQLException {
        logger.info("LeaveManagementService.submitLeaveRequest entering...");

        Connection con = Dbfactory.getConnection();
        try {
            double totalDays = calculateLeaveDays(startDate, endDate);

            String sql = "INSERT INTO reporting_system.leave_requests " +
                        "(user_id, leave_type, start_date, end_date, total_days, status, reason, requested_at) " +
                        "VALUES (?, ?, ?, ?, ?, 'PENDING', ?, ?)";
            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setString(1, userId);
            preparedStmt.setString(2, leaveType);
            preparedStmt.setTimestamp(3, startDate);
            preparedStmt.setTimestamp(4, endDate);
            preparedStmt.setDouble(5, totalDays);
            preparedStmt.setString(6, reason);
            preparedStmt.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
            preparedStmt.execute();
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }
        return false;
    }

    public static boolean approveLeaveRequest(int leaveId, String approverId, String comments) throws SQLException {
        logger.info("LeaveManagementService.approveLeaveRequest entering...");

        Connection con = Dbfactory.getConnection();
        try {
            String sql = "UPDATE reporting_system.leave_requests " +
                        "SET status = 'APPROVED', approver_id = ?, approver_comments = ?, " +
                        "reviewed_at = ? WHERE leave_id = ?";
            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setString(1, approverId);
            preparedStmt.setString(2, comments);
            preparedStmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            preparedStmt.setInt(4, leaveId);
            preparedStmt.execute();
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }
        return false;
    }

    public static boolean rejectLeaveRequest(int leaveId, String approverId, String comments) throws SQLException {
        logger.info("LeaveManagementService.rejectLeaveRequest entering...");

        Connection con = Dbfactory.getConnection();
        try {
            String sql = "UPDATE reporting_system.leave_requests " +
                        "SET status = 'REJECTED', approver_id = ?, approver_comments = ?, " +
                        "reviewed_at = ? WHERE leave_id = ?";
            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setString(1, approverId);
            preparedStmt.setString(2, comments);
            preparedStmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            preparedStmt.setInt(4, leaveId);
            preparedStmt.execute();
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }
        return false;
    }

    public static List<LeaveRequest> getLeaveRequestsByUser(String userId) throws SQLException {
        logger.info("LeaveManagementService.getLeaveRequestsByUser entering...");

        Connection con = Dbfactory.getConnection();
        List<LeaveRequest> list = new ArrayList<>();
        try {
            String sql = "SELECT * FROM reporting_system.leave_requests WHERE user_id = ? ORDER BY requested_at DESC";
            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setString(1, userId);
            ResultSet rs = preparedStmt.executeQuery();

            while (rs.next()) {
                LeaveRequest leave = new LeaveRequest();
                leave.setLeaveId(rs.getInt("leave_id"));
                leave.setUserId(rs.getString("user_id"));
                leave.setLeaveType(rs.getString("leave_type"));
                leave.setStartDate(rs.getTimestamp("start_date"));
                leave.setEndDate(rs.getTimestamp("end_date"));
                leave.setTotalDays(rs.getDouble("total_days"));
                leave.setStatus(rs.getString("status"));
                leave.setReason(rs.getString("reason"));
                leave.setApproverId(rs.getString("approver_id"));
                leave.setApproverComments(rs.getString("approver_comments"));
                leave.setRequestedAt(rs.getTimestamp("requested_at"));
                leave.setReviewedAt(rs.getTimestamp("reviewed_at"));
                list.add(leave);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }
        return list;
    }

    public static List<LeaveRequest> getPendingLeaveRequests() throws SQLException {
        logger.info("LeaveManagementService.getPendingLeaveRequests entering...");

        Connection con = Dbfactory.getConnection();
        List<LeaveRequest> list = new ArrayList<>();
        try {
            String sql = "SELECT * FROM reporting_system.leave_requests WHERE status = 'PENDING' ORDER BY requested_at ASC";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                LeaveRequest leave = new LeaveRequest();
                leave.setLeaveId(rs.getInt("leave_id"));
                leave.setUserId(rs.getString("user_id"));
                leave.setLeaveType(rs.getString("leave_type"));
                leave.setStartDate(rs.getTimestamp("start_date"));
                leave.setEndDate(rs.getTimestamp("end_date"));
                leave.setTotalDays(rs.getDouble("total_days"));
                leave.setStatus(rs.getString("status"));
                leave.setReason(rs.getString("reason"));
                leave.setApproverId(rs.getString("approver_id"));
                leave.setApproverComments(rs.getString("approver_comments"));
                leave.setRequestedAt(rs.getTimestamp("requested_at"));
                leave.setReviewedAt(rs.getTimestamp("reviewed_at"));
                list.add(leave);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }
        return list;
    }

    public static Map<String, Double> getLeaveBalance(String userId, int year) throws SQLException {
        logger.info("LeaveManagementService.getLeaveBalance entering...");

        Connection con = Dbfactory.getConnection();
        Map<String, Double> leaveBalance = new HashMap<>();
        try {
            String sql = "SELECT leave_type, SUM(total_days) as used_days " +
                        "FROM reporting_system.leave_requests " +
                        "WHERE user_id = ? AND status = 'APPROVED' AND YEAR(start_date) = ? " +
                        "GROUP BY leave_type";
            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setString(1, userId);
            preparedStmt.setInt(2, year);
            ResultSet rs = preparedStmt.executeQuery();

            Map<String, Double> allocations = new HashMap<>();
            allocations.put("VACATION", 15.0);
            allocations.put("SICK", 10.0);
            allocations.put("PERSONAL", 5.0);

            for (String type : allocations.keySet()) {
                leaveBalance.put(type + "_allocated", allocations.get(type));
                leaveBalance.put(type + "_used", 0.0);
                leaveBalance.put(type + "_remaining", allocations.get(type));
            }

            while (rs.next()) {
                String leaveType = rs.getString("leave_type");
                double usedDays = rs.getDouble("used_days");
                double allocated = allocations.getOrDefault(leaveType, 0.0);
                leaveBalance.put(leaveType + "_used", usedDays);
                leaveBalance.put(leaveType + "_remaining", allocated - usedDays);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }
        return leaveBalance;
    }

    private static double calculateLeaveDays(Timestamp startDate, Timestamp endDate) {
        long milliseconds = endDate.getTime() - startDate.getTime();
        return Math.ceil(milliseconds / (1000.0 * 60 * 60 * 24)) + 1;
    }
}
