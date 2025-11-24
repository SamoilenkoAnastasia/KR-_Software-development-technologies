package ua.kpi.personal.repo;

import ua.kpi.personal.model.access.BudgetAccess;
import ua.kpi.personal.util.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BudgetAccessDao {
    
    public static final String ROLE_OWNER = "OWNER"; 
    public static final String ROLE_EDITOR = "EDITOR";
    public static final String ROLE_VIEWER = "VIEWER";


    private BudgetAccess mapResultSetToBudgetAccess(ResultSet rs) throws SQLException {
        BudgetAccess access = new BudgetAccess();
 
        access.setBudgetId(rs.getLong("budget_id"));
        access.setUserId(rs.getLong("user_id"));
        access.setAccessRole(rs.getString("role"));
        return access;
    }


    public BudgetAccess save(BudgetAccess access) {

        String sql = "INSERT INTO budget_access (budget_id, user_id, role) VALUES (?, ?, ?)" +
                     " ON DUPLICATE KEY UPDATE role = ?";
        
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setLong(1, access.getBudgetId());
            ps.setLong(2, access.getUserId());
            ps.setString(3, access.getAccessRole());
            ps.setString(4, access.getAccessRole());
            
            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                return access;
            }
        } catch (SQLException e) {

            System.err.println("Помилка UPSERT доступу до бюджету: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Помилка БД при збереженні доступу до бюджету.", e);
        }
        return null;
    }

    public BudgetAccess findAccessByBudgetAndUser(Long budgetId, Long userId) {
        if (budgetId == null || userId == null) return null;

        if (budgetId.equals(userId)) {
            BudgetAccess privateAccess = new BudgetAccess();
            privateAccess.setBudgetId(budgetId);
            privateAccess.setUserId(userId);
            privateAccess.setAccessRole(ROLE_OWNER); 
            return privateAccess;
        }
        
        String sql = "SELECT budget_id, user_id, role FROM budget_access WHERE budget_id = ? AND user_id = ?";
        
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setLong(1, budgetId);
            ps.setLong(2, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBudgetAccess(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка пошуку доступу до бюджету: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Помилка БД при пошуку доступу до бюджету.", e);
        }
        return null; 
    }

    public List<BudgetAccess> findMembersByBudgetId(Long budgetId) {
        if (budgetId == null) return new ArrayList<>();
        List<BudgetAccess> members = new ArrayList<>();

        String sql = "SELECT budget_id, user_id, role FROM budget_access WHERE budget_id = ?";
        
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setLong(1, budgetId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(mapResultSetToBudgetAccess(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка пошуку членів бюджету: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Помилка БД при пошуку членів бюджету.", e);
        }
        return members;
    }


    public boolean delete(Long budgetId, Long userId) {

        if (budgetId == null || userId == null) return false;

        String sql = "DELETE FROM budget_access WHERE budget_id = ? AND user_id = ?";
        
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setLong(1, budgetId);
            ps.setLong(2, userId);
            
            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Помилка видалення доступу: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    
    public boolean deleteAllAccessByBudgetId(Long budgetId) {
        if (budgetId == null) return false;

        String sql = "DELETE FROM budget_access WHERE budget_id = ?";
        
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setLong(1, budgetId);
            
            int affectedRows = ps.executeUpdate();
            return affectedRows >= 0; 
            
        } catch (SQLException e) {
            System.err.println("Помилка видалення всіх прав доступу для бюджету ID " + budgetId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}