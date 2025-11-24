package ua.kpi.personal.repo;

import ua.kpi.personal.model.Goal;
import ua.kpi.personal.util.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class GoalDao {

    private static final String TABLE_NAME = "goals";

    private static final String FIND_BY_BUDGET_ID_SQL =
        "SELECT id, budget_id, name, target_amount, current_amount, currency, deadline FROM " + TABLE_NAME + " WHERE budget_id = ?";

    private static final String FIND_BY_ID_AND_BUDGET_ID_SQL =
        "SELECT id, budget_id, name, target_amount, current_amount, currency, deadline FROM " + TABLE_NAME + " WHERE id = ? AND budget_id = ?";

    private static final String INSERT_SQL =
        "INSERT INTO " + TABLE_NAME + " (budget_id, name, target_amount, current_amount, currency, deadline) VALUES (?,?,?,?,?,?)";

    private static final String UPDATE_SQL =
        "UPDATE " + TABLE_NAME + " SET name=?, target_amount=?, current_amount=?, currency=?, deadline=? WHERE id=? AND budget_id=?";

    public List<Goal> findByBudgetId(Long budgetId){
        var list = new ArrayList<Goal>();

        try(Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(FIND_BY_BUDGET_ID_SQL)) {

            ps.setLong(1, budgetId);

            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()){
                    list.add(mapResultSetToGoal(rs));
                }
            }
        } catch(SQLException e){ e.printStackTrace(); }
        return list;
    }

    public Goal findById(Long id, Long budgetId){

        try(Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(FIND_BY_ID_AND_BUDGET_ID_SQL)) {

            ps.setLong(1, id);
            ps.setLong(2, budgetId);

            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()){
                    return mapResultSetToGoal(rs);
                }
            }
        } catch(SQLException e){ e.printStackTrace(); }
        return null;
    }

    public Goal update(Goal goal){

        try(Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(UPDATE_SQL)) {

            ps.setString(1, goal.getName());
            ps.setDouble(2, goal.getTargetAmount());
            ps.setDouble(3, goal.getCurrentAmount() == null ? 0.0 : goal.getCurrentAmount());
            ps.setString(4, goal.getCurrency());
            ps.setDate(5, goal.getDeadline() != null ? new java.sql.Date(goal.getDeadline().getTime()) : null);

            ps.setLong(6, goal.getId());
            ps.setLong(7, goal.getBudgetId());

            ps.executeUpdate();
            return goal;
        } catch(SQLException e){ e.printStackTrace(); return null; }
    }

    public Goal create(Goal goal){

        try(Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, goal.getBudgetId());
            ps.setString(2, goal.getName());
            ps.setDouble(3, goal.getTargetAmount());
            ps.setDouble(4, goal.getCurrentAmount() == null ? 0.0 : goal.getCurrentAmount());
            ps.setString(5, goal.getCurrency());
            ps.setDate(6, goal.getDeadline() != null ? new java.sql.Date(goal.getDeadline().getTime()) : null);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if(keys.next()) goal.setId(keys.getLong(1));
            }
            return goal;
        } catch(SQLException e){ e.printStackTrace(); return null; }
    }

    private Goal mapResultSetToGoal(ResultSet rs) throws SQLException {
        Goal g = new Goal();
        g.setId(rs.getLong("id"));
        g.setName(rs.getString("name"));
        g.setTargetAmount(rs.getDouble("target_amount"));
        g.setCurrentAmount(rs.getDouble("current_amount"));
        g.setCurrency(rs.getString("currency"));

        Timestamp ts = rs.getTimestamp("deadline");
        if (ts != null) g.setDeadline(new Date(ts.getTime()));

        g.setBudgetId(rs.getLong("budget_id"));

        return g;
    }

}