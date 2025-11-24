package ua.kpi.personal.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ua.kpi.personal.model.Account;
import ua.kpi.personal.service.AccountService; 
import ua.kpi.personal.model.User;
import ua.kpi.personal.state.ApplicationSession;
import ua.kpi.personal.state.BudgetAccessState;

public class AccountsController { 
    
    @FXML private TextField nameField;
    @FXML private TextField balanceField;
    @FXML private ComboBox<String> currencyChoice; 
    @FXML private ComboBox<String> typeChoice;    
    @FXML private Label messageLabel;
    @FXML private Button saveBtn;
    
    @FXML private CheckBox isSharedCheckbox; 

    private final AccountService accountService;
    private final ApplicationSession session = ApplicationSession.getInstance();
    private User user;
    private DashboardViewController dashboardController; 

    // Конструктор для DI (Dependency Injection)
    public AccountsController(AccountService accountService) {
        this.accountService = accountService;
    }

    @FXML
    private void initialize(){
        this.user = session.getCurrentUser();
        
        typeChoice.getItems().addAll("Cash", "Card", "Savings", "Other"); 
        currencyChoice.getItems().addAll("UAH", "USD", "EUR");
        
        typeChoice.getSelectionModel().selectFirst();
        currencyChoice.getSelectionModel().selectFirst();
        
        // ЛОГІКА: ОБМЕЖЕННЯ ДОСТУПУ ДО ЧЕКБОКСА
        BudgetAccessState accessState = session.getCurrentBudgetAccessState();
        if (isSharedCheckbox != null) {
            if (!accessState.isOwner()) {
                // Гість не може робити рахунки спільними - деактивуємо та інформуємо
                isSharedCheckbox.setDisable(true);
                isSharedCheckbox.setText("Зробити рахунок спільним (Доступно лише Власнику)");
            } else {
                // Власник може, за замовчуванням приватний
                isSharedCheckbox.setSelected(false);
            }
        }
    }

    public void setDashboardController(DashboardViewController dashboardController) {
        this.dashboardController = dashboardController;
    }
    
    @FXML
    private void onSave(){
        String name = nameField.getText();
        String currency = currencyChoice.getValue();
        String type = typeChoice.getValue(); 
        
        if(name==null || name.isBlank()){ messageLabel.setText("Введіть назву"); return; }
        if(currency==null){ messageLabel.setText("Оберіть валюту"); return; }
        if(type==null){ messageLabel.setText("Оберіть тип рахунку"); return; }

        double bal = 0;
        try{ 
            bal = Double.parseDouble(balanceField.getText().trim()); 
        } catch(Exception e){ 
            messageLabel.setText("Невірний формат балансу (використовуйте .)"); 
            return;
        }
        
        Account a = new Account();
        a.setName(name);
        a.setBalance(bal);
        // User та Budget ID будуть встановлені у AccountService
        a.setCurrency(currency); 
        a.setType(type); 
        
        // ЛОГІКА: ЗБЕРЕЖЕННЯ isShared (перевірка isSharedCheckbox на null - безпечна практика)
        boolean isSharedSelected = isSharedCheckbox != null && isSharedCheckbox.isSelected();
        
        // Встановлюємо isShared тільки якщо це ВЛАСНИК І він обрав чекбокс
        if (isSharedSelected && session.getCurrentBudgetAccessState().isOwner()) {
             a.setShared(true);
        } else {
             a.setShared(false); 
        }
        
        try {
            // Викликаємо сервіс, який виконає всі перевірки та збереже
            accountService.createAccount(a); 
            messageLabel.setText("Рахунок успішно додано."); 
                
            if (dashboardController != null) {
                dashboardController.refreshData(); // Оновлюємо таблицю на дашборді
            }
            closeDialog();
        } catch (SecurityException se) {
             messageLabel.setText("Помилка доступу: " + se.getMessage());
        } catch (Exception e) {
             messageLabel.setText("Помилка збереження: " + e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }
    
    private void closeDialog() {
        // Отримуємо Stage з будь-якого елемента і закриваємо
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}