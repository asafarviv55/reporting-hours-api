package com.services;

import com.db.Dbfactory;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Calendar;

public class UtilizationRateService {

    static Logger logger = Logger.getRootLogger();
    private static final double STANDARD_HOURS_PER_WEEK = 40.0;
    private static final double STANDARD_HOURS_PER_DAY = 8.0;

    public static Map<String, Object> calculateUtilizationRate(String userId, Date startDate, Date endDate) throws SQLException {
        logger.info("UtilizationRateService.calculateUtilizationRate entering...");

        Map<String, Object> utilizationData = new HashMap<>();
        Connection con = Dbfactory.getConnection();

        try {
            long days = (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24);
            double availableHours = (days / 7.0) * STANDARD_HOURS_PER_WEEK;

            double leaveDays = getLeaveDays(userId, startDate, endDate, con);
            availableHours -= (leaveDays * STANDARD_HOURS_PER_DAY);

            String sql = "SELECT " +
                        "SUM(hours_worked) as total_hours, " +
                        "SUM(CASE WHEN is_billable = true THEN hours_worked ELSE 0 END) as billable_hours, " +
                        "SUM(CASE WHEN is_billable = false THEN hours_worked ELSE 0 END) as non_billable_hours " +
                        "FROM reporting_system.time_entries " +
                        "WHERE user_id = ? AND start_time >= ? AND start_time <= ?";

            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setString(1, userId);
            preparedStmt.setDate(2, startDate);
            preparedStmt.setDate(3, endDate);

            ResultSet rs = preparedStmt.executeQuery();
            if (rs.next()) {
                double totalHours = rs.getDouble("total_hours");
                double billableHours = rs.getDouble("billable_hours");
                double nonBillableHours = rs.getDouble("non_billable_hours");

                double utilizationRate = (totalHours / availableHours) * 100;
                double billableRate = (billableHours / availableHours) * 100;
                double nonBillableRate = (nonBillableHours / availableHours) * 100;

                utilizationData.put("userId", userId);
                utilizationData.put("startDate", startDate);
                utilizationData.put("endDate", endDate);
                utilizationData.put("availableHours", availableHours);
                utilizationData.put("totalHours", totalHours);
                utilizationData.put("billableHours", billableHours);
                utilizationData.put("nonBillableHours", nonBillableHours);
                utilizationData.put("leaveDays", leaveDays);
                utilizationData.put("utilizationRate", utilizationRate);
                utilizationData.put("billableUtilizationRate", billableRate);
                utilizationData.put("nonBillableUtilizationRate", nonBillableRate);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return utilizationData;
    }

    public static List<Map<String, Object>> calculateTeamUtilization(Date startDate, Date endDate) throws SQLException {
        logger.info("UtilizationRateService.calculateTeamUtilization entering...");

        List<Map<String, Object>> teamUtilization = new ArrayList<>();
        Connection con = Dbfactory.getConnection();

        try {
            String sql = "SELECT u.id, u.fullname, " +
                        "SUM(te.hours_worked) as total_hours, " +
                        "SUM(CASE WHEN te.is_billable = true THEN te.hours_worked ELSE 0 END) as billable_hours " +
                        "FROM reporting_system.users u " +
                        "LEFT JOIN reporting_system.time_entries te ON u.id = te.user_id " +
                        "AND te.start_time >= ? AND te.start_time <= ? " +
                        "GROUP BY u.id, u.fullname";

            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setDate(1, startDate);
            preparedStmt.setDate(2, endDate);

            ResultSet rs = preparedStmt.executeQuery();

            long days = (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24);
            double standardHours = (days / 7.0) * STANDARD_HOURS_PER_WEEK;

            while (rs.next()) {
                Map<String, Object> userUtil = new HashMap<>();
                String userId = rs.getString("id");
                double totalHours = rs.getDouble("total_hours");
                double billableHours = rs.getDouble("billable_hours");

                double leaveDays = getLeaveDays(userId, startDate, endDate, con);
                double availableHours = standardHours - (leaveDays * STANDARD_HOURS_PER_DAY);

                userUtil.put("userId", userId);
                userUtil.put("userName", rs.getString("fullname"));
                userUtil.put("totalHours", totalHours);
                userUtil.put("billableHours", billableHours);
                userUtil.put("availableHours", availableHours);
                userUtil.put("utilizationRate", (totalHours / availableHours) * 100);
                userUtil.put("billableRate", (billableHours / availableHours) * 100);
                teamUtilization.add(userUtil);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return teamUtilization;
    }

    public static Map<String, Object> calculateProjectUtilization(int projectId, Date startDate, Date endDate) throws SQLException {
        logger.info("UtilizationRateService.calculateProjectUtilization entering...");

        Map<String, Object> projectUtil = new HashMap<>();
        Connection con = Dbfactory.getConnection();

        try {
            String sql = "SELECT p.project_id, p.project_name, p.budget_hours, " +
                        "SUM(te.hours_worked) as actual_hours, " +
                        "COUNT(DISTINCT te.user_id) as team_size " +
                        "FROM reporting_system.projects p " +
                        "LEFT JOIN reporting_system.time_entries te ON p.project_id = te.project_id " +
                        "AND te.start_time >= ? AND te.start_time <= ? " +
                        "WHERE p.project_id = ? " +
                        "GROUP BY p.project_id, p.project_name, p.budget_hours";

            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setDate(1, startDate);
            preparedStmt.setDate(2, endDate);
            preparedStmt.setInt(3, projectId);

            ResultSet rs = preparedStmt.executeQuery();
            if (rs.next()) {
                double budgetHours = rs.getDouble("budget_hours");
                double actualHours = rs.getDouble("actual_hours");
                double utilizationRate = (actualHours / budgetHours) * 100;

                projectUtil.put("projectId", rs.getInt("project_id"));
                projectUtil.put("projectName", rs.getString("project_name"));
                projectUtil.put("budgetHours", budgetHours);
                projectUtil.put("actualHours", actualHours);
                projectUtil.put("remainingHours", budgetHours - actualHours);
                projectUtil.put("utilizationRate", utilizationRate);
                projectUtil.put("teamSize", rs.getInt("team_size"));
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return projectUtil;
    }

    public static List<Map<String, Object>> getUtilizationTrend(String userId, int year) throws SQLException {
        logger.info("UtilizationRateService.getUtilizationTrend entering...");

        List<Map<String, Object>> trendList = new ArrayList<>();
        Connection con = Dbfactory.getConnection();

        try {
            String sql = "SELECT MONTH(start_time) as month, " +
                        "SUM(hours_worked) as total_hours, " +
                        "SUM(CASE WHEN is_billable = true THEN hours_worked ELSE 0 END) as billable_hours " +
                        "FROM reporting_system.time_entries " +
                        "WHERE user_id = ? AND YEAR(start_time) = ? " +
                        "GROUP BY MONTH(start_time) " +
                        "ORDER BY month";

            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setString(1, userId);
            preparedStmt.setInt(2, year);

            ResultSet rs = preparedStmt.executeQuery();
            while (rs.next()) {
                int month = rs.getInt("month");
                double totalHours = rs.getDouble("total_hours");
                double billableHours = rs.getDouble("billable_hours");

                Calendar cal = Calendar.getInstance();
                cal.set(year, month - 1, 1);
                int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                double availableHours = (daysInMonth / 7.0) * STANDARD_HOURS_PER_WEEK;

                Map<String, Object> monthData = new HashMap<>();
                monthData.put("month", month);
                monthData.put("totalHours", totalHours);
                monthData.put("billableHours", billableHours);
                monthData.put("availableHours", availableHours);
                monthData.put("utilizationRate", (totalHours / availableHours) * 100);
                monthData.put("billableRate", (billableHours / availableHours) * 100);
                trendList.add(monthData);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return trendList;
    }

    private static double getLeaveDays(String userId, Date startDate, Date endDate, Connection con) {
        double leaveDays = 0;
        try {
            String sql = "SELECT SUM(total_days) as leave_days " +
                        "FROM reporting_system.leave_requests " +
                        "WHERE user_id = ? AND status = 'APPROVED' " +
                        "AND start_date >= ? AND end_date <= ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, userId);
            ps.setDate(2, startDate);
            ps.setDate(3, endDate);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                leaveDays = rs.getDouble("leave_days");
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        return leaveDays;
    }
}
