/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ua.kpi.personal.repo;

import ua.kpi.personal.model.Goal;
import ua.kpi.personal.model.User;
import ua.kpi.personal.util.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class GoalDao {
    
    // Припустимо, що в БД є таблиця 'goals'
    private static final String TABLE_NAME = "goals";

    /**
     * Знайти всі цілі користувача.
     */
    public List<Goal> findByUserId(Long userId){ 
        var list = new ArrayList<Goal>();
        // SQL: пошук цілей, створених користувачем
        String sql = "SELECT id, user_id, name, target_amount, current_amount, currency, deadline, is_family_fund FROM " + TABLE_NAME + " WHERE user_id = ?";
        
        try(Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)) { 
            
            ps.setLong(1, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()){
                    list.add(mapResultSetToGoal(rs));
                }
            }
        } catch(SQLException e){ e.printStackTrace(); }
        return list;
    }
    
    /**
     * Знайти ціль за ID та ID користувача для перевірки прав доступу.
     */
    public Goal findById(Long id, Long userId){ 
        String sql = "SELECT id, user_id, name, target_amount, current_amount, currency, deadline, is_family_fund FROM " + TABLE_NAME + " WHERE id = ? AND user_id = ?";
        
        try(Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)) { 
            
            ps.setLong(1, id);
            ps.setLong(2, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()){
                    return mapResultSetToGoal(rs);
                }
            }
        } catch(SQLException e){ e.printStackTrace(); }
        return null;
    }
    
    /**
     * Оновлення існуючої цілі (особливо важливе для оновлення current_amount).
     */
    public Goal update(Goal goal){
        String sql = "UPDATE " + TABLE_NAME + " SET name=?, target_amount=?, current_amount=?, currency=?, deadline=?, is_family_fund=? WHERE id=? AND user_id=?";
        
        try(Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)) { 
            
            ps.setString(1, goal.getName());
            ps.setDouble(2, goal.getTargetAmount()); 
            ps.setDouble(3, goal.getCurrentAmount() == null ? 0.0 : goal.getCurrentAmount()); // Оновлення балансу
            ps.setString(4, goal.getCurrency());
            ps.setDate(5, goal.getDeadline() != null ? new java.sql.Date(goal.getDeadline().getTime()) : null);
            ps.setBoolean(6, goal.getIsFamilyFund());
            ps.setLong(7, goal.getId());
            ps.setObject(8, goal.getUser() != null ? goal.getUser().getId() : null); 
            
            ps.executeUpdate();
            return goal;
        } catch(SQLException e){ e.printStackTrace(); return null; }
    }
    
    /**
     * Створення нової цілі.
     */
    public Goal create(Goal goal){
        String sql = "INSERT INTO " + TABLE_NAME + " (user_id, name, target_amount, current_amount, currency, deadline, is_family_fund) VALUES (?,?,?,?,?,?,?)";
        
        try(Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { 
            
            ps.setObject(1, goal.getUser() != null ? goal.getUser().getId() : null); 
            ps.setString(2, goal.getName());
            ps.setDouble(3, goal.getTargetAmount());
            ps.setDouble(4, goal.getCurrentAmount() == null ? 0.0 : goal.getCurrentAmount());
            ps.setString(5, goal.getCurrency());
            ps.setDate(6, goal.getDeadline() != null ? new java.sql.Date(goal.getDeadline().getTime()) : null);
            ps.setBoolean(7, goal.getIsFamilyFund());
            
            ps.executeUpdate();
            
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if(keys.next()) goal.setId(keys.getLong(1));
            }
            return goal;
        } catch(SQLException e){ e.printStackTrace(); return null; }
    }
    
    // Допоміжний метод для уникнення дублювання коду в методах find
    private Goal mapResultSetToGoal(ResultSet rs) throws SQLException {
        Goal g = new Goal();
        g.setId(rs.getLong("id"));
        g.setName(rs.getString("name"));
        g.setTargetAmount(rs.getDouble("target_amount"));
        g.setCurrentAmount(rs.getDouble("current_amount"));
        g.setCurrency(rs.getString("currency"));
        
        Timestamp ts = rs.getTimestamp("deadline");
        if (ts != null) g.setDeadline(new Date(ts.getTime()));
        
        g.setIsFamilyFund(rs.getBoolean("is_family_fund"));
        
        User u = new User();
        u.setId(rs.getLong("user_id"));
        g.setUser(u);
        return g;
    }
    
    // TODO: Додати метод delete
}