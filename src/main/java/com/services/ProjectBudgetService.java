package com.services;

import com.beans.Project;
import com.db.Dbfactory;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectBudgetService {

    static Logger logger = Logger.getRootLogger();

    public static boolean createProject(Project project) throws SQLException {
        logger.info("ProjectBudgetService.createProject entering...");

        Connection con = Dbfactory.getConnection();
        try {
            String sql = "INSERT INTO reporting_system.projects " +
                        "(project_name, client_name, description, hourly_rate, budget_hours, status, project_manager) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setString(1, project.getProjectName());
            preparedStmt.setString(2, project.getClientName());
            preparedStmt.setString(3, project.getDescription());
            preparedStmt.setDouble(4, project.getHourlyRate());
            preparedStmt.setDouble(5, project.getBudgetHours());
            preparedStmt.setString(6, project.getStatus());
            preparedStmt.setString(7, project.getProjectManager());
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

    public static Map<String, Object> getBudgetVsActual(int projectId) throws SQLException {
        logger.info("ProjectBudgetService.getBudgetVsActual entering...");

        Map<String, Object> budgetData = new HashMap<>();
        Connection con = Dbfactory.getConnection();

        try {
            String sql = "SELECT p.project_id, p.project_name, p.client_name, " +
                        "p.hourly_rate, p.budget_hours, p.status, " +
                        "SUM(te.hours_worked) as actual_hours, " +
                        "SUM(CASE WHEN te.is_billable = true THEN te.hours_worked ELSE 0 END) as billable_hours, " +
                        "COUNT(DISTINCT te.user_id) as team_size " +
                        "FROM reporting_system.projects p " +
                        "LEFT JOIN reporting_system.time_entries te ON p.project_id = te.project_id " +
                        "WHERE p.project_id = ? " +
                        "GROUP BY p.project_id, p.project_name, p.client_name, p.hourly_rate, p.budget_hours, p.status";

            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setInt(1, projectId);

            ResultSet rs = preparedStmt.executeQuery();
            if (rs.next()) {
                double budgetHours = rs.getDouble("budget_hours");
                double actualHours = rs.getDouble("actual_hours");
                double billableHours = rs.getDouble("billable_hours");
                double hourlyRate = rs.getDouble("hourly_rate");

                double budgetAmount = budgetHours * hourlyRate;
                double actualAmount = actualHours * hourlyRate;
                double billableAmount = billableHours * hourlyRate;
                double variance = budgetHours - actualHours;
                double varianceAmount = budgetAmount - actualAmount;
                double percentUsed = (actualHours / budgetHours) * 100;

                budgetData.put("projectId", rs.getInt("project_id"));
                budgetData.put("projectName", rs.getString("project_name"));
                budgetData.put("clientName", rs.getString("client_name"));
                budgetData.put("status", rs.getString("status"));
                budgetData.put("hourlyRate", hourlyRate);
                budgetData.put("budgetHours", budgetHours);
                budgetData.put("actualHours", actualHours);
                budgetData.put("billableHours", billableHours);
                budgetData.put("budgetAmount", budgetAmount);
                budgetData.put("actualAmount", actualAmount);
                budgetData.put("billableAmount", billableAmount);
                budgetData.put("varianceHours", variance);
                budgetData.put("varianceAmount", varianceAmount);
                budgetData.put("percentUsed", percentUsed);
                budgetData.put("teamSize", rs.getInt("team_size"));
                budgetData.put("isOverBudget", actualHours > budgetHours);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return budgetData;
    }

    public static List<Map<String, Object>> getAllProjectsBudgetStatus() throws SQLException {
        logger.info("ProjectBudgetService.getAllProjectsBudgetStatus entering...");

        List<Map<String, Object>> projectsList = new ArrayList<>();
        Connection con = Dbfactory.getConnection();

        try {
            String sql = "SELECT p.project_id, p.project_name, p.client_name, " +
                        "p.hourly_rate, p.budget_hours, p.status, " +
                        "COALESCE(SUM(te.hours_worked), 0) as actual_hours, " +
                        "COALESCE(SUM(CASE WHEN te.is_billable = true THEN te.hours_worked ELSE 0 END), 0) as billable_hours " +
                        "FROM reporting_system.projects p " +
                        "LEFT JOIN reporting_system.time_entries te ON p.project_id = te.project_id " +
                        "GROUP BY p.project_id, p.project_name, p.client_name, p.hourly_rate, p.budget_hours, p.status " +
                        "ORDER BY p.project_name";

            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                Map<String, Object> project = new HashMap<>();
                double budgetHours = rs.getDouble("budget_hours");
                double actualHours = rs.getDouble("actual_hours");
                double billableHours = rs.getDouble("billable_hours");
                double hourlyRate = rs.getDouble("hourly_rate");

                double percentUsed = budgetHours > 0 ? (actualHours / budgetHours) * 100 : 0;
                double remainingHours = budgetHours - actualHours;

                project.put("projectId", rs.getInt("project_id"));
                project.put("projectName", rs.getString("project_name"));
                project.put("clientName", rs.getString("client_name"));
                project.put("status", rs.getString("status"));
                project.put("budgetHours", budgetHours);
                project.put("actualHours", actualHours);
                project.put("billableHours", billableHours);
                project.put("remainingHours", remainingHours);
                project.put("percentUsed", percentUsed);
                project.put("budgetAmount", budgetHours * hourlyRate);
                project.put("actualAmount", actualHours * hourlyRate);
                project.put("isOverBudget", actualHours > budgetHours);
                projectsList.add(project);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return projectsList;
    }

    public static Map<String, Object> getProjectMilestones(int projectId, Date startDate, Date endDate) throws SQLException {
        logger.info("ProjectBudgetService.getProjectMilestones entering...");

        Map<String, Object> milestoneData = new HashMap<>();
        Connection con = Dbfactory.getConnection();

        try {
            String sql = "SELECT DATE(start_time) as work_date, " +
                        "SUM(hours_worked) as daily_hours, " +
                        "SUM(SUM(hours_worked)) OVER (ORDER BY DATE(start_time)) as cumulative_hours " +
                        "FROM reporting_system.time_entries " +
                        "WHERE project_id = ? AND start_time >= ? AND start_time <= ? " +
                        "GROUP BY DATE(start_time) " +
                        "ORDER BY work_date";

            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setInt(1, projectId);
            preparedStmt.setDate(2, startDate);
            preparedStmt.setDate(3, endDate);

            ResultSet rs = preparedStmt.executeQuery();
            List<Map<String, Object>> dailyProgress = new ArrayList<>();

            while (rs.next()) {
                Map<String, Object> daily = new HashMap<>();
                daily.put("date", rs.getDate("work_date"));
                daily.put("dailyHours", rs.getDouble("daily_hours"));
                daily.put("cumulativeHours", rs.getDouble("cumulative_hours"));
                dailyProgress.add(daily);
            }

            milestoneData.put("projectId", projectId);
            milestoneData.put("dailyProgress", dailyProgress);
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return milestoneData;
    }

    public static List<Map<String, Object>> getProjectTeamContribution(int projectId) throws SQLException {
        logger.info("ProjectBudgetService.getProjectTeamContribution entering...");

        List<Map<String, Object>> teamContribution = new ArrayList<>();
        Connection con = Dbfactory.getConnection();

        try {
            String sql = "SELECT u.id, u.fullname, " +
                        "SUM(te.hours_worked) as total_hours, " +
                        "SUM(CASE WHEN te.is_billable = true THEN te.hours_worked ELSE 0 END) as billable_hours, " +
                        "COUNT(te.entry_id) as entry_count " +
                        "FROM reporting_system.users u " +
                        "INNER JOIN reporting_system.time_entries te ON u.id = te.user_id " +
                        "WHERE te.project_id = ? " +
                        "GROUP BY u.id, u.fullname " +
                        "ORDER BY total_hours DESC";

            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setInt(1, projectId);

            ResultSet rs = preparedStmt.executeQuery();
            double totalProjectHours = 0;

            List<Map<String, Object>> tempList = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> member = new HashMap<>();
                double hours = rs.getDouble("total_hours");
                totalProjectHours += hours;

                member.put("userId", rs.getString("id"));
                member.put("userName", rs.getString("fullname"));
                member.put("totalHours", hours);
                member.put("billableHours", rs.getDouble("billable_hours"));
                member.put("entryCount", rs.getInt("entry_count"));
                tempList.add(member);
            }

            for (Map<String, Object> member : tempList) {
                double hours = (double) member.get("totalHours");
                member.put("contributionPercentage", (hours / totalProjectHours) * 100);
                teamContribution.add(member);
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            con.close();
        }

        return teamContribution;
    }

    public static boolean updateProjectBudget(int projectId, double newBudgetHours) throws SQLException {
        logger.info("ProjectBudgetService.updateProjectBudget entering...");

        Connection con = Dbfactory.getConnection();
        try {
            String sql = "UPDATE reporting_system.projects SET budget_hours = ? WHERE project_id = ?";
            PreparedStatement preparedStmt = con.prepareStatement(sql);
            preparedStmt.setDouble(1, newBudgetHours);
            preparedStmt.setInt(2, projectId);
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
