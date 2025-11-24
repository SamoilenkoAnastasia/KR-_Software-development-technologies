package ua.kpi.personal.repo;

import ua.kpi.personal.model.User;
import ua.kpi.personal.util.Db;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    private final String SELECT_FIELDS = "id, username, password_hash, full_name, email, reset_token, token_expiry_date";

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));

        try {
            u.setResetToken(rs.getString("reset_token"));
            Timestamp ts = rs.getTimestamp("token_expiry_date");
            if (ts != null) u.setTokenExpiryDate(ts.toLocalDateTime());
        } catch (SQLException e) {
        }

        return u;
    }

    public List<User> findByBudgetId(Long budgetId) {
        var list = new ArrayList<User>();
        if (budgetId == null) return list;

        String sql = "SELECT u." + SELECT_FIELDS.replace(",", ", u.") +
                     " FROM users u JOIN budget_access ba ON u.id = ba.user_id WHERE ba.budget_id = ?";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, budgetId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Помилка БД при отриманні користувачів за budgetId: " + e.getMessage());
        }
        return list;
    }

    public User findById(Long id){
        if (id == null) return null;

        String sql = "SELECT " + SELECT_FIELDS + " FROM users WHERE id = ?";

        try(Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()){
                    return mapResultSetToUser(rs);
                }
            }
        } catch(SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public User findByUsername(String username){
        String sql = "SELECT " + SELECT_FIELDS + " FROM users WHERE username = ?";

        try(Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()){
                    return mapResultSetToUser(rs);
                }
            }
        } catch(SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public User findByEmail(String email){
        String sql = "SELECT " + SELECT_FIELDS + " FROM users WHERE email = ?";

        try(Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()){
                    return mapResultSetToUser(rs);
                }
            }
        } catch(SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public boolean saveResetToken(User user) {
        String sql = "UPDATE users SET reset_token = ?, token_expiry_date = ? WHERE id = ?";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, user.getResetToken());
            ps.setTimestamp(2, user.getTokenExpiryDate() != null ? Timestamp.valueOf(user.getTokenExpiryDate()) : null);
            ps.setLong(3, user.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updatePasswordAndClearToken(Long userId, String newHashedPassword) {
        String sql = "UPDATE users SET password_hash = ?, reset_token = NULL, token_expiry_date = NULL WHERE id = ?";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, newHashedPassword);
            ps.setLong(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean clearResetToken(Long userId) {
          String sql = "UPDATE users SET reset_token = NULL, token_expiry_date = NULL WHERE id = ?";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, userId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public User create(User user){

        String sql = "INSERT INTO users (username, password_hash, full_name, email) VALUES (?,?,?,?)";

        try(Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {


            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());


            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.err.println("Creating user failed, no rows affected.");
                return null;
            }


            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }

            return user;

        } catch(SQLException e){
            e.printStackTrace();
            return null;
        }
    }

    public User findByResetToken(String token){
          String sql = "SELECT " + SELECT_FIELDS + " FROM users WHERE reset_token = ?";

        try(Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, token);

            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()){
                    return mapResultSetToUser(rs);
                }
            }
        } catch(SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public User findByUsernameOrEmail(String identifier) {
        String sql = "SELECT " + SELECT_FIELDS + " FROM users WHERE username = ? OR email = ?";

        try(Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, identifier);
            ps.setString(2, identifier);

            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()){
                    return mapResultSetToUser(rs);
                }
            }
        } catch(SQLException e){
            e.printStackTrace();
        }
        return null;
    }
}