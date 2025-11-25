package com.services;

import com.beans.TimesheetApproval;
import com.db.Dbfactory;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TimesheetApprovalService {

    static Logger logger = Logger.getRootLogger();

    public static boolean submitTimesheetForApproval(String userId, Timestamp weekStart, Timestamp weekEnd) throws SQLException {
        logger.info("TimesheetApprovalService.submitTimesheetForApproval entering...");

        Connection con = Dbfactory.getConnection();
        try {
            double[] hours = calculateWeekHours(userId, weekStart, weekEnd, con);

            String sql = "INSERT INTO reporting_system.timesheet_approvals " +
                        "(user_id, week_start_date, week_end_date, status, submitted_at, " +
                        "total_hours, billable_hours, non_billable_hours) " +
                        "VALUES (?, ?, ?, 'PENDING', ?, ?, ?, ?)";
            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setString(1, userId);
            preparedStmt.setTimestamp(2, weekStart);
            preparedStmt.setTimestamp(3, weekEnd);
            preparedStmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            preparedStmt.setDouble(5, hours[0]);
            preparedStmt.setDouble(6, hours[1]);
            preparedStmt.setDouble(7, hours[2]);
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

    public static boolean approveTimesheet(int approvalId, String approverId, String comments) throws SQLException {
        logger.info("TimesheetApprovalService.approveTimesheet entering...");

        Connection con = Dbfactory.getConnection();
        try {
            String sql = "UPDATE reporting_system.timesheet_approvals " +
                        "SET status = 'APPROVED', approver_id = ?, approver_comments = ?, " +
                        "reviewed_at = ? WHERE approval_id = ?";
            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setString(1, approverId);
            preparedStmt.setString(2, comments);
            preparedStmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            preparedStmt.setInt(4, approvalId);
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

    public static boolean rejectTimesheet(int approvalId, String approverId, String comments) throws SQLException {
        logger.info("TimesheetApprovalService.rejectTimesheet entering...");

        Connection con = Dbfactory.getConnection();
        try {
            String sql = "UPDATE reporting_system.timesheet_approvals " +
                        "SET status = 'REJECTED', approver_id = ?, approver_comments = ?, " +
                        "reviewed_at = ? WHERE approval_id = ?";
            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setString(1, approverId);
            preparedStmt.setString(2, comments);
            preparedStmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            preparedStmt.setInt(4, approvalId);
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

    public static List<TimesheetApproval> getPendingApprovals(String approverId) throws SQLException {
        logger.info("TimesheetApprovalService.getPendingApprovals entering...");

        Connection con = Dbfactory.getConnection();
        List<TimesheetApproval> list = new ArrayList<>();
        try {
            String sql = "SELECT * FROM reporting_system.timesheet_approvals WHERE status = 'PENDING'";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                TimesheetApproval approval = new TimesheetApproval();
                approval.setApprovalId(rs.getInt("approval_id"));
                approval.setUserId(rs.getString("user_id"));
                approval.setApproverId(rs.getString("approver_id"));
                approval.setWeekStartDate(rs.getTimestamp("week_start_date"));
                approval.setWeekEndDate(rs.getTimestamp("week_end_date"));
                approval.setStatus(rs.getString("status"));
                approval.setApproverComments(rs.getString("approver_comments"));
                approval.setSubmittedAt(rs.getTimestamp("submitted_at"));
                approval.setReviewedAt(rs.getTimestamp("reviewed_at"));
                approval.setTotalHours(rs.getDouble("total_hours"));
                approval.setBillableHours(rs.getDouble("billable_hours"));
                approval.setNonBillableHours(rs.getDouble("non_billable_hours"));
                list.add(approval);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }
        return list;
    }

    public static List<TimesheetApproval> getTimesheetsByUser(String userId) throws SQLException {
        logger.info("TimesheetApprovalService.getTimesheetsByUser entering...");

        Connection con = Dbfactory.getConnection();
        List<TimesheetApproval> list = new ArrayList<>();
        try {
            String sql = "SELECT * FROM reporting_system.timesheet_approvals WHERE user_id = ? ORDER BY week_start_date DESC";
            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setString(1, userId);
            ResultSet rs = preparedStmt.executeQuery();

            while (rs.next()) {
                TimesheetApproval approval = new TimesheetApproval();
                approval.setApprovalId(rs.getInt("approval_id"));
                approval.setUserId(rs.getString("user_id"));
                approval.setApproverId(rs.getString("approver_id"));
                approval.setWeekStartDate(rs.getTimestamp("week_start_date"));
                approval.setWeekEndDate(rs.getTimestamp("week_end_date"));
                approval.setStatus(rs.getString("status"));
                approval.setApproverComments(rs.getString("approver_comments"));
                approval.setSubmittedAt(rs.getTimestamp("submitted_at"));
                approval.setReviewedAt(rs.getTimestamp("reviewed_at"));
                approval.setTotalHours(rs.getDouble("total_hours"));
                approval.setBillableHours(rs.getDouble("billable_hours"));
                approval.setNonBillableHours(rs.getDouble("non_billable_hours"));
                list.add(approval);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }
        return list;
    }

    private static double[] calculateWeekHours(String userId, Timestamp weekStart, Timestamp weekEnd, Connection con) {
        double[] hours = new double[3];
        try {
            String sql = "SELECT SUM(hours_worked) as total, " +
                        "SUM(CASE WHEN is_billable = true THEN hours_worked ELSE 0 END) as billable, " +
                        "SUM(CASE WHEN is_billable = false THEN hours_worked ELSE 0 END) as non_billable " +
                        "FROM reporting_system.time_entries " +
                        "WHERE user_id = ? AND start_time >= ? AND start_time <= ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, userId);
            ps.setTimestamp(2, weekStart);
            ps.setTimestamp(3, weekEnd);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                hours[0] = rs.getDouble("total");
                hours[1] = rs.getDouble("billable");
                hours[2] = rs.getDouble("non_billable");
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        return hours;
    }
}
