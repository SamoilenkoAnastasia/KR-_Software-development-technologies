package ua.kpi.personal.model;

import java.time.LocalDateTime;

public class User {
    private Long id;
    private String username;
    private String password; // Зберігає хеш пароля
    private String fullName;
    private String email; // Додаємо поле email
    
    // Нові поля для відновлення пароля
    private String resetToken;
    private LocalDateTime tokenExpiryDate; 

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    // Виправлений метод getEmail та новий setEmail
    public String getEmail() { return email; } 
    public void setEmail(String email) { this.email = email; }

    // Getters/Setters для токена
    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }
    public LocalDateTime getTokenExpiryDate() { return tokenExpiryDate; }
    public void setTokenExpiryDate(LocalDateTime tokenExpiryDate) { this.tokenExpiryDate = tokenExpiryDate; }
}