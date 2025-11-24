package ua.kpi.personal.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ua.kpi.personal.model.Account;
import ua.kpi.personal.model.User;
import ua.kpi.personal.service.AccountService; 
import ua.kpi.personal.service.ExchangeRateService;
import ua.kpi.personal.service.ReportingService;
import ua.kpi.personal.state.ApplicationSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javafx.beans.property.SimpleStringProperty;

public class DashboardViewController {
    
    // UI Елементи
    @FXML private Label welcomeLabel;
    @FXML private Label netWorthLabel;
    @FXML private Label incomeLabel;
    @FXML private Label expenseLabel;
    @FXML private VBox exchangeRatesContainer; 
    @FXML private TableView<Account> accountsTable;
    @FXML private TableColumn<Account, String> nameCol;
    @FXML private TableColumn<Account, String> typeCol;
    @FXML private TableColumn<Account, String> currencyCol;
    @FXML private TableColumn<Account, Double> balanceCol;
    @FXML private Button addAccountBtn;
    
    // !!! НОВА КОЛОНКА !!!
    @FXML private TableColumn<Account, String> sharedCol; 

    // --- СЕРВІСИ (Ін'єктовані через конструктор) ---
    private final ApplicationSession session = ApplicationSession.getInstance();
    private final AccountService accountService;
    private final ReportingService reportingService;
    private final ExchangeRateService rateService; 

    private User user;
    private Long currentBudgetId;

    /**
     * Конструктор для ін'єкції залежностей (вимагає ControllerFactory).
     */
    public DashboardViewController(AccountService accountService, ReportingService reportingService, ExchangeRateService rateService) {
        this.accountService = accountService;
        this.reportingService = reportingService;
        this.rateService = rateService;
    }
    
    @FXML
    public void initialize() {
        this.user = session.getCurrentUser();
        this.currentBudgetId = session.getCurrentBudgetId(); 

        if (this.user == null || this.currentBudgetId == null) {
            welcomeLabel.setText("Будь ласка, оберіть бюджет.");
            return;
        }
        
        setupAccountTable();
        
        String userName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        welcomeLabel.setText("Ласкаво просимо, " + userName + "!");
        
        // Початкове завантаження даних
        refreshData(); 
    }
    
    // ------------------- НАЛАШТУВАННЯ ТАБЛИЦІ -------------------
    
    private void setupAccountTable() {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        currencyCol.setCellValueFactory(new PropertyValueFactory<>("currency"));
        balanceCol.setCellValueFactory(new PropertyValueFactory<>("balance"));

        // Колонка "Баланс" (форматування)
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

        // !!! НАЛАШТУВАННЯ КОЛОНКИ "ДОСТУП/СПІЛЬНІСТЬ" !!!
        sharedCol.setCellValueFactory(cellData -> {
            Account account = cellData.getValue();
            if (account.isShared()) {
                return new SimpleStringProperty("Спільний"); 
            } else {
                return new SimpleStringProperty("Приватний");
            }
        });

        // Додаємо стилізацію для спільного рахунку (опціонально)
        sharedCol.setCellFactory(column -> new TableCell<Account, String>() {
             @Override
             protected void updateItem(String item, boolean empty) {
                 super.updateItem(item, empty);
                 if (empty || item == null) {
                     setText(null);
                     setStyle("");
                 } else {
                     setText(item);
                     if ("Спільний".equals(item)) {
                         // Яскравий зелений для спільного доступу
                         setStyle("-fx-font-weight: bold; -fx-background-color: #dff0d8; -fx-text-fill: #3c763d;"); 
                     } else {
                         setStyle(null); // Скидаємо стиль для приватного
                     }
                 }
             }
         });
    }

    // ------------------- ЗАВАНТАЖЕННЯ ДАНИХ -------------------
    
    private void loadAccountsData() {
        try {
            // !!! ВИПРАВЛЕНО: Використовуємо AccountService для логіки доступу !!!
            List<Account> accounts = accountService.getAccountsForCurrentBudget();
            accountsTable.setItems(FXCollections.observableArrayList(accounts));
        } catch (Exception e) {
            System.err.println("Помилка при завантаженні рахунків: " + e.getMessage());
        }
    }
    
    private void loadStatistics() {
        if (currentBudgetId == null) return;

        // Використовуємо ReportingService з ID бюджету
        double netWorth = reportingService.getTotalNetWorth(currentBudgetId);
        netWorthLabel.setText(String.format("%.2f UAH", netWorth));
        
        Map<String, Double> summary = reportingService.getMonthlySummary(currentBudgetId);
        
        double totalIncome = summary.getOrDefault("Income", 0.0);
        double totalExpense = summary.getOrDefault("Expense", 0.0);
        
        incomeLabel.setText(String.format("%.2f UAH", totalIncome));
        expenseLabel.setText(String.format("%.2f UAH", totalExpense));
    }
    
    private void loadExchangeRatesAsync() {
        // (Логіка асинхронного завантаження курсів залишається коректною)
        if (exchangeRatesContainer == null) {
             System.err.println("ExchangeRatesContainer не підключено у FXML!");
             return;
         }
         exchangeRatesContainer.getChildren().clear();
         exchangeRatesContainer.getChildren().add(new Label("Завантаження курсів..."));
         
         CompletableFuture.supplyAsync(rateService::getRates)
             .thenAccept(rates -> {
                 javafx.application.Platform.runLater(() -> {
                     updateExchangeRatesUI(rates);
                 });
             });
    }

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

    // ------------------- ОБРОБНИКИ ПОДІЙ -------------------

    @FXML
    public void onOpenReportDialog() {
        MainController mainController = session.getMainController();
        if (mainController != null) {
            mainController.onReports();
        } else {
            System.err.println("MainController недоступний для навігації.");
        }
    }

    @FXML
    private void onAddAccount() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/account_form_dialog.fxml"));
            
            // !!! КЛЮЧОВЕ ВИПРАВЛЕННЯ: ControllerFactory для DI !!!
            loader.setControllerFactory(c -> {
                if (c.equals(AccountsController.class)) {
                    // Передаємо необхідний AccountService в AccountsController
                    return new AccountsController(accountService);
                }
                // Використовуйте конструктор за замовчуванням для інших контролерів
                try {
                    return c.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Не вдалося створити контролер " + c.getName(), e);
                }
            });
            
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
            showAlert(Alert.AlertType.ERROR, "Помилка завантаження", "Не вдалося завантажити діалогове вікно додавання рахунку.");
            e.printStackTrace();
        }
    }
    
    // ------------------- МЕТОДИ ОНОВЛЕННЯ -------------------

    public void updateViewForNewBudget() {
        this.currentBudgetId = session.getCurrentBudgetId();
        // Оновлюємо відображення імені користувача при перемиканні
        String userName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        welcomeLabel.setText("Ласкаво просимо, " + userName + "!");
        refreshData();
    }

    public void refreshData() {
        loadAccountsData();
        loadStatistics();
        loadExchangeRatesAsync(); 
    }
    
    // Допоміжна функція для повідомлень (залишається корисною)
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}