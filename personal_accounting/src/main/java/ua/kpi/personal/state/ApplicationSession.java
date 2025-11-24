package ua.kpi.personal.state;

import ua.kpi.personal.model.User;
import ua.kpi.personal.controller.MainController;
import ua.kpi.personal.state.SessionState; // ДОДАНО: Для коректного використання
import ua.kpi.personal.state.BudgetAccessState;
import ua.kpi.personal.state.LoggedOutState;
import ua.kpi.personal.state.NoAccessState;
import ua.kpi.personal.repo.BudgetAccessDao;
import ua.kpi.personal.repo.SharedBudgetDao;
import ua.kpi.personal.service.BudgetAccessService;

import ua.kpi.personal.repo.TemplateDao;
import ua.kpi.personal.repo.TransactionDao;
import ua.kpi.personal.repo.AccountDao;
import ua.kpi.personal.service.TransactionService; 
import ua.kpi.personal.service.AccountService;
import ua.kpi.personal.service.ExchangeRateService;
import ua.kpi.personal.service.TemplateSchedulerService;

import ua.kpi.personal.processor.TransactionProcessor;
import ua.kpi.personal.processor.JdbcTransactionProcessor;
import ua.kpi.personal.processor.BalanceCheckDecorator;
import ua.kpi.personal.processor.CurrencyDecorator;

import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import java.io.IOException;
import java.time.LocalDateTime; 
import ua.kpi.personal.model.Transaction; 

public class ApplicationSession {

    private static ApplicationSession instance;

    private SessionState currentState;
    private User currentUser;
    private Stage primaryStage;
    private MainController mainController;

    private Long currentBudgetId;
    private BudgetAccessState currentBudgetAccessState;

    // --- DAO Layer ---
    private final TemplateDao templateDao = new TemplateDao();
    private final TransactionDao transactionDao = new TransactionDao();
    private final SharedBudgetDao sharedBudgetDao = new SharedBudgetDao();
    private final BudgetAccessDao budgetAccessDao = new BudgetAccessDao();
    private final AccountDao accountDao = new AccountDao();
    
    // --- Service Layer ---
    private TemplateSchedulerService schedulerService;
    private final BudgetAccessService budgetAccessService;
    private final TransactionService transactionService;
    private final AccountService accountService;

    private ApplicationSession(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.currentState = new LoggedOutState();

        this.budgetAccessService = new BudgetAccessService(budgetAccessDao, sharedBudgetDao, this);
        this.accountService = new AccountService(accountDao, this);
        
        // 2. ІНІЦІАЛІЗАЦІЯ TransactionService
        // 1. Створюємо базовий процесор
        TransactionProcessor baseProcessor = new JdbcTransactionProcessor(transactionDao, accountDao, accountService);
        
        // 2. Створюємо декоратори
        ExchangeRateService rateService = new ExchangeRateService();
        TransactionProcessor currencyProcessor = new CurrencyDecorator(baseProcessor, rateService);
        
        // !!! ВИПРАВЛЕНО РЯДОК 71 !!!: Передаємо AccountDao та TransactionDao до BalanceCheckDecorator
        TransactionProcessor fullProcessor = new BalanceCheckDecorator(currencyProcessor, accountDao, transactionDao);
        
        // 3. Передаємо DAO та ПОВНИЙ процесор у TransactionService
        this.transactionService = new TransactionService(transactionDao, fullProcessor, this);

        this.currentBudgetId = null;
        this.currentBudgetAccessState = new NoAccessState();

        loadView();
    }

    public static void initialize(Stage stage) {
        if (instance != null) {
            throw new IllegalStateException("ApplicationSession вже ініціалізовано.");
        }
        instance = new ApplicationSession(stage);
    }

    public static ApplicationSession getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ApplicationSession не ініціалізовано. Викличте initialize(Stage) у методі start().");
        }
        return instance;
    }

    public void changeState(SessionState newState) {
        this.currentState = newState;
        loadView();
    }

    public void login(User user) {
        currentState.handleLogin(this, user);

        if (this.currentUser != null && this.currentUser.getId() != null) {
            budgetAccessService.switchActiveBudget(currentUser.getId());
            initializeSchedulerAndRunChecks();
        } else if (this.currentUser != null) {
            System.err.println("Помилка: Користувач увійшов, але не має ID. Планувальник не запущено.");
        }
    }

    public void logout() {

        if (this.schedulerService != null) {
            this.schedulerService.stopScheduler();
            this.schedulerService = null;
        }

        this.currentUser = null;
        this.mainController = null;

        this.currentBudgetId = null;
        this.currentBudgetAccessState = new NoAccessState();

        currentState.handleLogout(this);
    }

    private void initializeSchedulerAndRunChecks() {
        System.out.println("Запуск ініціалізації планувальника для користувача ID: " + currentUser.getId());

        this.schedulerService = new TemplateSchedulerService(templateDao, transactionService.getTransactionProcessor());

        schedulerService.runScheduledChecks(currentUser.getId());
    }

    private void loadView() {
        String fxmlPath = null;
        try {
            fxmlPath = currentState.getFxmlView();

            if (fxmlPath.equals("/fxml/login.fxml")) {
                this.mainController = null;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load());

            if (fxmlPath.equals("/fxml/login.fxml")) {
                primaryStage.setTitle("Вхід / Реєстрація");
                primaryStage.setResizable(false);
                primaryStage.setMaximized(false);
            } else if (fxmlPath.equals("/fxml/root_view.fxml")) {
                primaryStage.setTitle("Персональний фінансовий облік");

                primaryStage.setResizable(true);
                primaryStage.setMaximized(true);

                MainController newMainController = loader.getController();
                if (newMainController != null) {
                    this.mainController = newMainController;
                    newMainController.showInitialView();
                }
            }

            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Помилка завантаження FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }

    public Long getCurrentBudgetId() {
        return currentBudgetId;
    }

    public void setCurrentBudgetId(Long currentBudgetId) {
        this.currentBudgetId = currentBudgetId;
    }

    public BudgetAccessState getCurrentBudgetAccessState() {
        if (currentBudgetAccessState == null) {
            return new NoAccessState();
        }
        return currentBudgetAccessState;
    }

    public void setCurrentBudgetAccessState(BudgetAccessState currentBudgetAccessState) {
        this.currentBudgetAccessState = currentBudgetAccessState;

        if (this.mainController != null) {
            this.mainController.updateViewForNewBudget();
        }
    }

    public MainController getMainController() {
        return this.mainController;
    }

    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    public BudgetAccessService getBudgetAccessService() {
        return budgetAccessService;
    }

    public TransactionService getTransactionService() {
        return transactionService;
    }

    public AccountService getAccountService() {
        return accountService;
    }
    
    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public BudgetAccessDao getBudgetAccessDao() {
        return budgetAccessDao;
    }

    public SharedBudgetDao getSharedBudgetDao() {
        return sharedBudgetDao;
    }
    
    public AccountDao getAccountDao() {
        return accountDao;
    }
    
    // ДОДАНО: Гетер для TransactionDao, може бути корисний для тестів або інших компонентів.
    public TransactionDao getTransactionDao() {
        return transactionDao;
    }
}
