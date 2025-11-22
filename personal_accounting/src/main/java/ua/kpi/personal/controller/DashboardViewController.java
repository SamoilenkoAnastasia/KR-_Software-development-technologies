package ua.kpi.personal.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ua.kpi.personal.model.User;
import ua.kpi.personal.repo.AccountDao;
import ua.kpi.personal.repo.GoalDao;
import ua.kpi.personal.repo.TransactionDao;
import ua.kpi.personal.service.ExchangeRateService;
import ua.kpi.personal.service.ReportingService;
import ua.kpi.personal.state.ApplicationSession;
import ua.kpi.personal.model.Account;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DashboardViewController {
    
    // UI Елементи
    @FXML private Label welcomeLabel;
    @FXML private Label netWorthLabel;
    
    @FXML private VBox accountsContainer;
    @FXML private TableView<Account> accountsTable;
    @FXML private TableColumn<Account, String> nameCol;
    @FXML private TableColumn<Account, String> typeCol;
    @FXML private TableColumn<Account, String> currencyCol;
    @FXML private TableColumn<Account, Double> balanceCol;

    @FXML private Button addAccountBtn;
    @FXML private Button exportBtn;
    
    @FXML private Label incomeLabel;
    @FXML private Label expenseLabel;
    @FXML private PieChart categoryChart; // Залишено, але не використовується в логіці
    
    @FXML private VBox exchangeRatesContainer; // ? Доданий контейнер для курсів

    // Сервіси та Дані
    private final AccountDao accountDao = new AccountDao();
    private final GoalDao goalDao = new GoalDao();
    private final TransactionDao transactionDao = new TransactionDao();
    private final ExchangeRateService rateService = new ExchangeRateService(); // ? Сервіс курсів
    private ReportingService reportingService; 
    private User user;

    /**
     * Обробник події для кнопки "Експорт Звіту".
     */
    @FXML
    public void onOpenReportDialog() {
        MainController mainController = ApplicationSession.getInstance().getController();
        if (mainController != null) {
            mainController.onReports();
        } else {
            System.err.println("MainController недоступний для навігації.");
        }
    }
    
    @FXML
    public void initialize() {
        this.user = ApplicationSession.getInstance().getCurrentUser();
        if (this.user == null) return;
        
        this.reportingService = new ReportingService(accountDao, goalDao, transactionDao);
        
        setupAccountTable();
        
        String userName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        welcomeLabel.setText("Ласкаво просимо, " + userName + "!");
        
        loadAccountsData();
        loadStatistics();
        loadExchangeRatesAsync(); // ? Завантаження курсів
    }
    
    /**
     * Встановлює зв'язок між колонками TableView та полями моделі Account.
     */
    private void setupAccountTable() {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        currencyCol.setCellValueFactory(new PropertyValueFactory<>("currency"));
        balanceCol.setCellValueFactory(new PropertyValueFactory<>("balance"));
        balanceCol.setCellFactory(tc -> new TableCell<Account, Double>() {
            @Override
            protected void updateItem(Double balance, boolean empty) {
                super.updateItem(balance, empty);
                if (empty || balance == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", balance));
                }
            }
        });
    }

    private void loadAccountsData() {
        try {
            List<Account> accounts = accountDao.findByUserId(user.getId());
            accountsTable.setItems(FXCollections.observableArrayList(accounts));
        } catch (Exception e) {
            System.err.println("Помилка при завантаженні рахунків: " + e.getMessage());
        }
    }
    
    private void loadStatistics() {
        // --- 1. Загальний капітал ---
        double netWorth = reportingService.getTotalNetWorth(user);
        netWorthLabel.setText(String.format("%.2f UAH", netWorth));
        
        // --- 2. Місячний звіт ---
        Map<String, Double> summary = reportingService.getMonthlySummary(user);
        
        double totalIncome = summary.getOrDefault("Income", 0.0);
        double totalExpense = summary.getOrDefault("Expense", 0.0);
        
        incomeLabel.setText(String.format("%.2f UAH", totalIncome));
        expenseLabel.setText(String.format("%.2f UAH", totalExpense));

        // --- 3. Витрати за категоріями (логіка PieChart видалена) ---
    }
    
    /**
     * Асинхронно завантажує курси валют і оновлює UI.
     */
    private void loadExchangeRatesAsync() {
        if (exchangeRatesContainer == null) {
            System.err.println("ExchangeRatesContainer не підключено у FXML!");
            return;
        }
        
        exchangeRatesContainer.getChildren().clear();
        exchangeRatesContainer.getChildren().add(new Label("Завантаження курсів..."));
        
        // Виконуємо запит в окремому потоці
        CompletableFuture.supplyAsync(rateService::getRates)
            .thenAccept(rates -> {
                // Оновлення UI в потоці JavaFX Application Thread
                javafx.application.Platform.runLater(() -> {
                    updateExchangeRatesUI(rates);
                });
            });
    }

    /**
     * Оновлює VBox з курсами валют.
     */
    private void updateExchangeRatesUI(Map<String, Double> rates) {
        exchangeRatesContainer.getChildren().clear(); 
        
        Label title = new Label("Актуальні курси НБУ:");
        title.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 5 0; -fx-font-size: 15px;");
        exchangeRatesContainer.getChildren().add(title);

        if (rates.isEmpty()) {
             exchangeRatesContainer.getChildren().add(new Label("Не вдалося отримати курси або немає даних (USD/EUR)."));
             return;
        }

        rates.forEach((currency, rate) -> {
            Label rateLabel = new Label(
                String.format("1 %s = %.2f UAH", currency, rate)
            );
            rateLabel.setStyle("-fx-font-size: 14px; -fx-padding: 2 0 2 0;");
            exchangeRatesContainer.getChildren().add(rateLabel);
        });
    }

    /**
     * Оновлює PieChart, відображаючи розподіл витрат за категоріями.
     * Залишено для компіляції, але не використовується.
     */
    private void updateCategoryChart(Map<String, Double> categoryExpenses) {
        // Логіка прибрана
    }

    @FXML
    private void onAddAccount() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/account_form_dialog.fxml"));
            AnchorPane root = loader.load(); 
            
            AccountsController controller = loader.getController();
            controller.setDashboardController(this); 
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Додати новий рахунок");
            dialogStage.initOwner(addAccountBtn.getScene().getWindow()); 
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.setResizable(false);

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            
        } catch (IOException e) {
            System.err.println("Не вдалося завантажити діалогове вікно додавання рахунку.");
            e.printStackTrace();
        }
    }
    
    // Допоміжна функція для повідомлень
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void refreshData() {
        loadAccountsData();
        loadStatistics();
        loadExchangeRatesAsync(); // ? Оновлюємо також курси
    }
}
