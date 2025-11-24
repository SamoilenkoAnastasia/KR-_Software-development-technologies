package ua.kpi.personal.repo;

import ua.kpi.personal.model.access.SharedBudget;
import ua.kpi.personal.model.access.BudgetAccess;
import ua.kpi.personal.util.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SharedBudgetDao {

    private SharedBudget mapResultSetToSharedBudget(ResultSet rs) throws SQLException {
        SharedBudget budget = new SharedBudget();
        budget.setId(rs.getLong("budget_id"));
        budget.setName(rs.getString("name"));
        budget.setOwnerId(rs.getLong("owner_user_id"));
        return budget;
    }

    public SharedBudget create(SharedBudget budget) {
        String sql = "INSERT INTO shared_budgets (name, owner_user_id) VALUES (?, ?)";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, budget.getName());
            ps.setLong(2, budget.getOwnerId());

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
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

    public SharedBudget findById(Long id) {
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

    public List<SharedBudget> findBudgetsByUserId(Long userId) {
        String sql = "SELECT budget_id, name, owner_user_id FROM shared_budgets WHERE owner_user_id = ? OR budget_id IN (SELECT budget_id FROM budget_access WHERE user_id = ?)";

        List<SharedBudget> budgets = new ArrayList<>();

        SharedBudget privateBudget = new SharedBudget();
        privateBudget.setId(userId);
        privateBudget.setName("Мій Персональний Бюджет");
        privateBudget.setOwnerId(userId);
        budgets.add(privateBudget);

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
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