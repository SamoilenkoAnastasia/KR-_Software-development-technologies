package ua.kpi.personal.service;

import ua.kpi.personal.model.User;
import ua.kpi.personal.repo.UserDao;
import java.time.LocalDateTime;
import java.util.UUID; 

public class PasswordResetService {
    
    private final UserDao userDao = new UserDao();
    private final AuthService authService = new AuthService();
    
    private static final int TOKEN_VALID_MINUTES = 30; 

    public User initiatePasswordReset(String email) {
        User user = userDao.findByEmail(email);
        
        if (user == null) {
            return null;
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(TOKEN_VALID_MINUTES);

        user.setResetToken(token);
        user.setTokenExpiryDate(expiryDate);

        if (userDao.saveResetToken(user)) {
           
            System.out.println("--- ЛОКАЛЬНЕ ТЕСТУВАННЯ ---");
            System.out.println("Згенеровано токен для користувача " + user.getUsername() + ":");
            System.out.println("ТОКЕН: " + token);
            System.out.println("---------------------------");
            
            return user; 
        }
        return null;
    }

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
        
        return userDao.updatePasswordAndClearToken(user.getId(), newHashedPassword);
    }
}