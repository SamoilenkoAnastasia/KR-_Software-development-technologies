package ua.kpi.personal.repo;

import ua.kpi.personal.model.access.BudgetAccess;
import ua.kpi.personal.util.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BudgetAccessDao {
    
    // Константи ролей
    public static final String ROLE_OWNER = "OWNER"; 
    public static final String ROLE_EDITOR = "EDITOR";
    public static final String ROLE_VIEWER = "VIEWER";

    // --- МЕТОД МАПІНГУ ---

    private BudgetAccess mapResultSetToBudgetAccess(ResultSet rs) throws SQLException {
        BudgetAccess access = new BudgetAccess();
 
        access.setBudgetId(rs.getLong("budget_id"));
        access.setUserId(rs.getLong("user_id"));
        access.setAccessRole(rs.getString("role"));
        return access;
    }

    // --- CRUD ---

    /**
     * Створює або оновлює (UPSERT) право доступу для користувача до бюджету.
     */
    public BudgetAccess save(BudgetAccess access) {
        // !!! КОМЕНТАР: Переконайтеся, що на таблиці budget_access є UNIQUE індекс на (budget_id, user_id)
        String sql = "INSERT INTO budget_access (budget_id, user_id, role) VALUES (?, ?, ?)" +
                     " ON DUPLICATE KEY UPDATE role = ?";
        
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setLong(1, access.getBudgetId());
            ps.setLong(2, access.getUserId());
            ps.setString(3, access.getAccessRole());
            ps.setString(4, access.getAccessRole());
            
            int affectedRows = ps.executeUpdate();

            // Якщо ми щось оновили або вставили
            if (affectedRows > 0) {
                return access;
            }
        } catch (SQLException e) {
            // Виведення помилки в консоль є гарним тоном, але для DAO краще кинути виняток.
            System.err.println("Помилка UPSERT доступу до бюджету: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Помилка БД при збереженні доступу до бюджету.", e);
        }
        return null;
    }

    /**
     * Знаходить право доступу користувача до конкретного бюджету.
     * !!! КРИТИЧНО ВАЖЛИВА ЛОГІКА !!!: Обробляє приватний бюджет (budgetId == userId).
     */
    public BudgetAccess findAccessByBudgetAndUser(Long budgetId, Long userId) {
        if (budgetId == null || userId == null) return null;
        
        // ! ЛОГІКА ПРИВАТНОГО БЮДЖЕТУ: Якщо ID бюджету збігається з ID користувача, 
        // це його особистий, нешарений бюджет, і він завжди є OWNER.
        if (budgetId.equals(userId)) {
            BudgetAccess privateAccess = new BudgetAccess();
            privateAccess.setBudgetId(budgetId);
            privateAccess.setUserId(userId);
            privateAccess.setAccessRole(ROLE_OWNER); 
            return privateAccess;
        }
        
        String sql = "SELECT budget_id, user_id, role FROM budget_access WHERE budget_id = ? AND user_id = ?";
        
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setLong(1, budgetId);
            ps.setLong(2, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBudgetAccess(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка пошуку доступу до бюджету: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Помилка БД при пошуку доступу до бюджету.", e);
        }
        return null; // Повертаємо null, якщо доступ не знайдено (користувач не є членом спільного бюджету)
    }

    /**
     * Отримує список усіх членів, які мають доступ до спільного бюджету.
     */
    public List<BudgetAccess> findMembersByBudgetId(Long budgetId) {
        if (budgetId == null) return new ArrayList<>();
        List<BudgetAccess> members = new ArrayList<>();

        // !!! КОРЕКЦІЯ: ВИКЛЮЧЕННЯ ПРИВАТНОГО ДОСТУПУ З БД !!!
        // Якщо budgetId == userId, це приватний бюджет. Його член (сам власник) 
        // не зберігається у таблиці budget_access.
        // Ми повинні отримати всіх, хто є у budget_access, і, якщо budgetId != userId, 
        // додати власника. Ця логіка обробляється на рівні сервісу BudgetAccessService.
        
        // У DAO ми просто шукаємо записи.
        String sql = "SELECT budget_id, user_id, role FROM budget_access WHERE budget_id = ?";
        
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setLong(1, budgetId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(mapResultSetToBudgetAccess(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Помилка пошуку членів бюджету: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Помилка БД при пошуку членів бюджету.", e);
        }
        return members;
    }

    /**
     * Видаляє право доступу користувача до бюджету.
     */
    public boolean delete(Long budgetId, Long userId) {

        // !!! КОРЕКЦІЯ: Перевірка повинна бути на рівні сервісу !!!
        // У DAO ми просто виконуємо операцію, а сервіс вирішує, чи можна її виконати.
        
        if (budgetId == null || userId == null) return false;

        String sql = "DELETE FROM budget_access WHERE budget_id = ? AND user_id = ?";
        
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setLong(1, budgetId);
            ps.setLong(2, userId);
            
            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Помилка видалення доступу: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // --- ДОДАННЯ КРИТИЧНОГО МЕТОДУ ---

    /**
     * !!! КРИТИЧНО ВАЖЛИВО !!!
     * Видаляє ВСІ записи про доступ для даного бюджету. 
     * Цей метод повинен викликатися при видаленні самого бюджету, щоб уникнути
     * "залишкових" прав доступу.
     */
    public boolean deleteAllAccessByBudgetId(Long budgetId) {
        if (budgetId == null) return false;
        
        // !!! УВАГА: Не видаляйте приватний бюджет, якщо він не спільний, 
        // це має перевірятися сервісом. Тут ми просто очищуємо таблицю access.

        String sql = "DELETE FROM budget_access WHERE budget_id = ?";
        
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setLong(1, budgetId);
            
            int affectedRows = ps.executeUpdate();
            return affectedRows >= 0; // >= 0, тому що може бути 0 спільних користувачів
            
        } catch (SQLException e) {
            System.err.println("Помилка видалення всіх прав доступу для бюджету ID " + budgetId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}