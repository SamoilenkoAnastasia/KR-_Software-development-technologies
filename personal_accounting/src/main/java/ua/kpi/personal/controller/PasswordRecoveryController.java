package ua.kpi.personal.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ua.kpi.personal.model.User;
import ua.kpi.personal.service.PasswordResetService;
import java.io.IOException;

public class PasswordRecoveryController {

    @FXML private TextField emailField;
    @FXML private Label messageLabel;
    @FXML private TextField tokenDisplayField; 

    private final PasswordResetService resetService = new PasswordResetService();

    @FXML
    private void onSendResetLink() {
        String email = emailField.getText();

        if (email.isEmpty()) {
            messageLabel.setText("Введіть email.");
            return;
        }

        User user = resetService.initiatePasswordReset(email); 
        
        if (user != null) {
            String token = user.getResetToken();
            tokenDisplayField.setText(token);
            messageLabel.setText("Ключ згенеровано. Скопіюйте його та перейдіть до Кроку 2.");
        } else {
            messageLabel.setText("Якщо обліковий запис існує, ключ згенеровано.");
        }
    }
    
    @FXML
    private void onOpenResetWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/password_reset_form.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            Stage stage = new Stage();
            stage.setTitle("Скидання Пароля");
            stage.setScene(scene);
            stage.show();
            
        } catch (IOException e) {
            System.err.println("Помилка відкриття вікна скидання пароля: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    private void onCancel() {
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.close();
    }
}