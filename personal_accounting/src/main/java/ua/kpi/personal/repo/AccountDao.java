package ua.kpi.personal.repo;

import ua.kpi.personal.model.Account;
import ua.kpi.personal.model.User;
import ua.kpi.personal.util.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AccountDao {

    
    // !!! ОНОВЛЕННЯ SELECT_FIELDS !!!
    private static final String SELECT_FIELDS = "id, user_id, name, type, currency, balance, budget_id, is_shared"; 
    
    private static final String FIND_BY_BUDGET_ID_SQL =
        "SELECT " + SELECT_FIELDS + " FROM accounts WHERE budget_id = ?";

    // Логіка findByBudgetId залишається без змін, оскільки фільтрацію прав доступу
    // ми зробимо в AccountService
    public List<Account> findByBudgetId(Long budgetId){
        if (budgetId == null) return new ArrayList<>();
        var list = new ArrayList<Account>();
        
        try(Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(FIND_BY_BUDGET_ID_SQL)) {

            ps.setLong(1, budgetId);

            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()){
                    list.add(mapResultSetToAccount(rs));
                }
            }
        } catch(SQLException e){ 
            e.printStackTrace(); 
            throw new RuntimeException("Помилка БД при отриманні рахунків за budgetId: " + e.getMessage());
        }
        return list;
    }
    
    
    public Account findById(Long id, Long budgetId){
         if (id == null || budgetId == null) return null;

         String sql = "SELECT " + SELECT_FIELDS + " FROM accounts WHERE id = ? AND budget_id = ?";

         try(Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

             ps.setLong(1, id);
             ps.setLong(2, budgetId);

             try (ResultSet rs = ps.executeQuery()) {
                 if(rs.next()){
                     return mapResultSetToAccount(rs); 
                 }
             }
         } catch(SQLException e){ 
             e.printStackTrace(); 
             throw new RuntimeException("Помилка БД при пошуку рахунку за ID та Budget ID: " + e.getMessage());
         }
         return null;
    }


    public Account findById(Long id){
        if (id == null) return null;
        String sql = "SELECT " + SELECT_FIELDS + " FROM accounts WHERE id = ?"; 

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
        if (userId == null) return new ArrayList<>();
        var list = new ArrayList<Account>();
        String sql = "SELECT " + SELECT_FIELDS + " FROM accounts WHERE user_id = ?";

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


    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        Account a = new Account();
        a.setId(rs.getLong("id"));
        a.setName(rs.getString("name"));
        a.setType(rs.getString("type"));
        a.setCurrency(rs.getString("currency"));
        a.setBalance(rs.getDouble("balance"));
 
        Long budgetId = rs.getLong("budget_id");
        if (!rs.wasNull()) {
             a.setBudgetId(budgetId);
        }

        // !!! ЧИТАННЯ НОВОГО ПОЛЯ is_shared !!!
        a.setShared(rs.getBoolean("is_shared"));
        
        User u = new User();
        u.setId(rs.getLong("user_id"));
        a.setUser(u);
        
        return a;
    }

    // UPDATE (Транзакційний - використовується JdbcTransactionProcessor)
    public void update(Account account, Connection existingConnection) throws SQLException {
        if (account.getId() == null || account.getUser() == null || account.getUser().getId() == null || account.getBudgetId() == null) {
             throw new IllegalArgumentException("Account ID, User ID, and Budget ID must not be null for transactional update.");
        }

        // !!! ОНОВЛЕННЯ SQL: ДОДАНО is_shared !!!
        // Рахунок оновлюється лише за id та user_id (власником)
        String sql = "UPDATE accounts SET name=?, balance=?, type=?, currency=?, budget_id=?, is_shared=? WHERE id=? AND user_id=?"; 

        try (PreparedStatement ps = existingConnection.prepareStatement(sql)) {
            ps.setString(1, account.getName());
            ps.setDouble(2, account.getBalance() == null ? 0.0 : account.getBalance());
            ps.setString(3, account.getType());
            ps.setString(4, account.getCurrency());
            ps.setLong(5, account.getBudgetId()); 
            ps.setBoolean(6, account.isShared()); // ЗБЕРІГАННЯ isShared
            ps.setLong(7, account.getId());
            ps.setLong(8, account.getUser().getId()); // Перевірка власності

            int rows = ps.executeUpdate();
            if (rows == 0) {
                 throw new SQLException("Не вдалося оновити рахунок ID " + account.getId() + ". Перевірте, чи належить він користувачу ID " + account.getUser().getId());
            }
        }
    }

    
    // UPDATE (Нетранзакційний)
    public Account update(Account account){
        try (Connection c = Db.getConnection()) {
            update(account, c); 
            return account;
        } catch(SQLException e){
            e.printStackTrace();
            throw new RuntimeException("Помилка БД при оновленні рахунку: " + e.getMessage());
        }
    }

    
    // CREATE
    public Account create(Account account){
         
         Objects.requireNonNull(account.getUser(), "User must be set on account.");
         Objects.requireNonNull(account.getBudgetId(), "BudgetId must be set on account.");

         // !!! ОНОВЛЕННЯ SQL: ДОДАНО is_shared !!!
         String sql = "INSERT INTO accounts (user_id, name, type, currency, balance, budget_id, is_shared) VALUES (?,?,?,?,?,?,?)";

         try(Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

             ps.setObject(1, account.getUser().getId());
             ps.setString(2, account.getName());
             ps.setString(3, account.getType());
             ps.setString(4, account.getCurrency());
             ps.setDouble(5, account.getBalance()==null?0.0:account.getBalance());
             ps.setLong(6, account.getBudgetId()); 
             ps.setBoolean(7, account.isShared()); // ЗБЕРІГАННЯ isShared

             ps.executeUpdate();

             try (ResultSet keys = ps.getGeneratedKeys()) {
                  if(keys.next()) account.setId(keys.getLong(1));
             }
             return account;
         } catch(SQLException e){ 
             e.printStackTrace(); 
             throw new RuntimeException("Помилка БД при створенні рахунку: " + e.getMessage());
         }
    }
}