package ua.kpi.personal.repo;

import ua.kpi.personal.model.Category;
import ua.kpi.personal.util.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import ua.kpi.personal.repo.CategoryCache; // Додано імпорт кешу

public class CategoryDao {
    
    // --- Допоміжний приватний метод для мапінгу ResultSet на Category ---
    private Category mapResultSetToCategory(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        
        // Обробка user_id (може бути NULL)
        Long uid = rs.getLong("user_id");
        if (rs.wasNull()) uid = null;
        
        // Обробка parent_id (може бути NULL)
        Long parentId = rs.getLong("parent_id");
        if (rs.wasNull()) parentId = null;

        return new Category(
            id,
            uid,
            rs.getString("name"),
            rs.getString("type"),
            parentId,
            rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
    
    /**
     * Повертає всі категорії для користувача (user_id = ?) та всі системні категорії (user_id IS NULL).
     * Після завантаження оновлює CategoryCache.
     */
    public List<Category> findByUserId(Long userId){
        // Очищення кешу перед заповненням, щоб уникнути змішування даних користувачів.
        CategoryCache.clearCache(); 
        
        var list = new ArrayList<Category>();
        String sql = "SELECT id, user_id, name, type, parent_id, created_at FROM categories WHERE user_id = ? OR user_id IS NULL ORDER BY type DESC, name";
        
        try(Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)) { 
            
            ps.setLong(1, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()){
                    list.add(mapResultSetToCategory(rs));
                }
            }
        } catch(SQLException e){ 
            System.err.println("Error finding categories for user " + userId + ": " + e.getMessage());
        }
        
        // *** ФІКС: ЗБЕРЕЖЕННЯ ЗАВАНТАЖЕНИХ ДАНИХ У КЕШ ***
        CategoryCache.updateCache(list);
        
        return list;
    }

    /**
     * Повертає всі системні категорії (user_id IS NULL).
     */
    public List<Category> findSystemCategories(){
        var list = new ArrayList<Category>();
        String sql = "SELECT id, user_id, name, type, parent_id, created_at FROM categories WHERE user_id IS NULL ORDER BY id";
        
        try(Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)) {
            
            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()){
                    list.add(mapResultSetToCategory(rs));
                }
            }
        } catch(SQLException e){ 
            System.err.println("Error finding system categories: " + e.getMessage());
        }
        
        return list;
    }
    
    
    
    public Category create(Category category){
        String sql = "INSERT INTO categories (user_id, name, type, parent_id, created_at) VALUES (?,?,?,?,?)";
        
        try(Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { 
            
            
            ps.setObject(1, category.getUserId(), JDBCType.BIGINT);
            ps.setString(2, category.getName()); 
            ps.setString(3, category.getType()); 
            ps.setObject(4, category.getParentId(), JDBCType.BIGINT);
            ps.setTimestamp(5, Timestamp.valueOf(category.getCreatedAt())); 
            
            ps.executeUpdate();
            
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if(keys.next()) {
                    
                    Category created = category.withId(keys.getLong(1)); 
                    // Можливо, варто додати нову категорію до кешу
                    CategoryCache.put(created); 
                    return created;
                }
            }
            return category; 
        } catch(SQLException e){ 
            System.err.println("Error creating category: " + e.getMessage());
            return null; 
        }
    }
    
    
    public boolean update(Category category) {
        
        String sql = "UPDATE categories SET name = ?, type = ?, parent_id = ? WHERE id = ? AND user_id = ?";
        
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, category.getName());
            ps.setString(2, category.getType());
            ps.setObject(3, category.getParentId());
            ps.setLong(4, category.getId());
            
            
            if (category.getUserId() == null) {
                System.err.println("Attempt to update system category without user_id is blocked.");
                return false; 
            }
            ps.setLong(5, category.getUserId());

            int rowsAffected = ps.executeUpdate();
             // Оновлюємо кеш при успішному оновленні
             if (rowsAffected > 0) CategoryCache.put(category);
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating category: " + e.getMessage());
            return false;
        }
    }

    
    public boolean delete(Long id) {
        
        String sql = "DELETE FROM categories WHERE id = ?"; 
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);
            int rowsAffected = ps.executeUpdate();

             // Видаляємо з кешу при успішному видаленні
             if (rowsAffected > 0) CategoryCache.remove(id);
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting category: " + e.getMessage());
            return false;
        }
    }
    
 
    public Optional<Category> findById(Long id) { 
        String sql = "SELECT id, user_id, name, type, parent_id, created_at FROM categories WHERE id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Category cat = mapResultSetToCategory(rs);
                     // Якщо знайшли, додаємо в кеш
                     CategoryCache.put(cat); 
                    return Optional.of(cat);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding category by ID: " + e.getMessage());
        }
        return Optional.empty(); 
    }
}