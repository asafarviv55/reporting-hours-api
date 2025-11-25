package com.services;

import com.beans.TimeEntry;
import com.beans.User;
import com.db.Dbfactory;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OvertimeCalculationService {

    static Logger logger = Logger.getRootLogger();
    private static final double STANDARD_HOURS_PER_WEEK = 40.0;
    private static final double STANDARD_HOURS_PER_DAY = 8.0;

    public static Map<String, Double> calculateWeeklyOvertime(String userId, Date weekStartDate) throws SQLException {
        logger.info("OvertimeCalculationService.calculateWeeklyOvertime entering...");

        Map<String, Double> overtimeData = new HashMap<>();
        Connection con = Dbfactory.getConnection();

        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(weekStartDate);
            cal.add(Calendar.DAY_OF_MONTH, 7);
            Date weekEndDate = new Date(cal.getTimeInMillis());

            String sql = "SELECT SUM(hours_worked) as total_hours FROM reporting_system.time_entries " +
                        "WHERE user_id = ? AND start_time >= ? AND start_time < ?";
            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setString(1, userId);
            preparedStmt.setDate(2, weekStartDate);
            preparedStmt.setDate(3, weekEndDate);

            ResultSet rs = preparedStmt.executeQuery();
            if (rs.next()) {
                double totalHours = rs.getDouble("total_hours");
                double regularHours = Math.min(totalHours, STANDARD_HOURS_PER_WEEK);
                double overtimeHours = Math.max(0, totalHours - STANDARD_HOURS_PER_WEEK);

                overtimeData.put("totalHours", totalHours);
                overtimeData.put("regularHours", regularHours);
                overtimeData.put("overtimeHours", overtimeHours);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return overtimeData;
    }

    public static List<Map<String, Object>> calculateDailyOvertime(String userId, Date startDate, Date endDate) throws SQLException {
        logger.info("OvertimeCalculationService.calculateDailyOvertime entering...");

        List<Map<String, Object>> dailyOvertimeList = new ArrayList<>();
        Connection con = Dbfactory.getConnection();

        try {
            String sql = "SELECT DATE(start_time) as work_date, SUM(hours_worked) as daily_hours " +
                        "FROM reporting_system.time_entries " +
                        "WHERE user_id = ? AND start_time >= ? AND start_time <= ? " +
                        "GROUP BY DATE(start_time)";
            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setString(1, userId);
            preparedStmt.setDate(2, startDate);
            preparedStmt.setDate(3, endDate);

            ResultSet rs = preparedStmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> dailyData = new HashMap<>();
                Date workDate = rs.getDate("work_date");
                double dailyHours = rs.getDouble("daily_hours");
                double regularHours = Math.min(dailyHours, STANDARD_HOURS_PER_DAY);
                double overtimeHours = Math.max(0, dailyHours - STANDARD_HOURS_PER_DAY);

                dailyData.put("workDate", workDate);
                dailyData.put("totalHours", dailyHours);
                dailyData.put("regularHours", regularHours);
                dailyData.put("overtimeHours", overtimeHours);
                dailyOvertimeList.add(dailyData);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return dailyOvertimeList;
    }

    public static double calculateOvertimePay(String userId, Date periodStart, Date periodEnd, double hourlyRate) throws SQLException {
        logger.info("OvertimeCalculationService.calculateOvertimePay entering...");

        double overtimePay = 0.0;
        Connection con = Dbfactory.getConnection();

        try {
            String sql = "SELECT SUM(hours_worked) as total_hours FROM reporting_system.time_entries " +
                        "WHERE user_id = ? AND start_time >= ? AND start_time <= ?";
            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setString(1, userId);
            preparedStmt.setDate(2, periodStart);
            preparedStmt.setDate(3, periodEnd);

            ResultSet rs = preparedStmt.executeQuery();
            if (rs.next()) {
                double totalHours = rs.getDouble("total_hours");
                long days = (periodEnd.getTime() - periodStart.getTime()) / (1000 * 60 * 60 * 24);
                double standardHours = (days / 7.0) * STANDARD_HOURS_PER_WEEK;
                double overtimeHours = Math.max(0, totalHours - standardHours);
                overtimePay = overtimeHours * hourlyRate * 1.5;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return overtimePay;
    }

    public static boolean recordOvertimeEntry(String userId, int projectId, Timestamp startTime,
                                             Timestamp endTime, double hours) throws SQLException {
        logger.info("OvertimeCalculationService.recordOvertimeEntry entering...");

        Connection con = Dbfactory.getConnection();
        try {
            String sql = "INSERT INTO reporting_system.time_entries " +
                        "(user_id, project_id, start_time, end_time, hours_worked, is_billable, entry_type, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'OVERTIME', ?)";
            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setString(1, userId);
            preparedStmt.setInt(2, projectId);
            preparedStmt.setTimestamp(3, startTime);
            preparedStmt.setTimestamp(4, endTime);
            preparedStmt.setDouble(5, hours);
            preparedStmt.setBoolean(6, true);
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
}
