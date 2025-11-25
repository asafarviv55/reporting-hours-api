package com.services;

import com.db.Dbfactory;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamSummaryService {

    static Logger logger = Logger.getRootLogger();

    public static Map<String, Object> getTeamSummary(Date startDate, Date endDate) throws SQLException {
        logger.info("TeamSummaryService.getTeamSummary entering...");

        Map<String, Object> summary = new HashMap<>();
        Connection con = Dbfactory.getConnection();

        try {
            String sql = "SELECT " +
                        "COUNT(DISTINCT u.id) as total_employees, " +
                        "COUNT(DISTINCT te.user_id) as active_employees, " +
                        "SUM(te.hours_worked) as total_hours, " +
                        "AVG(te.hours_worked) as avg_hours_per_entry, " +
                        "SUM(CASE WHEN te.is_billable = true THEN te.hours_worked ELSE 0 END) as billable_hours, " +
                        "SUM(CASE WHEN te.is_billable = false THEN te.hours_worked ELSE 0 END) as non_billable_hours, " +
                        "COUNT(DISTINCT te.project_id) as active_projects " +
                        "FROM reporting_system.users u " +
                        "LEFT JOIN reporting_system.time_entries te ON u.id = te.user_id " +
                        "AND te.start_time >= ? AND te.start_time <= ?";

            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setDate(1, startDate);
            preparedStmt.setDate(2, endDate);

            ResultSet rs = preparedStmt.executeQuery();
            if (rs.next()) {
                int totalEmployees = rs.getInt("total_employees");
                int activeEmployees = rs.getInt("active_employees");
                double totalHours = rs.getDouble("total_hours");
                double billableHours = rs.getDouble("billable_hours");
                double nonBillableHours = rs.getDouble("non_billable_hours");

                summary.put("totalEmployees", totalEmployees);
                summary.put("activeEmployees", activeEmployees);
                summary.put("inactiveEmployees", totalEmployees - activeEmployees);
                summary.put("totalHours", totalHours);
                summary.put("billableHours", billableHours);
                summary.put("nonBillableHours", nonBillableHours);
                summary.put("avgHoursPerEntry", rs.getDouble("avg_hours_per_entry"));
                summary.put("activeProjects", rs.getInt("active_projects"));
                summary.put("avgHoursPerEmployee", activeEmployees > 0 ? totalHours / activeEmployees : 0);
                summary.put("billablePercentage", totalHours > 0 ? (billableHours / totalHours) * 100 : 0);
                summary.put("periodStart", startDate);
                summary.put("periodEnd", endDate);
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return summary;
    }

    public static List<Map<String, Object>> getTeamMembersSummary(Date startDate, Date endDate) throws SQLException {
        logger.info("TeamSummaryService.getTeamMembersSummary entering...");

        List<Map<String, Object>> teamMembers = new ArrayList<>();
        Connection con = Dbfactory.getConnection();

        try {
            String sql = "SELECT u.id, u.fullname, u.email, " +
                        "COALESCE(SUM(te.hours_worked), 0) as total_hours, " +
                        "COALESCE(SUM(CASE WHEN te.is_billable = true THEN te.hours_worked ELSE 0 END), 0) as billable_hours, " +
                        "COALESCE(SUM(CASE WHEN te.is_billable = false THEN te.hours_worked ELSE 0 END), 0) as non_billable_hours, " +
                        "COUNT(DISTINCT te.project_id) as project_count, " +
                        "COUNT(te.entry_id) as entry_count, " +
                        "COUNT(DISTINCT DATE(te.start_time)) as days_worked " +
                        "FROM reporting_system.users u " +
                        "LEFT JOIN reporting_system.time_entries te ON u.id = te.user_id " +
                        "AND te.start_time >= ? AND te.start_time <= ? " +
                        "GROUP BY u.id, u.fullname, u.email " +
                        "ORDER BY total_hours DESC";

            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setDate(1, startDate);
            preparedStmt.setDate(2, endDate);

            ResultSet rs = preparedStmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> member = new HashMap<>();
                double totalHours = rs.getDouble("total_hours");
                double billableHours = rs.getDouble("billable_hours");
                int daysWorked = rs.getInt("days_worked");

                member.put("userId", rs.getString("id"));
                member.put("userName", rs.getString("fullname"));
                member.put("email", rs.getString("email"));
                member.put("totalHours", totalHours);
                member.put("billableHours", billableHours);
                member.put("nonBillableHours", rs.getDouble("non_billable_hours"));
                member.put("projectCount", rs.getInt("project_count"));
                member.put("entryCount", rs.getInt("entry_count"));
                member.put("daysWorked", daysWorked);
                member.put("avgHoursPerDay", daysWorked > 0 ? totalHours / daysWorked : 0);
                member.put("billablePercentage", totalHours > 0 ? (billableHours / totalHours) * 100 : 0);
                teamMembers.add(member);
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return teamMembers;
    }

    public static List<Map<String, Object>> getTeamProjectsSummary(Date startDate, Date endDate) throws SQLException {
        logger.info("TeamSummaryService.getTeamProjectsSummary entering...");

        List<Map<String, Object>> projects = new ArrayList<>();
        Connection con = Dbfactory.getConnection();

        try {
            String sql = "SELECT p.project_id, p.project_name, p.client_name, p.status, " +
                        "COUNT(DISTINCT te.user_id) as team_size, " +
                        "COALESCE(SUM(te.hours_worked), 0) as total_hours, " +
                        "COALESCE(SUM(CASE WHEN te.is_billable = true THEN te.hours_worked ELSE 0 END), 0) as billable_hours, " +
                        "p.budget_hours, " +
                        "p.hourly_rate " +
                        "FROM reporting_system.projects p " +
                        "LEFT JOIN reporting_system.time_entries te ON p.project_id = te.project_id " +
                        "AND te.start_time >= ? AND te.start_time <= ? " +
                        "GROUP BY p.project_id, p.project_name, p.client_name, p.status, p.budget_hours, p.hourly_rate " +
                        "ORDER BY total_hours DESC";

            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setDate(1, startDate);
            preparedStmt.setDate(2, endDate);

            ResultSet rs = preparedStmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> project = new HashMap<>();
                double totalHours = rs.getDouble("total_hours");
                double billableHours = rs.getDouble("billable_hours");
                double budgetHours = rs.getDouble("budget_hours");
                double hourlyRate = rs.getDouble("hourly_rate");

                project.put("projectId", rs.getInt("project_id"));
                project.put("projectName", rs.getString("project_name"));
                project.put("clientName", rs.getString("client_name"));
                project.put("status", rs.getString("status"));
                project.put("teamSize", rs.getInt("team_size"));
                project.put("totalHours", totalHours);
                project.put("billableHours", billableHours);
                project.put("budgetHours", budgetHours);
                project.put("remainingHours", budgetHours - totalHours);
                project.put("budgetUtilization", budgetHours > 0 ? (totalHours / budgetHours) * 100 : 0);
                project.put("revenue", billableHours * hourlyRate);
                projects.add(project);
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return projects;
    }

    public static Map<String, Object> getDepartmentSummary(String department, Date startDate, Date endDate) throws SQLException {
        logger.info("TeamSummaryService.getDepartmentSummary entering...");

        Map<String, Object> deptSummary = new HashMap<>();
        Connection con = Dbfactory.getConnection();

        try {
            String sql = "SELECT " +
                        "COUNT(DISTINCT u.id) as employee_count, " +
                        "SUM(te.hours_worked) as total_hours, " +
                        "SUM(CASE WHEN te.is_billable = true THEN te.hours_worked ELSE 0 END) as billable_hours, " +
                        "AVG(te.hours_worked) as avg_hours " +
                        "FROM reporting_system.users u " +
                        "LEFT JOIN reporting_system.time_entries te ON u.id = te.user_id " +
                        "AND te.start_time >= ? AND te.start_time <= ? " +
                        "WHERE u.department = ?";

            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setDate(1, startDate);
            preparedStmt.setDate(2, endDate);
            preparedStmt.setString(3, department);

            ResultSet rs = preparedStmt.executeQuery();
            if (rs.next()) {
                deptSummary.put("department", department);
                deptSummary.put("employeeCount", rs.getInt("employee_count"));
                deptSummary.put("totalHours", rs.getDouble("total_hours"));
                deptSummary.put("billableHours", rs.getDouble("billable_hours"));
                deptSummary.put("avgHours", rs.getDouble("avg_hours"));
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return deptSummary;
    }

    public static List<Map<String, Object>> getTopPerformers(Date startDate, Date endDate, int limit) throws SQLException {
        logger.info("TeamSummaryService.getTopPerformers entering...");

        List<Map<String, Object>> topPerformers = new ArrayList<>();
        Connection con = Dbfactory.getConnection();

        try {
            String sql = "SELECT u.id, u.fullname, " +
                        "SUM(te.hours_worked) as total_hours, " +
                        "SUM(CASE WHEN te.is_billable = true THEN te.hours_worked ELSE 0 END) as billable_hours, " +
                        "COUNT(DISTINCT te.project_id) as project_count " +
                        "FROM reporting_system.users u " +
                        "INNER JOIN reporting_system.time_entries te ON u.id = te.user_id " +
                        "WHERE te.start_time >= ? AND te.start_time <= ? " +
                        "GROUP BY u.id, u.fullname " +
                        "ORDER BY billable_hours DESC " +
                        "LIMIT ?";

            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setDate(1, startDate);
            preparedStmt.setDate(2, endDate);
            preparedStmt.setInt(3, limit);

            ResultSet rs = preparedStmt.executeQuery();
            int rank = 1;

            while (rs.next()) {
                Map<String, Object> performer = new HashMap<>();
                double totalHours = rs.getDouble("total_hours");
                double billableHours = rs.getDouble("billable_hours");

                performer.put("rank", rank++);
                performer.put("userId", rs.getString("id"));
                performer.put("userName", rs.getString("fullname"));
                performer.put("totalHours", totalHours);
                performer.put("billableHours", billableHours);
                performer.put("projectCount", rs.getInt("project_count"));
                performer.put("billablePercentage", totalHours > 0 ? (billableHours / totalHours) * 100 : 0);
                topPerformers.add(performer);
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return topPerformers;
    }

    public static Map<String, Object> getTeamProductivity(Date startDate, Date endDate) throws SQLException {
        logger.info("TeamSummaryService.getTeamProductivity entering...");

        Map<String, Object> productivity = new HashMap<>();
        Connection con = Dbfactory.getConnection();

        try {
            String sql = "SELECT " +
                        "SUM(hours_worked) as total_hours, " +
                        "SUM(CASE WHEN is_billable = true THEN hours_worked ELSE 0 END) as billable_hours, " +
                        "COUNT(entry_id) as total_entries, " +
                        "COUNT(DISTINCT user_id) as active_users, " +
                        "COUNT(DISTINCT DATE(start_time)) as working_days " +
                        "FROM reporting_system.time_entries " +
                        "WHERE start_time >= ? AND start_time <= ?";

            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setDate(1, startDate);
            preparedStmt.setDate(2, endDate);

            ResultSet rs = preparedStmt.executeQuery();
            if (rs.next()) {
                double totalHours = rs.getDouble("total_hours");
                double billableHours = rs.getDouble("billable_hours");
                int activeUsers = rs.getInt("active_users");
                int totalEntries = rs.getInt("total_entries");

                productivity.put("totalHours", totalHours);
                productivity.put("billableHours", billableHours);
                productivity.put("nonBillableHours", totalHours - billableHours);
                productivity.put("totalEntries", totalEntries);
                productivity.put("activeUsers", activeUsers);
                productivity.put("avgHoursPerUser", activeUsers > 0 ? totalHours / activeUsers : 0);
                productivity.put("avgHoursPerEntry", totalEntries > 0 ? totalHours / totalEntries : 0);
                productivity.put("productivityScore", totalHours > 0 ? (billableHours / totalHours) * 100 : 0);
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return productivity;
    }
}
