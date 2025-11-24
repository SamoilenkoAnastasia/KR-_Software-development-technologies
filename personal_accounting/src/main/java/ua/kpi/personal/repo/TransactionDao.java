package ua.kpi.personal.repo;

import ua.kpi.personal.model.*;
import ua.kpi.personal.util.Db;
import ua.kpi.personal.model.analytics.ReportParams;
import ua.kpi.personal.repo.CategoryCache; 

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.sql.Date;
import java.sql.Types;
import java.util.stream.Collectors;

public class TransactionDao {

    private final CategoryDao categoryDao = new CategoryDao();
    private final AccountDao accountDao = new AccountDao();
    private final UserDao userDao = new UserDao();

    // !!! ПРИБРАНО: debugLog(Transaction t) !!!

    private final String SELECT_FIELDS = "t.id, t.amount, t.type, t.description, t.created_at, t.category_id, t.account_id, t.user_id, t.currency, t.template_id, t.trans_date, t.budget_id, t.created_by_user_id";

    
    public List<Transaction> findTransactionsByDateRange(ReportParams params, Long budgetId) {
        var list = new ArrayList<Transaction>();

        // Оптимізація N+1: Завантажуємо всі необхідні об'єкти (Account, User) одним запитом
        List<Account> allAccounts = accountDao.findByBudgetId(budgetId);
        List<User> budgetUsers = userDao.findByBudgetId(budgetId);

        String sql = "SELECT " + SELECT_FIELDS + " FROM transactions t WHERE t.budget_id = ? AND t.created_at BETWEEN ? AND ? ORDER BY t.created_at DESC";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setLong(1, budgetId);
            ps.setTimestamp(2, Timestamp.valueOf(params.getStartDate().atStartOfDay()));
            // Включаємо останній день (до кінця дня)
            ps.setTimestamp(3, Timestamp.valueOf(params.getEndDate().plusDays(1).atStartOfDay().minusNanos(1)));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Викликаємо оновлений mapResultSetToTransaction
                    Transaction t = mapResultSetToTransaction(rs, allAccounts, budgetUsers); 
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Помилка БД при отриманні транзакцій за датами: " + e.getMessage());
        }
        return list;
    }

    // =======================================================
    // 2. findById(Long id, long userId)
    // =======================================================
    public Transaction findById(Long id, long userId) {
        if (id == null) return null;

        String sql = "SELECT " + SELECT_FIELDS + " FROM transactions t WHERE t.id = ? AND t.user_id = ?";
        
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.setLong(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Передаємо порожні списки. mapResultSetToTransaction тепер самостійно
                    // завантажить об'єкти User та Account через їхні DAO.
                    return mapResultSetToTransaction(rs, new ArrayList<>(), new ArrayList<>());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Помилка БД при отриманні транзакції за id/userId: " + e.getMessage());
        }
        return null;
    }
    
    // =======================================================
    // 3. findByBudgetId
    // =======================================================
    public List<Transaction> findByBudgetId(Long budgetId){
        var list = new ArrayList<Transaction>();

        // Оптимізація N+1: Завантажуємо всіх для мапінгу
        List<Account> allAccounts = accountDao.findByBudgetId(budgetId); 
        List<User> budgetUsers = userDao.findByBudgetId(budgetId); 

        String sql = "SELECT " + SELECT_FIELDS + " FROM transactions t WHERE t.budget_id = ? ORDER BY t.created_at DESC";
        try(Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, budgetId);    
            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()){
                    Transaction t = mapResultSetToTransaction(rs, allAccounts, budgetUsers);    
                    list.add(t);
                }
            }
        } catch(SQLException e){    
            e.printStackTrace();    
            throw new RuntimeException("Помилка БД при отриманні транзакцій за budgetId: " + e.getMessage());
        }
        return list;
    }

    // =======================================================
    // 4. mapResultSetToTransaction (ОСНОВНЕ ВИПРАВЛЕННЯ)
    // =======================================================
    private Transaction mapResultSetToTransaction(ResultSet rs, List<Account> allAccounts, List<User> budgetUsers) throws SQLException {
        Transaction t = new Transaction();

        t.setId(rs.getLong("id"));
        t.setAmount(rs.getDouble("amount"));
        t.setType(rs.getString("type"));
        t.setDescription(rs.getString("description"));

        Timestamp ts = rs.getTimestamp("created_at");
        if(ts != null) t.setCreatedAt(ts.toLocalDateTime());

        Date transDate = rs.getDate("trans_date");
        if(transDate != null) t.setTransDate(transDate.toLocalDate());

        Long templateId = rs.getLong("template_id");
        if (!rs.wasNull()) {
            t.setTemplateId(templateId);
        }

        Long catId = rs.getLong("category_id");
        if (!rs.wasNull()) {
            // Використовуємо кеш категорій
            t.setCategory(CategoryCache.getById(catId));    
        }

        // --- Завантаження Account ---
        Long accId = rs.getLong("account_id");
        if (!rs.wasNull()) {
            Account foundAccount = allAccounts.stream()
                                     .filter(acc -> acc.getId().equals(accId))
                                     .findFirst()
                                     .orElse(null);
            
            // Якщо кеш порожній (наприклад, викликано findById), завантажуємо напряму
            if (foundAccount == null && accId != 0 && allAccounts.isEmpty()) { 
                foundAccount = accountDao.findById(accId); 
            }
            t.setAccount(foundAccount);
        }

        // --- 1. Встановлення ВЛАСНИКА БЮДЖЕТУ/РАХУНКУ (user_id) ---
        Long ownerUserId = rs.getLong("user_id");
        if (!rs.wasNull()) {
            User owner = null;
            
            // Шукаємо в кеші
            if (budgetUsers != null) {
                owner = budgetUsers.stream()
                   .filter(user -> user.getId().equals(ownerUserId))
                   .findFirst()
                   .orElse(null);
            }
            // Якщо не знайшли в кеші, завантажуємо напряму
            if (owner == null) {
                owner = userDao.findById(ownerUserId); 
            }
            t.setUser(owner);
        }

        // --- 2. Встановлення ТВОРЦЯ ТРАНЗАКЦІЇ (created_by_user_id) ---
        
        // !!! КЛЮЧОВЕ ВИПРАВЛЕННЯ: Використовуємо rs.getObject для Long ID !!!
        Long createdByUserId = (Long) rs.getObject("created_by_user_id"); 
        
        if (createdByUserId != null) { // Перевірка на NULL об'єкта
            User createdBy = null;

            // Шукаємо в кеші
            if (budgetUsers != null) {
                createdBy = budgetUsers.stream()
                   .filter(user -> user.getId().equals(createdByUserId))
                   .findFirst()
                   .orElse(null);
            }
            
            // Якщо не знайшли в кеші, ЗАВЖДИ ЗАВАНТАЖУЄМО З БД
            if (createdBy == null) {
                createdBy = userDao.findById(createdByUserId); 
            }
            
            if (createdBy != null) {
                t.setCreatedBy(createdBy);
            } else {
                // Запасний варіант, якщо користувач видалений
                User fallback = new User();
                fallback.setId(createdByUserId);
                fallback.setFullName("Невідомий користувач (ID " + createdByUserId + ")");
                t.setCreatedBy(fallback);
            }
        } 
        
        // !!! FALLBACK: Якщо created_by_user_id був NULL в БД, використовуємо власника, якщо він є !!!
        if (t.getCreatedBy() == null && t.getUser() != null) {
            t.setCreatedBy(t.getUser());
        }
        
        // !!! ПРИБРАНО: Виклик debugLog(t) !!!
        
        Long budgetId = rs.getLong("budget_id");
        if (!rs.wasNull()) {
            t.setBudgetId(budgetId);
        }

        t.setCurrency(rs.getString("currency"));

        return t;
    }

    // =======================================================
    // 5. create
    // =======================================================
    public Transaction create(Transaction tx, Connection c) throws SQLException {
        String sql = "INSERT INTO transactions (amount, type, description, created_at, category_id, account_id, user_id, currency, template_id, trans_date, budget_id, created_by_user_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";    

        try(PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setDouble(1, tx.getAmount());
            ps.setString(2, tx.getType());
            ps.setString(3, tx.getDescription());
            ps.setTimestamp(4, Timestamp.valueOf(tx.getCreatedAt() != null ? tx.getCreatedAt() : LocalDateTime.now()));
            ps.setObject(5, tx.getCategory()!=null?tx.getCategory().getId():null);
            ps.setObject(6, tx.getAccount()!=null?tx.getAccount().getId():null);
            ps.setObject(7, tx.getUser()!=null?tx.getUser().getId():null);
            ps.setString(8, tx.getCurrency());
            ps.setObject(9, tx.getTemplateId(), Types.BIGINT);
            ps.setObject(10, tx.getTransDate() != null ? Date.valueOf(tx.getTransDate()) : null, Types.DATE);
            ps.setObject(11, tx.getBudgetId());    
            
            // Встановлення created_by_user_id (використовує ID)
            ps.setObject(12, tx.getCreatedBy()!=null?tx.getCreatedBy().getId():null);    
            
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if(keys.next()) tx.setId(keys.getLong(1));
            }

            return tx;
        }    
    }

    // =======================================================
    // 6. update
    // =======================================================
    public Transaction update(Transaction originalTx, Transaction updatedTx, Connection c) throws SQLException {
          if (updatedTx.getId() == null || updatedTx.getUser() == null || originalTx == null || updatedTx.getBudgetId() == null) {
              throw new IllegalArgumentException("Транзакція, оригінальна транзакція, користувач або контекст бюджету не визначені для оновлення.");
          }

          updatedTx.setOriginalAccount(originalTx.getAccount());
          updatedTx.setOriginalAmount(originalTx.getAmount());
          updatedTx.setOriginalType(originalTx.getType());

          // created_by_user_id НЕ ОНОВЛЮЄМО, оскільки він встановлюється лише при створенні
          String sql = "UPDATE transactions SET amount=?, type=?, description=?, created_at=?, category_id=?, account_id=?, currency=?, template_id=?, trans_date=?, budget_id=? WHERE id=? AND budget_id=?";
          try (PreparedStatement ps = c.prepareStatement(sql)) {
              ps.setDouble(1, updatedTx.getAmount());
              ps.setString(2, updatedTx.getType());
              ps.setString(3, updatedTx.getDescription());
              ps.setTimestamp(4, Timestamp.valueOf(updatedTx.getCreatedAt()));
              ps.setObject(5, updatedTx.getCategory() != null ? updatedTx.getCategory().getId() : null);
              ps.setObject(6, updatedTx.getAccount() != null ? updatedTx.getAccount().getId() : null);
              ps.setString(7, updatedTx.getCurrency());
              ps.setObject(8, updatedTx.getTemplateId(), Types.BIGINT);
              ps.setObject(9, updatedTx.getTransDate() != null ? Date.valueOf(updatedTx.getTransDate()) : null, Types.DATE);
              
              ps.setObject(10, updatedTx.getBudgetId());    
              
              ps.setLong(11, updatedTx.getId());
              ps.setLong(12, updatedTx.getBudgetId());    
              
              int rowsAffected = ps.executeUpdate();
              if (rowsAffected == 0) {
                  throw new RuntimeException("Не вдалося оновити транзакцію ID " + updatedTx.getId() + ". Вона може не належати бюджету " + updatedTx.getBudgetId());
              }
          }
          return updatedTx;
    }

    public Transaction update(Transaction originalTx, Transaction updatedTx) {
          try (Connection c = Db.getConnection()) {
              c.setAutoCommit(true);
              return update(originalTx, updatedTx, c);
          } catch (SQLException e) {
              e.printStackTrace();
              throw new RuntimeException("Помилка БД при оновленні транзакції: " + e.getMessage());
          }
    }

    // =======================================================
    // 7. delete
    // =======================================================
    public void delete(Transaction tx, Connection c) throws SQLException {
          if (tx.getId() == null || tx.getUser() == null || tx.getBudgetId() == null) {
              throw new IllegalArgumentException("Транзакція або контекст бюджету не визначені для видалення.");
          }

          tx.setOriginalAccount(tx.getAccount());
          tx.setOriginalAmount(tx.getAmount());
          tx.setOriginalType(tx.getType());
          
          String sql = "DELETE FROM transactions WHERE id=? AND budget_id=?";
          try (PreparedStatement ps = c.prepareStatement(sql)) {
              ps.setLong(1, tx.getId());
              ps.setLong(2, tx.getBudgetId());    
              ps.executeUpdate();
          }
    }

    public void delete(Transaction tx) {
          try (Connection c = Db.getConnection()) {
              c.setAutoCommit(true);
              delete(tx, c);
          } catch (SQLException e) {
              e.printStackTrace();
              throw new RuntimeException("Помилка БД при видаленні транзакції: " + e.getMessage());
          }
    }


    // =======================================================
    // 8. Aggregate methods
    // =======================================================
    public List<Object[]> aggregateMonthlySummary(ReportParams params, Long budgetId) {
          String sql = "SELECT DATE_FORMAT(t.created_at, '%Y-%m') AS month_year, t.type, SUM(t.amount) AS total_amount, t.currency FROM transactions t WHERE t.budget_id = ? AND t.created_at BETWEEN ? AND ? AND t.type IN ('INCOME', 'EXPENSE') GROUP BY month_year, t.type, t.currency ORDER BY month_year, t.type";

          var rawDataList = new ArrayList<Object[]>();

          try (Connection conn = Db.getConnection();
               PreparedStatement ps = conn.prepareStatement(sql)) {

               ps.setLong(1, budgetId);    
               ps.setTimestamp(2, Timestamp.valueOf(params.getStartDate().atStartOfDay()));
               ps.setTimestamp(3, Timestamp.valueOf(params.getEndDate().plusDays(1).atStartOfDay().minusNanos(1)));

               try (ResultSet rs = ps.executeQuery()) {
                   while (rs.next()) {
                       rawDataList.add(new Object[]{
                           rs.getString("month_year"),
                           rs.getString("type"),
                           rs.getDouble("total_amount"),
                           rs.getString("currency")
                       });
                   }
               }
          } catch (SQLException e) {
               e.printStackTrace();
               throw new RuntimeException("Помилка БД при агрегації місячної динаміки: " + e.getMessage());
          }
          return rawDataList;
    }

    public List<Object[]> aggregateByCategorySummary(ReportParams params, Long budgetId) {
          String sql = "SELECT t.category_id, t.type, SUM(t.amount) AS total_amount, t.currency FROM transactions t WHERE t.budget_id = ? AND t.created_at BETWEEN ? AND ? AND t.type IN ('INCOME', 'EXPENSE') AND t.category_id IS NOT NULL GROUP BY t.category_id, t.type, t.currency";

          var rawDataList = new ArrayList<Object[]>();

          try (Connection conn = Db.getConnection();
               PreparedStatement ps = conn.prepareStatement(sql)) {

               ps.setLong(1, budgetId);    
               ps.setTimestamp(2, Timestamp.valueOf(params.getStartDate().atStartOfDay()));
               ps.setTimestamp(3, Timestamp.valueOf(params.getEndDate().plusDays(1).atStartOfDay().minusNanos(1)));

               try (ResultSet rs = ps.executeQuery()) {
                   while (rs.next()) {
                       rawDataList.add(new Object[]{
                           rs.getLong("category_id"),
                           rs.getString("type"),
                           rs.getDouble("total_amount"),
                           rs.getString("currency")
                       });
                   }
               }
          } catch (SQLException e) {
               e.printStackTrace();
               throw new RuntimeException("Помилка БД при агрегації за категоріями: " + e.getMessage());
          }
          return rawDataList;
    }
}
