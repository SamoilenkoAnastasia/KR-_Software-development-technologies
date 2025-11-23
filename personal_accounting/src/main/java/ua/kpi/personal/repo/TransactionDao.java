package ua.kpi.personal.repo;

import ua.kpi.personal.model.*;
import ua.kpi.personal.util.Db;
import ua.kpi.personal.processor.TransactionProcessor;
import ua.kpi.personal.model.analytics.ReportParams;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.sql.Date;
import java.sql.Types; // Додано для Types.BIGINT та Types.DATE

public class TransactionDao implements TransactionProcessor {

    private final CategoryDao categoryDao = new CategoryDao();
    private final AccountDao accountDao = new AccountDao();

    private final String SELECT_FIELDS = "t.id, t.amount, t.type, t.description, t.created_at, t.category_id, t.account_id, t.user_id, t.currency, t.template_id, t.trans_date";

    public List<Transaction> findByBudgetId(Long budgetId){
         return findByUserId(budgetId);
    }

    public List<Transaction> findTransactionsByDateRange(ReportParams params, Long userId) {
        var list = new ArrayList<Transaction>();

        categoryDao.findByUserId(userId);

        List<Account> allAccounts = accountDao.findByUserId(userId);
        String sql = "SELECT " + SELECT_FIELDS + " FROM transactions t WHERE t.user_id = ? AND t.created_at BETWEEN ? AND ? ORDER BY t.created_at DESC";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setTimestamp(2, Timestamp.valueOf(params.getStartDate().atStartOfDay()));
            ps.setTimestamp(3, Timestamp.valueOf(params.getEndDate().plusDays(1).atStartOfDay().minusNanos(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Transaction t = mapResultSetToTransaction(rs, allAccounts);
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Помилка БД при отриманні транзакцій за діапазоном: " + e.getMessage());
        }
        return list;
    }


    public List<Transaction> findByUserId(Long userId){
        var list = new ArrayList<Transaction>();

        categoryDao.findByUserId(userId);

        List<Account> allAccounts = accountDao.findByUserId(userId);
        try(Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT " + SELECT_FIELDS + " FROM transactions t WHERE t.user_id = ? ORDER BY t.created_at DESC")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()){
                    Transaction t = mapResultSetToTransaction(rs, allAccounts);
                    list.add(t);
                }
            }
        } catch(SQLException e){ e.printStackTrace(); }
        return list;
    }

    private Transaction mapResultSetToTransaction(ResultSet rs, List<Account> allAccounts) throws SQLException {
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
            t.setCategory(CategoryCache.getById(catId));
        }

        Long accId = rs.getLong("account_id");
        if (!rs.wasNull()) {
            t.setAccount(allAccounts.stream()
                             .filter(acc -> acc.getId().equals(accId))
                             .findFirst()
                             .orElse(null));
        }

        User u = new User();
        u.setId(rs.getLong("user_id"));
        t.setUser(u);

        t.setCurrency(rs.getString("currency"));

