package ua.kpi.personal.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import ua.kpi.personal.repo.AccountDao;
import ua.kpi.personal.repo.GoalDao;
import ua.kpi.personal.repo.TransactionDao;
import ua.kpi.personal.service.AccountService;
import ua.kpi.personal.service.ExchangeRateService;
import ua.kpi.personal.service.ReportingService;
import ua.kpi.personal.state.ApplicationSession;

import java.io.IOException;
import java.net.URL;

public class MainController {

    // 1. DAO (для створення сервісів)
    private final AccountDao accountDao = new AccountDao();
    private final GoalDao goalDao = new GoalDao();
    private final TransactionDao transactionDao = new TransactionDao();
    
    // 2. СЕРВІСИ (Ін'єктуються в DashboardViewController)
    private final AccountService accountService;
    private final ReportingService reportingService;
    private final ExchangeRateService rateService = new ExchangeRateService();

    // 3. Сесія
    private final ApplicationSession session = ApplicationSession.getInstance();
    
    // 4. Посилання на DashboardController для оновлення
    private DashboardViewController dashboardController; 

    // UI Елементи
    @FXML private Button dashboardBtn;
    @FXML private Button transactionsBtn;
    @FXML private Button categoriesBtn;    
    @FXML private Button analyticsBtn;    
    @FXML private Button goalsBtn;
    @FXML private Button budgetBtn;    
    @FXML private Button logoutBtn;
    @FXML private Pane contentPane; 

    public MainController() {
        // Ініціалізація сервісів, використовуючи DAO та Сесію
        this.accountService = new AccountService(accountDao, session);
        this.reportingService = new ReportingService(accountDao, goalDao, transactionDao);
    }

    @FXML
    private void initialize() {
        if (session.getMainController() == null) {
            session.setMainController(this);
        }
        // Встановлення DashboardView як початкового
        showInitialView();
    }
    
    
    public void showInitialView() {
        onDashboard();    
        updateViewForNewBudget(); // Оновлюємо UI після завантаження
    }
    
    
    public void updateViewForNewBudget() {
        String role = session.getCurrentBudgetAccessState().getDisplayRole();
        Long budgetId = session.getCurrentBudgetId();
        
        System.out.printf("? MainController оновлює UI. Бюджет ID: %d, Роль: %s\n", budgetId, role);
        
        // Логіка приховування/показу кнопок
        boolean canEdit = session.getCurrentBudgetAccessState().canEdit();
        transactionsBtn.setDisable(!canEdit);
        categoriesBtn.setDisable(!canEdit);
        goalsBtn.setDisable(!canEdit);

        // Оновлюємо Dashboard, якщо він завантажений
        if (dashboardController != null) {
            dashboardController.updateViewForNewBudget();
        }
    }

    /**
     * Завантажує FXML та використовує ControllerFactory для DI.
     * @param fxmlPath Шлях до FXML файлу.
     */
    private void loadView(String fxmlPath) {
        URL location = getClass().getResource(fxmlPath);

        if (location == null) {
            System.err.println("? Помилка завантаження FXML: Ресурс не знайдено за шляхом: " + fxmlPath);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(location);
            
            // !!! КЛЮЧОВЕ ВИПРАВЛЕННЯ: ControllerFactory для DI !!!
            loader.setControllerFactory(c -> {
                if (c.equals(DashboardViewController.class)) {
                    // DI для DashboardViewController
                    DashboardViewController controller = new DashboardViewController(accountService, reportingService, rateService);
                    this.dashboardController = controller; // Зберігаємо посилання
                    return controller;
                } 
                // Якщо потрібно додати DI для інших контролерів (наприклад, TransactionsController):
                // if (c.equals(TransactionsController.class)) {
                //    return new TransactionsController(accountService, transactionDao);
                // }
                
                // Стандартний виклик конструктора без аргументів для інших контролерів
                try {
                    return c.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Не вдалося створити контролер " + c.getName(), e);
                }
            });

            Node view = loader.load();
            
            // Очищаємо та встановлюємо новий вміст
            contentPane.getChildren().setAll(view);
            
            // Встановлюємо прив'язку розмірів (правильно, для адаптивності)
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
        session.logout();
    }

    // --- Методи навігації ---
    @FXML public void onDashboard() { loadView("/fxml/dashboard_view.fxml"); }
    @FXML public void onTransactions() { loadView("/fxml/transactions.fxml"); }    
    @FXML public void onCategories() { loadView("/fxml/categories.fxml"); }
    @FXML public void onGoals() { loadView("/fxml/goal_management_view.fxml"); }    
    @FXML public void onReports() { loadView("/fxml/reports.fxml"); }
    @FXML public void onBudgetManagement() {
        loadView("/fxml/budget_management_view.fxml");
    }
}