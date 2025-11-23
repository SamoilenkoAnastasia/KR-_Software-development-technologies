package ua.kpi.personal.repo;

import ua.kpi.personal.model.access.SharedBudget;
import ua.kpi.personal.model.access.BudgetAccess;
import ua.kpi.personal.util.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SharedBudgetDao {

    /**
     * ? ВИПРАВЛЕНО: Використовуємо budget_id та owner_user_id згідно зі схемою.
     */
    private SharedBudget mapResultSetToSharedBudget(ResultSet rs) throws SQLException {
        SharedBudget budget = new SharedBudget();
        // Замість "id" використовуємо "budget_id"
        budget.setId(rs.getLong("budget_id"));
        budget.setName(rs.getString("name"));
        // Замість "owner_id" використовуємо "owner_user_id"
        budget.setOwnerId(rs.getLong("owner_user_id"));
        return budget;
    }

    // =======================================================
    // 1. СТВОРЕННЯ (CREATE)
    // =======================================================
    public SharedBudget create(SharedBudget budget) {
        // ? ВИПРАВЛЕНО: Використовуємо owner_user_id
        String sql = "INSERT INTO shared_budgets (name, owner_user_id) VALUES (?, ?)";
        
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, budget.getName());
            ps.setLong(2, budget.getOwnerId());
            
            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        // Якщо використовується AUTO_INCREMENT, ключ буде id (тобто budget_id)
                        budget.setId(keys.getLong(1)); 
                        return budget;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка створення бюджету: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // =======================================================
    // 2. ПОШУК ЗА ID (READ)
    // =======================================================
    public SharedBudget findById(Long id) {
        // ? ВИПРАВЛЕНО: Використовуємо budget_id та owner_user_id
        String sql = "SELECT budget_id, name, owner_user_id FROM shared_budgets WHERE budget_id = ?";
        
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setLong(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSharedBudget(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка пошуку бюджету за ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // =======================================================
    // 3. ПОШУК УСІХ БЮДЖЕТІВ КОРИСТУВАЧА
    // =======================================================
    /**
     * Знаходить усі бюджети (SharedBudget та приватний), до яких користувач має доступ.
     * @param userId ID користувача.
     * @return Список SharedBudget, включаючи приватний бюджет.
     */
    public List<SharedBudget> findBudgetsByUserId(Long userId) {
        // ? ВИПРАВЛЕНО: Використовуємо budget_id та owner_user_id
        String sql = "SELECT budget_id, name, owner_user_id FROM shared_budgets WHERE owner_user_id = ? OR budget_id IN (SELECT budget_id FROM budget_access WHERE user_id = ?)";
        
        List<SharedBudget> budgets = new ArrayList<>();
        
        // ? ДОДАЄМО ПРИВАТНИЙ БЮДЖЕТ (Він завжди існує і має ID == ID Користувача)
        SharedBudget privateBudget = new SharedBudget();
        privateBudget.setId(userId); 
        privateBudget.setName("Мій Персональний Бюджет");
        privateBudget.setOwnerId(userId);
        budgets.add(privateBudget);
        
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setLong(1, userId); // owner_user_id = ?
            ps.setLong(2, userId); // user_id = ?
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Порівнюємо ID з бази даних з ID користувача, щоб не дублювати приватний бюджет
                    if (rs.getLong("budget_id") != userId.longValue()) { 
                        budgets.add(mapResultSetToSharedBudget(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка пошуку бюджетів користувача: " + e.getMessage());
            e.printStackTrace();
        }
        return budgets;
    }
}