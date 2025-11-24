package ua.kpi.personal.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import ua.kpi.personal.service.AuthService;
import ua.kpi.personal.model.User;
import ua.kpi.personal.state.ApplicationSession; 
import java.io.IOException; 
import javafx.stage.Stage; 
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene; 

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton; 
    @FXML private Button registerButton; 
    @FXML private Label messageLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize(){
        messageLabel.setText("Будь ласка, увійдіть або зареєструйтеся");
    }

    @FXML
    private void onLogin() {
        String u = usernameField.getText();
        String p = passwordField.getText();
        User user = authService.login(u,p);
        
        if (user != null) {
            
            ApplicationSession.getInstance().login(user); 

        } else {
            messageLabel.setText("Невірні облікові дані або користувача не знайдено");
        }
    }

    @FXML
    private void onRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Реєстрація"); 
            
        } catch(IOException ex){ 
            ex.printStackTrace(); 
            messageLabel.setText("Помилка відкриття вікна реєстрації"); 
        }
    }

    @FXML
    private void onForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/password_recovery.fxml"));
            Stage recoveryStage = new Stage();
            recoveryStage.setScene(new Scene(loader.load()));
            recoveryStage.setTitle("Відновлення пароля");
            recoveryStage.show();
            
        } catch (IOException ex) {
            ex.printStackTrace();
            System.err.println("Помилка відкриття вікна відновлення пароля: " + ex.getMessage());
        }
    }
}