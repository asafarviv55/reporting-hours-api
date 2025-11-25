package com.services;

import com.db.Dbfactory;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Calendar;

public class WeeklyReportService {

    static Logger logger = Logger.getRootLogger();

    public static Map<String, Object> generateWeeklyReport(String userId, Date weekStartDate) throws SQLException {
        logger.info("WeeklyReportService.generateWeeklyReport entering...");

        Map<String, Object> report = new HashMap<>();
        Connection con = Dbfactory.getConnection();

        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(weekStartDate);
            cal.add(Calendar.DAY_OF_MONTH, 7);
            Date weekEndDate = new Date(cal.getTimeInMillis());

            String sql = "SELECT " +
                        "SUM(hours_worked) as total_hours, " +
                        "SUM(CASE WHEN is_billable = true THEN hours_worked ELSE 0 END) as billable_hours, " +
                        "SUM(CASE WHEN is_billable = false THEN hours_worked ELSE 0 END) as non_billable_hours, " +
                        "COUNT(DISTINCT DATE(start_time)) as days_worked, " +
                        "COUNT(entry_id) as total_entries " +
                        "FROM reporting_system.time_entries " +
                        "WHERE user_id = ? AND start_time >= ? AND start_time < ?";

            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setString(1, userId);
            preparedStmt.setDate(2, weekStartDate);
            preparedStmt.setDate(3, weekEndDate);

            ResultSet rs = preparedStmt.executeQuery();
            if (rs.next()) {
                report.put("userId", userId);
                report.put("weekStart", weekStartDate);
                report.put("weekEnd", weekEndDate);
                report.put("totalHours", rs.getDouble("total_hours"));
                report.put("billableHours", rs.getDouble("billable_hours"));
                report.put("nonBillableHours", rs.getDouble("non_billable_hours"));
                report.put("daysWorked", rs.getInt("days_worked"));
                report.put("totalEntries", rs.getInt("total_entries"));

                double totalHours = rs.getDouble("total_hours");
                double avgHoursPerDay = totalHours / Math.max(1, rs.getInt("days_worked"));
                report.put("averageHoursPerDay", avgHoursPerDay);
            }

            report.put("projectBreakdown", getProjectBreakdown(userId, weekStartDate, weekEndDate, con));

        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return report;
    }

    public static Map<String, Object> generateMonthlyReport(String userId, int year, int month) throws SQLException {
        logger.info("WeeklyReportService.generateMonthlyReport entering...");

        Map<String, Object> report = new HashMap<>();
        Connection con = Dbfactory.getConnection();

        try {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month - 1, 1, 0, 0, 0);
            Date monthStart = new Date(cal.getTimeInMillis());

            cal.add(Calendar.MONTH, 1);
            Date monthEnd = new Date(cal.getTimeInMillis());

            String sql = "SELECT " +
                        "SUM(hours_worked) as total_hours, " +
                        "SUM(CASE WHEN is_billable = true THEN hours_worked ELSE 0 END) as billable_hours, " +
                        "SUM(CASE WHEN is_billable = false THEN hours_worked ELSE 0 END) as non_billable_hours, " +
                        "COUNT(DISTINCT DATE(start_time)) as days_worked, " +
                        "COUNT(entry_id) as total_entries " +
                        "FROM reporting_system.time_entries " +
                        "WHERE user_id = ? AND start_time >= ? AND start_time < ?";

            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setString(1, userId);
            preparedStmt.setDate(2, monthStart);
            preparedStmt.setDate(3, monthEnd);

            ResultSet rs = preparedStmt.executeQuery();
            if (rs.next()) {
                report.put("userId", userId);
                report.put("year", year);
                report.put("month", month);
                report.put("monthStart", monthStart);
                report.put("monthEnd", monthEnd);
                report.put("totalHours", rs.getDouble("total_hours"));
                report.put("billableHours", rs.getDouble("billable_hours"));
                report.put("nonBillableHours", rs.getDouble("non_billable_hours"));
                report.put("daysWorked", rs.getInt("days_worked"));
                report.put("totalEntries", rs.getInt("total_entries"));

                double totalHours = rs.getDouble("total_hours");
                double avgHoursPerDay = totalHours / Math.max(1, rs.getInt("days_worked"));
                report.put("averageHoursPerDay", avgHoursPerDay);
            }

            report.put("projectBreakdown", getProjectBreakdown(userId, monthStart, monthEnd, con));
            report.put("weeklyBreakdown", getWeeklyBreakdown(userId, monthStart, monthEnd, con));

        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return report;
    }

