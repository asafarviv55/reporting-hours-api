package com.services;

import com.db.Dbfactory;
import org.apache.log4j.Logger;

import java.sql.*;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PayrollExportService {

    static Logger logger = Logger.getRootLogger();

    public static String exportToCSV(Date startDate, Date endDate) throws SQLException {
        logger.info("PayrollExportService.exportToCSV entering...");

        StringBuilder csv = new StringBuilder();
        Connection con = Dbfactory.getConnection();

        try {
            csv.append("Employee ID,Employee Name,Regular Hours,Overtime Hours,Total Hours,")
               .append("Billable Hours,Non-Billable Hours,Gross Pay,Period Start,Period End\n");

            String sql = "SELECT u.id, u.fullname, " +
                        "SUM(te.hours_worked) as total_hours, " +
                        "SUM(CASE WHEN te.is_billable = true THEN te.hours_worked ELSE 0 END) as billable_hours, " +
                        "SUM(CASE WHEN te.is_billable = false THEN te.hours_worked ELSE 0 END) as non_billable_hours " +
                        "FROM reporting_system.users u " +
                        "LEFT JOIN reporting_system.time_entries te ON u.id = te.user_id " +
                        "AND te.start_time >= ? AND te.start_time <= ? " +
                        "GROUP BY u.id, u.fullname " +
                        "ORDER BY u.fullname";

            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setDate(1, startDate);
            preparedStmt.setDate(2, endDate);

            ResultSet rs = preparedStmt.executeQuery();

            while (rs.next()) {
                String userId = rs.getString("id");
                String userName = rs.getString("fullname");
                double totalHours = rs.getDouble("total_hours");
                double billableHours = rs.getDouble("billable_hours");
                double nonBillableHours = rs.getDouble("non_billable_hours");

                long days = (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24);
                double standardHours = (days / 7.0) * 40.0;
                double regularHours = Math.min(totalHours, standardHours);
                double overtimeHours = Math.max(0, totalHours - standardHours);

                double hourlyRate = 25.0;
                double grossPay = (regularHours * hourlyRate) + (overtimeHours * hourlyRate * 1.5);

                csv.append(userId).append(",")
                   .append("\"").append(userName).append("\"").append(",")
                   .append(String.format("%.2f", regularHours)).append(",")
                   .append(String.format("%.2f", overtimeHours)).append(",")
                   .append(String.format("%.2f", totalHours)).append(",")
                   .append(String.format("%.2f", billableHours)).append(",")
                   .append(String.format("%.2f", nonBillableHours)).append(",")
                   .append(String.format("%.2f", grossPay)).append(",")
                   .append(startDate).append(",")
                   .append(endDate).append("\n");
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return csv.toString();
    }

    public static List<Map<String, Object>> getPayrollSummary(Date startDate, Date endDate) throws SQLException {
        logger.info("PayrollExportService.getPayrollSummary entering...");

        List<Map<String, Object>> payrollList = new ArrayList<>();
        Connection con = Dbfactory.getConnection();

        try {
            String sql = "SELECT u.id, u.fullname, u.email, " +
                        "SUM(te.hours_worked) as total_hours, " +
                        "SUM(CASE WHEN te.is_billable = true THEN te.hours_worked ELSE 0 END) as billable_hours, " +
                        "SUM(CASE WHEN te.is_billable = false THEN te.hours_worked ELSE 0 END) as non_billable_hours, " +
                        "COUNT(DISTINCT DATE(te.start_time)) as days_worked " +
                        "FROM reporting_system.users u " +
                        "LEFT JOIN reporting_system.time_entries te ON u.id = te.user_id " +
                        "AND te.start_time >= ? AND te.start_time <= ? " +
                        "GROUP BY u.id, u.fullname, u.email " +
                        "ORDER BY u.fullname";

            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setDate(1, startDate);
            preparedStmt.setDate(2, endDate);

            ResultSet rs = preparedStmt.executeQuery();

            long days = (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24);
            double standardHours = (days / 7.0) * 40.0;

            while (rs.next()) {
                Map<String, Object> payroll = new HashMap<>();
                String userId = rs.getString("id");
                double totalHours = rs.getDouble("total_hours");
                double billableHours = rs.getDouble("billable_hours");
                double nonBillableHours = rs.getDouble("non_billable_hours");

                double regularHours = Math.min(totalHours, standardHours);
                double overtimeHours = Math.max(0, totalHours - standardHours);

                double hourlyRate = 25.0;
                double regularPay = regularHours * hourlyRate;
                double overtimePay = overtimeHours * hourlyRate * 1.5;
                double grossPay = regularPay + overtimePay;

                payroll.put("employeeId", userId);
                payroll.put("employeeName", rs.getString("fullname"));
                payroll.put("email", rs.getString("email"));
                payroll.put("totalHours", totalHours);
                payroll.put("regularHours", regularHours);
                payroll.put("overtimeHours", overtimeHours);
                payroll.put("billableHours", billableHours);
                payroll.put("nonBillableHours", nonBillableHours);
                payroll.put("daysWorked", rs.getInt("days_worked"));
                payroll.put("hourlyRate", hourlyRate);
                payroll.put("regularPay", regularPay);
                payroll.put("overtimePay", overtimePay);
                payroll.put("grossPay", grossPay);
                payroll.put("periodStart", startDate);
                payroll.put("periodEnd", endDate);

                payrollList.add(payroll);
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return payrollList;
    }

    public static String exportProjectBillingToCSV(int projectId, Date startDate, Date endDate) throws SQLException {
        logger.info("PayrollExportService.exportProjectBillingToCSV entering...");

        StringBuilder csv = new StringBuilder();
        Connection con = Dbfactory.getConnection();

        try {
            csv.append("Date,Employee Name,Hours Worked,Billable,Description,Amount\n");

            String sql = "SELECT te.entry_id, DATE(te.start_time) as work_date, " +
                        "u.fullname, te.hours_worked, te.is_billable, te.description, p.hourly_rate " +
                        "FROM reporting_system.time_entries te " +
                        "INNER JOIN reporting_system.users u ON te.user_id = u.id " +
                        "INNER JOIN reporting_system.projects p ON te.project_id = p.project_id " +
                        "WHERE te.project_id = ? AND te.start_time >= ? AND te.start_time <= ? " +
                        "ORDER BY te.start_time";

            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setInt(1, projectId);
            preparedStmt.setDate(2, startDate);
            preparedStmt.setDate(3, endDate);

            ResultSet rs = preparedStmt.executeQuery();

            while (rs.next()) {
                Date workDate = rs.getDate("work_date");
                String employeeName = rs.getString("fullname");
                double hoursWorked = rs.getDouble("hours_worked");
                boolean isBillable = rs.getBoolean("is_billable");
                String description = rs.getString("description");
                double hourlyRate = rs.getDouble("hourly_rate");
                double amount = isBillable ? hoursWorked * hourlyRate : 0;

                csv.append(workDate).append(",")
                   .append("\"").append(employeeName).append("\"").append(",")
                   .append(String.format("%.2f", hoursWorked)).append(",")
                   .append(isBillable ? "Yes" : "No").append(",")
                   .append("\"").append(description != null ? description : "").append("\"").append(",")
                   .append(String.format("%.2f", amount)).append("\n");
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return csv.toString();
    }

    public static Map<String, Object> getPayrollTotals(Date startDate, Date endDate) throws SQLException {
        logger.info("PayrollExportService.getPayrollTotals entering...");

        Map<String, Object> totals = new HashMap<>();
        Connection con = Dbfactory.getConnection();

        try {
            String sql = "SELECT " +
                        "COUNT(DISTINCT u.id) as employee_count, " +
                        "SUM(te.hours_worked) as total_hours, " +
                        "SUM(CASE WHEN te.is_billable = true THEN te.hours_worked ELSE 0 END) as billable_hours " +
                        "FROM reporting_system.users u " +
                        "LEFT JOIN reporting_system.time_entries te ON u.id = te.user_id " +
                        "AND te.start_time >= ? AND te.start_time <= ?";

            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setDate(1, startDate);
            preparedStmt.setDate(2, endDate);

            ResultSet rs = preparedStmt.executeQuery();
            if (rs.next()) {
                int employeeCount = rs.getInt("employee_count");
                double totalHours = rs.getDouble("total_hours");
                double billableHours = rs.getDouble("billable_hours");

                long days = (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24);
                double standardHours = (days / 7.0) * 40.0 * employeeCount;
                double overtimeHours = Math.max(0, totalHours - standardHours);
                double regularHours = totalHours - overtimeHours;

                double hourlyRate = 25.0;
                double totalRegularPay = regularHours * hourlyRate;
                double totalOvertimePay = overtimeHours * hourlyRate * 1.5;
                double totalGrossPay = totalRegularPay + totalOvertimePay;

                totals.put("employeeCount", employeeCount);
                totals.put("totalHours", totalHours);
                totals.put("regularHours", regularHours);
                totals.put("overtimeHours", overtimeHours);
                totals.put("billableHours", billableHours);
                totals.put("totalRegularPay", totalRegularPay);
                totals.put("totalOvertimePay", totalOvertimePay);
                totals.put("totalGrossPay", totalGrossPay);
                totals.put("periodStart", startDate);
                totals.put("periodEnd", endDate);
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return totals;
    }
}
