package ua.kpi.personal.service;

import ua.kpi.personal.model.User;
import ua.kpi.personal.repo.UserDao;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class AuthService {
    private final UserDao userDao = new UserDao();

  
    public String hashPassword(String password) {
        if (password == null) return null;
        try {
           
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            
            e.printStackTrace();
            return password; 
        }
    }

    
    private boolean checkPassword(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        
        String hashedRawPassword = hashPassword(rawPassword);
        return hashedPassword.equals(hashedRawPassword);
    }


    public User login(String username, String password){
        User u = userDao.findByUsername(username);
        if(u==null) return null;
        
       
        if(checkPassword(password, u.getPassword())) {
            return u;
        }
        return null;
    }

    
    // НОВА виправлена версія у AuthService.java:
    public User register(String username, String password, String fullName, String email){
        User exists = userDao.findByUsername(username);
            if(exists!=null) return null;

        User u = new User();
        u.setUsername(username);

        // Хешування пароля (ми це вже виправили раніше)
        String hashedPassword = hashPassword(password);
        u.setPassword(hashedPassword); 

        u.setFullName(fullName);
        u.setEmail(email); // <--- ВАЖЛИВО: Встановлення email

        return userDao.create(u);
    }
}