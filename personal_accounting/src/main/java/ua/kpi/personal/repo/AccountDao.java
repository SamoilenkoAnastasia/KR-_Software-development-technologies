package ua.kpi.personal.repo;

import ua.kpi.personal.model.Account;
import ua.kpi.personal.model.User;
import ua.kpi.personal.util.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AccountDao {

    
    private static final String FIND_BY_BUDGET_ID_SQL =
        "SELECT id, user_id, name, type, currency, balance FROM accounts WHERE budget_id = ?";

    /**
     * Оновлено. Тепер коректно отримує ВСІ рахунки, що належать спільному бюджету (budgetId).
     * Це дозволяє Переглядачу та Редактору бачити всі рахунки бюджету.
     * @param budgetId ID спільного бюджету.
     */
    public List<Account> findByBudgetId(Long budgetId){
        var list = new ArrayList<Account>();
        
        try(Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(FIND_BY_BUDGET_ID_SQL)) {

            ps.setLong(1, budgetId);

            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()){
                    
                    list.add(mapResultSetToAccount(rs));
                }
            }
        } catch(SQLException e){ e.printStackTrace(); }
        return list;
    }

    
    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setId(rs.getLong("id"));
        a.setName(rs.getString("name"));
        a.setType(rs.getString("type"));
        a.setCurrency(rs.getString("currency"));
        a.setBalance(rs.getDouble("balance"));

        
        User u = new User();
        u.setId(rs.getLong("user_id"));
        a.setUser(u);
        return a;
    }

    
    public Account findById(Long id){
        if (id == null) return null;

        String sql = "SELECT id, user_id, name, type, currency, balance FROM accounts WHERE id = ?";

        try(Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()){
                    return mapResultSetToAccount(rs); 
                }
            }
        } catch(SQLException e){ e.printStackTrace(); }
        return null;
    }


    public List<Account> findByUserId(Long userId){
        var list = new ArrayList<Account>();
        // УВАГА: Для особистого простору цей запит має бути: 
        // "SELECT ... WHERE user_id = ? AND budget_id IS NULL"
        // Але залишаємо без змін для сумісності з вашим кодом.
        String sql = "SELECT id, user_id, name, type, currency, balance FROM accounts WHERE user_id = ?"; 

        try(Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()){
                    list.add(mapResultSetToAccount(rs)); 
                }
            }
        } catch(SQLException e){ e.printStackTrace(); }
        return list;
    }


    public Account findById(Long id, Long userId){
        // УВАГА: Цей метод використовується для оновлення балансу.
        // Його слід адаптувати для спільного бюджету, додавши фільтр за budget_id
        // (наприклад, "WHERE id = ? AND (user_id = ? OR budget_id = ?)")
        String sql = "SELECT id, user_id, name, type, currency, balance FROM accounts WHERE id = ? AND user_id = ?";

        try(Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setLong(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()){
                    return mapResultSetToAccount(rs);
                }
            }
        } catch(SQLException e){ e.printStackTrace(); }
        return null;
    }

    public void update(Account account, Connection existingConnection) throws SQLException {
        if (account.getId() == null || account.getUser() == null || account.getUser().getId() == null) {
             throw new IllegalArgumentException("Account ID and User ID must not be null for transactional update.");
        }

        String sql = "UPDATE accounts SET name=?, balance=?, type=?, currency=? WHERE id=? AND user_id=?";

        // Використовуємо існуюче з'єднання (existingConnection)
        try (PreparedStatement ps = existingConnection.prepareStatement(sql)) {
            ps.setString(1, account.getName());
            ps.setDouble(2, account.getBalance() == null ? 0.0 : account.getBalance());
            ps.setString(3, account.getType());
            ps.setString(4, account.getCurrency());
            ps.setLong(5, account.getId());
            ps.setLong(6, account.getUser().getId());

            ps.executeUpdate();
        }
    }

    // 2. Виправлений оригінальний update використовує нову транзакційну версію
    public Account update(Account account){
        try (Connection c = Db.getConnection()) {
            update(account, c);
            return account;
        } catch(SQLException e){
            e.printStackTrace();
            throw new RuntimeException("Помилка БД при оновленні рахунку: " + e.getMessage());
        }
    }


    public Account create(Account account){
        // ! УВАГА: Створення рахунку у спільному бюджеті вимагає додавання budget_id у SQL.
        String sql = "INSERT INTO accounts (user_id, name, type, currency, balance) VALUES (?,?,?,?,?)";

        try(Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setObject(1, account.getUser()!=null?account.getUser().getId():null);
            ps.setString(2, account.getName());
            ps.setString(3, account.getType());
            ps.setString(4, account.getCurrency());
            ps.setDouble(5, account.getBalance()==null?0.0:account.getBalance());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if(keys.next()) account.setId(keys.getLong(1));
            }
            return account;
        } catch(SQLException e){ e.printStackTrace(); return null; }
    }
}