package ua.kpi.personal.repo;

import ua.kpi.personal.model.Goal;
import ua.kpi.personal.util.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class GoalDao {

    private static final String TABLE_NAME = "goals";


    private static final String SELECT_FIELDS = "id, budget_id, user_id, name, target_amount, current_amount, currency, deadline, category_id"; 

    private static final String FIND_BY_BUDGET_ID_SQL =
        "SELECT " + SELECT_FIELDS + " FROM " + TABLE_NAME + " WHERE budget_id = ?";

    private static final String FIND_BY_ID_AND_BUDGET_ID_SQL =
        "SELECT " + SELECT_FIELDS + " FROM " + TABLE_NAME + " WHERE id = ? AND budget_id = ?";


    private static final String INSERT_SQL =
        "INSERT INTO " + TABLE_NAME + " (budget_id, user_id, name, target_amount, current_amount, currency, deadline, category_id) VALUES (?,?,?,?,?,?,?,?)"; 

    private static final String UPDATE_SQL =
        "UPDATE " + TABLE_NAME + " SET name=?, target_amount=?, current_amount=?, currency=?, deadline=?, category_id=? WHERE id=? AND budget_id=?"; 
        
    private static final String DELETE_SQL = 
        "DELETE FROM " + TABLE_NAME + " WHERE id=? AND budget_id=?";


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
            ps.setObject(6, goal.getCategoryId());
            ps.setLong(7, goal.getId());
            ps.setLong(8, goal.getBudgetId());

            ps.executeUpdate();
            return goal;
        } catch(SQLException e){ e.printStackTrace(); return null; }
    }

    public Goal create(Goal goal){

        try(Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            int paramIndex = 1;
            
            ps.setLong(paramIndex++, goal.getBudgetId());
            ps.setLong(paramIndex++, goal.getUserId());   
            ps.setString(paramIndex++, goal.getName());
            ps.setDouble(paramIndex++, goal.getTargetAmount());
            ps.setDouble(paramIndex++, goal.getCurrentAmount() == null ? 0.0 : goal.getCurrentAmount());
            ps.setString(paramIndex++, goal.getCurrency());
            ps.setDate(paramIndex++, goal.getDeadline() != null ? new java.sql.Date(goal.getDeadline().getTime()) : null);
            ps.setObject(paramIndex++, goal.getCategoryId());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if(keys.next()) goal.setId(keys.getLong(1));
            }
            return goal;
        } catch(SQLException e){ e.printStackTrace(); return null; }
    }
    
    public void delete(Long goalId, Long budgetId) {
        try (Connection c = Db.getConnection();
              PreparedStatement ps = c.prepareStatement(DELETE_SQL)) {
            ps.setLong(1, goalId);
            ps.setLong(2, budgetId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace(); 
            throw new RuntimeException("Помилка при видаленні цілі ID " + goalId, e);
        }
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
        g.setUserId(rs.getLong("user_id")); 
      
        Long catId = rs.getLong("category_id");
        if (!rs.wasNull()) {
            g.setCategoryId(catId);
        }

        return g;
    }
}