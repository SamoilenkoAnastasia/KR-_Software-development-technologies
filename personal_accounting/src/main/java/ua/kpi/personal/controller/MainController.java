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
import java.net.URL;

public class MainController {

    // UI Елементи, прив'язані до root_view.fxml
    @FXML private Button dashboardBtn;
    @FXML private Button transactionsBtn;
    @FXML private Button categoriesBtn;
    @FXML private Button accountsBtn;    
    @FXML private Button analyticsBtn;    
    @FXML private Button goalsBtn;
    @FXML private Button budgetBtn;    
    @FXML private Button logoutBtn;
    
    @FXML private Pane contentPane; // Головний контейнер для фрагментів

    private final ApplicationSession session = ApplicationSession.getInstance();

    @FXML
    private void initialize() {
       
        if (session.getMainController() == null) {
            session.setMainController(this);
        }
    }
    
    
    public void showInitialView() {
        onDashboard();    
        updateViewForNewBudget();
    }
    
   
    public void updateViewForNewBudget() {
        String role = session.getCurrentBudgetAccessState().getDisplayRole();
        Long budgetId = session.getCurrentBudgetId();
        
        System.out.printf("? MainController оновлює UI. Бюджет ID: %d, Роль: %s\n", budgetId, role);
        
        // Логіка приховування/показу кнопок
        boolean canEdit = session.getCurrentBudgetAccessState().canEdit();
        transactionsBtn.setDisable(!canEdit);
        accountsBtn.setDisable(!canEdit);
        categoriesBtn.setDisable(!canEdit);
        goalsBtn.setDisable(!canEdit);
    }

    private void loadView(String fxmlPath) {
        URL location = getClass().getResource(fxmlPath);

        if (location == null) {
            System.err.println("? Помилка завантаження FXML: Ресурс не знайдено за шляхом: " + fxmlPath);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(location);
            Node view = loader.load();
            
            contentPane.getChildren().setAll(view);
            
            // Встановлюємо розміри
            if (view instanceof Pane) {
                ((Pane) view).prefWidthProperty().bind(contentPane.widthProperty());
                ((Pane) view).prefHeightProperty().bind(contentPane.heightProperty());
            }

        } catch (IOException e) {
            System.err.println("Не вдалося завантажити FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }
    
    @FXML
    private void onLogout() {
      
        ApplicationSession.getInstance().logout();
        
    }

    @FXML public void onDashboard() { loadView("/fxml/dashboard_view.fxml"); }
    @FXML public void onTransactions() { loadView("/fxml/transactions.fxml"); }
    @FXML public void onAccounts() { loadView("/fxml/accounts.fxml"); }    
    @FXML public void onCategories() { loadView("/fxml/categories.fxml"); }
    @FXML public void onGoals() { loadView("/fxml/goal_management_view.fxml"); }    
    @FXML public void onReports() { loadView("/fxml/reports.fxml"); }

    @FXML public void onBudgetManagement() {
        loadView("/fxml/budget_management_view.fxml");
    }
}