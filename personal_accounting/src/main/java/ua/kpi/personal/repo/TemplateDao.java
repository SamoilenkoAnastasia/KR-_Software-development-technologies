package ua.kpi.personal.repo;

import ua.kpi.personal.model.TransactionTemplate;
import ua.kpi.personal.model.Category;
import ua.kpi.personal.model.Account;
import ua.kpi.personal.model.User;
import ua.kpi.personal.util.Db;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TemplateDao {

    private final String ALL_FIELDS = "id, name, user_id, type, default_amount, description, category_id, account_id, " +
                                      "recurring_type, recurrence_interval, start_date, last_execution_date, day_of_month, day_of_week";
    
    public TransactionTemplate create(TransactionTemplate template) {
        String sql = "INSERT INTO transaction_templates (name, user_id, type, default_amount, description, category_id, account_id, " +
                     "recurring_type, recurrence_interval, start_date, day_of_month, day_of_week) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                     
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, template.getName());
            ps.setLong(2, template.getUser() != null ? template.getUser().getId() : 0);
            ps.setString(3, template.getType());
            ps.setObject(4, template.getDefaultAmount(), Types.DOUBLE);
            ps.setString(5, template.getDescription());
            ps.setObject(6, template.getCategory() != null ? template.getCategory().getId() : null, Types.BIGINT);
            ps.setObject(7, template.getAccount() != null ? template.getAccount().getId() : null, Types.BIGINT);
            
            ps.setString(8, template.getRecurringType().name());
            ps.setObject(9, template.getRecurrenceInterval(), Types.INTEGER);
            ps.setObject(10, template.getStartDate() != null ? Date.valueOf(template.getStartDate()) : null, Types.DATE);
            ps.setObject(11, template.getDayOfMonth(), Types.INTEGER);
            ps.setObject(12, template.getDayOfWeek() != null ? template.getDayOfWeek().name() : null, Types.VARCHAR);
            
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    template.setId(keys.getLong(1));
                } else {
                    throw new SQLException("Creating template failed, no ID obtained.");
                }
            }
            return template;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<TransactionTemplate> findByUserId(Long userId) {
        List<TransactionTemplate> templates = new ArrayList<>();
        String sql = "SELECT " + ALL_FIELDS + " FROM transaction_templates t WHERE t.user_id = ?";
                    
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TransactionTemplate t = mapResultSetToTemplate(rs, userId);
                    templates.add(t);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return templates;
    }

    public List<TransactionTemplate> findRecurringByUserId(Long userId) {
        List<TransactionTemplate> recurringTemplates = new ArrayList<>();
        
        String sql = "SELECT " + ALL_FIELDS + " FROM transaction_templates t " +
                     "WHERE t.user_id = ? AND t.recurring_type != 'NONE'";
                     
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TransactionTemplate t = mapResultSetToTemplate(rs, userId);
                    recurringTemplates.add(t);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return recurringTemplates;
    }
    
    public boolean updateLastExecutionDate(Long templateId, LocalDate date) {
        String sql = "UPDATE transaction_templates SET last_execution_date = ? WHERE id = ?";
        
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setDate(1, Date.valueOf(date));
            ps.setLong(2, templateId);
            
            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(Long templateId) {
        String sql = "DELETE FROM transaction_templates WHERE id = ?";
        
          try (Connection c = Db.getConnection();
               PreparedStatement ps = c.prepareStatement(sql)) {

              ps.setLong(1, templateId);
              return ps.executeUpdate() > 0;

          } catch (SQLException e) {
              e.printStackTrace();
              return false;
          }
    }

    private TransactionTemplate mapResultSetToTemplate(ResultSet rs, Long userId) throws SQLException {
        TransactionTemplate t = new TransactionTemplate();
        
        t.setId(rs.getLong("id"));
        t.setName(rs.getString("name"));
        t.setType(rs.getString("type"));
        t.setDescription(rs.getString("description"));

        double amount = rs.getDouble("default_amount");
        if (!rs.wasNull()) {
            t.setDefaultAmount(amount);
        }
        
        User user = new User();
        user.setId(userId);
        t.setUser(user);

        Long categoryId = rs.getLong("category_id");
        if (!rs.wasNull()) {
            t.setCategory(CategoryCache.getById(categoryId));
        }
        Long accountId = rs.getLong("account_id");
        if (!rs.wasNull()) {
            Account acc = new Account();
            acc.setId(accountId);
            t.setAccount(acc); 
        }

        String recurringTypeStr = rs.getString("recurring_type");
        if (recurringTypeStr != null) {
            t.setRecurringType(recurringTypeStr);
        }
        
        int interval = rs.getInt("recurrence_interval");
        if (!rs.wasNull()) {
             t.setRecurrenceInterval(interval);
        } else {
             t.setRecurrenceInterval(null);
        }
        
        Date startDate = rs.getDate("start_date");
        if (startDate != null) t.setStartDate(startDate.toLocalDate());
        
        Date lastDate = rs.getDate("last_execution_date");
        if (lastDate != null) t.setLastExecutionDate(lastDate.toLocalDate());
        
        int dayOfMonth = rs.getInt("day_of_month");
        if (!rs.wasNull()) t.setDayOfMonth(dayOfMonth);
        
        String dayOfWeekStr = rs.getString("day_of_week");
        if (dayOfWeekStr != null) {
            t.setDayOfWeek(dayOfWeekStr);
        }

        return t;
    }
}