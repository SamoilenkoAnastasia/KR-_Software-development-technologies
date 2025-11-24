package ua.kpi.personal.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import ua.kpi.personal.service.AuthService;
import ua.kpi.personal.model.User;

import java.io.IOException;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField fullnameField;
    @FXML private TextField emailField; 
    @FXML private Label messageLabel;
    @FXML private Button createButton; 
    
    
    private final AuthService authService = new AuthService();

    @FXML
    private void onCreate() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String fullName = fullnameField.getText();
        String email = emailField.getText(); 
 
        if (username.isBlank() || password.isBlank() || email.isBlank()) { 
            messageLabel.setText("Необхідно вказати ім'я, пароль та email"); 
            return; 
        }

        User user = authService.register(username, password, fullName, email);
        
        if (user != null) {
            messageLabel.setText("Успішно зареєстровано. Будь ласка, увійдіть.");
            goToLoginScreen();
            
        } else {
            messageLabel.setText("Користувач вже існує або сталася помилка реєстрації");
        }
    }
    
    
    @FXML
    private void onBackToLogin() {
        goToLoginScreen();
    }
    
    
    private void goToLoginScreen() {
          try {
             FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
             
             
             Stage stage = (Stage) createButton.getScene().getWindow(); 
             
             Scene scene = new Scene(loader.load());
             
             stage.setTitle("Вхід"); 
             stage.setScene(scene);
             
         } catch(IOException ex){ 
              System.err.println("Помилка при завантаженні екрану входу: " + ex.getMessage());
              ex.printStackTrace(); 
         }
    }
}