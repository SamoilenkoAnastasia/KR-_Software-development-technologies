package ua.kpi.personal.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ua.kpi.personal.model.Account;
import ua.kpi.personal.repo.AccountDao;
import ua.kpi.personal.model.User;
import ua.kpi.personal.state.ApplicationSession;

// ? Примітка: Цей контролер буде працювати як контролер для account_form_dialog.fxml
public class AccountsController { 
    
    // Елементи форми (залишаємо, але змінюємо ChoiceBox на ComboBox для універсальності)
    @FXML private TextField nameField;
    @FXML private TextField balanceField;
    @FXML private ComboBox<String> currencyChoice; // Changed from ChoiceBox to ComboBox
    @FXML private ComboBox<String> typeChoice;     // New field for type
    @FXML private Label messageLabel;
    @FXML private Button saveBtn; // Button name changed

    // НЕ ПОТРІБНІ ДЛЯ ДІАЛОГУ:
    // @FXML private ListView<Account> listView;
    // @FXML private Button backBtn;

    private final AccountDao accountDao = new AccountDao();
    private User user;
    private DashboardViewController dashboardController; // ? Посилання на головний контролер

    @FXML
    private void initialize(){
        this.user = ApplicationSession.getInstance().getCurrentUser();
        // Додаємо тип рахунку, який був відсутній у вашій формі, але є в моделі
        typeChoice.getItems().addAll("Cash", "Card", "Savings", "Other"); 
        currencyChoice.getItems().addAll("UAH", "USD", "EUR");
        
        // Встановлюємо значення за замовчуванням
        typeChoice.getSelectionModel().selectFirst();
        currencyChoice.getSelectionModel().selectFirst();
        // refresh(); // Режим діалогу не потребує цього при ініціалізації
    }

    /**
     * Встановлює посилання на контролер дашборду для оновлення даних після збереження.
     */
    public void setDashboardController(DashboardViewController dashboardController) {
        this.dashboardController = dashboardController;
    }
    
    // onSave замість onAdd
    @FXML
    private void onSave(){
        String name = nameField.getText();
        String currency = currencyChoice.getValue();
        String type = typeChoice.getValue(); // Отримуємо тип
        
        if(name==null || name.isBlank()){ messageLabel.setText("Введіть назву"); return; }
        if(currency==null){ messageLabel.setText("Оберіть валюту"); return; }
        if(type==null){ messageLabel.setText("Оберіть тип рахунку"); return; }

        double bal = 0;
        try{ 
            // Використовуємо .trim() для чистоти
            bal = Double.parseDouble(balanceField.getText().trim()); 
        } catch(Exception e){ 
            messageLabel.setText("Невірний формат балансу (використовуйте .)"); 
            return;
        }
        
        Account a = new Account();
        a.setName(name);
        a.setBalance(bal);
        a.setUser(user);    
        a.setCurrency(currency); 
        a.setType(type); 
        
        Account created = accountDao.create(a);
        if(created!=null){ 
            messageLabel.setText("Рахунок успішно додано."); 
            
            // 1. Оновлення дашборду
            if (dashboardController != null) {
                dashboardController.refreshData();
            }
            // 2. Закриття діалогу
            closeDialog();
            
        } else {
            messageLabel.setText("Помилка збереження в базі даних.");
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