    public static List<Map<String, Object>> getDailyBreakdown(String userId, Date startDate, Date endDate) throws SQLException {
        logger.info("WeeklyReportService.getDailyBreakdown entering...");

        List<Map<String, Object>> dailyList = new ArrayList<>();
        Connection con = Dbfactory.getConnection();

        try {
            String sql = "SELECT DATE(start_time) as work_date, " +
                        "SUM(hours_worked) as total_hours, " +
                        "SUM(CASE WHEN is_billable = true THEN hours_worked ELSE 0 END) as billable_hours, " +
                        "SUM(CASE WHEN is_billable = false THEN hours_worked ELSE 0 END) as non_billable_hours " +
                        "FROM reporting_system.time_entries " +
                        "WHERE user_id = ? AND start_time >= ? AND start_time <= ? " +
                        "GROUP BY DATE(start_time) " +
                        "ORDER BY work_date";

            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setString(1, userId);
            preparedStmt.setDate(2, startDate);
            preparedStmt.setDate(3, endDate);

            ResultSet rs = preparedStmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> dailyData = new HashMap<>();
                dailyData.put("date", rs.getDate("work_date"));
                dailyData.put("totalHours", rs.getDouble("total_hours"));
                dailyData.put("billableHours", rs.getDouble("billable_hours"));
                dailyData.put("nonBillableHours", rs.getDouble("non_billable_hours"));
                dailyList.add(dailyData);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return dailyList;
    }

    private static List<Map<String, Object>> getProjectBreakdown(String userId, Date startDate, Date endDate, Connection con) {
        List<Map<String, Object>> projectList = new ArrayList<>();
        try {
            String sql = "SELECT te.project_id, p.project_name, " +
                        "SUM(te.hours_worked) as project_hours, " +
                        "SUM(CASE WHEN te.is_billable = true THEN te.hours_worked ELSE 0 END) as billable_hours " +
                        "FROM reporting_system.time_entries te " +
                        "LEFT JOIN reporting_system.projects p ON te.project_id = p.project_id " +
                        "WHERE te.user_id = ? AND te.start_time >= ? AND te.start_time < ? " +
                        "GROUP BY te.project_id, p.project_name";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, userId);
            ps.setDate(2, startDate);
            ps.setDate(3, endDate);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> project = new HashMap<>();
                project.put("projectId", rs.getInt("project_id"));
                project.put("projectName", rs.getString("project_name"));
                project.put("totalHours", rs.getDouble("project_hours"));
                project.put("billableHours", rs.getDouble("billable_hours"));
                projectList.add(project);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        return projectList;
    }

    private static List<Map<String, Object>> getWeeklyBreakdown(String userId, Date startDate, Date endDate, Connection con) {
        List<Map<String, Object>> weeklyList = new ArrayList<>();
        try {
            String sql = "SELECT WEEK(start_time) as week_num, " +
                        "MIN(DATE(start_time)) as week_start, " +
                        "MAX(DATE(start_time)) as week_end, " +
                        "SUM(hours_worked) as total_hours " +
                        "FROM reporting_system.time_entries " +
                        "WHERE user_id = ? AND start_time >= ? AND start_time < ? " +
                        "GROUP BY WEEK(start_time) " +
                        "ORDER BY week_num";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, userId);
            ps.setDate(2, startDate);
            ps.setDate(3, endDate);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> week = new HashMap<>();
                week.put("weekNumber", rs.getInt("week_num"));
                week.put("weekStart", rs.getDate("week_start"));
                week.put("weekEnd", rs.getDate("week_end"));
                week.put("totalHours", rs.getDouble("total_hours"));
                weeklyList.add(week);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        return weeklyList;
    }
}
