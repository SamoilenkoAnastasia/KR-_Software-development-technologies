package ua.kpi.personal.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import ua.kpi.personal.state.ApplicationSession;

import java.io.IOException;

public class MainController {

    // UI Елементи, прив'язані до root_view.fxml
    @FXML private Button dashboardBtn;
    @FXML private Button transactionsBtn;
    @FXML private Button categoriesBtn;
    @FXML private Button accountsBtn; // ДОДАНО
    @FXML private Button analyticsBtn; // ДОДАНО
    @FXML private Button goalsBtn;
    @FXML private Button logoutBtn;
    
    @FXML private Pane contentPane; // Головний контейнер для фрагментів

    @FXML
    private void initialize() {
        // Ініціалізація відтепер відбувається через showInitialView(), 
        // що викликається з ApplicationSession.
    }
    
    /**
     * НОВИЙ ПУБЛІЧНИЙ МЕТОД: Викликається ApplicationSession після 
     * ін'єкції FXML-полів для безпечного завантаження першого фрагмента.
     */
    public void showInitialView() {
        loadView("/fxml/dashboard_view.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            
            contentPane.getChildren().setAll(view);
            
            // Встановлюємо розміри
            if (view instanceof Pane) {
                ((Pane) view).setPrefSize(contentPane.getWidth(), contentPane.getHeight());
            }

        } catch (IOException e) {
            System.err.println("Не вдалося завантажити FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }
    
    @FXML
    private void onLogout() {
        ApplicationSession.getInstance().logout();
        
        // Вихід: перемикаємо сцену на Login.fxml
        Stage currentStage = (Stage) logoutBtn.getScene().getWindow();
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene loginScene = new Scene(loader.load());
            
            currentStage.setScene(loginScene);
            currentStage.setTitle("Авторизація");
            
        } catch (IOException e) {
            System.err.println("Помилка завантаження сцени авторизації.");
            e.printStackTrace();
        }
    }

    // МЕТОДИ ПЕРЕХОДУ НА ФРАГМЕНТИ
    @FXML public void onDashboard() { loadView("/fxml/dashboard_view.fxml"); }
    @FXML public void onTransactions() { loadView("/fxml/transactions.fxml"); }
    @FXML public void onAccounts() { loadView("/fxml/accounts.fxml"); } // ДОДАНО
    @FXML public void onCategories() { loadView("/fxml/categories.fxml"); }
    @FXML public void onGoals() { loadView("/fxml/goal_management_view.fxml"); } 
    @FXML public void onReports() { loadView("/fxml/reports.fxml"); } // ВИКОРИСТОВУЄТЬСЯ ДЛЯ АНАЛІТИКИ
}
