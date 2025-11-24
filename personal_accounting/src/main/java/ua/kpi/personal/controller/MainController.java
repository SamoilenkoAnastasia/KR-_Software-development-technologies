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

import ua.kpi.personal.controller.DashboardViewController;

public class MainController {

    private final AccountDao accountDao = new AccountDao();
    private final GoalDao goalDao = new GoalDao();
    private final TransactionDao transactionDao = new TransactionDao();

    private final AccountService accountService;
    private final ReportingService reportingService;
    private final ExchangeRateService rateService = new ExchangeRateService();

    private final ApplicationSession session = ApplicationSession.getInstance();

    private DashboardViewController dashboardController;

    @FXML private Button dashboardBtn;
    @FXML private Button transactionsBtn;
    @FXML private Button categoriesBtn;
    @FXML private Button analyticsBtn;
    @FXML private Button goalsBtn;
    @FXML private Button budgetBtn;
    @FXML private Button logoutBtn;
    @FXML private Pane contentPane;

    public MainController() {
        this.accountService = new AccountService(accountDao, session);
        this.reportingService = new ReportingService(accountDao, goalDao, transactionDao);
    }

    @FXML
    private void initialize() {
        if (session.getMainController() == null) {
            session.setMainController(this);
        }
    }


    public void showInitialView() {
        onDashboard();

        updateViewForNewBudget();

        if (this.dashboardController != null) {
            System.out.println("MainController: Запуск оновлення даних дашборда після входу.");
            this.dashboardController.refreshData();
        }
    }


    public void updateViewForNewBudget() {
        String role = session.getCurrentBudgetAccessState().getDisplayRole();
        Long budgetId = session.getCurrentBudgetId();

        System.out.printf("MainController оновлює UI. Бюджет ID: %d, Роль: %s\n", budgetId, role);

        boolean canEdit = session.getCurrentBudgetAccessState().canEdit();
        transactionsBtn.setDisable(!canEdit);
        categoriesBtn.setDisable(!canEdit);
        goalsBtn.setDisable(!canEdit);

        if (dashboardController != null) {
            dashboardController.updateViewForNewBudget();
        }
    }

    private void loadView(String fxmlPath) {
        URL location = getClass().getResource(fxmlPath);

        if (location == null) {
            System.err.println("Помилка завантаження FXML: Ресурс не знайдено за шляхом: " + fxmlPath);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(location);

            loader.setControllerFactory(c -> {
                if (c.equals(DashboardViewController.class)) {
                    DashboardViewController controller = new DashboardViewController(accountService, reportingService, rateService);
                    this.dashboardController = controller;
                    return controller;
                }

                try {
                    return c.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Не вдалося створити контролер " + c.getName(), e);
                }
            });

            Node view = loader.load();

            contentPane.getChildren().setAll(view);

            if (view instanceof Pane) {
                ((Pane) view).prefWidthProperty().bind(contentPane.widthProperty());
                ((Pane) view).prefHeightProperty().bind(contentPane.heightProperty());
            }

            if (fxmlPath.equals("/fxml/dashboard_view.fxml") && this.dashboardController != null) {
                System.out.println("MainController: Завантажено Dashboard, викликаємо оновлення даних.");
                this.dashboardController.refreshData();
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

    @FXML public void onDashboard() { loadView("/fxml/dashboard_view.fxml"); }
    @FXML public void onTransactions() { loadView("/fxml/transactions.fxml"); }
    @FXML public void onCategories() { loadView("/fxml/categories.fxml"); }
    @FXML public void onGoals() { loadView("/fxml/goal_management_view.fxml"); }
    @FXML public void onReports() { loadView("/fxml/reports.fxml"); }
    @FXML public void onBudgetManagement() {loadView("/fxml/budget_management_view.fxml");}
}