        return t;
    }

    @Override
    public Transaction create(Transaction tx){
        String sql = "INSERT INTO transactions (amount, type, description, created_at, category_id, account_id, user_id, currency, template_id, trans_date) VALUES (?,?,?,?,?,?,?,?,?,?)";

        try(Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setDouble(1, tx.getAmount());
            ps.setString(2, tx.getType());
            ps.setString(3, tx.getDescription());
            ps.setTimestamp(4, Timestamp.valueOf(tx.getCreatedAt()));
            ps.setObject(5, tx.getCategory()!=null?tx.getCategory().getId():null);
            ps.setObject(6, tx.getAccount()!=null?tx.getAccount().getId():null);
            ps.setObject(7, tx.getUser()!=null?tx.getUser().getId():null);
            ps.setString(8, tx.getCurrency());

            ps.setObject(9, tx.getTemplateId(), Types.BIGINT);
            ps.setObject(10, tx.getTransDate() != null ? Date.valueOf(tx.getTransDate()) : null, Types.DATE);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if(keys.next()) tx.setId(keys.getLong(1));
            }

            if(tx.getAccount()!=null && tx.getUser()!=null){
                Account acc = accountDao.findById(tx.getAccount().getId(), tx.getUser().getId());
                if (acc != null) {
                    double bal = acc.getBalance()==null?0.0:acc.getBalance();
                    if("EXPENSE".equalsIgnoreCase(tx.getType())) bal -= tx.getAmount();
                    else bal += tx.getAmount();
                    acc.setBalance(bal);
                    acc.setUser(tx.getUser());
                    accountDao.update(acc);
                }
            }
            return tx;
        } catch(SQLException e){
            e.printStackTrace();
            throw new RuntimeException("Помилка БД при збереженні транзакції: " + e.getMessage());
        }
    }

    @Override
    public Transaction update(Transaction originalTx, Transaction updatedTx) {
        if (updatedTx.getId() == null || updatedTx.getUser() == null || originalTx == null) {
            throw new IllegalArgumentException("Транзакція (оригінал або оновлена) або користувач не визначені для оновлення.");
        }

            updatedTx.setOriginalAccount(originalTx.getAccount());
            updatedTx.setOriginalAmount(originalTx.getAmount());
            updatedTx.setOriginalType(originalTx.getType());

        try (Connection c = Db.getConnection()) {
           c.setAutoCommit(false);

           revertBalanceChange(c, updatedTx);

           String sql = "UPDATE transactions SET amount=?, type=?, description=?, created_at=?, category_id=?, account_id=?, currency=?, template_id=?, trans_date=? WHERE id=? AND user_id=?";
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
               ps.setLong(10, updatedTx.getId());
               ps.setLong(11, updatedTx.getUser().getId());
               ps.executeUpdate();
           }

           applyBalanceChange(c, updatedTx);

           c.commit();
           return updatedTx;

        } catch (SQLException e) {
        e.printStackTrace();
        throw new RuntimeException("Помилка БД при оновленні транзакції: " + e.getMessage());
        }
    }

    @Override
    public void delete(Transaction tx) {
        if (tx.getId() == null || tx.getUser() == null) {
            throw new IllegalArgumentException("Транзакція або користувач не визначені для видалення.");
        }

        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false);

            tx.setOriginalAccount(tx.getAccount());
            tx.setOriginalAmount(tx.getAmount());
            tx.setOriginalType(tx.getType());
            revertBalanceChange(c, tx);

            String sql = "DELETE FROM transactions WHERE id=? AND user_id=?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, tx.getId());
                ps.setLong(2, tx.getUser().getId());
                ps.executeUpdate();
            }

            c.commit();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Помилка БД при видаленні транзакції: " + e.getMessage());
        }
    }

    @Override
    public void transferToGoal(Account sourceAccount, Goal targetGoal, double amount) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    private void revertBalanceChange(Connection c, Transaction tx) throws SQLException {
        Account originalAcc = tx.getOriginalAccount();
        Double originalAmount = tx.getOriginalAmount();
        String originalType = tx.getOriginalType();
        Long userId = tx.getUser().getId();

        if (originalAcc == null || originalAmount == null || originalType == null) return;

        Account accInDb = accountDao.findById(originalAcc.getId(), userId);
        if (accInDb == null) return;

        double currentBalance = accInDb.getBalance() != null ? accInDb.getBalance() : 0.0;

        if ("EXPENSE".equalsIgnoreCase(originalType)) {
            currentBalance += originalAmount;
        } else if ("INCOME".equalsIgnoreCase(originalType)) {
            currentBalance -= originalAmount;
        }

        accInDb.setBalance(currentBalance);
        accountDao.update(accInDb, c);
    }

    private void applyBalanceChange(Connection c, Transaction tx) throws SQLException {
        Account newAcc = tx.getAccount();
        Double newAmount = tx.getAmount();
        String newType = tx.getType();
        Long userId = tx.getUser().getId();

        if (newAcc == null || newAmount == null || newType == null) return;

        Account accInDb = accountDao.findById(newAcc.getId(), userId);
        if (accInDb == null) return;

        double currentBalance = accInDb.getBalance() != null ? accInDb.getBalance() : 0.0;

        if ("EXPENSE".equalsIgnoreCase(newType)) {
            currentBalance -= newAmount;
        } else if ("INCOME".equalsIgnoreCase(newType)) {
            currentBalance += newAmount;
        }

        accInDb.setBalance(currentBalance);
        accountDao.update(accInDb, c);
    }

    public List<Object[]> aggregateMonthlySummary(ReportParams params, Long userId) {

        // ВИКОРИСТОВУЄМО DATE_FORMAT для MySQL
        String sql = "SELECT " +
                     "DATE_FORMAT(t.created_at, '%Y-%m') AS month_year, " +
                     "t.type, " +
                     "SUM(t.amount) AS total_amount, " +
                     "t.currency " +
                     "FROM transactions t " +
                     "WHERE t.user_id = ? AND t.created_at BETWEEN ? AND ? AND t.type IN ('INCOME', 'EXPENSE') " +
                     "GROUP BY month_year, t.type, t.currency " +
                     "ORDER BY month_year, t.type";

        var rawDataList = new ArrayList<Object[]>();

        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
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


    public List<Object[]> aggregateByCategorySummary(ReportParams params, Long userId) {

        // ВИКОРИСТОВУЄМО DATE_FORMAT для MySQL
        String sql = "SELECT " +
                     "t.category_id, " +
                     "t.type, " +
                     "SUM(t.amount) AS total_amount, " +
                     "t.currency " +
                     "FROM transactions t " +
                     "WHERE t.user_id = ? AND t.created_at BETWEEN ? AND ? AND t.type IN ('INCOME', 'EXPENSE') AND t.category_id IS NOT NULL " +
                     "GROUP BY t.category_id, t.type, t.currency";

        var rawDataList = new ArrayList<Object[]>();

        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setTimestamp(2, Timestamp.valueOf(params.getStartDate().atStartOfDay()));
            ps.setTimestamp(3, Timestamp.valueOf(params.getEndDate().plusDays(1).atStartOfDay().minusNanos(1)));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rawDataList.add(new Object[]{
                        rs.getLong("category_id"),    // row[0]
                        rs.getString("type"),         // row[1]
                        rs.getDouble("total_amount"), // row[2]
                        rs.getString("currency")      // row[3]
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