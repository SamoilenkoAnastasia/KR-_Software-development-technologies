package ua.kpi.personal.state;

import ua.kpi.personal.model.User;
import ua.kpi.personal.controller.MainController;
import ua.kpi.personal.repo.TemplateDao;
import ua.kpi.personal.repo.TransactionDao;
import ua.kpi.personal.service.ExchangeRateService;
import ua.kpi.personal.service.TemplateSchedulerService;
import ua.kpi.personal.processor.TransactionProcessor;
import ua.kpi.personal.processor.BalanceCheckDecorator;
import ua.kpi.personal.processor.CurrencyDecorator;

import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import java.io.IOException;

public class ApplicationSession {
    
    private static ApplicationSession instance;
    
    private SessionState currentState;
    private User currentUser;
    private Stage primaryStage;
    private MainController mainController;
    
    // ? НОВІ ПОЛЯ ДЛЯ СЕРВІСІВ
    private TemplateSchedulerService schedulerService;
    private final TemplateDao templateDao = new TemplateDao();
    private final TransactionDao transactionDao = new TransactionDao();
    
    private ApplicationSession(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.currentState = new LoggedOutState();
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
        // У цьому місці LoggedOutState має викликати changeState(new LoggedInState())
        currentState.handleLogin(this, user);
        
        // ? КРОК 1: Запуск планувальника після успішного входу
        if (this.currentUser != null && this.currentUser.getId() != null) {
            initializeSchedulerAndRunChecks();
        } else if (this.currentUser != null) {
            System.err.println("Помилка: Користувач увійшов, але не має ID. Планувальник не запущено.");
        }
    }
    
    public void logout() {
        // Очищаємо всі дані сесії
        this.currentUser = null;
        this.mainController = null;
        this.schedulerService = null; // Очищаємо планувальник
        currentState.handleLogout(this);
    }
    
    // ----------------------------------------------------------------------------------
    // ? НОВИЙ МЕТОД: ІНІЦІАЛІЗАЦІЯ ПЛАНУВАЛЬНИКА
    // ----------------------------------------------------------------------------------
    /**
     * Ініціалізує TransactionProcessor з декораторами та запускає планувальник.
     * Забезпечує, що автоматичні транзакції проходять повну бізнес-логіку.
     */
    private void initializeSchedulerAndRunChecks() {
        System.out.println("Запуск ініціалізації планувальника для користувача ID: " + currentUser.getId());
        
        // 1. Створення ланцюжка TransactionProcessor (Core -> Currency -> Balance Check)
        
        // Базовий процесор: збереження в БД
        TransactionProcessor baseProcessor = transactionDao; 
        
        // Додаємо логіку конвертації валют
        ExchangeRateService rateService = new ExchangeRateService();
        TransactionProcessor currencyProcessor = new CurrencyDecorator(baseProcessor, rateService);
        
        // Додаємо логіку перевірки балансу
        TransactionProcessor fullProcessor = new BalanceCheckDecorator(currencyProcessor);
        
        // 2. Ініціалізація Планувальника
        this.schedulerService = new TemplateSchedulerService(templateDao, fullProcessor);
        
        // 3. Запуск перевірки регулярних транзакцій
        schedulerService.runScheduledChecks(currentUser.getId());
    }
    // ----------------------------------------------------------------------------------

    private void loadView() {
        String fxmlPath = null;
        try {
            fxmlPath = currentState.getFxmlView();
            
            // Скидаємо контролер перед завантаженням нової сцени (крім MainController)
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

    
    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }
    
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    
    public MainController getController() {
        return this.mainController;
    }
}