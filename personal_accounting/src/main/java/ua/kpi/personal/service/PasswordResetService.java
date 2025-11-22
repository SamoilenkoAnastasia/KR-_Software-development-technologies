package ua.kpi.personal.service;

import ua.kpi.personal.model.User;
import ua.kpi.personal.repo.UserDao;
import java.time.LocalDateTime;
import java.util.UUID; 

public class PasswordResetService {
    
    private final UserDao userDao = new UserDao();
    private final AuthService authService = new AuthService();
    // Видаляємо: private final EmailService emailService = new EmailService();
    
    private static final int TOKEN_VALID_MINUTES = 30; 

    /**
     * Ініціює процес скидання пароля: генерує токен і зберігає його в БД.
     * @param email Email користувача.
     * @return User з токеном, якщо ініціація успішна, або null.
     */
    public User initiatePasswordReset(String email) {
        User user = userDao.findByEmail(email);
        
        if (user == null) {
            return null;
        }

        // 1. Генеруємо токен і час дії
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(TOKEN_VALID_MINUTES);

        user.setResetToken(token);
        user.setTokenExpiryDate(expiryDate);

        // 2. Зберігаємо токен і час дії
        if (userDao.saveResetToken(user)) {
            
            // 3. Логіка виводу токена в консоль (замість надсилання поштою)
            System.out.println("--- ЛОКАЛЬНЕ ТЕСТУВАННЯ ---");
            System.out.println("Згенеровано токен для користувача " + user.getUsername() + ":");
            System.out.println("ТОКЕН: " + token);
            System.out.println("---------------------------");
            
            return user; // Повертаємо користувача, щоб UI міг відобразити токен
        }
        return null;
    }

    /**
     * Скидає пароль після перевірки токена.
     * (Цей метод залишається незмінним, оскільки він містить безпекову логіку)
     */
    public boolean resetPassword(String token, String newPassword) {
        User user = userDao.findByResetToken(token);
        
        if (user == null) {
            return false;
        }

        if (user.getTokenExpiryDate() == null || user.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
            userDao.clearResetToken(user.getId()); 
            return false;
        }
        
        String newHashedPassword = authService.hashPassword(newPassword); 
        
        // Використовуємо метод, що оновлює пароль та очищає токен
        return userDao.updatePasswordAndClearToken(user.getId(), newHashedPassword);
    }
}