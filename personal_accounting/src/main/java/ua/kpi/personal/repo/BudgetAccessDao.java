package ua.kpi.personal.repo;

import ua.kpi.personal.model.access.BudgetAccess;
import ua.kpi.personal.util.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BudgetAccessDao {

    // --- Мапінг результатів на BudgetAccess ---
    private BudgetAccess mapResultSetToBudgetAccess(ResultSet rs) throws SQLException {
        BudgetAccess access = new BudgetAccess();
        
        // ? ВИПРАВЛЕННЯ 1: Видалено map.setId(), оскільки таблиця не має стовпця 'id'
        // Якщо таблиця не має 'id', то: access.setId(rs.getLong("id")) спричинить помилку.
        
        access.setBudgetId(rs.getLong("budget_id"));
        access.setUserId(rs.getLong("user_id"));
        
        // ? ВИПРАВЛЕННЯ 2: Замість "access_role" використовуємо "role"
        access.setAccessRole(rs.getString("role"));
        return access;
    }

    // =======================================================
    // 1. ЗБЕРЕЖЕННЯ/ОНОВЛЕННЯ (UPSERT)
    // =======================================================
    /**
     * Вставляє новий запис або оновлює існуючий (через складений ключ).
     * @param access Об'єкт BudgetAccess.
     * @return Збережений об'єкт BudgetAccess.
     */
    public BudgetAccess save(BudgetAccess access) {
        // ? ВИПРАВЛЕННЯ 3: Використовуємо UPSERT (INSERT ... ON DUPLICATE KEY UPDATE),
        // оскільки немає ID для розрізнення INSERT і UPDATE.
        String sql = "INSERT INTO budget_access (budget_id, user_id, role) VALUES (?, ?, ?)" +
                     " ON DUPLICATE KEY UPDATE role = ?";
        
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            // Параметри INSERT
            ps.setLong(1, access.getBudgetId());
            ps.setLong(2, access.getUserId());
            ps.setString(3, access.getAccessRole());
            
            // Параметри UPDATE (повторюємо роль)
            ps.setString(4, access.getAccessRole());
            
            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                 // Успішне збереження або оновлення
                return access;
            }
        } catch (SQLException e) {
            System.err.println("Помилка UPSERT доступу до бюджету: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    // Примітка: Методи insert/update видалено, оскільки вони не потрібні при використанні UPSERT.
    
    // =======================================================
    // 2. ПОШУК ДОСТУПУ (READ)
    // =======================================================
    public BudgetAccess findAccessByBudgetAndUser(Long budgetId, Long userId) {
        // ? ВАЖЛИВА ЛОГІКА ДЛЯ ПРИВАТНОГО БЮДЖЕТУ
        if (budgetId.equals(userId)) {
            BudgetAccess privateAccess = new BudgetAccess();
            privateAccess.setBudgetId(budgetId);
            privateAccess.setUserId(userId);
            // Припускаємо, що у вас є константа BudgetAccess.ROLE_OWNER
            // Або використовуйте пряме значення ролі, наприклад, "OWNER"
            privateAccess.setAccessRole("OWNER"); 
            return privateAccess;
        }

        // Для спільного бюджету шукаємо запис у таблиці
        // ? ВИПРАВЛЕННЯ 4: Видалено посилання на 'id' та замінено 'access_role' на 'role'
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
        }
        return null;
    }
    
    // =======================================================
    // 3. ПОШУК ЧЛЕНІВ БЮДЖЕТУ
    // =======================================================
    public List<BudgetAccess> findMembersByBudgetId(Long budgetId) {
        List<BudgetAccess> members = new ArrayList<>();
        // ? ВИПРАВЛЕННЯ 5: Видалено посилання на 'id' та замінено 'access_role' на 'role'
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
        }
        return members;
    }

    // =======================================================
    // 4. ВИДАЛЕННЯ ДОСТУПУ
    // =======================================================
    public boolean delete(Long budgetId, Long userId) {
        // Запобігаємо видаленню власника приватного бюджету
        if (budgetId.equals(userId)) {
             System.err.println("Помилка: Не можна видалити власника з його приватного бюджету.");
             return false;
        }

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
}