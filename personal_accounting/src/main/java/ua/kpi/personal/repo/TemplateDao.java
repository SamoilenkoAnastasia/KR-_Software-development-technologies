package ua.kpi.personal.repo;

import ua.kpi.personal.model.*;
import ua.kpi.personal.util.Db;
import ua.kpi.personal.model.TransactionTemplate.RecurringType;

import java.sql.*;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

public class TemplateDao {

    private final CategoryDao categoryDao = new CategoryDao();

    private final String SELECT_FIELDS =
        "t.id, t.name, t.user_id, t.type, t.default_amount, t.description, t.currency, " +
        "t.recurring_type, t.recurrence_interval, t.start_date, t.last_execution_date, t.day_of_month, t.day_of_week, " +
        "c.id AS category_id, c.name AS category_name, c.type AS category_type, " +
        "a.id AS account_id, a.name AS account_name, a.balance AS account_balance, a.currency AS account_currency";

    public TransactionTemplate create(TransactionTemplate template) {
        String sql = "INSERT INTO transaction_templates (name, user_id, type, default_amount, description, category_id, account_id, currency, " +
                     "recurring_type, recurrence_interval, start_date, day_of_month, day_of_week) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, template.getName());
            ps.setLong(2, template.getUser() != null ? template.getUser().getId() : 0);
            ps.setString(3, template.getType());
            ps.setObject(4, template.getDefaultAmount(), Types.DOUBLE);
            ps.setString(5, template.getDescription());

            ps.setObject(6, template.getCategory() != null ? template.getCategory().getId() : null, Types.BIGINT);
            ps.setObject(7, template.getAccount() != null ? template.getAccount().getId() : null, Types.BIGINT);
            ps.setString(8, template.getCurrency() != null ? template.getCurrency() : "UAH");

            ps.setString(9, template.getRecurringType().name());
            ps.setObject(10, template.getRecurrenceInterval(), Types.INTEGER);
            ps.setObject(11, template.getStartDate() != null ? Date.valueOf(template.getStartDate()) : null, Types.DATE);
            ps.setObject(12, template.getDayOfMonth(), Types.INTEGER);
            ps.setObject(13, template.getDayOfWeek() != null ? template.getDayOfWeek().name() : null, Types.VARCHAR);

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

    public TransactionTemplate findById(Long id) {
        final String SQL = String.format("""
            SELECT %s FROM transaction_templates t
            LEFT JOIN categories c ON t.category_id = c.id
            LEFT JOIN accounts a ON t.account_id = a.id
            WHERE t.id = ?
            """, SELECT_FIELDS);

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapFullResultSetToTemplate(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    public List<TransactionTemplate> findByUserId(Long userId) {
        List<TransactionTemplate> templates = new ArrayList<>();
        final String SQL = String.format("""
            SELECT %s FROM transaction_templates t
            LEFT JOIN categories c ON t.category_id = c.id
            LEFT JOIN accounts a ON t.account_id = a.id
            WHERE t.user_id = ?
            ORDER BY t.name ASC
            """, SELECT_FIELDS);

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TransactionTemplate t = mapFullResultSetToTemplate(rs);
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

        final String SQL = String.format("""
            SELECT %s FROM transaction_templates t
            LEFT JOIN categories c ON t.category_id = c.id
            LEFT JOIN accounts a ON t.account_id = a.id
            WHERE t.user_id = ? AND t.recurring_type != 'NONE'
            ORDER BY t.name ASC
            """, SELECT_FIELDS);

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(SQL)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TransactionTemplate t = mapFullResultSetToTemplate(rs);
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

    private TransactionTemplate mapFullResultSetToTemplate(ResultSet rs) throws SQLException {
        TransactionTemplate t = new TransactionTemplate();

        t.setId(rs.getLong("id"));
        t.setName(rs.getString("name"));
        t.setType(rs.getString("type"));
        t.setDescription(rs.getString("description"));
        t.setCurrency(rs.getString("currency"));

        double amount = rs.getDouble("default_amount");
        if (!rs.wasNull()) {
            t.setDefaultAmount(amount);
        } else {
            t.setDefaultAmount(null);
        }

        User user = new User();
        user.setId(rs.getLong("user_id"));
        t.setUser(user);

        Long categoryId = rs.getLong("category_id");
        if (!rs.wasNull()) {
            Category category = new Category(
                categoryId,
                rs.getString("category_name"),
                rs.getString("category_type")
            );
            t.setCategory(category);
        } else {
            t.setCategory(null);
        }

        Long accountId = rs.getLong("account_id");
        if (!rs.wasNull()) {
            Account account = new Account();
            account.setId(accountId);
            account.setName(rs.getString("account_name"));
            account.setBalance(rs.getDouble("account_balance"));
            account.setCurrency(rs.getString("account_currency"));
            t.setAccount(account);
        } else {
            t.setAccount(null);
        }

        String recurringTypeStr = rs.getString("recurring_type");
        if (recurringTypeStr != null) {
            t.setRecurringType(RecurringType.valueOf(recurringTypeStr));
        } else {
            t.setRecurringType(RecurringType.NONE);
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
            t.setDayOfWeek(DayOfWeek.valueOf(dayOfWeekStr));
        }

        return t;
    }
}