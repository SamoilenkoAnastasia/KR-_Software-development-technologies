package ua.kpi.personal.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ua.kpi.personal.service.PasswordResetService;

public class PasswordResetController {

    @FXML private TextField tokenField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    private final PasswordResetService resetService = new PasswordResetService();

    @FXML
    private void onChangePassword() {
        String token = tokenField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (token.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            statusLabel.setText("Будь ласка, заповніть усі поля.");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            statusLabel.setText("Помилка: Паролі не співпадають.");
            return;
        }

        // Виклик логіки скидання пароля
        if (resetService.resetPassword(token, newPassword)) {
            statusLabel.setText("Пароль успішно змінено! Тепер ви можете увійти.");
            statusLabel.setStyle("-fx-text-fill: green;");
        } else {
            statusLabel.setText("Помилка: Невірний або прострочений ключ відновлення.");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }
}